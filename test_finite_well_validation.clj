(require '[functions :refer :all]
         '[fastmath.core :as m])

(println "=== Finite Square Well Bound State Validation ===")
(println "Testing l=1 and l=2 cases for physical consistency\n")

;; Test parameters
(def test-cases [{:l 0 :z0 3.0 :name "l=0, z0=3.0 (shallow)"}
                 {:l 0 :z0 6.0 :name "l=0, z0=6.0 (medium)"}
                 {:l 0 :z0 10.0 :name "l=0, z0=10.0 (deep)"}
                 {:l 1 :z0 3.0 :name "l=1, z0=3.0 (shallow)"}
                 {:l 1 :z0 6.0 :name "l=1, z0=6.0 (medium)"}
                 {:l 1 :z0 10.0 :name "l=1, z0=10.0 (deep)"}
                 {:l 2 :z0 6.0 :name "l=2, z0=6.0 (medium)"}
                 {:l 2 :z0 10.0 :name "l=2, z0=10.0 (deep)"}])

(defn validate-bound-state [state l z0]
  "Validate that a bound state result is physically reasonable."
  (let [e-ratio (:e-ratio state)
        xi (:xi state)
        eta (:eta state)
        matching-error (:matching-error state)
        converged? (:converged? state)]
    {:valid? (and (>= e-ratio 0.01)
                  (<= e-ratio 0.99)
                  (> xi 0)
                  (> eta 0)
                  (< (m/abs matching-error) 1e-3)  ; Matching error should be small
                  converged?)
     :e-ratio e-ratio
     :matching-error matching-error
     :xi xi
     :eta eta
     :converged? converged?}))

(defn test-case [test]
  (let [l (:l test)
        z0 (:z0 test)
        name (:name test)]
    (println (format "=== %s ===" name))
    (println (format "Parameters: l=%d, z0=%.1f" l z0))
    
      (let [bound-states (find-all-bound-states l z0)
          validated (map #(validate-bound-state % l z0) bound-states)
          valid-states (filter :valid? validated)]
      
      (println (format "Found %d bound state(s)" (count bound-states)))
      (println (format "Valid states: %d" (count valid-states)))
      (println "")
      
      (doseq [[idx validation] (map-indexed vector validated)]
        (when (:valid? validation)
          (println (format "  State %d:" (inc idx)))
          (println (format "    e-ratio = %.6f" (:e-ratio validation)))
          (println (format "    |E|/V0 = %.6f" (:e-ratio validation)))
          (println (format "    xi = %.6f" (:xi validation)))
          (println (format "    eta = %.6f" (:eta validation)))
          (println (format "    Matching error = %.6e" (:matching-error validation)))
          (println (format "    Converged: %s" (:converged? validation)))
          
          ;; Verify the matching condition directly
          (let [verify-error (finite-well-matching-error (:xi validation) (:eta validation) l)]
            (println (format "    Direct verification: %.6e" verify-error))
            (when (>= (m/abs verify-error) 1e-3)
              (println (format "    ⚠️  Warning: Large matching error!"))))
          (println "")))
      
      ;; Physical consistency checks
      (println "  Physical consistency checks:")
      
      ;; Check 1: Energies should be in valid range
      (let [all-valid? (every? #(and (>= (:e-ratio %) 0.01) (<= (:e-ratio %) 0.99)) valid-states)]
        (println (format "    ✓ Energies in range [0.01, 0.99]: %s" all-valid?)))
      
      ;; Check 2: Matching errors should be small
      (let [all-small-errors? (every? #(< (m/abs (:matching-error %)) 1e-3) valid-states)]
        (println (format "    ✓ Matching errors < 1e-3: %s" all-small-errors?)))
      
      ;; Check 3: States should be sorted by energy (lowest first)
      (let [e-ratios (map :e-ratio valid-states)
            sorted? (= e-ratios (sort e-ratios))]
        (println (format "    ✓ States sorted by energy: %s" sorted?)))
      
      ;; Check 4: For higher l, there should be fewer or equal bound states than l=0
      (when (and (> l 0) (>= z0 6.0))
        (let [l0-states (count (find-all-bound-states 0 z0))
              l-states (count valid-states)]
          (println (format "    ✓ l=%d has %d states, l=0 has %d states (l>0 should have ≤ l=0): %s"
                          l l-states l0-states (<= l-states l0-states)))))
      
      (println "")
      valid-states)))

(println "Running validation tests...\n")

;; Run all test cases
(def all-results (map test-case test-cases))

(println "=== Summary ===")
(println "")
(println "Comparison across l values for z0=6.0:")
(let [z0-6-results (filter #(= (:z0 %) 6.0) test-cases)
      l0-count (count (find-all-bound-states 0 6.0))
      l1-count (count (find-all-bound-states 1 6.0))
      l2-count (count (find-all-bound-states 2 6.0))]
  (println (format "  l=0: %d bound states" l0-count))
  (println (format "  l=1: %d bound states" l1-count))
  (println (format "  l=2: %d bound states" l2-count))
  (println "")
  (println "  Expected: l=0 ≥ l=1 ≥ l=2 (centrifugal barrier effect)")
  (println (format "  Result: %s" (and (>= l0-count l1-count) (>= l1-count l2-count)))))

(println "")
(println "=== Test Complete ===")
(println "")
(println "Note: For specific numerical values, you may want to:")
(println "  1. Compare with published results in quantum mechanics textbooks")
(println "  2. Use the infinite square well limit (z0 → ∞) as a check")
(println "  3. Verify against other numerical methods (e.g., Numerov shooting)")
(println "  4. Check that the number of bound states follows expected trends")

