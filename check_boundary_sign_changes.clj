(require '[dwba.transfer :as t])

;; Check for sign changes in boundary value function
(defn check-boundary-sign-changes [E-start E-end num-steps V-params l r-max h]
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
        E-step (/ (- E-end E-start) num-steps)
        results (for [i (range (inc num-steps))]
                  (let [E (+ E-start (* i E-step))
                        u (t/solve-bound-state-numerov E l v0 rad diff h r-max)
                        u-end (t/bound-state-boundary-value u r-max h)
                        nodes (t/count-nodes u)]
                    {:energy E
                     :boundary-value u-end
                     :nodes nodes
                     :sign (Math/signum u-end)}))
        sign-changes (for [i (range (dec (count results)))]
                      (let [curr (nth results i)
                            next (nth results (inc i))]
                        (when (not= (:sign curr) (:sign next))
                          {:E1 (:energy curr)
                           :E2 (:energy next)
                           :val1 (:boundary-value curr)
                           :val2 (:boundary-value next)
                           :nodes1 (:nodes curr)
                           :nodes2 (:nodes next)})))]
    {:sign-changes (filter some? sign-changes)
     :min-boundary (apply min-key #(Math/abs (:boundary-value %)) results)
     :all-results results}))

;; Test with the same parameters
(let [result (check-boundary-sign-changes -21.0 -1.0 200 0 [50.0 2.0 0.6] 0 20.0 0.001)]
  (println "=== Sign Changes in Boundary Value Function ===")
  (println "\nNumber of sign changes:" (count (:sign-changes result)))
  (doseq [sc (:sign-changes result)]
    (println (format "\nSign change between E=%.2f and E=%.2f MeV" (:E1 sc) (:E2 sc)))
    (println (format "  Boundary values: %.2e -> %.2e" (:val1 sc) (:val2 sc)))
    (println (format "  Nodes: %d -> %d" (:nodes1 sc) (:nodes2 sc)))))
  
  (println "\n=== Candidate with Smallest Boundary Value ===")
  (let [best (:min-boundary result)]
    (println (format "Energy: %.2f MeV" (:energy best)))
    (println (format "Boundary value: %.2e" (:boundary-value best)))
    (println (format "Nodes: %d" (:nodes best)))))

