#!/usr/bin/env clojure

;; Test script for bound state wavefunction solver
;; This demonstrates how to use the bound state solver for transfer reactions

(require '[dwba.transfer :refer :all])
(require '[functions :refer :all])

(println "=== Testing Bound State Solver ===\n")

;; Test 1: Simple Woods-Saxon well - find 1s bound state
(println "Test 1: Finding 1s bound state (n=1, l=0)")
(println "Woods-Saxon parameters: V0=50 MeV, R0=2.0 fm, a0=0.6 fm")
(def ws-params [50.0 2.0 0.6])

(let [result (solve-bound-state ws-params 1 0 nil 20.0 0.01)]
  (plot-bound-state-info result)
  (println "First few wavefunction values:")
  (doseq [i (range (min 10 (count (:normalized-wavefunction result))))]
    (let [r (* i 0.01)
          u (get (:normalized-wavefunction result) i)]
      (println (format "  u(%.2f fm) = %.6e" r u))))
  (println ""))

;; Test 2: Find 1p bound state (n=1, l=1)
(println "Test 2: Finding 1p bound state (n=1, l=1)")
(let [result (solve-bound-state ws-params 1 1 nil 20.0 0.01)]
  (plot-bound-state-info result)
  (println ""))

;; Test 3: Find 2s bound state (n=2, l=0) - should have 1 node
(println "Test 3: Finding 2s bound state (n=2, l=0) - should have 1 node")
(let [result (solve-bound-state ws-params 2 0 nil 20.0 0.01)]
  (plot-bound-state-info result)
  (println "Checking for node (sign change):")
  (let [u (:normalized-wavefunction result)
        ;; Find where sign changes
        sign-changes (for [i (range 1 (count u))]
                       (let [prev-sign (Math/signum (get u (dec i)))
                             curr-sign (Math/signum (get u i))
                             r (* i 0.01)]
                         (when (and (not= prev-sign curr-sign)
                                    (not (zero? (get u i))))
                           {:r r :u (get u i)})))]
    (doseq [node (filter some? sign-changes)]
      (println (format "  Node at r = %.2f fm, u = %.6e" (:r node) (:u node)))))
  (println ""))

;; Test 4: Energy estimate comparison
(println "Test 4: Comparing estimated vs calculated energy")
(let [E-est (bound-state-energy-approx ws-params 1 0)
      result (solve-bound-state ws-params 1 0 nil 20.0 0.01)
      E-calc (:energy result)]
  (println (format "Estimated energy: %.6f MeV" E-est))
  (println (format "Calculated energy: %.6f MeV" E-calc))
  (println (format "Difference: %.6f MeV" (Math/abs (- E-est E-calc))))
  (println ""))

;; Test 5: Normalization check
(println "Test 5: Checking normalization")
(let [result (solve-bound-state ws-params 1 0 nil 20.0 0.01)
        u (:normalized-wavefunction result)
        h 0.01
        ;; Calculate ∫ u²(r) dr using Simpson's rule
        integrand (mapv #(* % %) u)
        n (count integrand)
        simpson-sum (loop [i 1 sum 0.0]
                      (if (>= i (dec n))
                        sum
                        (let [coeff (if (odd? i) 4.0 2.0)
                              term (* coeff (get integrand i))]
                          (recur (inc i) (+ sum term)))))
        integral (* (/ h 3.0) 
                    (+ (first integrand) 
                       (last integrand) 
                       simpson-sum))]
    (println (format "Normalization integral ∫ u²(r) dr = %.8f" integral))
    (println (format "Should be 1.0, error: %.2e" (Math/abs (- integral 1.0))))
    (println ""))

(println "=== Tests Complete ===")
(println "\nNext steps:")
(println "1. Use bound states to calculate transfer form factors")
(println "2. Implement zero-range approximation for transfer interaction")
(println "3. Calculate transfer cross-sections")

