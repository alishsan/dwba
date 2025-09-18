#!/usr/bin/env clojure

;; Debug kinematic conversion step by step

;; Physical constants
(def mp 938.272)   ; proton mass (MeV/c²)
(def mAlpha 3727.379)  ; alpha mass (MeV/c²)

;; Check the conversion step by step
(defn debug-lab-to-cm-conversion [theta-lab m1 m2]
  (let [ratio (/ m2 m1)
        cos-theta-lab (Math/cos theta-lab)
        sin-theta-lab (Math/sin theta-lab)
        discriminant (- 1.0 (* ratio ratio (Math/pow sin-theta-lab 2)))
        cos-theta-cm-num (+ cos-theta-lab ratio)
        cos-theta-cm-denom (Math/sqrt (+ 1.0 (* 2 ratio cos-theta-lab) (* ratio ratio)))
        cos-theta-cm (/ cos-theta-cm-num cos-theta-cm-denom)]
    
    (println "=== Debug Lab-to-CM Conversion ===")
    (println "Mass ratio (m2/m1):" ratio)
    (println "Lab angle (rad):" theta-lab)
    (println "Lab angle (deg):" (* theta-lab 180.0 Math/PI))
    (println "cos(theta_lab):" cos-theta-lab)
    (println "sin(theta_lab):" sin-theta-lab)
    (println "Discriminant:" discriminant)
    (println "cos(theta_cm) numerator:" cos-theta-cm-num)
    (println "cos(theta_cm) denominator:" cos-theta-cm-denom)
    (println "cos(theta_cm):" cos-theta-cm)
    (println "Is cos(theta_cm) valid?" (and (>= cos-theta-cm -1.0) (<= cos-theta-cm 1.0)))
    
    (if (and (>= cos-theta-cm -1.0) (<= cos-theta-cm 1.0))
      (let [theta-cm (Math/acos cos-theta-cm)]
        (println "CM angle (rad):" theta-cm)
        (println "CM angle (deg):" (* theta-cm 180.0 Math/PI))
        theta-cm)
      (do
        (println "❌ Invalid cos(theta_cm) - angle not accessible")
        nil))))

;; Test with 165° lab angle
(def theta-lab-165 (* 165.0 Math/PI 180.0))
(def result (debug-lab-to-cm-conversion theta-lab-165 mp mAlpha))

(println "\n=== Alternative Approach ===")
(println "Let's try the reverse: what lab angle gives us a reasonable CM angle?")

;; Try different lab angles to see what works
(def test-angles [90.0 120.0 135.0 150.0 160.0 170.0 180.0])
(println "Lab Angle (deg)\tCM Angle (deg)\tAccessible?")
(doseq [angle-deg test-angles]
  (let [angle-rad (* angle-deg Math/PI 180.0)
        result (debug-lab-to-cm-conversion angle-rad mp mAlpha)]
    (if result
      (printf "%.1f\t\t%.1f\t\tYES\n" angle-deg (* result 180.0 Math/PI))
      (printf "%.1f\t\tN/A\t\tNO\n" angle-deg))))

(println "\n=== Key Insight ===")
(println "The issue might be that 165° lab angle is beyond the kinematic limit")
(println "for p + α scattering. Let's check what the maximum accessible angle is.")

;; Calculate maximum accessible lab angle
(def max-lab-angle (Math/asin (/ 1.0 (/ mAlpha mp))))
(println "Maximum accessible lab angle:" (* max-lab-angle 180.0 Math/PI) "°")
(println "Experimental angle:" 165.0 "°")
(println "Is 165° accessible?" (if (< 165.0 (* max-lab-angle 180.0 Math/PI)) "YES" "NO"))

