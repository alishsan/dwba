#!/usr/bin/env clojure

;; Test to verify which k-l implementation is correct
;; Comparing our k-l with spec/spherical-bessel-k

(require '[functions :refer :all])
(require '[fastmath.special :as spec])

(println "=== Verification of Modified Spherical Bessel Function k_l(x) ===\n")

;; Known formulas for modified spherical Bessel functions:
;; k_0(x) = e^(-x) / x
;; k_1(x) = e^(-x) * (1 + 1/x) / x = e^(-x) * (x + 1) / x^2

(println "Test 1: k_0(1)")
(let [x 1.0
      our-k0 (k-l 0 x)
      fastmath-k0 (spec/spherical-bessel-k 0 x)
      expected-k0 (/ (Math/exp (- x)) x)]
  (println (format "  Our k-l(0, %.1f):     %.10f" x our-k0))
  (println (format "  Fastmath k(0, %.1f):  %.10f" x fastmath-k0))
  (println (format "  Expected e^(-x)/x:   %.10f" expected-k0))
  (println (format "  Our matches expected: %s" (< (Math/abs (- our-k0 expected-k0)) 1e-10)))
  (println (format "  Fastmath matches:     %s" (< (Math/abs (- fastmath-k0 expected-k0)) 1e-10)))
  (println ""))

(println "Test 2: k_1(1)")
(let [x 1.0
      our-k1 (k-l 1 x)
      fastmath-k1 (spec/spherical-bessel-k 1 x)
      expected-k1 (* (/ (Math/exp (- x)) x) (+ 1 (/ 1 x)))]
  (println (format "  Our k-l(1, %.1f):     %.10f" x our-k1))
  (println (format "  Fastmath k(1, %.1f):  %.10f" x fastmath-k1))
  (println (format "  Expected formula:     %.10f" expected-k1))
  (println (format "  Our matches expected: %s" (< (Math/abs (- our-k1 expected-k1)) 1e-10)))
  (println (format "  Fastmath matches:     %s" (< (Math/abs (- fastmath-k1 expected-k1)) 1e-10)))
  (println ""))

(println "Test 3: k_0(2)")
(let [x 2.0
      our-k0 (k-l 0 x)
      fastmath-k0 (spec/spherical-bessel-k 0 x)
      expected-k0 (/ (Math/exp (- x)) x)]
  (println (format "  Our k-l(0, %.1f):     %.10f" x our-k0))
  (println (format "  Fastmath k(0, %.1f):  %.10f" x fastmath-k0))
  (println (format "  Expected e^(-x)/x:   %.10f" expected-k0))
  (println (format "  Our matches expected: %s" (< (Math/abs (- our-k0 expected-k0)) 1e-10)))
  (println (format "  Fastmath matches:     %s" (< (Math/abs (- fastmath-k0 expected-k0)) 1e-10)))
  (println ""))

(println "Test 4: k_1(2)")
(let [x 2.0
      our-k1 (k-l 1 x)
      fastmath-k1 (spec/spherical-bessel-k 1 x)
      expected-k1 (* (/ (Math/exp (- x)) x) (+ 1 (/ 1 x)))]
  (println (format "  Our k-l(1, %.1f):     %.10f" x our-k1))
  (println (format "  Fastmath k(1, %.1f):  %.10f" x fastmath-k1))
  (println (format "  Expected formula:     %.10f" expected-k1))
  (println (format "  Our matches expected: %s" (< (Math/abs (- our-k1 expected-k1)) 1e-10)))
  (println (format "  Fastmath matches:     %s" (< (Math/abs (- fastmath-k1 expected-k1)) 1e-10)))
  (println ""))

(println "Test 5: Relationship with cylindrical Bessel K")
(println "  k_l(x) = sqrt(π/(2x)) * K_{l+1/2}(x)")
(let [x 1.0
      l 1
      our-k1 (k-l l x)
      cylindrical-k (* (Math/sqrt (/ Math/PI (* 2.0 x))) (spec/bessel-K (+ l 0.5) x))
      fastmath-k1 (spec/spherical-bessel-k l x)]
  (println (format "  Our k-l(%d, %.1f):              %.10f" l x our-k1))
  (println (format "  sqrt(π/(2x)) * K_{%.1f}(%.1f):    %.10f" (+ l 0.5) x cylindrical-k))
  (println (format "  Fastmath k(%d, %.1f):             %.10f" l x fastmath-k1))
  (println (format "  Our matches cylindrical formula: %s" (< (Math/abs (- our-k1 cylindrical-k)) 1e-8)))
  (println (format "  Fastmath matches cylindrical:     %s" (< (Math/abs (- fastmath-k1 cylindrical-k)) 1e-8)))
  (println ""))

(println "=== Summary ===")
(println "The correct modified spherical Bessel function k_l(x) should:")
(println "  - k_0(x) = e^(-x) / x")
(println "  - k_1(x) = e^(-x) * (1 + 1/x) / x")
(println "  - k_l(x) = sqrt(π/(2x)) * K_{l+1/2}(x)")
(println "")
(println "Compare the results above to determine which implementation is correct.")

