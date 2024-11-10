(ns dwba.core
( :require
[fastmath.core :as m]
 [fastmath.polynomials :as poly]
  [fastmath.special :as spec]
)
)
(use 'complex)
;(use 'functions)



(def xs (range 0.1 5.1 0.01))


 (def phase-data (->> xs
                      (mapv (fn[p] {:x p, "Ra_L" (Ra 1 [4 1 1] p 0)}))))


