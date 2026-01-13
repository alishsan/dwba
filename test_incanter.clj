(require '[incanter.core :as incanter])
(require '[incanter.charts :as charts])

;(def data [1 2 3 4 5 6 7 8 9 10])
 ;(def data23 (plot-function solve-bound-state-numerov -40.0 -10.0 0.1 0 50.0 2.0 0.6 0.01 15.0))
;(def chart23 (charts/xy-plot (mapv first data23) (map :f (mapv last data23))))

 ;(def er45 (  solve-bound-state-numerov -40.0 0 50.0 3.0 0.2 0.01 15.0))
 ;(def chart (charts/xy-plot (range 0. 15. 0.01) er45))

  (def er45 (plot-function  f-r-numerov 0.01 3. 0.01 -40.0 0 50.0 2.0 0.1 ))
 (def chart (charts/xy-plot (mapv first er45) (mapv last er45)))
(incanter/view chart)
(incanter/save chart "output/solver1.png" :width 800 :height 600)
