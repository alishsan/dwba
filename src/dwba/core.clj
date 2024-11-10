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



(def xs (range 0.1 5.1 0.01))


 (def phase-data (->> xs
                      (mapv (fn[p] [ p (r-matrix-a 1 [4 1 1] p 0)]))))


(-> (b/series [:scatter phase-data {:color :red}])
    (b/preprocess-series)
    (r/render-lattice {:width 600 :height 300})
    (show))
