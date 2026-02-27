(require '[dwba.halo-nuclei :as halo]
         '[fastmath.core :as m])

(println "=== HALO NUMEROV START COMPARISON ===")
(println)

;; -----------------------------------------------------------------------------
;; 1. ¹¹Be halo bound state: hybrid (Bessel) vs finite start, coarse h scan
;; -----------------------------------------------------------------------------

(println "1. ¹¹Be bound state (neutron halo, l = 0)")
(println "   Comparing hybrid Riccati-Hankel start vs finite power-law start")
(println)

(let [E-b 0.504                           ; MeV (binding energy)
      l 0
      V-params [62.0 2.7 0.6]             ; [V0 R a] from halo_nuclei example
      mu 869.4                            ; MeV/c^2
      R (second V-params)
      r-max 60.0                          ; fm, extended radius
      h-values [0.20 0.10 0.05 0.02 0.01] ; increasingly finer steps
      r-match (halo/adaptive-matching-radius R E-b mu)]

  (println (format "   Parameters: E_b = %.3f MeV, R = %.2f fm, r_max = %.1f fm"
                   E-b R r-max))
  (println (format "   Adaptive matching radius r_match ≈ %.2f fm" r-match))
  (println (format "   Step sizes h = %s" (pr-str h-values)))
  (println)

  (println (format "%-8s %-16s %-16s %-16s %-12s %-14s %-12s"
                   "h (fm)"
                   "ANC_hybrid"
                   "ANC_finite"
                   "|ΔANC|"
                   "ANC_ratio"
                   "max_rel_diff_u"
                   "overlap"))
  (println (apply str (repeat 110 "-")))

  (doseq [h h-values]
    (let [steps (int (/ r-max h))
          ;; Hybrid start (Riccati-Hankel)
          u-hybrid-raw (halo/solve-bound-state-numerov E-b l V-params mu h r-max)
          u-hybrid (halo/normalize-bound-state u-hybrid-raw h)
          r-values (mapv #(* % h) (range (count u-hybrid)))
          anc-hybrid (halo/extract-anc u-hybrid r-values E-b mu 0 0 l r-match)

          ;; Finite power-law start
          u-finite-raw (halo/solve-bound-state-numerov-finite-start E-b l V-params mu h r-max)
          u-finite (halo/normalize-bound-state u-finite-raw h)
          anc-finite (halo/extract-anc u-finite r-values E-b mu 0 0 l r-match)

          ;; Wavefunction comparison
          n (min (count u-hybrid) (count u-finite))
          max-rel-diff (when (and (> n 0)
                                  (some #(not (zero? %)) u-hybrid))
                         (let [diffs (for [i (range n)]
                                       (let [uh (nth u-hybrid i)
                                             uf (nth u-finite i)]
                                         (if (and (not (zero? uh))
                                                  (Double/isFinite uh))
                                           (Math/abs (/ (- uf uh) uh))
                                           0.0)))]
                           (when (seq diffs) (apply max diffs))))
          overlap (when (>= n 2)
                    (let [integrand (mapv #(* (nth u-hybrid %) (nth u-finite %)) (range n))
                          simpson-sum (loop [i 1 sum 0.0]
                                        (if (>= i (dec n))
                                          sum
                                          (let [coeff (if (odd? i) 4.0 2.0)]
                                            (recur (inc i) (+ sum (* coeff (get integrand i)))))))
                          integral (* (/ h 3.0)
                                      (+ (first integrand) (last integrand) simpson-sum))]
                      integral))
          anc-ratio (if (and (pos? anc-finite) (Double/isFinite anc-finite))
                      (/ anc-hybrid anc-finite)
                      Double/NaN)]

      (println
       (format "%-8.3f %-16.6f %-16.6f %-16.6f %-12.4f %-14.4e %-12.6f"
               h
               anc-hybrid
               anc-finite
               (Math/abs (- anc-hybrid anc-finite))
               (if (Double/isNaN (double anc-ratio)) 0.0 anc-ratio)
               (double (or max-rel-diff 0.0))
               (double (or overlap 0.0)))))))

(println)

;; -----------------------------------------------------------------------------
;; 2. Extreme weak-binding halo test (E_b = 0.1 MeV, same geometry)
;; -----------------------------------------------------------------------------

(println "2. Extreme weak halo test (E_b = 0.1 MeV, l = 0)")
(println "   Same potential geometry, much weaker binding to enhance halo tail")
(println)

(let [E-b 0.1                            ; MeV, very weakly bound
      l 0
      V-params [62.0 2.7 0.6]
      mu 869.4
      R (second V-params)
      r-max 80.0                         ; fm, larger radius for longer tail
      h-values [0.20 0.10 0.05 0.02 0.01]
      r-match (halo/adaptive-matching-radius R E-b mu)]

  (println (format "   Parameters: E_b = %.3f MeV, R = %.2f fm, r_max = %.1f fm"
                   E-b R r-max))
  (println (format "   Adaptive matching radius r_match ≈ %.2f fm" r-match))
  (println (format "   Step sizes h = %s" (pr-str h-values)))
  (println)

  (println (format "%-8s %-16s %-16s %-16s %-12s %-14s %-12s"
                   "h (fm)"
                   "ANC_hybrid"
                   "ANC_finite"
                   "|ΔANC|"
                   "ANC_ratio"
                   "max_rel_diff_u"
                   "overlap"))
  (println (apply str (repeat 110 "-")))

  (doseq [h h-values]
    (let [steps (int (/ r-max h))
          u-hybrid-raw (halo/solve-bound-state-numerov E-b l V-params mu h r-max)
          u-hybrid (halo/normalize-bound-state u-hybrid-raw h)
          r-values (mapv #(* % h) (range (count u-hybrid)))
          anc-hybrid (halo/extract-anc u-hybrid r-values E-b mu 0 0 l r-match)

          u-finite-raw (halo/solve-bound-state-numerov-finite-start E-b l V-params mu h r-max)
          u-finite (halo/normalize-bound-state u-finite-raw h)
          anc-finite (halo/extract-anc u-finite r-values E-b mu 0 0 l r-match)

          n (min (count u-hybrid) (count u-finite))
          max-rel-diff (when (and (> n 0)
                                  (some #(not (zero? %)) u-hybrid))
                         (let [diffs (for [i (range n)]
                                       (let [uh (nth u-hybrid i)
                                             uf (nth u-finite i)]
                                         (if (and (not (zero? uh))
                                                  (Double/isFinite uh))
                                           (Math/abs (/ (- uf uh) uh))
                                           0.0)))]
                           (when (seq diffs) (apply max diffs))))
          overlap (when (>= n 2)
                    (let [integrand (mapv #(* (nth u-hybrid %) (nth u-finite %)) (range n))
                          simpson-sum (loop [i 1 sum 0.0]
                                        (if (>= i (dec n))
                                          sum
                                          (let [coeff (if (odd? i) 4.0 2.0)]
                                            (recur (inc i) (+ sum (* coeff (get integrand i)))))))
                          integral (* (/ h 3.0)
                                      (+ (first integrand) (last integrand) simpson-sum))]
                      integral))
          anc-ratio (if (and (pos? anc-finite) (Double/isFinite anc-finite))
                      (/ anc-hybrid anc-finite)
                      Double/NaN)]

      (println
       (format "%-8.3f %-16.6f %-16.6f %-16.6f %-12.4f %-14.4e %-12.6f"
               h
               anc-hybrid
               anc-finite
               (Math/abs (- anc-hybrid anc-finite))
               (if (Double/isNaN (double anc-ratio)) 0.0 anc-ratio)
               (double (or max-rel-diff 0.0))
               (double (or overlap 0.0)))))))

(println)
(println "=== HALO NUMEROV COMPARISON COMPLETE ===")

