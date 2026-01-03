;; Debug script to understand what's happening with bound state search

(require '[dwba.transfer :refer :all])
(require '[functions :refer :all])

(println "=== Debugging Bound State Search ===\n")

(def ws-params [50.0 2.0 0.6])
(def v0 (first ws-params))
(def rad (second ws-params))
(def diff (last ws-params))
(def r-max 20.0)
(def h 0.01)

(println "Testing node counting on a simple wavefunction...")
;; Test node counting: u = sin(x) should have nodes at x = π, 2π, etc.
(def test-u (mapv #(Math/sin (* % 0.1)) (range 200)))
(println (format "Test wavefunction (sin): nodes = %d (should be ~19)" (count-nodes test-u)))

;; Test with monotonic function (no nodes)
(def test-u2 (mapv #(* % %) (range 200)))
(println (format "Test wavefunction (x²): nodes = %d (should be 0)" (count-nodes test-u2)))

(println "\n=== Scanning energies for l=0 ===")
(println "Looking for states with different numbers of nodes...\n")

;; Scan energies and count nodes
(let [energies (range -45.0 -5.0 2.0)
      results (mapv (fn [E]
                      (let [u (solve-bound-state-numerov E 0 v0 rad diff h r-max)
                            u-end (bound-state-boundary-value u r-max h)
                            nodes (count-nodes u)]
                        {:energy E :boundary u-end :nodes nodes}))
                    energies)]
  (println "Energy (MeV) | u(r_max)      | Nodes | State type")
  (println "-------------|---------------|-------|-----------")
  (doseq [r results]
    (let [state-type (cond
                       (and (= (:nodes r) 0) (< (Math/abs (:boundary r)) 0.1)) "1s candidate"
                       (and (= (:nodes r) 1) (< (Math/abs (:boundary r)) 0.1)) "2s candidate"
                       (and (= (:nodes r) 2) (< (Math/abs (:boundary r)) 0.1)) "3s candidate"
                       :else "other")]
      (println (format "%12.2f | %13.6e | %5d | %s"
                       (:energy r) (:boundary r) (:nodes r) state-type))))
  
  ;; Find best candidates
  (println "\n=== Best Candidates ===")
  (let [candidates-0-nodes (filter #(and (= (:nodes %) 0) (< (Math/abs (:boundary %)) 1.0)) results)
        candidates-1-node (filter #(and (= (:nodes %) 1) (< (Math/abs (:boundary %)) 1.0)) results)]
    (println "\n0-node states (1s candidates):")
    (doseq [c (take 5 candidates-0-nodes)]
      (println (format "  E = %.2f MeV, u(r_max) = %.6e" (:energy c) (:boundary c)))
    (println "\n1-node states (2s candidates):")
    (doseq [c (take 5 candidates-1-node)]
      (println (format "  E = %.2f MeV, u(r_max) = %.6e" (:energy c) (:boundary c))))))

(println "\n=== Testing actual wavefunction shape ===")
(let [E-test -15.579456
      u (solve-bound-state-numerov E-test 0 v0 rad diff h r-max)
      nodes (count-nodes u)]
  (println (format "Energy: %.6f MeV" E-test))
  (println (format "Nodes: %d" nodes))
  (println "\nWavefunction values at key points:")
  (doseq [r [0.0 0.1 0.5 1.0 2.0 5.0 10.0 15.0 20.0]]
    (let [idx (min (dec (count u)) (int (/ r h)))
          u-val (get u idx)]
      (println (format "  u(%.1f fm) = %.6e" r u-val))))
  
  ;; Check for sign changes
  (println "\nSign changes (potential nodes):")
  (let [sign-changes (for [i (range 1 (min 1000 (count u)))]
                       (let [prev-sign (Math/signum (get u (dec i)))
                             curr-sign (Math/signum (get u i))
                             r (* i h)]
                         (when (not= prev-sign curr-sign)
                           {:r r :u-prev (get u (dec i)) :u-curr (get u i)})))]
    (doseq [change (take 10 (filter some? sign-changes))]
      (println (format "  Sign change at r = %.2f fm: %.6e -> %.6e"
                       (:r change) (:u-prev change) (:u-curr change))))))

