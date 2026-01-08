(require '[dwba.transfer :as t])

;; Comprehensive test for sign changes and shooting method

(println "=== Testing Sign Changes for Bound States ===\n")

;; Test l=0 (1s state) - we know there's a sign change
(println "1. Testing l=0 (1s state) with fine grid around sign change:")
(let [energies (range -14.0 -17.0 -0.1)
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E 0 50.0 2.0 0.6 0.001 20.0)
                            boundary-val (last u)]
                        {:energy E :boundary boundary-val}))
                    energies)
      sign-changes (for [i (range (dec (count results)))]
                    (let [curr (nth results i)
                          next (nth results (inc i))]
                      (when (not= (Math/signum (:boundary curr)) 
                                  (Math/signum (:boundary next)))
                        {:E1 (:energy curr)
                         :E2 (:energy next)
                         :val1 (:boundary curr)
                         :val2 (:boundary next)})))]
  (println (format "   Tested %d energies from %.1f to %.1f MeV" 
                  (count energies) (first energies) (last energies)))
  (if (seq (filter some? sign-changes))
    (doseq [sc (filter some? sign-changes)]
      (println (format "   ✓ Sign change between E=%.2f and E=%.2f MeV" 
                      (:E1 sc) (:E2 sc)))
      (println (format "     Boundary: %.2e -> %.2e" (:val1 sc) (:val2 sc))))
    (println "   ✗ No sign change found"))
  (println ""))

;; Test l=1 (2p state) with very wide range
(println "2. Testing l=1 (2p state) with wide energy range:")
(let [energies (concat (range -1.0 -50.0 -2.0)    ; Shallow
                      (range -50.0 -150.0 -5.0)     ; Medium
                      (range -150.0 -300.0 -10.0)   ; Deep
                      (range -300.0 -350.0 -5.0))   ; Very deep
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E 1 50.0 2.0 0.6 0.001 20.0)
                            boundary-val (last u)
                            nodes (t/count-nodes u)]
                        {:energy E 
                         :boundary boundary-val
                         :boundary-abs (Math/abs boundary-val)
                         :nodes nodes}))
                    energies)
      sign-changes (for [i (range (dec (count results)))]
                    (let [curr (nth results i)
                          next (nth results (inc i))]
                      (when (not= (Math/signum (:boundary curr)) 
                                  (Math/signum (:boundary next)))
                        {:E1 (:energy curr)
                         :E2 (:energy next)
                         :val1 (:boundary curr)
                         :val2 (:boundary next)
                         :nodes1 (:nodes curr)
                         :nodes2 (:nodes next)})))]
  (println (format "   Tested %d energies from %.1f to %.1f MeV" 
                  (count energies) (first energies) (last energies)))
  (if (seq (filter some? sign-changes))
    (do
      (println (format "   ✓ Found %d sign change(s):" (count (filter some? sign-changes))))
      (doseq [sc (filter some? sign-changes)]
        (println (format "     Between E=%.1f and E=%.1f MeV" (:E1 sc) (:E2 sc)))
        (println (format "       Boundary: %.2e -> %.2e" (:val1 sc) (:val2 sc)))
        (println (format "       Nodes: %d -> %d" (:nodes1 sc) (:nodes2 sc)))))
    (do
      (println "   ✗ No sign changes found")
      (println "")
      (println "   Checking minimum boundary value:")
      (let [min-boundary (apply min-key :boundary-abs results)]
        (println (format "     E=%.1f MeV: boundary=%.2e, nodes=%d" 
                        (:energy min-boundary) 
                        (:boundary min-boundary)
                        (:nodes min-boundary)))
        (println "")
        (println "   Checking sign consistency:")
        (let [all-positive (every? #(> (:boundary %) 0) results)
              all-negative (every? #(< (:boundary %) 0) results)]
          (if all-positive
            (println "     All boundary values are POSITIVE (wavefunction growing)")
            (if all-negative
              (println "     All boundary values are NEGATIVE (wavefunction growing)")
              (println "     Mixed signs found")))))))
  (println ""))

;; Test shooting method for l=0
(println "3. Testing shooting method for l=0 (1s state):")
(let [V-params [50.0 2.0 0.6]
      result (t/find-bound-state-energy V-params 0 1 20.0 0.001)]
  (println (format "   Energy: %.6f MeV" (:energy result)))
  (println (format "   Boundary value: %.2e" (:boundary-value result)))
  (println (format "   Nodes: %d (expected: 0)" (:nodes result)))
  (println (format "   Converged: %s" (:converged? result)))
  (println ""))

(println "=== Analysis ===")
(println "For l=0: Sign change found → shooting method should work")
(println "For l=1: Need to check if:")
(println "  - Bound state exists for these parameters")
(println "  - Energy is outside tested range")
(println "  - Different search strategy needed")

