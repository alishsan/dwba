(ns dwba.bound-state-test
  "Tests for bound state wavefunction solver."
  (:require [clojure.test :refer :all]
            [dwba.transfer :as t]
            [functions :refer :all]
            [fastmath.core :as m]))

(def ws-params [50.0 2.0 0.6])  ; V0=50 MeV, R0=2.0 fm, a0=0.6 fm

(deftest bound-state-1s-test
  (testing "Finding 1s bound state (n=1, l=0)"
    (let [result (t/solve-bound-state ws-params 1 0 nil 20.0 0.01)]
      (is (:energy result) "Should find an energy")
      (is (< (:energy result) 0) "Energy should be negative (bound state)")
      (is (> (Math/abs (:energy result)) 0.5) "Energy should be significant")
      (is (:normalized-wavefunction result) "Should have wavefunction")
      (is (seq (:normalized-wavefunction result)) "Wavefunction should not be empty")
      (is (= (:nodes result) 0) "1s state should have 0 nodes"))))

(deftest bound-state-1p-test
  (testing "Finding 1p bound state (n=1, l=1)"
    (let [result (t/solve-bound-state ws-params 1 1 nil 20.0 0.01)]
      (is (:energy result) "Should find an energy")
      (is (< (:energy result) 0) "Energy should be negative (bound state)")
      (is (:normalized-wavefunction result) "Should have wavefunction")
      (is (= (:nodes result) 0) "1p state should have 0 nodes"))))

(deftest bound-state-2s-test
  (testing "Finding 2s bound state (n=2, l=0) - should have 1 node"
    (let [result (t/solve-bound-state ws-params 2 0 nil 20.0 0.01)]
      (is (:energy result) "Should find an energy")
      (is (< (:energy result) 0) "Energy should be negative (bound state)")
      (is (:normalized-wavefunction result) "Should have wavefunction")
      (is (= (:nodes result) 1) "2s state should have 1 node"))))

(deftest bound-state-energy-approximation-test
  (testing "Energy approximation vs calculated energy"
    (let [E-est (t/bound-state-energy-approx ws-params 1 0)
          result (t/solve-bound-state ws-params 1 0 nil 20.0 0.01)
          E-calc (:energy result)]
      (is (number? E-est) "Estimated energy should be a number")
      (is (number? E-calc) "Calculated energy should be a number")
      (is (< (Math/abs (- E-est E-calc)) 20.0)
          "Estimated and calculated energies should be reasonably close"))))

(deftest bound-state-normalization-test
  (testing "Bound state wavefunction normalization"
    (let [result (t/solve-bound-state ws-params 1 0 nil 20.0 0.01)
          u (:normalized-wavefunction result)
          h (:h result)
          ;; Calculate ∫ u²(r) dr using Simpson's rule
          integrand (mapv #(* % %) u)
          n (count integrand)
          simpson-sum (loop [i 1 sum 0.0]
                       (if (>= i (dec n))
                         sum
                         (let [coeff (if (odd? i) 4.0 2.0)
                               term (* coeff (get integrand i))]
                           (recur (inc i) (+ sum term)))))
          integral (* (/ h 3.0)
                     (+ (first integrand)
                        (last integrand)
                        simpson-sum))]
      (is (< (Math/abs (- integral 1.0)) 0.01)
          (format "Normalization should be close to 1.0: got %.8f" integral)))))

(deftest bound-state-boundary-condition-test
  (testing "Bound state wavefunction satisfies boundary condition u(r_max) ≈ 0"
    (let [result (t/solve-bound-state ws-params 1 0 nil 20.0 0.01)
          u (:normalized-wavefunction result)
          r-max (:r-max result)
          h (:h result)
          idx (min (dec (count u)) (int (/ r-max h)))
          u-end (get u idx)]
      (is (< (Math/abs u-end) 0.01)
          (format "u(r_max) should be close to 0: got %.6e" u-end)))))

(deftest find-bound-state-energy-test
  (testing "find-bound-state-energy finds bound states for l=0"
    (let [result (t/find-bound-state-energy ws-params 0 1 20.0 0.01)]
      (is (seq result) "Should find at least one bound state")
      (doseq [state result]
        (when (map? state)
          (is (:energy state) "Should have energy")
          (is (< (:energy state) 0) "Energy should be negative")
          (is (number? (:energy state)) "Energy should be a number"))))))

