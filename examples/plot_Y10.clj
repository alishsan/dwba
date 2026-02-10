;; Plot Y_10(θ, 0) vs angle (20°, 40°, ..., 160°).
;; Run from project root: clj -M:example -e "(load-file \"examples/plot_Y10.clj\")"
;; Or ensure classpath has dwba, fastmath, complex, incanter, clojure.java.io.

(ns examples.plot-Y10
  (:require [dwba.transfer :as t]
            [incanter.core :as i]
            [incanter.charts :as c]
            [clojure.java.io :as io]
            [complex :as cx :refer [re im]]))

(def angles-deg (vec (range 20.0 181.0 20.0)))  ;; 20, 40, ..., 160, 180

(defn deg->rad [deg]
  (* deg (/ Math/PI 180.0)))

(defn Y10-real [theta-rad]
  ;; Y_10(θ, φ=0) is real: Y_10 = sqrt(3/(4π)) cos(θ)
  (let [y (t/spherical-harmonic 1 0 theta-rad 0.0)]
    (if (number? y) y (re y))))

(def angles-rad (mapv deg->rad angles-deg))
(def Y10-values (mapv Y10-real angles-rad))

(println "Y_10(θ, 0) at angles (deg):")
(doseq [[deg val] (map vector angles-deg Y10-values)]
  (println (format "  θ = %5.1f°  Y_10 = %+.6f" deg val)))

(try
  (let [_ (io/make-parents (io/file "output/plot_Y10.png"))
        chart (c/xy-plot (vec angles-deg) (vec Y10-values)
                         :title "Y_10(θ, 0) vs angle"
                         :x-label "θ (deg)"
                         :y-label "Y_10")]
    (i/save chart "output/plot_Y10.png" :width 800 :height 500)
    (println "Plot saved: output/plot_Y10.png"))
  (catch Exception e
    (println (format "Could not save plot: %s" (.getMessage e)))))
