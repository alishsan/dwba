(ns dwba.form-factors
  "Transfer form factors and overlap integrals for single nucleon transfer reactions.
   
   Terminology:
   - Form factor F(r) = φ*_f(r) φ_i(r) r² (function of r, the integrand)
   - Overlap integral = ∫₀^∞ F(r) dr = ∫₀^∞ φ*_f(r) φ_i(r) r² dr (scalar value)
   
   These are essential for calculating transfer reaction amplitudes."
  (:require [functions :refer :all]
            [fastmath.core :as m]
            [fastmath.special :as spec]
            [complex :refer :all]))

(defn form-factor-r
  "Calculate form factor at specific radial distance r.
   
   This is the form factor FUNCTION evaluated at r: F(r) = φ*_f(r) · φ_i(r) · r²
   
   Parameters:
   - r: Radial distance (fm)
   - phi-i: Initial bound state wavefunction (vector of values)
   - phi-f: Final bound state wavefunction (vector of values)
   - h: Step size used in wavefunction integration (fm)
   
   Returns: F(r) = φ*_f(r) · φ_i(r) · r²
   
   Note: This is the integrand for the overlap integral.
   r is first for easier partial application when plotting."
  [r phi-i phi-f h]
  (let [idx (int (/ r h))
        idx-safe (min idx (dec (min (count phi-i) (count phi-f))))]
    (if (and (>= idx-safe 0) (< idx-safe (count phi-i)) (< idx-safe (count phi-f)))
      (let [phi-i-val (get phi-i idx-safe)
            phi-f-val (get phi-f idx-safe)
            ;; For real wavefunctions, conjugation is identity; for complex, use complex conjugation
            phi-f-conj (if (number? phi-f-val) 
                        phi-f-val 
                        (complex-cartesian (re phi-f-val) (- (im phi-f-val))))]
        (* phi-f-conj phi-i-val r r))
      0.0)))

(defn overlap-integral
  "Calculate overlap integral between two bound states.
   
   This is the INTEGRAL (scalar value): O = ∫₀^r_max φ*_f(r) φ_i(r) r² dr
   
   Parameters:
   - phi-i: Initial bound state wavefunction (vector of values)
   - phi-f: Final bound state wavefunction (vector of values)
   - r-max: Maximum integration radius (fm)
   - h: Step size used in wavefunction integration (fm)
   
   Returns: O = ∫₀^r_max φ*_f(r) φ_i(r) r² dr
   
   Uses Simpson's rule for numerical integration.
   
   Example:
   (require '[dwba.transfer :as t])
   (let [result-1s (t/solve-bound-state [50.0 2.0 0.6] 1 0 nil 20.0 0.01)
         result-2s (t/solve-bound-state [50.0 2.0 0.6] 2 0 nil 20.0 0.01)
         phi-1s (:normalized-wavefunction result-1s)
         phi-2s (:normalized-wavefunction result-2s)]
     (overlap-integral phi-1s phi-2s 20.0 0.01))"
  [phi-i phi-f r-max h]
  (let [n (min (count phi-i) (count phi-f))
        integrand (mapv (fn [i]
                         (let [r (* i h)]
                           (form-factor-r r phi-i phi-f h)))
                       (range n))
        ;; Simpson's rule integration
        simpson-sum (loop [i 1 sum 0.0]
                     (if (>= i (dec n))
                       sum
                       (let [coeff (if (odd? i) 4.0 2.0)
                             term (* coeff (get integrand i))]
                         (recur (inc i) (+ sum term)))))
        integral (* (/ h 3.0)
                   (+ (first integrand)
                      (last integrand)
                      simpson-sum))]
    integral))

(defn form-factor-function
  "Calculate form factor as a function of radial distance.
   
   This returns the FORM FACTOR FUNCTION F(r) evaluated at all radial points.
   
   Parameters:
   - phi-i: Initial bound state wavefunction (vector)
   - phi-f: Final bound state wavefunction (vector)
   - h: Step size (fm) used in wavefunction integration
   
   Returns: Vector of form factor values F(r) at each radial point.
   
   The length of the returned vector equals min(count phi-i, count phi-f).
   Useful for plotting and understanding the radial dependence."
  [phi-i phi-f h]
  (let [n (min (count phi-i) (count phi-f))]
    (mapv (fn [i]
           (let [r (* i h)]
             (form-factor-r r phi-i phi-f h)))
         (range n))))

(defn normalized-overlap
  "Calculate normalized overlap integral (overlap coefficient).
   
   This is the overlap integral normalized by the individual wavefunction norms.
   
   Parameters:
   - phi-i: Initial bound state wavefunction
   - phi-f: Final bound state wavefunction
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: O_norm = O / (||φ_i|| · ||φ_f||)
   
   This gives the overlap coefficient, which is useful for spectroscopic factors."
  [phi-i phi-f r-max h]
  (let [overlap-val (overlap-integral phi-i phi-f r-max h)
        ;; Calculate norms: ||φ||² = ∫ |φ|² r² dr
        norm-i (Math/sqrt (overlap-integral phi-i phi-i r-max h))
        norm-f (Math/sqrt (overlap-integral phi-f phi-f r-max h))
        norm-product (* norm-i norm-f)]
    (if (> norm-product 0.0)
      (/ overlap-val norm-product)
      0.0)))

(defn momentum-space-overlap
  "Calculate overlap integral in momentum space (Fourier transform).
   
   This is the overlap integral with a momentum-dependent kernel.
   
   Parameters:
   - phi-i: Initial bound state wavefunction
   - phi-f: Final bound state wavefunction
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   - q: Momentum transfer (fm⁻¹)
   
   Returns: O(q) = ∫₀^r_max φ*_f(r) φ_i(r) j₀(qr) r² dr
   
   Where j₀ is the spherical Bessel function of order 0.
   This is useful for momentum-dependent transfer reactions."
  [phi-i phi-f r-max h q]
  (let [n (min (count phi-i) (count phi-f))
        integrand (mapv (fn [i]
                         (let [r (* i h)
                               phi-i-val (get phi-i i)
                               phi-f-val (get phi-f i)
                               j0-val (spec/spherical-bessel-j 0 (* q r))
                               phi-f-conj (if (number? phi-f-val)
                                           phi-f-val
                                           (complex-cartesian (re phi-f-val) (- (im phi-f-val))))]
                           (* phi-f-conj phi-i-val j0-val r r)))
                       (range n))
        simpson-sum (loop [i 1 sum 0.0]
                     (if (>= i (dec n))
                       sum
                       (let [coeff (if (odd? i) 4.0 2.0)
                             term (* coeff (get integrand i))]
                         (recur (inc i) (+ sum term)))))
        integral (* (/ h 3.0)
                   (+ (first integrand)
                      (last integrand)
                      simpson-sum))]
    integral))

