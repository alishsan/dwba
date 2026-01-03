;; Test phase shifts at different boundary radii to find where differences appear

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)

(println "=== TESTING PHASE SHIFTS AT DIFFERENT RADII ===\n")
(println "Looking for where naive and Bessel starts give different phase shifts\n")

;; Test at various radii
(doseq [r-bound [3.0 4.0 5.0 6.0 7.0 8.0 10.0 12.0]]
  (let [u-bessel (solve-numerov e l v0 rad diff h r-bound)
        u-naive (solve-numerov-naive e l v0 rad diff h r-bound)
        delta-bessel (phase-shift-from-numerov u-bessel h r-bound e l)
        delta-naive (phase-shift-from-numerov u-naive h r-bound e l)
        diff (Math/abs (- delta-bessel delta-naive))
        V [v0 rad diff]
        delta-exact (phase-shift0 e V r-bound l)
        error-bessel (Math/abs (- delta-bessel delta-exact))
        error-naive (Math/abs (- delta-naive delta-exact))]
    (println (format "r = %4.1f fm:" r-bound))
    (println (format "  Bessel: %.10f rad, error: %.6e" delta-bessel error-bessel))
    (println (format "  Naive:  %.10f rad, error: %.6e" delta-naive error-naive))
    (println (format "  Difference: %.6e rad" diff))
    (if (> diff 1e-8)
      (println "  ✓ DIFFERENT")
      (println "  ✗ IDENTICAL"))
    (println "")))

;; Check what the paper might be using
(println "=== CHECKING PAPER'S BOUNDARY ===\n")
(println "Paper might use a smaller boundary where differences are more visible.")
(println "Or might compare wavefunction values directly, not phase shifts.")

