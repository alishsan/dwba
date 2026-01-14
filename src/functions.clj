(ns functions
(:require
[fastmath.core :as m]
[fastmath.polynomials :as poly]
 [fastmath.special.hypergeometric :as hg]
 [fastmath.special :as spec]
   [fastmath.vector :as v]
   [dwba.finite-well :as fw :refer [j-l k-l j-l-deriv k-l-deriv j-ratio k-ratio
                                     finite-well-matching-error solver-step
                                     find-bound-state-finite-well find-discontinuities
                                     find-all-bound-states]]
))
(use 'complex)

(declare f-func f-func-deriv g-func g-func-deriv hankel0+ hankel0- phase-shift0)

(def hbarc 197.7) ;MeV-fm
;(def mu 745) ;MeV/c^2; alpha+n
;(def mu 869.4) ; 14C+n
;(def mu 884.3); 16O + n
(def mu 469.46); p+n

(def mass-factor (/ (* 2 mu) hbarc hbarc )); mass-factor * E = k^2


(def Z1Z2ee (* 2 1.44) ; alpha and proton
  );Z1Z2 e^2 (MeV fm)

;; Kinematic transformation functions
(defn lab-to-cm-energy [E-lab m1 m2]
  "Convert laboratory energy to center-of-mass energy"
  (* E-lab (/ m2 (+ m1 m2))))

(defn lab-to-cm-angle [theta-lab m1 m2]
  "Convert laboratory angle to center-of-mass angle - User's cteformula"
  (let [ratio (/ m1 (+ m1 m2))
        tan-theta-cm (/ (Math/sin theta-lab) (- (Math/cos theta-lab) ratio))
        sign-tan (m/signum tan-theta-cm)
        cos-theta-cm (Math/sqrt (/ 1.0 (+ 1.0 (* tan-theta-cm tan-theta-cm))))]
    (Math/acos (* sign-tan cos-theta-cm))))

(defn cm-to-lab-angle [theta-cm m1 m2]
  "Convert center-of-mass angle to laboratory angle - inverse of lab-to-cm-angle"
  (let [ratio (/ m1 (+ m1 m2))  ; Same ratio as lab-to-cm
        sincm (Math/sin theta-cm)
        coscm (Math/cos theta-cm);
        sign-cos (m/signum coscm)
        cos-theta-lab (+ (* ratio sincm sincm) (* sign-cos (Math/sqrt (+ (* coscm coscm) (* (Math/pow sincm 4) ratio ratio) (* -1 ratio ratio sincm sincm) ))))]
    (Math/acos cos-theta-lab)))

(defn jacobian-lab-to-cm [theta-lab m1 m2]
  "Calculate Jacobian for lab-to-CM transformation"
  (let [ratio (/ m1 (+ m1 m2))
        cos-theta-lab (Math/cos theta-lab)
        sin-theta-lab (Math/sin theta-lab)
        denominator (+ (* cos-theta-lab cos-theta-lab) 
                       (* ratio ratio) 
                       (- (* 2 ratio cos-theta-lab)))]
    (/ (* ratio ratio) denominator)))


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

(defn WS-complex-full [r [V0 R0 a0 W0]]
  "Complex Woods-Saxon potential: V(r) = -(V0 + iW0)/(1+exp((r-R0)/a0))"
  (let [real-part (/ (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0))))
        imag-part (/ (* -1.0 W0) (+ 1.0 (Math/exp (/ (- r R0) a0))))]
    (complex-from-cartesian real-part imag-part)))

;; --- Numerov Integration Functions ---

(defn woods-saxon-numerov [r v0 rad diff]
  "Woods-Saxon potential for Numerov method: V(r) = -V0/(1+exp((r-R0)/a0))"
  (/ (- v0) (+ 1.0 (Math/exp (/ (- r rad) diff)))))

(defn f-r-numerov ([r e l v0 rad diff mass-factor];mass-factor is different from the name-space mass factor
  "Effective potential function for Numerov integration"
  (if (zero? r)
    ;; At r=0, centrifugal term dominates: l(l+1)/r^2 -> infinity
    ;; But we never actually use r=0 in Numerov (starts at r=h)
    Double/POSITIVE_INFINITY
    (let [v-eff (+ (woods-saxon-numerov r v0 rad diff)
                   (/ (* l (inc l)) (* mass-factor r r)))]
      (* mass-factor (- v-eff e)))))

 ([r e l v0 rad diff] ; use the name-space mass-factor
  "Effective potential function for Numerov integration"
  (if (zero? r)
    ;; At r=0, centrifugal term dominates: l(l+1)/r^2 -> infinity
    ;; But we never actually use r=0 in Numerov (starts at r=h)
    Double/POSITIVE_INFINITY
    (let [v-eff (+ (woods-saxon-numerov r v0 rad diff)
                   (/ (* l (inc l)) (* mass-factor r r)))]
      (* mass-factor (- v-eff e)))))
  )


(defn plot-function [f start end step & y];;"plots" function f vs. the first variable
(mapv (fn [x] [x (apply f x y)] ) (range start end step ))
  )

(defn bessel-start-l1 [r q]
  "Power series expansion for Riccati-Bessel function F1 near r=0 for l=1"
  ;; Use power series to avoid numerical underflow near origin
  ;; F1(qr) = sin(qr)/(qr) - cos(qr) ≈ (qr)²/3 - (qr)⁴/30 + ...
  (let [z (* q r)]
    ;; Power series: F1(z) ≈ z²/3 - z⁴/30 (accurate for small z, avoids underflow)
    (- (/ (* z z) 3.0) 
       (/ (* z z z z) 30.0))))

(defn solve-numerov [e l v0 rad diff h r-max]
  "Solve the radial Schrödinger equation using the Numerov method"
  (let [steps (int (/ r-max h))
        q (Math/sqrt (* mass-factor (+ e v0)))
        ;; Initialize with Bessel Start
        u0 0.0
        u1 (bessel-start-l1 h q)
        
        ;; Pre-calculate f(r) values
        ;; fs[i] corresponds to f(i*h)
        ;; f(0) is infinite for l>0, but u(0)=0, so f(0)*u(0) = 0
        ;; We set f(0)=0 to avoid NaN from infinity*0
        fs (mapv (fn [r] 
                   (if (zero? r)
                     0.0  ; f(0) is infinite, but u(0)=0, so f(0)*u(0)=0 anyway
                     (f-r-numerov r e l v0 rad diff)))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    
    (loop [n 1
           results [u0 u1]]
      (if (>= n (dec steps))
        results
        (let [un (get results n)        ; u at r = n*h
              un-1 (get results (dec n)) ; u at r = (n-1)*h = 0 when n=1
              ;; Numerov formula uses: f_{n-1}, f_n, f_{n+1}
              ;; where f_n = f(n*h)
              ;; When n=1: f[0]*u[0] = f(0)*0 = 0, so fs[0] value doesn't matter
              fn-1 (get fs (dec n))  ; f at r = (n-1)*h
              fn (get fs n)          ; f at r = n*h
              fn+1 (get fs (inc n))  ; f at r = (n+1)*h
              
              ;; Numerov Step:
              ;; un+1 (1 - h^2/12 fn+1) = 2un - un-1 + h^2/12 (10fn un + fn-1 un-1)
              numerator (+ (* 2.0 un) 
                           (- un-1) 
                           (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
              denominator (- 1.0 (* h2-12 fn+1))
              un+1 (/ numerator denominator)]
          (recur (inc n) (conj results un+1)))))))

(defn check-wronskian [u e l v0 rad diff h]
  "Check Wronskian conservation for Numerov integration"
  ;; The paper's discrete Wronskian formula is: W_n = (1 - h²/12 f_{n+1}) u_{n+1} v_n - (1 - h²/12 f_n) u_n v_{n+1}
  ;; This is for two independent solutions u and v.
  ;;
  ;; For a single solution, we can't compute a true Wronskian (W(u,u) = 0).
  ;; However, the paper mentions checking ΔW = W_1 - W_0, which suggests they're checking
  ;; the conservation of a quantity related to the Numerov step structure.
  ;;
  ;; The Numerov algorithm preserves a symplectic structure. For a single solution,
  ;; we can check the conservation of a quantity that measures the "phase-space volume"
  ;; or the deviation from perfect conservation.
  ;;
  ;; A practical approach: Check if the quantity (1 - h²/12 f_n) u_n remains consistent,
  ;; or check the conservation of a related discrete invariant.
  ;;
  ;; Actually, let's check the paper's formula with v = u, but understand that this
  ;; measures something related to the step structure, not a true Wronskian:
  ;; W_n = (1 - h²/12 f_{n+1}) u_{n+1} u_n - (1 - h²/12 f_n) u_n u_{n+1}
  ;;     = (h²/12)(f_n - f_{n+1}) u_n u_{n+1}
  ;;
  ;; This quantity should be approximately constant (conserved) for the Numerov algorithm.
  ;; We check its drift from the first value.
  (let [h2-12 (/ (* h h) 12.0)
        ;; fs[i] corresponds to f(i*h)
        num-points (count u)
        fs (mapv #(f-r-numerov % e l v0 rad diff) 
                 (take num-points (iterate #(+ % h) 0.0)))]
    ;; Start from n=1 to avoid n=0 where u_0 = 0
    (for [n (range 1 (dec (count u)))]
      (let [un (get u n)
            un+1 (get u (inc n))
            fn (get fs n)
            fn+1 (get fs (inc n))
            ;; Discrete Wronskian-like quantity: (h²/12)(f_n - f_{n+1}) u_n u_{n+1}
            ;; This should be conserved (approximately constant)
            w-n (* h2-12 (- fn fn+1) un un+1)]
        w-n))))

(defn calculate-error [u-true u-test]
  "Calculate absolute errors between true and test wavefunction solutions"
  (mapv (fn [t tst] (Math/abs (- t tst))) u-true u-test))

(defn numerov-convergence-test [e l v0 rad diff h-fine h-test r-max]
  "Test Numerov convergence by comparing fine-grid solution with test solution"
  (let [u-true (solve-numerov e l v0 rad diff h-fine r-max)
        u-test (solve-numerov e l v0 rad diff h-test r-max)
        ;; Downsample fine solution to match test grid
        downsample-factor (int (/ h-test h-fine))
        u-true-downsampled (take-nth downsample-factor u-true)
        ;; Ensure same length (may differ by 1 due to rounding)
        min-len (min (count u-true-downsampled) (count u-test))
        errors (calculate-error (take min-len u-true-downsampled) 
                                (take min-len u-test))]
    {:max-error (apply max errors)
     :mean-error (/ (reduce + errors) (count errors))
     :errors errors
     :u-true-count (count u-true)
     :u-test-count (count u-test)
     :downsampled-count min-len}))

(defn naive-start-l1 [h]
  "Naive power series start: u(r) ≈ r^(l+1) for l=1"
  (Math/pow h 2))

(defn solve-numerov-naive [e l v0 rad diff h r-max]
  "Solve Numerov with naive r^(l+1) power series start"
  (let [steps (int (/ r-max h))
        ;; Initialize with naive start
        u0 0.0
        u1 (Math/pow h (inc l))
        
        ;; Pre-calculate f(r) values
        ;; fs[i] corresponds to f(i*h)
        ;; f(0) is infinite for l>0, but u(0)=0, so f(0)*u(0) = 0
        ;; We set f(0)=0 to avoid NaN from infinity*0
        fs (mapv (fn [r] 
                   (if (zero? r)
                     0.0  ; f(0) is infinite, but u(0)=0, so f(0)*u(0)=0 anyway
                     (f-r-numerov r e l v0 rad diff)))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    
    (loop [n 1
           results [u0 u1]]
      (if (>= n (dec steps))
        results
        (let [un (get results n)        ; u at r = n*h
              un-1 (get results (dec n)) ; u at r = (n-1)*h
              ;; Numerov formula uses: f_{n-1}, f_n, f_{n+1}
              ;; where f_n = f(n*h)
              ;; When n=1: f[0]*u[0] = f(0)*0 = 0, so fs[0] value doesn't matter
              fn-1 (get fs (dec n))  ; f at r = (n-1)*h
              fn (get fs n)          ; f at r = n*h
              fn+1 (get fs (inc n))  ; f at r = (n+1)*h
              
              ;; Numerov Step:
              ;; un+1 (1 - h^2/12 fn+1) = 2un - un-1 + h^2/12 (10fn un + fn-1 un-1)
              numerator (+ (* 2.0 un) 
                           (- un-1) 
                           (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
              denominator (- 1.0 (* h2-12 fn+1))
              un+1 (/ numerator denominator)]
          (recur (inc n) (conj results un+1)))))))

(defn calculate-stability-data [e l v0 rad diff h r-max]
  "Calculate Wronskian stability comparison between Bessel start and naive start"
  (let [;; Bessel Start Integration
        u-bessel (solve-numerov e l v0 rad diff h r-max)
        
        ;; Naive Power Start Integration (u1 = h^(l+1))
        u-naive (solve-numerov-naive e l v0 rad diff h r-max)
        
        ;; Discrete Wronskians
        w-bessel (check-wronskian u-bessel e l v0 rad diff h)
        w-naive (check-wronskian u-naive e l v0 rad diff h)]
    
    {:bessel-w-drift (if (seq w-bessel)
                       (apply max (map #(Math/abs (- % (first w-bessel))) w-bessel))
                       0.0)
     :naive-w-drift (if (seq w-naive)
                      (apply max (map #(Math/abs (- % (first w-naive))) w-naive))
                      0.0)
     :bessel-wronskian w-bessel
     :naive-wronskian w-naive
     :bessel-w-initial (first w-bessel)
     :naive-w-initial (first w-naive)}))

;; --- Phase Shift Extraction from Numerov Solution ---

(defn extract-wavefunction-at-boundary [u h r-boundary]
  "Extract wavefunction value and derivative at boundary from Numerov solution"
  (let [idx (int (/ r-boundary h))
        idx (min idx (- (count u) 2))  ; Ensure we can calculate derivative
        ;; Use the actual r value at this index, not r-boundary
        r-actual (* idx h)
        u-a (get u idx)
        ;; Central difference for derivative (more accurate)
        u-prime-a (if (and (> idx 0) (< idx (dec (count u))))
                    (/ (- (get u (inc idx)) (get u (dec idx))) (* 2 h))
                    ;; Forward/backward difference at boundaries
                    (if (zero? idx)
                      (/ (- (get u (inc idx)) u-a) h)
                      (/ (- u-a (get u (dec idx))) h)))]
    {:u u-a
     :u-prime u-prime-a
     :r r-actual  ; Use actual r value, not requested boundary
     :index idx}))

(defn r-matrix-from-numerov [u h r-boundary]
  "Calculate R-matrix from Numerov wavefunction solution"
  (let [{:keys [u u-prime r]} (extract-wavefunction-at-boundary u h r-boundary)]
    ;; R = u(a) / (a * u'(a))
    ;; Use actual r value from extraction, not requested boundary
    (if (or (zero? u-prime) (Double/isNaN u-prime) (Double/isInfinite u-prime))
      Double/NaN
      (/ u (* r u-prime)))))

(defn interpolate [wave-function r r-max]
  "Interpolate wave function value at radius r from discrete wave-function values.
   
   Parameters:
   - wave-function: Vector/list of wave-function values at discrete points
   - r: Radius at which to interpolate (must be between 0 and r-max)
   - r-max: Maximum radius corresponding to the last wave-function value
   
   Returns: Interpolated wave-function value at radius r
   
   Uses linear interpolation between the two nearest grid points.
   Assumes the first value corresponds to r=0 and the last to r=r-max."
  (let [n-points (count wave-function)]
    (cond
      ;; Edge case: empty or single point
      (<= n-points 1)
      (if (empty? wave-function)
        0.0
        (first wave-function))
      ;; If r is exactly at a grid point or beyond r-max
      (<= r 0.0)
      (first wave-function)
      (>= r r-max)
      (last wave-function)
      ;; Linear interpolation between two nearest points
      :else
      (let [;; Calculate step size: h = r-max / (n-points - 1)
            ;; since we have n-points values from r=0 to r=r-max
            h (/ r-max (dec n-points))
            ;; Find the index where r falls: idx = r / h
            idx (/ r h)
            idx-floor (int (Math/floor idx))
            idx-ceil (min (inc idx-floor) (dec n-points))
            ;; Handle edge cases
            idx-floor (max 0 (min idx-floor (dec n-points)))
            r-floor (* idx-floor h)
            r-ceil (* idx-ceil h)
            u-floor (get wave-function idx-floor)
            u-ceil (get wave-function idx-ceil)
            ;; Linear interpolation: u(r) = u_floor + (u_ceil - u_floor) * (r - r_floor) / (r_ceil - r_floor)
            weight (if (zero? (- r-ceil r-floor))
                     0.0
                     (/ (- r r-floor) (- r-ceil r-floor)))]
        (+ u-floor (* weight (- u-ceil u-floor)))))))

(defn phase-shift-from-numerov [u h r-boundary e l]
  "Extract phase shift from Numerov wavefunction solution using S-matrix method (same as phase-shift0)"
  (let [{:keys [u u-prime r]} (extract-wavefunction-at-boundary u h r-boundary)
        ;; Calculate Ra = u/u' (R-matrix times a, same as r-matrix-a returns)
        ;; r-matrix-a returns: Ra = u/dudr where dudr = u'
        Ra (/ u u-prime)
        k (m/sqrt (* mass-factor e))
        rho (* k r)
        ;; Use S-matrix method (same as s-matrix0 and phase-shift0)
        ;; S = (H- - Ra*k*H-') / (H+ - Ra*k*H+')
        ;; where H+ and H- are outgoing/incoming Hankel functions
        hankel-minus (hankel0- l rho)
        hankel-plus (hankel0+ l rho)
        hankel-minus-prime (deriv hankel0- l rho 0.000001)
        hankel-plus-prime (deriv hankel0+ l rho 0.000001)
        numerator (subt2 hankel-minus (mul Ra k hankel-minus-prime))
        denominator (subt2 hankel-plus (mul Ra k hankel-plus-prime))
        s-matrix (div numerator denominator)]
    (if (or (Double/isNaN Ra) (Double/isInfinite Ra)
            (Double/isNaN (arg s-matrix)))
      Double/NaN
      (/ (arg s-matrix) 2))))

(defn exact-phase-shift-numerov [e l v0 rad diff r-boundary]
  "Calculate 'exact' phase shift using very fine Numerov integration with Bessel start"
  ;; Use very fine grid with Bessel start as reference (matches paper methodology)
  ;; Paper uses h=0.001 or finer as "exact" reference for convergence comparison
  ;; This measures how quickly coarse-grid solutions converge to the fine-grid solution
  (let [h-fine 0.0001  ; Very fine grid for reference (finer than paper's 0.001 for better accuracy)
        u (solve-numerov e l v0 rad diff h-fine r-boundary)]
    (phase-shift-from-numerov u h-fine r-boundary e l)))

(defn phase-shift-convergence-table [e l v0 rad diff h-values r-boundary]
  "Generate phase shift convergence table matching paper format"
  ;; Use R-matrix method as exact reference (independent of Numerov initialization)
  (let [V [v0 rad diff]
        delta-exact (phase-shift0 e V r-boundary l)]
    (mapv (fn [h]
            (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
                  u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
                  delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
                  delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)]
              {:h h
               :naive-error (Math/abs (- delta-naive delta-exact))
               :bessel-error (Math/abs (- delta-bessel delta-exact))
               :naive-phase-shift delta-naive
               :bessel-phase-shift delta-bessel
               :exact-phase-shift delta-exact}))
          h-values)))

(defn print-convergence-table [table]
  "Print phase shift convergence table in paper format"
  (println "Phase Shift Error Convergence |δ_calc - δ_exact|")
  (println (apply str (repeat 60 "-")))
  (println (format "%-8s %-20s %-20s" "h (fm)" "Naive Start Error" "Bessel-Start Error"))
  (println (apply str (repeat 60 "-")))
  (doseq [row table]
    (println (format "%-8.2f %-20.6e %-20.6e" 
                    (:h row) (:naive-error row) (:bessel-error row))))
  (println ""))

(defn xi ;h-bar and speed of light c are set to 1
  [^double E V  ^double a ^long L]  ;no coulomb ;construct R-matrix * a depending on 1D Woods-Saxon potential V(R) = -V0/(1+exp ((r-R0)/a0)) V = [V0, R0, a0]
;choose u(0) = 0, u'(0) = 1 for initial conditions
(let [
dr 0.0000100]
  (loop [x dr pot 0 d2udr2 -0.1 
dudr 1
 ur (* dudr dr) accum []]
(if (> x a)
  (take-nth 1000 accum)
(recur  (+ x dr) (WS x V) (*  (+ (/ (* L (inc L)) (* x x)) (* mass-factor (-  pot E))) ur) (+ dudr (* d2udr2 dr))  (+ ur (* dudr dr)) (into accum [[x ur ]])) ) 
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

(defn r-matrix-step 
  [^double E ^double V ^double a ^long L]  ;no coulomb ;construct R-matrix 
  (let [  k (m/sqrt (*  mass-factor (- E V)))
rho (* k a) ]
    (/
     (f-func L rho)
     (f-func-deriv L rho)
     rho
)
)
)

(defn g-matrix-step ;ratio of G to G' , divided by a, same as G/rho G dot
  [^double E  ^double a ^long L]  ;no coulomb ;construct R-matrix 
  (let [  k (m/sqrt (*  mass-factor  E ))
rho (* k a) ]
    (/
     (g-func L rho)
     (g-func-deriv L rho)
     rho
)
)
)


(defn Coulomb-pot ([ r r0] ; potential of a uniformly charged sphere of charge e and radius r0
                   (if (> r r0) (/  Z1Z2ee r) (* r (/ Z1Z2ee r0 r0))))
   ([Ze r r0] ; potential of a uniformly charged sphere of charge e and radius r0
  (if (> r r0) (/  Ze r) (* r (/ Ze r0 r0))))
  ) 
  


(defn f-func [L rho] (* rho (spec/spherical-bessel-j L rho)))
(defn g-func [L rho] (* -1 rho (spec/spherical-bessel-y L rho)))

;; --- Finite Well Functions ---
;; These functions have been moved to dwba.finite-well namespace.
;; They are re-exported here for backward compatibility.

(defn bisection

  ([f [low high]]
(bisection f [low high] 1.e20 100)
   )
  ([f [low high] tolerance max-iters]
  "Generic bisection root-finding algorithm.
   
   Parameters:
   - f: Function to find root of (should change sign between low and high)
   - low: Lower bound of search interval
   - high: Upper bound of search interval
   - tolerance: Convergence tolerance (stop when |high - low| < tolerance)
   - max-iters: Maximum number of iterations
   
   Returns: {:root, :value, :iterations, :converged?}
   - root: The root found
   - value: f(root) - should be close to 0 (check |value| < tolerance for precision)
   - iterations: Number of iterations used
   - converged?: Whether convergence was achieved"
  (let [f-low (f low)
        f-high (f high)
precision 0.00001]
    (if (= (m/signum f-low) (m/signum f-high))
      {:root low
       :value f-low
       :iterations 0
       :converged? false
       :error "Function has same sign at both endpoints"}
      (loop [low low
             high high
             iter 0]
        (if (or (>= iter max-iters)
                (< (Math/abs (- high low)) precision))
          (let [mid (/ (+ low high) 2.0)
                f-mid (f mid)]
            {:root mid
             :value f-mid
             :iterations iter
             :converged? (< (Math/abs f-mid) tolerance)})
          (let [mid (/ (+ low high) 2.0)
                f-mid (f mid)
                f-low (f low)]
            (if (= (m/signum f-low) (m/signum f-mid))
              (recur mid high (inc iter))
              (recur low mid (inc iter))))))))))

(defn secant [f x0 x1 tolerance max-iters]
  "Secant method for root finding.
   
   The secant method is faster than bisection for smooth functions, but doesn't
   guarantee convergence. It uses linear interpolation between two points to
   approximate the root.
   
   Parameters:
   - f: Function to find root of
   - x0: First initial guess
   - x1: Second initial guess (should be different from x0)
   - tolerance: Convergence tolerance (stop when |f(x)| < tolerance or |x_n - x_{n-1}| < tolerance)
   - max-iters: Maximum number of iterations
   
   Returns: {:root, :value, :iterations, :converged?}
   - root: The root found
   - value: f(root) - should be close to 0
   - iterations: Number of iterations used
   - converged?: Whether convergence was achieved
   
   Algorithm:
   x_{n+1} = x_n - f(x_n) * (x_n - x_{n-1}) / (f(x_n) - f(x_{n-1}))
   
   Example:
   (secant #(- (* % %) 4) 1.0 3.0 1e-10 100)
   => Finds root of x² - 4 = 0 (should be x = 2)"
  (let [f-x0 (f x0)
        f-x1 (f x1)]
    ;; Check for nil values
    (if (or (nil? f-x0) (nil? f-x1))
      {:root x0
       :value f-x0
       :iterations 0
       :converged? false
       :error "Function returned nil"}
      ;; Check if we already have a root
      (if (< (Math/abs f-x0) tolerance)
        {:root x0
         :value f-x0
         :iterations 0
         :converged? true}
        (if (< (Math/abs f-x1) tolerance)
          {:root x1
           :value f-x1
           :iterations 0
           :converged? true}
          ;; Check if denominator would be zero (function values are equal)
          (if (< (Math/abs (- f-x1 f-x0)) 1e-15)
            {:root x1
             :value f-x1
             :iterations 0
             :converged? false
             :error "Function values at initial guesses are too close"}
            ;; Iterate using secant method
            (loop [x-prev x0
                   x-curr x1
                   f-prev f-x0
                   f-curr f-x1
                   iter 0]
              (if (or (>= iter max-iters)
                      (< (Math/abs f-curr) tolerance)
                      (< (Math/abs (- x-curr x-prev)) tolerance))
                {:root x-curr
                 :value f-curr
                 :iterations iter
                 :converged? (< (Math/abs f-curr) tolerance)}
                (let [;; Secant method formula
                      denominator (- f-curr f-prev)
                      ;; Avoid division by zero
                      x-next (if (< (Math/abs denominator) 1e-15)
                               x-curr  ; If denominator too small, don't update
                               (- x-curr (* f-curr (/ (- x-curr x-prev) denominator))))
                      f-next (f x-next)]
                  (if (nil? f-next)
                    {:root x-curr
                     :value f-curr
                     :iterations iter
                     :converged? false
                     :error "Function returned nil during iteration"}
                    (recur x-curr x-next f-curr f-next (inc iter))))))))))))

;; Finite-well functions removed - now in dwba.finite-well namespace

(defn hankel0+ [L rho]
  (complex-from-cartesian (g-func L rho ) (f-func L rho))
  )

(defn hankel0- [L rho]
  (complex-from-cartesian (g-func L rho ) (* -1.0 (f-func L rho)))
  )

(defn f-func-deriv [^long L ^double rho] 
  (* 0.5 (+ (f-func (dec L) rho) 
(/ (f-func L rho) rho)
 (* -1 (f-func (inc L) rho))
)))

(defn g-func-deriv [^long L ^double rho] 
  (* 0.5 (+ (g-func (dec L) rho) 
(/ (g-func L rho) rho)
 (* -1 (g-func (inc L) rho))
)))


(defn hankel0+-deriv [L rho]
  (complex-from-cartesian (g-func-deriv L rho ) (f-func-deriv L rho))
  )

(defn hankel0--deriv [L rho]
  (complex-from-cartesian (g-func-deriv L rho ) (* -1.0 (f-func-deriv L rho)))
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

(defn delta1 [k1 k2 R]; phase shift for L=1, square well
 (let [rho1 (* k1 R) rho2 (* k2 R)]   (- (m/acot (+ (* (/ rho2 rho1) (m/cot rho1 )) (/ 1. rho2) (/ (* -1. rho2) rho1 rho1))) rho2)))

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

(defn s-matrix-step [^double E ^double V  ^double a ^long L]
  (let [rm (r-matrix-step E V a L)
         k (m/sqrt (*  mass-factor E))
        rho (* k a)]
    (div (subt2 (hankel0- L rho) (mul rm rho (hankel0--deriv L rho )) )
         (subt2 (hankel0+ L rho) (mul rm rho (hankel0+-deriv L rho )) ))
 ))

(defn phase-shift-step  [^double E ^double V  ^double a ^long L ]
  (let [s (s-matrix-step E V a L)]
    (/ (arg s) 2)
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


;R-matrix method
 (defn epsn0 [n V a] ;eigenenergies of trial R-matrix eigenfunctions with beta = 0, and L=1
   (+ (Math/pow (* Math/PI (/ (+ n 0.5) (Math/sqrt mass-factor) a) ) 2) V))

(defn rm-omega0 [n a r] (* (m/sqrt (/ 2 a)) (Math/sin (* Math/PI (+ n 0.5) (/ r a) ))))

(defn rm0-N [E V a N] ; R in the R-matrix method, for L= 0
  (reduce +  (map (fn [n] (/ (rm-omega0 n a a) (epsn0 n V a) mass-factor a)) (range 1 N)))
)
;end R-matrix method

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

;; DWBA Differential Cross-Section Functions
(defn differential-cross-section [E-cm ws-params theta-cm L-max]
  "Calculate differential cross-section using full DWBA with Coulomb effects"
  (let [k (m/sqrt (* mass-factor E-cm))
        eta (* Z1Z2ee (/ mass-factor k 2))
        ;; Sum over partial waves
        total-amplitude 
        (reduce add
          (for [L (range 0 (inc L-max))]
            (let [S-matrix-val (s-matrix E-cm ws-params L)
                  phase-shift-val (phase-shift E-cm ws-params L)
                  ;; Scattering amplitude for this L
                  f-L (mul (div (complex-from-cartesian 0 -1) k)
                           (inc (* 2 L))
                           (poly/eval-legendre-P L (m/cos theta-cm))
                           (subt S-matrix-val 1.0))]
              f-L)))]
    ;; Differential cross-section = |f|²
    (mul total-amplitude (conj total-amplitude))))

(defn total-cross-section [E-cm ws-params L-max]
  "Calculate total cross-section using DWBA"
  (let [k (m/sqrt (* mass-factor E-cm))
        ;; Sum over partial waves
        total-sigma
        (reduce +
          (for [L (range 0 (inc L-max))]
            (let [S-matrix-val (s-matrix E-cm ws-params L)
                  ;; Cross-section contribution for this L
                  sigma-L (* (/ 2 E-cm) Math/PI (inc (* 2 L)) 
                             (Math/pow (mag (subt 1.0 S-matrix-val)) 2))]
              sigma-L)))]
    total-sigma))
