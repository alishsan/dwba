(ns dwba.fastmath-test
  (:require [clojure.test :refer :all]
            [fastmath.special :as spec]
            [fastmath.complex :as cplx]))

(deftest tricomi-U-complex-test
  (testing "tricomi-U-complex function with complex arguments"
    (let [z (cplx/complex 2 -1)  ; 2 - i
          a 4.0
          b cplx/I              ; imaginary unit
          result (spec/tricomi-U-complex z a b)]
      (println "=== Tricomi-U-Complex Test ===")
      (println "Input:")
      (println "  z = 2 - i = " z)
      (println "  a = " a)
      (println "  b = cplx/I = " b)
      (println "Result:")
      (println "  spec/tricomi-U-complex(2-i, 4.0, cplx/I) = " result)
      (println "  Real part = " (cplx/real result))
      (println "  Imaginary part = " (cplx/imag result))
      (println "  Magnitude = " (cplx/abs result))
      (println "  Phase = " (cplx/arg result) " radians")
      (is (number? (cplx/real result)))
      (is (number? (cplx/imag result)))
      (is (not (Double/isNaN (cplx/real result))))
      (is (not (Double/isNaN (cplx/imag result)))))))

(deftest fastmath-basic-test
  (testing "Basic fastmath functions work"
    (require '[fastmath.core :as fm])
    (is (= (fm/sin 0) 0.0))
    (is (= (fm/cos 0) 1.0))
    (is (= (fm/exp 0) 1.0))))

(deftest fastmath-complex-test
  (testing "Fastmath complex number operations"
    (let [z1 (cplx/complex 1 1)
          z2 (cplx/complex 2 2)
          sum (cplx/add z1 z2)]
      (is (= (cplx/real sum) 3.0))
      (is (= (cplx/imag sum) 3.0)))))
