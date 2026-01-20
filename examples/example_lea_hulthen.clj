;; Example: Local Energy Approximation (LEA) with Hulthen Potential
;;
;; This demonstrates how to calculate transfer amplitudes using LEA
;; with Hulthen potential model for the deuteron.

(require '[dwba.transfer :as t] :reload)
(require '[dwba.form-factors :as ff] :reload)
(require '[fastmath.core :as m])

;; ============================================================================
;; Example 1: Hulthen Potential
;; ============================================================================

(println "=== Example 1: Hulthen Potential ===")

(def V0 60.0)  ; Potential depth (MeV)
(def alpha 0.23)  ; Range parameter (fm⁻¹) - typical for deuteron
(def beta 1.4)    ; Second range parameter (fm⁻¹)

(println "Hulthen potential parameters:")
(println (format "  V₀ = %.1f MeV" V0))
(println (format "  α = %.2f fm⁻¹" alpha))
(println (format "  β = %.2f fm⁻¹" beta))
(println "")

(println "Hulthen potential at different distances:")
(doseq [r [0.1 0.5 1.0 2.0 5.0]]
  (let [V (t/hulthen-potential r V0 alpha beta)]
    (println (format "  V(%.1f fm) = %.3f MeV" r V))))
(println "")

;; ============================================================================
;; Example 2: Hulthen Wavefunction
;; ============================================================================

(println "=== Example 2: Hulthen Wavefunction ===")

(println "Hulthen wavefunction (unnormalized) at different distances:")
(doseq [r [0.1 0.5 1.0 2.0 5.0]]
  (let [u (t/hulthen-wavefunction r alpha beta)]
    (println (format "  u(%.1f fm) = %.6f" r u))))
(println "")

(println "Normalized Hulthen wavefunction:")
(let [r-max 20.0
      h 0.01]
  (doseq [r [0.1 0.5 1.0 2.0 5.0]]
    (let [u-norm (t/hulthen-wavefunction-normalized r alpha beta r-max h)]
      (println (format "  u_norm(%.1f fm) = %.6f" r u-norm)))))
(println "")

;; ============================================================================
;; Example 3: LEA Transfer Amplitude
;; ============================================================================

(println "=== Example 3: LEA Transfer Amplitude for (d,p) Reaction ===")

;; Define final state (neutron bound in target nucleus)
(def V-params-f [50.0 2.0 0.6])  ; [V0, R0, a0]
(def energy-f -2.21)  ; Energy in MeV
(def mu-f 469.46)  ; Reduced mass for final state
(def m-f-f (/ (m/sqrt (* 2. mu-f)) 197.7))  ; Mass factor

;; Solve for bound state using solve-bound-state-numerov
(def u-f-raw (t/solve-bound-state-numerov energy-f 0 V-params-f m-f-f))
(def phi-f (t/normalize-bound-state u-f-raw 0.01))  ; Normalize the wavefunction

(when (seq phi-f)
  (println "Final state (neutron bound in target):")
  (println (format "  Energy: %.3f MeV" energy-f))
  (println (format "  Wavefunction length: %d points" (count phi-f)))
  (println "")
  
  ;; Calculate LEA transfer amplitude
  (let [D0 (t/zero-range-constant :d-p)
        T-lea (t/lea-transfer-amplitude phi-f alpha beta 20.0 0.01 D0)]
    (println "LEA Transfer Amplitude:")
    (println (format "  D₀ = %.1f MeV·fm^(3/2)" D0))
    (println (format "  T_LEA = %.6f" T-lea))
    (println ""))
  
  ;; Compare with simplified version
  (let [T-lea-simple (t/lea-transfer-amplitude-simplified phi-f 20.0 0.01 :d-p)]
    (println "LEA Transfer Amplitude (simplified, standard parameters):")
    (println (format "  T_LEA = %.6f" T-lea-simple))
    (println "")))

;; ============================================================================
;; Example 4: Comparison with Zero-Range
;; ============================================================================

(println "=== Example 4: LEA vs Zero-Range Comparison ===")

(when (seq phi-f)
  ;; For zero-range, we need phi-i (initial state)
  ;; In zero-range, phi-i is often approximated differently
  ;; Here we'll use a simple comparison
  (let [T-lea (t/lea-transfer-amplitude-simplified phi-f 20.0 0.01 :d-p)
        ;; Zero-range would use a different phi-i, so this is just for illustration
        D0 (t/zero-range-constant :d-p)]
    (println "LEA uses Hulthen wavefunction for deuteron")
    (println "Zero-range uses different approximation")
    (println (format "LEA amplitude: %.6f" T-lea))
    (println (format "D₀ constant: %.1f MeV·fm^(3/2)" D0))
    (println "")
    (println "Note: Direct comparison requires same initial state description")))

(println "")
(println "=== Examples Complete ===")
(println "")
(println "LEA with Hulthen potential is particularly useful for (d,p) reactions")
(println "where the deuteron is well-described by the Hulthen model.")

