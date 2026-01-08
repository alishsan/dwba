(require '[dwba.transfer :as t])

;; Test boundary values at different energies for 2p state (l=1)
(def V-params [350.0 2.0 0.6])
(def l 1)
(def r-max 20.0)
(def h 0.001)

;; Test energies from -1 to -201 MeV
(let [energies (range -1.0 -201.0 -5.0)
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
  (println "=== Boundary Values for 2p State (l=1) ===")
  (println (format "Testing %d energies from %.1f to %.1f MeV" 
                  (count energies) (first energies) (last energies)))
  (println "")
  
  ;; Find sign changes
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
    (println (format "Found %d sign changes" (count (filter some? sign-changes))))
    (doseq [sc (filter some? sign-changes)]
      (println (format "Sign change between E=%.1f and E=%.1f MeV" (:E1 sc) (:E2 sc)))
      (println (format "  Boundary values: %.2e -> %.2e" (:val1 sc) (:val2 sc)))
      (println (format "  Nodes: %d -> %d" (:nodes1 sc) (:nodes2 sc)))
      (println ""))
    
    ;; Find minimum boundary value
    (let [min-boundary (apply min-key :boundary-abs results)]
      (println "=== Minimum Boundary Value ===")
      (println (format "Energy: %.1f MeV" (:energy min-boundary)))
      (println (format "Boundary value: %.2e" (:boundary-value min-boundary)))
      (println (format "Nodes: %d" (:nodes min-boundary)))
      (println "")
      
      ;; Find candidates with 0 nodes (for 2p state)
      (let [zero-node-candidates (filter #(= (:nodes %) 0) results)
            best-zero-node (when (seq zero-node-candidates)
                            (apply min-key :boundary-abs zero-node-candidates))]
        (println "=== Candidates with 0 Nodes (for 2p) ===")
        (if best-zero-node
          (do
            (println (format "Found %d candidates with 0 nodes" (count zero-node-candidates)))
            (println (format "Best: E=%.1f MeV, boundary=%.2e" 
                           (:energy best-zero-node) (:boundary-value best-zero-node))))
          (println "No candidates with 0 nodes found"))))))

