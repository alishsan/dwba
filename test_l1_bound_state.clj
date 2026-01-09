(require '[dwba.transfer :as t] :reload)

;; Test the 2p bound state at the found energy
(println "=== Testing 2p Bound State (l=1, n=2) ===")
(println "Found sign change around -184.75 MeV")
(println "")

;; Test the shooting method
(let [V-params [350.0 2.0 0.6]
      l 1
      n 2  ; 2p state should have 0 radial nodes (n - l - 1 = 2 - 1 - 1 = 0)
      r-max 20.0
      h 0.001
      result (t/find-bound-state-energy V-params l n r-max h)]
  (println "=== Shooting Method Result ===")
  (println (format "Energy: %.6f MeV" (:energy result)))
  (println (format "Boundary value: %.2e" (:boundary-value result)))
  (println (format "Nodes: %d (expected: 0 for 2p)" (:nodes result)))
  (println (format "Converged: %s" (:converged? result)))
  (println "")
  
  ;; Test the found energy directly
  (when (:energy result)
    (let [E-test (:energy result)
          u-test (t/solve-bound-state-numerov E-test l 350.0 2.0 0.6 h r-max)
          boundary-test (last u-test)
          nodes-test (t/count-nodes u-test)]
      (println "=== Verification at Found Energy ===")
      (println (format "Energy: %.6f MeV" E-test))
      (println (format "Boundary value: %.2e" boundary-test))
      (println (format "Nodes: %d" nodes-test))
      (println "")
      
      ;; Check wavefunction behavior
      (when (:wavefunction result)
        (let [u (:wavefunction result)
              u-max (apply max (map Math/abs u))
              u-at-5 (nth u (int (/ 5.0 h)))
              u-at-10 (nth u (int (/ 10.0 h)))
              u-at-15 (nth u (int (/ 15.0 h)))]
          (println "=== Wavefunction Properties ===")
          (println (format "Length: %d points" (count u)))
          (println (format "Max |u|: %.2e" u-max))
          (println (format "u(5 fm): %.2e" u-at-5))
          (println (format "u(10 fm): %.2e" u-at-10))
          (println (format "u(15 fm): %.2e" u-at-15))
          (println (format "u(20 fm): %.2e" (last u))))))))

;; Also test energies around -184.75 to see the sign change
(println "")
(println "=== Fine Grid Around Sign Change ===")
(let [energies (range -184.70 -184.80 -0.01)
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E 1 350.0 2.0 0.6 0.001 20.0)
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
  (if (seq (filter some? sign-changes))
    (doseq [sc (filter some? sign-changes)]
      (println (format "Sign change between E=%.2f and E=%.2f MeV" (:E1 sc) (:E2 sc)))
      (println (format "  Boundary: %.2e -> %.2e" (:val1 sc) (:val2 sc))))
    (println "No sign changes found in this range")))

