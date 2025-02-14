(ns functions
(:require
[fastmath.core :as m]
[fastmath.polynomials :as poly]
 [fastmath.special.hypergeometric :as hg]
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

(defn CL [eta L] 
(->   (* Math/PI eta -0.5)
(* (Math/pow 2 L))
(mul (gamma-complex  (+ L 1 )))
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


(defn weniger-1F1
  ^double [^double a ^double b ^double x]
  (let [absa (m/abs a)
        zeta (m// x)
        nlo (m// (m/* b zeta) a)
        dlo nlo
        a0 (m/inc a)
        b0 (m/* 2.0 (m/inc b))
        b0zeta (m/* b0 zeta)
        dmid (m/* dlo (m/- b0zeta a0))
        nmid (m/+ dmid b0zeta)
        tmid (m// nmid dmid)]
    (if (m/< (m/abs a0) (m/ulp (m/inc absa)))
      tmid
      (let [nmid (m// nmid a0)
            dmid (m// dmid a0)
            a0 (m/+ a 2.0)
            b0 (m/* 6.0 (m/+ b 2.0))
            b1 (m/* -6.0 b)
            t0 (m/+ (m/* b0 zeta) (m/* 3.0 a))
            t1 (m/+ (m/* b1 zeta) (m/* 4.0 a) 2.0)
            nhi (m/+ (m/* t0 nmid) (m/* t1 nlo) (m/* b1 zeta))
            dhi (m/+ (m/* t0 dmid) (m/* t1 dlo))
            thi (m// nhi dhi)]
        (if (m/< (m/abs a0) (m/ulp (m/+ absa 2.0)))
         thi
          (let [nhi (m// nhi a0)
                dhi (m// dhi a0)]
            (loop [k (long 2)
                   tlo 1.0
                   tmid tmid
                   thi thi
                   nlo nlo
                   nmid nmid
                   nhi nhi
                   dlo dlo
                   dmid dmid
                   dhi dhi]
              (if (or (m/< k 5)
                      (and (m/< k 1048576) (m/> (m/abs (m/- tmid thi))
                                                (m/* m/MACHINE-EPSILON10 (m/max (m/abs tmid)
                                                                                (m/abs thi))))))
                (let [k2 (m/* 2.0 k)
                      a0 (m/+ a k 1.0)
                      a2 (m// (m/* (m/- a k -1.0) (m/inc k2)) (m/dec k2))
                      k42 (m/+ (m/* 4.0 k) 2.0)
                      b0 (m/* k42 (m/inc (m/+ k b)))
                      b1 (m/* k42 (m/dec (m/- k b)))
                      t0 (m/+ (m/* b0 zeta) a2)
                      t1 (m/+ (m/* b1 zeta) a0)
                      nnhi (m/- (m/+ (m/* t0 nhi) (m/* t1 nmid)) (m/* a2 nlo))
                      ndhi (m/- (m/+ (m/* t0 dhi) (m/* t1 dmid)) (m/* a2 dlo))
                      nthi (m// nnhi ndhi)]
                  (if (m/< (m/abs a0) (m/ulp (m/inc (m/+ absa k))))
                    nthi
                    (recur (m/inc k)
                           tmid thi nthi
                           nmid nhi (m// nnhi a0)
                           dmid dhi (m// ndhi a0))))
                (cond
                  (m/valid-double? thi) thi
                  (m/valid-double? tmid) tmid
                  :else tlo)))))))))
          

(defn rising-factorial
  "Rising (Pochhammer) factorial."
  ^double [^double n ^double x]
  (if (m/integer? n)
    (m/rising-factorial-int n x)
    (/ (gamma-complex (+ x n))
       (gamma-complex x))))


(defn pocn
  [ac ^double b z n]
   (div (mul (npow z n) (rising-factorial n ac)) (mul (rising-factorial n b) (m/factorial n)))
)

(defn hypergeometric-complex-1F1
  "Kummer's (confluent hypergeometric, 1F1) function for compex arguments."
  [ac ^double b z]
 (->> (range 30) (map #(pocn ac b z %)) (reduce add))   
)

(defn pocn2F0 ;used for hypergeomtric-2F0
  [a1 a2 z n]
  (div (mul (npow z n) (rising-factorial n a1) (rising-factorial n a2)) (m/factorial n))
)

(defn hypergeometric-complex-2F0
[a1 a2 z]
 (->> (range 30) (map #(pocn2F0 a1 a2 z %)) (reduce add))   
)


