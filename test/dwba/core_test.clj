(ns dwba.core-test
  (:require [clojure.test :refer :all]
            [complex :refer :all]
            [functions :refer :all]))

(deftest basic-functionality-test
  (testing "Basic complex number operations work"
    (let [z1 (complex-cartesian 1 2)
          z2 (complex-cartesian 3 4)
          sum (add z1 z2)]
      (is (= (re sum) 4))
      (is (= (im sum) 6)))))

(deftest woods-saxon-potential-test
  (testing "Woods-Saxon potential calculation"
    (let [ws-params [40.0 2.0 0.6]  ; V0=40, R0=2, a0=0.6
          potential-at-r0 (WS 2.0 ws-params)]
      (is (< potential-at-r0 0))  ; Should be negative
      (is (> (Math/abs potential-at-r0) 0)))))

(deftest r-matrix-calculation-test
  (testing "R-matrix calculation returns reasonable values"
    (let [ws-params [40.0 2.0 0.6]
          r-matrix-val (r-matrix-a 10.0 ws-params 3.0 0)]
      (is (number? r-matrix-val))
      (is (not (Double/isNaN r-matrix-val))))))

(deftest complex-arithmetic-test
  (testing "Complex number arithmetic operations"
    (let [z1 (complex-cartesian 3 4)
          z2 (complex-polar 0 2)
          product (mul z1 z2)]
      (is (number? (re product)))
      (is (number? (im product))))))

(deftest woods-saxon-basic-test
  (testing "Woods-Saxon potential basic properties"
    (let [ws-params [40.0 2.0 0.6]
          potential-at-origin (WS 0.1 ws-params)
          potential-at-r0 (WS 2.0 ws-params)]
      (is (< potential-at-origin 0))
      (is (< potential-at-r0 0))
      (is (> (Math/abs potential-at-origin) (Math/abs potential-at-r0))))))
