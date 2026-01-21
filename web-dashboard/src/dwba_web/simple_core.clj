(ns dwba-web.simple-core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response content-type]]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]))

;; Simple DWBA calculation functions (self-contained)
(def hbarc 197.7)
(def mu 745)
(def mass-factor (/ (* 2 mu) hbarc hbarc))
(def Z1Z2ee (* 2 1.44))

(defn WS [r [V0 R0 a0]]
  (/ (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0)))))

(defn Coulomb-pot [r r0]
  (if (> r r0) (/ Z1Z2ee r) (* r (/ Z1Z2ee r0 r0))))

(defn r-matrix-nuclear-only [E V a L]
  (let [dr 0.001
        N (int (/ a dr))]
    (loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr n 0]
      (if (>= n N)
        (/ ur dudr)
        (let [new-pot (WS x V)
              new-d2udr2 (* (+ (/ (* L (inc L)) (* x x)) (* mass-factor (- new-pot E))) ur)
              new-dudr (+ dudr (* new-d2udr2 dr))
              new-ur (+ ur (* new-dudr dr))]
          (recur (+ x dr) new-pot new-d2udr2 new-dudr new-ur (inc n)))))))

(defn r-matrix-coulomb-nuclear [E V a L]
  (let [dr 0.001
        N (int (/ a dr))
        R0 (second V)]
    (loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr n 0]
      (if (>= n N)
        (/ ur dudr a)
        (let [new-pot (+ (Coulomb-pot x R0) (WS x V))
              new-d2udr2 (* (+ (/ (* L (inc L)) (* x x)) (* mass-factor (- new-pot E))) ur)
              new-dudr (+ dudr (* new-d2udr2 dr))
              new-ur (+ ur (* new-dudr dr))]
          (recur (+ x dr) new-pot new-d2udr2 new-dudr new-ur (inc n)))))))

(defn nuclear-phase-shift [E V a L]
  (let [R-coulomb-nuclear (r-matrix-coulomb-nuclear E V a L)
        R-coulomb-only (r-matrix-nuclear-only E [0 2.0 0.6] a L)
        R-nuclear (- R-coulomb-nuclear R-coulomb-only)
        phase-shift (Math/atan (/ R-nuclear 1.0))]
    phase-shift))

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
        
        ;; Calculate datasets (avoid inline defs)
        (let [phase-shift-data
              (for [E energies
                    L L-values]
                {:energy E 
                 :L L 
                 :phase_shift (nuclear-phase-shift E ws-params radius L)})
              
              r-matrix-data
              (for [E energies
                    L L-values]
                {:energy E 
                 :L L 
                 :r_nuclear (r-matrix-nuclear-only E ws-params radius L)
                 :r_coulomb_nuclear (r-matrix-coulomb-nuclear E ws-params radius L)})
              
              potential-data
              (let [radii (range 0.1 10.0 0.1)]
                (for [r radii]
                  {:radius r 
                   :woods_saxon (WS r ws-params)
                   :coulomb (Coulomb-pot r (second ws-params))
                   :combined (+ (WS r ws-params) (Coulomb-pot r (second ws-params)))}))
              
              cross-section-data
              (for [E energies]
                {:energy E 
                 :total_cross_section (reduce + (map #(Math/pow (Math/sin (nuclear-phase-shift E ws-params radius %)) 2) L-values))})]
          
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
  
  (GET "/api/parameters" []
    (response {:default_parameters
               {:energies [5.0 10.0 15.0 20.0 25.0 30.0]
                :L_values [0 1 2 3 4 5]
                :V0 40.0
                :R0 2.0
                :a0 0.6
                :radius 3.0}
               :parameter_ranges
               {:V0 {:min -100.0 :max 100.0 :step 1.0}
                :R0 {:min 0.5 :max 5.0 :step 0.1}
                :a0 {:min 0.1 :max 2.0 :step 0.1}
                :radius {:min 1.0 :max 10.0 :step 0.1}}}))
  
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
