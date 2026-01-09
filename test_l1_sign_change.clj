(require '[dwba.transfer :as t])

;; Test l=1 (2p state) with fine grid around the sign change
(println "=== Testing l=1 (2p state) Sign Change ===")
(println "Using physical units with f-r-numerov")
(println "")

(let [energies (range -130.0 -160.0 -1.0)  ; Fine grid around sign change
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E 1 350.0 2.0 0.6 0.001 20.0)
                            boundary-val (last u)
                            nodes (t/count-nodes u)]
                        {:energy E 
                         :boundary boundary-val
                         :boundary-abs (Math/abs boundary-val)
                         :sign (Math/signum boundary-val)
                         :nodes nodes}))
                    energies)
      sign-changes (for [i (range (dec (count results)))]
                    (let [curr (nth results i)
                          next (nth results (inc i))]
                      (when (not= (:sign curr) (:sign next))
                        {:E1 (:energy curr)
                         :E2 (:energy next)
                         :val1 (:boundary curr)
                         :val2 (:boundary next)
                         :nodes1 (:nodes curr)
                         :nodes2 (:nodes next)})))]
  (println (format "Tested %d energies from %.1f to %.1f MeV" 
                  (count energies) (first energies) (last energies)))
  (println "")
  
  (println "=== Sign Changes ===")
  (if (seq (filter some? sign-changes))
    (doseq [sc (filter some? sign-changes)]
      (println (format "Sign change between E=%.1f and E=%.1f MeV" (:E1 sc) (:E2 sc)))
      (println (format "  Boundary: %.2e -> %.2e" (:val1 sc) (:val2 sc)))
      (println (format "  Nodes: %d -> %d" (:nodes1 sc) (:nodes2 sc)))
      (println ""))
    (println "No sign changes found in this range"))
  
  ;; Find minimum boundary value
  (let [min-boundary (apply min-key :boundary-abs results)]
    (println "=== Minimum Boundary Value ===")
    (println (format "Energy: %.1f MeV" (:energy min-boundary)))
    (println (format "Boundary value: %.2e" (:boundary min-boundary)))
    (println (format "Nodes: %d (expected: 0 for 2p state)" (:nodes min-boundary)))
    (println ""))
  
  ;; Test shooting method
  (println "=== Testing Shooting Method ===")
  (let [V-params [350.0 2.0 0.6]
        result (t/find-bound-state-energy V-params 1 2 20.0 0.001)]  ; n=2 for 2p state
    (println (format "Energy: %.6f MeV" (:energy result)))
    (println (format "Boundary value: %.2e" (:boundary-value result)))
    (println (format "Nodes: %d (expected: 0)" (:nodes result)))
    (println (format "Converged: %s" (:converged? result)))))

