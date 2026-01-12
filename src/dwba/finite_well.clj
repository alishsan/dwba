(ns dwba.finite-well
  "Functions for solving bound states in a finite square well potential.
   
   This namespace provides functions for:
   - Spherical Bessel functions (j-l, k-l) and their derivatives
   - Matching condition calculations
   - Finding bound state energies using Newton-Raphson method
   - Finding all bound states for a given well depth"
  (:require [fastmath.core :as m]))

;; --- Spherical Bessel Function Approximations for low L (finite well) ---

(defn j-l [l x]
  "Spherical Bessel function j_l(x) using recurrence relations.
   More efficient for low l values than general implementations."
  (case l
    -1 (/ (m/cos x) x)
    0  (/ (m/sin x) x)
    1  (- (/ (m/sin x) (* x x)) (/ (m/cos x) x))
    ;; General recurrence: j_{l} = ((2l-1)/x)j_{l-1} - j_{l-2}
    (let [j-m2 (j-l (- l 2) x)
          j-m1 (j-l (- l 1) x)]
      (- (* (/ (dec (* 2 l)) x) j-m1) j-m2))))

(defn k-l [l x]
  "Modified Spherical Bessel function k_l(x) for bound states.
   Used for exponential decay outside the well."
  (case l
    -1 (/ (m/exp (- x)) x)  ;; Related to k_1 via recurrence
    0  (/ (m/exp (- x)) x)
    1  (* (/ (m/exp (- x)) x) (+ 1 (/ 1 x)))
    ;; General recurrence: k_{l+1} = k_{l-1} + (2l+1)/x * k_l
    (let [km2 (k-l (- l 2) x)
          km1 (k-l (- l 1) x)]
      (+ km2 (* (/ (dec (* 2 l)) x) km1)))))

;; --- Finite Well Bound State Functions ---

(defn j-l-deriv [l x]
  "Derivative of spherical Bessel function j_l(x).
   Uses the recurrence relation: j_l'(x) = (l/x) * j_l(x) - j_{l+1}(x)"
  (if (zero? x)
    (case l
      0 0.0
      1 (/ 1.0 3.0)
      Double/NaN)
    (- (* (/ l x) (j-l l x)) (j-l (inc l) x))))

(defn k-l-deriv [l x]
  "Derivative of modified spherical Bessel function k_l(x).
   Uses the recurrence relation: k_l'(x) = -(l/x) * k_l(x) - k_{l+1}(x)"
  (if (zero? x)
    Double/POSITIVE_INFINITY
    (- (* (/ l x) (k-l l x)) (k-l (inc l) x))))

(defn j-ratio [l x]
  "Ratio j_{l-1}(x) / j_l(x) for spherical Bessel functions."
  (if (zero? l)
    ;; For l=0, j_{-1} doesn't exist, use derivative form
    (/ (j-l-deriv 0 x) (j-l 0 x))
    (/ (j-l (dec l) x) (j-l l x))))

(defn k-ratio [l x]
  "Ratio k_{l-1}(x) / k_l(x) for modified spherical Bessel functions."
  (if (zero? l)
    ;; For l=0, k_{-1} doesn't exist, use derivative form
    (/ (k-l-deriv 0 x) (k-l 0 x))
    (/ (k-l (dec l) x) (k-l l x))))

(defn finite-well-matching-error [xi eta l]
  "Returns the mismatch in the log-derivative matching condition at the well boundary.
   
   Parameters:
   - xi: Dimensionless wave number inside well (xi = ka, where k = sqrt(2m(E+V0))/hbar)
   - eta: Dimensionless decay parameter outside well (eta = kappa*a, where kappa = sqrt(2m|E|)/hbar)
   - l: Orbital angular momentum quantum number
   
   Returns: Error in matching condition (should be 0 for bound state).
   
   Uses the recurrence relation form of the matching condition:
   xi * j_{l-1}(xi) / j_l(xi) + eta * k_{l-1}(eta) / k_l(eta) = 0
   
   Or equivalently:
   xi * j_{l-1}(xi) / j_l(xi) = -eta * k_{l-1}(eta) / k_l(eta)
   
   This is equivalent to the log-derivative form but more numerically stable.
   The sign convention: we want left - right = 0, so error = left + right (since right has negative sign)."
  (if (zero? l)
    ;; For l=0, j_{-1} doesn't exist, so use direct derivative form
    ;; At the same physical radius r=a, we need to match:
    ;; k * j_0'(xi)/j_0(xi) = kappa * k_0'(eta)/k_0(eta)
    ;; Since xi = k*a and eta = kappa*a, this becomes:
    ;; xi * j_0'(xi)/j_0(xi) = eta * k_0'(eta)/k_0(eta)
    (let [j0-xi (j-l 0 xi)
          j0-prime-xi (j-l-deriv 0 xi)
          k0-eta (k-l 0 eta)
          k0-prime-eta (k-l-deriv 0 eta)
          log-deriv-inside (* xi (/ j0-prime-xi j0-xi))  ; xi * j_0'(xi)/j_0(xi)
          log-deriv-outside (* eta (/ k0-prime-eta k0-eta))]  ; eta * k_0'(eta)/k_0(eta)
      (- log-deriv-inside log-deriv-outside))
    ;; For l > 0, use recurrence relation form
    (let [left (* xi (j-ratio l xi))
          right (* eta (k-ratio l eta))]
      ;; Matching condition: left + right = 0 (since right should be negative of left)
      (+ left right))))

(defn solver-step [e-ratio l z0]
  "Returns a map of {f(E), f'(E)} for the energy ratio e = |E|/V0.
   
   Parameters:
   - l: Orbital angular momentum quantum number
   - z0: Dimensionless well depth parameter
   - e-ratio: |E|/V0 (dimensionless energy, 0 < e-ratio < 1)
   
   Returns: {:f f-val, :f-prime f-prime}
   - f: Function value (matching error)
   - f-prime: Derivative of function w.r.t. e-ratio
   
   Uses Newton-Raphson method with analytical derivatives.
   
   Note: Returns NaN if e-ratio is outside valid range [0, 1]."
  ;; Check if e-ratio is in valid range (0, 1)
  ;; For e-ratio >= 1: xi = z0*sqrt(1-e) becomes imaginary (NaN)
  ;; For e-ratio <= 0: eta = z0*sqrt(e) becomes imaginary or zero (NaN)
  ;; Return NaN map if outside valid range (for plotting purposes)
  (if (or (<= e-ratio 0) (>= e-ratio 1.0))
    {:f Double/NaN :f-prime Double/NaN}
    (let [eta (* z0 (m/sqrt e-ratio))
          xi (* z0 (m/sqrt (- 1 e-ratio)))
          ;; Use finite-well-matching-error for consistency
          f-val (finite-well-matching-error xi eta l)
          ;; For derivative calculation, we need the intermediate values
          ;; For l=0: j-ratio = j_0'/j_0, for l>0: j-ratio = j_{l-1}/j_l
          left-val (* xi (j-ratio l xi))
          right-val (* eta (k-ratio l eta))
          ;; Raw derivatives w.r.t xi and eta
          ;; For l=0: j-ratio = j_0'/j_0, so left-val = xi * j_0'/j_0
          ;; For l>0: j-ratio = j_{l-1}/j_l, so left-val = xi * j_{l-1}/j_l
          [d-left-dxi d-right-deta]
          (if (zero? l)
            ;; For l=0: left-val = xi * j_0'/j_0, where j_0' = -j_1
            ;; d/dxi [xi * j_0'/j_0] = j_0'/j_0 + xi * d/dxi[j_0'/j_0]
            ;; Using j_0' = -j_1, we have j_0'/j_0 = -j_1/j_0
            ;; d/dxi[-j_1/j_0] = [-(j_1'*j_0 - j_1*j_0')] / j_0^2
            ;; Using j_1' = j_0/x - j_1 and j_0' = -j_1:
            ;; = [-(j_0/x - j_1)*j_0 - j_1*(-j_1)] / j_0^2
            ;; = [-(j_0^2/x - j_1*j_0) + j_1^2] / j_0^2
            ;; = -j_0/(x*j_0) + j_1/j_0 + j_1^2/j_0^2 = -1/x + j_1/j_0 + (j_1/j_0)^2
            (let [j0-xi (j-l 0 xi)
                  j0-prime-xi (j-l-deriv 0 xi)  ; = -j_1
                  j1-xi (j-l 1 xi)
                  j-ratio-val (/ j0-prime-xi j0-xi)  ; = -j_1/j_0
                  j1-over-j0 (/ j1-xi j0-xi)
                  d-j-ratio-dxi (+ (- (/ 1.0 xi)) j1-over-j0 (* j1-over-j0 j1-over-j0))
                  d-left-dxi-val (+ j-ratio-val (* xi d-j-ratio-dxi))
                  
                  ;; For k_0: k_0' = -k_1 (from k-l-deriv)
                  k0-eta (k-l 0 eta)
                  k0-prime-eta (k-l-deriv 0 eta)  ; = -k_1
                  k1-eta (k-l 1 eta)
                  k-ratio-val (/ k0-prime-eta k0-eta)  ; = -k_1/k_0
                  ;; d/deta[k_0'/k_0] = d/deta[-k_1/k_0]
                  ;; k_1' = -k_0/eta - k_1 (from k-l-deriv for l=1)
                  ;; d/deta[-k_1/k_0] = [-(k_1'*k_0 - k_1*k_0')] / k_0^2
                  ;; = [-(-k_0/eta - k_1)*k_0 - k_1*(-k_1)] / k_0^2
                  ;; = [(k_0^2/eta + k_1*k_0) + k_1^2] / k_0^2
                  ;; = k_0/(eta*k_0) + k_1/k_0 + (k_1/k_0)^2 = 1/eta + k_1/k_0 + (k_1/k_0)^2
                  k1-over-k0 (/ k1-eta k0-eta)
                  d-k-ratio-deta (+ (/ 1.0 eta) k1-over-k0 (* k1-over-k0 k1-over-k0))
                  ;; For f = xi * j_0'/j_0 - eta * k_0'/k_0
                  ;; d/deta[f] = -d/deta[eta * k_0'/k_0]
                  ;; d/deta[eta * k_0'/k_0] = k_0'/k_0 + eta * d/deta[k_0'/k_0]
                  ;; So d/deta[f] = -[k_0'/k_0 + eta * d/deta[k_0'/k_0]]
                  ;; For f = xi * j_0'/j_0 - eta * k_0'/k_0
                  ;; d/deta[f] = -d/deta[eta * k_0'/k_0]
                  ;; d/deta[eta * k_0'/k_0] = k_0'/k_0 + eta * d/deta[k_0'/k_0]
                  ;; Since f-prime > 0 everywhere, d/deta[f] > 0, so d/deta[eta*k_0'/k_0] < 0
                  ;; We store d-right-deta as d/deta[eta*k_0'/k_0] (which should be negative)
                  ;; Note: k-ratio-val = k_0'/k_0 is typically negative, making d-right-deta negative
                  d-right-deta-val (+ k-ratio-val (* eta d-k-ratio-deta))]
              [d-left-dxi-val d-right-deta-val])
            ;; For l>0: use recurrence relation
            (let [d-left-dxi-val (+ 1 (* (- (/ (dec (* 2 l)) xi)) left-val) (* left-val left-val))
                  d-right-deta-val (+ 1 (* (/ (dec (* 2 l)) eta) right-val) (* right-val right-val))]
              [d-left-dxi-val d-right-deta-val]))
          ;; Chain rule: d/de-ratio = (d/dxi * dxi/de) + (d/deta * deta/de)
          ;; dxi/de = -z0 / (2 * sqrt(1-e)) [negative: as e increases, xi decreases]
          ;; deta/de = z0 / (2 * sqrt(e)) [positive: as e increases, eta increases]
          dxi-de (/ (- z0) (* 2.0 (m/sqrt (- 1.0 e-ratio))))
          deta-de (/ z0 (* 2.0 (m/sqrt e-ratio)))
          ;; For l=0: f = xi*j_0'/j_0 - eta*k_0'/k_0
          ;; d/dxi[f] = d/dxi[xi*j_0'/j_0] = d-left-dxi
          ;; d/deta[f] = -d/deta[eta*k_0'/k_0] = d-right-deta (which is negative)
          ;; f-prime = d-left-dxi * dxi/de + d-right-deta * deta/de
          ;; Since dxi/de is negative and deta/de is positive:
          ;; f-prime = d-left-dxi * (negative) + (negative) * (positive)
          ;; If f is increasing, we need the second term to dominate positively
          ;; This suggests d-right-deta should actually be positive, not negative!
          ;; Let me check: if f = left - right, then d/deta[f] = -d/deta[right]
          ;; But d-right-deta is calculated as d/deta[right], so we need to negate it
          f-prime (if (zero? l)
                    (- (+ (* d-left-dxi dxi-de) (* (- d-right-deta) deta-de)))  ; Negate to fix sign - f is increasing so f-prime should be positive
                    (+ (* d-left-dxi dxi-de) (* d-right-deta deta-de)))]  ; d-right-deta is correct for l>0
      {:f f-val :f-prime f-prime})))

(defn find-bound-state-finite-well [l z0]
  "Finds bound state energies for a finite square well using Newton-Raphson method.
   
   Parameters:
   - l: Orbital angular momentum quantum number
   - z0: Dimensionless well depth parameter z0 = a * sqrt(2mV0)/hbar
         where a is the well radius and V0 is the well depth
   
   Returns: {:e-ratio, :xi, :eta, :energy, :converged?, :matching-error, :iterations}
   - e-ratio: |E|/V0 (dimensionless energy ratio, 0 < e-ratio < 1)
     NOTE: The actual bound state energy is E = -V0 * e-ratio (negative)
   - xi: ka = z0 * sqrt(1 - e_ratio) (dimensionless wave number inside well)
   - eta: kappa*a = z0 * sqrt(e_ratio) (dimensionless decay parameter outside well)
   - energy: e-ratio (same as e-ratio, for convenience - this is the RATIO, not the physical energy)
   - converged?: Whether Newton-Raphson converged AND matching error is small
   - matching-error: The error in the matching condition (should be ~0 for a true bound state)
   - iterations: Number of iterations used
   
   The relationship between xi and eta is: xi^2 + eta^2 = z0^2
   This comes from: k^2 = 2m(E+V0)/hbar^2 and kappa^2 = 2m|E|/hbar^2
   so: (ka)^2 + (kappa*a)^2 = (2mV0/hbar^2) * a^2 = z0^2
   
   Uses Newton-Raphson method with analytical derivatives for faster convergence.
   NOTE: This finds ONE bound state. For multiple bound states, scan the energy range
   or use find-all-bound-states."
  (let [tolerance 1e-9
        max-iters 50  ; Increased iterations
        ;; For z0 > π/2, bound states exist. Use a better initial guess.
        ;; For shallow wells (z0 < π), bound states are typically in the middle range
        ;; For deep wells, bound states can be anywhere
        ;; Try a few initial guesses and pick the one closest to a root
        initial-guess (if (> z0 Math/PI)
                        0.5  ; Deep well - start in middle
                        ;; For shallow wells, scan for minimum |f| using pipeline
                        (let [valid-results (->> (range 0.1 0.99 0.001)  ; Fine grid like test script (0.001 step)
                                                 (map (fn [e-ratio]
                                                        (let [{:keys [f]} (solver-step e-ratio l z0)]
                                                          (when (not (Double/isNaN f))
                                                            {:e-ratio e-ratio 
                                                             :f f
                                                             :abs-f (m/abs f)}))))
                                                 (filter some?))]
                          (if (seq valid-results)
                            (:e-ratio (apply min-key :abs-f valid-results))
                            0.5)))]  ; Fallback to middle
    ;; Use tail recursion with loop/recur for better stack safety
    (loop [e-ratio initial-guess
           iters 0
           prev-f-val nil
           prev-e-ratio nil]
      ;; Safety check: always terminate if max iterations reached
      (if (>= iters max-iters)
        (let [xi (* z0 (m/sqrt (- 1 e-ratio)))
              eta (* z0 (m/sqrt e-ratio))
              final-error (finite-well-matching-error xi eta l)]
          {:e-ratio e-ratio
           :xi xi
           :eta eta
           :energy e-ratio
           :converged? false
           :matching-error final-error
           :iterations iters})
        (let [{:keys [f f-prime]} (solver-step e-ratio l z0)
              ;; Check if we're making progress
              f-abs (m/abs f)
              ;; Avoid division by zero or very small derivatives
              next-e (if (or (< (m/abs f-prime) 1e-15)
                            (Double/isNaN f-prime)
                            (Double/isInfinite f-prime))
                       ;; If derivative is problematic, use a small step toward zero
                       (let [step-size (min 0.01 (* 0.001 f-abs))]
                         (+ e-ratio (* step-size (- (m/signum f)))))
                       ;; Normal Newton-Raphson step: x_{n+1} = x_n - f(x_n) / f'(x_n)
                       (let [step (/ f f-prime)
                             ;; Limit step size to avoid overshooting, but allow small steps near root
                             ;; For very small function values, allow larger relative steps
                             max-step-size (if (< f-abs 1e-3)
                                           0.1  ; Allow larger steps when close to root
                                           0.5) ; Normal limit
                             limited-step (if (> (m/abs step) max-step-size)
                                           (* max-step-size (m/signum step))
                                           step)]
                         (- e-ratio limited-step)))
              ;; Clamp to valid range, but avoid getting stuck at boundaries
              next-e (max 0.01 (min 0.99 next-e))  ; Wider range to avoid boundary issues
              ;; Check if we hit a boundary and function value is still large
              at-boundary? (or (<= next-e 0.01) (>= next-e 0.99))
              ;; Check convergence: both position and function value
              converged-pos? (< (m/abs (- next-e e-ratio)) tolerance)
              converged-func? (< f-abs 1e-6)
              converged? (and converged-pos? converged-func?)
              ;; Check if we're stuck (not making progress in function value)
              stuck-func? (and prev-f-val (< (m/abs (- f-abs (m/abs prev-f-val))) 1e-12))
              ;; Check if we're oscillating (position not changing significantly)
              stuck-pos? (and prev-e-ratio (< (m/abs (- next-e prev-e-ratio)) 1e-12))
              ;; Check if we're stuck at boundary with large function value
              stuck-at-boundary? (and at-boundary? (> f-abs 0.1))]
          (if (or converged? stuck-func? stuck-pos? stuck-at-boundary?)
            (let [xi (* z0 (m/sqrt (- 1 next-e)))
                  eta (* z0 (m/sqrt next-e))
                  final-error (finite-well-matching-error xi eta l)]
              {:e-ratio next-e
               :xi xi
               :eta eta
               :energy next-e
               :converged? (and converged? (< (m/abs final-error) 1e-6))
               :matching-error final-error
               :iterations (inc iters)})
            (recur next-e (inc iters) f-abs e-ratio)))))))

(defn find-discontinuities [l z0]
  "Finds analytical discontinuities in the matching error function.
   
   For l=0: j_0'/j_0 = cot(xi) - 1/xi, which is discontinuous when sin(xi) = 0,
   i.e., when xi = n*π for n = 1, 2, 3, ...
   
   Since xi = z0 * sqrt(1 - e_ratio), we have:
   z0 * sqrt(1 - e_ratio) = n*π
   1 - e_ratio = (n*π/z0)^2
   e_ratio = 1 - (n*π/z0)^2
   
   For l>0: Discontinuities occur at zeros of j_l(xi), which are more complex.
   For now, we'll use numerical detection for l>0.
   
   Returns: Vector of e-ratio values where discontinuities occur, sorted."
  (if (zero? l)
    ;; For l=0, calculate analytically
    (let [max-n (int (/ z0 Math/PI))  ; Only consider n such that n*π < z0
          discontinuities (for [n (range 1 (inc max-n))
                                :let [n-pi-over-z0 (/ (* n Math/PI) z0)
                                      e-ratio (- 1.0 (* n-pi-over-z0 n-pi-over-z0))]]  ; e_ratio = 1 - (n*π/z0)^2
                            (when (and (> e-ratio 0.01) (< e-ratio 0.99))
                              e-ratio))]
      (->> discontinuities
           (filter some?)
           (sort)
           (vec)))
    ;; For l>0, return empty (use numerical detection)
    []))

(defn scan-energy-range-dimensionless [l z0 step-size]
  "Scans the energy range and returns function values.
   
   Returns: Vector of {:e-ratio, :f} for each valid point."
  (let [test-points (range 0.01 0.99 step-size)]
    (->> test-points
         (map (fn [e-ratio]
                (let [{:keys [f]} (solver-step e-ratio l z0)]
                  (when (not (Double/isNaN f))
                    {:e-ratio e-ratio :f f}))))
         (filter some?)
         (vec))))

(defn find-numerical-discontinuities [results]
  "Finds numerical discontinuities (large jumps) in the function values.
   
   Returns: Vector of e-ratio values where discontinuities occur."
  (->> (partition 2 1 results)
       (keep (fn [[curr next]]
               (when (and curr next)
                 (let [jump-size (m/abs (- (:f next) (:f curr)))]
                   (when (> jump-size 1000.0)
                     (/ (+ (:e-ratio curr) (:e-ratio next)) 2.0))))))
       (vec)))

(defn find-sign-change-candidates [results all-disc-e-ratios]
  "Finds sign change candidates, excluding those near discontinuities.
   
   Returns: Vector of {:e1, :e2, :f1, :f2} for each sign change."
  (->> (partition 2 1 results)
       (keep (fn [[curr next]]
               (when (and curr next)
                 (let [f1 (:f curr)
                       f2 (:f next)
                       s1 (m/signum f1)
                       s2 (m/signum f2)
                       e1 (:e-ratio curr)
                       e2 (:e-ratio next)
                       e-mid (/ (+ e1 e2) 2.0)
                       near-disc? (some (fn [disc-e]
                                          (< (m/abs (- e-mid disc-e)) 0.01))
                                        all-disc-e-ratios)
                       is-sign-change? (and (not (zero? s1))
                                            (not (zero? s2))
                                            (not= s1 s2)
                                            (not near-disc?))]
                   (when is-sign-change?
                     {:e1 e1 :e2 e2 :f1 f1 :f2 f2})))))
       (vec)))

(defn find-local-minima-candidates [results]
  "Finds local minima in |f| as potential roots.
   
   Returns: Vector of {:e1, :e2, :f1, :f2} for each local minimum."
  (->> (partition 3 1 results)
       (keep (fn [[prev curr next]]
               (when (and prev curr next)
                 (let [f-prev (m/abs (:f prev))
                       f-curr (m/abs (:f curr))
                       f-next (m/abs (:f next))
                       is-minimum? (and (< f-curr f-prev)
                                       (< f-curr f-next)
                                       (< f-curr 10.0))]
                   (when is-minimum?
                     {:e1 (max (:e-ratio prev) 0.01)
                      :e2 (min (:e-ratio next) 0.99)
                      :f1 (:f curr)
                      :f2 (:f curr)})))))
       (vec)))

(defn refine-root [candidate l z0]
  "Refines a root candidate using Newton-Raphson method.
   
   Parameters:
   - candidate: {:e1, :e2, :f1, :f2} - bracket for the root
   - l: Orbital angular momentum quantum number
   - z0: Dimensionless well depth parameter
   
   Returns: Bound state map with {:e-ratio, :xi, :eta, :energy, :converged?, :matching-error, :iterations}"
  (let [{:keys [e1 e2]} candidate
        initial-guess (/ (+ e1 e2) 2.0)
        tolerance 1e-9
        max-iters 50
        bracket-low e1
        bracket-high e2]
    (loop [e-ratio initial-guess
           iters 0
           prev-f-val nil
           prev-e-ratio nil
           low bracket-low
           high bracket-high]
      (if (>= iters max-iters)
        (let [xi (* z0 (m/sqrt (- 1 e-ratio)))
              eta (* z0 (m/sqrt e-ratio))
              final-error (finite-well-matching-error xi eta l)]
          {:e-ratio e-ratio
           :xi xi
           :eta eta
           :energy e-ratio
           :converged? false
           :matching-error final-error
           :iterations iters})
        (let [{:keys [f f-prime]} (solver-step e-ratio l z0)
              f-valid? (and (not (Double/isNaN f))
                           (not (Double/isInfinite f))
                           (not (Double/isNaN f-prime))
                           (not (Double/isInfinite f-prime)))
              f-abs (if f-valid? (m/abs f) Double/MAX_VALUE)
              next-e (if (or (not f-valid?)
                            (< (m/abs f-prime) 1e-15)
                            (> f-abs 1e6))
                      (let [step-size (* 0.5 (m/abs (- high low)))]
                        (if (> f 0)
                          (- e-ratio step-size)
                          (+ e-ratio step-size)))
                      (let [step (/ f f-prime)
                            max-step-size (if (< f-abs 1e-3) 0.1 0.5)
                            limited-step (if (> (m/abs step) max-step-size)
                                          (* max-step-size (m/signum step))
                                          step)]
                        (- e-ratio limited-step)))
              next-e (max (max 0.01 low) (min 0.99 (min high next-e)))
              [new-low new-high] (if f-valid?
                                  (if (> f 0)
                                    [low e-ratio]
                                    [e-ratio high])
                                  [low high])
              converged-pos? (< (m/abs (- next-e e-ratio)) tolerance)
              converged-func? (< f-abs 1e-6)
              converged? (and converged-pos? converged-func?)
              stuck-func? (and (>= iters 3)
                              prev-f-val
                              (< (m/abs (- f-abs (m/abs prev-f-val))) 1e-12))
              stuck-pos? (and (>= iters 3)
                            prev-e-ratio
                            (< (m/abs (- next-e prev-e-ratio)) 1e-12))
              at-boundary? (or (<= next-e 0.01) (>= next-e 0.99))
              stuck-at-boundary? (and at-boundary? (> f-abs 0.1))]
          (if (or converged? stuck-func? stuck-pos? stuck-at-boundary?)
            (let [xi (* z0 (m/sqrt (- 1 next-e)))
                  eta (* z0 (m/sqrt e-ratio))
                  final-error (finite-well-matching-error xi eta l)]
              {:e-ratio next-e
               :xi xi
               :eta eta
               :energy next-e
               :converged? (and converged? (< (m/abs final-error) 1e-6))
               :matching-error final-error
               :iterations (inc iters)})
            (recur next-e (inc iters) f-abs e-ratio new-low new-high)))))))

(defn deduplicate-states [states min-separation]
  "Removes duplicate states that are too close to each other.
   
   Parameters:
   - states: Vector of bound state maps, sorted by e-ratio
   - min-separation: Minimum e-ratio difference to consider states distinct
   
   Returns: Deduplicated vector of states."
  (loop [remaining states
         result []]
    (if (empty? remaining)
      result
      (let [current (first remaining)
            rest-states (rest remaining)
            too-close? (some (fn [prev-state]
                              (< (m/abs (- (:e-ratio current) (:e-ratio prev-state))) min-separation))
                            result)]
        (if too-close?
          (recur rest-states result)
          (recur rest-states (conj result current)))))))

(defn find-all-bound-states [l z0]
  "Finds all bound states for a given l and z0.
   
   Parameters:
   - l: Orbital angular momentum quantum number
   - z0: Dimensionless well depth parameter
   
   Returns: Vector of bound states, each with {:e-ratio, :xi, :eta, :energy, :converged?, :matching-error, :iterations}
   sorted by e-ratio (lowest energy first).
   
   Uses a fine grid scan to find sign changes, then refines each root using Newton-Raphson."
  (let [;; Find analytical discontinuities first
        analytical-discs (find-discontinuities l z0)
        ;; Scan energy range
        results (scan-energy-range-dimensionless l z0 0.001)
        ;; Find numerical discontinuities
        numerical-discs (find-numerical-discontinuities results)
        ;; Combine all discontinuities
        all-disc-e-ratios (sort (concat analytical-discs numerical-discs))
        ;; Find sign change candidates
        sign-changes (find-sign-change-candidates results all-disc-e-ratios)
        ;; Find local minima candidates
        local-minima (find-local-minima-candidates results)
        ;; Combine all candidates
        all-candidates (concat sign-changes local-minima)
        ;; Refine each candidate
        bound-states (mapv #(refine-root % l z0) all-candidates)
        ;; Sort by e-ratio
        sorted-states (sort-by :e-ratio bound-states)
        ;; Deduplicate
        deduplicated (deduplicate-states sorted-states 0.001)]
    deduplicated))

