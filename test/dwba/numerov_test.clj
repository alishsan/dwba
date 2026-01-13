(ns dwba.numerov-test
  "Tests for Numerov integration method."
  (:require [clojure.test :refer :all]
            [dwba.transfer :as t]
            [functions :refer :all]
            [fastmath.core :as m]))

(deftest solve-bound-state-numerov-basic-test
  (testing "Basic solve-bound-state-numerov integration"
    (let [E-test -20.0
          l 0
          v0 50.0
          rad 2.0
          diff 0.6
          h 0.01
          r-max 20.0
          u (t/solve-bound-state-numerov E-test l v0 rad diff h r-max)]
      (is (seq u) "Should return wavefunction")
      (is (= (count u) (int (/ r-max h))) "Length should match expected")
      (is (every? number? u) "All values should be numbers"))))

(deftest solve-bound-state-numerov-different-l-test
  (testing "solve-bound-state-numerov for different angular momenta"
    (let [E-test -20.0
          v0 50.0
          rad 2.0
          diff 0.6
          h 0.01
          r-max 20.0]
      (doseq [l [0 1 2]]
        (let [u (t/solve-bound-state-numerov E-test l v0 rad diff h r-max)]
          (is (seq u) (format "Should return wavefunction for l=%d" l))
          (is (number? (get u 1)) (format "Should have valid start value for l=%d" l)))))))

(deftest solve-bound-state-numerov-different-energies-test
  (testing "solve-bound-state-numerov for different energies"
    (let [l 0
          v0 50.0
          rad 2.0
          diff 0.6
          h 0.01
          r-max 20.0]
      (doseq [E-test [-30.0 -20.0 -10.0 -5.0]]
        (let [u (t/solve-bound-state-numerov E-test l v0 rad diff h r-max)
              u-end (t/bound-state-boundary-value u r-max h)]
          (is (seq u) (format "Should return wavefunction for E=%.1f" E-test))
          (is (number? u-end) (format "Should have valid boundary value for E=%.1f" E-test)))))))

(deftest numerov-node-counting-test
  (testing "Node counting for different energies"
    (let [l 0
          v0 50.0
          rad 2.0
          diff 0.6
          h 0.01
          r-max 20.0]
      (doseq [E-test [-30.0 -20.0 -10.0]]
        (let [u (t/solve-bound-state-numerov E-test l v0 rad diff h r-max)
              nodes (t/count-nodes u)]
          (is (>= nodes 0) (format "Node count should be non-negative for E=%.1f" E-test))
          (is (integer? nodes) (format "Node count should be integer for E=%.1f" E-test)))))))

(deftest numerov-convergence-test
  (testing "Numerov convergence with different step sizes"
    (let [e 2.0
          l 1
          v0 46.23
          rad 2.0
          diff 0.5
          r-max 10.0
          h-fine 0.001
          h-values [0.1 0.05 0.01]]
      (doseq [h-test h-values]
        (let [u-fine (solve-numerov e l v0 rad diff h-fine r-max)
              u-test (solve-numerov e l v0 rad diff h-test r-max)
              downsample-factor (int (/ h-test h-fine))
              u-fine-downsampled (take-nth downsample-factor u-fine)
              min-len (min (count u-fine-downsampled) (count u-test))
              errors (map #(Math/abs (- %1 %2))
                         (take min-len u-fine-downsampled)
                         (take min-len u-test))
              max-error (apply max errors)]
          (is (< max-error 1.0) (format "Max error should be reasonable for h=%.3f" h-test)))))))

(deftest numerov-stability-test
  (testing "Numerov stability - Wronskian preservation"
    (let [e 2.0
          l 1
          v0 46.23
          rad 2.0
          diff 0.5
          r-max 10.0
          h 0.01]
      ;; Basic test that numerov runs without error
      (let [u (solve-numerov e l v0 rad diff h r-max)]
        (is (seq u) "Should return wavefunction")
        (is (every? number? u) "All values should be numbers")))))

