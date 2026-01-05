(ns dwba.dwba-calculations-test
  (:require [clojure.test :refer :all]
            [fastmath.core :as m]
            [fastmath.special :as spec]
            [fastmath.complex :as cplx]
            [complex :refer :all]
            [functions :refer :all]))

(deftest woods-saxon-potential-test
  (testing "Woods-Saxon potential calculation"
    (let [ws-params [40.0 2.0 0.6]
          potential-at-r0 (WS 2.0 ws-params)]
      (println "Woods-Saxon at r=2.0 fm:" potential-at-r0 "MeV")
      (is (number? potential-at-r0))
      (is (< potential-at-r0 0))  ; Should be negative
      (is (> (Math/abs potential-at-r0) 0)))))

(deftest r-matrix-calculation-test
  (testing "R-matrix calculation"
    (let [ws-params [40.0 2.0 0.6]
          E-cm 1.5
          r-matrix-val (r-matrix-a E-cm ws-params 3.0 0)]
      (println "R-matrix at E_cm=1.5 MeV:" r-matrix-val)
      (is (number? r-matrix-val))
      (is (not (Double/isNaN r-matrix-val))))))

(deftest s-matrix-calculation-test
  (testing "S-matrix calculation"
    (let [ws-params [40.0 2.0 0.6]
          E-cm 1.5
          s-matrix-val (s-matrix0 E-cm ws-params 3.0 0)]
      (println "S-matrix at E_cm=1.5 MeV:" s-matrix-val)
      (is (complex? s-matrix-val))  ; S-matrix should be complex
      (is (number? (re s-matrix-val)))  ; Real part should be a number
      (is (number? (im s-matrix-val)))  ; Imaginary part should be a number
      (is (not (Double/isNaN (re s-matrix-val))))
      (is (not (Double/isNaN (im s-matrix-val)))))))

(deftest phase-shift-calculation-test
  (testing "Phase shift calculation"
    (let [ws-params [40.0 2.0 0.6]
          E-cm 1.5
          phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)]
      (println "Phase shift at E_cm=1.5 MeV:" phase-shift-val "radians")
      (is (number? phase-shift-val))
      (is (not (Double/isNaN phase-shift-val))))))

(deftest kinematic-conversion-test
  (testing "Kinematic conversions"
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
                       (Math/acos (m/signum numerator))))]
      (println "Lab energy:" E-lab "MeV")
      (println "CM energy:" E-cm "MeV")
      (println "Lab angle:" theta-lab "degrees")
      (println "CM angle:" (* theta-cm (/ 180 Math/PI)) "degrees")
      (is (number? E-cm))
      (is (number? theta-cm))
      (is (> E-cm 0))
      (is (> theta-cm 0)))))

(deftest cross-section-calculation-test
  (testing "Cross-section calculation"
    (let [E-lab 2.0
          E-cm (* E-lab (/ 3727.379 (+ 938.272 3727.379)))
          ws-params [40.0 2.0 0.6]
          phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)
          k (Math/sqrt (* (/ (* 2 745) 197.7 197.7) E-cm))
          sigma (* (/ 1.0 (* k k)) 
                   (Math/sin phase-shift-val)
                   (Math/sin phase-shift-val)
                   1e28)]
      (println "Cross-section at E_lab=2.0 MeV:" sigma "b/sr")
      (is (number? sigma))
      (is (> sigma 0))
      (is (not (Double/isNaN sigma))))))
