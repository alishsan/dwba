(ns dwba-web.simple-core
  (:require [compojure.core :refer [routes GET POST OPTIONS]]
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

(defn- query-param [request param-name]
  (when-let [q (str (:query-string request))]
    (when-let [match (re-find (re-pattern (str (str/replace param-name #"\s" "") "=([^&]*)")) q)]
      (str/trim (second match)))))

;; Target nucleus params for (p,d) default plot
(def ^:private transfer-targets
  {"16O" {:A 16 :Z 8 :residual-A 15 :m-target 14899.0 :m-residual 13975.0
          :Q (+ 938.27 14899.0 (- 1876.136) (- 13975.0))
          :Es-i -15.67 :Es-f -2.214 :label "16O(p,d)15O"}
   "12C" {:A 12 :Z 6 :residual-A 11 :m-target 11178.0 :m-residual 10257.0
          :Q (+ 938.27 11178.0 (- 1876.136) (- 10257.0))
          :Es-i -16.0 :Es-f -2.0 :label "12C(p,d)11C"}})

(defn- transfer-default-data
  "Build default (p,d) DCS data for a target. Returns {:label ... :transfer (vec of {:energy :angle :differential_cross_section})}."
  [target]
  (let [{:keys [A Z residual-A m-target m-residual Q Es-i Es-f label]} target]
    (when-not (find-ns 'dwba.transfer) (require 'dwba.transfer))
    (when-not (find-ns 'dwba.form-factors) (require 'dwba.form-factors))
    (when-not (find-ns 'dwba.inelastic) (require 'dwba.inelastic))
    (let [t (resolve 'dwba.transfer/transfer-differential-cross-section-angular)
          t-post (resolve 'dwba.transfer/transfer-amplitude-post)
          t-zero (resolve 'dwba.transfer/zero-range-constant)
          inel-entrance (resolve 'dwba.inelastic/distorted-wave-entrance)
          inel-exit (resolve 'dwba.inelastic/distorted-wave-exit)
          solve-num (resolve 'dwba.transfer/solve-bound-state-numerov)
          normalize (resolve 'dwba.transfer/normalize-bound-state)
          r-max 20.0, h 0.01, l-i 1, l-f 0
          v0-i 62.0, R0-i 2.7, diff-i 0.6
          v0-f 50.0, R0-f 1.5, diff-f 0.6
          m-f 0.048
          phi-i (normalize (solve-num Es-i l-i v0-i R0-i diff-i m-f h r-max) h)
          phi-f (normalize (solve-num Es-f l-f v0-f R0-f diff-f m-f h r-max) h)
          E-lab 20.0, m-p 938.27, m-d 1876.136
          mu-i (/ (* m-p m-target) (+ m-p m-target))
          mu-f (/ (* m-d m-residual) (+ m-d m-residual))
          mass-factor-i (/ (* 2.0 mu-i) (* 197.7 197.7))
          mass-factor-f (/ (* 2.0 mu-f) (* 197.7 197.7))
          E-CM-i (* E-lab (/ m-target (+ m-target m-p)))
          E-CM-f (+ E-CM-i Q)
          E-lab-f (* E-CM-f (/ (+ m-d m-residual) m-residual))
          L-max 7
          D0 (t-zero :p-d)
          T-amplitudes (into {}
                           (for [L (range (inc L-max))]
                             (let [chi-i (inel-entrance E-CM-i L nil h r-max
                                                        :projectile-type :p :target-A A :target-Z Z
                                                        :E-lab E-lab :s 0.5 :j (+ L 0.5) :mass-factor mass-factor-i)
                                   chi-f (inel-exit E-CM-i Q L nil h r-max
                                                    :outgoing-type :d :residual-A residual-A :residual-Z Z
                                                    :E-lab E-lab-f :s 1 :j (inc L) :mass-factor mass-factor-f)
                                   T-L (t-post chi-i chi-f phi-i phi-f r-max h :zero-range D0)]
                               [L T-L])))
          k-i (Math/sqrt (* mass-factor-i E-CM-i))
          k-f (Math/sqrt (* mass-factor-f E-CM-f))
          S-factor 1.0
          E-default 20.0
          angles-deg (range 20.0 181.0 20.0)]
      (let [;; 1 mb = 10 fm² => dσ/dΩ (mb/sr) = dσ/dΩ (fm²/sr) / 10
            fm2->mb 0.1
            transfer-vec (vec (for [theta-deg angles-deg]
                                (let [theta-rad (* theta-deg (/ Math/PI 180.0))
                                      dsigma-fm2 (t T-amplitudes S-factor k-i k-f theta-rad
                                                    mass-factor-i mass-factor-f 0.0 l-i l-f)]
                                  {:energy E-default
                                   :angle theta-deg
                                   :differential_cross_section (* fm2->mb dsigma-fm2)})))]
        {:label label
         :transfer transfer-vec}))))

(defn- transfer-default-response
  "Ring response for GET /api/transfer-default. target-key is e.g. \"16O\"."
  [target-key]
  (let [target (get transfer-targets target-key (get transfer-targets "16O"))
        {:keys [label transfer]} (transfer-default-data target)]
    (response {:success true
               :target label
               :data {:transfer transfer
                      :parameters {:default (str label " at 20 MeV")
                                   :target label}}})))

(defn- serve-resource [resource-name]
  (if-let [resource (io/resource resource-name)]
    (-> resource
        (io/input-stream)
        (response)
        (content-type (content-type-for resource-name)))
    {:status 404 :body "Not found" :headers {"Content-Type" "text/plain"}}))

;; ---------------------------
;; Shared request parsing
;; ---------------------------
(defn- tokens-from [xs]
  "Normalize to a seq of string tokens: split string by comma/whitespace, use sequential as-is, else []."
  (cond (string? xs)    (str/split (str/trim (str xs)) #"[\s,]+")
        (sequential? xs) (seq xs)
        :else           []))

(defn- safe-parse-double [s]
  (try (when-not (str/blank? (str s)) (Double/parseDouble (str/trim (str s))))
       (catch Exception _ nil)))

(defn- safe-parse-int [s]
  (try (when-not (str/blank? (str s)) (Integer/parseInt (str/trim (str s))))
       (catch Exception _ nil)))

(defn- parse-doubles [xs]
  (let [toks (filter (fn [x] (not (str/blank? (str x)))) (tokens-from xs))
        parsed (mapv safe-parse-double toks)]
    (vec (remove nil? parsed))))
(defn- parse-ints [xs]
  (let [toks (filter (fn [x] (not (str/blank? (str x)))) (tokens-from xs))
        parsed (mapv safe-parse-int toks)]
    (vec (remove nil? parsed))))
(defn- params [req] (or (:body req) (:params req) {}))

(defn- parse-double-default
  "Parse v as double; if nil or blank, return default."
  [v default]
  (let [s (str/trim (str v))]
    (if (str/blank? s) default (Double/parseDouble s))))

(defn- ws-params-from [params]
  [(parse-double-default (:V0 params) 40.0)
   (parse-double-default (:R0 params) 2.0)
   (parse-double-default (:a0 params) 0.6)])

(def ^:private default-energies (vec (range 5 31)))
(def ^:private default-L-values [0 1 2 3 4 5])

(defn- ensure-energies-L [energies L-values]
  [(if (empty? energies) default-energies energies)
   (if (empty? L-values) default-L-values L-values)])

(defn- parse-lambdas [params]
  "Parse :lambdas \"2,3,4\" or :lambda 2 into a vector of integers (multipole orders). Default [2]."
  (let [lambdas-raw (or (:lambdas params) (:lambda params))]
    (if (nil? lambdas-raw)
      [2]
      (let [parsed (parse-ints (if (string? lambdas-raw)
                                (str/split (str lambdas-raw) #"[\s,]+")
                                [lambdas-raw]))]
        (if (empty? parsed) [2] (vec parsed))))))

;; ---------------------------
;; API handlers (req -> response)
;; ---------------------------
(defn- handle-api-calculate [req]
  (try
    (let [p (params req)
          [energies L-values] (ensure-energies-L (parse-doubles (:energies p)) (parse-ints (:L_values p)))
          ws (ws-params-from p)
          radius (parse-double-default (:radius p) 3.0)]
      (let [compare-methods (boolean (:compare_methods p))
            h-numerov 0.01
            combinations (for [E energies L L-values] [E L])
            numerov-cache (into {} (pmap (fn [[E L]] (let [[V0 R0 a0] ws, u (phys/solve-numerov E L V0 R0 a0 h-numerov radius)] [[E L] u])) combinations))
            phase-shift-cache (into {} (pmap (fn [[E L]] [[E L] (phys/phase-shift E ws radius L)]) combinations))
            r-matrix-a-cache (into {} (pmap (fn [[E L]] (let [u (get numerov-cache [E L]), R (phys/r-matrix-from-numerov u h-numerov radius)] [[E L] (* R radius)])) combinations))
            r-matrix-cache (into {} (pmap (fn [[E L]] [[E L] (phys/r-matrix E ws radius L)]) combinations))
            comparison-data (when compare-methods
                              (into {} (pmap (fn [[E L]]
                                               (let [Ra-orig (phys/r-matrix-a E ws radius L), Ra-num (get r-matrix-a-cache [E L])
                                                     Ra-diff (Math/abs (- Ra-orig Ra-num))
                                                     Ra-rel (if (zero? Ra-orig) (if (zero? Ra-diff) 0.0 100.0) (* 100.0 (/ Ra-diff (Math/abs Ra-orig))))]
                                                 [[E L] {:r_matrix_a {:original Ra-orig :numerov Ra-num :difference Ra-diff :relative_error_percent Ra-rel}
                                                         :note "Phase shifts use same method (4-arg phase-shift)"}]))
                                             (take 5 combinations))))
            phase-shift-data (for [E energies L L-values] {:energy E :L L :phase_shift (get phase-shift-cache [E L])})
            r-matrix-data (for [E energies L L-values] {:energy E :L L :r_nuclear (get r-matrix-a-cache [E L]) :r_coulomb_nuclear (get r-matrix-cache [E L])})
            radii (range 0.1 10.0 0.2)
            potential-data (for [r radii] {:radius r :woods_saxon (phys/WS r ws) :coulomb (phys/Coulomb-pot r (second ws))
                                          :combined (+ (phys/WS r ws) (phys/Coulomb-pot r (second ws)))})
            cross-section-data (for [E energies]
                                {:energy E :total_cross_section (reduce + (map #(Math/pow (Math/sin (get phase-shift-cache [E %])) 2) L-values))})]
        (response {:success true
                   :data {:phase_shifts (vec phase-shift-data) :r_matrices (vec r-matrix-data) :potentials (vec potential-data)
                          :cross_sections (vec cross-section-data)
                          :parameters {:energies energies :L_values L-values :ws_params ws :radius radius :h_numerov h-numerov :method "Numerov-based"}
                          :comparison (when comparison-data {:method_comparison comparison-data :note "Comparison of Numerov-based vs original fine-step methods"})}})))
    (catch Exception e (response {:success false :error (.getMessage e)}))))

(defn- handle-api-elastic [req]
  (try
    (let [p (params req)
          [energies L-values] (ensure-energies-L (parse-doubles (:energies p)) (parse-ints (:L_values p)))
          ws (ws-params-from p)
          radius (parse-double-default (:radius p) 3.0)
          angles (or (seq (parse-doubles (:angles p))) (range 0.0 181.0 10.0))]
      (let [dsigma-fn (or (resolve 'functions/differential-cross-section) (do (require 'functions) (resolve 'functions/differential-cross-section)))
            L-max (apply max L-values)
            elastic-data (for [E energies theta angles]
                           (let [theta-rad (* theta (/ Math/PI 180.0))
                                 dsigma-complex (if dsigma-fn (dsigma-fn E ws theta-rad L-max) 0.0)
                                 dsigma (if (number? dsigma-complex) dsigma-complex (c/mag dsigma-complex))]
                             {:energy E :angle theta :differential_cross_section dsigma}))]
        (response {:success true :data {:elastic elastic-data :parameters {:energies energies :L_values L-values :ws_params ws :radius radius :angles angles}}})))
    (catch Exception e (response {:success false :error (.getMessage e)}))))

(defn- handle-api-inelastic [req]
  (try
    (when-not (find-ns 'dwba.inelastic) (require 'dwba.inelastic))
    (let [inel (or (resolve 'dwba.inelastic/distorted-wave-entrance) (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/distorted-wave-entrance)))
          inel-exit (or (resolve 'dwba.inelastic/distorted-wave-exit) (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/distorted-wave-exit)))
          inel-cross (or (resolve 'dwba.inelastic/inelastic-cross-section) (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/inelastic-cross-section)))
          p (params req)
          [energies L-values] (ensure-energies-L (parse-doubles (:energies p)) (parse-ints (:L_values p)))
          lambdas (parse-lambdas p)
          ws (ws-params-from p)
          E-ex (parse-double-default (:E_ex p) 4.44)
          beta (parse-double-default (:beta p) 0.25)
          h 0.02, r-max 15.0, mu 0, mass-factor phys/mass-factor
          n-points (int (/ r-max h))
          transition-form-factor-fn (or (resolve 'dwba.inelastic/transition-form-factor) (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/transition-form-factor)))
          inelastic-amplitude-radial-fn (or (resolve 'dwba.inelastic/inelastic-amplitude-radial) (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/inelastic-amplitude-radial)))
          inelastic-dsigma-fn (or (resolve 'dwba.inelastic/inelastic-differential-cross-section) (do (require 'dwba.inelastic) (resolve 'dwba.inelastic/inelastic-differential-cross-section)))
          angular-factor (* 4.0 Math/PI)
          zero-complex (c/complex-cartesian 0.0 0.0)]
        (let [inelastic-data (for [lambda lambdas
                                  E-i    energies]
                               (try
                                 (let [V-transition-vec (when transition-form-factor-fn
                                                          (mapv (fn [i] (transition-form-factor-fn (* i h) lambda beta ws)) (range n-points)))
                                       T-sum (if (and V-transition-vec inelastic-amplitude-radial-fn)
                                               (reduce (fn [acc L-i]
                                                         (let [chi-i (inel E-i L-i ws h r-max)
                                                               chi-f (inel-exit E-i E-ex L-i ws h r-max)
                                                               T-radial (inelastic-amplitude-radial-fn chi-i chi-f V-transition-vec r-max h)
                                                               T-L (if (number? T-radial) (* angular-factor T-radial) (c/mul angular-factor T-radial))]
                                                           (c/add acc (if (number? T-L) (c/complex-cartesian T-L 0.0) T-L))))
                                                       zero-complex
                                                       L-values)
                                               nil)
                                       dsigma-fm2 (if T-sum
                                                    (let [k-i (Math/sqrt (* mass-factor E-i))
                                                          E-f (- E-i E-ex)
                                                          k-f (Math/sqrt (* mass-factor E-f))]
                                                      (inelastic-dsigma-fn T-sum k-i k-f E-i E-ex mass-factor))
                                                    (reduce + 0.0 (for [L-i L-values]
                                                                    (inel-cross (inel E-i L-i ws h r-max) (inel-exit E-i E-ex L-i ws h r-max)
                                                                                lambda mu beta ws E-i E-ex r-max h mass-factor))))
                                       ;; 1 mb = 10 fm² => dσ/dΩ (mb/sr) = dσ/dΩ (fm²/sr) * 0.1
                                       dsigma-mb (* 0.1 dsigma-fm2)]
                                   {:energy E-i :lambda lambda :excitation_energy E-ex :differential_cross_section dsigma-mb})
                                 (catch Exception e {:energy E-i :lambda lambda :excitation_energy E-ex :differential_cross_section 0.0 :error (.getMessage e)})))]
          (response {:success true :data {:inelastic inelastic-data :parameters {:energies energies :L_values L-values :lambdas lambdas :ws_params ws :E_ex E-ex :beta beta :h h :r_max r-max}}})))
    (catch Exception e (response {:success false :error (.getMessage e)}))))

(defn- handle-api-transfer [req]
  (try
    (when-not (find-ns 'dwba.transfer) (require 'dwba.transfer))
    (when-not (find-ns 'dwba.form-factors) (require 'dwba.form-factors))
    (let [zero-range-const (or (resolve 'dwba.transfer/zero-range-constant) (do (require 'dwba.transfer) (resolve 'dwba.transfer/zero-range-constant)))
          transfer-amp (or (resolve 'dwba.transfer/transfer-amplitude-zero-range) (do (require 'dwba.transfer) (resolve 'dwba.transfer/transfer-amplitude-zero-range)))
          transfer-dsigma-angular (or (resolve 'dwba.transfer/transfer-differential-cross-section-angular) (do (require 'dwba.transfer) (resolve 'dwba.transfer/transfer-differential-cross-section-angular)))
          solve-bound-state (or (resolve 'dwba.transfer/solve-bound-state) (do (require 'dwba.transfer) (resolve 'dwba.transfer/solve-bound-state)))
          normalized-overlap (or (resolve 'dwba.form-factors/normalized-overlap) (do (require 'dwba.form-factors) (resolve 'dwba.form-factors/normalized-overlap)))
          p (params req)
          [energies L-values] (ensure-energies-L (parse-doubles (:energies p)) (parse-ints (:L_values p)))
          ws (ws-params-from p)
          reaction-type (keyword (str (or (when-not (str/blank? (str (:reaction_type p))) (:reaction_type p)) "p-d")))
          S-factor 1.0, mass-factor phys/mass-factor, h 0.01, r-max 20.0
          l-i 1, l-f 0
          bound-i (solve-bound-state ws 1 1 nil r-max h)
          bound-f (solve-bound-state ws 1 0 nil r-max h)
          phi-i (:normalized-wavefunction bound-i)
          phi-f (:normalized-wavefunction bound-f)
          overlap-approx (normalized-overlap phi-i phi-f r-max h)
          D0 (zero-range-const reaction-type)
          angles-deg (range 20.0 181.0 20.0)]
      ;; DCS vs. angle: 20°–160° step 20° (exclude 0°, 90°, 180°); output in mb/sr (1 mb = 10 fm²)
      (let [fm2->mb 0.1
            transfer-data (for [E-i energies
                               theta-deg angles-deg]
                           (try
                             (let [E-f-approx (* 0.8 E-i)
                                   k-i (Math/sqrt (* mass-factor E-i))
                                   k-f (Math/sqrt (* mass-factor E-f-approx))
                                   T (transfer-amp overlap-approx D0)
                                   ;; (p,d) l-i=1 l-f=0 → only L=1
                                   T-amplitudes {1 T}
                                   theta-rad (* theta-deg (/ Math/PI 180.0))
                                   dsigma-fm2 (transfer-dsigma-angular T-amplitudes S-factor k-i k-f theta-rad
                                                                       mass-factor mass-factor 0.0 l-i l-f)]
                               {:energy E-i :angle theta-deg :differential_cross_section (* fm2->mb dsigma-fm2)})
                             (catch Exception e {:energy E-i :angle theta-deg :differential_cross_section 0.0 :error (.getMessage e)})))]
        (response {:success true :data {:transfer transfer-data :parameters {:energies energies :L_values L-values :ws_params ws :reaction_type (str reaction-type) :S_factor S-factor}}})))
    (catch Exception e (response {:success false :error (.getMessage e)}))))

(defn- handle-transfer-default [req]
  (try (transfer-default-response (str (or (query-param req "target") "16O")))
       (catch Exception e (response {:success false :error (.getMessage e)}))))

;; ---------------------------
;; Routes: build flat list and apply (avoids deep nesting / delimiter errors)
;; ---------------------------
(defn- route-list []
  (list
    (POST "/api/calculate" [req] (handle-api-calculate req))
    (POST "/api/elastic" [req] (handle-api-elastic req))
    (POST "/api/inelastic" [req] (handle-api-inelastic req))
    (POST "/api/transfer" [req] (handle-api-transfer req))
    (GET "/" [] (serve-index))
    (GET "/app.js" [] (serve-resource "app.js"))
    (GET "/js/dashboard.js" [] (serve-resource "js/dashboard.js"))
    (GET "/api/health" [] (response {:status "ok" :message "DWBA Web Dashboard API"}))
    (GET "/api/parameters" [] (response {:default_parameters {:energies (vec (map double (range 5 31))) :L_values [0 1 2 3 4 5]
                                                               :V0 40.0 :R0 2.0 :a0 0.6 :radius 3.0 :E_ex 4.44 :lambda 2 :beta 0.25 :reaction_type "p-d"}
                                         :parameter_ranges {:V0 {:min -100.0 :max 100.0 :step 1.0} :R0 {:min 0.5 :max 5.0 :step 0.1}
                                                            :a0 {:min 0.1 :max 2.0 :step 0.1} :radius {:min 1.0 :max 10.0 :step 0.1}
                                                            :E_ex {:min 0.0 :max 20.0 :step 0.1} :lambda {:min 1 :max 5 :step 1} :beta {:min 0.0 :max 1.0 :step 0.01}}}))
    (GET "/api/transfer-default" [req] (handle-transfer-default req))
    (OPTIONS "/api/health" [] (response nil))
    (OPTIONS "/api/calculate" [] (response nil))
    (OPTIONS "/api/parameters" [] (response nil))
    (OPTIONS "/api/elastic" [] (response nil))
    (OPTIONS "/api/inelastic" [] (response nil))
    (OPTIONS "/api/transfer" [] (response nil))
    (OPTIONS "/api/transfer-default" [] (response nil))
    (GET "/api/calculate" [] (assoc (response "Use POST to submit calculation") :status 405))
    (GET "/api/transfer" [] (assoc (response "Use POST for transfer calculation, GET /api/transfer-default for default (p,d) plot") :status 405))
    (route/files "/" {:root "public"})
    (route/not-found "Not Found")))

(def app
  (let [routes-handler (try (apply routes (route-list))
                           (catch Throwable t (fn [req] {:status 500 :body (str "Routes failed to load: " (.getMessage t)) :headers {"Content-Type" "text/plain"}})))]
    (-> routes-handler
      wrap-nil-response
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :options]
                 :access-control-allow-headers ["Content-Type"]))))

;; Server
(defn start-server [port]
  (jetty/run-jetty app {:port port :join? false}))

(defn -main [& args]
  (let [port (or (some-> args first Integer/parseInt) 3000)]
    (println (str "Starting DWBA Web Dashboard on port " port))
    (start-server port)))
