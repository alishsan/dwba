(ns functions
(:require
[fastmath.core :as m]
 [fastmath.polynomials :as poly]
  [fastmath.special :as spec]
))
(use 'complex)


(defn deriv
  ([fn1 x dx]
   (/ (- (fn1 (+ x dx)) (fn1 x)) dx))
  ([fn1 L x dx] ;for hankel functions with L dependence and complex numbers
   (div (subt2 (fn1 L (+ x dx)) (fn1 L x)) dx)))


(defn distorted-wave [k r]
  "Returns the distorted wave function at distance r for wavenumber k."
  (complex-from-polar (* k r) (* k r)))


(defn WS ;  Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0))
  [r [V0 R0 a0]]
  ( / (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0))))
  )

(defn WS-complex ;  Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0))
  [r [V0 R0 a0]]
  ( div (mul -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0))))
  )

(defn hankel0+ [L rho]
(-  (* rho (poly/eval-bessel-t L rho))   (* rho (poly/eval-bessel-t L rho)))
  )


(defn hankel0- [L rho]
(+  (* rho (poly/eval-bessel-t L rho))   (* rho (poly/eval-bessel-t L rho)))
  )

(defn r-matrix-a [^double E V  ^double a ^long L]  ;construct R-matrix * a depending on 1D Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [N  1000
dr (/ a N)]
(loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr]
(if (> x a)
(/ ur dudr) ;Ra = u/dudr  (dudr = d2udr2 * dr)
(recur  (+ x dr) (WS x V) (*  (+ (/ (* L (inc L)) (* x x)) (-  pot E)) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr))) ) 
)))


(defn f-func [L rho] (* rho (spec/spherical-bessel-j L rho)))
(defn g-func [L rho] (* -1 rho (spec/spherical-bessel-y L rho)))

(defn hankel0+ [L rho]
  (complex-from-cartesian (g-func L rho ) (f-func L rho))
  )

(defn hankel0- [L rho]
  (complex-from-cartesian (g-func L rho ) (* -1.0 (f-func L rho)))
  )



(defn s-matrix [^double E V  ^double a ^long L]
  (let [ra (r-matrix-a E V a L)]
    (div (subt2 (hankel0- L a) (mul ra (deriv hankel0- L a 0.000001)) )
         (subt2 (hankel0+ L a) (mul ra (deriv hankel0+ L a 0.000001)) ))
 ))

(defn k-matrix [^double E V  ^double a ^long L]
  (let [ra (r-matrix-a E V a L)]
    (div (subt2  (mul ra (deriv f-func L a 0.0000001))  (f-func L a))
         (subt2 (g-func L a) (mul ra (deriv g-func L a 0.0000001)) ))
 ))

(defn phase-shift  [^double E V  ^double a ^long L ]
  (let [s (s-matrix E V a L)]
    (div (arg s) 2)
    ))

(defn sigma-L [E V a L]
(* (/ 2 E) Math/PI (+ (* 2 L) 1) (Math/pow (mag (subt2 1. (s-matrix E V a L)))  2) )
)




