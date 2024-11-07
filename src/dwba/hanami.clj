(ns dwba.hanami
  (:require [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.core :as hmi]
            ))

(def point-chart
  (hc/xform ht/point-chart
            :UDATA "https://vega.github.io/vega-lite/data.cars.json"
            :X "Horsepower"
            :Y "Miles_per_Gallon"
            :COLOR "Origin")
  )
