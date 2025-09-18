;; Test if REPL is ready and fastmath is working
(require '[fastmath.special.hypergeometric :as hg])
(require '[fastmath.complex :as cplx])

(println "=== Testing REPL and Fastmath ===")

;; Test basic fastmath
(require '[fastmath.core :as fm])
(println "Basic fastmath test:")
(println "  sin(1.0) = " (fm/sin 1.0))
(println "  cos(1.0) = " (fm/cos 1.0))

;; Test tricomi-U-complex function
(require '[fastmath.special :as spec])
(let [z (cplx/complex 2 -1)  ; 2 - i
      a 4.0
      b cplx/I              ; imaginary unit
      result (spec/tricomi-U-complex z a b)]
  (println "\nTricomi-U-Complex test:")
  (println "  spec/tricomi-U-complex(2-i, 4.0, cplx/I) = " result)
  (println "  Real part = " (cplx/real result))
  (println "  Imaginary part = " (cplx/imag result)))

(println "\n✅ REPL and Fastmath are working!")
