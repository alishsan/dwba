;; Example: Phase 1 - Transition Form Factors for Inelastic Scattering
;;
;; This demonstrates the use of transition form factors, deformation parameters,
;; and reduced matrix elements for inelastic scattering calculations.

(require '[dwba.inelastic :as inel])

;; ============================================================================
;; Example 1: Deformation Parameters
;; ============================================================================

(println "=== Example 1: Deformation Parameters ===")

;; Get deformation parameters for different nuclei
(println "β_2 for ¹²C:" (inel/deformation-parameter 2 :C12))
(println "β_2 for ²⁰⁸Pb:" (inel/deformation-parameter 2 :Pb208))
(println "β_2 for ¹⁵⁴Sm:" (inel/deformation-parameter 2 :Sm154))
(println "β_3 for ¹²C:" (inel/deformation-parameter 3 :C12))
(println "β_4 for ¹²C:" (inel/deformation-parameter 4 :C12))
(println "")

;; ============================================================================
;; Example 2: Woods-Saxon Derivative
;; ============================================================================

(println "=== Example 2: Woods-Saxon Potential Derivative ===")

;; Define Woods-Saxon parameters [V0, R0, a0]
(def V-params [50.0 2.0 0.6])  ; [V0 in MeV, R0 in fm, a0 in fm]

;; Calculate dV/dr at different radii
(println "dV/dr at r=1.0 fm:" (inel/woods-saxon-derivative 1.0 V-params) "MeV/fm")
(println "dV/dr at r=2.0 fm:" (inel/woods-saxon-derivative 2.0 V-params) "MeV/fm")
(println "dV/dr at r=3.0 fm:" (inel/woods-saxon-derivative 3.0 V-params) "MeV/fm")
(println "dV/dr at r=5.0 fm:" (inel/woods-saxon-derivative 5.0 V-params) "MeV/fm")
(println "")

;; ============================================================================
;; Example 3: Transition Form Factors
;; ============================================================================

(println "=== Example 3: Transition Form Factors ===")

;; Calculate transition form factor F_λ(r) for quadrupole (λ=2) excitation
(def beta-2 0.25)  ; β_2 for ¹²C
(println "F_2(r) for ¹²C (β_2=" beta-2 ")")
(println "  F_2(1.0 fm):" (inel/transition-form-factor 1.0 2 beta-2 V-params) "MeV/fm")
(println "  F_2(2.0 fm):" (inel/transition-form-factor 2.0 2 beta-2 V-params) "MeV/fm")
(println "  F_2(3.0 fm):" (inel/transition-form-factor 3.0 2 beta-2 V-params) "MeV/fm")
(println "  F_2(5.0 fm):" (inel/transition-form-factor 5.0 2 beta-2 V-params) "MeV/fm")
(println "")

;; Using nucleus-type to automatically get β_λ
(println "F_2(r) for ¹²C (using nucleus-type lookup):")
(println "  F_2(2.0 fm):" (inel/transition-form-factor 2.0 2 nil V-params :C12) "MeV/fm")
(println "")

;; Octupole (λ=3) transition
(def beta-3 0.1)
(println "F_3(r) for octupole excitation (β_3=" beta-3 ")")
(println "  F_3(2.0 fm):" (inel/transition-form-factor 2.0 3 beta-3 V-params) "MeV/fm")
(println "  F_3(3.0 fm):" (inel/transition-form-factor 3.0 3 beta-3 V-params) "MeV/fm")
(println "")

;; ============================================================================
;; Example 4: Transition Form Factor Function
;; ============================================================================

(println "=== Example 4: Transition Form Factor as Function ===")

;; Get F_λ(r) as a vector for plotting
(def r-max 20.0)
(def h 0.1)
(def F2-function (inel/transition-form-factor-function r-max h 2 beta-2 V-params))

(println "F_2(r) calculated at" (count F2-function) "points from r=0 to r=" r-max "fm")
(println "First few values:")
(doseq [i (range (min 5 (count F2-function)))]
  (let [r (* i h)]
    (println (format "  F_2(%.1f fm) = %.4f MeV/fm" r (nth F2-function i)))))
(println "")

;; ============================================================================
;; Example 5: Reduced Matrix Elements
;; ============================================================================

(println "=== Example 5: Reduced Matrix Elements ===")

;; Calculate reduced matrix element for 2⁺ transition in ¹²C
;; Ground state: J_i = 0⁺, First excited state: J_f = 2⁺
(def J-i 0)  ; Ground state: 0⁺
(def J-f 2)  ; First excited state: 2⁺
(def Z 6)    ; Atomic number of carbon
(def R0 2.0) ; Nuclear radius (fm)

(def reduced-matrix (inel/reduced-matrix-element 2 J-i J-f beta-2 R0 Z))
(println "Reduced matrix element <2⁺||M(2)||0⁺> for ¹²C:")
(println "  |<J_f||M(2)||J_i>| =" reduced-matrix "e·fm²")
(println "")

;; ============================================================================
;; Example 6: B(Eλ) Values
;; ============================================================================

(println "=== Example 6: B(Eλ) Values ===")

;; Calculate B(E2) value
(def B-E2 (inel/B-Elambda 2 J-i J-f beta-2 R0 Z))
(println "B(E2; 0⁺ → 2⁺) for ¹²C:")
(println "  B(E2) =" B-E2 "e²·fm⁴")
(println "")

;; Compare with typical experimental values
;; ¹²C: B(E2) ≈ 3-5 e²·fm⁴ (experimental)
(println "Typical experimental B(E2) for ¹²C: ~3-5 e²·fm⁴")
(println "")

;; ============================================================================
;; Example 7: Comparison of Different Nuclei
;; ============================================================================

(println "=== Example 7: Comparison of Different Nuclei ===")

(defn compare-nuclei [nucleus-type Z R0]
  (let [beta-2 (inel/deformation-parameter 2 nucleus-type)
        F2-at-R0 (inel/transition-form-factor R0 2 beta-2 [50.0 R0 0.6])
        B-E2-val (inel/B-Elambda 2 0 2 beta-2 R0 Z)]
    {:nucleus nucleus-type
     :beta-2 beta-2
     :F2-at-R0 F2-at-R0
     :B-E2 B-E2-val}))

(def C12-result (compare-nuclei :C12 6 2.0))
(def Pb208-result (compare-nuclei :Pb208 82 7.0))
(def Sm154-result (compare-nuclei :Sm154 62 6.0))

(println "Comparison:")
(println "  " (:nucleus C12-result) ": β_2=" (:beta-2 C12-result) 
        ", F_2(R0)=" (format "%.2f" (:F2-at-R0 C12-result)) "MeV/fm"
        ", B(E2)=" (format "%.2f" (:B-E2 C12-result)) "e²·fm⁴")
(println "  " (:nucleus Pb208-result) ": β_2=" (:beta-2 Pb208-result)
        ", F_2(R0)=" (format "%.2f" (:F2-at-R0 Pb208-result)) "MeV/fm"
        ", B(E2)=" (format "%.2f" (:B-E2 Pb208-result)) "e²·fm⁴")
(println "  " (:nucleus Sm154-result) ": β_2=" (:beta-2 Sm154-result)
        ", F_2(R0)=" (format "%.2f" (:F2-at-R0 Sm154-result)) "MeV/fm"
        ", B(E2)=" (format "%.2f" (:B-E2 Sm154-result)) "e²·fm⁴")
(println "")

(println "=== Examples Complete ===")
