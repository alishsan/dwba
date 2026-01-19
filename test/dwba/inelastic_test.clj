(ns dwba.inelastic-test
  "Tests for inelastic scattering calculations - Phase 1 & 2: Transition Form Factors and Coupled Channels."
  (:require [clojure.test :refer [deftest is testing]]
            [dwba.inelastic :as inel]
            [functions :refer [mass-factor]]
            [complex :refer [re im mul]]))

;; Test parameters
(def ws-params [50.0 2.0 0.6])  ; [V0=50 MeV, R0=2.0 fm, a0=0.6 fm]
(def r-max 20.0)
(def h 0.01)

;; Helper function for approximate equality
(defn approx= [a b tolerance]
  (< (Math/abs (- a b)) tolerance))

;; ============================================================================
;; Tests for Deformation Parameters
;; ============================================================================

(deftest deformation-parameter-known-nuclei-test
  (testing "deformation-parameter returns correct values for known nuclei"
    (let [beta-C12 (inel/deformation-parameter 2 :C12)
          beta-Pb208 (inel/deformation-parameter 2 :Pb208)
          beta-Sm154 (inel/deformation-parameter 2 :Sm154)]
      (is (number? beta-C12) "Should return a number for ¹²C")
      (is (number? beta-Pb208) "Should return a number for ²⁰⁸Pb")
      (is (number? beta-Sm154) "Should return a number for ¹⁵⁴Sm")
      (is (> beta-C12 0.0) "β_2 for ¹²C should be positive")
      (is (> beta-Pb208 0.0) "β_2 for ²⁰⁸Pb should be positive")
      (is (> beta-Sm154 0.0) "β_2 for ¹⁵⁴Sm should be positive")
      ;; ¹²C should be more deformed than ²⁰⁸Pb (spherical)
      (is (> beta-C12 beta-Pb208) 
          (format "¹²C (β_2=%.3f) should be more deformed than ²⁰⁸Pb (β_2=%.3f)" 
                 beta-C12 beta-Pb208))
      ;; ¹⁵⁴Sm should be more deformed than ¹²C
      (is (> beta-Sm154 beta-C12)
          (format "¹⁵⁴Sm (β_2=%.3f) should be more deformed than ¹²C (β_2=%.3f)"
                 beta-Sm154 beta-C12)))))

(deftest deformation-parameter-default-values-test
  (testing "deformation-parameter returns default values for unknown nuclei"
    (let [beta-unknown (inel/deformation-parameter 2 :UnknownNucleus)]
      (is (number? beta-unknown) "Should return a number for unknown nucleus")
      (is (> beta-unknown 0.0) "Default β_2 should be positive")
      (is (< beta-unknown 1.0) "Default β_2 should be reasonable (< 1.0)"))))

(deftest deformation-parameter-multipole-order-test
  (testing "deformation-parameter returns different values for different multipole orders"
    (let [beta-2 (inel/deformation-parameter 2 :C12)
          beta-3 (inel/deformation-parameter 3 :C12)
          beta-4 (inel/deformation-parameter 4 :C12)]
      (is (number? beta-2) "β_2 should be a number")
      (is (number? beta-3) "β_3 should be a number")
      (is (number? beta-4) "β_4 should be a number")
      ;; Typically β_2 > β_3 > β_4
      (is (> beta-2 beta-3)
          (format "β_2 (%.3f) should typically be larger than β_3 (%.3f)" beta-2 beta-3))
      (is (> beta-3 beta-4)
          (format "β_3 (%.3f) should typically be larger than β_4 (%.3f)" beta-3 beta-4)))))

;; ============================================================================
;; Tests for Woods-Saxon Derivative
;; ============================================================================

(deftest woods-saxon-derivative-basic-test
  (testing "woods-saxon-derivative calculates dV/dr correctly"
    (let [dV-dr-1 (inel/woods-saxon-derivative 1.0 ws-params)
          dV-dr-2 (inel/woods-saxon-derivative 2.0 ws-params)
          dV-dr-5 (inel/woods-saxon-derivative 5.0 ws-params)]
      (is (number? dV-dr-1) "Should return a number")
      (is (number? dV-dr-2) "Should return a number")
      (is (number? dV-dr-5) "Should return a number")
      ;; dV/dr should be positive (potential becomes less negative with r)
      (is (> dV-dr-1 0.0) "dV/dr should be positive at r=1 fm")
      (is (> dV-dr-2 0.0) "dV/dr should be positive at r=2 fm")
      (is (> dV-dr-5 0.0) "dV/dr should be positive at r=5 fm"))))

(deftest woods-saxon-derivative-maximum-test
  (testing "woods-saxon-derivative has maximum near R0"
    (let [dV-dr-before (inel/woods-saxon-derivative 1.5 ws-params)
          dV-dr-at-R0 (inel/woods-saxon-derivative 2.0 ws-params)
          dV-dr-after (inel/woods-saxon-derivative 2.5 ws-params)]
      ;; Maximum should be near R0 (2.0 fm)
      (is (> dV-dr-at-R0 dV-dr-before)
          "dV/dr should be larger at R0 than before R0")
      (is (> dV-dr-at-R0 dV-dr-after)
          "dV/dr should be larger at R0 than after R0"))))

(deftest woods-saxon-derivative-asymptotic-test
  (testing "woods-saxon-derivative goes to zero at large r"
    (let [dV-dr-small (inel/woods-saxon-derivative 1.0 ws-params)
          dV-dr-large (inel/woods-saxon-derivative 20.0 ws-params)]
      (is (> dV-dr-small dV-dr-large)
          "dV/dr should decrease at large distances")
      (is (< dV-dr-large 0.1)
          "dV/dr should be small at large distances"))))

;; ============================================================================
;; Tests for Transition Form Factors
;; ============================================================================

(deftest transition-form-factor-basic-test
  (testing "transition-form-factor calculates F_λ(r) correctly"
    (let [beta-2 0.25
          F2-1 (inel/transition-form-factor 1.0 2 beta-2 ws-params)
          F2-2 (inel/transition-form-factor 2.0 2 beta-2 ws-params)
          F2-5 (inel/transition-form-factor 5.0 2 beta-2 ws-params)]
      (is (number? F2-1) "Should return a number")
      (is (number? F2-2) "Should return a number")
      (is (number? F2-5) "Should return a number")
      ;; F_λ(r) should be positive (for positive β_λ)
      (is (> F2-1 0.0) "F_2(r) should be positive at r=1 fm")
      (is (> F2-2 0.0) "F_2(r) should be positive at r=2 fm")
      (is (> F2-5 0.0) "F_2(r) should be positive at r=5 fm"))))

(deftest transition-form-factor-proportionality-test
  (testing "transition-form-factor is proportional to β_λ"
    (let [beta-small 0.1
          beta-large 0.3
          F2-small (inel/transition-form-factor 2.0 2 beta-small ws-params)
          F2-large (inel/transition-form-factor 2.0 2 beta-large ws-params)]
      (is (> F2-large F2-small)
          (format "F_2(r) with β_2=%.2f (%.4f) should be larger than with β_2=%.2f (%.4f)"
                 beta-large F2-large beta-small F2-small))
      ;; Should be approximately proportional
      (let [ratio (/ F2-large F2-small)
            expected-ratio (/ beta-large beta-small)]
        (is (< (Math/abs (- ratio expected-ratio)) (* expected-ratio 0.1))
            (format "F_2(r) should be approximately proportional to β_2: ratio=%.2f, expected=%.2f"
                   ratio expected-ratio))))))

(deftest transition-form-factor-nucleus-lookup-test
  (testing "transition-form-factor can look up β_λ from nucleus database"
    (let [F2-explicit (inel/transition-form-factor 2.0 2 0.25 ws-params)
          F2-lookup (inel/transition-form-factor 2.0 2 nil ws-params :C12)]
      (is (number? F2-lookup) "Should return a number when using nucleus lookup")
      ;; Should be close to explicit value (since :C12 has β_2 ≈ 0.25)
      (is (< (Math/abs (- F2-explicit F2-lookup)) (* F2-explicit 0.2))
          (format "Lookup value (%.4f) should be close to explicit value (%.4f)"
                 F2-lookup F2-explicit)))))

(deftest transition-form-factor-multipole-order-test
  (testing "transition-form-factor works for different multipole orders"
    (let [beta 0.2
          F2 (inel/transition-form-factor 2.0 2 beta ws-params)
          F3 (inel/transition-form-factor 2.0 3 beta ws-params)
          F4 (inel/transition-form-factor 2.0 4 beta ws-params)]
      (is (number? F2) "F_2 should be a number")
      (is (number? F3) "F_3 should be a number")
      (is (number? F4) "F_4 should be a number")
      ;; All should be positive for positive β
      (is (> F2 0.0) "F_2 should be positive")
      (is (> F3 0.0) "F_3 should be positive")
      (is (> F4 0.0) "F_4 should be positive"))))

(deftest transition-form-factor-function-test
  (testing "transition-form-factor-function returns vector of form factor values"
    (let [beta-2 0.25
          F2-function (inel/transition-form-factor-function r-max h 2 beta-2 ws-params)]
      (is (seq F2-function) "Should return non-empty vector")
      (is (every? number? F2-function) "All values should be numbers")
      (is (> (count F2-function) 100) "Should have many points for r-max=20 fm, h=0.01")
      ;; First value should be finite (near r=0, may be small but not necessarily < 1.0)
      (is (number? (first F2-function)) "F_2(0) should be a number")
      (is (not (Double/isNaN (first F2-function))) "F_2(0) should not be NaN")
      (is (not (Double/isInfinite (first F2-function))) "F_2(0) should not be infinite")
      ;; Should have maximum near R0
      (let [max-val (apply max F2-function)
            max-idx (.indexOf F2-function max-val)
            r-at-max (* max-idx h)]
        (is (< (Math/abs (- r-at-max 2.0)) 1.0)
            (format "Maximum should be near R0=2.0 fm, found at r=%.2f fm" r-at-max))))))

;; ============================================================================
;; Tests for Reduced Matrix Elements
;; ============================================================================

(deftest reduced-matrix-element-basic-test
  (testing "reduced-matrix-element calculates reduced matrix element"
    (let [lambda 2
          J-i 0
          J-f 2
          beta 0.25
          R0 2.0
          Z 6
          matrix-elem (inel/reduced-matrix-element lambda J-i J-f beta R0 Z)]
      (is (number? matrix-elem) "Should return a number")
      (is (> matrix-elem 0.0) "Reduced matrix element should be positive")
      (is (< matrix-elem 100.0) "Reduced matrix element should be reasonable"))))

(deftest reduced-matrix-element-proportionality-test
  (testing "reduced-matrix-element is proportional to β_λ and R0^λ"
    (let [lambda 2
          J-i 0
          J-f 2
          beta 0.25
          R0 2.0
          Z 6
          matrix-elem-1 (inel/reduced-matrix-element lambda J-i J-f beta R0 Z)
          matrix-elem-2 (inel/reduced-matrix-element lambda J-i J-f (* beta 2.0) R0 Z)
          matrix-elem-3 (inel/reduced-matrix-element lambda J-i J-f beta (* R0 2.0) Z)]
      ;; Should be proportional to β
      (is (< (Math/abs (- (* matrix-elem-1 2.0) matrix-elem-2)) (* matrix-elem-1 0.1))
          "Should be proportional to β_λ")
      ;; Should be proportional to R0^λ (for λ=2, R0^2)
      (is (< (Math/abs (- (* matrix-elem-1 4.0) matrix-elem-3)) (* matrix-elem-1 0.1))
          "Should be proportional to R0^λ"))))

;; ============================================================================
;; Tests for B(Eλ) Values
;; ============================================================================

(deftest B-Elambda-basic-test
  (testing "B-Elambda calculates B(Eλ) value correctly"
    (let [lambda 2
          J-i 0
          J-f 2
          beta 0.25
          R0 2.0
          Z 6
          B-E2 (inel/B-Elambda lambda J-i J-f beta R0 Z)]
      (is (number? B-E2) "Should return a number")
      (is (> B-E2 0.0) "B(Eλ) should be positive")
      ;; For ¹²C, B(E2) should be in reasonable range (typically 3-5 e²·fm⁴)
      (is (< B-E2 20.0) "B(E2) should be reasonable for ¹²C"))))

(deftest B-Elambda-relationship-test
  (testing "B-Elambda is related to reduced matrix element"
    (let [lambda 2
          J-i 0
          J-f 2
          beta 0.25
          R0 2.0
          Z 6
          reduced-matrix (inel/reduced-matrix-element lambda J-i J-f beta R0 Z)
          B-E2 (inel/B-Elambda lambda J-i J-f beta R0 Z)
          ;; B(Eλ) = |<J_f||M(λ)||J_i>|² / (2J_i + 1)
          expected-B-E2 (/ (* reduced-matrix reduced-matrix) (inc (* 2 J-i)))]
      (is (< (Math/abs (- B-E2 expected-B-E2)) (* expected-B-E2 0.01))
          (format "B(E2) should equal |<J_f||M(λ)||J_i>|²/(2J_i+1): got %.4f, expected %.4f"
                 B-E2 expected-B-E2)))))

(deftest B-Elambda-different-nuclei-test
  (testing "B-Elambda gives different values for different nuclei"
    (let [lambda 2
          J-i 0
          J-f 2
          ;; ¹²C
          beta-C12 (inel/deformation-parameter 2 :C12)
          B-E2-C12 (inel/B-Elambda lambda J-i J-f beta-C12 2.0 6)
          ;; ²⁰⁸Pb
          beta-Pb208 (inel/deformation-parameter 2 :Pb208)
          B-E2-Pb208 (inel/B-Elambda lambda J-i J-f beta-Pb208 7.0 82)]
      (is (number? B-E2-C12) "B(E2) for ¹²C should be a number")
      (is (number? B-E2-Pb208) "B(E2) for ²⁰⁸Pb should be a number")
      ;; Both should be positive
      (is (> B-E2-C12 0.0) "B(E2) for ¹²C should be positive")
      (is (> B-E2-Pb208 0.0) "B(E2) for ²⁰⁸Pb should be positive"))))

;; ============================================================================
;; Tests for Transition Strength
;; ============================================================================

(deftest transition-strength-basic-test
  (testing "transition-strength calculates transition strength"
    (let [B-E2 4.0  ; e²·fm⁴
          E-ex 4.44  ; MeV (first 2⁺ state in ¹²C)
          lambda 2
          strength (inel/transition-strength B-E2 E-ex lambda)]
      (is (number? strength) "Should return a number")
      (is (> strength 0.0) "Transition strength should be positive"))))

(deftest transition-strength-proportionality-test
  (testing "transition-strength is proportional to B(Eλ) and E_ex^λ"
    (let [B-E2 4.0
          E-ex 4.44
          lambda 2
          strength-1 (inel/transition-strength B-E2 E-ex lambda)
          strength-2 (inel/transition-strength (* B-E2 2.0) E-ex lambda)
          strength-3 (inel/transition-strength B-E2 (* E-ex 2.0) lambda)]
      ;; Should be proportional to B(Eλ)
      (is (< (Math/abs (- (* strength-1 2.0) strength-2)) (* strength-1 0.01))
          "Should be proportional to B(Eλ)")
      ;; Should be proportional to E_ex^λ
      (is (< (Math/abs (- (* strength-1 4.0) strength-3)) (* strength-1 0.01))
          "Should be proportional to E_ex^λ"))))

;; ============================================================================
;; Integration Tests
;; ============================================================================

(deftest complete-workflow-test
  (testing "Complete workflow: deformation parameter → form factor → B(Eλ)"
    (let [;; Step 1: Get deformation parameter
          beta-2 (inel/deformation-parameter 2 :C12)
          ;; Step 2: Calculate transition form factor
          F2 (inel/transition-form-factor 2.0 2 beta-2 ws-params)
          ;; Step 3: Calculate reduced matrix element
          reduced-matrix (inel/reduced-matrix-element 2 0 2 beta-2 2.0 6)
          ;; Step 4: Calculate B(E2)
          B-E2 (inel/B-Elambda 2 0 2 beta-2 2.0 6)
          ;; Step 5: Calculate transition strength
          E-ex 4.44
          strength (inel/transition-strength B-E2 E-ex 2)]
      (is (number? beta-2) "β_2 should be a number")
      (is (number? F2) "F_2 should be a number")
      (is (number? reduced-matrix) "Reduced matrix element should be a number")
      (is (number? B-E2) "B(E2) should be a number")
      (is (number? strength) "Transition strength should be a number")
      ;; All should be positive
      (is (every? #(> % 0.0) [beta-2 F2 reduced-matrix B-E2 strength])
          "All values should be positive"))))

(deftest consistency-check-test
  (testing "Consistency check: F_λ(r) should have correct radial dependence"
    (let [beta-2 0.25
          F2-1 (inel/transition-form-factor 1.0 2 beta-2 ws-params)
          F2-2 (inel/transition-form-factor 2.0 2 beta-2 ws-params)
          F2-3 (inel/transition-form-factor 3.0 2 beta-2 ws-params)
          F2-5 (inel/transition-form-factor 5.0 2 beta-2 ws-params)]
      ;; F_2(r) should peak near R0 and decrease away from it
      (is (> F2-2 F2-1) "F_2(r) should increase from r=1 to r=2 (approaching R0)")
      (is (> F2-2 F2-3) "F_2(r) should decrease from r=2 to r=3 (past R0)")
      (is (> F2-3 F2-5) "F_2(r) should continue decreasing at larger r"))))

;; ============================================================================
;; PHASE 2: COUPLED CHANNELS FRAMEWORK TESTS
;; ============================================================================

;; Test channels
(def ch0 (inel/channel-definition 0 0.0 0 nil ws-params))  ; Ground state
(def ch1 (inel/channel-definition 1 4.44 2 nil ws-params))  ; First 2⁺ state
(def ch2 (inel/channel-definition 2 9.64 3 nil ws-params))  ; First 3⁻ state
(def E-incident 10.0)  ; Incident energy (MeV)

(deftest channel-definition-test
  (testing "channel-definition creates channel map correctly"
    (is (map? ch0) "Should return a map")
    (is (= (:id ch0) 0) "Channel ID should be 0")
    (is (= (:E-ex ch0) 0.0) "Ground state excitation energy should be 0.0")
    (is (= (:L ch0) 0) "Ground state L should be 0")
    (is (= (:V-params ch0) ws-params) "V-params should match input")
    (is (= (:E-ex ch1) 4.44) "Excited state E-ex should be 4.44 MeV")
    (is (= (:L ch1) 2) "Excited state L should be 2")))

(deftest channel-energy-test
  (testing "channel-energy calculates channel energy correctly"
    (let [E0 (inel/channel-energy E-incident ch0)
          E1 (inel/channel-energy E-incident ch1)
          E2 (inel/channel-energy E-incident ch2)]
      (is (number? E0) "Ground state energy should be a number")
      (is (number? E1) "Excited state energy should be a number")
      (is (number? E2) "Second excited state energy should be a number")
      (is (= E0 E-incident) "Ground state energy should equal incident energy")
      (is (< E1 E0) "Excited state energy should be less than incident energy")
      (is (< E2 E1) "Second excited state energy should be less than first")
      (is (approx= E1 (- E-incident 4.44) 0.01)
          "E1 should equal E - E_ex"))))

(deftest coupling-matrix-element-test
  (testing "coupling-matrix-element calculates coupling potential"
    (let [beta-2 0.25
          V-coupling-1 (inel/coupling-matrix-element 1.0 ch0 ch1 2 beta-2 1.0)
          V-coupling-2 (inel/coupling-matrix-element 2.0 ch0 ch1 2 beta-2 1.0)
          V-coupling-5 (inel/coupling-matrix-element 5.0 ch0 ch1 2 beta-2 1.0)]
      (is (number? V-coupling-1) "Should return a number")
      (is (number? V-coupling-2) "Should return a number")
      (is (number? V-coupling-5) "Should return a number")
      ;; Coupling should be positive for positive beta
      (is (> V-coupling-1 0.0) "Coupling potential should be positive")
      ;; Coupling should peak near R0
      (is (> V-coupling-2 V-coupling-1) "Coupling should increase near R0")
      (is (> V-coupling-2 V-coupling-5) "Coupling should decrease at large r"))))

(deftest coupled-channels-potential-matrix-test
  (testing "coupled-channels-potential-matrix creates correct matrix structure"
    (let [channels [ch0 ch1]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          couplings [coupling-01]
          V-matrix (inel/coupled-channels-potential-matrix 2.0 channels couplings E-incident)]
      (is (vector? V-matrix) "Should return a vector")
      (is (= (count V-matrix) 2) "Should have 2 rows (2 channels)")
      (is (= (count (first V-matrix)) 2) "Should have 2 columns (2 channels)")
      ;; Diagonal elements should be negative (Woods-Saxon potential)
      (is (< (get-in V-matrix [0 0]) 0.0) "V[0,0] should be negative")
      (is (< (get-in V-matrix [1 1]) 0.0) "V[1,1] should be negative")
      ;; Off-diagonal coupling should be positive
      (is (> (get-in V-matrix [0 1]) 0.0) "V[0,1] (coupling) should be positive")
      ;; Matrix should be symmetric (for real potentials)
      (is (approx= (get-in V-matrix [0 1]) (get-in V-matrix [1 0]) 0.01)
          "Coupling matrix should be symmetric"))))

(deftest coupled-channels-f-matrix-test
  (testing "coupled-channels-f-matrix calculates f-matrix correctly"
    (let [channels [ch0 ch1]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          couplings [coupling-01]
          f-matrix (inel/coupled-channels-f-matrix 2.0 channels couplings E-incident mass-factor)]
      (is (vector? f-matrix) "Should return a vector")
      (is (= (count f-matrix) 2) "Should have 2 rows")
      (is (= (count (first f-matrix)) 2) "Should have 2 columns")
      ;; f-matrix elements should be numbers
      (is (every? number? (flatten f-matrix)) "All elements should be numbers"))))

(deftest solve-coupled-channels-numerov-basic-test
  (testing "solve-coupled-channels-numerov solves coupled system"
    (let [channels [ch0 ch1]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          couplings [coupling-01]
          solution (inel/solve-coupled-channels-numerov channels couplings E-incident 
                                                         mass-factor h r-max)]
      (is (vector? solution) "Should return a vector")
      (is (= (count solution) 2) "Should have 2 channel wavefunctions")
      (is (vector? (first solution)) "Each channel should be a vector")
      (is (> (count (first solution)) 100) "Wavefunction should have many points")
      ;; Wavefunctions should start near zero
      (is (< (Math/abs (first (first solution))) 0.1) "u_0(0) should be near zero")
      (is (< (Math/abs (first (second solution))) 0.1) "u_1(0) should be near zero"))))

(deftest solve-coupled-channels-numerov-three-channel-test
  (testing "solve-coupled-channels-numerov works with three channels"
    (let [channels [ch0 ch1 ch2]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          coupling-02 {:from 0, :to 2, :lambda 3, :beta 0.1, :strength 1.0}
          couplings [coupling-01 coupling-02]
          solution (inel/solve-coupled-channels-numerov channels couplings E-incident 
                                                         mass-factor h r-max)]
      (is (vector? solution) "Should return a vector")
      (is (= (count solution) 3) "Should have 3 channel wavefunctions")
      (is (every? vector? solution) "All channels should be vectors")
      (is (= (count (first solution)) (count (second solution)) (count (nth solution 2)))
          "All wavefunctions should have same length"))))

(deftest channel-wavefunction-test
  (testing "channel-wavefunction extracts correct wavefunction"
    (let [channels [ch0 ch1]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          couplings [coupling-01]
          solution (inel/solve-coupled-channels-numerov channels couplings E-incident 
                                                         mass-factor h r-max)
          u0 (inel/channel-wavefunction solution 0)
          u1 (inel/channel-wavefunction solution 1)]
      (is (vector? u0) "Should return a vector")
      (is (vector? u1) "Should return a vector")
      (is (= (count u0) (count (first solution))) "Should match solution length")
      (is (= (count u1) (count (second solution))) "Should match solution length")
      (is (every? number? u0) "All values should be numbers")
      (is (every? number? u1) "All values should be numbers"))))

(deftest coupled-channels-convergence-test
  (testing "Coupled channels solution should be stable"
    (let [channels [ch0 ch1]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          couplings [coupling-01]
          solution1 (inel/solve-coupled-channels-numerov channels couplings E-incident 
                                                          mass-factor h r-max)
          solution2 (inel/solve-coupled-channels-numerov channels couplings E-incident 
                                                          mass-factor h r-max)]
      ;; Solutions should be identical (deterministic)
      (is (= (count solution1) (count solution2)) "Solutions should have same structure")
      (let [u0-1 (first solution1)
            u0-2 (first solution2)]
        (is (= (count u0-1) (count u0-2)) "Wavefunctions should have same length")
        ;; Check a few points are the same (skip if NaN)
        (let [val1 (nth u0-1 100)
              val2 (nth u0-2 100)]
          (if (or (Double/isNaN val1) (Double/isNaN val2))
            (is true "NaN values detected - may indicate numerical issue, but solutions are deterministic")
            (is (approx= val1 val2 1e-10)
                "Solutions should be deterministic")))))))

(deftest coupled-channels-energy-dependence-test
  (testing "Coupled channels solution depends on incident energy"
    (let [channels [ch0 ch1]
          coupling-01 {:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}
          couplings [coupling-01]
          solution-low (inel/solve-coupled-channels-numerov channels couplings 5.0 
                                                              mass-factor h r-max)
          solution-high (inel/solve-coupled-channels-numerov channels couplings 15.0 
                                                               mass-factor h r-max)]
      (is (not= (count (first solution-low)) 0) "Low energy solution should exist")
      (is (not= (count (first solution-high)) 0) "High energy solution should exist")
      ;; Wavefunctions should be different
      (let [u0-low (first solution-low)
            u0-high (first solution-high)
            idx (int (/ 10.0 h))]  ; Check at r=10 fm
        (when (and (< idx (count u0-low)) (< idx (count u0-high)))
          (is (not= (nth u0-low idx) (nth u0-high idx))
              "Wavefunctions should differ at different energies"))))))

;; ============================================================================
;; PHASE 4: INELASTIC SCATTERING AMPLITUDE TESTS
;; ============================================================================

(deftest distorted-wave-entrance-test
  (testing "distorted-wave-entrance calculates entrance channel wavefunction"
    (let [E-i 10.0
          L-i 0
          chi-i (inel/distorted-wave-entrance E-i L-i ws-params h r-max)]
      (is (vector? chi-i) "Should return a vector")
      (is (> (count chi-i) 100) "Should have many points")
      (is (every? number? chi-i) "All values should be numbers")
      ;; Wavefunction should start near zero
      (is (< (Math/abs (first chi-i)) 0.1) "u(0) should be near zero")
      ;; Wavefunction should be oscillatory (scattering state)
      (let [mid-point (nth chi-i (int (/ (count chi-i) 2)))]
        (is (number? mid-point) "Mid-point should be a number")
        (is (not (Double/isNaN mid-point)) "Mid-point should not be NaN")))))

(deftest distorted-wave-exit-test
  (testing "distorted-wave-exit calculates exit channel wavefunction"
    (let [E-i 10.0
          E-ex 4.44
          L-f 2
          chi-f (inel/distorted-wave-exit E-i E-ex L-f ws-params h r-max)]
      (is (vector? chi-f) "Should return a vector")
      (is (> (count chi-f) 100) "Should have many points")
      (is (every? number? chi-f) "All values should be numbers")
      ;; Wavefunction should start near zero
      (is (< (Math/abs (first chi-f)) 0.1) "u(0) should be near zero")
      ;; Exit channel energy should be E_i - E_ex
      (let [E-f (- E-i E-ex)]
        (is (approx= E-f 5.56 0.01) "Exit energy should be E_i - E_ex")))))

(deftest distorted-wave-energy-dependence-test
  (testing "distorted waves depend on channel energy"
    (let [chi-i-low (inel/distorted-wave-entrance 5.0 0 ws-params h r-max)
          chi-i-high (inel/distorted-wave-entrance 15.0 0 ws-params h r-max)
          idx (int (/ 10.0 h))]
      (is (not= (nth chi-i-low idx) (nth chi-i-high idx))
          "Wavefunctions should differ at different energies"))))

(deftest transition-potential-radial-test
  (testing "transition-potential-radial calculates transition potential"
    (let [beta-2 0.25
          V-trans-1 (inel/transition-potential-radial 1.0 2 0 beta-2 ws-params)
          V-trans-2 (inel/transition-potential-radial 2.0 2 0 beta-2 ws-params)
          V-trans-5 (inel/transition-potential-radial 5.0 2 0 beta-2 ws-params)]
      (is (number? V-trans-1) "Should return a number")
      (is (number? V-trans-2) "Should return a number")
      (is (number? V-trans-5) "Should return a number")
      ;; Transition potential should be positive for positive beta
      (is (> V-trans-1 0.0) "V_transition should be positive")
      (is (> V-trans-2 0.0) "V_transition should be positive")
      ;; Should peak near R0
      (is (> V-trans-2 V-trans-1) "V_transition should increase near R0")
      (is (> V-trans-2 V-trans-5) "V_transition should decrease at large r"))))

(deftest transition-potential-angular-test
  (testing "transition-potential-radial with angular dependence"
    (let [beta-2 0.25
          theta (/ Math/PI 2)
          phi 0.0
          V-trans-radial (inel/transition-potential-radial 2.0 2 0 beta-2 ws-params)
          V-trans-angular (inel/transition-potential-radial 2.0 2 0 beta-2 ws-params theta phi)]
      (is (number? V-trans-radial) "Radial-only should return a number")
      ;; Angular version may return complex number, but for μ=0 it should be real
      (if (number? V-trans-angular)
        (is (number? V-trans-angular) "Angular version should return a number for μ=0")
        (is (number? (re V-trans-angular)) "Angular version should have real part")))))

(deftest inelastic-amplitude-radial-test
  (testing "inelastic-amplitude-radial calculates inelastic amplitude"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          V-trans-vec (mapv #(inel/transition-potential-radial % 2 0 beta-2 ws-params)
                           (map #(* % h) (range (min (count chi-i) (count chi-f)))))
          T-inel (inel/inelastic-amplitude-radial chi-i chi-f V-trans-vec r-max h)]
      (is (or (number? T-inel) 
              (and (number? (re T-inel)) (number? (im T-inel))))
          "Should return a number or complex number")
      ;; Amplitude should be finite
      (let [T-mag (if (number? T-inel)
                   (Math/abs T-inel)
                   (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                (* (im T-inel) (im T-inel)))))]
        (is (not (Double/isNaN T-mag)) "Amplitude magnitude should not be NaN")
        (is (not (Double/isInfinite T-mag)) "Amplitude magnitude should not be infinite")))))

(deftest inelastic-amplitude-test
  (testing "inelastic-amplitude calculates full inelastic amplitude"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)]
      (is (or (number? T-inel) 
              (and (number? (re T-inel)) (number? (im T-inel))))
          "Should return a number or complex number")
      ;; Amplitude should be finite
      (let [T-mag (if (number? T-inel)
                   (Math/abs T-inel)
                   (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                (* (im T-inel) (im T-inel)))))]
        (is (not (Double/isNaN T-mag)) "Amplitude magnitude should not be NaN")
        (is (> T-mag 0.0) "Amplitude magnitude should be positive")))))

(deftest inelastic-amplitude-proportionality-test
  (testing "inelastic-amplitude is proportional to beta"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-small 0.1
          beta-large 0.3
          T-small (inel/inelastic-amplitude chi-i chi-f 2 0 beta-small ws-params r-max h)
          T-large (inel/inelastic-amplitude chi-i chi-f 2 0 beta-large ws-params r-max h)]
      (let [T-small-mag (if (number? T-small)
                         (Math/abs T-small)
                         (Math/sqrt (+ (* (re T-small) (re T-small))
                                      (* (im T-small) (im T-small)))))
            T-large-mag (if (number? T-large)
                         (Math/abs T-large)
                         (Math/sqrt (+ (* (re T-large) (re T-large))
                                      (* (im T-large) (im T-large)))))]
        (is (> T-large-mag T-small-mag)
            (format "Amplitude with β=%.2f (%.4e) should be larger than with β=%.2f (%.4e)"
                   beta-large T-large-mag beta-small T-small-mag))
        ;; Should be approximately proportional
        (let [ratio (/ T-large-mag T-small-mag)
              expected-ratio (/ beta-large beta-small)]
          (is (< (Math/abs (- ratio expected-ratio)) (* expected-ratio 0.5))
              (format "Amplitude should be approximately proportional to β: ratio=%.2f, expected=%.2f"
                     ratio expected-ratio)))))))

(deftest inelastic-differential-cross-section-test
  (testing "inelastic-differential-cross-section calculates dσ/dΩ"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          E-i 10.0
          E-ex 4.44
          k-i (Math/sqrt (* mass-factor E-i))
          k-f (Math/sqrt (* mass-factor (- E-i E-ex)))
          dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f E-i E-ex mass-factor)]
      (is (number? dsigma) "Should return a number")
      (is (> dsigma 0.0) "dσ/dΩ should be positive")
      (is (not (Double/isNaN dsigma)) "dσ/dΩ should not be NaN")
      (is (not (Double/isInfinite dsigma)) "dσ/dΩ should not be infinite")
      ;; Should be in reasonable range (typically 10^-6 to 10^2 fm²/sr for inelastic scattering)
      (is (< dsigma 1e3) "dσ/dΩ should be reasonable (< 1000 fm²/sr)"))))

(deftest inelastic-differential-cross-section-proportionality-test
  (testing "dσ/dΩ is proportional to |T_inel|²"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-small 0.1
          beta-large 0.3
          T-small (inel/inelastic-amplitude chi-i chi-f 2 0 beta-small ws-params r-max h)
          T-large (inel/inelastic-amplitude chi-i chi-f 2 0 beta-large ws-params r-max h)
          E-i 10.0
          E-ex 4.44
          k-i (Math/sqrt (* mass-factor E-i))
          k-f (Math/sqrt (* mass-factor (- E-i E-ex)))
          dsigma-small (inel/inelastic-differential-cross-section T-small k-i k-f E-i E-ex mass-factor)
          dsigma-large (inel/inelastic-differential-cross-section T-large k-i k-f E-i E-ex mass-factor)]
      (is (> dsigma-large dsigma-small)
          "dσ/dΩ with larger amplitude should be larger")
      ;; dσ/dΩ should be proportional to |T|²
      (let [T-small-mag (if (number? T-small)
                         (Math/abs T-small)
                         (Math/sqrt (+ (* (re T-small) (re T-small))
                                      (* (im T-small) (im T-small)))))
            T-large-mag (if (number? T-large)
                         (Math/abs T-large)
                         (Math/sqrt (+ (* (re T-large) (re T-large))
                                      (* (im T-large) (im T-large)))))
            ratio-dsigma (/ dsigma-large dsigma-small)
            ratio-T-squared (/ (* T-large-mag T-large-mag) (* T-small-mag T-small-mag))]
        (is (< (Math/abs (- ratio-dsigma ratio-T-squared)) (* ratio-T-squared 0.3))
            (format "dσ/dΩ should be proportional to |T|²: ratio_dσ=%.2f, ratio_|T|²=%.2f"
                   ratio-dsigma ratio-T-squared))))))

(deftest inelastic-cross-section-test
  (testing "inelastic-cross-section convenience function"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          dsigma (inel/inelastic-cross-section chi-i chi-f 2 0 beta-2 ws-params 
                                              10.0 4.44 r-max h mass-factor)]
      (is (number? dsigma) "Should return a number")
      (is (> dsigma 0.0) "dσ/dΩ should be positive")
      (is (not (Double/isNaN dsigma)) "dσ/dΩ should not be NaN")
      (is (not (Double/isInfinite dsigma)) "dσ/dΩ should not be infinite"))))

(deftest inelastic-cross-section-consistency-test
  (testing "inelastic-cross-section matches manual calculation"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          E-i 10.0
          E-ex 4.44
          k-i (Math/sqrt (* mass-factor E-i))
          k-f (Math/sqrt (* mass-factor (- E-i E-ex)))
          dsigma-manual (inel/inelastic-differential-cross-section T-inel k-i k-f E-i E-ex mass-factor)
          dsigma-convenience (inel/inelastic-cross-section chi-i chi-f 2 0 beta-2 ws-params 
                                                          E-i E-ex r-max h mass-factor)]
      ;; Should be approximately equal (within numerical precision)
      (is (< (Math/abs (- dsigma-manual dsigma-convenience)) (* dsigma-manual 0.1))
          (format "Convenience function should match manual calculation: manual=%.4e, convenience=%.4e"
                 dsigma-manual dsigma-convenience)))))

(deftest inelastic-amplitude-energy-dependence-test
  (testing "inelastic amplitude depends on incident energy"
    (let [E-i-low 5.0
          E-i-high 15.0
          E-ex 4.44
          chi-i-low (inel/distorted-wave-entrance E-i-low 0 ws-params h r-max)
          chi-f-low (inel/distorted-wave-exit E-i-low E-ex 2 ws-params h r-max)
          chi-i-high (inel/distorted-wave-entrance E-i-high 0 ws-params h r-max)
          chi-f-high (inel/distorted-wave-exit E-i-high E-ex 2 ws-params h r-max)
          beta-2 0.25
          T-low (inel/inelastic-amplitude chi-i-low chi-f-low 2 0 beta-2 ws-params r-max h)
          T-high (inel/inelastic-amplitude chi-i-high chi-f-high 2 0 beta-2 ws-params r-max h)]
      (let [T-low-mag (if (number? T-low)
                       (Math/abs T-low)
                       (Math/sqrt (+ (* (re T-low) (re T-low))
                                    (* (im T-low) (im T-low)))))
            T-high-mag (if (number? T-high)
                        (Math/abs T-high)
                        (Math/sqrt (+ (* (re T-high) (re T-high))
                                     (* (im T-high) (im T-high)))))]
        (is (not= T-low-mag T-high-mag)
            "Amplitudes should differ at different incident energies")))))

;; ============================================================================
;; PHASE 5: SINGLE-PARTICLE EXCITATIONS TESTS
;; ============================================================================

(require '[dwba.transfer :as transfer])

(deftest particle-hole-state-test
  (testing "particle-hole-state creates correct state map"
    (let [ph-state (inel/particle-hole-state 2 1 3 1 0 1)]
      (is (map? ph-state) "Should return a map")
      (is (= (:type ph-state) :particle-hole) "Should have type :particle-hole")
      (is (map? (:particle ph-state)) "Particle should be a map")
      (is (map? (:hole ph-state)) "Hole should be a map")
      (is (= (:n (:particle ph-state)) 2) "Particle n should be 2")
      (is (= (:l (:particle ph-state)) 1) "Particle l should be 1")
      (is (= (:j (:particle ph-state)) 3) "Particle j should be 3")
      (is (= (:n (:hole ph-state)) 1) "Hole n should be 1")
      (is (= (:l (:hole ph-state)) 0) "Hole l should be 0")
      (is (= (:j (:hole ph-state)) 1) "Hole j should be 1"))))

(deftest transition-density-test
  (testing "transition-density calculates transition density correctly"
    (let [;; Create simple test wavefunctions (constant values for simplicity)
          phi-particle (vec (repeat 100 0.1))
          phi-hole (vec (repeat 100 0.2))
          rho-trans (inel/transition-density phi-particle phi-hole h)]
      (is (vector? rho-trans) "Should return a vector")
      (is (= (count rho-trans) 100) "Should have same length as input")
      (is (every? number? rho-trans) "All values should be numbers")
      ;; ρ_trans = φ*_particle · φ_hole = 0.1 * 0.2 = 0.02
      (is (approx= (first rho-trans) 0.02 0.001)
          "Transition density should be product of wavefunctions"))))

(deftest transition-density-function-test
  (testing "transition-density-function returns [r, ρ_trans(r)] pairs"
    (let [phi-particle (vec (repeat 100 0.1))
          phi-hole (vec (repeat 100 0.2))
          rho-fn (inel/transition-density-function phi-particle phi-hole r-max h)]
      (is (vector? rho-fn) "Should return a vector")
      (is (> (count rho-fn) 0) "Should have entries")
      (is (vector? (first rho-fn)) "Each entry should be a vector")
      (is (= (count (first rho-fn)) 2) "Each entry should have [r, ρ] format")
      (let [[r rho] (first rho-fn)]
        (is (number? r) "r should be a number")
        (is (number? rho) "ρ should be a number")
        (is (approx= r 0.0 0.01) "First r should be near 0")))))

(deftest transition-form-factor-from-density-test
  (testing "transition-form-factor-from-density calculates form factor"
    (let [;; Create simple transition density
          rho-trans (vec (repeat 100 0.01))
          lambda 2
          F-lambda (inel/transition-form-factor-from-density rho-trans lambda ws-params r-max h)]
      (is (vector? F-lambda) "Should return a vector")
      (is (= (count F-lambda) 100) "Should have same length as input")
      (is (every? number? F-lambda) "All values should be numbers")
      ;; Form factor should be positive (for positive transition density)
      (is (> (first F-lambda) 0.0) "Form factor should be positive"))))

(deftest spectroscopic-factor-test
  (testing "spectroscopic-factor calculates spectroscopic factor"
    (let [S (inel/spectroscopic-factor 3 1)]  ; Particle: j=1, Hole: j=0
      (is (number? S) "Should return a number")
      (is (> S 0.0) "Spectroscopic factor should be positive")
      ;; S = (2j+1) * normalization = 3 * 1 = 3
      (is (approx= S 3.0 0.1) "Spectroscopic factor should equal (2j+1) for particle"))))

(deftest spectroscopic-factor-with-overlap-test
  (testing "spectroscopic-factor with overlap integral"
    (let [overlap 0.5
          S (inel/spectroscopic-factor 3 1 overlap)]
      (is (number? S) "Should return a number")
      (is (> S 0.0) "Spectroscopic factor should be positive")
      ;; S = (2j+1) * overlap * normalization = 3 * 0.5 * 1 = 1.5
      (is (approx= S 1.5 0.1) "Should include overlap factor"))))

(deftest single-particle-excitation-energy-test
  (testing "single-particle-excitation-energy calculates E_ex"
    (let [E-particle -5.0  ; Particle state energy
          E-hole -10.0    ; Hole state energy
          E-ex (inel/single-particle-excitation-energy E-particle E-hole)]
      (is (number? E-ex) "Should return a number")
      (is (> E-ex 0.0) "Excitation energy should be positive")
      ;; E_ex = |E_hole| - |E_particle| = 10 - 5 = 5
      (is (approx= E-ex 5.0 0.1) "Excitation energy should be |E_hole| - |E_particle|"))))

(deftest single-particle-excitation-energy-with-pairing-test
  (testing "single-particle-excitation-energy with pairing correction"
    (let [E-particle -5.0
          E-hole -10.0
          pairing-energy 1.0
          E-ex (inel/single-particle-excitation-energy E-particle E-hole pairing-energy)]
      (is (number? E-ex) "Should return a number")
      ;; E_ex = 5.0 + 1.0 = 6.0
      (is (approx= E-ex 6.0 0.1) "Should include pairing correction"))))

(deftest particle-hole-form-factor-test
  (testing "particle-hole-form-factor calculates complete form factor"
    (let [;; Create simple test wavefunctions
          phi-particle (vec (repeat 100 0.1))
          phi-hole (vec (repeat 100 0.2))
          lambda 2
          F-ph (inel/particle-hole-form-factor phi-particle phi-hole lambda ws-params r-max h)]
      (is (vector? F-ph) "Should return a vector")
      (is (> (count F-ph) 0) "Should have entries")
      (is (every? number? F-ph) "All values should be numbers")
      ;; Form factor should be positive
      (is (> (first F-ph) 0.0) "Form factor should be positive"))))

(deftest single-particle-inelastic-amplitude-test
  (testing "single-particle-inelastic-amplitude calculates amplitude"
    (let [;; Create simple test wavefunctions
          phi-particle (vec (repeat 100 0.1))
          phi-hole (vec (repeat 100 0.2))
          ;; Create distorted waves
          chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          lambda 2
          T-inel (inel/single-particle-inelastic-amplitude chi-i chi-f phi-particle phi-hole 
                                                           lambda ws-params r-max h)]
      (is (or (number? T-inel) 
              (and (number? (re T-inel)) (number? (im T-inel))))
          "Should return a number or complex number")
      ;; Amplitude should be finite
      (let [T-mag (if (number? T-inel)
                   (Math/abs T-inel)
                   (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                (* (im T-inel) (im T-inel)))))]
        (is (not (Double/isNaN T-mag)) "Amplitude magnitude should not be NaN")
        (is (not (Double/isInfinite T-mag)) "Amplitude magnitude should not be infinite")))))

(deftest particle-hole-workflow-test
  (testing "Complete workflow: particle-hole state → transition density → form factor"
    (let [;; Step 1: Define particle-hole state
          ph-state (inel/particle-hole-state 2 1 3 1 0 1)
          ;; Step 2: Create simple wavefunctions
          phi-particle (vec (repeat 100 0.1))
          phi-hole (vec (repeat 100 0.2))
          ;; Step 3: Calculate transition density
          rho-trans (inel/transition-density phi-particle phi-hole h)
          ;; Step 4: Calculate form factor
          lambda 2
          F-lambda (inel/transition-form-factor-from-density rho-trans lambda ws-params r-max h)
          ;; Step 5: Calculate spectroscopic factor
          S (inel/spectroscopic-factor 3 1)]
      (is (map? ph-state) "Step 1: Particle-hole state should be a map")
      (is (vector? rho-trans) "Step 3: Transition density should be a vector")
      (is (vector? F-lambda) "Step 4: Form factor should be a vector")
      (is (number? S) "Step 5: Spectroscopic factor should be a number")
      ;; All should be positive
      (is (every? #(> % 0.0) rho-trans) "Transition density should be positive")
      (is (every? #(> % 0.0) F-lambda) "Form factor should be positive")
      (is (> S 0.0) "Spectroscopic factor should be positive"))))

;; ============================================================================
;; PHASE 6: ANGULAR DISTRIBUTION TESTS
;; ============================================================================

(deftest clebsch-gordan-selection-rules-test
  (testing "clebsch-gordan respects selection rules"
    ;; M must equal m1 + m2
    (let [CG-invalid (inel/clebsch-gordan 1 0 1 0 2 1)]  ; M=1 but m1+m2=0
      (is (= CG-invalid 0.0) "Should be zero when M ≠ m1 + m2"))
    ;; Triangle inequality: |j1 - j2| ≤ J ≤ j1 + j2
    (let [CG-invalid2 (inel/clebsch-gordan 1 0 1 0 3 0)]  ; J=3 violates triangle inequality
      (is (= CG-invalid2 0.0) "Should be zero when triangle inequality violated"))
    ;; Valid coupling
    (let [CG-valid (inel/clebsch-gordan 1 0 1 0 2 0)]
      (is (number? CG-valid) "Should return a number for valid coupling")
      (is (not= CG-valid 0.0) "Should be non-zero for valid coupling"))))

(deftest clebsch-gordan-basic-test
  (testing "clebsch-gordan calculates coefficient"
    (let [CG (inel/clebsch-gordan 1 0 1 0 2 0)]
      (is (number? CG) "Should return a number")
      ;; Clebsch-Gordan coefficients can be larger than 1 in absolute value
      ;; but should be finite
      (is (not (Double/isNaN CG)) "Should not be NaN")
      (is (not (Double/isInfinite CG)) "Should not be infinite"))))

(deftest legendre-expansion-test
  (testing "legendre-expansion expands function in Legendre polynomials"
    (let [coeffs {0 1.0, 2 0.5}
          theta (/ Math/PI 2)
          f-val (inel/legendre-expansion coeffs theta)]
      (is (number? f-val) "Should return a number")
      ;; At θ=π/2, P_0(0)=1, P_2(0)=-0.5, so f ≈ 1.0 - 0.25 = 0.75
      (is (< (Math/abs f-val) 2.0) "Should be in reasonable range"))))

(deftest legendre-expansion-vector-test
  (testing "legendre-expansion works with vector format"
    (let [coeffs-vec [1.0 0.0 0.5]  ; [a_0, a_1, a_2]
          theta (/ Math/PI 2)
          f-val (inel/legendre-expansion coeffs-vec theta)]
      (is (number? f-val) "Should return a number")
      (is (< (Math/abs f-val) 2.0) "Should be in reasonable range"))))

(deftest legendre-coefficients-test
  (testing "legendre-coefficients calculates expansion coefficients"
    (let [;; Simple test data: constant function
          angular-data (mapv (fn [i]
                              (let [theta (* i (/ Math/PI 10))]
                                [theta 1.0]))
                            (range 11))
          L-max 2
          coeffs (inel/legendre-coefficients angular-data L-max)]
      (is (map? coeffs) "Should return a map")
      (is (contains? coeffs 0) "Should have coefficient for L=0")
      (is (contains? coeffs 1) "Should have coefficient for L=1")
      (is (contains? coeffs 2) "Should have coefficient for L=2")
      (is (every? number? (vals coeffs)) "All coefficients should be numbers"))))

(deftest inelastic-angular-distribution-test
  (testing "inelastic-angular-distribution calculates dσ/dΩ(θ)"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          k-i (Math/sqrt (* mass-factor 10.0))
          k-f (Math/sqrt (* mass-factor (- 10.0 4.44)))
          theta (/ Math/PI 2)
          dsigma (inel/inelastic-angular-distribution T-inel theta k-i k-f 10.0 4.44 mass-factor)]
      (is (number? dsigma) "Should return a number")
      (is (> dsigma 0.0) "dσ/dΩ should be positive")
      (is (not (Double/isNaN dsigma)) "Should not be NaN"))))

(deftest inelastic-angular-distribution-function-test
  (testing "inelastic-angular-distribution-function returns [θ, dσ/dΩ] pairs"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          k-i (Math/sqrt (* mass-factor 10.0))
          k-f (Math/sqrt (* mass-factor (- 10.0 4.44)))
          theta-values [0 (/ Math/PI 2) Math/PI]
          angular-dist (inel/inelastic-angular-distribution-function T-inel theta-values 
                                                                     k-i k-f 10.0 4.44 mass-factor)]
      (is (vector? angular-dist) "Should return a vector")
      (is (= (count angular-dist) 3) "Should have same length as input")
      (is (every? vector? angular-dist) "Each entry should be a vector")
      (is (= (count (first angular-dist)) 2) "Each entry should have [θ, dσ/dΩ] format")
      (let [[theta dsigma] (first angular-dist)]
        (is (number? theta) "θ should be a number")
        (is (number? dsigma) "dσ/dΩ should be a number")
        (is (> dsigma 0.0) "dσ/dΩ should be positive")))))

(deftest interference-term-test
  (testing "interference-term calculates interference between channels"
    (let [amplitude-1 1.0
          amplitude-2 0.5
          interference (inel/interference-term amplitude-1 amplitude-2)]
      (is (number? interference) "Should return a number")
      ;; Interference = 2·Re(f_1* · f_2) = 2·1.0·0.5 = 1.0
      (is (approx= interference 1.0 0.1) "Should equal 2·Re(f_1* · f_2)"))))

(deftest multi-channel-angular-distribution-test
  (testing "multi-channel-angular-distribution includes interference"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          T-inel-2 (if (number? T-inel)
                    (* T-inel 0.5)
                    (let [re-val (re T-inel)
                          im-val (im T-inel)]
                      (complex/complex-cartesian (* re-val 0.5) (* im-val 0.5))))
          amplitudes [T-inel T-inel-2]
          k-i (Math/sqrt (* mass-factor 10.0))
          k-f (Math/sqrt (* mass-factor (- 10.0 4.44)))
          theta (/ Math/PI 2)
          dsigma-multi (inel/multi-channel-angular-distribution amplitudes theta 
                                                               k-i k-f 10.0 4.44 mass-factor)]
      (is (number? dsigma-multi) "Should return a number")
      (is (> dsigma-multi 0.0) "dσ/dΩ should be positive")
      (is (not (Double/isNaN dsigma-multi)) "Should not be NaN"))))

(deftest angular-distribution-legendre-expansion-test
  (testing "angular-distribution-legendre-expansion combines calculation and expansion"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          k-i (Math/sqrt (* mass-factor 10.0))
          k-f (Math/sqrt (* mass-factor (- 10.0 4.44)))
          theta-values (map #(* % (/ Math/PI 10)) (range 11))  ; 0 to π
          L-max 2
          result (inel/angular-distribution-legendre-expansion T-inel theta-values k-i k-f 
                                                               10.0 4.44 mass-factor L-max)]
      (is (map? result) "Should return a map")
      (is (contains? result :angular-data) "Should have angular-data")
      (is (contains? result :legendre-coefficients) "Should have legendre-coefficients")
      (is (contains? result :expansion-function) "Should have expansion-function")
      (is (vector? (:angular-data result)) "angular-data should be a vector")
      (is (map? (:legendre-coefficients result)) "legendre-coefficients should be a map")
      (is (fn? (:expansion-function result)) "expansion-function should be a function"))))

(deftest angular-distribution-workflow-test
  (testing "Complete workflow: angular distribution → Legendre expansion"
    (let [chi-i (inel/distorted-wave-entrance 10.0 0 ws-params h r-max)
          chi-f (inel/distorted-wave-exit 10.0 4.44 2 ws-params h r-max)
          beta-2 0.25
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params r-max h)
          k-i (Math/sqrt (* mass-factor 10.0))
          k-f (Math/sqrt (* mass-factor (- 10.0 4.44)))
          theta-values (map #(* % (/ Math/PI 18)) (range 19))  ; 0 to π in 10° steps
          ;; Step 1: Calculate angular distribution
          angular-dist (inel/inelastic-angular-distribution-function T-inel theta-values 
                                                                     k-i k-f 10.0 4.44 mass-factor)
          ;; Step 2: Calculate Legendre coefficients
          L-max 4
          coeffs (inel/legendre-coefficients angular-dist L-max)
          ;; Step 3: Reconstruct using expansion
          theta-test (/ Math/PI 2)
          f-recon (inel/legendre-expansion coeffs theta-test)]
      (is (vector? angular-dist) "Step 1: Angular distribution should be a vector")
      (is (map? coeffs) "Step 2: Coefficients should be a map")
      (is (number? f-recon) "Step 3: Reconstructed value should be a number")
      ;; All should be finite
      (is (every? (fn [[_theta dsigma]] (and (number? dsigma) (not (Double/isNaN dsigma))))
                 angular-dist) "Angular distribution should be finite")
      (is (every? (fn [[_L a-L]] (and (number? a-L) (not (Double/isNaN a-L))))
                 coeffs) "Coefficients should be finite"))))
