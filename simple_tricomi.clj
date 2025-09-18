;; Simple tricomi-U-complex test
(require '[fastmath.special.hypergeometric :as hg])
(require '[fastmath.complex :as cplx])

(let [z (cplx/complex 2 -1)
      a 4.0
      b cplx/I
      result (hg/tricomi-U-complex z a b)]
  (println "tricomi-U-complex(2-i, 4.0, i) = " result))

(println "Test completed!")
