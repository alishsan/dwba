(require '[functions :refer :all])

;; Test to find the root of solver-step between e-ratio 0.4 and 0.5
(println "=== Finding Root of solver-step ===")
(println "Parameters: l=1, z0=6.0")
(println "")

(let [l 2
      z0 6.0
      ;; Fine grid around the suspected root
      e-ratios (range 0.4 0.5 0.001)
      results (mapv (fn [e-ratio]
                      (let [result (solver-step e-ratio l z0)
                            f-val (:f result)
                            f-prime (:f-prime result)]
                        {:e-ratio e-ratio
                         :f f-val
                         :f-prime f-prime
                         :abs-f (Math/abs f-val)}))
                    e-ratios)
      ;; Find sign changes
      sign-changes (for [i (range (dec (count results)))]
                    (let [curr (nth results i)
                          next (nth results (inc i))]
                      (when (and (not (Double/isNaN (:f curr)))
                                (not (Double/isNaN (:f next)))
                                (not= (Math/signum (:f curr))
                                      (Math/signum (:f next))))
                        {:e1 (:e-ratio curr)
                         :e2 (:e-ratio next)
                         :f1 (:f curr)
                         :f2 (:f next)})))
      ;; Find minimum |f|
      min-f-result (apply min-key :abs-f (filter #(not (Double/isNaN (:f %))) results))]

  (println "=== Sign Changes ===")
  (if (seq (filter some? sign-changes))
    (doseq [sc (filter some? sign-changes)]
      (println (format "Sign change between e-ratio=%.4f and e-ratio=%.4f" (:e1 sc) (:e2 sc)))
      (println (format "  f(%.4f) = %.6e" (:e1 sc) (:f1 sc)))
      (println (format "  f(%.4f) = %.6e" (:e2 sc) (:f2 sc)))
      (println ""))
    (println "No sign changes found"))
  
  (println "=== Minimum |f| ===")
  (println (format "e-ratio: %.6f" (:e-ratio min-f-result)))
  (println (format "f: %.6e" (:f min-f-result)))
  (println (format "f-prime: %.6e" (:f-prime min-f-result)))
  (println "")
  
  ;; Use find-all-bound-states to find all bound states
  (println "=== Using find-all-bound-states ===")
  (let [bound-states (find-all-bound-states l z0)]
    (println (format "Found %d bound state(s):" (count bound-states)))
    (println "")
    (doseq [[idx state] (map-indexed vector bound-states)]
      (println (format "=== Bound State %d ===" (inc idx)))
      (if (:converged? state)
        (do
          (println (format "e-ratio = %.9f" (:e-ratio state)))
          (println (format "Matching error: %.6e" (:matching-error state)))
          (println (format "Iterations: %d" (:iterations state)))
          (println (format "xi (ka) = %.6f" (:xi state)))
          (println (format "eta (kappa*a) = %.6f" (:eta state)))
          (println (format "Energy ratio |E|/V0 = %.6f" (:e-ratio state)))
          (println "")
          (println "=== Verification ===")
          (let [verify-result (solver-step (:e-ratio state) l z0)]
            (println (format "f(e-ratio) = %.6e" (:f verify-result)))
            (println (format "f-prime(e-ratio) = %.6e" (:f-prime verify-result)))))
        (do
          (println "Did not converge")
          (println (format "Final e-ratio: %.6f" (:e-ratio state)))
          (println (format "Matching error: %.6e" (:matching-error state)))
          (println (format "Iterations: %d" (:iterations state)))))
      (println ""))))

