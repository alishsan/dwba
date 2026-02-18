(ns dwba.halo-nuclei
  "Extensions to the Bessel-Numerov method for halo nuclei calculations.
   
   This module implements:
   - Extended asymptotic matching for large-radius wavefunctions
   - Coulomb interactions for charged halo nuclei
   - Bound state calculations with Riccati-Hankel initialization
   - Asymptotic Normalization Coefficient (ANC) extraction
   - Low-energy scattering calculations
   
   References:
   - numerical_riccati.tex: Main paper on Bessel-Numerov method
   - halo_nuclei_improvements.tex: Detailed improvements for halo nuclei"
  (:require [fastmath.core :as m]))

(def hbarc 197.7) ; MeV·fm (ℏc)
(def e2 1.44) ; e² in MeV·fm

;; ============================================================================
;; 1. Extended Asymptotic Matching Region
;; ============================================================================

(defn decay-length
  "Calculate the decay length κ⁻¹ = ℏ/√(2μ|E_b|) for a bound state.
   
   Parameters:
   - E_b: Binding energy (MeV, positive value)
   - mu: Reduced mass (MeV/c²)
   
   Returns: Decay length in fm
   
   Example:
   (decay-length 0.504 869.4) ; For ¹¹Be (E_b = 504 keV)
   => ~10.5 fm"
  [E-b mu]
  (let [kappa (Math/sqrt (/ (* 2.0 mu (Math/abs E-b)) (* hbarc hbarc)))]
    (/ 1.0 kappa)))

(defn adaptive-matching-radius
  "Calculate adaptive matching radius for halo nuclei.
   
   Formula: a_match = max(3R, 5/κ, 20 fm)
   
   Parameters:
   - R: Nuclear radius (fm)
   - E-b: Binding energy (MeV, positive value)
   - mu: Reduced mass (MeV/c²)
   
   Returns: Matching radius in fm
   
   Example:
   (adaptive-matching-radius 2.0 0.504 869.4) ; For ¹¹Be
   => ~30-40 fm"
  [R E-b mu]
  (let [kappa-inv (decay-length E-b mu)
        r1 (* 3.0 R)
        r2 (* 5.0 kappa-inv)
        r3 20.0]
    (max r1 r2 r3)))

;; ============================================================================
;; 2. Bound State Initialization (Riccati-Hankel)
;; ============================================================================

(defn double-factorial
  "Calculate double factorial (2l+1)!! = (2l+1)(2l-1)...3·1"
  [l]
  (if (<= l 0)
    1
    (reduce * (range 1 (+ (* 2 l) 2) 2))))

(defn bessel-start-bound-state
  "Riccati-Hankel initialization for bound states (E < 0).
   
   Uses imaginary wave number q = iκ where κ = √(2μ|E_b|/ℏ²).
   
   For l=1: u(h) ≈ (iκh)²/(3!!) [1 - (κh)²/(2·5) + ...]
   
   Parameters:
   - h: Step size (fm)
   - kappa: Decay constant κ = √(2μ|E_b|/ℏ²) (fm⁻¹)
   - l: Angular momentum quantum number
   
   Returns: Initial wavefunction value u(h)
   
   Example:
   (bessel-start-bound-state 0.01 0.095 1) ; For ¹¹Be
   => ~3.0e-5"
  [h kappa l]
  (let [kh (* kappa h)
        ;; Power series: (iκh)^(l+1) / (2l+1)!! [1 - (κh)²/(2(2l+3)) + ...]
        ;; For small kh, we use the expansion
        base (m/pow kh (inc l))
        dfac (double-factorial l)
        first-term (/ base dfac)
        correction (- 1.0 (/ (* kh kh) (* 2.0 (+ (* 2 l) 3))))]
    (* first-term correction)))

(defn solve-bound-state-numerov
  "Solve bound state using Numerov with Riccati-Hankel initialization.
   
   Parameters:
   - E-b: Binding energy (MeV, positive value)
   - l: Angular momentum
   - V-params: [V0 R a] Woods-Saxon parameters
   - mu: Reduced mass (MeV/c²)
   - h: Step size (fm)
   - r-max: Maximum radius (fm)
   
   Returns: Vector of wavefunction values u(r)
   
   Example:
   (solve-bound-state-numerov 0.504 0 [62.0 2.7 0.6] 869.4 0.01 50.0)
   => wavefunction for ¹¹Be"
  [E-b l [V0 R a] mu h r-max]
  (let [mass-factor (/ (* 2.0 mu) (* hbarc hbarc))
        kappa (Math/sqrt (/ (* 2.0 mu E-b) (* hbarc hbarc)))
        E (- E-b) ; Energy is negative for bound state
        steps (int (/ r-max h))
        ;; Initialize with bound-state Bessel start
        u0 0.0
        u1 (bessel-start-bound-state h kappa l)
        
        ;; Woods-Saxon potential
        ws-pot (fn [r]
                 (/ (- V0) (+ 1.0 (Math/exp (/ (- r R) a)))))
        
        ;; f(r) function for Numerov
        f-r (fn [r]
              (if (zero? r)
                0.0
                (let [v-tot (ws-pot r)
                      centrifugal (/ (* l (inc l)) (* r r))
                      v-eff (+ v-tot centrifugal)]
                  (* mass-factor (- v-eff E)))))
        
        ;; Pre-calculate f(r) values
        fs (mapv (fn [r] (f-r r))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    
    (loop [n 1
           results [u0 u1]]
      (if (>= n (dec steps))
        results
        (let [un (get results n)
              un-1 (get results (dec n))
              fn-1 (get fs (dec n))
              fn (get fs n)
              fn+1 (get fs (inc n))
              
              numerator (+ (* 2.0 un)
                           (- un-1)
                           (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
              denominator (- 1.0 (* h2-12 fn+1))
              un+1 (/ numerator denominator)]
          (recur (inc n) (conj results un+1)))))))

;; ============================================================================
;; 3. Coulomb Interactions
;; ============================================================================

(defn sommerfeld-parameter
  "Calculate Sommerfeld parameter η = Z₁Z₂e²μ/(ℏ²k).
   
   Parameters:
   - Z1, Z2: Charge numbers
   - mu: Reduced mass (MeV/c²)
   - k: Wave number (fm⁻¹)
   
   Returns: Sommerfeld parameter (dimensionless)
   
   Example:
   (sommerfeld-parameter 4 1 745.0 0.5) ; For α+p at E ~ 10 MeV
   => ~0.6"
  [Z1 Z2 mu k]
  (if (zero? k)
    Double/POSITIVE_INFINITY
    (/ (* Z1 Z2 e2 mu) (* hbarc hbarc k))))

(defn coulomb-normalization-constant
  "Calculate Coulomb normalization constant C_l(η).
   
   For small η: C_l(η) ≈ 1/(2l+1)!!
   
   Parameters:
   - _eta: Sommerfeld parameter (not used in simplified form)
   - l: Angular momentum
   
   Returns: Normalization constant"
  [_eta l]
  (let [dfac (double-factorial l)]
    (/ 1.0 dfac)))

(defn bessel-start-coulomb
  "Riccati-Bessel initialization with Coulomb effects.
   
   u_l(h) ≈ C_l(η) (kh)^(l+1) [1 + ηkh/(l+1) + ...]
   
   Parameters:
   - h: Step size (fm)
   - k: Wave number (fm⁻¹)
   - eta: Sommerfeld parameter
   - l: Angular momentum
   
   Returns: Initial wavefunction value u(h)
   
   Example:
   (bessel-start-coulomb 0.01 0.5 0.6 1) ; For α+p scattering
   => ~8.3e-5"
  [h k eta l]
  (let [kh (* k h)
        C-l (coulomb-normalization-constant eta l)
        base (* C-l (m/pow kh (inc l)))
        correction (+ 1.0 (/ (* eta kh) (inc l)))]
    (* base correction)))

(defn coulomb-potential
  "Coulomb potential V_C(r) = Z₁Z₂e²/r.
   
   For r < r0, uses constant potential to avoid singularity.
   
   Parameters:
   - r: Radius (fm)
   - Z1, Z2: Charge numbers
   - r0: Cutoff radius (fm), typically ~0.1 fm
   
   Returns: Potential in MeV"
  [r Z1 Z2 r0]
  (if (< r r0)
    (/ (* Z1 Z2 e2) r0)
    (/ (* Z1 Z2 e2) r)))

(defn solve-coulomb-numerov
  "Solve scattering with Coulomb + nuclear potential using Numerov.
   
   Parameters:
   - E: Energy (MeV)
   - l: Angular momentum
   - V-params: [V0 R a] Woods-Saxon parameters
   - Z1, Z2: Charge numbers
   - mu: Reduced mass (MeV/c²)
   - h: Step size (fm)
   - r-max: Maximum radius (fm)
   
   Returns: Vector of wavefunction values u(r)
   
   Example:
   (solve-coulomb-numerov 10.0 1 [50.0 2.0 0.6] 4 1 745.0 0.01 30.0)
   => wavefunction for α+p scattering"
  [E l [V0 R a] Z1 Z2 mu h r-max]
  (let [mass-factor (/ (* 2.0 mu) (* hbarc hbarc))
        k (Math/sqrt (/ (* 2.0 mu E) (* hbarc hbarc)))
        eta (sommerfeld-parameter Z1 Z2 mu k)
        steps (int (/ r-max h))
        r0 0.1 ; Coulomb cutoff radius
        
        ;; Initialize with Coulomb Bessel start
        u0 0.0
        u1 (bessel-start-coulomb h k eta l)
        
        ;; Combined potential
        total-pot (fn [r]
                    (let [v-nuclear (/ (- V0) (+ 1.0 (Math/exp (/ (- r R) a))))
                          v-coulomb (coulomb-potential r Z1 Z2 r0)]
                      (+ v-nuclear v-coulomb)))
        
        ;; f(r) function
        f-r (fn [r]
              (if (zero? r)
                0.0
                (let [v-tot (total-pot r)
                      centrifugal (/ (* l (inc l)) (* r r))
                      v-eff (+ v-tot centrifugal)]
                  (* mass-factor (- v-eff E)))))
        
        fs (mapv (fn [r] (f-r r))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    
    (loop [n 1
           results [u0 u1]]
      (if (>= n (dec steps))
        results
        (let [un (get results n)
              un-1 (get results (dec n))
              fn-1 (get fs (dec n))
              fn (get fs n)
              fn+1 (get fs (inc n))
              
              numerator (+ (* 2.0 un)
                           (- un-1)
                           (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
              denominator (- 1.0 (* h2-12 fn+1))
              un+1 (/ numerator denominator)]
          (recur (inc n) (conj results un+1)))))))

;; ============================================================================
;; 4. Asymptotic Normalization Coefficient (ANC) Extraction
;; ============================================================================

(defn whittaker-w
  "Calculate Whittaker function W_{-η, l+1/2}(2κr) for bound states.
   
   For large r, this gives the asymptotic form of the bound state wavefunction.
   
   Uses approximation: W_{-η, l+1/2}(z) ≈ z^{-η} e^{-z/2} for large z
   
   Parameters:
   - r: Radius (fm)
   - kappa: Decay constant (fm⁻¹)
   - eta: Effective Sommerfeld parameter (0 for neutral systems)
   - _l: Angular momentum (not used in simplified form)
   
   Returns: Whittaker function value
   
   Note: This is a simplified implementation. For production use,
   consider using a specialized library for Whittaker functions."
  [r kappa eta _l]
  (let [z (* 2.0 kappa r)
        ;; Simplified form for large r: W_{-η, l+1/2}(z) ≈ z^{-η} e^{-z/2}
        ;; More accurate would require confluent hypergeometric functions
        exp-term (Math/exp (/ (- z) 2.0))
        power-term (m/pow z (- eta))]
    (* power-term exp-term)))

(defn extract-anc
  "Extract Asymptotic Normalization Coefficient from bound state wavefunction.
   
   The ANC C is extracted from the asymptotic form:
   u_l(r) ~ C W_{-η, l+1/2}(2κr) as r → ∞
   
   Parameters:
   - u-wave: Vector of wavefunction values
   - r-values: Vector of corresponding radii
   - E-b: Binding energy (MeV)
   - mu: Reduced mass (MeV/c²)
   - Z1, Z2: Charge numbers (0 for neutral)
   - l: Angular momentum
   - r-match: Matching radius in asymptotic region (fm)
   
   Returns: ANC in fm^(-1/2)
   
   Example:
   (let [u (solve-bound-state-numerov 0.504 0 [62.0 2.7 0.6] 869.4 0.01 50.0)
         r (range 0.0 50.0 0.01)]
     (extract-anc u r 0.504 869.4 0 0 0 30.0))
   => ANC for ¹¹Be"
  [u-wave r-values E-b mu Z1 Z2 l r-match]
  (let [kappa (Math/sqrt (/ (* 2.0 mu E-b) (* hbarc hbarc)))
        k (Math/sqrt (/ (* 2.0 mu E-b) (* hbarc hbarc)))
        eta-val (if (and (zero? Z1) (zero? Z2))
                  0.0
                  (sommerfeld-parameter Z1 Z2 mu k))
        
        ;; Find index closest to r_match
        match-idx (first (keep-indexed
                          (fn [i r] (when (>= r r-match) i))
                          r-values))
        
        ;; Use several points in asymptotic region for averaging
        n-points 10
        start-idx (max 0 (- match-idx (int (/ n-points 2))))
        end-idx (min (count u-wave) (+ start-idx n-points))
        
        ;; Extract ANC from each point and average
        anc-values (for [i (range start-idx end-idx)]
                     (let [r (nth r-values i)
                           u (nth u-wave i)
                           w (whittaker-w r kappa eta-val l)]
                       (if (and (> (Math/abs w) 1e-10) (> (Math/abs u) 1e-10))
                         (/ u w)
                         nil)))
        
        valid-ancs (filter some? anc-values)]
    
    (if (seq valid-ancs)
      (/ (reduce + valid-ancs) (count valid-ancs))
      0.0)))

;; ============================================================================
;; 5. Low-Energy Scattering
;; ============================================================================

(defn solve-low-energy-numerov
  "Solve scattering at very low energies (E ~ 10-100 keV).
   
   Uses extended matching radius and careful handling of near-threshold behavior.
   
   Parameters:
   - E: Energy (MeV), typically 0.01-0.1 MeV
   - l-val: Angular momentum
   - V-params: [V0 R a] Woods-Saxon parameters
   - mu: Reduced mass (MeV/c²)
   - h: Step size (fm)
   - r-max: Maximum radius (fm), should be large for low energy
   
   Returns: Vector of wavefunction values u(r)
   
   Example:
   (solve-low-energy-numerov 0.05 0 [50.0 2.0 0.6] 869.4 0.01 50.0)
   => wavefunction at 50 keV"
  [E l-val [V0 R a] mu h r-max]
  (let [mass-factor (/ (* 2.0 mu) (* hbarc hbarc))
        q (Math/sqrt (* mass-factor (+ E V0)))
        steps (int (/ r-max h))
        
        ;; Use Bessel start (same as regular scattering)
        u0 0.0
        u1 (if (= l-val 1)
             ;; For l=1, use power series: F1(qh) ≈ (qh)²/3 - (qh)⁴/30
             (let [z (* q h)]
               (- (/ (* z z) 3.0) (/ (* z z z z) 30.0)))
             ;; For other l, use general power series
             (m/pow (* q h) (inc l-val)))
        
        ws-pot (fn [r]
                 (/ (- V0) (+ 1.0 (Math/exp (/ (- r R) a)))))
        
        f-r (fn [r]
              (if (zero? r)
                0.0
                (let [v-tot (ws-pot r)
                      centrifugal (/ (* l-val (inc l-val)) (* r r))
                      v-eff (+ v-tot centrifugal)]
                  (* mass-factor (- v-eff E)))))
        
        fs (mapv (fn [r] (f-r r))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    
    (loop [n 1
           results [u0 u1]]
      (if (>= n (dec steps))
        results
        (let [un (get results n)
              un-1 (get results (dec n))
              fn-1 (get fs (dec n))
              fn (get fs n)
              fn+1 (get fs (inc n))
              
              numerator (+ (* 2.0 un)
                           (- un-1)
                           (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
              denominator (- 1.0 (* h2-12 fn+1))
              un+1 (/ numerator denominator)]
          (recur (inc n) (conj results un+1)))))))

;; ============================================================================
;; Example Usage Functions
;; ============================================================================

(defn example-11be
  "Example calculation for ¹¹Be halo nucleus.
   
   Properties: E_b = 504 keV, l = 0, S_{1/2} state
   
   Returns: Map with wavefunction, ANC, and matching radius"
  []
  (let [E-b 0.504 ; MeV
        l 0
        V-params [62.0 2.7 0.6] ; [V0 R a]
        mu 869.4 ; MeV/c²
        h 0.01 ; fm
        r-max 50.0 ; fm
        
        ;; Calculate adaptive matching radius
        R (second V-params)
        r-match (adaptive-matching-radius R E-b mu)
        
        ;; Solve bound state
        u (solve-bound-state-numerov E-b l V-params mu h r-max)
        r-values (map #(* % h) (range (count u)))
        
        ;; Extract ANC
        anc (extract-anc u r-values E-b mu 0 0 l r-match)]
    
    {:wavefunction u
     :radii r-values
     :anc anc
     :matching-radius r-match
     :binding-energy E-b}))

(defn example-8b
  "Example calculation for ⁸B proton halo nucleus.
   
   Properties: E_b = 137 keV, l = 2, p-wave proton halo
   
   Returns: Map with wavefunction, ANC, and matching radius"
  []
  (let [E-b 0.137 ; MeV
        l 2
        V-params [50.0 2.0 0.6] ; [V0 R a]
        mu 745.0 ; MeV/c² (approximate for ⁷Be+p)
        h 0.01 ; fm
        r-max 60.0 ; fm
        
        R (second V-params)
        r-match (adaptive-matching-radius R E-b mu)
        
        u (solve-bound-state-numerov E-b l V-params mu h r-max)
        r-values (map #(* % h) (range (count u)))
        
        ;; For ⁸B, Z1=4 (⁷Be), Z2=1 (proton)
        anc (extract-anc u r-values E-b mu 4 1 l r-match)]
    
    {:wavefunction u
     :radii r-values
     :anc anc
     :matching-radius r-match
     :binding-energy E-b}))

(defn example-coulomb-scattering
  "Example Coulomb + nuclear scattering calculation.
   
   Calculates α+p scattering at 10 MeV.
   
   Returns: Map with wavefunction and parameters"
  []
  (let [E 10.0 ; MeV
        l 1
        V-params [50.0 2.0 0.6] ; [V0 R a]
        Z1 4 ; α particle
        Z2 1 ; proton
        mu 745.0 ; MeV/c²
        h 0.01 ; fm
        r-max 30.0 ; fm
        
        u (solve-coulomb-numerov E l V-params Z1 Z2 mu h r-max)
        r-values (map #(* % h) (range (count u)))
        
        k (Math/sqrt (/ (* 2.0 mu E) (* hbarc hbarc)))
        eta (sommerfeld-parameter Z1 Z2 mu k)]
    
    {:wavefunction u
     :radii r-values
     :energy E
     :sommerfeld-parameter eta
     :wave-number k}))
