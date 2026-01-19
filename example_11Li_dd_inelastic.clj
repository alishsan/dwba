;; Example: 11Li(d,d') Inelastic Scattering - Monopole (L=0) "Breathing" Mode
;;
;; Calculation of inelastic scattering cross section for 11Li(d,d') reaction
;; at E/A = 7.1 MeV exciting the 2.09 MeV L=0 monopole state.
;;
;; Reference: "A measurement of scattering of 11Li on a deuteron target at beam 
;; energy of E/A = 7.1 MeV (at the center of the target) was performed at the 
;; IRIS facility at TRIUMF. The differential cross section of the 2.09 MeV state 
;; when interpreted by distorted wave Born approximation calculation (DWBA) shows 
;; the first evidence of soft monopole (L = 0) excitation."
;;
;; Ground state: 11Li is 3/2^-

(require '[dwba.inelastic :as inel])
(require '[functions :refer [mass-factor differential-cross-section total-cross-section s-matrix phase-shift solve-numerov]])
(require '[fastmath.core :as m])
(require '[fastmath.polynomials :as poly])
(require '[complex :refer [mag re im arg add mul div subt complex-from-cartesian]])

;; ============================================================================
;; Reaction Parameters
;; ============================================================================

(println "=== 11Li(d,d') Inelastic Scattering - Monopole (L=0) Excitation ===")
(println "")

;; Reaction: 11Li(d,d')
;; Beam energy: E/A = 7.1 MeV (energy per nucleon)
;; For deuteron projectile: E_d = 7.1 * 2 = 14.2 MeV (lab frame)
(def E-lab 14.2)  ; Lab frame energy (MeV)

;; Excitation energy
(def E-ex 2.09)  ; Monopole state at 2.09 MeV

;; Multipolarity
(def lambda 0)  ; Monopole (L=0, "breathing" mode)
(def mu 0)      ; Axially symmetric

;; Ground state: 11Li is 3/2^-
;; For inelastic scattering, we typically use L=0 in entrance channel
;; and L=0 in exit channel (monopole transition)

;; ============================================================================
;; Nuclear Parameters
;; ============================================================================

;; Woods-Saxon parameters for 11Li (estimated)
;; 11Li is a halo nucleus, so we need appropriate parameters
;; Typical values for light nuclei:
(def V-params [50.0 2.5 0.6])  ; [V0 (MeV), R0 (fm), a0 (fm)]
;; R0 ≈ 1.2 * A^(1/3) ≈ 1.2 * 11^(1/3) ≈ 2.5 fm

;; Deformation parameter for monopole (L=0) "breathing" mode
;; For monopole excitations, β_0 is typically smaller than β_2
;; Typical range: β_0 ≈ 0.05 - 0.15 for soft monopole modes
;; For 11Li (halo nucleus), the breathing mode might be enhanced
(def beta-0 0.10)  ; Monopole deformation parameter (to be adjusted)

(println "Reaction Parameters:")
(println (format "  Reaction: 11Li(d,d')"))
(println (format "  Beam energy: E/A = 7.1 MeV → E_d = %.1f MeV (lab)" E-lab))
(println (format "  Excitation energy: E_ex = %.2f MeV" E-ex))
(println (format "  Multipolarity: L = %d (monopole, 'breathing' mode)" lambda))
(println (format "  Ground state: 3/2^-"))
(println "")

(println "Nuclear Parameters:")
(println (format "  Woods-Saxon: V₀ = %.1f MeV, R₀ = %.2f fm, a₀ = %.2f fm"
                (first V-params) (second V-params) (last V-params)))
(println (format "  Monopole deformation: β₀ = %.3f" beta-0))
(println "")

;; ============================================================================
;; Mass and Kinematics
;; ============================================================================

;; Masses (in MeV/c²)
(def m-d 1875.6)  ; Deuteron mass
(def m-Li11 10252.0)  ; 11Li mass (approximate)

;; Reduced mass for d+11Li system
(def mu-reduced (/ (* m-d m-Li11) (+ m-d m-Li11)))  ; MeV/c²

;; Mass factor: 2μ/ħ² (in MeV⁻¹·fm⁻²)
;; ħc = 197.7 MeV·fm
(def mass-f (/ (* 2.0 mu-reduced) (* 197.7 197.7)))

(println "Kinematics:")
(println (format "  Deuteron mass: %.1f MeV/c²" m-d))
(println (format "  11Li mass: %.1f MeV/c²" m-Li11))
(println (format "  Reduced mass: μ = %.1f MeV/c²" mu-reduced))
(println (format "  Mass factor: 2μ/ħ² = %.4e MeV⁻¹·fm⁻²" mass-f))
(println "")

;; Calculate CM frame energy from lab frame
;; For projectile on target: E_CM = (m_target / (m_target + m_projectile)) * E_lab
(def E-CM (* E-lab (/ m-Li11 (+ m-Li11 m-d))))

(println (format "  Lab energy: E_lab = %.2f MeV" E-lab))
(println (format "  CM energy: E_CM = %.2f MeV" E-CM))
(println "")

;; ============================================================================
;; Distorted Waves
;; ============================================================================

(println "=== Calculating Distorted Waves ===")

(def h 0.01)  ; Step size (fm)
(def r-max 30.0)  ; Maximum radius (fm) - larger for halo nucleus

;; Entrance channel: elastic scattering (L=0)
;; Use E_CM for the calculation
(def L-i 0)  ; Orbital angular momentum in entrance channel
(def chi-i (inel/distorted-wave-entrance E-CM L-i V-params h r-max))

(println "Entrance channel:")
(println (format "  Energy: E_i = %.2f MeV (CM)" E-CM))
(println (format "  Angular momentum: L_i = %d" L-i))
(println (format "  Wavefunction length: %d points" (count chi-i)))
(println "")

;; Exit channel: inelastic scattering (L=0 for monopole)
;; Exit energy: E_f = E_i - E_ex
(def L-f 0)  ; For monopole, L_f = L_i = 0
(def chi-f (inel/distorted-wave-exit E-CM E-ex L-f V-params h r-max))

(println "Exit channel:")
(println (format "  Energy: E_f = E_i - E_ex = %.2f MeV (CM)" (- E-CM E-ex)))
(println (format "  Angular momentum: L_f = %d" L-f))
(println (format "  Wavefunction length: %d points" (count chi-f)))
(println "")

;; ============================================================================
;; Transition Form Factor (Monopole L=0)
;; ============================================================================

(println "=== Transition Form Factor (Monopole L=0) ===")

;; Calculate transition form factor at a few points
(println "Transition form factor F_0(r) = β₀ · R₀ · dV/dr:")
(doseq [r [1.0 2.0 3.0 5.0 10.0]]
  (let [F-0 (inel/transition-form-factor r lambda beta-0 V-params)]
    (println (format "  F₀(%.1f fm) = %.4e MeV/fm" r F-0))))
(println "")

;; ============================================================================
;; Inelastic Scattering Amplitude
;; ============================================================================

(println "=== Inelastic Scattering Amplitude ===")

;; Calculate inelastic amplitude
(def T-inel (inel/inelastic-amplitude chi-i chi-f lambda mu beta-0 V-params r-max h))

(println "Inelastic scattering amplitude:")
(if (number? T-inel)
  (println (format "  T_inel = %.4e" T-inel))
  (println (format "  T_inel = %.4e + i%.4e" (re T-inel) (im T-inel))))
(println "")

;; ============================================================================
;; Differential Cross Section
;; ============================================================================

(println "=== Differential Cross Section ===")

;; Calculate wavenumbers
(def k-i (Math/sqrt (* mass-f E-CM)))
(def E-f (- E-CM E-ex))
(def k-f (Math/sqrt (* mass-f E-f)))

(println "Wavenumbers:")
(println (format "  k_i (entrance): %.4f fm⁻¹" k-i))
(println (format "  k_f (exit): %.4f fm⁻¹" k-f))
(println (format "  Ratio k_f/k_i: %.4f" (/ k-f k-i)))
(println "")

;; Calculate differential cross-section
(def dsigma-dOmega (inel/inelastic-differential-cross-section T-inel k-i k-f E-CM E-ex mass-f))

(println "Differential cross-section:")
(println (format "  dσ/dΩ = %.4e fm²/sr" dsigma-dOmega))
(println (format "  dσ/dΩ = %.4e mb/sr (1 mb = 10 fm²)" (* dsigma-dOmega 10.0)))
(println "")

;; ============================================================================
;; Comparison with Complete Function
;; ============================================================================

(println "=== Verification: Complete Calculation ===")

(def dsigma-complete (inel/inelastic-cross-section chi-i chi-f lambda mu beta-0 V-params 
                                                   E-CM E-ex r-max h mass-f))

(println "Complete calculation:")
(println (format "  dσ/dΩ = %.4e fm²/sr" dsigma-complete))
(println (format "  dσ/dΩ = %.4e mb/sr" (* dsigma-complete 10.0)))
(println "")

;; ============================================================================
;; Sensitivity to Deformation Parameter
;; ============================================================================

(println "=== Sensitivity to β₀ Parameter ===")

(println "Differential cross-section as function of β₀:")
(doseq [beta-test [0.05 0.10 0.15 0.20]]
  (let [T-test (inel/inelastic-amplitude chi-i chi-f lambda mu beta-test V-params r-max h)
        dsigma-test (inel/inelastic-differential-cross-section T-test k-i k-f E-CM E-ex mass-f)]
    (println (format "  β₀ = %.2f: dσ/dΩ = %.4e mb/sr" beta-test (* dsigma-test 10.0)))))
(println "")

;; ============================================================================
;; Elastic Scattering Cross Section
;; ============================================================================

(println "=== Elastic Scattering Cross Section ===")

;; For elastic scattering, we use the S-matrix formalism
;; The differential cross section is: dσ/dΩ = |f(θ)|²
;; where f(θ) is the scattering amplitude

(println "Elastic scattering (d+11Li):")
(println (format "  CM energy: E_CM = %.2f MeV" E-CM))
(println (format "  Using Woods-Saxon: V₀ = %.1f MeV, R₀ = %.2f fm, a₀ = %.2f fm"
                (first V-params) (second V-params) (last V-params)))
(println "")

;; Calculate S-matrix and phase shift for L=0
;; Note: The s-matrix and phase-shift functions use the global mass-factor
;; We need to temporarily set it or calculate directly

;; For elastic scattering, the amplitude is:
;; f(θ) = (1/(2ik)) Σ_L (2L+1) P_L(cos θ) (S_L - 1)

;; Calculate S-matrix for L=0 using our parameters
;; We'll use a simplified approach: calculate phase shift from the wavefunction
;; at large r, then S = exp(2iδ)

;; Extract phase shift from chi-i at large r
;; For large r, u(r) ~ sin(kr + δ_L) for L=0
;; We can extract δ from the asymptotic form

(def r-asymptotic (* 0.9 r-max))  ; Use 90% of r-max for asymptotic region
(def idx-asymptotic (int (/ r-asymptotic h)))
(def u-asymptotic (nth chi-i idx-asymptotic))
(def u-asymptotic-prev (nth chi-i (dec idx-asymptotic)))
(def r-asymptotic-val (* idx-asymptotic h))
(def r-asymptotic-prev (* (dec idx-asymptotic) h))

;; Estimate phase shift from wavefunction (simplified)
;; For L=0, u(r) ~ sin(kr + δ) at large r
;; This is a rough estimate - full calculation needs proper asymptotic matching

(println "Elastic scattering (L=0 partial wave):")
(println (format "  Wavenumber: k = %.4f fm⁻¹" k-i))
(println (format "  Wavefunction at r=%.1f fm: %.4e" r-asymptotic-val u-asymptotic))
(println "  Note: Full phase shift extraction requires asymptotic matching")
(println "")

;; For a proper elastic cross section calculation, we need:
;; 1. S-matrix for all partial waves (L=0, 1, 2, ...)
;; 2. Sum over all L: f(θ) = (1/(2ik)) Σ_L (2L+1) P_L(cos θ) (S_L - 1)
;; 3. dσ/dΩ = |f(θ)|²

;; Simplified calculation: Use the fact that for L=0 only:
;; f(θ) = (1/(2ik)) (S_0 - 1)  (since P_0(cos θ) = 1)
;; This gives isotropic distribution

(println "Elastic differential cross section (L=0 only, simplified):")
(println "  Note: This is an approximation using only L=0 partial wave")
(println "  Full calculation requires summing over all L values")
(println "")

;; Calculate elastic cross section using the entrance channel wavefunction
;; The elastic amplitude is related to the scattering wavefunction
;; For a rough estimate, we can use the fact that elastic scattering
;; is typically much larger than inelastic

(println "Comparison: Elastic vs Inelastic")
(println (format "  Inelastic (L=0 monopole): dσ/dΩ = %.4e mb/sr" (* dsigma-dOmega 10.0)))
(println "  Elastic: dσ/dΩ >> Inelastic (typically 10³-10⁶ times larger)")
(println "  Note: Full elastic calculation requires S-matrix for all L")
(println "")

;; For proper elastic calculation, we would need to:
;; 1. Calculate S-matrix for L = 0, 1, 2, ..., L_max
;; 2. Sum over all L to get f(θ)
;; 3. Calculate dσ/dΩ = |f(θ)|²

;; Calculate elastic scattering using S-matrix approach
;; Note: The functions in functions.clj use global mass-factor
;; We need to adapt the calculation for our d+11Li system

(def L-max-elastic 4)  ; Maximum L for elastic calculation

(println "Calculating elastic scattering (L=0 to L=" L-max-elastic "):")

;; For elastic scattering, the amplitude is:
;; f(θ) = (1/(2ik)) Σ_L (2L+1) P_L(cos θ) (S_L - 1)
;; dσ/dΩ = |f(θ)|²

;; We need to calculate S-matrix for each L
;; The s-matrix function uses global mass-factor, so we need to adapt
;; For now, we'll calculate a simplified version

;; Calculate elastic amplitude at different angles
(defn elastic-dsigma [theta]
  "Calculate elastic differential cross section at angle theta"
  (let [k k-i
        cos-theta (m/cos theta)
        ;; Sum over partial waves (simplified: use L=0 only for now)
        ;; Full calculation would sum L=0 to L_max
        L 0
        ;; For L=0, we need S_0
        ;; Simplified: Estimate from wavefunction or use a model value
        ;; For a rough estimate, we'll use the fact that elastic is much larger
        ;; than inelastic and typically has forward peak
        
        ;; Placeholder: Would calculate S-matrix for each L
        ;; For now, return a simplified estimate
        ]
    ;; Simplified estimate (would need proper S-matrix calculation)
    ;; Elastic cross section is typically 10³-10⁶ times inelastic
    (* dsigma-dOmega 1e4)))  ; Rough estimate: 10⁴ times inelastic

(println "")
(println "Elastic differential cross section (simplified estimate):")
(println "  Note: This uses a simplified model. Full calculation requires:")
(println "    - S-matrix calculation for all partial waves")
(println "    - Proper mass factor for d+11Li system")
(println "    - Coulomb effects (Z1*Z2 = 1*3 = 3)")
(println "")

(println "Elastic vs Inelastic comparison (at θ=90°):")
(println (format "  Inelastic (L=0 monopole): dσ/dΩ = %.4e mb/sr" (* dsigma-dOmega 10.0)))
(let [elastic-estimate (* dsigma-dOmega 1e4)]
  (println (format "  Elastic (estimated):      dσ/dΩ ≈ %.4e mb/sr" (* elastic-estimate 10.0)))
  (println (format "  Ratio (elastic/inelastic): ≈ %.0e" 1e4)))
(println "")

(println "Elastic scattering characteristics:")
(println "  - Forward peak: dσ/dΩ(0°) >> dσ/dΩ(180°)")
(println "  - Oscillatory angular distribution")
(println "  - Includes both nuclear and Coulomb scattering")
(println "  - Typically 10³-10⁶ times larger than inelastic")
(println "")
(println "  To calculate properly:")
(println "    1. Calculate S-matrix for L = 0, 1, 2, ..., L_max")
(println "    2. Sum: f(θ) = (1/(2ik)) Σ_L (2L+1) P_L(cos θ) (S_L - 1)")
(println "    3. dσ/dΩ = |f(θ)|²")
(println "    4. Use functions.clj:differential-cross-section with adapted parameters")
(println "")

;; ============================================================================
;; Angular Distribution
;; ============================================================================

(println "=== Angular Distribution ===")

;; For monopole (L=0), the angular distribution should be isotropic (constant)
;; However, we calculate it at various angles to verify and show the method

(println "Angular distribution dσ/dΩ(θ) for monopole (L=0) excitation:")
(println "")
(println "Angle (deg) | Angle (rad) | dσ/dΩ (mb/sr)")
(println "------------|-------------|---------------")

(doseq [theta-deg [0 15 30 45 60 75 90 105 120 135 150 165 180]]
  (let [theta (* theta-deg (/ Math/PI 180.0))
        dsigma (inel/inelastic-angular-distribution T-inel theta k-i k-f 
                                                   E-CM E-ex mass-factor lambda mu)]
    (println (format "  %3d      |  %6.4f     |  %.4e" 
                    theta-deg theta (* dsigma 10.0)))))
(println "")

;; Calculate angular distribution function for plotting
(def theta-values (map #(* % (/ Math/PI 18)) (range 19)))  ; 0 to π in 10° steps
(def angular-dist (inel/inelastic-angular-distribution-function T-inel theta-values 
                                                                 k-i k-f E-CM E-ex 
                                                                 mass-factor lambda mu))

(println "Angular distribution data points (first 5):")
(doseq [[theta dsigma] (take 5 angular-dist)]
  (let [theta-deg (* theta (/ 180.0 Math/PI))]
    (println (format "  θ=%.1f°: dσ/dΩ = %.4e mb/sr" theta-deg (* dsigma 10.0)))))
(println "")

;; Check if distribution is isotropic (should be constant for L=0)
(def dsigma-values (map second angular-dist))
(def dsigma-mean (/ (reduce + dsigma-values) (count dsigma-values)))
(def dsigma-std (Math/sqrt (/ (reduce + (map #(let [diff (- % dsigma-mean)]
                                               (* diff diff)) dsigma-values))
                             (count dsigma-values))))

(println "Isotropy check (for L=0, should be constant):")
(println (format "  Mean dσ/dΩ: %.4e mb/sr" (* dsigma-mean 10.0)))
(println (format "  Std deviation: %.4e mb/sr" (* dsigma-std 10.0)))
(println (format "  Relative variation: %.2f%%" (* 100.0 (/ dsigma-std dsigma-mean))))
(if (< (/ dsigma-std dsigma-mean) 0.01)
  (println "  ✓ Distribution is isotropic (as expected for L=0)")
  (println "  ⚠️  Distribution shows angular dependence (may indicate numerical issues)"))
(println "")

;; ============================================================================
;; Legendre Polynomial Expansion
;; ============================================================================

(println "=== Legendre Polynomial Expansion ===")

;; For monopole (L=0), the angular distribution should only have P_0 term
;; Calculate Legendre coefficients
(def L-max 4)
(def legendre-coeffs (inel/legendre-coefficients angular-dist L-max))

(println "Legendre expansion coefficients:")
(println "  (For L=0 monopole, only a_0 should be non-zero)")
(doseq [[L a-L] (sort legendre-coeffs)]
  (println (format "  a_%d = %.4e" L a-L)))
(println "")

;; Check if higher-order terms are small (as expected for L=0)
(def a0 (get legendre-coeffs 0 0.0))
(def higher-order-sum (reduce + (map second (filter #(not= (first %) 0) legendre-coeffs))))

(println "Coefficient analysis:")
(println (format "  a_0 (monopole): %.4e" a0))
(println (format "  Sum of higher-order terms: %.4e" higher-order-sum))
(if (< (Math/abs higher-order-sum) (* 0.01 (Math/abs a0)))
  (println "  ✓ Only P_0 term is significant (as expected for L=0)")
  (println "  ⚠️  Higher-order terms present (may indicate numerical issues or L≠0)"))
(println "")

;; ============================================================================
;; Notes
;; ============================================================================

(println "=== Notes ===")
(println "1. This is a simplified DWBA calculation for monopole (L=0) excitation")
(println "2. The deformation parameter β₀ may need adjustment to match experimental data")
(println "3. For 11Li (halo nucleus), the breathing mode may be enhanced")
(println "4. The calculation assumes collective model for the monopole excitation")
(println "5. For L=0 monopole, the angular distribution should be isotropic (constant)")
(println "6. Full calculation would include:")
(println "   - Proper optical model parameters for d+11Li")
(println "   - Coupled channels effects")
(println "   - Halo structure effects")
(println "   - More sophisticated transition form factor for breathing mode")
(println "")

(println "=== Calculation Complete ===")

