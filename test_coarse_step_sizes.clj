;; Test at coarser step sizes where differences should be visible

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def r-boundary 10.0)

(println "=== TESTING AT COARSE STEP SIZES ===\n")
(println "Paper shows differences at h=0.1, 0.05, 0.01\n")

;; Test at each step size from paper
(doseq [h [0.1 0.05 0.01]]
  (println (format "=== h = %.2f fm ===" h))
  
  (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
        u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
        delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
        delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)
        V [v0 rad diff]
        delta-exact (phase-shift0 e V r-boundary l)
        error-bessel (Math/abs (- delta-bessel delta-exact))
        error-naive (Math/abs (- delta-naive delta-exact))
        diff-phase (Math/abs (- delta-bessel delta-naive))]
    
    (println (format "Exact phase shift: %.10f rad" delta-exact))
    (println (format "Bessel phase shift: %.10f rad, error: %.6e" delta-bessel error-bessel))
    (println (format "Naive phase shift:  %.10f rad, error: %.6e" delta-naive error-naive))
    (println (format "Difference: %.6e rad" diff-phase))
    (println "")
    
    ;; Check Wronskian
    (let [w-bessel (check-wronskian u-bessel e l v0 rad diff h)
          w-naive (check-wronskian u-naive e l v0 rad diff h)
          w-b-first (first w-bessel)
          w-n-first (first w-naive)
          w-b-drift (apply max (map #(Math/abs (- % w-b-first)) w-bessel))
          w-n-drift (apply max (map #(Math/abs (- % w-n-first)) w-naive))]
      (println (format "Wronskian drift - Bessel: %.6e, Naive: %.6e" w-b-drift w-n-drift))
      (println ""))
    
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

