#!/usr/bin/env clojure

;; Test bisection function separately

(require '[functions :refer :all])
(require '[fastmath.core :as m])

(println "=== Testing Bisection Function ===\n")

;; Test 1: Simple function with known root
(println "Test 1: Simple function f(x) = x^2 - 4 (root at x=2)")
(let [f (fn [x] (- (* x x) 4))
      result (bisection f 0.0 5.0 1e-10 100)]
  (println (format "  Root found: %.10f" (:root result)))
  (println (format "  f(root) = %.10e" (:value result)))
  (println (format "  Iterations: %d" (:iterations result)))
  (println (format "  Converged: %s" (:converged? result)))
  (println (format "  Expected: 2.0, error: %.2e" (Math/abs (- (:root result) 2.0))))
  (println ""))

;; Test 2: Another simple function
(println "Test 2: Function f(x) = x^3 - 8 (root at x=2)")
(let [f (fn [x] (- (* x x x) 8))
      result (bisection f 0.0 5.0 1e-10 100)]
  (println (format "  Root found: %.10f" (:root result)))
  (println (format "  f(root) = %.10e" (:value result)))
  (println (format "  Iterations: %d" (:iterations result)))
  (println (format "  Converged: %s" (:converged? result)))
  (println (format "  Expected: 2.0, error: %.2e" (Math/abs (- (:root result) 2.0))))
  (println ""))

;; Test 3: Test with finite well matching error
(println "Test 3: Finite well matching error for l=0, z0=10")
(let [l 0
      z0 10.0
      f (fn [e-ratio]
          (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
                eta (* z0 (Math/sqrt e-ratio))]
            (finite-well-matching-error xi eta l)))
      ;; Check function values at endpoints
      f-low (f 0.001)
      f-high (f 0.999)]
  (println (format "  f(0.001) = %.6e" f-low))
  (println (format "  f(0.999) = %.6e" f-high))
  (println (format "  Signs match: %s" (= (m/signum f-low) (m/signum f-high))))
  (println "")
  (if (not= (m/signum f-low) (m/signum f-high))
    (let [result (bisection f 0.001 0.999 1e-7 100)
          e-ratio (:root result)
          xi (* z0 (Math/sqrt (- 1 e-ratio)))
          eta (* z0 (Math/sqrt e-ratio))]
      (println (format "  Root found: e-ratio = %.10f" e-ratio))
      (println (format "  xi = %.10f" xi))
      (println (format "  eta = %.10f" eta))
      (println (format "  Matching error: %.10e" (:value result)))
      (println (format "  Iterations: %d" (:iterations result)))
      (println (format "  Converged: %s" (:converged? result)))
      (println (format "  Verification: xi^2 + eta^2 = %.10f (should be %.10f)" 
                      (+ (* xi xi) (* eta eta)) (* z0 z0))))
    (println "  Cannot use bisection - function has same sign at both endpoints"))
  (println ""))

;; Test 4: Test with l=1, z0=10
(println "Test 4: Finite well matching error for l=1, z0=10")
(let [l 1
      z0 10.0
      f (fn [e-ratio]
          (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
                eta (* z0 (Math/sqrt e-ratio))]
            (finite-well-matching-error xi eta l)))
      f-low (f 0.001)
      f-high (f 0.999)]
  (println (format "  f(0.001) = %.6e" f-low))
  (println (format "  f(0.999) = %.6e" f-high))
  (println (format "  Signs match: %s" (= (m/signum f-low) (m/signum f-high))))
  (println "")
  (if (not= (m/signum f-low) (m/signum f-high))
    (let [result (bisection f 0.001 0.999 1e-7 100)
          e-ratio (:root result)
          xi (* z0 (Math/sqrt (- 1 e-ratio)))
          eta (* z0 (Math/sqrt e-ratio))]
      (println (format "  Root found: e-ratio = %.10f" e-ratio))
      (println (format "  xi = %.10f" xi))
      (println (format "  eta = %.10f" eta))
      (println (format "  Matching error: %.10e" (:value result)))
      (println (format "  Iterations: %d" (:iterations result)))
      (println (format "  Converged: %s" (:converged? result)))
      (println (format "  Verification: xi^2 + eta^2 = %.10f (should be %.10f)" 
                      (+ (* xi xi) (* eta eta)) (* z0 z0))))
    (println "  Cannot use bisection - function has same sign at both endpoints"))
  (println ""))

;; Test 5: Compare with find-bound-state-finite-well
(println "Test 5: Using find-bound-state-finite-well function")
(doseq [l [0 1]]
  (let [z0 10.0
        result (find-bound-state-finite-well l z0)]
    (println (format "l=%d, z0=%.1f:" l z0))
    (println (format "  e-ratio: %.10f" (:e-ratio result)))
    (println (format "  xi: %.10f" (:xi result)))
    (println (format "  eta: %.10f" (:eta result)))
    (println (format "  Matching error: %.10e" (:matching-error result)))
    (println (format "  Converged: %s" (:converged? result)))
    (println (format "  Iterations: %d" (:iterations result)))
    (println (format "  Verification: xi^2 + eta^2 = %.10f (should be %.10f)" 
                    (+ (* (:xi result) (:xi result)) 
                       (* (:eta result) (:eta result))) 
                    (* z0 z0)))
    (println "")))

(println "=== Tests Complete ===")

