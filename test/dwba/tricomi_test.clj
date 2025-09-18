(ns dwba.tricomi-test
  (:require [clojure.test :refer :all]
            [fastmath.special.hypergeometric :as hg]
            [fastmath.complex :as cplx]))

(deftest tricomi-U-complex-test
  (testing "tricomi-U-complex function with complex arguments"
    (let [z (cplx/complex 2 -1)  ; 2 - i
          a 4.0
          b cplx/I              ; imaginary unit
          result (hg/tricomi-U-complex z a b)]
      (println "=== Tricomi-U-Complex Test ===")
      (println "Input:")
      (println "  z = 2 - i = " z)
      (println "  a = " a)
      (println "  b = cplx/I = " b)
      (println "Result:")
      (println "  hg/tricomi-U-complex(2-i, 4.0, cplx/I) = " result)
      (println "  Real part = " (cplx/real result))
      (println "  Imaginary part = " (cplx/imag result))
      (println "  Magnitude = " (cplx/abs result))
      (println "  Phase = " (cplx/arg result) " radians")
      (is (number? (cplx/real result)))
      (is (number? (cplx/imag result)))
      (is (not (Double/isNaN (cplx/real result))))
      (is (not (Double/isNaN (cplx/imag result)))))))

(deftest tricomi-U-complex-additional-test
  (testing "tricomi-U-complex with different arguments"
    (let [z (cplx/complex 1 0)    ; 1 + 0i
          a 2.0
          b cplx/I               ; i
          result (hg/tricomi-U-complex z a b)]
      (println "\n=== Additional Test ===")
      (println "hg/tricomi-U-complex(1, 2.0, cplx/I) = " result)
      (println "  Real part = " (cplx/real result))
      (println "  Imaginary part = " (cplx/imag result))
      (is (number? (cplx/real result)))
      (is (number? (cplx/imag result))))))
