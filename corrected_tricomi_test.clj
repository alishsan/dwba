;; Corrected tricomi-U-complex test using spec namespace
(require '[fastmath.special :as spec])
(require '[fastmath.complex :as cplx])

(println "=== Testing Tricomi-U-Complex Function ===")
(println "Using fastmath.special namespace (spec)")

;; Test the function call you requested
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 4.0
      b cplx/I              ; imaginary unit
      result (spec/tricomi-U-complex z a b)]
  (println "\nInput:")
  (println "  z = 2 - i = " z)
  (println "  a = " a)
  (println "  b = cplx/I = " b)
  (println "\nResult:")
  (println "  spec/tricomi-U-complex(2-i, 4.0, cplx/I) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result))
  (println "  Magnitude = " (cplx/abs result))
  (println "  Phase = " (cplx/arg result) " radians"))

(println "\n✅ Tricomi-U-Complex function test completed!")
