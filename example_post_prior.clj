;; Example: Post and Prior Formulations for Transfer Reactions
;;
;; This demonstrates the post and prior formulations of DWBA transfer amplitudes.
;; Post and prior should give equivalent results (post-prior equivalence).

(require '[dwba.transfer :as t] :reload)
(require '[dwba.form-factors :as ff] :reload)
(require '[fastmath.core :as m])

;; ============================================================================
;; Helper Functions: Simple Distorted Waves
;; ============================================================================

(defn plane-wave
  "Simple plane wave approximation for distorted wave.
   
   For testing purposes, we use a plane wave: χ(r) = sin(kr) / (kr)
   This is a simplified form. In full DWBA, distorted waves are calculated
   by solving the Schrödinger equation with optical potentials.
   
   Parameters:
   - k: Wave number (fm⁻¹)
   - r: Radial distance (fm)
   
   Returns: Distorted wave value at r"
  [k r]
  (if (zero? r)
    1.0
    (/ (Math/sin (* k r)) (* k r))))

(defn distorted-wave-vector
  "Generate a vector of distorted wave values.
   
   Parameters:
   - k: Wave number (fm⁻¹)
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: Vector of distorted wave values"
  [k r-max h]
  (let [n (int (/ r-max h))]
    (mapv (fn [i]
           (let [r (* i h)]
             (plane-wave k r)))
         (range n))))

;; ============================================================================
;; Setup: Bound States and Parameters
;; ============================================================================

(println "=== Post and Prior Formulations for Transfer Reactions ===")
(println "")

;; Define potential parameters for initial and final states
(def mu-i 884.3)  ; Reduced mass for initial channel
(def mu-f 469.46) ; Reduced mass for final channel
(def m-f-i (/ (m/sqrt (* 2. mu-i)) 197.7))
(def m-f-f (/ (m/sqrt (* 2. mu-f)) 197.7))

(def V-params-i [60.0 2.7 0.6])  ; Initial state: [V0, R0, a0]
(def V-params-f [50.0 1.66 0.6]) ; Final state: [V0, R0, a0]
(def energy-i -15.66)  ; Initial bound state energy (MeV)
(def energy-f -2.21)  ; Final bound state energy (MeV)

;; Calculate bound state wavefunctions
(println "Calculating bound state wavefunctions...")
(def u-i-raw (t/solve-bound-state-numerov energy-i 0 V-params-i m-f-i))
(def u-f-raw (t/solve-bound-state-numerov energy-f 0 V-params-f m-f-f))
(def phi-i (t/normalize-bound-state u-i-raw 0.01))
(def phi-f (t/normalize-bound-state u-f-raw 0.01))

(println (format "Initial bound state: E = %.2f MeV, length = %d points" 
                energy-i (count phi-i)))
(println (format "Final bound state: E = %.2f MeV, length = %d points" 
                energy-f (count phi-f)))
(println "")

;; Integration parameters
(def r-max 20.0)
(def h 0.01)

;; Wave numbers for distorted waves (from kinetic energy)
;; For testing, use typical values
(def E-lab-i 50.0)  ; Lab energy in entrance channel (MeV)
(def E-lab-f 48.0)  ; Lab energy in exit channel (MeV)

;; Convert to wave numbers: k = sqrt(2μE) / ℏc
(def k-i (/ (m/sqrt (* 2. mu-i E-lab-i)) 197.7))  ; fm⁻¹
(def k-f (/ (m/sqrt (* 2. mu-f E-lab-f)) 197.7))  ; fm⁻¹

(println "Distorted wave parameters:")
(println (format "  Entrance channel: k_i = %.3f fm⁻¹ (E_lab = %.1f MeV)" k-i E-lab-i))
(println (format "  Exit channel: k_f = %.3f fm⁻¹ (E_lab = %.1f MeV)" k-f E-lab-f))
(println "")

;; Generate distorted waves
(def chi-i (distorted-wave-vector k-i r-max h))
(def chi-f (distorted-wave-vector k-f r-max h))

(println (format "Distorted waves: length = %d points" (count chi-i)))
(println "")

;; ============================================================================
;; Example 1: Zero-Range Interaction (Post and Prior)
;; ============================================================================

(println "=== Example 1: Zero-Range Interaction ===")
(println "")

(def D0 (t/zero-range-constant :d-p))
(println (format "Zero-range constant D₀: %.1f MeV·fm^(3/2)" D0))
(println "")

;; Post formulation
(def T-post-zero (t/transfer-amplitude-post chi-i chi-f phi-i phi-f 
                                            r-max h :zero-range D0))

;; Prior formulation
(def T-prior-zero (t/transfer-amplitude-prior chi-i chi-f phi-i phi-f 
                                               r-max h :zero-range D0))

(println "Zero-Range Results:")
(println (format "  Post amplitude:  T_post  = %.6f" T-post-zero))
(println (format "  Prior amplitude: T_prior = %.6f" T-prior-zero))
(println (format "  Difference:      %.6e" (Math/abs (- T-post-zero T-prior-zero))))
(println (format "  Ratio:           %.6f" (/ T-post-zero T-prior-zero)))
(println "")

;; ============================================================================
;; Example 2: Finite-Range Interaction (Yukawa) - Post and Prior
;; ============================================================================

(println "=== Example 2: Finite-Range Interaction (Yukawa) ===")
(println "")

(def V0-finite 50.0)  ; Interaction strength (MeV)
(def mu-range 0.7)    ; Range parameter (fm⁻¹)
(def finite-params {:V0 V0-finite 
                    :form-factor :yukawa 
                    :range-param mu-range})

(println "Finite-range parameters:")
(println (format "  V₀ = %.1f MeV" V0-finite))
(println (format "  μ = %.2f fm⁻¹ (Yukawa)" mu-range))
(println "")

;; Post formulation
(def T-post-finite (t/transfer-amplitude-post chi-i chi-f phi-i phi-f 
                                               r-max h :finite-range finite-params))

;; Prior formulation
(def T-prior-finite (t/transfer-amplitude-prior chi-i chi-f phi-i phi-f 
                                                 r-max h :finite-range finite-params))

(println "Finite-Range (Yukawa) Results:")
(println (format "  Post amplitude:  T_post  = %.6f" T-post-finite))
(println (format "  Prior amplitude: T_prior = %.6f" T-prior-finite))
(println (format "  Difference:      %.6e" (Math/abs (- T-post-finite T-prior-finite))))
(println (format "  Ratio:           %.6f" (/ T-post-finite T-prior-finite)))
(println "")

;; ============================================================================
;; Example 3: Comparison of Zero-Range vs Finite-Range
;; ============================================================================

(println "=== Example 3: Zero-Range vs Finite-Range Comparison ===")
(println "")

(println "Post Formulation:")
(println (format "  Zero-range:  T = %.6f" T-post-zero))
(println (format "  Finite-range: T = %.6f" T-post-finite))
(println (format "  Ratio (FR/ZR): %.4f" (/ T-post-finite T-post-zero)))
(println "")

(println "Prior Formulation:")
(println (format "  Zero-range:  T = %.6f" T-prior-zero))
(println (format "  Finite-range: T = %.6f" T-prior-finite))
(println (format "  Ratio (FR/ZR): %.4f" (/ T-prior-finite T-prior-zero)))
(println "")

;; ============================================================================
;; Example 4: Post-Prior Equivalence Check
;; ============================================================================

(println "=== Example 4: Post-Prior Equivalence ===")
(println "")

(def tolerance 1e-6)
(def post-prior-diff-zero (Math/abs (- T-post-zero T-prior-zero)))
(def post-prior-diff-finite (Math/abs (- T-post-finite T-prior-finite)))

(println "Equivalence Check:")
(println (format "  Zero-range: |T_post - T_prior| = %.6e" post-prior-diff-zero))
(if (< post-prior-diff-zero tolerance)
  (println "  ✓ Post and Prior are equivalent (within tolerance)")
  (println "  ⚠️  Post and Prior differ (may be due to numerical precision)"))
(println "")

(println (format "  Finite-range: |T_post - T_prior| = %.6e" post-prior-diff-finite))
(if (< post-prior-diff-finite tolerance)
  (println "  ✓ Post and Prior are equivalent (within tolerance)")
  (println "  ⚠️  Post and Prior differ (may be due to numerical precision)"))
(println "")

(println "Note:")
(println "  - Post and Prior should be mathematically equivalent")
(println "  - Small differences may arise from numerical integration")
(println "  - In full DWBA, distorted waves come from optical model calculations")
(println "  - This example uses simplified plane-wave distorted waves for testing")
(println "")

(println "=== Examples Complete ===")

