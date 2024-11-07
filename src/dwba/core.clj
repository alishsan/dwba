(ns dwba.core
( :require
[fastmath.core :as m]
 [fastmath.polynomials :as poly]
  [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
 [aerial.hanami.core :as hmi]
 [nextjournal.clerk :as clerk]
)
)
(use 'complex)
;(use 'functions)


(defn nuclear-potential [r]
  "Returns the potential at a distance r from the center of the nucleus."
  (let [V0 -50.0  ; example potential depth
        r0 1.2]   ; nuclear radius constant
    (/ V0 (+ 1 (Math/exp (- (/ r r0)))))))



(defn distorted-wave [k r]
  "Returns the distorted wave function at distance r for wavenumber k."
  (complex-from-polar (* k r) (* k r)))

(defn t-matrix-element [k1 k2 r-max n]
  "Calculates the T-matrix element for initial and final wave numbers k1 and k2."
  (complex-integrate (fn [r]
                       (let [initial (complex-conjugate (distorted-wave k1 r)) 
                     final (distorted-wave k2 r)
                     potential (nuclear-potential r)]
                 (reduce mul [potential initial final] )))
             0 r-max n))

(defn dwba-calc [initial-momentum final-momentum]
  (let [r-max 10.0  ; integration limit for radial distance in femtometers
        n 1000      ; number of intervals for integration
        t-element (t-matrix-element initial-momentum final-momentum r-max n)]
    t-element))

(defn phase-shift [^double E ^double V ^double R]  ;construct phase shift depending on 1D potential
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [N  1000
dr (/ R N)]
(loop [x 0 ur 0 d2udr2 0 dudr 1]
(if (> x R)
[ur dudr]
(recur  (+ x dr) (+ ur (* dudr dr)) (* (- V E) ur) (+ dudr (* d2udr2 dr))))
)))

(def xs (range 1 10 0.1))

(def ys (map phase-shift (repeat (count xs) -4) (repeat (count xs) -2) xs))

 (def phase-data (->> xs
                      (mapv (fn[p] {:x p, :y (first (phase-shift -2 -4 p))}))))

(def point-chart
  (hc/xform ht/point-chart
            :DATA phase-data
            :X :x
            :Y :y
            )
  )

(clerk/vl point-chart)
