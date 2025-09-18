;; Copy and paste this into the REPL to test tricomis-U-complex

;; First, require the necessary namespaces
(require '[fastmath.special :as special])
(require '[fastmath.complex :as cplx])

;; Test the specific function call you requested
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 2.0
      b cplx/I               ; i
      result (special/tricomis-U-complex z a b)]
  (println "=== Tricomis-U-Complex Test ===")
  (println "Input:")
  (println "  z = 2 - i = " z)
  (println "  a = " a)
  (println "  b = i = " b)
  (println "Result:")
  (println "  tricomis-U-complex(2-i, 2.0, i) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result))
  (println "  Magnitude = " (cplx/abs result))
  (println "  Phase = " (cplx/arg result) " radians"))

;; Additional test with different arguments
(let [z2 (cplx/complex 1 0)    ; 1 + 0i
      a2 1.0
      b2 cplx/I               ; i
      result2 (special/tricomis-U-complex z2 a2 b2)]
  (println "\n=== Additional Test ===")
  (println "tricomis-U-complex(1, 1.0, i) = " result2))

(println "\n✅ Test completed!")
