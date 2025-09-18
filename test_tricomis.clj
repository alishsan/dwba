#!/usr/bin/env clojure

;; Test fastmath tricomis-U-complex function
(require '[fastmath.core :as fm])
(require '[fastmath.special :as spec])

;; Load our complex number implementation
(load-file "src/complex.clj")

(println "=== Testing Tricomis-U-Complex Function ===")

;; Test the specific function call you requested
(let [z (complex-from-cartesian 2.0 -1.0)  ; 2 - i
      a 2.0
      b cplx/I  ; i
      result (spec/tricomis-U-complex z a b)]
  (println "Input:")
  (println "  z = 2 - i = " (str (re z) " + " (im z) "i"))
  (println "  a = " a)
  (println "  b = i")
  (println "Result:")
  (println "  tricomis-U-complex(2-i, 2.0, i) = " (str (re result) " + " (im result) "i"))
  (println "  Magnitude = " (Math/sqrt (+ (* (re result) (re result)) (* (im result) (im result)))))
  (println "  Phase = " (Math/atan2 (im result) (re result)) " radians"))

;; Test with some additional values to verify the function works
(println "\n=== Additional Tests ===")

;; Test with real arguments
(let [z-real (complex-from-cartesian 1.0 0.0)  ; 1 + 0i
      a 1.0
      b (complex-from-cartesian 0.0 1.0)  ; 0 + i
      result (spec/tricomis-U-complex z-real a b)]
  (println "tricomis-U-complex(1, 1.0, i) = " (str (re result) " + " (im result) "i")))

;; Test with different complex arguments
(let [z2 (complex-from-cartesian 0.5 0.5)  ; 0.5 + 0.5i
      a 1.5
      b (complex-from-cartesian 1.0 0.0)  ; 1 + 0i
      result (spec/tricomis-U-complex z2 a b)]
  (println "tricomis-U-complex(0.5+0.5i, 1.5, 1) = " (str (re result) " + " (im result) "i")))

(println "\n✅ Tricomis-U-Complex function test completed!")
