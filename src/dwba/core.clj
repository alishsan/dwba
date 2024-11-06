
(ns dwba.core)

(require '[generateme.fastmath.polynomials :as poly])
(use 'complex)



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


