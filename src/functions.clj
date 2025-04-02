
(ns functions
(:require
[fastmath.core :as m]
[fastmath.polynomials :as poly]
 [fastmath.special.hypergeometric :as hg]
 [fastmath.special :as spec]
   [fastmath.vector :as v]
))
(use 'complex)


(defn deriv
  ([fn1 x dx]
   (/ (- (fn1 (+ x dx)) (fn1 x)) dx))
  ([fn1 L x dx] ;for hankel functions with L dependence and complex numbers
   (div (subt2 (fn1 L (+ x dx)) (fn1 L x)) dx)))

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


(defn r-matrix-a ;h-bar and speed of light c are set to 1
  [^double E V  ^double a ^long L]  ;no coulomb ;construct R-matrix * a depending on 1D Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [N  1000
dr (/ a N)]
(loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr]
(if (> x a)
(/ ur dudr) ;Ra = u/dudr  (dudr = d2udr2 * dr)
(recur  (+ x dr) (WS x V) (*  (+ (/ (* L (inc L)) (* x x)) (-  pot E)) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr))) ) 
)))

   (defn r-matrix [^double E V ^long L ^double eta]  ;with coulomb ;construct R-matrix * a depending on 1D Coulomb + Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
     (let [N  1000
           a (* 2 (second V))
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

(defn hypergeometric-complex-U2
[a b z]
  (mul (div Math/PI (Math/sin (* Math/PI b)))  (subt (div (hypergeometric-complex-1F1 a b z) (mul
                                                                                  (gamma-complex (subt (add a 1) b)) (gamma-complex b))) 
                                         (mul (cpowc z (dec b)) (div (hypergeometric-complex-1F1  (subt (add a 1) b)  (- b 2) z) (mul (gamma-complex  a)  (gamma-complex (subt b 2))) ))))

;  (div   (hypergeometric-complex-1F1 a (- b 2) z)      (mul (gamma-complex (subt b 2)) (gamma-complex a))) 
  )
