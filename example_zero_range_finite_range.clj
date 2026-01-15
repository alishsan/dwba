;; Example: Using Zero-Range and Finite-Range Interactions for Transfer Reactions
;;
;; This demonstrates how to calculate transfer amplitudes using both
;; zero-range and finite-range approximations.

(require '[dwba.transfer :as t])
(require '[dwba.form-factors :as ff])

;; ============================================================================
;; Example 1: Zero-Range Approximation
;; ============================================================================

(println "=== Example 1: Zero-Range Approximation ===")
(def mu-i 884.3)
(def mu-f 469.46)
(def m-f-i (/ (m/sqrt (* 2. mu-i))  197.7 ))
(def m-f-f (/ (m/sqrt (* 2. mu-f))  197.7 ))

;; Define potential parameters for initial and final states
(def V-params-i [60.0 2.7 0.6])  ; Initial state: [V0, R0, a0]
(def V-params-f [50.0 1.66 0.6])  ; Final state: [V0, R0, a0]
(def energy-i -15.66)
(def energy-f -2.21)
;; Solve for bound states (1s states, l=0)
(def result-i (t/solve-bound-state-numerov energy-i 1 V-params-i m-f-i))
(def result-f (t/solve-bound-state-numerov energy-f 0 V-params-f m-f-f))

;; Extract normalized wavefunctions
(def phi-i  result-i)
(def phi-f  result-f)

(println "Initial state energy:" -15.66 "MeV")
(println "Final state energy:" -2.21 "MeV")

;; Calculate overlap integral
(def overlap (ff/normalized-overlap result-i result-f 20.0 0.01))
(println "Overlap integral:" overlap)

;; Get zero-range constant for (d,p) reaction
(def D0 (t/zero-range-constant :d-p))
(println "Zero-range constant D₀:" D0 "MeV·fm^(3/2)")

;; Calculate zero-range transfer amplitude
(def T-zero-range (t/transfer-amplitude-zero-range overlap D0))
(println "Zero-range transfer amplitude:" T-zero-range)
(println "")

;; ============================================================================
;; Example 2: Finite-Range Approximation (Yukawa)
;; ============================================================================

(println "=== Example 2: Finite-Range Approximation (Yukawa) ===")

;; Calculate finite-range overlap integral with Yukawa form factor
;; Typical range parameter: μ ≈ 0.7 fm⁻¹ for nucleon-nucleon interaction
(def mu-range 0.7)  ; Range parameter in fm⁻¹
(def overlap-fr-yukawa (t/finite-range-overlap-integral phi-i phi-f 20.0 0.01 :yukawa mu))
(println "Finite-range overlap (Yukawa, μ=" mu-range "fm⁻¹):" overlap-fr-yukawa)

;; Interaction strength (typical value)
(def V0 50.0)  ; MeV
(def T-finite-range-yukawa (t/transfer-amplitude-finite-range overlap-fr-yukawa V0))
(println "Finite-range transfer amplitude (Yukawa):" T-finite-range-yukawa)
(println "")

;; ============================================================================
;; Example 3: Finite-Range Approximation (Gaussian)
;; ============================================================================

(println "=== Example 3: Finite-Range Approximation (Gaussian) ===")

;; Calculate finite-range overlap integral with Gaussian form factor
;; Typical range parameter: β ≈ 1.0-1.5 fm
(def beta 1.2)  ; Range parameter in fm
(def overlap-fr-gaussian (t/finite-range-overlap-integral phi-i phi-f 20.0 0.01 :gaussian beta))
(println "Finite-range overlap (Gaussian, β=" beta "fm):" overlap-fr-gaussian)

(def T-finite-range-gaussian (t/transfer-amplitude-finite-range overlap-fr-gaussian V0))
(println "Finite-range transfer amplitude (Gaussian):" T-finite-range-gaussian)
(println "")

;; ============================================================================
;; Example 4: Comparison of Zero-Range vs Finite-Range
;; ============================================================================

(println "=== Example 4: Comparison ===")
(println "Zero-range amplitude:" T-zero-range)
(println "Finite-range (Yukawa) amplitude:" T-finite-range-yukawa)
(println "Finite-range (Gaussian) amplitude:" T-finite-range-gaussian)
(println "")
(println "Ratio (Yukawa/Zero-range):" (/ T-finite-range-yukawa T-zero-range))
(println "Ratio (Gaussian/Zero-range):" (/ T-finite-range-gaussian T-zero-range))
(println "")

;; ============================================================================
;; Example 5: Different Reaction Types
;; ============================================================================

(println "=== Example 5: Zero-Range Constants for Different Reactions ===")
(println "(d,p) reaction:" (t/zero-range-constant :d-p) "MeV·fm^(3/2)")
(println "(p,d) reaction:" (t/zero-range-constant :p-d) "MeV·fm^(3/2)")
(println "(α,t) reaction:" (t/zero-range-constant :alpha-t) "MeV·fm^(3/2)")
(println "")

;; ============================================================================
;; Example 6: Form Factor Functions
;; ============================================================================

(println "=== Example 6: Form Factor Functions ===")
(println "Yukawa form factor at r=0:" (t/yukawa-form-factor 0.0 0.7))
(println "Yukawa form factor at r=1 fm:" (t/yukawa-form-factor 1.0 0.7))
(println "Yukawa form factor at r=2 fm:" (t/yukawa-form-factor 2.0 0.7))
(println "")
(println "Gaussian form factor at r=0:" (t/gaussian-form-factor 0.0 1.2))
(println "Gaussian form factor at r=1 fm:" (t/gaussian-form-factor 1.0 1.2))
(println "Gaussian form factor at r=2 fm:" (t/gaussian-form-factor 2.0 1.2))
(println "")

(println "=== Examples Complete ===")
