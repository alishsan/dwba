;; Example: Phase 3 - Collective Model Excitations for Inelastic Scattering
;;
;; This demonstrates the use of collective model (rotational and vibrational)
;; for describing nuclear excitations.

(require '[dwba.inelastic :as inel])

;; ============================================================================
;; Example 1: Spherical Harmonics
;; ============================================================================

(println "=== Example 1: Spherical Harmonics ===")

;; Calculate spherical harmonics Y_λμ(θ,φ)
(def theta (/ Math/PI 2))  ; π/2 radians (90 degrees)
(def phi 0.0)

(println "Y_20(π/2, 0):" (inel/spherical-harmonic 2 0 theta phi))
(println "Y_22(π/2, 0):" (inel/spherical-harmonic 2 2 theta phi))
(println "Y_30(π/2, 0):" (inel/spherical-harmonic 3 0 theta phi))
(println "")

;; ============================================================================
;; Example 2: Deformed Potential
;; ============================================================================

(println "=== Example 2: Deformed Potential ===")

;; Define potential parameters
(def V-params [50.0 2.0 0.6])  ; [V0, R0, a0]
(def beta-2 0.25)  ; Quadrupole deformation

;; Calculate deformed potential at different angles
(println "Deformed potential V(r,θ,φ) at r=2.0 fm:")
(doseq [theta-val [0.0 (/ Math/PI 4) (/ Math/PI 2) Math/PI]]
  (let [V-deformed (inel/deformed-potential 2.0 theta-val 2 0 beta-2 V-params)]
    (println (format "  V(2.0, %.2f, 0) = %.4f MeV" theta-val V-deformed))))
(println "")

;; ============================================================================
;; Example 3: Rotational Band Structure
;; ============================================================================

(println "=== Example 3: Rotational Band Structure ===")

;; Calculate rotational band for even-even nucleus (I_gs=0, K=0)
(def I-gs 0)
(def K 0)
(def E-bandhead 0.0)  ; Ground state band

(println "Rotational band energies (ground state band):")
(def rotational-band (inel/rotational-band-structure I-gs K E-bandhead 8))
(doseq [[I E] rotational-band]
  (println (format "  I=%d: E = %.4f MeV" I E)))
(println "")

;; First excited band (β-vibrational band)
(def E-beta-bandhead 0.8)  ; Bandhead at 0.8 MeV
(println "First excited rotational band (β-vibrational):")
(def beta-band (inel/rotational-band-structure I-gs K E-beta-bandhead 8))
(doseq [[I E] beta-band]
  (println (format "  I=%d: E = %.4f MeV" I E)))
(println "")

;; ============================================================================
;; Example 4: Vibrational Band Structure
;; ============================================================================

(println "=== Example 4: Vibrational Band Structure ===")

;; Calculate vibrational band for spherical nucleus
(def E-bandhead-vib 0.0)
(def phonon-energy 2.0)  ; ħω = 2.0 MeV

(println "Vibrational band energies (ħω = 2.0 MeV):")
(def vibrational-band (inel/vibrational-band-structure E-bandhead-vib phonon-energy 3))
(doseq [[n E] vibrational-band]
  (println (format "  n=%d phonons: E = %.4f MeV" n E)))
(println "")

;; ============================================================================
;; Example 5: Collective Excitation Type Classification
;; ============================================================================

(println "=== Example 5: Collective Excitation Type ===")

;; Classify different nuclei
(def beta-C12 (inel/deformation-parameter 2 :C12))
(def beta-Pb208 (inel/deformation-parameter 2 :Pb208))
(def beta-Sm154 (inel/deformation-parameter 2 :Sm154))

(println "Nucleus classification:")
(println (format "  ¹²C (β_2=%.3f): %s" beta-C12 (inel/collective-excitation-type beta-C12)))
(println (format "  ²⁰⁸Pb (β_2=%.3f): %s" beta-Pb208 (inel/collective-excitation-type beta-Pb208)))
(println (format "  ¹⁵⁴Sm (β_2=%.3f): %s" beta-Sm154 (inel/collective-excitation-type beta-Sm154)))
(println "")

;; ============================================================================
;; Example 6: Comparison of Rotational vs Vibrational
;; ============================================================================

(println "=== Example 6: Rotational vs Vibrational Models ===")

;; For a transitional nucleus (β_2 ≈ 0.2)
(def beta-transitional 0.2)
(println (format "Transitional nucleus (β_2=%.2f):" beta-transitional))
(println "  Classification:" (inel/collective-excitation-type beta-transitional))

;; Rotational model prediction
(println "  Rotational model (first few states):")
(def rot-prediction (inel/rotational-band-structure 0 0 0.0 6))
(doseq [[I E] (take 4 rot-prediction)]
  (println (format "    I=%d: E = %.4f MeV" I E)))

;; Vibrational model prediction
(println "  Vibrational model (first few states):")
(def vib-prediction (inel/vibrational-band-structure 0.0 2.0 3))
(doseq [[n E] (take 4 vib-prediction)]
  (println (format "    n=%d: E = %.4f MeV" n E)))
(println "")

;; ============================================================================
;; Example 7: Deformed Potential Angular Dependence
;; ============================================================================

(println "=== Example 7: Angular Dependence of Deformed Potential ===")

;; Calculate deformed potential at different angles
(def r 2.0)
(def lambda 2)
(def mu 0)
(def beta 0.25)

(println (format "V(r=%.1f, θ, φ=0) for λ=%d, μ=%d, β=%.2f:" r lambda mu beta))
(doseq [theta-deg [0 30 45 60 90 120 135 150 180]]
  (let [theta-rad (* theta-deg (/ Math/PI 180.0))
        V-deformed (inel/deformed-potential r theta-rad lambda mu beta V-params)]
    (println (format "  θ=%3d°: V = %.4f MeV" theta-deg V-deformed))))
(println "")

(println "=== Examples Complete ===")
