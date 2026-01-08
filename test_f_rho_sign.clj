(require '[dwba.transfer :as t])

;; Test the sign of f-rho for bound states
;; For bound states, at large r where V → 0, we need f(r) < 0 for exponential decay

(let [v0 350.0
      rad 2.0
      diff 0.6
      l 1
      ;; Test at large r (rho = 10, which is r = 20 fm)
      rho-large 10.0
      ;; Test different energies
      energies [-50.0 -100.0 -200.0 -300.0]
      lambda (* t/mass-factor v0 rad rad)]
  (println "=== Testing f-rho Sign Convention ===")
  (println (format "V0=%.1f MeV, R0=%.1f fm, a0=%.1f fm" v0 rad diff))
  (println (format "lambda = %.2e" lambda))
  (println (format "Testing at large rho = %.1f (r = %.1f fm)" rho-large (* rho-large rad)))
  (println "")
  
  (doseq [E energies]
    (let [epsilon (/ E v0)
          f-rho (t/f-rho-numerov-dimensionless rho-large epsilon l lambda (/ diff rad))
          ;; At large r, V → 0, so v-eff ≈ l(l+1)/rho^2
          v-eff-large (/ (* l (inc l)) (* rho-large rho-large))
          expected-f (* lambda (- v-eff-large epsilon))]
      (println (format "E = %.1f MeV (epsilon = %.4f)" E epsilon))
      (println (format "  f(rho=%.1f) = %.2e" rho-large f-rho))
      (println (format "  v-eff ≈ %.4f (centrifugal only)" v-eff-large))
      (println (format "  (v-eff - epsilon) = %.4f" (- v-eff-large epsilon)))
      (println (format "  Sign: %s" (if (< f-rho 0) "NEGATIVE (decay)" "POSITIVE (growing)")))
      (println "")))
  
  (println "=== Analysis ===")
  (println "For bound states, at large r where V → 0:")
  (println "  f(rho) = lambda * (v-eff - epsilon)")
  (println "  For decay, we need f(rho) < 0")
  (println "  This requires: (v-eff - epsilon) < 0")
  (println "  Since epsilon < 0 for bound states, this means:")
  (println "    v-eff < epsilon (epsilon is more negative)")
  (println "  At large r, v-eff ≈ 0, so we need epsilon < 0 (which is true)")
  (println "  But (0 - epsilon) = -epsilon > 0, so f(rho) > 0")
  (println "")
  (println "This suggests the sign convention might be wrong!"))
