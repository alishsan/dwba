#!/usr/bin/env clojure

;; Simple Theoretical vs Experimental Comparison
;; Without fastmath dependency for now

;; Experimental data from EXFOR D0557
(def experimental-data
  {:entry "D0557"
   :title "Elastic scattering cross section of proton from helium at 165°"
   :reaction "p + α → p + α"
   :angle-lab 165.0  ; degrees (laboratory frame)
   :data-points [
     {:energy-lab 1.6 :cross-section 0.193 :unit "b/sr"}
     {:energy-lab 1.7 :cross-section 0.196 :unit "b/sr"}
     {:energy-lab 1.8 :cross-section 0.211 :unit "b/sr"}
     {:energy-lab 1.9 :cross-section 0.237 :unit "b/sr"}
     {:energy-lab 2.0 :cross-section 0.254 :unit "b/sr"}
     {:energy-lab 2.1 :cross-section 0.263 :unit "b/sr"}
     {:energy-lab 2.2 :cross-section 0.271 :unit "b/sr"}
     {:energy-lab 2.3 :cross-section 0.269 :unit "b/sr"}
     {:energy-lab 2.4 :cross-section 0.251 :unit "b/sr"}
     {:energy-lab 2.5 :cross-section 0.229 :unit "b/sr"}
     {:energy-lab 2.6 :cross-section 0.213 :unit "b/sr"}
     {:energy-lab 2.7 :cross-section 0.193 :unit "b/sr"}
     {:energy-lab 2.8 :cross-section 0.174 :unit "b/sr"}
     {:energy-lab 2.9 :cross-section 0.170 :unit "b/sr"}
     {:energy-lab 3.0 :cross-section 0.160 :unit "b/sr"}
     {:energy-lab 3.2 :cross-section 0.136 :unit "b/sr"}
     {:energy-lab 3.4 :cross-section 0.127 :unit "b/sr"}
     {:energy-lab 3.6 :cross-section 0.117 :unit "b/sr"}
   ]
})

;; Physical constants
(def hbarc 197.7)  ; MeV·fm
(def mp 938.272)   ; proton mass (MeV/c²)
(def mAlpha 3727.379)  ; alpha mass (MeV/c²)

;; Reduced mass for p + α system
(def mu (/ (* mp mAlpha) (+ mp mAlpha)))  ; ≈ 745 MeV/c²
(def mass-factor (/ (* 2 mu) hbarc hbarc))

;; Simple theoretical cross-section calculation (placeholder)
;; This is a simplified version - in reality we'd use the full DWBA calculation
(defn simple-theoretical-cross-section [E-lab]
  "Simplified theoretical cross-section calculation"
  (let [E-cm (* E-lab 0.8)  ; Rough CM energy conversion
        k (Math/sqrt (* mass-factor E-cm))
        ;; Simplified cross-section formula
        sigma (* (/ 1.0 (* k k)) 1e28)]  ; Convert to barns
    sigma))

;; Generate comparison
(println "=== Simple Theoretical vs Experimental Comparison ===")
(println "EXFOR Entry:" (:entry experimental-data))
(println "Title:" (:title experimental-data))
(println "Reaction:" (:reaction experimental-data))
(println "Lab Angle:" (:angle-lab experimental-data) "°")
(println "Energy Range:" (str (first (map :energy-lab (:data-points experimental-data))) " - " (last (map :energy-lab (:data-points experimental-data))) " MeV"))

(println "\n=== Physical Constants ===")
(println "Proton mass:" mp "MeV/c²")
(println "Alpha mass:" mAlpha "MeV/c²")
(println "Reduced mass:" mu "MeV/c²")
(println "Mass factor:" mass-factor)

(println "\n=== Simple Theoretical vs Experimental Comparison ===")
(println "Energy (Lab)\tTheoretical\tExperimental\tRatio")
(doseq [point (:data-points experimental-data)]
  (let [E-lab (:energy-lab point)
        theoretical (simple-theoretical-cross-section E-lab)
        experimental (:cross-section point)
        ratio (/ theoretical experimental)]
    (printf "%.1f\t\t%.6f\t\t%.3f\t\t%.3f\n" 
            E-lab theoretical experimental ratio)))

(println "\n=== Note ===")
(println "This is a simplified calculation for demonstration.")
(println "For proper validation, we need to:")
(println "1. Fix the fastmath loading issue")
(println "2. Use the full DWBA calculation functions")
(println "3. Apply proper kinematic conversions")
(println "4. Use correct differential cross-section formulas")

(println "\n🔬 This shows the basic structure for theoretical vs experimental comparison!")
(println "📊 Once fastmath is working, we can use the full DWBA calculations!")

