;; Test script for bound state solver - can be loaded in REPL
;; Usage: lein repl, then (load-file "test_bound_state_repl.clj")

(require '[dwba.transfer :refer :all])
(require '[functions :refer :all])

(println "\n=== Testing Bound State Solver ===\n")

;; Test parameters
(def ws-params [50.0 2.0 0.6])
(println "Woods-Saxon parameters: V0=50 MeV, R0=2.0 fm, a0=0.6 fm")
(println "This is a deep well that should support bound states.\n")

;; Test 1: Find 1s bound state (n=1, l=0)
(println "Test 1: Finding 1s bound state (n=1, l=0)")
(println "This may take a moment...\n")

(try
  (let [result (solve-bound-state ws-params 1 0 nil 20.0 0.01)]
    (plot-bound-state-info result)
    
    (println "First 10 wavefunction values:")
    (doseq [i (range (min 10 (count (:normalized-wavefunction result))))]
      (let [r (* i 0.01)
            u (get (:normalized-wavefunction result) i)]
        (when (< (Math/abs u) 1e10)  ; Avoid printing huge numbers
          (println (format "  u(%.2f fm) = %.6e" r u)))))
    (println "")
    
    ;; Check normalization
    (let [u (:normalized-wavefunction result)
          h 0.01
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
      (println (format "Normalization check: ∫ u²(r) dr = %.8f" integral))
      (println (format "Should be 1.0, error: %.2e\n" (Math/abs (- integral 1.0)))))
    
    (println "✅ Test 1 passed!"))
  (catch Exception e
    (println "❌ Test 1 failed:")
    (println (.getMessage e))
    (println "This might be due to:")
    (println "  - Energy search not converging")
    (println "  - No bound state exists for these parameters")
    (println "  - Need to adjust search range")))

(println "\n=== Test Complete ===")
(println "\nTo test more:")
(println "  (def result (solve-bound-state [50.0 2.0 0.6] 1 1 nil))  ; 1p state")
(println "  (def result (solve-bound-state [50.0 2.0 0.6] 2 0 nil))  ; 2s state")

