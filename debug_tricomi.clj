;; Debug tricomi-U-complex function
(require '[fastmath.special :as spec])
(require '[fastmath.complex :as cplx])

(println "=== Debugging Tricomi-U-Complex Function ===")

;; Test with the exact function name you used
(println "Testing spec/tricomis-U-complex (with 's'):")
(let [z (cplx/complex 2 -1)
      a 4.0
      b cplx/I
      result (spec/tricomis-U-complex z a b)]
  (println "  spec/tricomis-U-complex(2-i, 4.0, cplx/I) = " result))

;; Test with corrected function name (without 's')
(println "\nTesting spec/tricomi-U-complex (without 's'):")
(let [z (cplx/complex 2 -1)
      a 4.0
      b cplx/I
      result (spec/tricomi-U-complex z a b)]
  (println "  spec/tricomi-U-complex(2-i, 4.0, cplx/I) = " result))

;; Test with simpler arguments
(println "\nTesting with simpler arguments:")
(let [z (cplx/complex 1 0)  ; 1 + 0i
      a 1.0
      b (cplx/complex 0 1)  ; 0 + i
      result (spec/tricomi-U-complex z a b)]
  (println "  spec/tricomi-U-complex(1, 1.0, i) = " result))

;; Test with real arguments
(println "\nTesting with real arguments:")
(let [z (cplx/complex 1 0)  ; 1 + 0i
      a 1.0
      b (cplx/complex 1 0)  ; 1 + 0i
      result (spec/tricomi-U-complex z a b)]
  (println "  spec/tricomi-U-complex(1, 1.0, 1) = " result))

;; Check what tricomi functions are available
(println "\nAvailable tricomi functions:")
(println "  tricomi-U-complex: " (resolve 'spec/tricomi-U-complex))
(println "  tricomis-U-complex: " (resolve 'spec/tricomis-U-complex))

(println "\n✅ Debug completed!")
