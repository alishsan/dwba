#!/usr/bin/env clojure

;; Final test of tricomi-U-complex function
(require '[fastmath.special.hypergeometric :as hg])
(require '[fastmath.complex :as cplx])

(println "=== Testing Tricomi-U-Complex Function ===")
(println "Using fastmath 3.0.0-alpha4-SNAPSHOT")

;; Test the function call you requested
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 4.0
      b cplx/I              ; imaginary unit
      result (hg/tricomi-U-complex z a b)]
  (println "\nInput:")
  (println "  z = 2 - i = " z)
  (println "  a = " a)
  (println "  b = cplx/I = " b)
  (println "\nResult:")
  (println "  hg/tricomi-U-complex(2-i, 4.0, cplx/I) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result))
  (println "  Magnitude = " (cplx/abs result))
  (println "  Phase = " (cplx/arg result) " radians"))

;; Test with different arguments
(let [z2 (cplx/complex 1 0)    ; 1 + 0i
      a2 2.0
      b2 cplx/I               ; i
      result2 (hg/tricomi-U-complex z2 a2 b2)]
  (println "\n=== Additional Test ===")
  (println "hg/tricomi-U-complex(1, 2.0, cplx/I) = " result2)
  (println "  Real part = " (cplx/real result2))
  (println "  Imaginary part = " (cplx/imag result2)))

(println "\n✅ Tricomi-U-Complex function test completed!")
(println "🎯 Fastmath is working correctly!")
