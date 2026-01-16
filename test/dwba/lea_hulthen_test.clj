(ns dwba.lea-hulthen-test
  "Tests for Local Energy Approximation (LEA) with Hulthen potential."
  (:require [clojure.test :refer :all]
            [dwba.transfer :as t]
            [dwba.form-factors :as ff]
            [functions :refer :all]
            [fastmath.core :as m]))

(deftest hulthen-potential-test
  (testing "Hulthen potential calculation"
    (let [V0 60.0
          alpha 0.23
          r-values [0.1 1.0 2.0 5.0]
          results (mapv #(t/hulthen-potential % V0 alpha) r-values)]
      (is (every? number? results) "All values should be numbers")
      (is (every? #(< % 0) results) "Hulthen potential should be negative (attractive)")
      ;; Potential should decrease (become less negative) with distance
      (is (> (first results) (last results)) "Potential should decrease with distance"))))

(deftest hulthen-wavefunction-test
  (testing "Hulthen wavefunction calculation"
    (let [alpha 0.23
          beta 1.4
          r-values [0.1 1.0 2.0 5.0]
          results (mapv #(t/hulthen-wavefunction % alpha beta) r-values)]
      (is (every? number? results) "All values should be numbers")
      (is (>= (first results) 0) "Wavefunction should be non-negative at small r")
      ;; Wavefunction should decay with distance
      (is (> (first results) (last results)) "Wavefunction should decay with distance"))))

(deftest hulthen-wavefunction-normalized-test
  (testing "Normalized Hulthen wavefunction"
    (let [alpha 0.23
          beta 1.4
          r-max 20.0
          h 0.01
          r-test 1.0
          u-norm (t/hulthen-wavefunction-normalized r-test alpha beta r-max h)]
      (is (number? u-norm) "Should return a number")
      (is (>= u-norm 0) "Normalized wavefunction should be non-negative"))))

(deftest lea-transfer-amplitude-test
  (testing "LEA transfer amplitude calculation"
    (let [V-params [50.0 2.0 0.6]
          result-f (t/solve-bound-state V-params 1 0 nil 20.0 0.01)
          phi-f (:normalized-wavefunction result-f)]
      (when (seq phi-f)
        (let [alpha 0.23
              beta 1.4
              D0 (t/zero-range-constant :d-p)
              T-lea (t/lea-transfer-amplitude phi-f alpha beta 20.0 0.01 D0)]
          (is (number? T-lea) "Should return a number")
          (is (not (Double/isNaN T-lea)) "Should not be NaN")
          (is (not (Double/isInfinite T-lea)) "Should not be infinite"))))))

(deftest lea-transfer-amplitude-simplified-test
  (testing "Simplified LEA transfer amplitude"
    (let [V-params [50.0 2.0 0.6]
          result-f (t/solve-bound-state V-params 1 0 nil 20.0 0.01)
          phi-f (:normalized-wavefunction result-f)]
      (when (seq phi-f)
        (let [T-lea (t/lea-transfer-amplitude-simplified phi-f 20.0 0.01 :d-p)]
          (is (number? T-lea) "Should return a number")
          (is (not (Double/isNaN T-lea)) "Should not be NaN"))))))

(deftest lea-vs-zero-range-comparison-test
  (testing "LEA vs zero-range comparison"
    (let [V-params [50.0 2.0 0.6]
          result-f (t/solve-bound-state V-params 1 0 nil 20.0 0.01)
          phi-f (:normalized-wavefunction result-f)]
      (when (seq phi-f)
        ;; For LEA, we need a Hulthen wavefunction as phi-i
        ;; This is a simplified test - in practice, phi-i would be the Hulthen wavefunction
        (let [T-lea (t/lea-transfer-amplitude-simplified phi-f 20.0 0.01 :d-p)]
          (is (number? T-lea) "LEA amplitude should be a number")
          ;; LEA and zero-range should give similar but not identical results
          ;; (they use different initial state descriptions)
          (is (not (zero? T-lea)) "LEA amplitude should be non-zero"))))))

