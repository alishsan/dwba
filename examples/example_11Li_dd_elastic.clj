;; Example: 11Li(d,d) Elastic Scattering
;;
;; Calculation of elastic scattering cross section for 11Li(d,d) reaction
;; at E/A = 7.1 MeV.
;;
;; This uses the S-matrix formalism from functions.clj to calculate
;; elastic differential cross sections.

(require '[functions :refer [differential-cross-section total-cross-section s-matrix phase-shift mass-factor mass-factor-from-mu Z1Z2ee]])
(require '[fastmath.core :as m])
(require '[fastmath.polynomials :as poly])
(require '[complex :refer [mag re im arg complex-conjugate subt]])

;; ============================================================================
;; Reaction Parameters
;; ============================================================================

(println "=== 11Li(d,d) Elastic Scattering ===")
(println "")

;; Reaction: 11Li(d,d) - elastic scattering
;; Beam energy: E/A = 7.1 MeV (energy per nucleon)
;; For deuteron projectile: E_d = 7.1 * 2 = 14.2 MeV (lab frame)
(def E-lab 14.2)  ; Lab frame energy (MeV)

;; ============================================================================
;; Nuclear Parameters
;; ============================================================================

;; Woods-Saxon parameters for 11Li (same as inelastic calculation)
(def V-params [50.0 2.5 0.6])  ; [V0 (MeV), R0 (fm), a0 (fm)]

(println "Reaction Parameters:")
(println (format "  Reaction: 11Li(d,d) - elastic scattering"))
(println (format "  Beam energy: E/A = 7.1 MeV → E_d = %.1f MeV (lab)" E-lab))
(println "")

(println "Nuclear Parameters:")
(println (format "  Woods-Saxon: V₀ = %.1f MeV, R₀ = %.2f fm, a₀ = %.2f fm"
                (first V-params) (second V-params) (last V-params)))
(println "")

;; ============================================================================
;; Mass and Kinematics
;; ============================================================================

;; Masses (in MeV/c²)
(def m-d 1875.6)  ; Deuteron mass
(def m-Li11 10252.0)  ; 11Li mass (approximate)

;; Reduced mass for d+11Li system
(def mu-reduced (/ (* m-d m-Li11) (+ m-d m-Li11)))  ; MeV/c²

;; Calculate CM frame energy from lab frame
;; For projectile on target: E_CM = (m_target / (m_target + m_projectile)) * E_lab
(def E-CM (* E-lab (/ m-Li11 (+ m-Li11 m-d))))

(println "Kinematics:")
(println (format "  Deuteron mass: %.1f MeV/c²" m-d))
(println (format "  11Li mass: %.1f MeV/c²" m-Li11))
(println (format "  Reduced mass: μ = %.1f MeV/c²" mu-reduced))
(println (format "  Lab energy: E_lab = %.2f MeV" E-lab))
(println (format "  CM energy: E_CM = %.2f MeV" E-CM))
(println "")

;; We bind mass-factor and Z1Z2ee below for d+11Li so the calculation uses the correct kinematics.
;;
;; OPTICAL POTENTIAL SUPPORT:
;; For more realistic elastic scattering calculations with optical potentials
;; (including imaginary, spin-orbit, and Coulomb terms), you can use the
;; optical potential functions from dwba.transfer. However, the current
;; S-matrix calculation in functions.clj uses real potentials only.
;; To use optical potentials, you would need to:
;; 1. Calculate distorted waves using dwba.transfer/distorted-wave-optical
;; 2. Extract S-matrix from the asymptotic behavior of the distorted waves
;; This is more complex and requires modifying the S-matrix extraction.

;; ============================================================================
;; S-Matrix and Phase Shifts
;; ============================================================================

;; Bind reaction-specific mass-factor and Z1Z2ee for d+11Li (μ = 1585.5 MeV/c², Z1*Z2 = 3)
(binding [mass-factor (mass-factor-from-mu mu-reduced)
          Z1Z2ee (* 3 1.44)]
  (do
    (println "=== S-Matrix and Phase Shifts ===")

    ;; Calculate S-matrix and phase shifts for different partial waves
    (def L-max 10)  ; Maximum L to include

    (println (format "Calculating S-matrix and phase shifts for L = 0 to %d:" L-max))
(println "")

;; Calculate for first few L values
(doseq [L (range (inc (min 5 L-max)))]
  (try
    (let [S-val (s-matrix E-CM V-params L)
          delta (phase-shift E-CM V-params L)
          S-mag (mag S-val)
          S-arg (arg S-val)]
      (println (format "  L = %d:" L))
      (println (format "    S = %.4f + i%.4f" (re S-val) (im S-val)))
      (println (format "    |S| = %.4f, arg(S) = %.4f rad" S-mag S-arg))
      (println (format "    Phase shift: δ = %.4f rad (%.2f°)" delta (* delta (/ 180.0 Math/PI))))
      (println ""))
    (catch Exception e
      (println (format "  L = %d: Error calculating S-matrix: %s" L (.getMessage e)))
      (println ""))))

;; ============================================================================
;; Differential Cross Section
;; ============================================================================

(println "=== Differential Cross Section ===")

;; Calculate differential cross section at various angles
(println "Elastic differential cross section dσ/dΩ(θ):")
(println "")
(println "Angle (deg) | Angle (rad) | dσ/dΩ (fm²/sr) | dσ/dΩ (mb/sr)")
(println "------------|-------------|-----------------|---------------")

(doseq [theta-deg [0 15 30 45 60 75 90 105 120 135 150 165 180]]
  (try
    (let [theta (* theta-deg (/ Math/PI 180.0))
          dsigma-complex (differential-cross-section E-CM V-params theta L-max)
          dsigma (mag dsigma-complex)]  ; Take magnitude for cross section
      (println (format "  %3d      |  %6.4f     |  %.4e      |  %.4e"
                      theta-deg theta dsigma (* dsigma 10.0))))
    (catch Exception e
      (println (format "  %3d      |  %6.4f     |  Error: %s" 
                      theta-deg (* theta-deg (/ Math/PI 180.0)) (.getMessage e))))))
(println "")

;; ============================================================================
;; Angular Distribution Analysis
;; ============================================================================

(println "=== Angular Distribution Analysis ===")

;; Calculate at forward and backward angles
(def theta-0 0.0)
(def theta-90 (/ Math/PI 2))
(def theta-180 Math/PI)

(def dsigma-0 (mag (differential-cross-section E-CM V-params theta-0 L-max)))
(def dsigma-90 (mag (differential-cross-section E-CM V-params theta-90 L-max)))
(def dsigma-180 (mag (differential-cross-section E-CM V-params theta-180 L-max)))

(println "Key angles:")
(println (format "  Forward (0°):   dσ/dΩ = %.4e mb/sr" (* dsigma-0 10.0)))
(println (format "  Perpendicular (90°): dσ/dΩ = %.4e mb/sr" (* dsigma-90 10.0)))
(println (format "  Backward (180°): dσ/dΩ = %.4e mb/sr" (* dsigma-180 10.0)))
(println "")
(println (format "  Forward/Backward ratio: %.2e" (/ dsigma-0 dsigma-180)))
(println (format "  Forward/90° ratio: %.2e" (/ dsigma-0 dsigma-90)))
(println "")

;; ============================================================================
;; Total Cross Section
;; ============================================================================

(println "=== Total Cross Section ===")

(def sigma-total (total-cross-section E-CM V-params L-max))

(println (format "Total cross section: σ_total = %.4e fm²" sigma-total))
(println (format "Total cross section: σ_total = %.4e mb (1 mb = 10 fm²)" (* sigma-total 10.0)))
(println "")

;; ============================================================================
;; Partial Wave Contributions
;; ============================================================================

(println "=== Partial Wave Contributions ===")

(println "Cross section contribution from each partial wave:")
(println "L  | |S-1|²  | Contribution (fm²)")
(println "---|--------|-------------------")

(let [k (m/sqrt (* mass-factor E-CM))]
  (doseq [L (range (inc L-max))]
    (let [S-val (s-matrix E-CM V-params L)
          S-minus-1 (subt S-val 1.0)
          S-minus-1-mag (mag S-minus-1)
          ;; Cross section contribution: σ_L = (π/k²) (2L+1) |S_L - 1|²
          sigma-L (* (/ Math/PI (* k k)) (inc (* 2 L)) (* S-minus-1-mag S-minus-1-mag))]
      (println (format "%2d | %.4f | %.4e" L S-minus-1-mag sigma-L)))))
(println "")

;; ============================================================================
;; Convergence Check
;; ============================================================================

(println "=== Convergence Check ===")

;; Check convergence by comparing results with different L_max
(println "Checking convergence with different L_max values:")
(println "L_max | dσ/dΩ(90°) (mb/sr)")
(println "------|-------------------")

(doseq [L-test [2 4 6 8 10 12]]
  (let [dsigma-test (mag (differential-cross-section E-CM V-params theta-90 L-test))]
    (println (format "  %2d   | %.4e" L-test (* dsigma-test 10.0)))))
(println "")

;; ============================================================================
;; Notes
;; ============================================================================

(println "=== Notes ===")
(println "1. This calculation uses the S-matrix formalism from functions.clj")
(println "2. mass-factor and Z1Z2ee are bound for d+11Li (not the default p+n)")
(println "3. For d+11Li, Z1*Z2 = 1*3 = 3 (Coulomb effects included)")
(println "4. The calculation includes both nuclear and Coulomb scattering")
(println "5. Elastic cross sections are typically much larger than inelastic")
(println "6. The angular distribution shows forward peak and oscillations")
(println "")

    (println "=== Calculation Complete ===")))

