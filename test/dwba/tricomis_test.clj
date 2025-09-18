(ns dwba.tricomis-test
  (:require [clojure.test :refer :all]
            [fastmath.special :as special]
            [fastmath.complex :as cplx]))

(deftest tricomis-U-complex-test
  (testing "tricomis-U-complex function with complex arguments"
    (let [z (cplx/complex 2 -1)  ; 2 - i
          a 2.0
          b cplx/I               ; i
          result (special/tricomis-U-complex z a b)]
      (println "tricomis-U-complex(2-i, 2.0, i) = " result)
      (is (number? (cplx/real result)))
      (is (number? (cplx/imag result)))
      (is (not (Double/isNaN (cplx/real result))))
      (is (not (Double/isNaN (cplx/imag result)))))))

(deftest tricomis-U-complex-additional-test
  (testing "tricomis-U-complex with different arguments"
    (let [z (cplx/complex 1 0)    ; 1 + 0i
          a 1.0
          b cplx/I               ; i
          result (special/tricomis-U-complex z a b)]
      (println "tricomis-U-complex(1, 1.0, i) = " result)
      (is (number? (cplx/real result)))
      (is (number? (cplx/imag result))))))
