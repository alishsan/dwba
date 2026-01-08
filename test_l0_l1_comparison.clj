(require '[dwba.transfer :as t])

;; Test both l=0 and l=1 to find sign changes
(def V-params [50.0 2.0 0.6])
(def r-max 20.0)
(def h 0.001)

(println "=== Testing l=0 (1s state) ===")
(let [energies (range -1.0 -31.0 -1.0)
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E 0 50.0 2.0 0.6 h r-max)
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
  (println "Sign changes found:")
  (doseq [sc (filter some? sign-changes)]
    (println (format "  Between E=%.1f and E=%.1f MeV" (:E1 sc) (:E2 sc)))
    (println (format "    Boundary: %.2e -> %.2e" (:val1 sc) (:val2 sc))))
  (println ""))

(println "=== Testing l=1 (2p state) with wider range ===")
(let [energies (concat (range -1.0 -50.0 -2.0)  ; Shallow
                      (range -50.0 -200.0 -5.0)  ; Medium
                      (range -200.0 -350.0 -10.0))  ; Deep
      results (mapv (fn [E]
                      (let [u (t/solve-bound-state-numerov E 1 50.0 2.0 0.6 h r-max)
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
  (println (format "Tested %d energies from %.1f to %.1f MeV" 
                  (count energies) (first energies) (last energies)))
  (println "")
  (println "Sign changes found:")
  (if (seq (filter some? sign-changes))
    (doseq [sc (filter some? sign-changes)]
      (println (format "  Between E=%.1f and E=%.1f MeV" (:E1 sc) (:E2 sc)))
      (println (format "    Boundary: %.2e -> %.2e" (:val1 sc) (:val2 sc)))
      (println (format "    Nodes: %d -> %d" (:nodes1 sc) (:nodes2 sc))))
    (do
      (println "  No sign changes found!")
      (println "")
      (println "  Minimum boundary value:")
      (let [min-boundary (apply min-key :boundary-abs results)]
        (println (format "    E=%.1f MeV: boundary=%.2e, nodes=%d" 
                        (:energy min-boundary) 
                        (:boundary min-boundary)
                        (:nodes min-boundary))))))
  (println "")
  (println "  Checking if all values have same sign:")
  (let [all-positive (every? #(> (:boundary %) 0) results)
        all-negative (every? #(< (:boundary %) 0) results)]
    (if all-positive
      (println "    All boundary values are POSITIVE")
      (if all-negative
        (println "    All boundary values are NEGATIVE")
        (println "    Mixed signs")))))

