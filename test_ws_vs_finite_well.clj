(require '[dwba.transfer :as t]
         '[functions :refer :all]
         '[fastmath.core :as m])

(println "=== Testing Woods-Saxon vs Finite Square Well ===")
(println "Using small diffuseness (a0 << R0) to approximate square well\n")

;; Helper function to calculate z0 from physical parameters
(defn calculate-z0 [V0 R0]
  "Calculate dimensionless well depth parameter z0 from physical parameters.
   
   Parameters:
   - V0: Well depth in MeV
   - R0: Well radius in fm
   
   Returns: z0 = sqrt(2m·V0·R0²/ℏ²)
   
   Uses the mass factor from functions.clj: mass-factor = 2μ/(ℏc)²
   For a square well, z0 = sqrt(mass-factor · V0 · R0²)"
  (let [mass-factor (/ (* 2 869.4) (* 197.7 197.7))  ; 2μ/(ℏc)² for 14C+n
        z0 (m/sqrt (* mass-factor V0 (* R0 R0)))]
    z0))

;; Test cases: [V0 (MeV), R0 (fm), a0 (fm), l, expected-states]
;; Using very small a0 to approximate square well
;; z0 will be calculated from V0 and R0
(def test-cases [{:V0 30.0 :R0 2.0 :a0 0.01 :l 0}
                 {:V0 50.0 :R0 2.0 :a0 0.01 :l 0}
                 {:V0 50.0 :R0 2.0 :a0 0.01 :l 1}
                 {:V0 70.0 :R0 2.0 :a0 0.01 :l 0}
                 {:V0 70.0 :R0 2.0 :a0 0.01 :l 1}
                 {:V0 50.0 :R0 2.5 :a0 0.01 :l 0}
                 {:V0 50.0 :R0 2.5 :a0 0.01 :l 1}
                 {:V0 70.0 :R0 2.5 :a0 0.01 :l 0}
                 {:V0 70.0 :R0 2.5 :a0 0.01 :l 1}
                 {:V0 100.0 :R0 2.0 :a0 0.01 :l 0}
                 {:V0 100.0 :R0 2.0 :a0 0.01 :l 1}])

(defn find-finite-well-reference [z0 l]
  "Find the reference e-ratio values from finite square well calculation."
  (let [bound-states (find-all-bound-states l z0)
        valid-states (filter :converged? bound-states)]
    (map :e-ratio valid-states)))

(defn test-case [test]
  (let [V0 (:V0 test)
        R0 (:R0 test)
        a0 (:a0 test)
        l (:l test)
        V-params [V0 R0 a0]
        z0-calculated (calculate-z0 V0 R0)]
    
    (println (format "=== Test: V0=%.1f MeV, R0=%.1f fm, a0=%.3f fm, l=%d ===" V0 R0 a0 l))
    (println (format "Calculated z0 = %.2f" z0-calculated))
    (println "")
    
    ;; Get finite square well reference values
    (let [reference-e-ratios (find-finite-well-reference z0-calculated l)]
      (println (format "Finite Square Well Reference (z0=%.2f, l=%d):" z0-calculated l))
      (if (empty? reference-e-ratios)
        (println "  No bound states found")
        (doseq [[idx e-ratio] (map-indexed vector reference-e-ratios)]
          (println (format "  State %d: e-ratio = %.6f, E = %.3f MeV" 
                          (inc idx) e-ratio (- (* V0 e-ratio))))))
      (println "")
      
      ;; Try to find bound states using Woods-Saxon solver
      (println "Woods-Saxon Results (small diffuseness):")
      ;; Show expected search range
      (let [expected-E-min (- (* V0 0.8))
            expected-E-max (- (* V0 0.2))]
        (println (format "Expected search range: [%.2f, %.2f] MeV" expected-E-min expected-E-max))
        (when (seq reference-e-ratios)
          (doseq [ref-e-ratio reference-e-ratios]
            (let [ref-energy (- (* V0 ref-e-ratio))]
              (if (and (>= ref-energy expected-E-min) (<= ref-energy expected-E-max))
                (println (format "  Reference E=%.2f MeV is within search range ✓" ref-energy))
                (println (format "  Reference E=%.2f MeV is OUTSIDE search range ⚠️" ref-energy))))))
        (println ""))
      
      ;; For Woods-Saxon, n represents radial nodes
      ;; For the first bound state, we expect 0 radial nodes (n=1 means 0 nodes)
      ;; Try finding states with different n values (n = number of radial nodes)
      ;; For the first bound state, n=1 means 0 radial nodes
      (let [max-n (min 3 (inc (count reference-e-ratios)))
            ws-results (for [n (range 1 (inc max-n))]
                         (try
                           ;; Note: find-bound-state-energy uses get-energy-search-range internally
                           ;; For n=1, l=0: searches from -0.8*V0 to -0.2*V0
                           ;; For n=1, l=1: searches from -0.9*V0 to -0.2*V0 (with l adjustment)
                           ;; The reference e-ratio should be within this range
                           (let [result (t/find-bound-state-energy V-params l n 20.0 0.01)
                                 energy (:energy result)
                                 expected-E-min (- (* V0 0.8))
                                 expected-E-max (- (* V0 0.2))
                                 energy-valid? (and energy 
                                                   (< energy -0.5)
                                                   (>= energy expected-E-min)
                                                   (<= energy expected-E-max))]
                             (if (and energy-valid?
                                     (not (nil? (:wavefunction result)))
                                     (not (empty? (:wavefunction result))))
                               {:n n
                                :success? true
                                :energy energy
                                :e-ratio (/ (m/abs energy) V0)
                                :nodes (:nodes result)
                                :boundary-value (:boundary-value result)
                                :converged? (:converged? result)
                                :energy-valid? true}
                               {:n n
                                :success? false
                                :error (format "Energy %.3f MeV outside expected range [%.1f, %.1f] or invalid"
                                             (if energy energy 0.0) expected-E-min expected-E-max)}))
                           (catch Exception e
                             {:n n
                              :success? false
                              :error (.getMessage e)})))]
        
        (doseq [ws-result ws-results]
          (if (:success? ws-result)
            (let [e-ratio (:e-ratio ws-result)
                  energy (:energy ws-result)
                  energy-valid? (:energy-valid? ws-result)]
              (println (format "  n=%d: E = %.6f MeV, e-ratio = %.6f, nodes=%d, converged=%s"
                              (:n ws-result) energy e-ratio (:nodes ws-result) (:converged? ws-result)))
              
              ;; Warn if energy seems too shallow or not converged
              (when (not energy-valid?)
                (println "    ⚠️  Warning: Energy seems too shallow (may be at search boundary)"))
              (when (not (:converged? ws-result))
                (println "    ⚠️  Warning: Solver did not converge"))
              
              ;; Compare with reference only if energy is reasonable
              (when (and (seq reference-e-ratios) energy-valid? (:converged? ws-result))
                (let [closest-ref (apply min-key #(m/abs (- e-ratio %)) reference-e-ratios)
                      diff (m/abs (- e-ratio closest-ref))
                      rel-diff (* 100.0 (/ diff closest-ref))]
                  (println (format "    → Closest reference: e-ratio = %.6f, difference = %.6f (%.2f%%)"
                                  closest-ref diff rel-diff))
                  (if (< rel-diff 5.0)  ; Within 5%
                    (println "    ✓ Good agreement with finite square well")
                    (if (< rel-diff 20.0)
                      (println "    ⚠️  Moderate difference from finite square well")
                      (println "    ⚠️  Significant difference from finite square well"))))))
            (println (format "  n=%d: Failed - %s" (:n ws-result) (:error ws-result)))))
        
        (println "")
        
        ;; Summary
        (let [ws-found (count (filter :success? ws-results))
              ref-count (count reference-e-ratios)]
          (println "Summary:")
          (println (format "  Finite square well: %d bound state(s)" ref-count))
          (println (format "  Woods-Saxon (a0=%.3f): %d bound state(s) found" a0 ws-found))
          (if (= ws-found ref-count)
            (println "  ✓ Number of states matches")
            (println (format "  ⚠️  Number mismatch: expected %d, found %d" ref-count ws-found))))
        
        (println "")
        (println (str (apply str (repeat 70 "-"))))
        (println "")))))

(println "Running comparison tests...\n")
(println "Note: Using very small diffuseness (a0 = 0.01 fm) to approximate square well")
(println "")

(doseq [test test-cases]
  (test-case test))

(println "=== Test Complete ===")
(println "")
(println "Interpretation:")
(println "- e-ratio = |E|/V0 (dimensionless energy)")
(println "- For good agreement, e-ratio should match within ~5%")
(println "- Differences may arise from:")
(println "  • Finite diffuseness (even a0=0.01 fm is not exactly zero)")
(println "  • Numerical precision in shooting method")
(println "  • Different boundary conditions (square well vs Woods-Saxon)")
(println "")

