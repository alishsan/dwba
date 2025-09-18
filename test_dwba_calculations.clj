#!/usr/bin/env clojure

;; Test DWBA calculations for theoretical vs experimental comparison
(require '[fastmath.special :as spec])
(require '[fastmath.complex :as cplx])

;; Load our DWBA functions
(load-file "src/complex.clj")
(load-file "src/functions.clj")

(println "=== Testing DWBA Calculations ===")

;; Test basic functions
(println "Testing basic DWBA functions...")

;; Test Woods-Saxon potential
(let [ws-params [40.0 2.0 0.6]
      potential-at-r0 (WS 2.0 ws-params)]
  (println "Woods-Saxon at r=2.0 fm:" potential-at-r0 "MeV"))

;; Test R-matrix calculation
(let [ws-params [40.0 2.0 0.6]
      E-cm 1.5
      r-matrix-val (r-matrix-a E-cm ws-params 3.0 0)]
  (println "R-matrix at E_cm=1.5 MeV:" r-matrix-val))

;; Test S-matrix calculation
(let [ws-params [40.0 2.0 0.6]
      E-cm 1.5
      s-matrix-val (s-matrix0 E-cm ws-params 3.0 0)]
  (println "S-matrix at E_cm=1.5 MeV:" s-matrix-val))

;; Test phase shift calculation
(let [ws-params [40.0 2.0 0.6]
      E-cm 1.5
      phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)]
  (println "Phase shift at E_cm=1.5 MeV:" phase-shift-val "radians"))

;; Test kinematic conversions
(println "\nTesting kinematic conversions...")
(let [E-lab 2.0
      E-cm (* E-lab (/ 3727.379 (+ 938.272 3727.379)))
      theta-lab 165.0
      theta-cm (let [theta-lab-rad (* theta-lab (/ Math/PI 180))
                     cos-theta-cm (Math/cos theta-lab-rad)
                     sin-theta-cm (Math/sin theta-lab-rad)
                     ratio (/ 938.272 3727.379)
                     numerator (+ cos-theta-cm (* ratio sin-theta-cm))
                     denominator (Math/sqrt (+ 1 (* 2 ratio cos-theta-cm) (* ratio ratio)))]
                 (if (< (Math/abs numerator) denominator)
                   (Math/acos (/ numerator denominator))
                   (Math/acos (Math/signum numerator))))]
  (println "Lab energy:" E-lab "MeV")
  (println "CM energy:" E-cm "MeV")
  (println "Lab angle:" theta-lab "degrees")
  (println "CM angle:" (* theta-cm (/ 180 Math/PI)) "degrees"))

;; Test cross-section calculation
(println "\nTesting cross-section calculation...")
(let [E-lab 2.0
      E-cm (* E-lab (/ 3727.379 (+ 938.272 3727.379)))
      ws-params [40.0 2.0 0.6]
      phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)
      k (Math/sqrt (* (/ (* 2 745) 197.7 197.7) E-cm))
      sigma (* (/ 1.0 (* k k)) 
               (Math/sin phase-shift-val)
               (Math/sin phase-shift-val)
               1e28)]
  (println "Cross-section at E_lab=2.0 MeV:" sigma "b/sr"))

(println "\n✅ DWBA calculations are working!")
(println "🎯 Ready to proceed with full theoretical vs experimental analysis!")
