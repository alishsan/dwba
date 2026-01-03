;; Simple test for bound state solver
;; This test loads only what's needed, avoiding the forward reference issue

(load-file "src/complex.clj")
(load-file "src/functions.clj")

;; Define the bound state functions directly here to test
(ns-bound-state-test)

(defn bound-state-start [r l]
  "Power series expansion for bound state near r=0"
  (Math/pow r (inc l)))

(defn solve-bound-state-numerov [e l v0 rad diff h r-max]
  "Solve bound state using Numerov method"
  (let [steps (int (/ r-max h))
        u0 0.0
        u1 (bound-state-start h l)
        fs (mapv (fn [r] 
                   (if (zero? r)
                     0.0
                     (f-r-numerov r e l v0 rad diff)))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    (loop [n 1
           results [u0 u1]]
      (if (>= n (dec steps))
        results
        (let [un (get results n)
              un-1 (get results (dec n))
              fn-1 (get fs (dec n))
              fn (get fs n)
              fn+1 (get fs (inc n))
              numerator (+ (* 2.0 un) 
                           (- un-1) 
                           (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
              denominator (- 1.0 (* h2-12 fn+1))
              un+1 (/ numerator denominator)]
          (recur (inc n) (conj results un+1)))))))

(defn bound-state-boundary-value [u r-max h]
  "Check boundary condition"
  (let [idx (min (dec (count u)) (int (/ r-max h)))]
    (get u idx)))

(defn count-nodes [u]
  "Count nodes in wavefunction"
  (loop [n 0
         i 1
         prev-sign (Math/signum (get u 0))]
    (if (>= i (count u))
      n
      (let [current-sign (Math/signum (get u i))
            crossed-zero (not= prev-sign current-sign)]
        (recur (if (and crossed-zero (not (zero? (get u i))))
                 (inc n)
                 n)
               (inc i)
               (if (zero? (get u i)) prev-sign current-sign))))))

(println "=== Testing Bound State Solver ===\n")

;; Test parameters
(def ws-params [50.0 2.0 0.6])
(def v0 (first ws-params))
(def rad (second ws-params))
(def diff (last ws-params))
(def r-max 20.0)
(def h 0.01)

(println "Woods-Saxon parameters: V0=50 MeV, R0=2.0 fm, a0=0.6 fm")
(println "Integration: r_max = 20.0 fm, h = 0.01 fm\n")

;; Test 1: Try to find 1s bound state (n=1, l=0)
(println "Test 1: Finding 1s bound state (n=1, l=0)")
(println "Searching for energy where wavefunction → 0 at r_max...")

(let [E-test -25.0  ; Try a negative energy
      u (solve-bound-state-numerov E-test 0 v0 rad diff h r-max)
      u-end (bound-state-boundary-value u r-max h)
      nodes (count-nodes u)]
  (println (format "  Energy: %.2f MeV" E-test))
  (println (format "  u(r_max) = %.6e" u-end))
  (println (format "  Number of nodes: %d" nodes))
  (println (format "  Wavefunction length: %d points" (count u)))
  (println (format "  First few values:"))
  (doseq [i (range (min 5 (count u)))]
    (let [r (* i h)
          u-val (get u i)]
      (println (format "    u(%.2f fm) = %.6e" r u-val))))
  (println ""))

;; Test 2: Try different energies to find where boundary value crosses zero
(println "Test 2: Scanning energies to find bound state")
(println "Looking for energy where u(r_max) ≈ 0...")

(let [energies (range -45.0 -5.0 5.0)
      results (mapv (fn [E]
                      (let [u (solve-bound-state-numerov E 0 v0 rad diff h r-max)
                            u-end (bound-state-boundary-value u r-max h)
                            nodes (count-nodes u)]
                        {:energy E :boundary u-end :nodes nodes}))
                    energies)]
  (println "Energy (MeV) | u(r_max)      | Nodes")
  (println "-------------|---------------|------")
  (doseq [r results]
    (println (format "%12.2f | %13.6e | %5d" 
                     (:energy r) (:boundary r) (:nodes r))))
  
  ;; Find where sign changes
  (println "\nSign changes (potential bound states):")
  (doseq [i (range 1 (count results))]
    (let [prev (:boundary (get results (dec i)))
          curr (:boundary (get results i))
          prev-sign (Math/signum prev)
          curr-sign (Math/signum curr)]
      (when (not= prev-sign curr-sign)
        (println (format "  Between E=%.2f and E=%.2f MeV" 
                        (:energy (get results (dec i)))
                        (:energy (get results i)))))))
  (println ""))

(println "=== Test Complete ===")
(println "\nNote: This is a basic test. The full implementation in")
(println "src/dwba/transfer.clj includes:")
(println "  - Automatic energy search (shooting method)")
(println "  - Normalization")
(println "  - Complete bound state solver")

