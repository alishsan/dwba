(ns plot
  (:require [cljplot.render :as r]
            [cljplot.build :as b]
            [cljplot.common :as common]
            [fastmath.stats :as stats]
            [clojure2d.color :as c]
            [cljplot.scale :as s]
            [fastmath.core :as m]
            [fastmath.random :as rnd]
            [cljplot.core :refer [save xy-chart show]]
            [java-time :as dt]
            [clojure2d.core :as c2d]
            [clojure2d.pixels :as p]
            [fastmath.complex :as cx]
            [fastmath.fields :as f]
            [fastmath.vector :as v]
            [fastmath.interpolation.gp :as gp]
            [fastmath.kernel :as k]))
  
;; logo

(rnd/set-seed! rnd/default-rng 2)

(def logo (p/to-pixels (c2d/with-oriented-canvas-> :bottom-left+ (c2d/canvas 200 100)
                       (c2d/set-font "Raleway Thin")
                       (c2d/set-background :black)
                       (c2d/set-color :white)
                       (c2d/set-font-attributes 60)
                       (c2d/text "cljplot" 20 70))))

(let [gradient (c/gradient (c/palette :two-heads-filonov))
      side-conf {:color (c/set-alpha (gradient 0.3) 200) :area? true :margins {:x [0.02 0.02]}}
      pairs (->> (repeatedly #(v/vec2 (rnd/irand 200) (rnd/irand 100)))
                 (filter #(pos? (c/luma (apply p/get-color logo %))))
                 (map #(v/add % (v/generate-vec2 rnd/grand)))
                 (take 2500))]
  (-> (b/series [:grid]
                [:scatter pairs {:size (fn [_ _] (* 10 (m/pow (rnd/drand 0.1 1.0) 3)))
                                 :color (fn [[x _] conf]
                                          (let [[mn mx] (get-in conf [:extent :x 1])]
                                            (c/set-alpha (gradient (m/norm x mn mx)) 50)))}])
      (b/preprocess-series)
      (b/add-side :top 25 (b/series [:density (map first pairs) side-conf]))
      (b/add-side :right 25 (b/series [:density (map second pairs) side-conf]))
      (b/add-axes :bottom)
      (b/add-axes :left)
      (b/add-label :bottom "cljplot charting library")
      (r/render-lattice {:width 600 :height 300})
      (save "results/examples/logo.jpg")
      (show)))

(def boundary-left -200.0)
(def boundary-right 200.0)
(def width (- boundary-right boundary-left))

(defn correct-path
  [path]
  (first (reduce (fn [[new-path shift-x shift-y] [[curr-x curr-y] [next-x next-y]]]
                   (let [s-curr-x (+ shift-x curr-x)
                         s-curr-y (+ shift-y curr-y)
                         s-next-x (+ shift-x next-x)
                         s-next-y (+ shift-y next-y)
                         new-shift-x (cond
                                       (< s-next-x boundary-left) (+ shift-x width)
                                       (> s-next-x boundary-right) (- shift-x width)
                                       :else shift-x)
                         new-shift-y (cond
                                       (< s-next-y boundary-left) (+ shift-y width)
                                       (> s-next-y boundary-right) (- shift-y width)
                                       :else shift-y)]
                     [(if (and (== new-shift-x shift-x)
                               (== new-shift-y shift-y))
                        (conj new-path [s-curr-x s-curr-y])
                        (conj new-path
                              [s-curr-x s-curr-y] [s-next-x s-next-y]
                              [##NaN ##NaN] ;; new chunk separator
                              [(+ curr-x new-shift-x) (+ curr-y new-shift-y)]))
                      new-shift-x
                      new-shift-y]))
                 [[] 0.0 0.0] (partition 2 1 path))))

(defn walk-step
  [[x y]]
  [(+ x 2.0 (rnd/grand 2.0))
   (+ y 0.5 (rnd/grand 2.0))])

(def some-walk (iterate walk-step [0.0 0.0]))

(-> (b/series [:grid] [:line (correct-path (take 3000 some-walk))
                       {:color [0 50 255 150] :margins nil}])
    (b/preprocess-series)
    (b/update-scale :x :domain [-200 200])
    (b/update-scale :y :domain [-200 200])
    (b/add-axes :bottom)
    (b/add-axes :left)
    (r/render-lattice {:width 800 :height 800})
    (save "results/examples/random-walk-wrapped.jpg")
    (show))
