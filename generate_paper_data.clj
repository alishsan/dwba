;; Generate all data needed for the improved paper
;; Supports numerical_riccati_improved.tex

(require '[functions :refer :all])

(println "=== GENERATING DATA FOR IMPROVED PAPER ===\n")

;; Test parameters
(def test-params
  {:e 2.0
   :l 1
   :v0 46.23
   :rad 2.0
   :diff 0.5
   :r-boundary 10.0})

;; ============================================
;; 1. Wronskian Stability Table
;; ============================================
(println "1. Wronskian Stability Analysis")
(println "=================================")
(let [h-values [0.1 0.05 0.01]
      results (map (fn [h]
                     (let [stats (calculate-stability-data 
                                    (:e test-params) (:l test-params)
                                    (:v0 test-params) (:rad test-params) 
                                    (:diff test-params) h (:r-boundary test-params))]
                       {:h h
                        :naive-drift (:naive-w-drift stats)
                        :bessel-drift (:bessel-w-drift stats)}))
                   h-values)]
  (println (format "%-8s %-20s %-20s" "h (fm)" "Naive Drift" "Bessel Drift"))
  (println (apply str (repeat 60 "-")))
  (doseq [r results]
    (println (format "%-8.2f %-20.6e %-20.6e" 
                    (:h r) (:naive-drift r) (:bessel-drift r))))
  (println ""))

;; ============================================
;; 2. Phase Shift Convergence Table
;; ============================================
(println "2. Phase Shift Convergence Table")
(println "==================================")
(let [h-values [0.1 0.05 0.01]
      table (phase-shift-convergence-table 
              (:e test-params) (:l test-params)
              (:v0 test-params) (:rad test-params) 
              (:diff test-params) h-values (:r-boundary test-params))]
  (print-convergence-table table))

;; ============================================
;; 3. Energy Dependence Analysis
;; ============================================
(println "3. Energy Dependence Analysis")
(println "==============================")
(let [energies [0.5 1.0 2.0 5.0 10.0]
      h 0.01
      results (map (fn [e]
                     (let [u-bessel (solve-numerov e (:l test-params) 
                                                    (:v0 test-params) 
                                                    (:rad test-params) 
                                                    (:diff test-params) h 
                                                    (:r-boundary test-params))
                           u-naive (solve-numerov-naive e (:l test-params) 
                                                        (:v0 test-params) 
                                                        (:rad test-params) 
                                                        (:diff test-params) h 
                                                        (:r-boundary test-params))
                           delta-exact (exact-phase-shift-numerov e (:l test-params) 
                                                                   (:v0 test-params) 
                                                                   (:rad test-params) 
                                                                   (:diff test-params) 
                                                                   (:r-boundary test-params))
                           delta-bessel (phase-shift-from-numerov u-bessel h (:r-boundary test-params) e (:l test-params))
                           delta-naive (phase-shift-from-numerov u-naive h (:r-boundary test-params) e (:l test-params))
                           error-bessel (Math/abs (- delta-bessel delta-exact))
                           error-naive (Math/abs (- delta-naive delta-exact))
                           improvement (if (> error-bessel 0) (/ error-naive error-bessel) 0.0)]
                       {:e e
                        :error-naive error-naive
                        :error-bessel error-bessel
                        :improvement improvement}))
                   energies)]
  (println (format "%-8s %-20s %-20s %-15s" "E (MeV)" "Naive Error" "Bessel Error" "Improvement"))
  (println (apply str (repeat 75 "-")))
  (doseq [r results]
    (println (format "%-8.1f %-20.6e %-20.6e %-15.2f" 
                    (:e r) (:error-naive r) (:error-bessel r) (:improvement r))))
  (println ""))

;; ============================================
;; 4. Angular Momentum Dependence
;; ============================================
(println "4. Angular Momentum Dependence")
(println "=================================")
(let [l-values [0 1 2 3]
      h 0.01
      results (map (fn [l]
                     (try
                       (let [u-bessel (if (= l 0)
                                        ;; For l=0, use different start (no centrifugal term)
                                        (solve-numerov (:e test-params) l 
                                                       (:v0 test-params) 
                                                       (:rad test-params) 
                                                       (:diff test-params) h 
                                                       (:r-boundary test-params))
                                        (solve-numerov (:e test-params) l 
                                                       (:v0 test-params) 
                                                       (:rad test-params) 
                                                       (:diff test-params) h 
                                                       (:r-boundary test-params)))
                             u-naive (solve-numerov-naive (:e test-params) l 
                                                          (:v0 test-params) 
                                                          (:rad test-params) 
                                                          (:diff test-params) h 
                                                          (:r-boundary test-params))
                             delta-exact (exact-phase-shift-numerov (:e test-params) l 
                                                                    (:v0 test-params) 
                                                                    (:rad test-params) 
                                                                    (:diff test-params) 
                                                                    (:r-boundary test-params))
                             delta-bessel (phase-shift-from-numerov u-bessel h (:r-boundary test-params) (:e test-params) l)
                             delta-naive (phase-shift-from-numerov u-naive h (:r-boundary test-params) (:e test-params) l)
                             error-bessel (Math/abs (- delta-bessel delta-exact))
                             error-naive (Math/abs (- delta-naive delta-exact))
                             improvement (if (> error-bessel 0) (/ error-naive error-bessel) 0.0)]
                         {:l l
                          :error-naive error-naive
                          :error-bessel error-bessel
                          :improvement improvement})
                       (catch Exception e
                         {:l l
                          :error-naive Double/NaN
                          :error-bessel Double/NaN
                          :improvement 0.0})))
                   l-values)]
  (println (format "%-8s %-20s %-20s %-15s" "l" "Naive Error" "Bessel Error" "Improvement"))
  (println (apply str (repeat 75 "-")))
  (doseq [r results]
    (if (Double/isNaN (:error-naive r))
      (println (format "%-8d %-20s %-20s %-15s" 
                      (:l r) "N/A" "N/A" "N/A"))
      (println (format "%-8d %-20.6e %-20.6e %-15.2f" 
                      (:l r) (:error-naive r) (:error-bessel r) (:improvement r)))))
  (println ""))

;; ============================================
;; 5. Convergence Rate Analysis
;; ============================================
(println "5. Convergence Rate Analysis")
(println "==============================")
(let [h-values [0.1 0.05 0.025 0.01 0.005]
      table (phase-shift-convergence-table 
              (:e test-params) (:l test-params)
              (:v0 test-params) (:rad test-params) 
              (:diff test-params) h-values (:r-boundary test-params))
      ;; Calculate convergence rates (slope on log-log plot)
      calc-rate (fn [errors]
                  (if (< (count errors) 2)
                    Double/NaN
                    (let [log-h (map #(Math/log %) h-values)
                          log-err (map #(Math/log %) errors)
                          n (count errors)
                          sum-x (reduce + log-h)
                          sum-y (reduce + log-err)
                          sum-xy (reduce + (map * log-h log-err))
                          sum-x2 (reduce + (map #(* % %) log-h))
                          slope (/ (- (* n sum-xy) (* sum-x sum-y))
                                   (- (* n sum-x2) (* sum-x sum-x)))]
                      slope)))]
  (println "Bessel Start Convergence Rate:")
  (let [bessel-errors (map :bessel-error table)
        rate (calc-rate bessel-errors)]
    (println (format "  Estimated order: %.2f (theoretical: 6.0)" rate)))
  (println "Naive Start Convergence Rate:")
  (let [naive-errors (map :naive-error table)
        rate (calc-rate naive-errors)]
    (println (format "  Estimated order: %.2f" rate)))
  (println ""))

(println "=== DATA GENERATION COMPLETE ===")

