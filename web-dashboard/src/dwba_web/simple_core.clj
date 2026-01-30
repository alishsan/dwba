(ns dwba-web.simple-core
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response content-type]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [functions :as phys]           ;; use core calculations from main project
            ;; Don't require inelastic/transfer at startup - load lazily when needed
            [complex :as c]                 ;; complex numbers
            [ring.adapter.jetty :as jetty]))

;; ---------------------------
;; Parsing / validation helpers
;; ---------------------------

(defn- parse-double*
  "Parse a value into a double, raising a clear ex-info on failure."
  [field-name v]
  (try
    (Double/parseDouble (str/trim (str v)))
    (catch Exception _
      (throw (ex-info (format "Invalid %s: expected a number, got %s" field-name (pr-str v))
                      {:field field-name :value v})))))

(defn- parse-int*
  "Parse a value into an int, raising a clear ex-info on failure."
  [field-name v]
  (try
    (Integer/parseInt (str/trim (str v)))
    (catch Exception _
      (throw (ex-info (format "Invalid %s: expected an integer, got %s" field-name (pr-str v))
                      {:field field-name :value v})))))

;; Ensure handlers never return nil (Ring/Jetty require a response map)
(defn wrap-nil-response [handler]
  (fn [request]
    (or (handler request)
        {:status 404 :body "Not found" :headers {"Content-Type" "text/plain"}})))

;; Helper function to serve index.html
(defn serve-index []
  (let [resource (or (io/resource "index.html")
                     (io/resource "public/index.html"))
        ;; Try multiple file paths depending on working directory
        file-paths ["public/index.html"
                    "web-dashboard/public/index.html"
                    (str (System/getProperty "user.dir") "/public/index.html")
                    (str (System/getProperty "user.dir") "/web-dashboard/public/index.html")]
        file (some #(let [f (io/file %)] (when (.exists f) f)) file-paths)]
    (cond
      resource
      (-> resource
          (io/input-stream)
          (response)
          (content-type "text/html"))
      file
      (-> file
          (io/input-stream)
          (response)
          (content-type "text/html"))
      :else
      {:status 404 
       :body (str "index.html not found. Tried: " (pr-str file-paths))})))

;; Serve a static file from classpath (works regardless of working directory)
(defn- content-type-for [filename]
  (cond
    (str/ends-with? filename ".js")  "application/javascript"
    (str/ends-with? filename ".css") "text/css"
    (str/ends-with? filename ".html") "text/html"
    (str/ends-with? filename ".map")  "application/json"
    :else "application/octet-stream"))

(defn- serve-resource [resource-name]
  (if-let [resource (io/resource resource-name)]
    (-> resource
        (io/input-stream)
        (response)
        (content-type (content-type-for resource-name)))
    {:status 404 :body "Not found" :headers {"Content-Type" "text/plain"}}))

;; Web routes (API POST first so /api/calculate is never shadowed by file serving)
(defroutes app-routes
  (POST "/api/calculate" req
    (try
      (let [params (or (:body req) (:params req) {})
            parse-doubles (fn [xs] (mapv #(Double/parseDouble (str %)) xs))
            parse-ints    (fn [xs] (mapv #(Integer/parseInt (str %)) xs))
            energies      (parse-doubles (or (:energies params) []))
            L-values      (parse-ints (or (:L_values params) []))
            ws-params     [(Double/parseDouble (str (:V0 params)))
                           (Double/parseDouble (str (:R0 params)))
                           (Double/parseDouble (str (:a0 params)))]
            radius        (Double/parseDouble (str (:radius params)))]
        (when (or (empty? energies) (empty? L-values))
          (throw (ex-info "Missing energies or L_values" {:energies energies :L_values L-values})))
        (let [compare-methods (boolean (:compare_methods params))
              h-numerov 0.01
              combinations (for [E energies L L-values] [E L])
              numerov-cache (into {}
                                 (pmap (fn [[E L]]
                                         (let [[V0 R0 a0] ws-params
                                               u (phys/solve-numerov E L V0 R0 a0 h-numerov radius)]
                                           [[E L] u]))
                                       combinations))
              phase-shift-cache (into {}
                                     (pmap (fn [[E L]]
                                             [[E L] (phys/phase-shift E ws-params radius L)])
                                           combinations))
              r-matrix-a-cache (into {}
                                    (pmap (fn [[E L]]
                                            (let [u (get numerov-cache [E L])
                                                  R (phys/r-matrix-from-numerov u h-numerov radius)
                                                  Ra (* R radius)]
                                              [[E L] Ra]))
                                          combinations))
              r-matrix-cache (into {}
                                  (pmap (fn [[E L]]
                                          [[E L] (phys/r-matrix E ws-params radius L)])
                                        combinations))
              comparison-data (when compare-methods
                               (into {}
                                    (pmap (fn [[E L]]
                                            (let [Ra-orig (phys/r-matrix-a E ws-params radius L)
                                                  Ra-num (get r-matrix-a-cache [E L])
                                                  Ra-diff (Math/abs (- Ra-orig Ra-num))
                                                  Ra-rel-error (if (zero? Ra-orig)
                                                                (if (zero? Ra-diff) 0.0 100.0)
                                                                (* 100.0 (/ Ra-diff (Math/abs Ra-orig))))]
                                              [[E L] {:r_matrix_a {:original Ra-orig :numerov Ra-num
                                                                   :difference Ra-diff :relative_error_percent Ra-rel-error}
                                                      :note "Phase shifts use same method (4-arg phase-shift)"}]))
                                          (take 5 combinations))))
              phase-shift-data (for [E energies L L-values]
                                {:energy E :L L :phase_shift (get phase-shift-cache [E L])})
              r-matrix-data (for [E energies L L-values]
                             {:energy E :L L
                              :r_nuclear (get r-matrix-a-cache [E L])
                              :r_coulomb_nuclear (get r-matrix-cache [E L])})
              potential-data (let [radii (range 0.1 10.0 0.2)]
                              (for [r radii]
                                {:radius r
                                 :woods_saxon (phys/WS r ws-params)
                                 :coulomb (phys/Coulomb-pot r (second ws-params))
                                 :combined (+ (phys/WS r ws-params) (phys/Coulomb-pot r (second ws-params)))}))
              cross-section-data (for [E energies]
                                  {:energy E
                                   :total_cross_section (reduce + (map #(Math/pow (Math/sin (get phase-shift-cache [E %])) 2) L-values))})]
          (response {:success true
                     :data {:phase_shifts (vec phase-shift-data)
                            :r_matrices (vec r-matrix-data)
                            :potentials (vec potential-data)
                            :cross_sections (vec cross-section-data)
                            :parameters {:energies energies :L_values L-values :ws_params ws-params
                                         :radius radius :h_numerov h-numerov :method "Numerov-based"}
                            :comparison (when comparison-data
                                         {:method_comparison comparison-data
                                          :note "Comparison of Numerov-based vs original fine-step methods"})}})))
      (catch Exception e
        (response {:success false :error (.getMessage e)}))))

  ;; Serve static files
  (GET "/" [] (serve-index))
  (GET "/app.js" [] (serve-resource "app.js"))
  (GET "/js/dashboard.js" [] (serve-resource "js/dashboard.js"))
  (GET "/api/health" [] 
    (response {:status "ok" :message "DWBA Web Dashboard API"}))
  (GET "/api/parameters" []
    (response {:default_parameters
               {:energies (vec (map double (range 5 31)))
                :L_values [0 1 2 3 4 5]
                :V0 40.0 :R0 2.0 :a0 0.6 :radius 3.0
                :E_ex 4.44 :lambda 2 :beta 0.25 :reaction_type "d-p"}
               :parameter_ranges
               {:V0 {:min -100.0 :max 100.0 :step 1.0}
                :R0 {:min 0.5 :max 5.0 :step 0.1}
                :a0 {:min 0.1 :max 2.0 :step 0.1}
                :radius {:min 1.0 :max 10.0 :step 0.1}
                :E_ex {:min 0.0 :max 20.0 :step 0.1}
                :lambda {:min 1 :max 5 :step 1}
                :beta {:min 0.0 :max 1.0 :step 0.01}}}))
  (OPTIONS "/api/health" [] (response nil))
  (OPTIONS "/api/calculate" [] (response nil))
  (OPTIONS "/api/parameters" [] (response nil))
  (OPTIONS "/api/elastic" [] (response nil))
  (OPTIONS "/api/inelastic" [] (response nil))
  (OPTIONS "/api/transfer" [] (response nil))
  ;; GET /api/calculate would otherwise hit route/files and 404; return 405 so path is clearly ours
  (GET "/api/calculate" [] (assoc (response "Use POST to submit calculation") :status 405))

  ;; Elastic scattering endpoint
  (POST "/api/elastic" req
    (try
      (let [params (or (:body req) (:params req) {})
            parse-doubles (fn [xs] (mapv #(Double/parseDouble (str %)) xs))
            parse-ints    (fn [xs] (mapv #(Integer/parseInt (str %)) xs))
            energies      (parse-doubles (or (:energies params) []))
            L-values      (parse-ints (or (:L_values params) []))
            ws-params     [(Double/parseDouble (str (:V0 params)))
                           (Double/parseDouble (str (:R0 params)))
                           (Double/parseDouble (str (:a0 params)))]
            radius        (Double/parseDouble (str (:radius params)))
            angles        (or (parse-doubles (or (:angles params) []))
                             (range 0.0 181.0 10.0))]  ; Default: 0-180 deg in 10 deg steps
        
        (when (or (empty? energies) (empty? L-values))
          (throw (ex-info "Missing energies or L_values" {:energies energies :L_values L-values})))
        
        (let [;; Cache resolved function to avoid repeated resolution
              dsigma-fn (or (resolve 'functions/differential-cross-section)
                            (do (require 'functions) (resolve 'functions/differential-cross-section)))
              L-max (apply max L-values)
              elastic-data
              (for [E energies
                    theta angles]
                (let [theta-rad (* theta (/ Math/PI 180.0))
                      dsigma-complex (if dsigma-fn
                                       (dsigma-fn E ws-params theta-rad L-max)
                                       0.0)
                      dsigma (if (number? dsigma-complex) 
                              dsigma-complex 
                              (c/mag dsigma-complex))]
                  {:energy E
                   :angle theta
                   :differential_cross_section dsigma}))]
          
          (response {:success true
                     :data {:elastic elastic-data
                            :parameters {:energies energies
                                         :L_values L-values
                                         :ws_params ws-params
                                         :radius radius
                                         :angles angles}}})))
      (catch Exception e
        (response {:success false
                   :error (.getMessage e)}))))
  
  ;; Inelastic scattering endpoint
  (POST "/api/inelastic" req
    (try
      ;; Lazy load inelastic namespace only when needed (cache after first load)
      (when-not (find-ns 'dwba.inelastic)
        (require 'dwba.inelastic))
      ;; Cache resolved functions to avoid repeated resolution
      (let [inel (or (resolve 'dwba.inelastic/distorted-wave-entrance)
                     (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/distorted-wave-entrance)))
            inel-exit (or (resolve 'dwba.inelastic/distorted-wave-exit)
                          (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/distorted-wave-exit)))
            inel-cross (or (resolve 'dwba.inelastic/inelastic-cross-section)
                           (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/inelastic-cross-section)))
            params (or (:body req) (:params req) {})
            parse-doubles (fn [xs] (mapv #(Double/parseDouble (str %)) xs))
            parse-ints    (fn [xs] (mapv #(Integer/parseInt (str %)) xs))
            energies      (parse-doubles (or (:energies params) []))
            L-values      (parse-ints (or (:L_values params) []))
            ws-params     [(parse-double* "V0" (:V0 params))
                           (parse-double* "R0" (:R0 params))
                           (parse-double* "a0" (:a0 params))]
            E-ex          (parse-double* "excitation energy E_ex" (:E_ex params))
            lambda        (parse-int* "multipole order lambda" (:lambda params))
            beta          (parse-double* "deformation parameter beta" (:beta params))
            ;; Optimize: Use coarser step size and smaller radius for faster calculations
            ;; h=0.02 reduces points from ~2000 to ~1000, r-max=15 reduces from 1000 to 750 points
            ;; Total: ~2.7x fewer points = ~2.7x faster Numerov integration
            h             0.02  ; Optimized step size (was 0.01)
            r-max         15.0  ; Reduced max radius (was 20.0) - sufficient for most calculations
            mu            0     ; Magnetic quantum number (default 0)
            mass-factor   phys/mass-factor]
        
        (when (or (empty? energies) (empty? L-values))
          (throw (ex-info "Missing energies or L_values" {:energies energies :L_values L-values})))
        
        ;; Optimized: Pre-compute transition potential vector once (same for all E, L combinations)
        ;; This avoids recreating it 156 times (26 energies × 6 L-values)
        ;; The transition potential only depends on lambda, beta, V-params, r-max, h (all constant)
        (let [n-points (int (/ r-max h))
              transition-form-factor-fn (or (resolve 'dwba.inelastic/transition-form-factor)
                                            (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/transition-form-factor)))
              V-transition-vec (when transition-form-factor-fn
                                (mapv (fn [i]
                                       (let [r (* i h)]
                                         (transition-form-factor-fn r lambda beta ws-params)))
                                     (range n-points)))
              inelastic-amplitude-radial-fn (or (resolve 'dwba.inelastic/inelastic-amplitude-radial)
                                                (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/inelastic-amplitude-radial)))
              inelastic-dsigma-fn (or (resolve 'dwba.inelastic/inelastic-differential-cross-section)
                                     (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/inelastic-differential-cross-section)))
              angular-factor (* 4.0 Math/PI)  ; For mu=0 (axially symmetric)
              ;; Optimized: Use coarser step size (h=0.02) and smaller radius (r-max=15.0)
              ;; This reduces Numerov integration points from ~2000 to ~750 (2.7x fewer points)
              inelastic-data
              (for [E-i energies
                    L-i L-values]
                (try
                  (let [chi-i (inel E-i L-i ws-params h r-max)
                        chi-f (inel-exit E-i E-ex L-i ws-params h r-max)
                        ;; Use pre-computed transition potential to avoid recreating it 156 times
                        dsigma (if (and V-transition-vec inelastic-amplitude-radial-fn inelastic-dsigma-fn)
                                (let [T-radial (inelastic-amplitude-radial-fn chi-i chi-f V-transition-vec r-max h)
                                      T-inel (if (number? T-radial)
                                              (* angular-factor T-radial)
                                              (c/mul angular-factor T-radial))
                                      k-i (Math/sqrt (* mass-factor E-i))
                                      E-f (- E-i E-ex)
                                      k-f (Math/sqrt (* mass-factor E-f))]
                                  (inelastic-dsigma-fn T-inel k-i k-f E-i E-ex mass-factor))
                                ;; Fallback to original method if functions not available
                                (inel-cross chi-i chi-f lambda mu beta ws-params E-i E-ex r-max h mass-factor))]
                    {:energy E-i
                     :L L-i
                     :excitation_energy E-ex
                     :differential_cross_section dsigma})
                  (catch Exception e
                    {:energy E-i
                     :L L-i
                     :excitation_energy E-ex
                     :differential_cross_section 0.0
                     :error (.getMessage e)})))]
          
          (response {:success true
                     :data {:inelastic inelastic-data
                            :parameters {:energies energies
                                         :L_values L-values
                                         :ws_params ws-params
                                         :E_ex E-ex
                                         :lambda lambda
                                         :beta beta
                                         :h h
                                         :r_max r-max}}})))
      (catch Exception e
        (response {:success false
                   :error (.getMessage e)}))))
  
  ;; Transfer reaction endpoint
  (POST "/api/transfer" req
  (try
    ;; Lazy load transfer namespace only when needed (cache after first load)
    (when-not (find-ns 'dwba.transfer)
      (require 'dwba.transfer))
    ;; Also lazy-load form-factors for proper bound-state overlaps
    (when-not (find-ns 'dwba.form-factors)
      (require 'dwba.form-factors))
    ;; Cache resolved functions to avoid repeated resolution
    (let [zero-range-const (or (resolve 'dwba.transfer/zero-range-constant)
                               (do (require 'dwba.transfer) (resolve 'dwba.transfer/zero-range-constant)))
          transfer-amp (or (resolve 'dwba.transfer/transfer-amplitude-zero-range)
                           (do (require 'dwba.transfer) (resolve 'dwba.transfer/transfer-amplitude-zero-range)))
          transfer-dsigma (or (resolve 'dwba.transfer/transfer-differential-cross-section)
                              (do (require 'dwba.transfer) (resolve 'dwba.transfer/transfer-differential-cross-section)))
          ;; Use the higher-level bound-state solver and normalized overlap,
          ;; so the endpoint is consistent with the physics examples.
          solve-bound-state (or (resolve 'dwba.transfer/solve-bound-state)
                                (do (require 'dwba.transfer) (resolve 'dwba.transfer/solve-bound-state)))
          normalized-overlap (or (resolve 'dwba.form-factors/normalized-overlap)
                                 (do (require 'dwba.form-factors) (resolve 'dwba.form-factors/normalized-overlap)))
          params (or (:body req) (:params req) {})
          parse-doubles (fn [xs] (mapv #(Double/parseDouble (str %)) xs))
          parse-ints    (fn [xs] (mapv #(Integer/parseInt (str %)) xs))
          energies      (parse-doubles (or (:energies params) []))
          L-values      (parse-ints (or (:L_values params) []))
          ws-params     [(Double/parseDouble (str (:V0 params)))
                         (Double/parseDouble (str (:R0 params)))
                         (Double/parseDouble (str (:a0 params)))]
          reaction-type (keyword (str (:reaction_type params)))
          S-factor      1.0  ; Fixed to 1.0 as requested
          mass-factor   phys/mass-factor
          ;; Numerical parameters for bound-state and overlap calculation
          h             0.01
          r-max         20.0
          ;; For now, mirror the 16O(p,d) example:
          ;; - Initial bound state: l=1 neutron in WS potential
          ;; - Final bound state:   l=0 neutron in WS potential
          ;; This gives a more realistic overlap than the old placeholder = 1.0.
          bound-i       (solve-bound-state ws-params 1 1 nil r-max h)
          bound-f       (solve-bound-state ws-params 1 0 nil r-max h)
          phi-i         (:normalized-wavefunction bound-i)
          phi-f         (:normalized-wavefunction bound-f)
          overlap-approx (normalized-overlap phi-i phi-f r-max h)]
      
      (when (or (empty? energies) (empty? L-values))
        (throw (ex-info "Missing energies or L_values" {:energies energies :L_values L-values})))
      
      (let [D0 (zero-range-const reaction-type)
            transfer-data
            (for [E-i energies
                  L L-values]
              (try
                (let [E-f-approx (* 0.8 E-i)  ; Approximate exit energy (kept schematic for now)
                      k-i (Math/sqrt (* mass-factor E-i))
                      k-f (Math/sqrt (* mass-factor E-f-approx))
                      T-amplitude (transfer-amp overlap-approx D0)
                      dsigma (transfer-dsigma T-amplitude S-factor k-i k-f mass-factor mass-factor)]
                  {:energy E-i
                   :L L
                   :differential_cross_section dsigma
                   :transfer_amplitude (if (number? T-amplitude) T-amplitude (c/mag T-amplitude))})
                (catch Exception e
                  {:energy E-i
                   :L L
                   :differential_cross_section 0.0
                   :error (.getMessage e)})))]
        
        (response {:success true
                   :data {:transfer transfer-data
                          :parameters {:energies energies
                                       :L_values L-values
                                       :ws_params ws-params
                                       :reaction_type (str reaction-type)
                                       :S_factor S-factor}}})))
    (catch Exception e
      (response {:success false
                 :error (.getMessage e)})))
  
  ;; Fallback for static files
  (route/files "/" {:root "public"})
  (route/not-found "Not Found")))
;; Middleware
(def app
  (-> app-routes
      wrap-nil-response
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :options]
                 :access-control-allow-headers ["Content-Type"])))

;; Server
(defn start-server [port]
  (jetty/run-jetty app {:port port :join? false}))

(defn -main [& args]
  (let [port (or (some-> args first Integer/parseInt) 3000)]
    (println (str "Starting DWBA Web Dashboard on port " port))
    (start-server port)))
