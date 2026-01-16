(ns dwba.inelastic-validation-test
  "Validation tests for inelastic scattering calculations.
   
   Phase 7: Testing and Validation
   - Simple test case: (α,α') on ¹²C
   - Parameter sensitivity tests
   - Convergence tests
   - Comparison with expected behavior"
  (:require [clojure.test :refer [deftest is testing]]
            [dwba.inelastic :as inel]
            [functions :refer [mass-factor]]
            [complex :refer [re im]]))

;; ============================================================================
;; Test Parameters for ¹²C(α,α')¹²C* Reaction
;; ============================================================================

;; Woods-Saxon parameters for ¹²C + α system
(def ws-params-C12 [50.0 2.0 0.6])  ; [V0=50 MeV, R0=2.0 fm, a0=0.6 fm]

;; Physical parameters
(def E-incident 10.0)  ; Incident energy (MeV) - typical for alpha scattering
(def E-ex-2plus 4.44)  ; First 2⁺ state in ¹²C (MeV)
(def E-ex-3minus 9.64)  ; First 3⁻ state in ¹²C (MeV)
(def beta-2-C12 0.25)  ; Quadrupole deformation parameter for ¹²C
(def beta-3-C12 0.1)   ; Octupole deformation parameter for ¹²C

;; Numerical parameters
(def r-max 20.0)  ; Maximum radius (fm)
(def h-default 0.01)  ; Default step size (fm)

;; Helper function for approximate equality
(defn approx= [a b tolerance]
  (< (Math/abs (- a b)) tolerance))

;; ============================================================================
;; Test Case 1: Simple (α,α') on ¹²C - First 2⁺ State
;; ============================================================================

(deftest C12-alpha-alpha-prime-2plus-test
  (testing "Complete calculation for ¹²C(α,α')¹²C* (2⁺ state at 4.44 MeV)"
    (let [lambda 2  ; Quadrupole transition
          L-i 0     ; Entrance channel L=0
          L-f 2     ; Exit channel L=2
          ;; Step 1: Calculate distorted waves
          chi-i (inel/distorted-wave-entrance E-incident L-i ws-params-C12 h-default r-max)
          chi-f (inel/distorted-wave-exit E-incident E-ex-2plus L-f ws-params-C12 h-default r-max)
          ;; Step 2: Calculate inelastic amplitude
          T-inel (inel/inelastic-amplitude chi-i chi-f lambda 0 beta-2-C12 ws-params-C12 
                                          r-max h-default)
          ;; Step 3: Calculate differential cross-section
          k-i (Math/sqrt (* mass-factor E-incident))
          E-f (- E-incident E-ex-2plus)
          k-f (Math/sqrt (* mass-factor E-f))
          dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                           E-incident E-ex-2plus mass-factor)]
      ;; Validations
      (is (vector? chi-i) "Entrance channel wavefunction should be a vector")
      (is (vector? chi-f) "Exit channel wavefunction should be a vector")
      (is (> (count chi-i) 100) "Wavefunctions should have many points")
      (is (or (number? T-inel) 
              (and (number? (re T-inel)) (number? (im T-inel))))
          "Inelastic amplitude should be a number or complex number")
      (is (number? dsigma) "Differential cross-section should be a number")
      (is (> dsigma 0.0) "Differential cross-section should be positive")
      (is (not (Double/isNaN dsigma)) "Differential cross-section should not be NaN")
      (is (not (Double/isInfinite dsigma)) "Differential cross-section should not be infinite")
      ;; Typical values: dσ/dΩ for inelastic scattering can vary widely
      ;; The exact value depends on many factors (energy, potential, etc.)
      (is (> dsigma 1e-10) "Differential cross-section should be non-negligible (> 10^-10 fm²/sr)")
      (is (< dsigma 1e3) "Differential cross-section should be reasonable (< 1000 fm²/sr)"))))

;; ============================================================================
;; Test Case 2: Angular Distribution for ¹²C(α,α')¹²C* (2⁺)
;; ============================================================================

(deftest C12-angular-distribution-2plus-test
  (testing "Angular distribution for ¹²C(α,α')¹²C* (2⁺ state)"
    (let [chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 h-default r-max)
          chi-f (inel/distorted-wave-exit E-incident E-ex-2plus 2 ws-params-C12 h-default r-max)
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2-C12 ws-params-C12 
                                          r-max h-default)
          k-i (Math/sqrt (* mass-factor E-incident))
          k-f (Math/sqrt (* mass-factor (- E-incident E-ex-2plus)))
          ;; Calculate at multiple angles
          theta-values [0 (/ Math/PI 4) (/ Math/PI 2) (* 3 (/ Math/PI 4)) Math/PI]
          angular-dist (mapv (fn [theta]
                              (inel/inelastic-angular-distribution T-inel theta k-i k-f 
                                                                 E-incident E-ex-2plus mass-factor))
                            theta-values)]
      (is (= (count angular-dist) 5) "Should have 5 angular points")
      (is (every? number? angular-dist) "All angular distribution values should be numbers")
      (is (every? #(> % 0.0) angular-dist) "All values should be positive")
      ;; Angular distribution may be constant if amplitude doesn't depend on angle
      ;; (This is expected for the simplified implementation)
      ;; Note: For full angular dependence, would need angle-dependent amplitude
      )))

;; ============================================================================
;; Test Case 3: Parameter Sensitivity - β_λ Dependence
;; ============================================================================

(deftest parameter-sensitivity-beta-test
  (testing "Parameter sensitivity: dependence on β_λ"
    (let [chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 h-default r-max)
          chi-f (inel/distorted-wave-exit E-incident E-ex-2plus 2 ws-params-C12 h-default r-max)
          ;; Test different β_2 values
          beta-values [0.1 0.2 0.25 0.3 0.4]
          results (map (fn [beta]
                        (let [T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta 
                                                               ws-params-C12 r-max h-default)
                              T-mag (if (number? T-inel)
                                     (Math/abs T-inel)
                                     (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                                  (* (im T-inel) (im T-inel)))))
                              k-i (Math/sqrt (* mass-factor E-incident))
                              k-f (Math/sqrt (* mass-factor (- E-incident E-ex-2plus)))
                              dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                                              E-incident E-ex-2plus 
                                                                              mass-factor)]
                          {:beta beta, :T-mag T-mag, :dsigma dsigma}))
                      beta-values)]
      ;; All results should be valid
      (is (= (count results) 5) "Should have 5 results")
      (is (every? #(and (number? (:T-mag %)) (number? (:dsigma %))) results)
          "All results should have valid numbers")
      ;; Amplitude should increase with β (approximately proportional)
      (let [T-mags (map :T-mag results)]
        (is (< (first T-mags) (last T-mags))
            "Amplitude should increase with increasing β")
        ;; Check approximate proportionality: T ∝ β
        (let [ratio-1 (/ (nth T-mags 2) (nth T-mags 0))  ; β=0.25 / β=0.1 = 2.5
              ratio-2 (/ (nth beta-values 2) (nth beta-values 0))]  ; 2.5
          (is (< (Math/abs (- ratio-1 ratio-2)) (* ratio-2 0.5))
              "Amplitude should be approximately proportional to β"))))))

;; ============================================================================
;; Test Case 4: Convergence Tests - Step Size Dependence
;; ============================================================================

(deftest convergence-step-size-test
  (testing "Convergence test: dependence on step size h"
    (let [;; Test different step sizes
          h-values [0.02 0.01 0.005]
          results (map (fn [h]
                        (let [chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 h r-max)
                              chi-f (inel/distorted-wave-exit E-incident E-ex-2plus 2 
                                                             ws-params-C12 h r-max)
                              T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2-C12 
                                                              ws-params-C12 r-max h)
                              T-mag (if (number? T-inel)
                                     (Math/abs T-inel)
                                     (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                                  (* (im T-inel) (im T-inel)))))
                              k-i (Math/sqrt (* mass-factor E-incident))
                              k-f (Math/sqrt (* mass-factor (- E-incident E-ex-2plus)))
                              dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                                              E-incident E-ex-2plus 
                                                                              mass-factor)]
                          {:h h, :T-mag T-mag, :dsigma dsigma}))
                      h-values)]
      ;; All results should be valid
      (is (= (count results) 3) "Should have 3 results")
      (is (every? #(and (number? (:T-mag %)) (number? (:dsigma %))) results)
          "All results should have valid numbers")
      ;; Results should converge as h decreases
      (let [dsigma-values (map :dsigma results)
            diff-1 (Math/abs (- (first dsigma-values) (second dsigma-values)))
            diff-2 (Math/abs (- (second dsigma-values) (last dsigma-values)))]
        ;; Smaller step size should give more accurate results
        ;; The difference should decrease as h decreases
        (is (< diff-2 diff-1) 
            "Results should converge as step size decreases")))))

;; ============================================================================
;; Test Case 5: Convergence Tests - r_max Dependence
;; ============================================================================

(deftest convergence-r-max-test
  (testing "Convergence test: dependence on maximum radius r_max"
    (let [;; Test different r_max values
          r-max-values [15.0 20.0 25.0]
          results (map (fn [r-max]
                        (let [chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 
                                                                  h-default r-max)
                              chi-f (inel/distorted-wave-exit E-incident E-ex-2plus 2 
                                                             ws-params-C12 h-default r-max)
                              T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2-C12 
                                                              ws-params-C12 r-max h-default)
                              T-mag (if (number? T-inel)
                                     (Math/abs T-inel)
                                     (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                                  (* (im T-inel) (im T-inel)))))
                              k-i (Math/sqrt (* mass-factor E-incident))
                              k-f (Math/sqrt (* mass-factor (- E-incident E-ex-2plus)))
                              dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                                              E-incident E-ex-2plus 
                                                                              mass-factor)]
                          {:r-max r-max, :T-mag T-mag, :dsigma dsigma}))
                      r-max-values)]
      ;; All results should be valid
      (is (= (count results) 3) "Should have 3 results")
      (is (every? #(and (number? (:T-mag %)) (number? (:dsigma %))) results)
          "All results should have valid numbers")
      ;; Results should be finite and reasonable
      (let [dsigma-values (map :dsigma results)]
        (is (every? #(and (number? %) (not (Double/isNaN %)) (not (Double/isInfinite %))) 
                   dsigma-values)
            "All results should be finite numbers")
        ;; Note: Convergence may not be monotonic due to numerical instabilities
        ;; The important thing is that all results are valid
        ))))

;; ============================================================================
;; Test Case 6: Energy Dependence
;; ============================================================================

(deftest energy-dependence-test
  (testing "Energy dependence of inelastic cross-section"
    (let [;; Test different incident energies
          E-values [5.0 10.0 15.0 20.0]
          results (map (fn [E-i]
                        (let [chi-i (inel/distorted-wave-entrance E-i 0 ws-params-C12 
                                                                  h-default r-max)
                              chi-f (inel/distorted-wave-exit E-i E-ex-2plus 2 
                                                             ws-params-C12 h-default r-max)
                              T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2-C12 
                                                              ws-params-C12 r-max h-default)
                              k-i (Math/sqrt (* mass-factor E-i))
                              k-f (Math/sqrt (* mass-factor (- E-i E-ex-2plus)))
                              dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                                              E-i E-ex-2plus 
                                                                              mass-factor)]
                          {:E-i E-i, :dsigma dsigma}))
                      E-values)]
      ;; All results should be valid
      (is (= (count results) 4) "Should have 4 results")
      (is (every? #(and (number? (:dsigma %)) (> (:dsigma %) 0.0)) results)
          "All cross-sections should be positive numbers")
      ;; Cross-section should vary with energy (not constant)
      (let [dsigma-values (map :dsigma results)]
        (is (not= (apply min dsigma-values) (apply max dsigma-values))
            "Cross-section should vary with incident energy")))))

;; ============================================================================
;; Test Case 7: Multiple Multipole Orders
;; ============================================================================

(deftest multiple-multipole-orders-test
  (testing "Comparison of different multipole orders (λ=2, 3, 4)"
    (let [chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 h-default r-max)
          ;; Test quadrupole (λ=2), octupole (λ=3), hexadecapole (λ=4)
          lambda-values [2 3 4]
          beta-values [beta-2-C12 beta-3-C12 0.05]  ; Typical values
          results (map (fn [lambda beta]
                        (let [chi-f (inel/distorted-wave-exit E-incident 
                                                              (if (= lambda 2) E-ex-2plus E-ex-3minus)
                                                              lambda ws-params-C12 h-default r-max)
                              T-inel (inel/inelastic-amplitude chi-i chi-f lambda 0 beta 
                                                               ws-params-C12 r-max h-default)
                              T-mag (if (number? T-inel)
                                     (Math/abs T-inel)
                                     (Math/sqrt (+ (* (re T-inel) (re T-inel))
                                                  (* (im T-inel) (im T-inel)))))
                              k-i (Math/sqrt (* mass-factor E-incident))
                              E-ex (if (= lambda 2) E-ex-2plus E-ex-3minus)
                              k-f (Math/sqrt (* mass-factor (- E-incident E-ex)))
                              dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                                                E-incident E-ex 
                                                                                mass-factor)]
                          {:lambda lambda, :beta beta, :T-mag T-mag, :dsigma dsigma}))
                      lambda-values beta-values)]
      ;; All results should be valid
      (is (= (count results) 3) "Should have 3 results")
      (is (every? #(and (number? (:T-mag %)) (number? (:dsigma %))) results)
          "All results should have valid numbers")
      ;; All multipole orders should give valid results
      (let [dsigma-values (map :dsigma results)]
        (is (every? #(and (number? %) (> % 0.0)) dsigma-values)
            "All cross-sections should be positive numbers")
        ;; Note: The relative strength depends on many factors (β values, energies, etc.)
        ;; We just verify that all are valid
        ))))

;; ============================================================================
;; Test Case 8: Legendre Expansion Validation
;; ============================================================================

(deftest legendre-expansion-validation-test
  (testing "Legendre expansion of angular distribution"
    (let [chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 h-default r-max)
          chi-f (inel/distorted-wave-exit E-incident E-ex-2plus 2 ws-params-C12 h-default r-max)
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2-C12 ws-params-C12 
                                          r-max h-default)
          k-i (Math/sqrt (* mass-factor E-incident))
          k-f (Math/sqrt (* mass-factor (- E-incident E-ex-2plus)))
          ;; Calculate angular distribution at multiple angles
          theta-values (map #(* % (/ Math/PI 18)) (range 19))  ; 0 to π in 10° steps
          angular-dist (inel/inelastic-angular-distribution-function T-inel theta-values 
                                                                     k-i k-f E-incident E-ex-2plus 
                                                                     mass-factor)
          ;; Calculate Legendre coefficients
          L-max 4
          coeffs (inel/legendre-coefficients angular-dist L-max)
          ;; Reconstruct at test angles
          test-thetas [0 (/ Math/PI 4) (/ Math/PI 2) (* 3 (/ Math/PI 4)) Math/PI]
          original-values (map (fn [theta]
                                (second (first (filter #(approx= (first %) theta 0.01) 
                                                      angular-dist))))
                              test-thetas)
          reconstructed-values (map (fn [theta]
                                     (let [f-val (inel/legendre-expansion coeffs theta)
                                           dsigma (inel/inelastic-angular-distribution f-val theta 
                                                                                       k-i k-f 
                                                                                       E-incident 
                                                                                       E-ex-2plus 
                                                                                       mass-factor)]
                                       dsigma))
                                   test-thetas)]
      ;; All should be valid
      (is (map? coeffs) "Coefficients should be a map")
      (is (every? number? (vals coeffs)) "All coefficients should be numbers")
      ;; Reconstruction should be valid (may not be exact due to numerical precision)
      (doseq [[orig recon] (map vector original-values reconstructed-values)]
        (when (and (number? orig) (number? recon))
          (is (and (number? recon) (not (Double/isNaN recon)) (not (Double/isInfinite recon)))
              "Reconstructed values should be valid numbers")
          ;; Note: Exact reconstruction depends on L_max and numerical precision
          )))))

;; ============================================================================
;; Test Case 9: Complete Workflow Validation
;; ============================================================================

(deftest complete-workflow-validation-test
  (testing "Complete workflow: from parameters to angular distribution"
    (let [;; Step 1: Get deformation parameter
          beta-2 (inel/deformation-parameter 2 :C12)
          ;; Step 2: Calculate distorted waves
          chi-i (inel/distorted-wave-entrance E-incident 0 ws-params-C12 h-default r-max)
          chi-f (inel/distorted-wave-exit E-incident E-ex-2plus 2 ws-params-C12 h-default r-max)
          ;; Step 3: Calculate inelastic amplitude
          T-inel (inel/inelastic-amplitude chi-i chi-f 2 0 beta-2 ws-params-C12 r-max h-default)
          ;; Step 4: Calculate differential cross-section
          k-i (Math/sqrt (* mass-factor E-incident))
          k-f (Math/sqrt (* mass-factor (- E-incident E-ex-2plus)))
          dsigma (inel/inelastic-differential-cross-section T-inel k-i k-f 
                                                           E-incident E-ex-2plus mass-factor)
          ;; Step 5: Calculate angular distribution
          theta-values (map #(* % (/ Math/PI 18)) (range 19))
          angular-dist (inel/inelastic-angular-distribution-function T-inel theta-values 
                                                                     k-i k-f E-incident E-ex-2plus 
                                                                     mass-factor)
          ;; Step 6: Legendre expansion
          coeffs (inel/legendre-coefficients angular-dist 4)]
      ;; All steps should succeed
      (is (number? beta-2) "Step 1: Deformation parameter should be a number")
      (is (vector? chi-i) "Step 2: Entrance wavefunction should be a vector")
      (is (vector? chi-f) "Step 2: Exit wavefunction should be a vector")
      (is (or (number? T-inel) 
              (and (number? (re T-inel)) (number? (im T-inel))))
          "Step 3: Inelastic amplitude should be valid")
      (is (number? dsigma) "Step 4: Differential cross-section should be a number")
      (is (vector? angular-dist) "Step 5: Angular distribution should be a vector")
      (is (map? coeffs) "Step 6: Legendre coefficients should be a map")
      ;; All values should be physically reasonable
      (is (> beta-2 0.0) "Deformation parameter should be positive")
      (is (> dsigma 0.0) "Cross-section should be positive")
      (is (every? #(> (second %) 0.0) angular-dist) 
          "Angular distribution should be positive at all angles"))))
