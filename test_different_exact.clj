;; Test with different exact reference methods to match paper

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def r-boundary 10.0)

(println "=== TESTING DIFFERENT EXACT REFERENCES ===\n")

;; Test 1: Very fine Numerov (h=0.0001) - current
(println "1. Very fine Numerov (h=0.0001):")
(let [h-fine 0.0001
      u-fine (solve-numerov e l v0 rad diff h-fine r-boundary)
      delta-exact (phase-shift-from-numerov u-fine h-fine r-boundary e l)
      h-test 0.01
      u-bessel (solve-numerov e l v0 rad diff h-test r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h-test r-boundary)
      delta-bessel (phase-shift-from-numerov u-bessel h-test r-boundary e l)
      delta-naive (phase-shift-from-numerov u-naive h-test r-boundary e l)]
  (println (format "  Exact: %.10f rad" delta-exact))
  (println (format "  Bessel error: %.6e" (Math/abs (- delta-bessel delta-exact))))
  (println (format "  Naive error:  %.6e" (Math/abs (- delta-naive delta-exact))))
  (println ""))

;; Test 2: Fine Numerov (h=0.001) - paper might use this
(println "2. Fine Numerov (h=0.001) - paper's likely reference:")
(let [h-fine 0.001
      u-fine (solve-numerov e l v0 rad diff h-fine r-boundary)
      delta-exact (phase-shift-from-numerov u-fine h-fine r-boundary e l)
      h-test 0.01
      u-bessel (solve-numerov e l v0 rad diff h-test r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h-test r-boundary)
      delta-bessel (phase-shift-from-numerov u-bessel h-test r-boundary e l)
      delta-naive (phase-shift-from-numerov u-naive h-test r-boundary e l)]
  (println (format "  Exact: %.10f rad" delta-exact))
  (println (format "  Bessel error: %.6e" (Math/abs (- delta-bessel delta-exact))))
  (println (format "  Naive error:  %.6e" (Math/abs (- delta-naive delta-exact))))
  (println ""))

;; Test 3: R-matrix method
(println "3. R-matrix method:")
(let [V [v0 rad diff]
      delta-exact (phase-shift0 e V r-boundary l)
      h-test 0.01
      u-bessel (solve-numerov e l v0 rad diff h-test r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h-test r-boundary)
      delta-bessel (phase-shift-from-numerov u-bessel h-test r-boundary e l)
      delta-naive (phase-shift-from-numerov u-naive h-test r-boundary e l)]
  (println (format "  Exact: %.10f rad" delta-exact))
  (println (format "  Bessel error: %.6e" (Math/abs (- delta-bessel delta-exact))))
  (println (format "  Naive error:  %.6e" (Math/abs (- delta-naive delta-exact))))
  (println ""))

;; Test 4: Compare all at h=0.1, 0.05, 0.01
(println "=== COMPARISON AT DIFFERENT STEP SIZES ===\n")
(println "Using h=0.001 Numerov as exact reference (paper's likely method):\n")

(let [h-exact 0.001
      u-exact (solve-numerov e l v0 rad diff h-exact r-boundary)
      delta-exact (phase-shift-from-numerov u-exact h-exact r-boundary e l)]
  
  (doseq [h-test [0.1 0.05 0.01]]
    (let [u-bessel (solve-numerov e l v0 rad diff h-test r-boundary)
          u-naive (solve-numerov-naive e l v0 rad diff h-test r-boundary)
          delta-bessel (phase-shift-from-numerov u-bessel h-test r-boundary e l)
          delta-naive (phase-shift-from-numerov u-naive h-test r-boundary e l)
          error-bessel (Math/abs (- delta-bessel delta-exact))
          error-naive (Math/abs (- delta-naive delta-exact))]
      (println (format "h = %.2f fm:" h-test))
      (println (format "  Bessel error: %.6e" error-bessel))
      (println (format "  Naive error:  %.6e" error-naive))
      (println (format "  Paper Bessel: %.6e, Paper Naive: %.6e"
                      (get-in {0.1 {:bessel 1.1e-5 :naive 2.4e-3}
                               0.05 {:bessel 6.8e-7 :naive 6.1e-4}
                               0.01 {:bessel 7.5e-10 :naive 2.5e-5}} [h-test :bessel])
                      (get-in {0.1 {:bessel 1.1e-5 :naive 2.4e-3}
                               0.05 {:bessel 6.8e-7 :naive 6.1e-4}
                               0.01 {:bessel 7.5e-10 :naive 2.5e-5}} [h-test :naive])))
      (println ""))))

