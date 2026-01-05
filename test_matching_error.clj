#!/usr/bin/env clojure

;; Test the finite well matching error function to understand its behavior

(require '[functions :refer :all])
(require '[fastmath.core :as m])

(println "=== Testing Finite Well Matching Error Function ===\n")

;; Test 1: Check function behavior for l=0
(println "Test 1: l=0, z0=10, checking error at different e-ratio values")
(let [l 0
      z0 10.0]
  (doseq [e-ratio [0.01 0.1 0.3 0.5 0.7 0.9 0.99]]
    (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
          eta (* z0 (Math/sqrt e-ratio))
          error (finite-well-matching-error xi eta l)
          j-xi (j-l l xi)
          j-prime-xi (j-l-deriv l xi)
          k-eta (k-l l eta)
          k-prime-eta (k-l-deriv l eta)
          log-deriv-inside (* xi (/ j-prime-xi j-xi))
          log-deriv-outside (* eta (/ k-prime-eta k-eta))]
      (println (format "  e-ratio=%.2f: xi=%.4f, eta=%.4f" e-ratio xi eta))
      (println (format "    j_l(xi)=%.6e, j_l'(xi)=%.6e" j-xi j-prime-xi))
      (println (format "    k_l(eta)=%.6e, k_l'(eta)=%.6e" k-eta k-prime-eta))
      (println (format "    log-deriv inside:  %.6e" log-deriv-inside))
      (println (format "    log-deriv outside: %.6e" log-deriv-outside))
      (println (format "    error: %.6e" error))
      (println ""))))

;; Test 2: Check for l=1
(println "Test 2: l=1, z0=10, checking error at different e-ratio values")
(let [l 1
      z0 10.0]
  (doseq [e-ratio [0.01 0.1 0.3 0.5 0.7 0.9 0.99]]
    (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
          eta (* z0 (Math/sqrt e-ratio))
          error (finite-well-matching-error xi eta l)]
      (println (format "  e-ratio=%.2f: xi=%.4f, eta=%.4f, error=%.6e" 
                     e-ratio xi eta error)))
  (println ""))

;; Test 3: Check signs at endpoints
(println "Test 3: Checking signs at endpoints for bisection")
(doseq [l [0 1]]
  (let [z0 10.0
        f (fn [e-ratio]
            (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
                  eta (* z0 (Math/sqrt e-ratio))]
              (finite-well-matching-error xi eta l)))
        f-low (f 0.0001)
        f-high (f 0.9999)]
    (println (format "l=%d:" l))
    (println (format "  f(0.0001) = %.6e (sign: %d)" f-low (m/signum f-low)))
    (println (format "  f(0.9999) = %.6e (sign: %d)" f-high (m/signum f-high)))
    (println (format "  Signs match: %s" (= (m/signum f-low) (m/signum f-high))))
    (println "")))

;; Test 4: Try different e-ratio ranges
(println "Test 4: Testing different e-ratio ranges for l=0, z0=10")
(let [l 0
      z0 10.0
      f (fn [e-ratio]
          (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
                eta (* z0 (Math/sqrt e-ratio))]
            (finite-well-matching-error xi eta l)))]
  (doseq [[low high] [[0.001 0.1] [0.1 0.5] [0.5 0.9] [0.9 0.999]]]
    (let [f-low (f low)
          f-high (f high)]
      (println (format "  Range [%.3f, %.3f]:" low high))
      (println (format "    f(%.3f) = %.6e (sign: %d)" low f-low (m/signum f-low)))
      (println (format "    f(%.3f) = %.6e (sign: %d)" high f-high (m/signum f-high)))
      (println (format "    Signs match: %s" (= (m/signum f-low) (m/signum f-high))))
      (println ""))))

;; Test 5: Check if the matching condition formula is correct
(println "Test 5: Verifying matching condition formula")
(println "  For a finite square well, the matching condition should be:")
(println "  xi * j_l'(xi) / j_l(xi) = eta * k_l'(eta) / k_l(eta)")
(println "  This comes from continuity of u'(a)/u(a) at the boundary")
(println "")
(println "  Alternative form (from some references):")
(println "  j_{l-1}(xi) / j_l(xi) = -k_{l-1}(eta) / k_l(eta)")
(println "  (This uses recurrence relations for derivatives)")
(println "")

(println "=== Tests Complete ===")

