(ns dwba.core
( :require
[fastmath.core :as m]
 [fastmath.polynomials :as poly]
  [fastmath.special :as spec]
[cljplot.render :as r]
[cljplot.build :as b]
[cljplot.core :refer [save xy-chart show]]
)
)
(use 'complex)
(use 'functions)

(defn sigma-L [E V a L]
(* (/ 2 E) Math/PI (+ (* 2 L) 1) (Math/pow (mag (subt2 1. (s-matrix E V a L)))  2) )
)

(def xs (range 0.1 5.1 0.01))


 (def phase-data (->> xs
                      (mapv (fn[p] [ p (r-matrix-a 1 [4 1 1] p 0)]))))

(def Ls (range 10))

 (def sigmaL-data (->> Ls
                      (mapv (fn[p] [ p (sigma-L 1 [4 1 1] 3 p)]))))

(-> (b/series [:scatter sigmaL-data {:color :red}])
    (b/preprocess-series)
    (r/render-lattice {:width 600 :height 300})
    (show))
