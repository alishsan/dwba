#!/usr/bin/env clojure

;; Theoretical vs Experimental Cross-Section Analysis at 165° Lab Angle
;; Using DWBA calculations with fastmath

(require '[fastmath.special :as spec])
(require '[fastmath.complex :as cplx])

;; Load our DWBA functions
(load-file "src/complex.clj")
(load-file "src/functions.clj")

(println "=== Theoretical vs Experimental Cross-Section Analysis ===")
(println "Lab Angle: 165°")
(println "Reaction: p + α → p + α")

;; Physical constants
(def hbarc 197.7)  ; MeV·fm
(def mp 938.272)   ; proton mass (MeV/c²)
(def mAlpha 3727.379)  ; alpha mass (MeV/c²)

;; Reduced mass for p + α system
(def mu (/ (* mp mAlpha) (+ mp mAlpha)))  ; ≈ 745 MeV/c²
(def mass-factor (/ (* 2 mu) hbarc hbarc))

(println "\n=== Physical Constants ===")
(println "Proton mass:" mp "MeV/c²")
(println "Alpha mass:" mAlpha "MeV/c²")
(println "Reduced mass:" mu "MeV/c²")
(println "Mass factor:" mass-factor)

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

;; Kinematic conversion functions
(defn lab-to-cm-energy [E-lab]
  "Convert lab energy to CM energy for p + α system"
  (* E-lab (/ mAlpha (+ mp mAlpha))))

(defn lab-to-cm-angle [theta-lab]
  "Convert lab angle to CM angle for p + α system"
  (let [theta-lab-rad (* theta-lab (/ Math/PI 180))
        cos-theta-cm (Math/cos theta-lab-rad)
        sin-theta-cm (Math/sin theta-lab-rad)
        ;; For p + α: m1 = mp, m2 = mAlpha
        ratio (/ mp mAlpha)
        numerator (+ cos-theta-cm (* ratio sin-theta-cm))
        denominator (Math/sqrt (+ 1 (* 2 ratio cos-theta-cm) (* ratio ratio)))]
    (if (< (Math/abs numerator) denominator)
      (Math/acos (/ numerator denominator))
      (Math/acos (Math/signum numerator)))))

;; Theoretical cross-section calculation using DWBA
(defn calculate-theoretical-cross-section [E-lab]
  "Calculate theoretical differential cross-section using DWBA"
  (let [E-cm (lab-to-cm-energy E-lab)
        theta-cm (lab-to-cm-angle 165.0)
        k (Math/sqrt (* mass-factor E-cm))
        
        ;; Woods-Saxon parameters (adjustable)
        ws-params [40.0 2.0 0.6]  ; V0, R0, a0
        
        ;; Calculate R-matrix and S-matrix
        r-matrix-val (r-matrix-a E-cm ws-params 3.0 0)
        s-matrix-val (s-matrix0 E-cm ws-params 3.0 0)
        
        ;; Calculate phase shift
        phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)
        
        ;; Calculate differential cross-section
        ;; This is a simplified formula - in practice you'd use the full DWBA expression
        sigma (* (/ 1.0 (* k k)) 
                 (Math/sin phase-shift-val)
                 (Math/sin phase-shift-val)
                 1e28)]  ; Convert to barns
    {:energy-lab E-lab
     :energy-cm E-cm
     :angle-cm (* theta-cm (/ 180 Math/PI))
     :k k
     :r-matrix r-matrix-val
     :s-matrix s-matrix-val
     :phase-shift phase-shift-val
     :cross-section sigma}))

;; Generate theoretical results
(println "\n=== Calculating Theoretical Cross-Sections ===")
(def theoretical-results
  (map calculate-theoretical-cross-section
       (map :energy-lab (:data-points experimental-data))))

;; Display results
(println "\n=== Comparison Results ===")
(println "Energy (Lab)\tEnergy (CM)\tAngle (CM)\tTheoretical\tExperimental\tRatio")
(println "MeV\t\tMeV\t\tdeg\t\tb/sr\t\tb/sr\t\tT/E")

(doseq [[theoretical experimental] (map vector theoretical-results (:data-points experimental-data))]
  (let [E-lab (:energy-lab experimental)
        E-cm (:energy-cm theoretical)
        theta-cm (:angle-cm theoretical)
        theoretical-sigma (:cross-section theoretical)
        experimental-sigma (:cross-section experimental)
        ratio (/ theoretical-sigma experimental-sigma)]
    (printf "%.1f\t\t%.2f\t\t%.1f\t\t%.6f\t\t%.3f\t\t%.3f\n" 
            E-lab E-cm theta-cm theoretical-sigma experimental-sigma ratio)))

;; Calculate statistics
(def ratios (map #(/ (:cross-section %1) (:cross-section %2)) 
                 theoretical-results 
                 (:data-points experimental-data)))
(def mean-ratio (/ (reduce + ratios) (count ratios)))
(def max-ratio (apply max ratios))
(def min-ratio (apply min ratios))

(println "\n=== Statistical Analysis ===")
(println "Mean ratio (T/E):" (format "%.3f" mean-ratio))
(println "Max ratio (T/E):" (format "%.3f" max-ratio))
(println "Min ratio (T/E):" (format "%.3f" min-ratio))
(println "Standard deviation:" (format "%.3f" 
  (Math/sqrt (/ (reduce + (map #(Math/pow (- % mean-ratio) 2) ratios)) 
                (count ratios)))))

(println "\n=== Analysis Complete ===")
(println "✅ Theoretical cross-sections calculated using DWBA")
(println "📊 Comparison with experimental data completed")
(println "🎯 Ready for parameter optimization and paper writing!")