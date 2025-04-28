(ns dwba.core
( :require
[fastmath.core :as m]
[fastmath.vector :as v]
 [fastmath.polynomials :as poly]
 [fastmath.special :as spec]
   [fastmath.vector :as v]
[cljplot.render :as r]
[cljplot.build :as b]
[cljplot.core :refer [save xy-chart show]]
)
)
(use 'complex)
(use 'functions)

(def xs (range 1 15 0.1))

(def angles (range 0.01 1.55 0.01)) ; angles for cross-sections

(def Es  (range 1 51 1))

(def ws-stand [40 2.0 0.6]) ;standard WS function parameters
(def phase-data0 (->> xs
                     (mapv (fn[p] [ p (r-matrix-a 1 ws-stand p 0)]))))
(def shift-data (->> xs
                     (mapv (fn[p] [ p (phase-shift 10 ws-stand p 0)]))))

(def phase0-E0-data (->> Es
                       (mapv (fn[p] [ p (re (phase-shift0 p ws-stand 3. 0))]))))

(def phase0-E1-data (->> Es
                        (mapv (fn[p] [ p (re (phase-shift0 p ws-stand 3. 1))]))))

(def Ls (range 20))

 (def sigmaL-data (->> Ls
                       (mapv (fn[p] [ p (sigma-L0 1 ws-stand 3 p)]))))

(def phase-E1-data (->> Es
                        (mapv (fn[p] [ p (phase-shift p ws-stand 1)]))))

(defn plot-phase [x] (-> (b/series [:scatter x {:color :red}])
    (b/preprocess-series)
      (b/add-axes :bottom)
      (b/add-axes :left)
      (r/render-lattice {:width 600 :height 300})
      (show))
 )




