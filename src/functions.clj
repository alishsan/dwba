(ns functions)  ; Useful functions such as LEgendre polynomials

(require '[fastmath.core :as m])

(defn eval-legendre-P
  ^double [^long degree ^double x]
  (case (int degree)
    0 1.0
    1 x
    (loop [i (long 2)
           pprev 1.0
           prev x]
      (if (m/> i degree)
        prev
        (recur (m/inc i) prev
               (m// (m/- (m/* (m/dec (m/* 2.0 i)) x prev)
                         (m/* (m/dec i) pprev)) i))))))

(defn- legendre-P-ratio
  [^long degree]
  (case (int degree)
    0 RONE
    1 (ratio-polynomial [0 1])
    (loop [i (long 2)
           pprev RONE
           prev (ratio-polynomial [0 1])]
      (if (m/> i degree)
        prev
        (recur (m/inc i) prev
               (scale (sub (mult prev (ratio-polynomial [0 (m/dec (m/* 2.0 i))]))
                           (scale pprev (m/dec i))) (/ 1 i)))))))

(defn legendre-P
  [^long degree]
  (polynomial (coeffs (legendre-P-ratio degree))))
