;; Copy and paste this into the REPL to test hg/tricomis-U-complex

;; First, require the necessary namespaces
(require '[fastmath.special.hypergeometric :as hg])
(require '[fastmath.complex :as cplx])

;; Test the corrected function call
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 4.0
      b cplx/I              ; imaginary unit (not just cplx)
      result (hg/tricomi-U-complex z a b)]
  (println "=== HG Tricomi-U-Complex Test ===")
  (println "Input:")
  (println "  z = 2 - i = " z)
  (println "  a = " a)
  (println "  b = cplx/I = " b)
  (println "Result:")
  (println "  hg/tricomi-U-complex(2-i, 4.0, cplx/I) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result))
  (println "  Magnitude = " (cplx/abs result))
  (println "  Phase = " (cplx/arg result) " radians"))

(println "\n✅ Test completed!")
