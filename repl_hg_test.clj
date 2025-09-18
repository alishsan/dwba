;; Copy and paste this into the REPL to test hg/tricomis-U-complex

;; First, require the necessary namespaces
(require '[fastmath.special.hypergeometric :as hg])
(require '[fastmath.complex :as cplx])

;; Test the specific function call you requested
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 4.0
      b cplx/I              ; imaginary unit
      result (hg/tricomis-U-complex z a b)]
  (println "=== HG Tricomis-U-Complex Test ===")
  (println "Input:")
  (println "  z = 2 - i = " z)
  (println "  a = " a)
  (println "  b = cplx/I = " b)
  (println "Result:")
  (println "  hg/tricomis-U-complex(2-i, 4.0, cplx/I) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result))
  (println "  Magnitude = " (cplx/abs result))
  (println "  Phase = " (cplx/arg result) " radians"))

;; Test with cplx/I (imaginary unit) instead of just cplx
(let [z2 (cplx/complex 2 -1)  ; 2 - i
      a2 4.0
      b2 cplx/I              ; imaginary unit
      result2 (hg/tricomis-U-complex z2 a2 b2)]
  (println "\n=== Test with cplx/I (imaginary unit) ===")
  (println "hg/tricomis-U-complex(2-i, 4.0, cplx/I) = " result2)
  (println "  Real part = " (cplx/real result2))
  (println "  Imaginary part = " (cplx/imag result2)))

(println "\n✅ Test completed!")
