(require '[dwba.transfer :as t])

;; Test boundary values at deeper energies for 2p state (l=1)
(def V-params [350.0 2.0 0.6])
(def l 1)
(def r-max 20.0)
(def h 0.001)

;; Test a wide range of energies
(let [energies (concat (range -50.0 -350.0 -10.0)  ; Deeper energies
                      (range -200.0 -350.0 -5.0))  ; Finer grid in deeper region
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E l 350.0 2.0 0.6 h r-max)
                            boundary-val (t/bound-state-boundary-value u r-max h)
                            nodes (t/count-nodes u)]
                        {:energy E
                         :boundary-value boundary-val
                         :boundary-abs (Math/abs boundary-val)
                         :sign (Math/signum boundary-val)
                         :nodes nodes}))
                    energies)]
  (println "=== Testing Deeper Energies for 2p State (l=1) ===")
  (println (format "Testing %d energies" (count energies)))
  (println "")
  
  ;; Check for sign changes
  (let [sign-changes (for [i (range (dec (count results)))]
                      (let [curr (nth results i)
                            next (nth results (inc i))]
                        (when (not= (:sign curr) (:sign next))
                          {:E1 (:energy curr)
                           :E2 (:energy next)
                           :val1 (:boundary-value curr)
                           :val2 (:boundary-value next)
                           :nodes1 (:nodes curr)
                           :nodes2 (:nodes next)})))]
    (println "=== Sign Changes ===")
    (let [scs (filter some? sign-changes)]
      (if (seq scs)
        (doseq [sc scs]
          (println (format "Sign change between E=%.1f and E=%.1f MeV" (:E1 sc) (:E2 sc)))
          (println (format "  Boundary values: %.2e -> %.2e" (:val1 sc) (:val2 sc)))
          (println (format "  Nodes: %d -> %d" (:nodes1 sc) (:nodes2 sc)))
          (println ""))
        (println "No sign changes found - all boundary values have same sign")))
    
    ;; Find minimum boundary value
    (let [min-boundary (apply min-key :boundary-abs results)]
      (println "=== Minimum Boundary Value ===")
      (println (format "Energy: %.1f MeV" (:energy min-boundary)))
      (println (format "Boundary value: %.2e" (:boundary-value min-boundary)))
      (println (format "Nodes: %d" (:nodes min-boundary)))
      (println "")
      
      ;; Check if all values are positive
      (let [all-positive (every? #(> (:boundary-value %) 0) results)
            all-negative (every? #(< (:boundary-value %) 0) results)]
        (println "=== Sign Analysis ===")
        (if all-positive
          (println "All boundary values are POSITIVE - wavefunction is growing at r_max")
          (if all-negative
            (println "All boundary values are NEGATIVE - wavefunction is growing (negative) at r_max")
            (println "Mixed signs found")))
        
        ;; Find candidates with 0 nodes
        (let [zero-node-candidates (filter #(= (:nodes %) 0) results)]
          (println "")
          (println (format "=== Candidates with 0 Nodes (for 2p) ==="))
          (if (seq zero-node-candidates)
            (let [best (apply min-key :boundary-abs zero-node-candidates)]
              (println (format "Found %d candidates with 0 nodes" (count zero-node-candidates)))
              (println (format "Best: E=%.1f MeV, boundary=%.2e" 
                             (:energy best) (:boundary-value best)))
              (println "")
              (println "Top 5 candidates with smallest boundary values:")
              (doseq [c (take 5 (sort-by :boundary-abs zero-node-candidates))]
                (println (format "  E=%.1f MeV: boundary=%.2e, nodes=%d" 
                               (:energy c) (:boundary-value c) (:nodes c)))))
            (println "No candidates with 0 nodes found")))))))

