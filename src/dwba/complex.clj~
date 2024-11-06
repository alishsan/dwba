(ns dwia.complex)

(defprotocol ComplexArithmetic "Perform the basic arithmetic of the complex numbers."
  (re  [this] "The real part of the complex number.")
  (im  [this] "The imaginary part of the complex number.")
  (arg [this] "The argument of the complex number, in radians.")
  (mag [this] "The magnitude of the complex number."))

(defrecord complex-number [real imag])

(defn complex-from-cartesian [real imag]
  "Create a complex number by specifying cartesian coordinates."
  (->complex-number real imag))

(defn complex-from-polar [argument magnitude]
  "Create a complex number by specifying polar coordinates."
  (->complex-number (* magnitude (Math/cos argument))
                   (* magnitude (Math/sin argument))))



(extend-type complex-number
  ComplexArithmetic
  (re [this]
    (:real this))
  (im [this]
    (:imag this))
  (arg [this]
    (Math/atan2 (im this) (re this)))
  (mag [this]
    (Math/sqrt (+ (Math/pow (re this) 2)
                  (Math/pow (im this) 2)))))

(defn add2 [x y] "Adds the given complex numbers together."
  (complex-from-cartesian (+ (re x) (re y))
                          (+ (im x) (im y))))
(defn add [x & y] (reduce add2 (cons x y)))

(defn mul [x y] "Multiplies the given complex numbers together."
  (complex-from-polar (+ (arg x) (arg y))
                      (* (mag x) (mag y))))
(extend-type java.lang.Number
  ComplexArithmetic
  (re [this]
    this)
  (im [this]
    0)
  (arg [this]
    0)
  (mag [this]
    this))

(defn complex-conjugate [x] ( complex-from-cartesian (re x )  (* -1 ( im x)) ))

(defn complex-integrate
  "Numerically integrate a complex-valued function f from a to b using n steps.
   Assumes f returns a complex-number instance."
  [f a b n]
  (let [h (/ (- b a) n)                              ;; Step size
        x-values (map #(+ a (* % h)) (range (inc n))) ;; Generate x-values for trapezoidal rule
        f-values (map f x-values)                     ;; Evaluate f at each x-value
        ;; Sum the real and imaginary parts separately for integration
        real-part (apply + (map re f-values))
        imag-part (apply + (map im f-values))
        ;; Apply trapezoidal correction (subtracting half at endpoints)
        corrected-real (- real-part (* 0.5 (+ (re (first f-values)) (re (last f-values)))))
        corrected-imag (- imag-part (* 0.5 (+ (im (first f-values)) (im (last f-values)))))
        ;; Multiply by step size and create a complex result
        ]
    (complex-from-cartesian (* h corrected-real) (* h corrected-imag))))

