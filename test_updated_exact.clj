;; Test with updated exact reference (very fine Numerov grid)

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def r-boundary 10.0)

(println "=== TESTING WITH UPDATED EXACT REFERENCE ===\n")
(println "Using very fine Numerov grid (h=0.0001) as exact reference\n")

;; Test at each step size from paper
(doseq [h [0.1 0.05 0.01]]
  (println (format "=== h = %.2f fm ===" h))
  
  (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
        u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
        delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
        delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)
        delta-exact (exact-phase-shift-numerov e l v0 rad diff r-boundary)
        error-bessel (Math/abs (- delta-bessel delta-exact))
        error-naive (Math/abs (- delta-naive delta-exact))
        diff-phase (Math/abs (- delta-bessel delta-naive))]
    
    (println (format "Exact phase shift (h=0.0001): %.10f rad" delta-exact))
    (println (format "Bessel phase shift: %.10f rad, error: %.6e" delta-bessel error-bessel))
    (println (format "Naive phase shift:  %.10f rad, error: %.6e" delta-naive error-naive))
    (println (format "Difference (Bessel - Naive): %.6e rad" diff-phase))
    (println "")
    
    ;; Compare with paper
    (let [paper-errors {0.1 {:naive 2.4e-3 :bessel 1.1e-5}
                        0.05 {:naive 6.1e-4 :bessel 6.8e-7}
                        0.01 {:naive 2.5e-5 :bessel 7.5e-10}}
          paper-naive (get-in paper-errors [h :naive])
          paper-bessel (get-in paper-errors [h :bessel])]
      (when paper-naive
        (println "Paper claims:")
        (println (format "  Naive error: %.6e (we got: %.6e, ratio: %.2f)" 
                         paper-naive error-naive (/ error-naive paper-naive)))
        (println (format "  Bessel error: %.6e (we got: %.6e, ratio: %.2f)" 
                        paper-bessel error-bessel (/ error-bessel paper-bessel)))
        (println "")))))

