;; Regenerate table values using the exact Riccati-Bessel function

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def r-boundary 10.0)

(println "=== REGENERATING TABLE VALUES WITH EXACT RICCATI-BESSEL ===\n")

;; Test phase shift convergence
(println "PHASE SHIFT CONVERGENCE TABLE")
(println "==============================")
(let [delta-exact (exact-phase-shift-numerov e l v0 rad diff r-boundary)]
  (println (format "Exact phase shift (h=0.0001): %.10f rad\n" delta-exact))
  
  (doseq [h [0.1 0.05 0.01]]
    (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
          u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
          delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
          delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)
          error-bessel (Math/abs (- delta-bessel delta-exact))
          error-naive (Math/abs (- delta-naive delta-exact))]
      (println (format "h = %.2f fm:" h))
      (println (format "  Naive error:  %.6e" error-naive))
      (println (format "  Bessel error: %.6e" error-bessel))
      (println ""))))

;; Test Wronskian stability
(println "\nWRONSKIAN STABILITY TABLE")
(println "=========================")
(doseq [h [0.1 0.05 0.01]]
  (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
        u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
        w-bessel (check-wronskian u-bessel e l v0 rad diff h)
        w-naive (check-wronskian u-naive e l v0 rad diff h)
        w-b-first (first w-bessel)
        w-n-first (first w-naive)
        w-b-drift (apply max (map #(Math/abs (- % w-b-first)) w-bessel))
        w-n-drift (apply max (map #(Math/abs (- % w-n-first)) w-naive))]
    (println (format "h = %.2f fm:" h))
    (println (format "  Naive drift:  %.6e" w-n-drift))
    (println (format "  Bessel drift: %.6e" w-b-drift))
    (println "")))

