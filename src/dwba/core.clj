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



; E, V are multiplied by 2mu/(hbar)^2 to take into account the mass
; for neutron-alpha pair mu = 0.8 amu = 745 MeV/c^2 , 2mu/(hbar)^2 = 0.038 /(MeV fm^2)
(def mass-factor 0.038)

(def Es (range 1 51 1))

(def ws-stand [40 1.0 0.65]) ;standard WS function parameters, plus mass factor for R matrix equation
(def phase-data (->> xs
                     (mapv (fn[p] [ p (r-matrix-a 1 ws-stand p 0)]))))

(def phase-E-data (->> Es
                       (mapv (fn[p] [ p (re (phase-shift p ws-stand 3. 0))]))))

(def phase-E1-data (->> Es
                        (mapv (fn[p] [ p (re (phase-shift p ws-stand 3. 1))]))))

(def Ls (range 10))

 (def sigmaL-data (->> Ls
                      (mapv (fn[p] [ p (sigma-L 1 ws-stand 3 p)]))))

(defn plot-phase [x] (-> (b/series [:scatter x {:color :red}])
    (b/preprocess-series)
      (b/add-axes :bottom)
      (b/add-axes :left)
    (r/render-lattice {:width 600 :height 300})
    (show))
 )

;(-> (b/series [:scatter phase-E1-data {:color :blue}])
;    (b/preprocess-series)
;      (b/add-axes :bottom)
;      (b/add-axes :left)
;    (r/render-lattice {:width 600 :height 300})
;    (show))
