;; Debug why phase shifts are identical despite different wavefunctions

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)
(def r-boundary 10.0)

(println "=== DEBUGGING BOUNDARY VALUES ===\n")

;; Check wavefunction values at boundary
(let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
      {:keys [u u-prime r index]} (extract-wavefunction-at-boundary u-bessel h r-boundary)
      {:keys [u u-prime r index] :as naive-extract} (extract-wavefunction-at-boundary u-naive h r-boundary)]
  
  (println "At boundary (r = 10.0 fm):")
  (println (format "  Bessel: u = %.10e, u' = %.10e" 
                  (:u (extract-wavefunction-at-boundary u-bessel h r-boundary))
                  (:u-prime (extract-wavefunction-at-boundary u-bessel h r-boundary))))
  (println (format "  Naive:  u = %.10e, u' = %.10e" 
                  (:u naive-extract)
                  (:u-prime naive-extract)))
  (println (format "  Difference in u:  %.10e" 
                  (Math/abs (- (:u (extract-wavefunction-at-boundary u-bessel h r-boundary))
                              (:u naive-extract)))))
  (println (format "  Difference in u': %.10e" 
                  (Math/abs (- (:u-prime (extract-wavefunction-at-boundary u-bessel h r-boundary))
                              (:u-prime naive-extract)))))
  (println "")
  
  ;; Check R-matrices
  (let [R-bessel (r-matrix-from-numerov u-bessel h r-boundary)
        R-naive (r-matrix-from-numerov u-naive h r-boundary)]
    (println (format "R-matrix Bessel: %.10e" R-bessel))
    (println (format "R-matrix Naive:  %.10e" R-naive))
    (println (format "Difference:      %.10e" (Math/abs (- R-bessel R-naive))))
    (println ""))
  
  ;; Check if ratio u/u' is the same
  (let [ratio-bessel (/ (:u (extract-wavefunction-at-boundary u-bessel h r-boundary))
                        (:u-prime (extract-wavefunction-at-boundary u-bessel h r-boundary)))
        ratio-naive (/ (:u naive-extract) (:u-prime naive-extract))]
    (println (format "u/u' ratio Bessel: %.10e" ratio-bessel))
    (println (format "u/u' ratio Naive:  %.10e" ratio-naive))
    (println (format "Difference:        %.10e" (Math/abs (- ratio-bessel ratio-naive))))
    (println "")))

;; Check wavefunction values at different radii
(println "=== WAVEFUNCTION VALUES AT DIFFERENT RADII ===")
(doseq [r-check [5.0 8.0 10.0 12.0]]
  (let [u-bessel (solve-numerov e l v0 rad diff h r-check)
        u-naive (solve-numerov-naive e l v0 rad diff h r-check)
        {:keys [u u-prime]} (extract-wavefunction-at-boundary u-bessel h r-check)
        {:keys [u u-prime] :as naive-extract} (extract-wavefunction-at-boundary u-naive h r-check)]
    (println (format "r = %.1f fm:" r-check))
    (println (format "  Bessel: u = %.8e, u' = %.8e" 
                    (:u (extract-wavefunction-at-boundary u-bessel h r-check))
                    (:u-prime (extract-wavefunction-at-boundary u-bessel h r-check))))
    (println (format "  Naive:  u = %.8e, u' = %.8e" 
                    (:u naive-extract)
                    (:u-prime naive-extract)))
    (println (format "  u difference: %.8e" 
                    (Math/abs (- (:u (extract-wavefunction-at-boundary u-bessel h r-check))
                                (:u naive-extract)))))
    (println "")))

;; Compare with R-matrix method
(println "=== COMPARISON WITH R-MATRIX METHOD ===")
(let [V [v0 rad diff]
      delta-rmatrix (phase-shift0 e V r-boundary l)
      u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      delta-numerov (phase-shift-from-numerov u-bessel h r-boundary e l)]
  (println (format "R-matrix method:    %.10f rad (%.2f deg)" 
                  delta-rmatrix (* delta-rmatrix (/ 180.0 Math/PI))))
  (println (format "Numerov (direct):   %.10f rad (%.2f deg)" 
                  delta-numerov (* delta-numerov (/ 180.0 Math/PI))))
  (println (format "Difference:         %.10f rad" (Math/abs (- delta-rmatrix delta-numerov))))
  (println "")
  (println "Note: R-matrix method uses different integration, so some difference is expected"))

