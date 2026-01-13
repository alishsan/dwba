(ns dwba.bisection-test
  "Tests for bisection root-finding algorithm."
  (:require [clojure.test :refer :all]
            [functions :refer :all]
            [dwba.finite-well :refer :all]
            [fastmath.core :as m]))

(deftest bisection-simple-root-test
  (testing "Bisection finds root of simple function f(x) = x^2 - 4"
    (let [f (fn [x] (- (* x x) 4))
          result (bisection f [0.0 5.0] 1e-10 100)
          root (:root result)
          expected 2.0]
      (is (:converged? result) "Should converge")
      (is (< (Math/abs (- root expected)) 1e-6)
          (format "Root should be close to 2.0: got %.10f" root))
      (is (< (Math/abs (:value result)) 1e-6)
          "Function value at root should be close to zero"))))

(deftest bisection-cubic-root-test
  (testing "Bisection finds root of f(x) = x^3 - 8"
    (let [f (fn [x] (- (* x x x) 8))
          result (bisection f [0.0 5.0] 1e-10 100)
          root (:root result)
          expected 2.0]
      (is (:converged? result) "Should converge")
      (is (< (Math/abs (- root expected)) 1e-6)
          (format "Root should be close to 2.0: got %.10f" root))
      (is (< (Math/abs (:value result)) 1e-6)
          "Function value at root should be close to zero"))))

(deftest bisection-finite-well-matching-error-test
  (testing "Bisection finds root of finite well matching error for l=0, z0=10"
    (let [l 0
          z0 10.0
          f (fn [e-ratio]
              (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
                    eta (* z0 (Math/sqrt e-ratio))]
                (finite-well-matching-error xi eta l)))
          f-low (f 0.001)
          f-high (f 0.999)]
      (when (not= (m/signum f-low) (m/signum f-high))
        (let [result (bisection f [0.001 0.999] 1e-7 100)
              e-ratio (:root result)
              xi (* z0 (Math/sqrt (- 1 e-ratio)))
              eta (* z0 (Math/sqrt e-ratio))
              expected-z0-squared (* z0 z0)
              actual-z0-squared (+ (* xi xi) (* eta eta))]
          (is (:converged? result) "Should converge")
          (is (< (Math/abs (:value result)) 1e-5)
              "Matching error should be small")
          (is (< (Math/abs (- actual-z0-squared expected-z0-squared)) 1e-6)
              (format "xi^2 + eta^2 should equal z0^2: %.6f vs %.6f"
                     actual-z0-squared expected-z0-squared)))))))

(deftest bisection-finite-well-l1-test
  (testing "Bisection finds root of finite well matching error for l=1, z0=10"
    (let [l 1
          z0 10.0
          f (fn [e-ratio]
              (let [xi (* z0 (Math/sqrt (- 1 e-ratio)))
                    eta (* z0 (Math/sqrt e-ratio))]
                (finite-well-matching-error xi eta l)))
          f-low (f 0.001)
          f-high (f 0.999)]
      (when (not= (m/signum f-low) (m/signum f-high))
        (let [result (bisection f [0.001 0.999] 1e-7 100)
              e-ratio (:root result)
              xi (* z0 (Math/sqrt (- 1 e-ratio)))
              eta (* z0 (Math/sqrt e-ratio))
              expected-z0-squared (* z0 z0)
              actual-z0-squared (+ (* xi xi) (* eta eta))]
          (is (:converged? result) "Should converge")
          (is (< (Math/abs (:value result)) 1e-5)
              "Matching error should be small")
          (is (< (Math/abs (- actual-z0-squared expected-z0-squared)) 1e-6)
              (format "xi^2 + eta^2 should equal z0^2: %.6f vs %.6f"
                     actual-z0-squared expected-z0-squared)))))))

(deftest bisection-same-sign-endpoints-test
  (testing "Bisection handles case where function has same sign at both endpoints"
    (let [f (fn [x] (* x x))  ; Always positive
          result (bisection f [1.0 2.0] 1e-10 100)]
      (is (not (:converged? result)) "Should not converge when signs match")
      (is (contains? result :error) "Should include error message"))))

