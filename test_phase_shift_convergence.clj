;; Test Phase Shift Convergence Table
;; Generate the table from numerical_riccati.tex paper

(require '[functions :refer :all])

;; Test case from numerical_riccati.tex paper
(def test-params
  {:e 2.0      ; MeV
   :l 1
   :v0 46.23   ; MeV
   :rad 2.0    ; fm
   :diff 0.5   ; fm
   :r-boundary 10.0 ; fm (boundary for phase shift extraction)
   })

(println "=== PHASE SHIFT CONVERGENCE ANALYSIS ===")
(println "Parameters from numerical_riccati.tex:")
(println "  E = 2.0 MeV, l = 1")
(println "  Woods-Saxon: V₀ = 46.23 MeV, R = 2.0 fm, a = 0.5 fm")
(println "  Boundary radius: 10.0 fm")
(println "")

;; Calculate exact phase shift (reference)
(println "Calculating exact phase shift (h = 0.001)...")
(let [delta-exact (exact-phase-shift-numerov 
                    (:e test-params) (:l test-params)
                    (:v0 test-params) (:rad test-params) 
                    (:diff test-params) (:r-boundary test-params))]
  (println (format "Exact phase shift: %.10f radians (%.6f degrees)" 
                  delta-exact (* delta-exact (/ 180.0 Math/PI))))
  (println ""))

;; Generate convergence table
(println "=== CONVERGENCE TABLE ===")
(let [h-values [0.1 0.05 0.01]
      table (phase-shift-convergence-table 
              (:e test-params) (:l test-params)
              (:v0 test-params) (:rad test-params) 
              (:diff test-params) h-values (:r-boundary test-params))]
  
  (print-convergence-table table)
  
  ;; Detailed results
  (println "=== DETAILED RESULTS ===")
  (doseq [row table]
    (println (format "h = %.2f fm:" (:h row)))
    (println (format "  Naive Start:   δ = %.10f, Error = %.6e" 
                    (:naive-phase-shift row) (:naive-error row)))
    (println (format "  Bessel Start:  δ = %.10f, Error = %.6e" 
                    (:bessel-phase-shift row) (:bessel-error row)))
    (println (format "  Exact:         δ = %.10f" (:exact-phase-shift row)))
    (println "")))

;; Individual phase shift extraction test
(println "=== INDIVIDUAL PHASE SHIFT EXTRACTION TEST ===")
(let [h 0.01
      u-bessel (solve-numerov 2.0 1 46.23 2.0 0.5 h 10.0)
      u-naive (solve-numerov-naive 2.0 1 46.23 2.0 0.5 h 10.0)
      delta-bessel (phase-shift-from-numerov u-bessel h 10.0 2.0 1)
      delta-naive (phase-shift-from-numerov u-naive h 10.0 2.0 1)
      R-bessel (r-matrix-from-numerov u-bessel h 10.0)
      R-naive (r-matrix-from-numerov u-naive h 10.0)]
  (println (format "For h = %.2f fm:" h))
  (println (format "  Bessel Start R-matrix: %.10f" R-bessel))
  (println (format "  Bessel Start Phase Shift: %.10f rad (%.6f deg)" 
                  delta-bessel (* delta-bessel (/ 180.0 Math/PI))))
  (println (format "  Naive Start R-matrix: %.10f" R-naive))
  (println (format "  Naive Start Phase Shift: %.10f rad (%.6f deg)" 
                  delta-naive (* delta-naive (/ 180.0 Math/PI)))))

