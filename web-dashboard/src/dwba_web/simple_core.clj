(ns dwba-web.simple-core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response content-type]]
            [clojure.java.io :as io]
            [functions :as phys]           ;; use core calculations from main project
            ;; Don't require inelastic/transfer at startup - load lazily when needed
            [complex :as c]                 ;; complex numbers
            [ring.adapter.jetty :as jetty]))

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

;; Web routes
(defroutes app-routes
  ;; Serve static files
  (GET "/" [] (serve-index))
  
  ;; API endpoints
  (GET "/api/health" [] 
    (response {:status "ok" :message "DWBA Web Dashboard API"}))
  
  (POST "/api/calculate" req
    (try
      ;; Prefer parsed JSON body (from wrap-json-body), fall back to params map
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
        
        ;; Calculate datasets with caching to avoid redundant calculations
        (let [;; Cache phase shifts to avoid recalculating
              phase-shift-cache (into {} 
                                     (for [E energies
                                           L L-values]
                                       [[E L] (phys/phase-shift E ws-params radius L)]))
              
              phase-shift-data
              (for [E energies
                    L L-values]
                {:energy E 
                 :L L 
                 :phase_shift (get phase-shift-cache [E L])})
              
              r-matrix-data
              (for [E energies
                    L L-values]
                {:energy E 
                 :L L 
                 :r_nuclear (phys/r-matrix-a E ws-params radius L)
                 :r_coulomb_nuclear (phys/r-matrix E ws-params radius L)})
              
              ;; Reduce potential data points for faster calculation (0.2 fm steps instead of 0.1)
              potential-data
              (let [radii (range 0.1 10.0 0.2)]
                (for [r radii]
                  {:radius r 
                   :woods_saxon (phys/WS r ws-params)
                   :coulomb (phys/Coulomb-pot r (second ws-params))
                   :combined (+ (phys/WS r ws-params) (phys/Coulomb-pot r (second ws-params)))}))
              
              ;; Use cached phase shifts for cross-section calculation
              cross-section-data
              (for [E energies]
                {:energy E 
                 :total_cross_section (reduce + (map #(Math/pow (Math/sin (get phase-shift-cache [E %])) 2) L-values))})]
          
          (response {:success true
                     :data {:phase_shifts phase-shift-data
                            :r_matrices r-matrix-data
                            :potentials potential-data
                            :cross_sections cross-section-data
                            :parameters {:energies energies
                                         :L_values L-values
                                         :ws_params ws-params
                                         :radius radius}}})))
      (catch Exception e
        (response {:success false
                   :error (.getMessage e)}))))
  
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
            ws-params     [(Double/parseDouble (str (:V0 params)))
                           (Double/parseDouble (str (:R0 params)))
                           (Double/parseDouble (str (:a0 params)))]
            E-ex          (Double/parseDouble (str (:E_ex params)))
            lambda        (Integer/parseInt (str (:lambda params)))
            beta          (Double/parseDouble (str (:beta params)))
            h             0.01  ; Fixed step size
            r-max         20.0  ; Fixed max radius
            mu            0     ; Magnetic quantum number (default 0)
            mass-factor   phys/mass-factor]
        
        (when (or (empty? energies) (empty? L-values))
          (throw (ex-info "Missing energies or L_values" {:energies energies :L_values L-values})))
        
        (let [inelastic-data
              (for [E-i energies
                    L-i L-values]
                (try
                  (let [_E-f (- E-i E-ex)  ; Exit channel energy (calculated but not directly used)
                        chi-i (inel E-i L-i ws-params h r-max)
                        chi-f (inel-exit E-i E-ex L-i ws-params h r-max)
                        dsigma (inel-cross chi-i chi-f lambda mu beta ws-params E-i E-ex r-max h mass-factor)]
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
                                         :beta beta}}})))
      (catch Exception e
        (response {:success false
                   :error (.getMessage e)}))))
  
  ;; Transfer reaction endpoint
  (POST "/api/transfer" req
    (try
      ;; Lazy load transfer namespace only when needed (cache after first load)
      (when-not (find-ns 'dwba.transfer)
        (require 'dwba.transfer))
      ;; Cache resolved functions to avoid repeated resolution
      (let [zero-range-const (or (resolve 'dwba.transfer/zero-range-constant)
                                  (do (require 'dwba.transfer) (resolve 'dwba.transfer/zero-range-constant)))
            transfer-amp (or (resolve 'dwba.transfer/transfer-amplitude-zero-range)
                              (do (require 'dwba.transfer) (resolve 'dwba.transfer/transfer-amplitude-zero-range)))
            transfer-dsigma (or (resolve 'dwba.transfer/transfer-differential-cross-section)
                                 (do (require 'dwba.transfer) (resolve 'dwba.transfer/transfer-differential-cross-section)))
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
            ;; For zero-range, we need overlap integral - use a simple approximation
            ;; In real calculations, this would come from bound state wavefunctions
            overlap-approx 1.0]  ; Placeholder - should be calculated from bound states
        
        (when (or (empty? energies) (empty? L-values))
          (throw (ex-info "Missing energies or L_values" {:energies energies :L_values L-values})))
        
        (let [D0 (zero-range-const reaction-type)
              transfer-data
              (for [E-i energies
                    L L-values]
                (try
                  (let [E-f-approx (* 0.8 E-i)  ; Approximate exit energy (should be calculated from Q-value)
                        k-i (Math/sqrt (* mass-factor E-i))
                        k-f (Math/sqrt (* mass-factor E-f-approx))
                        T-amplitude (transfer-amp overlap-approx D0)
                        dsigma (transfer-dsigma T-amplitude S-factor k-i k-f mass-factor)]
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
                   :error (.getMessage e)}))))
  
  (GET "/api/parameters" []
    (response {:default_parameters
               {:energies [5.0 10.0 15.0 20.0 25.0 30.0]
                :L_values [0 1 2 3 4 5]
                :V0 40.0
                :R0 2.0
                :a0 0.6
                :radius 3.0
                :E_ex 4.44
                :lambda 2
                :beta 0.25
                :reaction_type "d-p"}
               :parameter_ranges
               {:V0 {:min -100.0 :max 100.0 :step 1.0}
                :R0 {:min 0.5 :max 5.0 :step 0.1}
                :a0 {:min 0.1 :max 2.0 :step 0.1}
                :radius {:min 1.0 :max 10.0 :step 0.1}
                :E_ex {:min 0.0 :max 20.0 :step 0.1}
                :lambda {:min 1 :max 5 :step 1}
                :beta {:min 0.0 :max 1.0 :step 0.01}}}))
  
  ;; Fallback for static files
  (route/files "/" {:root "public"})
  (route/not-found "Not Found"))

;; Middleware
(def app
  (-> app-routes
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
