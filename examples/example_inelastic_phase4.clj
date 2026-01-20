;; Example: Phase 4 - Inelastic Scattering Amplitude
;;
;; This demonstrates the calculation of inelastic scattering amplitudes
;; and differential cross-sections.

(require '[dwba.inelastic :as inel])
(require '[functions :refer [mass-factor]])

;; ============================================================================
;; Example 1: Distorted Waves
;; ============================================================================

(println "=== Example 1: Distorted Waves ===")

;; Define parameters
(def V-params [50.0 2.0 0.6])  ; [V0, R0, a0]
(def E-incident 10.0)  ; Incident energy (MeV)
(def E-ex 4.44)  ; Excitation energy (MeV) - first 2⁺ state in ¹²C
(def h 0.01)  ; Step size (fm)
(def r-max 20.0)  ; Maximum radius (fm)

;; Calculate distorted wave for entrance channel (elastic, L=0)
(def chi-i (inel/distorted-wave-entrance E-incident 0 V-params h r-max))
(println "Entrance channel distorted wave (L=0):")
(println "  Length:" (count chi-i))
(println "  First few values:" (take 5 chi-i))
(println "  Value at r=2.0 fm:" (nth chi-i (int (/ 2.0 h))))
(println "")

;; Calculate distorted wave for exit channel (inelastic, L=2)
(def chi-f (inel/distorted-wave-exit E-incident E-ex 2 V-params h r-max))
(println "Exit channel distorted wave (L=2, E_ex=" E-ex "MeV):")
(println "  Length:" (count chi-f))
(println "  First few values:" (take 5 chi-f))
(println "  Value at r=2.0 fm:" (nth chi-f (int (/ 2.0 h))))
(println "")

;; ============================================================================
;; Example 2: Transition Potential
;; ============================================================================

(println "=== Example 2: Transition Potential ===")

(def lambda 2)  ; Quadrupole
(def beta-2 0.25)  ; β_2 for ¹²C

;; Calculate transition potential (radial only)
(println "Transition potential V_transition(r) = F_2(r):")
(doseq [r [1.0 2.0 3.0 5.0]]
  (let [V-trans (inel/transition-potential-radial r lambda 0 beta-2 V-params)]
    (println (format "  V_trans(%.1f fm) = %.4f MeV" r V-trans))))
(println "")

;; With angular dependence
(println "Transition potential with angular dependence:")
(let [r 2.0
      theta (/ Math/PI 2)
      phi 0.0
      V-trans-angular (inel/transition-potential-radial r lambda 0 beta-2 V-params theta phi)]
  (println (format "  V_trans(%.1f fm, θ=π/2, φ=0) = %.4f MeV" r V-trans-angular)))
(println "")

;; ============================================================================
;; Example 3: Inelastic Scattering Amplitude
;; ============================================================================

(println "=== Example 3: Inelastic Scattering Amplitude ===")

;; Calculate inelastic amplitude
(def T-inel (inel/inelastic-amplitude chi-i chi-f lambda 0 beta-2 V-params r-max h))
(println "Inelastic scattering amplitude:")
(if (number? T-inel)
  (println "  T_inel =" T-inel)
  (println "  T_inel =" (format "%.4f + i%.4f" (re T-inel) (im T-inel))))
(println "")

;; ============================================================================
;; Example 4: Differential Cross-Section
;; ============================================================================

(println "=== Example 4: Differential Cross-Section ===")

;; Calculate wavenumbers
(def k-i (Math/sqrt (* mass-factor E-incident)))
(def E-f (- E-incident E-ex))
(def k-f (Math/sqrt (* mass-factor E-f)))

(println "Wavenumbers:")
(println "  k_i (entrance):" (format "%.4f" k-i) "fm⁻¹")
(println "  k_f (exit):" (format "%.4f" k-f) "fm⁻¹")
(println "")

;; Calculate differential cross-section
(def dsigma-dOmega (inel/inelastic-differential-cross-section T-inel k-i k-f E-incident E-ex mass-factor))
(println "Differential cross-section:")
(println "  dσ/dΩ =" (format "%.4e" dsigma-dOmega) "fm²/sr")
(println "  dσ/dΩ =" (format "%.4e" (* dsigma-dOmega 10.0)) "mb/sr (1 mb = 10 fm²)")
(println "")

;; ============================================================================
;; Example 5: Complete Calculation
;; ============================================================================

(println "=== Example 5: Complete Inelastic Cross-Section Calculation ===")

;; Use convenience function
(def dsigma-complete (inel/inelastic-cross-section chi-i chi-f lambda 0 beta-2 V-params 
                                                   E-incident E-ex r-max h mass-factor))
(println "Complete calculation:")
(println "  dσ/dΩ =" (format "%.4e" dsigma-complete) "fm²/sr")
(println "  dσ/dΩ =" (format "%.4e" (* dsigma-complete 10.0)) "mb/sr")
(println "")

;; ============================================================================
;; Example 6: Different Multipole Orders
;; ============================================================================

(println "=== Example 6: Different Multipole Orders ===")

;; Compare quadrupole (λ=2) vs octupole (λ=3)
(def beta-3 0.1)  ; β_3 for octupole

(def T-inel-2 (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 V-params r-max h))
(def T-inel-3 (inel/inelastic-amplitude chi-i chi-f 3 0 beta-3 V-params r-max h))

(println "Inelastic amplitudes:")
(if (number? T-inel-2)
  (println "  T_inel (λ=2):" (format "%.4e" T-inel-2))
  (println "  T_inel (λ=2):" (format "%.4e + i%.4e" (re T-inel-2) (im T-inel-2))))
(if (number? T-inel-3)
  (println "  T_inel (λ=3):" (format "%.4e" T-inel-3))
  (println "  T_inel (λ=3):" (format "%.4e + i%.4e" (re T-inel-3) (im T-inel-3))))

(def dsigma-2 (inel/inelastic-differential-cross-section T-inel-2 k-i k-f E-incident E-ex mass-factor))
(def dsigma-3 (inel/inelastic-differential-cross-section T-inel-3 k-i k-f E-incident E-ex mass-factor))

(println "Differential cross-sections:")
(println "  dσ/dΩ (λ=2):" (format "%.4e" dsigma-2) "fm²/sr")
(println "  dσ/dΩ (λ=3):" (format "%.4e" dsigma-3) "fm²/sr")
(println "  Ratio (λ=2/λ=3):" (format "%.2f" (/ dsigma-2 dsigma-3)))
(println "")

(println "=== Examples Complete ===")
