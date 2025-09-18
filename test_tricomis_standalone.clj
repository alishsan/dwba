#!/usr/bin/env clojure

;; Test tricomis-U-complex function with correct fastmath namespaces
(require '[fastmath.special :as special])
(require '[fastmath.complex :as cplx])

(println "=== Testing Tricomis-U-Complex Function ===")

;; Test the specific function call you requested
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 2.0
      b cplx/I               ; i
      result (special/tricomis-U-complex z a b)]
  (println "Input:")
  (println "  z = 2 - i = " (cplx/complex 2 -1))
  (println "  a = " a)
  (println "  b = i = " cplx/I)
  (println "Result:")
  (println "  tricomis-U-complex(2-i, 2.0, i) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result))
  (println "  Magnitude = " (cplx/abs result))
  (println "  Phase = " (cplx/arg result) " radians"))

;; Test with some additional values
(println "\n=== Additional Tests ===")

;; Test with real arguments
(let [z-real (cplx/complex 1 0)    ; 1 + 0i
      a 1.0
      b cplx/I                     ; i
      result (special/tricomis-U-complex z-real a b)]
  (println "tricomis-U-complex(1, 1.0, i) = " result))

;; Test with different complex arguments
(let [z2 (cplx/complex 0.5 0.5)   ; 0.5 + 0.5i
      a 1.5
      b (cplx/complex 1 0)         ; 1 + 0i
      result (special/tricomis-U-complex z2 a b)]
  (println "tricomis-U-complex(0.5+0.5i, 1.5, 1) = " result))

(println "\n✅ Tricomis-U-Complex function test completed!")
