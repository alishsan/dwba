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
          

(defn hypergeometric-complex-1F1
  "Kummer's (confluent hypergeometric, 1F1) function for compex arguments."
  [ac ^double b xc]
  (let [a (mag ac) x (mag xc)]
  (cond
    (or (m/== a b -1.0)
        (and (m/neg? b)
             (m/integer? a)
             (m/integer? b)
             (or (m/pos? a)
                 (and (m/neg? a) (m/< a b))))) ##NaN
    (m/near-zero? a (m/ulp a)) 1.0
    (m/zero? b) (m/copy-sign ##Inf (m/* a x))
    (m/near-zero? x m/MACHINE-EPSILON) 1.0
    (m/== a b) (m/exp x)
    (m/== a -1.0) (m/- 1.0 (m// x b))
    (and (m/one? a) (m/== b 2.0)) (let [hx (m/* 0.5 x)]
                                    (m/* (m// (m/exp hx) hx) (m/sinh hx)))
    (m/pos? x) (loop [i (long 1)
                      s0 1.0
                      s1 (m/inc (m// (m/* a x) b))]
                 (if (or (and (m/valid-double? s0) (m/valid-double? s1)
                              (m/delta-eq s0 s1 m/MACHINE-EPSILON m/MACHINE-EPSILON))
                         (m/== i 1000000))
                   s1
                   (let [rj (m// (m/* (m/+ a i) x) (m/* (m/+ b i) (m/inc i)))]
                     (recur (m/inc i) s1 (m/+ s1 (m/* (m/- s1 s0) rj))))))
    :else (hg/weniger-1F1 a b x))))


