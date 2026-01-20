;; Example: Phase 5 - Single-Particle Excitations
;;
;; This demonstrates particle-hole excitations and transition densities
;; for single-particle inelastic scattering.

(require '[dwba.inelastic :as inel])
(require '[dwba.transfer :as transfer])
(require '[functions :refer [mass-factor]])

;; ============================================================================
;; Example 1: Particle-Hole State Definition
;; ============================================================================

(println "=== Example 1: Particle-Hole State Definition ===")

;; Define a particle-hole excitation
;; Example: Promoting a nucleon from 1s_{1/2} (hole) to 2p_{3/2} (particle)
(def ph-state (inel/particle-hole-state 2 1 3 1 0 1))
(println "Particle-hole state:")
(println "  Particle:" (:particle ph-state))
(println "  Hole:" (:hole ph-state))
(println "")

;; ============================================================================
;; Example 2: Bound State Wavefunctions
;; ============================================================================

(println "=== Example 2: Bound State Wavefunctions ===")

;; Define Woods-Saxon potential
(def V-params [50.0 2.0 0.6])  ; [V0, R0, a0]
(def r-max 20.0)
(def h 0.01)

;; Solve for particle state: 2p (n=2 means 1 radial node, l=1)
(println "Solving for particle state (2p)...")
(def phi-particle-result (transfer/solve-bound-state V-params 2 1 nil r-max h))
(def phi-particle (:normalized-wavefunction phi-particle-result))
(println "  Energy:" (format "%.4f" (:energy phi-particle-result)) "MeV")
(println "  Nodes:" (:nodes phi-particle-result))
(println "")

;; Solve for hole state: 1s (n=1 means 0 radial nodes, l=0)
(println "Solving for hole state (1s)...")
(def phi-hole-result (transfer/solve-bound-state V-params 1 0 nil r-max h))
(def phi-hole (:normalized-wavefunction phi-hole-result))
(println "  Energy:" (format "%.4f" (:energy phi-hole-result)) "MeV")
(println "  Nodes:" (:nodes phi-hole-result))
(println "")

;; ============================================================================
;; Example 3: Transition Density
;; ============================================================================

(println "=== Example 3: Transition Density ===")

;; Calculate transition density: ρ_trans(r) = φ*_particle(r) · φ_hole(r)
(def rho-trans (inel/transition-density phi-particle phi-hole h))
(println "Transition density:")
(println "  Length:" (count rho-trans))
(println "  First few values:" (take 5 rho-trans))
(println "  Value at r=2.0 fm:" (nth rho-trans (int (/ 2.0 h))))
(println "")

;; Transition density as function of r
(def rho-trans-fn (inel/transition-density-function phi-particle phi-hole r-max h))
(println "Transition density function (first 5 points):")
(doseq [[r rho] (take 5 rho-trans-fn)]
  (println (format "  r=%.2f fm: ρ_trans=%.4e" r rho)))
(println "")

;; ============================================================================
;; Example 4: Form Factor from Transition Density
;; ============================================================================

(println "=== Example 4: Form Factor from Transition Density ===")

;; Calculate form factor for quadrupole (λ=2) transition
(def lambda 2)
(def F-lambda (inel/transition-form-factor-from-density rho-trans lambda V-params r-max h))
(println "Form factor F_λ(r) from transition density:")
(doseq [r [1.0 2.0 3.0 5.0]]
  (let [idx (int (/ r h))
        F-val (nth F-lambda idx)]
    (println (format "  F_%d(%.1f fm) = %.4e" lambda r F-val))))
(println "")

;; ============================================================================
;; Example 5: Spectroscopic Factor
;; ============================================================================

(println "=== Example 5: Spectroscopic Factor ===")

;; Calculate spectroscopic factor
(def S (inel/spectroscopic-factor 3 1))  ; Particle: j=1 (2j+1=3), Hole: j=0 (2j+1=1)
(println "Spectroscopic factor:")
(println "  S =" (format "%.4f" S))
(println "")

;; With overlap integral
(def overlap (first (filter #(> (Math/abs %) 0.01) rho-trans)))  ; Example overlap
(def S-with-overlap (inel/spectroscopic-factor 3 1 overlap))
(println "Spectroscopic factor (with overlap):")
(println "  S =" (format "%.4f" S-with-overlap))
(println "")

;; ============================================================================
;; Example 6: Excitation Energy
;; ============================================================================

(println "=== Example 6: Excitation Energy ===")

;; Calculate excitation energy from single-particle energies
(def E-particle (:energy phi-particle-result))
(def E-hole (:energy phi-hole-result))
(def E-ex (inel/single-particle-excitation-energy E-particle E-hole))
(println "Excitation energy:")
(println "  E_particle:" (format "%.4f" E-particle) "MeV")
(println "  E_hole:" (format "%.4f" E-hole) "MeV")
(println "  E_ex:" (format "%.4f" E-ex) "MeV")
(println "")

;; ============================================================================
;; Example 7: Complete Particle-Hole Form Factor
;; ============================================================================

(println "=== Example 7: Complete Particle-Hole Form Factor ===")

;; Calculate complete form factor
(def F-ph (inel/particle-hole-form-factor phi-particle phi-hole lambda V-params r-max h))
(println "Particle-hole form factor:")
(doseq [r [1.0 2.0 3.0 5.0]]
  (let [idx (int (/ r h))
        F-val (nth F-ph idx)]
    (println (format "  F_%d(%.1f fm) = %.4e" lambda r F-val))))
(println "")

;; ============================================================================
;; Example 8: Single-Particle Inelastic Amplitude
;; ============================================================================

(println "=== Example 8: Single-Particle Inelastic Amplitude ===")

;; Calculate distorted waves
(def E-incident 10.0)
(def chi-i (inel/distorted-wave-entrance E-incident 0 V-params h r-max))
(def chi-f (inel/distorted-wave-exit E-incident E-ex 2 V-params h r-max))

;; Calculate inelastic amplitude using particle-hole form factor
(def T-inel-ph (inel/single-particle-inelastic-amplitude chi-i chi-f phi-particle phi-hole 
                                                          lambda V-params r-max h))
(println "Inelastic scattering amplitude (single-particle):")
(if (number? T-inel-ph)
  (println "  T_inel =" (format "%.4e" T-inel-ph))
  (println "  T_inel =" (format "%.4e + i%.4e" (re T-inel-ph) (im T-inel-ph))))
(println "")

;; ============================================================================
;; Example 9: Comparison with Collective Model
;; ============================================================================

(println "=== Example 9: Comparison with Collective Model ===")

;; Compare single-particle vs collective form factors
(def beta-collective 0.25)
(def F-collective (mapv #(inel/transition-form-factor % lambda beta-collective V-params)
                       (map #(* % h) (range (int (/ r-max h))))))

(println "Form factor comparison at r=2.0 fm:")
(let [idx (int (/ 2.0 h))
      F-ph-val (nth F-ph idx)
      F-col-val (nth F-collective idx)]
  (println (format "  Single-particle: F_%d(2.0) = %.4e" lambda F-ph-val))
  (println (format "  Collective:      F_%d(2.0) = %.4e" lambda F-col-val))
  (println (format "  Ratio:            %.2f" (/ F-ph-val F-col-val))))
(println "")

(println "=== Examples Complete ===")
