;; Simple tricomis-U-complex test
(require '[fastmath.core :as fm])
(require '[fastmath.special :as spec])

;; Test tricomis-U-complex function
(let [z (fm/complex 2.0 -1.0)  ; 2 - i
      a 2.0
      b (fm/complex 0.0 1.0)   ; i
      result (spec/tricomis-U-complex z a b)]
  (println "tricomis-U-complex(2-i, 2.0, i) = " result))

(println "Test completed!")
