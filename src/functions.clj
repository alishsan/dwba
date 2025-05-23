(ns functions
(:require
[fastmath.core :as m]
[fastmath.polynomials :as poly]
 [fastmath.special.hypergeometric :as hg]
 [fastmath.special :as spec]
   [fastmath.vector :as v]
))
(use 'complex)

(def hbarc 197.7) ;MeV-fm
(def mu 745) ;MeV/c^2


(def mass-factor (/ (* 2 mu) hbarc hbarc )); mass-factor * E = k^2


(def Z1Z2ee (* 2 1.44) ; alpha and proton
  );Z1Z2 e^2 (MeV fm)


(defn deriv
  ([fn1 x dx]
   (/ (- (fn1 (+ x dx)) (fn1 x)) dx))
  ([fn1 L x dx] ;for hankel functions with L dependence and complex numbers
   (div (subt2 (fn1 L (+ x dx)) (fn1 L x)) dx))

  ([fn1 L eta x dx] ;for hankel functions with L dependence and complex numbers
   (div (subt2 (fn1 L eta (+ x dx)) (fn1 L eta x)) dx))
)
(defn subtract-second [a b] [(first a) (- (second a) (second b))])

(defn to-vec2 [x] (v/vec2 (re x) (im x)))

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


(defn xi ;h-bar and speed of light c are set to 1
  [^double E V  ^double a ^long L]  ;no coulomb ;construct R-matrix * a depending on 1D Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [
dr 0.00001]
  (loop [x dr pot 0 d2udr2 (/ 1. dr) 
dudr dr
 ur (* dudr dr) accum []]
(if (> x a)
accum
(recur  (+ x dr) (WS x V) (*  (+ (/ (* L (inc L)) (* x x)) (* mass-factor (-  pot E))) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr)) (into accum [[x ur]])) ) 
)))

(defn r-matrix-a 
  [^double E V  ^double a ^long L]  ;no coulomb ;construct R-matrix * a depending on 1D Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [
dr 0.00001]
(loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr]
(if (> x a)
(/ ur dudr) ;Ra = u/dudr  (dudr = d2udr2 * dr)
(recur  (+ x dr) (WS x V) (*  (+ (/ (* L (inc L)) (* x x)) (* mass-factor (-  pot E))) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr))) ) 
)))

(defn Coulomb-pot ([ r r0] ; potential of a uniformly charged sphere of charge e and radius r0
                   (if (> r r0) (/  Z1Z2ee r) (* r (/ Z1Z2ee r0 r0))))
   ([Ze r r0] ; potential of a uniformly charged sphere of charge e and radius r0
  (if (> r r0) (/  Ze r) (* r (/ Ze r0 r0))))
  ) 
  


(defn f-func [L rho] (* rho (spec/spherical-bessel-j L rho)))
(defn g-func [L rho] (* -1 rho (spec/spherical-bessel-y L rho)))

(defn hankel0+ [L rho]
  (complex-from-cartesian (g-func L rho ) (f-func L rho))
  )

(defn hankel0- [L rho]
  (complex-from-cartesian (g-func L rho ) (* -1.0 (f-func L rho)))
  )

(defn CL [L ^double eta] 
(->   (* Math/PI eta -0.5)
(Math/exp)
(* (Math/pow 2 L))
(mul (mag (gamma-complex   (complex-from-cartesian (inc L) eta))))
(div (m/factorial (inc L)))
)
)
;(defn hypergeometric-1F1)

(defn factorial [n]
(if (= n 1) 1 (* (factorial (- n 1)))
))

(defn s-matrix0 [^double E V  ^double a ^long L]
  (let [ra (r-matrix-a E V a L)
         k (m/sqrt (*  mass-factor E))
        rho (* k a)]
    (div (subt2 (hankel0- L rho) (mul ra k (deriv hankel0- L rho 0.000001)) )
         (subt2 (hankel0+ L rho) (mul ra k (deriv hankel0+ L rho 0.000001)) ))
 ))



(defn k-matrix0 [^double E V  ^double a ^long L]
  (let [ra (r-matrix-a E V a L)]
    (div (subt2  (mul ra (deriv f-func L a 0.0000001))  (f-func L a))
         (subt2 (g-func L a) (mul ra (deriv g-func L a 0.0000001)) ))
 ))

(defn phase-shift0  [^double E V  ^double a ^long L ]
  (let [s (s-matrix0 E V a L)]
    (/ (arg s) 2)
    ))

(defn sigma-L0 [E V a L]
(* (/ 2 E) Math/PI (+ (* 2 L) 1) (Math/pow (mag (subt2 1. (s-matrix0 E V a L)))  2) )
)


          

(defn rising-factorial
  "Rising (Pochhammer) factorial."
  [n x]
    (div (gamma-complex (add x n))
             (gamma-complex x)))


(defn pocn
  [ac b z n]
   (div (mul (npow z n) (rising-factorial n ac)) (mul (rising-factorial n b) (m/factorial n)))
)

(defn hypergeometric-complex-1F1
  "Kummer's (confluent hypergeometric, 1F1) function for compex arguments."
  [ac b z]
 (->> (range 20) (map #(pocn ac b z %)) (reduce add))   
)


(defn pocn2F0 ;used for hypergeometric-2F0
  [a1 a2 z n]
  (div  (mul  (npow z n)  (rising-factorial n a1) (rising-factorial n a2)) (m/factorial n))
)

(defn hypergeometric-complex-2F0
[a1 a2 z]
 ;(->> (range 20) (map #(pocn2F0 a1 a2 z %)) (reduce add))   
  (spec/hypergeometric-pFq-complex [(to-vec2 a1) (to-vec2 a2)] [] (to-vec2 z))
  )

(defn coulomb-F [L eta r]
(mul (CL L eta) (m/pow r (inc L)) (complex-from-polar (- r) 1)
     (hypergeometric-complex-1F1 (complex-from-cartesian (inc L) (- eta)) (* 2 (inc L)) (complex-from-cartesian 0 (* 2 r) )
                                 )
     ))

(defn hypergeometric-complex-U
  [a b z]
  (div 
   (apply complex-from-cartesian  (hypergeometric-complex-2F0 a ( subt  a b -1.)  (div -1. z) ))
    (cpowc  z a))    
  )

(defn Hankel+ [L, eta, rho]
  (let [sigmal (arg (apply complex-from-cartesian (spec/gamma-complex (v/vec2 (inc L) eta))))
        theta (+ rho (* L Math/PI -0.5) sigmal (* eta -1.0 (Math/log (* 2 rho))))
  a (complex-from-cartesian (inc L)  eta)
        ]
    (mul (complex-from-polar theta 1)
            (cpowc (complex-from-cartesian 0 (* -2 rho)) a) 
  ( hypergeometric-complex-U a (* 2 (inc L)) (complex-from-cartesian 0 (* -2.0 rho)))
)))

(defn Hankel- [L, eta, rho]
   (let [sigmal (arg (apply complex-from-cartesian (spec/gamma-complex (v/vec2 (inc L) eta))))
         theta (+ rho (* L Math/PI -0.5) sigmal (* eta -1.0 (Math/log (* 2 rho))))
         a (complex-from-cartesian (inc L) (* -1.0 eta))]
     (mul (complex-from-polar (* -1.0 theta) 1)
          (cpowc (complex-from-cartesian 0 (* 2 rho)) a) 
  ( hypergeometric-complex-U a (* 2 (inc L)) (complex-from-cartesian 0 (* 2.0 rho)))
)))

(defn r-matrix ([^double E V ^long L ]  ;with coulomb ;construct R-matrix * a depending on 1D Coulomb + Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
                                        ;choose u(0) = 0, u'(0) = 1 for initial conditions
                (let [
                      R0 (second V)
                      a0 (last V)
                      a (* 3 (+ R0 a0)) ; a outside of the nuclear range
                      dr 0.00001]
                  (loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr]
                    (if (> x a)
                      (/ ur dudr a); R = ur/ (a dudr) 

                      (recur  (+ x dr) (+ (Coulomb-pot x R0) (WS x V)) (*  (+ (/ (* L (inc L)) (* x x)) (* mass-factor (-  pot E))) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr))) ) 


                    )))
([^double E V a ^long L ]  ;construct R-matrix * a depending on 1D Coulomb + Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
                                        ;choose u(0) = 0, u'(0) = 1 for initial conditions
                (let [
                      R0 (second V)
                      a0 (last V)
                      dr 0.00001]
                  (loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr]
                    (if (> x a)
                      (/ ur dudr a); R = ur/ (a dudr) 

                      (recur  (+ x dr) (+ (Coulomb-pot x R0) (WS x V)) (*  (+ (/ (* L (inc L)) (* x x)) (* mass-factor (-  pot E))) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr))) ) 


                    )))
)

(defn s-matrix ([^double E V ^long L]
                (let [a (* 2  (+ (second V) (last V)))
                      k (m/sqrt (*  mass-factor E))
                      R (r-matrix E V a L)
                      eta (* Z1Z2ee (/ mass-factor k 2))
                      rho (* k a)
                      ]
                  (div (subt2 (Hankel- L eta rho) (mul rho R (deriv Hankel- L eta rho 0.0000001)) )
                       (subt2 (Hankel+ L eta rho) (mul rho R (deriv Hankel+ L eta rho 0.0000001)) ))
                  ))

 ([^double E V ^double a ^long L]
                (let [
                      k (m/sqrt (*  mass-factor E))
                      R (/ (r-matrix-a E V a L) a)
                      eta (* Z1Z2ee mass-factor (/ 1. k 2))
                      rho (* k a)
                      ]
                  (div (subt2 (Hankel- L eta rho) (mul rho R (deriv Hankel- L eta rho 0.0000001)) )
                       (subt2 (Hankel+ L eta rho) (mul rho R (deriv Hankel+ L eta rho 0.0000001)) ))
                  ))
)


(defn phase-shift ( [^double E V  ^long L ]
  (let [s (s-matrix E V L)]
    (/ (arg s) 2)
    ))
 ( [^double E V a  ^long L ]
  (let [s (s-matrix E V a L)]
    (/ (arg s) 2)
    ))
  )



(defn ftheta-L [^double E V  ^long L theta]
                (let [k  (m/sqrt (*  mass-factor E))]
 (mul (div (complex-from-cartesian 0 -1) k) (inc (* 2 L))  (poly/eval-legendre-P L (m/cos theta)) (subt  (s-matrix E V L) 1.)
                )))

(defn hypergeometric-complex-U2
[a b z]
  (mul (div Math/PI (Math/sin (* Math/PI b)))  (subt (div (hypergeometric-complex-1F1 a b z) (mul
                                                                                  (gamma-complex (subt (add a 1) b)) (gamma-complex b))) 
                                         (mul (cpowc z (dec b)) (div (hypergeometric-complex-1F1  (subt (add a 1) b)  (- b 2) z) (mul (gamma-complex  a)  (gamma-complex (subt b 2))) ))))

;  (div   (hypergeometric-complex-1F1 a (- b 2) z)      (mul (gamma-complex (subt b 2)) (gamma-complex a))) 
  )
