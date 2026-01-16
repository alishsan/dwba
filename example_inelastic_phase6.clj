;; Example: Phase 6 - Angular Distribution
;;
;; This demonstrates angular distribution calculations for inelastic scattering,
;; including Legendre polynomial expansions and interference terms.

(require '[dwba.inelastic :as inel])
(require '[functions :refer [mass-factor]])

;; ============================================================================
;; Example 1: Clebsch-Gordan Coefficients
;; ============================================================================

(println "=== Example 1: Clebsch-Gordan Coefficients ===")

;; Calculate Clebsch-Gordan coefficient <j1 m1 j2 m2 | J M>
;; Example: Coupling two angular momenta j1=1, j2=1 to total J=2
(def CG (inel/clebsch-gordan 1 0 1 0 2 0))
(println "Clebsch-Gordan coefficient <1 0 1 0 | 2 0>:")
(println "  CG =" (format "%.4f" CG))
(println "")

;; Check selection rules
(def CG-invalid (inel/clebsch-gordan 1 0 1 0 3 0))  ; J=3 violates triangle inequality
(println "Invalid coupling (J=3 violates triangle inequality):")
(println "  CG =" (format "%.4f" CG-invalid))
(println "")

;; ============================================================================
;; Example 2: Legendre Polynomial Expansion
;; ============================================================================

(println "=== Example 2: Legendre Polynomial Expansion ===")

;; Expand a function in Legendre polynomials
;; Example: f(θ) = 1.0·P_0(cos θ) + 0.5·P_2(cos θ)
(def coeffs {0 1.0, 2 0.5})
(def theta-test (/ Math/PI 2))
(def f-expanded (inel/legendre-expansion coeffs theta-test))
(println "Legendre expansion:")
(println "  Coefficients:" coeffs)
(println (format "  f(π/2) = %.4f" f-expanded))
(println "")

;; Vector format
(def coeffs-vec [1.0 0.0 0.5])  ; [a_0, a_1, a_2]
(def f-vec (inel/legendre-expansion coeffs-vec theta-test))
(println "Legendre expansion (vector format):")
(println "  Coefficients:" coeffs-vec)
(println (format "  f(π/2) = %.4f" f-vec))
(println "")

;; ============================================================================
;; Example 3: Angular Distribution
;; ============================================================================

(println "=== Example 3: Angular Distribution ===")

;; Define parameters
(def V-params [50.0 2.0 0.6])
(def E-incident 10.0)
(def E-ex 4.44)
(def h 0.01)
(def r-max 20.0)
(def beta-2 0.25)
(def lambda 2)

;; Calculate distorted waves
(def chi-i (inel/distorted-wave-entrance E-incident 0 V-params h r-max))
(def chi-f (inel/distorted-wave-exit E-incident E-ex 2 V-params h r-max))

;; Calculate inelastic amplitude (constant for now)
(def T-inel (inel/inelastic-amplitude chi-i chi-f lambda 0 beta-2 V-params r-max h))

;; Calculate wavenumbers
(def k-i (Math/sqrt (* mass-factor E-incident)))
(def E-f (- E-incident E-ex))
(def k-f (Math/sqrt (* mass-factor E-f)))

;; Calculate angular distribution at different angles
(println "Angular distribution dσ/dΩ(θ):")
(doseq [theta-deg [0 30 60 90 120 150 180]]
  (let [theta (* theta-deg (/ Math/PI 180))
        dsigma (inel/inelastic-angular-distribution T-inel theta k-i k-f 
                                                   E-incident E-ex mass-factor)]
    (println (format "  θ=%3d°: dσ/dΩ = %.4e fm²/sr" theta-deg dsigma))))
(println "")

;; ============================================================================
;; Example 4: Angular Distribution Function
;; ============================================================================

(println "=== Example 4: Angular Distribution Function ===")

;; Calculate angular distribution at multiple angles
(def theta-values (map #(* % (/ Math/PI 18)) (range 19)))  ; 0 to π in 10° steps
(def angular-dist (inel/inelastic-angular-distribution-function T-inel theta-values 
                                                                 k-i k-f E-incident E-ex 
                                                                 mass-factor))
(println "Angular distribution (first 5 points):")
(doseq [[theta dsigma] (take 5 angular-dist)]
  (let [theta-deg (* theta (/ 180 Math/PI))]
    (println (format "  θ=%.1f°: dσ/dΩ = %.4e fm²/sr" theta-deg dsigma))))
(println "")

;; ============================================================================
;; Example 5: Legendre Coefficients
;; ============================================================================

(println "=== Example 5: Legendre Coefficients ===")

;; Calculate Legendre expansion coefficients from angular distribution
(def L-max 4)
(def legendre-coeffs (inel/legendre-coefficients angular-dist L-max))
(println "Legendre expansion coefficients:")
(doseq [[L a-L] (sort legendre-coeffs)]
  (println (format "  a_%d = %.4e" L a-L)))
(println "")

;; Reconstruct angular distribution using Legendre expansion
(println "Reconstructed angular distribution (using Legendre expansion):")
(doseq [theta-deg [0 45 90 135 180]]
  (let [theta (* theta-deg (/ Math/PI 180))
        f-recon (inel/legendre-expansion legendre-coeffs theta)
        dsigma-recon (inel/inelastic-angular-distribution f-recon theta k-i k-f 
                                                         E-incident E-ex mass-factor)]
    (println (format "  θ=%3d°: dσ/dΩ = %.4e fm²/sr" theta-deg dsigma-recon))))
(println "")

;; ============================================================================
;; Example 6: Interference Terms
;; ============================================================================

(println "=== Example 6: Interference Terms ===")

;; Calculate interference between two channels
;; Example: Two different multipole transitions
(def T-inel-1 T-inel)  ; First channel (λ=2)
;; For second channel, use different amplitude (simplified)
(def T-inel-2 (if (number? T-inel)
               (* T-inel 0.5)
               (mul T-inel 0.5)))

(def interference (inel/interference-term T-inel-1 T-inel-2))
(println "Interference term between two channels:")
(if (number? interference)
  (println (format "  2·Re(f_1* · f_2) = %.4e" interference))
  (println (format "  2·Re(f_1* · f_2) = %.4e + i%.4e" (re interference) (im interference))))
(println "")

;; ============================================================================
;; Example 7: Multi-Channel Angular Distribution
;; ============================================================================

(println "=== Example 7: Multi-Channel Angular Distribution ===")

;; Calculate angular distribution with multiple channels (including interference)
(def amplitudes [T-inel-1 T-inel-2])
(def theta-test2 (/ Math/PI 2))
(def dsigma-multi (inel/multi-channel-angular-distribution amplitudes theta-test2 
                                                           k-i k-f E-incident E-ex mass-factor))
(println "Multi-channel angular distribution (with interference):")
(println (format "  dσ/dΩ(π/2) = %.4e fm²/sr" dsigma-multi))
(println "")

;; Compare with single channel
(def dsigma-single (inel/inelastic-angular-distribution T-inel-1 theta-test2 
                                                       k-i k-f E-incident E-ex mass-factor))
(println "Single-channel angular distribution:")
(println (format "  dσ/dΩ(π/2) = %.4e fm²/sr" dsigma-single))
(println (format "  Ratio (multi/single): %.2f" (/ dsigma-multi dsigma-single)))
(println "")

;; ============================================================================
;; Example 8: Complete Angular Distribution with Legendre Expansion
;; ============================================================================

(println "=== Example 8: Complete Angular Distribution with Legendre Expansion ===")

;; Calculate everything in one step
(def result (inel/angular-distribution-legendre-expansion T-inel theta-values k-i k-f 
                                                         E-incident E-ex mass-factor L-max))
(println "Complete result:")
(println "  Angular data points:" (count (:angular-data result)))
(println "  Legendre coefficients:" (count (:legendre-coefficients result)))
(println "  Expansion function:" (fn? (:expansion-function result)))
(println "")

;; Use expansion function to get value at specific angle
(def f-at-90 ((:expansion-function result) (/ Math/PI 2)))
(println (format "  Expansion at θ=90°: %.4e" f-at-90))
(println "")

(println "=== Examples Complete ===")
