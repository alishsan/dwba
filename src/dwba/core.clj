(ns dwba.core
( :require
[fastmath.core :as m]
 [fastmath.polynomials :as poly]
  [fastmath.special :as spec]
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

(defn WS ;  Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0))
  [r [V0 R0 a0]]
  ( / (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0))))
  )

(defn hankel0+ [L rho]
(-  (* rho (poly/eval-bessel-t L rho))   (* rho (poly/eval-bessel-t L rho)))
  )


(defn hankel0- [L rho]
(+  (* rho (poly/eval-bessel-t L rho))   (* rho (poly/eval-bessel-t L rho)))
  )

(defn Ra [^double E V  ^double R ^long L]  ;construct R-matrix * a depending on 1D Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [N  1000
dr (/ R N)]
(loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr]
(if (> x R)
(/ ur  dudr) ;Ra = u/dudr  (dudr = d2udr2 * dr)
(recur  (+ x dr) (WS x V) (*  (+ (/ (* L (inc L)) (* x x)) (-  pot E)) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr))) ) 
)))

(def xs (range 0.1 5.1 0.01))


 (def phase-data (->> xs
                      (mapv (fn[p] {:x p, "Ra_L" (Ra 1 [4 1 1] p 0)}))))

(defn ffunc [L rho] (* rho (spec/spherical-bessel-j L rho)))
(defn gfunc [L rho] (* -1 rho (spec/spherical-bessel-y L rho)))

(defn hankel0+ [L rho]
  (complex-from-cartesian (gfunc L rho ) (ffunc L rho))
  )


(defn hankel0- [L rho]
  (complex-from-cartesian (gfunc L rho ) (* -1.0 (ffunc L rho)))
  )


(defmacro deriv
  ([fn1 x dx]
`(/ (- (~fn1 (+ ~x ~dx)) (~fn1 ~x)) ~dx)
   )

    ([fn1 n x dx]
`(/ (- (~fn1 ~n (+ ~x ~dx)) (~fn1 ~n ~x)) ~dx)
   )
  )

