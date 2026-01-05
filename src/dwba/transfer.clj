(ns dwba.transfer
  "DWBA calculations for single nucleon transfer reactions.
   
   This namespace implements bound state wavefunctions, transfer form factors,
   and cross-section calculations for reactions like (d,p), (p,d), etc."
  (:require [fastmath.core :as m]
            [fastmath.special :as spec]
            [functions :refer :all]
            [complex :refer :all]))

;; ============================================================================
;; BOUND STATE WAVEFUNCTION SOLVER
;; ============================================================================

(defn bound-state-start [rho l]
  "Power series expansion for bound state near rho=0 (dimensionless).
   For bound states: u(rho) ≈ rho^(l+1) for small rho.
   This is the same as the naive start for scattering states."
  (Math/pow rho (inc l)))

(defn woods-saxon-dimensionless [rho alpha]
  "Woods-Saxon potential in dimensionless form.
   
   Parameters:
   - rho: Dimensionless radius (r/R0)
   - alpha: Dimensionless diffuseness (a0/R0)
   
   Returns: Dimensionless potential v(rho) = V(r)/V0 = -1/(1 + exp((rho-1)/alpha))"
  (/ -1.0 (+ 1.0 (Math/exp (/ (- rho 1.0) alpha)))))

(defn f-rho-numerov-dimensionless [rho epsilon l lambda alpha]
  "Effective potential function for Numerov integration in dimensionless form.
   
   Parameters:
   - rho: Dimensionless radius (r/R0)
   - epsilon: Dimensionless energy (E/V0)
   - l: Orbital angular momentum quantum number
   - lambda: Dimensionless parameter λ = (2μ/ħ²) · V0 · R0²
   - alpha: Dimensionless diffuseness (a0/R0)
   
   Returns: f(rho) = λ · [v(rho) + l(l+1)/(rho²) - ε]
   
   The Schrödinger equation in dimensionless form is:
   -d²u/dρ² + f(ρ)u = 0
   where f(ρ) = λ[v(ρ) + l(l+1)/(ρ²) - ε]"
  (if (zero? rho)
    ;; At rho=0, centrifugal term dominates: l(l+1)/rho^2 -> infinity
    ;; But we never actually use rho=0 in Numerov (starts at rho=h_rho)
    Double/POSITIVE_INFINITY
    (let [v-rho (woods-saxon-dimensionless rho alpha)
          centrifugal (/ (* l (inc l)) (* rho rho))
          v-eff (+ v-rho centrifugal)]
      (* lambda (- v-eff epsilon)))))

(defn solve-bound-state-numerov [e l v0 rad diff h r-max]
  "Solve the radial Schrödinger equation for a bound state using Numerov method
   with DIMENSIONLESS variables.
   
   Parameters (all in physical units):
   - e: Energy in MeV (must be negative for bound states)
   - l: Orbital angular momentum quantum number
   - v0: Woods-Saxon potential depth in MeV
   - rad: R0 parameter (nuclear radius) in fm
   - diff: a0 parameter (surface diffuseness) in fm
   - h: Step size in fm
   - r-max: Maximum radius for integration in fm
   
   Returns: Vector of wavefunction values u(r) at each grid point (physical units).
   
   Internal implementation uses dimensionless variables:
   - ρ = r/R0 (dimensionless radius)
   - ε = E/V0 (dimensionless energy)
   - α = a0/R0 (dimensionless diffuseness)
   - λ = (2μ/ħ²) · V0 · R0² (dimensionless coupling)
   - h_ρ = h/R0 (dimensionless step size)
   
   Note: For bound states, we expect u(r → ∞) → 0. This function
   just integrates; use find-bound-state-energy to find the correct energy."
  (let [;; Convert to dimensionless variables
        epsilon (/ e v0)  ; Dimensionless energy
        alpha (/ diff rad)  ; Dimensionless diffuseness
        lambda (* mass-factor v0 rad rad)  ; Dimensionless coupling parameter
        h-rho (/ h rad)  ; Dimensionless step size
        rho-max (/ r-max rad)  ; Dimensionless maximum radius
        
        steps (int (/ rho-max h-rho))
        ;; Initialize with bound state start: u(rho) ≈ rho^(l+1)
        ;; Note: u(rho) is the dimensionless radial wavefunction
        ;; For l=0: u(rho) ≈ rho, so u(h_rho) ≈ h_rho
        ;; For l=1: u(rho) ≈ rho^2, so u(h_rho) ≈ h_rho^2
        u0 0.0
        u1 (bound-state-start h-rho l)  ; u1 = h_rho^(l+1) in dimensionless units
        
        ;; Pre-calculate f(rho) values for Numerov in dimensionless form
        ;; f(rho) = λ · [v(rho) + l(l+1)/(rho²) - ε]
        ;; For bound states, ε < 0, so f(rho) > 0 in classically allowed region
        fs (mapv (fn [rho] 
                   (if (zero? rho)
                     0.0  ; f(0) is infinite, but u(0)=0, so f(0)*u(0)=0
                     (f-rho-numerov-dimensionless rho epsilon l lambda alpha)))
                 (take (+ steps 2) (iterate #(+ % h-rho) 0.0)))
        h-rho2-12 (/ (* h-rho h-rho) 12.0)]
    
    (let [results (loop [n 1
                         results [u0 u1]]
                    (if (>= n (dec steps))
                      results
                      (let [un (get results n)
                            un-1 (get results (dec n))
                            fn-1 (get fs (dec n))
                            fn (get fs n)
                            fn+1 (get fs (inc n))
                            
                            ;; Numerov step formula (dimensionless)
                            numerator (+ (* 2.0 un) 
                                         (- un-1) 
                                         (* h-rho2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
                            denominator (- 1.0 (* h-rho2-12 fn+1))
                            un+1 (/ numerator denominator)]
                        (recur (inc n) (conj results un+1)))))]
      ;; The radial wavefunction u(r) should satisfy u(r) ≈ r^(l+1) for small r
      ;; In dimensionless: u(rho) ≈ rho^(l+1) where rho = r/R0
      ;; For l=0: u(rho) ≈ rho, so u(h_rho) = h_rho = h/R0
      ;; But we want u(h) = h in physical units
      ;; So we need: u(h) = R0 * u(h_rho) = R0 * (h/R0) = h ✓
      ;; Therefore, we scale by R0 to convert from dimensionless to physical
      (mapv #(* rad %) results))))

(defn bound-state-boundary-value [u r-max h]
  "Check the boundary condition for a bound state.
   
   For a true bound state, u(r_max) should be approximately 0.
   Returns the value of u at r_max (should be close to 0 for bound state)."
  (let [idx (min (dec (count u)) (int (/ r-max h)))]
    (get u idx)))

(defn count-nodes [u]
  "Count the number of nodes (zeros) in the wavefunction.
   This helps identify the principal quantum number n.
   n = number of nodes + 1 (for l=0, n=1 has 0 nodes, n=2 has 1 node, etc.)
   
   Note: We skip the initial region where u ≈ 0 (near r=0) to avoid
   counting the boundary condition as a node. A node is where the
   wavefunction crosses zero AFTER it has started (i.e., after u > threshold)."
  (let [;; Find where wavefunction starts (becomes significantly non-zero)
        threshold 1e-6
        start-idx (loop [i 0]
                    (if (or (>= i (count u))
                            (> (Math/abs (get u i)) threshold))
                      i
                      (recur (inc i))))
        ;; Need at least 3 points after start to detect nodes reliably
        start-idx (max 2 (min start-idx (- (count u) 3)))]
    (if (>= start-idx (- (count u) 2))
      0  ; Can't count nodes if we don't have enough points
      (loop [n 0
             i (inc start-idx)
             prev-val (get u start-idx)
             prev-sign (m/signum prev-val)]
        (if (>= i (count u))
          n
          (let [u-i (get u i)
                current-sign (m/signum u-i)
                ;; Count a node if:
                ;; 1. Sign changes (crossed zero)
                ;; 2. Previous value was non-zero (not starting from zero)
                ;; 3. Current value is non-zero (not exactly at zero, which we handle separately)
                ;; 4. The values on either side are significant (not noise)
                crossed-zero (and (not= prev-sign current-sign)
                                  (not (zero? prev-sign))
                                  (not (zero? current-sign))
                                  (> (Math/abs prev-val) threshold)
                                  (> (Math/abs u-i) threshold))]
            (recur (if crossed-zero (inc n) n)
                   (inc i)
                   u-i
                   (if (zero? u-i) prev-sign current-sign))))))))

(defn find-energy-with-nodes
  "Coarse scan to find bound state energy with specific number of nodes.
   
   Parameters:
   - E-start, E-end: Energy range to search
   - num-steps: Number of energy points to scan
   - target-nodes: Desired number of nodes
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - l: Orbital angular momentum
   - r-max: Maximum radius
   - h: Step size
   
   Returns: {:energy E, :wavefunction u, :boundary-value u-end, :nodes n, :converged? bool}"
  [E-start E-end num-steps target-nodes V-params l r-max h]
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
        E-step (/ (- E-end E-start) num-steps)
        ;; First, find all candidates with correct number of nodes
        candidates-with-nodes (filter 
                               (fn [c] (= (:nodes c) target-nodes))
                               (for [i (range (inc num-steps))]
                                 (let [E (+ E-start (* i E-step))
                                       u (solve-bound-state-numerov E l v0 rad diff h r-max)
                                       u-end (bound-state-boundary-value u r-max h)
                                       nodes (count-nodes u)]
                                   {:energy E
                                    :wavefunction u
                                    :boundary-value u-end
                                    :nodes nodes})))
        ;; If we found candidates with correct nodes, pick the one with smallest boundary value
        best (if (seq candidates-with-nodes)
              (apply min-key (fn [c] (Math/abs (:boundary-value c))) candidates-with-nodes)
              ;; If no candidates with correct nodes, find best overall
              (let [all-candidates (for [i (range (inc num-steps))]
                                     (let [E (+ E-start (* i E-step))
                                           u (solve-bound-state-numerov E l v0 rad diff h r-max)
                                           u-end (bound-state-boundary-value u r-max h)
                                           nodes (count-nodes u)]
                                       {:energy E
                                        :wavefunction u
                                        :boundary-value u-end
                                        :nodes nodes
                                        :score (+ (* 10 (Math/abs (- nodes target-nodes)))  ; Heavy penalty for wrong nodes
                                                 (Math/abs u-end))}))]
                (apply min-key :score all-candidates)))]
    (assoc best :converged? (and (= (:nodes best) target-nodes)
                                (< (Math/abs (:boundary-value best)) 0.1)))))

(defn refine-bound-state-energy
  "Refine bound state energy using bisection around an initial guess.
   
   Parameters:
   - E-guess: Initial energy guess
   - target-nodes: Desired number of nodes
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - l: Orbital angular momentum
   - r-max: Maximum radius
   - h: Step size
   - tolerance: Energy convergence tolerance
   
   Returns: {:energy E, :wavefunction u, :boundary-value u-end, :nodes n, :converged? bool}
   
   Uses bisection to find energy where u(r_max) ≈ 0. If boundary values don't have
   opposite signs, uses golden section search to find minimum |u(r_max)|."
  [E-guess target-nodes V-params l r-max h tolerance]
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
        ;; Use larger range: ±15 MeV or from -V0 to -0.1, whichever is smaller
        E-range (min 15.0 (* v0 0.3))  ; Search ±15 MeV or 30% of V0
        E-lo (- E-guess E-range)
        E-hi (+ E-guess E-range)
        ;; Ensure E-lo and E-hi are negative (bound states)
        E-lo (max E-lo (- v0))
        E-hi (min E-hi -0.1)
        u-lo (solve-bound-state-numerov E-lo l v0 rad diff h r-max)
        u-hi (solve-bound-state-numerov E-hi l v0 rad diff h r-max)
        u-mid (solve-bound-state-numerov E-guess l v0 rad diff h r-max)
        u-lo-val (bound-state-boundary-value u-lo r-max h)
        u-hi-val (bound-state-boundary-value u-hi r-max h)
        u-mid-val (bound-state-boundary-value u-mid r-max h)
        ;; Check if we have opposite signs for bisection
        ;; Try to find a sign change by checking multiple points
        has-sign-change (or (not= (m/signum u-lo-val) (m/signum u-hi-val))
                            (not= (m/signum u-lo-val) (m/signum u-mid-val))
                            (not= (m/signum u-mid-val) (m/signum u-hi-val)))]
    (if has-sign-change
      ;; Use bisection function from functions.clj when signs are opposite
      ;; First, find the energy range with opposite signs
      (let [;; Search for sign change points
            search-points 20
            sign-change-range (loop [i 0
                                     found-range nil]
                                (if (or (>= i search-points) found-range)
                                  found-range
                                  (let [E-test (+ E-lo (* i (/ (- E-hi E-lo) (dec search-points))))
                                        u-test (solve-bound-state-numerov E-test l v0 rad diff h r-max)
                                        u-test-val (bound-state-boundary-value u-test r-max h)
                                        E-next (if (< i (dec search-points))
                                                 (+ E-lo (* (inc i) (/ (- E-hi E-lo) (dec search-points))))
                                                 E-hi)
                                        u-next (solve-bound-state-numerov E-next l v0 rad diff h r-max)
                                        u-next-val (bound-state-boundary-value u-next r-max h)]
                                    (if (not= (m/signum u-test-val) (m/signum u-next-val))
                                      (recur (inc i) [E-test E-next])
                                      (recur (inc i) found-range)))))
            ;; Use bisection on the range with sign change, or full range if none found
            [E-bisect-lo E-bisect-hi] (if sign-change-range
                                        sign-change-range
                                        [E-lo E-hi])
            ;; Define function f(E) = u(r_max) for bisection
            f (fn [E]
                (bound-state-boundary-value 
                 (solve-bound-state-numerov E l v0 rad diff h r-max) r-max h))
            bisection-result (bisection f E-bisect-lo E-bisect-hi tolerance 100)
            E-root (:root bisection-result)
            u-final (solve-bound-state-numerov E-root l v0 rad diff h r-max)
            u-final-val (bound-state-boundary-value u-final r-max h)
            nodes (count-nodes u-final)]
        {:energy E-root
         :wavefunction u-final
         :boundary-value u-final-val
         :nodes nodes
         :converged? (and (= nodes target-nodes)
                         (< (Math/abs u-final-val) 0.1))})
      ;; If no sign change, find minimum |u(r_max)| using grid search
      (let [;; Search for minimum boundary value with more points
            search-points 100
            candidates (for [i (range (inc search-points))]
                         (let [E-test (+ E-lo (* i (/ (- E-hi E-lo) search-points)))
                               u-test (solve-bound-state-numerov E-test l v0 rad diff h r-max)
                               u-test-val (bound-state-boundary-value u-test r-max h)
                               nodes-test (count-nodes u-test)]
                           {:energy E-test
                            :wavefunction u-test
                            :boundary-value u-test-val
                            :boundary-abs (Math/abs u-test-val)
                            :nodes nodes-test}))
            best (apply min-key :boundary-abs candidates)]
        {:energy (:energy best)
         :wavefunction (:wavefunction best)
         :boundary-value (:boundary-value best)
         :nodes (:nodes best)
         :converged? (and (= (:nodes best) target-nodes)
                         (< (:boundary-abs best) 0.1))}))))

(defn find-bound-state-energy
  "Find bound state energy using shooting method.
   
   Parameters:
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - l: Orbital angular momentum
   - n: Principal quantum number (1, 2, 3, ...)
   - r-max: Maximum radius for integration
   - h: Step size
   - E-min: Minimum energy to search (default: -V0, the potential depth)
   - E-max: Maximum energy to search (default: -0.1 MeV, just below zero)
   - tolerance: Energy convergence tolerance (default: 0.01 MeV)
   
   Returns: {:energy E, :wavefunction u, :nodes n-nodes, :converged? bool}
   
   Algorithm:
   1. Coarse scan to find approximate energy with correct nodes
   2. Refine using bisection around best candidate
   3. For different n values, searches in different energy ranges
       because bound states are ordered: E(n=1) < E(n=2) < E(n=3) < ..."
  ([V-params l n r-max h]
   (let [v0 (first V-params)]
     (find-bound-state-energy V-params l n r-max h (- v0) -0.1 0.01)))
  ([V-params l n r-max h E-min E-max tolerance]
     (let [v0 (first V-params)
           expected-nodes (- n 1)  ; n=1 has 0 nodes, n=2 has 1 node, etc.
         ;; Energy ranges: deeper states have lower (more negative) energies
         ;; For a Woods-Saxon well of depth V0:
         ;; - Ground state (n=1) is typically around -V0/2 to -V0/3
         ;; - Excited states are at progressively higher (less negative) energies
         ;; IMPORTANT: All energies must be NEGATIVE for bound states
         ;; Typical bound state energies: E ≈ -V0/2 to -V0/3 for ground state
         ;; For V0=50 MeV: ground state around -25 to -17 MeV
         E-search-min (cond
                       (= n 1) (- (* v0 0.7))  ; Ground: search from -35 MeV (70% of V0)
                       (= n 2) (- (* v0 0.5))  ; 2s: above ground, around -25 MeV
                       (= n 3) (- (* v0 0.3))  ; 3s: above 2s, around -15 MeV
                       :else (- (* v0 (- 0.7 (* (- n 1) 0.15)))))  ; Higher n: progressively higher
         E-search-max (cond
                       (= n 1) (- (* v0 0.3))  ; Ground: up to -15 MeV (30% of V0)
                       (= n 2) (- (* v0 0.2))  ; 2s: up to -10 MeV
                       (= n 3) (- (* v0 0.1))  ; 3s: up to -5 MeV
                       :else (- (* v0 (- 0.3 (* (- n 1) 0.05)))))  ; Higher n: closer to zero
         ;; Coarse scan with more points for better resolution
         coarse-result (find-energy-with-nodes E-search-min E-search-max 150 expected-nodes 
                                                V-params l r-max h)
         E-guess (:energy coarse-result)
         coarse-boundary (Math/abs (:boundary-value coarse-result))]
     ;; Always try to refine if we found correct nodes
     (if (= (:nodes coarse-result) expected-nodes)
       (let [refined (refine-bound-state-energy E-guess expected-nodes V-params l r-max h tolerance)
             refined-boundary (Math/abs (:boundary-value refined))]
         ;; If refinement improved significantly, use it; otherwise try wider search
         (if (< refined-boundary (* 0.5 coarse-boundary))
           refined
           (let [E-wide-min (- (* v0 0.8))  ; Wider range: -40 MeV
                 E-wide-max (- (* v0 0.1))   ; Up to -5 MeV
                 wide-result (find-energy-with-nodes E-wide-min E-wide-max 200 expected-nodes 
                                                     V-params l r-max h)]
             (if (= (:nodes wide-result) expected-nodes)
               (let [wide-refined (refine-bound-state-energy (:energy wide-result) expected-nodes 
                                                              V-params l r-max h tolerance)]
                 (if (< (Math/abs (:boundary-value wide-refined)) (Math/abs (:boundary-value refined)))
                   wide-refined
                   refined))
               refined))))
       ;; If wrong number of nodes, try wider search
       (let [E-wide-min (- (* v0 0.8))  ; Wider range: -40 MeV
             E-wide-max (- (* v0 0.1))   ; Up to -5 MeV
             wide-result (find-energy-with-nodes E-wide-min E-wide-max 200 expected-nodes 
                                                 V-params l r-max h)]
         (if (= (:nodes wide-result) expected-nodes)
           (refine-bound-state-energy (:energy wide-result) expected-nodes V-params l r-max h tolerance)
           ;; Return best we found, even if nodes don't match
           (do
             (println (format "Warning: Could not find state with %d nodes. Found %d nodes at E=%.2f MeV"
                             expected-nodes (:nodes coarse-result) (:energy coarse-result)))
             coarse-result)))))))  ; Return best we found

(defn normalize-bound-state [u h]
  "Normalize bound state wavefunction so that ∫₀^∞ |u(r)|² dr = 1.
   
   Uses Simpson's rule for integration: ∫ f(r) dr ≈ (h/3) * [f₀ + fₙ + 4∑f_odd + 2∑f_even]
   
   Parameters:
   - u: Wavefunction vector
   - h: Step size
   
   Returns: Normalized wavefunction vector"
  (let [;; Calculate normalization integral: N² = ∫ u²(r) r² dr
        ;; For radial wavefunctions, normalization is ∫ u²(r) dr (not r²)
        ;; But in some conventions it's ∫ u²(r) r² dr - we'll use ∫ u²(r) dr
        integrand (mapv #(* % %) u)
        n (count integrand)
        ;; Simpson's rule
        simpson-sum (loop [i 1
                           sum 0.0]
                      (if (>= i (dec n))
                        sum
                        (let [coeff (if (odd? i) 4.0 2.0)
                              term (* coeff (get integrand i))]
                          (recur (inc i) (+ sum term)))))
        integral (* (/ h 3.0) 
                    (+ (first integrand) 
                       (last integrand) 
                       simpson-sum))
        norm-factor (Math/sqrt integral)
        ;; Avoid division by zero
        norm-factor (if (zero? norm-factor) 1.0 (/ 1.0 norm-factor))]
    (mapv #(* % norm-factor) u)))

(defn solve-bound-state
  "Main function to solve for a bound state wavefunction.
   
   Parameters:
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - n: Principal quantum number (1, 2, 3, ...)
   - l: Orbital angular momentum (0, 1, 2, ...)
   - j: Total angular momentum (l ± 1/2 for nucleons, but we'll ignore spin-orbit for now)
   - r-max: Maximum radius (default: 20.0 fm)
   - h: Step size (default: 0.01 fm)
   
   Returns: {:energy E, :wavefunction u, :normalized-wavefunction u-norm, 
             :nodes n-nodes, :quantum-numbers {:n n, :l l, :j j}}
   
   Example:
   (solve-bound-state [50.0 2.0 0.6] 1 0 nil)
   => Finds 1s bound state in Woods-Saxon well"
  ([V-params n l]
   (solve-bound-state V-params n l nil 20.0 0.01))
  ([V-params n l j]
   (solve-bound-state V-params n l j 20.0 0.01))
  ([V-params n l j r-max h]
   (let [result (find-bound-state-energy V-params l n r-max h)
         u-norm (normalize-bound-state (:wavefunction result) h)]
     {:energy (:energy result)
      :wavefunction (:wavefunction result)
      :normalized-wavefunction u-norm
      :nodes (:nodes result)
      :boundary-value (:boundary-value result)
      :converged? (:converged? result)
      :quantum-numbers {:n n, :l l, :j j}
      :r-max r-max
      :h h})))

;; ============================================================================
;; UTILITY FUNCTIONS
;; ============================================================================

(defn bound-state-energy-approx [V-params n l]
  "Rough estimate of bound state energy using infinite square well approximation.
   Useful for providing initial guess: E ≈ -V0 + (n²π²ħ²)/(2mR²)
   
   This is just a rough estimate; actual energy will be different due to
   finite well depth and Woods-Saxon shape."
  (let [V0 (first V-params)
        R0 (second V-params)
        ;; Infinite square well: E_n = n²π²ħ²/(2mR²) - V0
        ;; For bound state, E < 0, so: E ≈ -V0 + n²π²ħ²/(2mR²)
        ;; But this gives positive energy, so we need: E ≈ -V0 + ...
        ;; Actually, for a well of depth V0, ground state is roughly at -V0/2
        ;; For excited states: E_n ≈ -V0 + n² * (V0/4) for n=1,2,3,...
        ]
    (- V0 (* n n (/ V0 4.0)))))

(defn plot-bound-state-info [result]
  "Print information about a bound state solution.
   Useful for debugging and verification."
  (println "=== Bound State Information ===")
  (println (format "Quantum numbers: n=%d, l=%d, j=%s"
                   (get-in result [:quantum-numbers :n])
                   (get-in result [:quantum-numbers :l])
                   (get-in result [:quantum-numbers :j])))
  (println (format "Energy: %.6f MeV" (:energy result)))
  (println (format "Number of nodes: %d" (:nodes result)))
  (println (format "Boundary value at r_max: %.6e" (:boundary-value result)))
  (println (format "Converged: %s" (:converged? result)))
  (println (format "Wavefunction length: %d points" (count (:normalized-wavefunction result))))
  (println ""))
