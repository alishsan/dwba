;; Test direct matching phase shift formula

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)
(def r-boundary 10.0)

(println "=== TESTING DIRECT MATCHING FORMULA ===\n")

;; Test with Bessel start
(let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      {:keys [u u-prime r]} (extract-wavefunction-at-boundary u-bessel h r-boundary)
      k (m/sqrt (* mass-factor e))
      rho (* k r)
      j-l (f-func l rho)
      j-l-prime (f-func-deriv l rho)
      y-l (g-func l rho)
      y-l-prime (g-func-deriv l rho)]
  
  (println "Direct matching formula:")
  (println (format "  u(a) = %.10e" u))
  (println (format "  u'(a) = %.10e" u-prime))
  (println (format "  k = %.10e" k))
  (println (format "  rho = k*a = %.10e" rho))
  (println "")
  (println (format "  j_l(rho) = %.10e" j-l))
  (println (format "  j_l'(rho) = %.10e" j-l-prime))
  (println (format "  y_l(rho) = %.10e" y-l))
  (println (format "  y_l'(rho) = %.10e" y-l-prime))
  (println "")
  
  (let [numerator (- (* k u-prime j-l) (* u j-l-prime))
        denominator (- (* k u-prime y-l) (* u y-l-prime))
        delta (Math/atan2 numerator denominator)]
    (println (format "  numerator = %.10e" numerator))
    (println (format "  denominator = %.10e" denominator))
    (println (format "  delta = %.10f rad (%.2f deg)" delta (* delta (/ 180.0 Math/PI))))
    (println "")))

;; Compare Bessel vs Naive
(println "=== COMPARING BESSEL VS NAIVE ===")
(let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
      delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
      delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)]
  
  (println (format "Bessel: %.10f rad (%.2f deg)" 
                  delta-bessel (* delta-bessel (/ 180.0 Math/PI))))
  (println (format "Naive:  %.10f rad (%.2f deg)" 
                  delta-naive (* delta-naive (/ 180.0 Math/PI))))
  (println (format "Difference: %.10e rad" (Math/abs (- delta-bessel delta-naive))))
  (println "")
  
  (if (> (Math/abs (- delta-bessel delta-naive)) 1e-6)
    (println "✓ Phase shifts are DIFFERENT (as expected)")
    (println "⚠️  WARNING: Phase shifts are IDENTICAL!")))

;; Test at different boundary radii
(println "\n=== BOUNDARY RADIUS DEPENDENCE ===")
(doseq [r-bound [8.0 10.0 12.0 15.0 20.0]]
  (let [u-bessel (solve-numerov e l v0 rad diff h r-bound)
        delta (phase-shift-from-numerov u-bessel h r-bound e l)]
    (println (format "r = %5.1f fm: delta = %10.6f rad (%7.2f deg)" 
                    r-bound delta (* delta (/ 180.0 Math/PI))))))

