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
  (m/pow rho (inc l)))

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
   in physical units.
   
   Parameters (all in physical units):
   - e: Energy in MeV (must be negative for bound states)
   - l: Orbital angular momentum quantum number
   - v0: Woods-Saxon potential depth in MeV
   - rad: R0 parameter (nuclear radius) in fm
   - diff: a0 parameter (surface diffuseness) in fm
   - h: Step size in fm
   - r-max: Maximum radius for integration in fm
   
   Returns: Vector of wavefunction values u(r) at each grid point (physical units).
   
   Note: For bound states, we expect u(r → ∞) → 0. This function
   just integrates; use find-bound-state-energy to find the correct energy."
  (let [steps (int (/ r-max h))
        ;; Initialize with bound state start: u(r) ≈ r^(l+1) for small r
        ;; For l=0: u(r) ≈ r, so u(h) ≈ h
        ;; For l=1: u(r) ≈ r^2, so u(h) ≈ h^2
        u0 0.0
        u1 (m/pow h (inc l))  ; u1 = h^(l+1) in physical units
        
        ;; Pre-calculate f(r) values for Numerov using f-r-numerov
        ;; f(r) = (2μ/ħ²) · [V_eff(r) - E]
        ;; For bound states, E < 0, so f(r) > 0 in classically allowed region
        fs (mapv (fn [r] 
                   (if (zero? r)
                     0.0  ; f(0) is infinite, but u(0)=0, so f(0)*u(0)=0
                     (f-r-numerov r e l v0 rad diff)))
                 (take (+ steps 2) (iterate #(+ % h) 0.0)))
        h2-12 (/ (* h h) 12.0)]
    
    (let [results (loop [n 1
                         results [u0 u1]]
                    (if (>= n (dec steps))
                      results
                      (let [un (get results n)
                            un-1 (get results (dec n))
                            fn-1 (get fs (dec n))
                            fn (get fs n)
                            fn+1 (get fs (inc n))
                            
                            ;; Numerov step formula (physical units)
                            numerator (+ (* 2.0 un) 
                                         (- un-1) 
                                         (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
                            denominator (- 1.0 (* h2-12 fn+1))
                            un+1 (/ numerator denominator)]
                        (recur (inc n) (conj results un+1)))))]
      results)))

(defn solve-bound-state-numerov-dimensionless [epsilon l lambda alpha h-rho rho-max]
  "Solve the radial Schrödinger equation for a bound state using Numerov method
   with DIMENSIONLESS variables.
   
   Parameters (all in physical units):
   - e: Energy in MeV (must be negative for bound states)
   - l: Orbital angular momentum quantum number
   - v0: Woods-Saxon potential depth in MeV
   - rad: R0 parameter (nuclear radius) in fm
   - diff: a0 parameter (surface diffuseness) in fm
   - h: Step size in fm

   

   
  Parameters: dimensionless variables:

   - ε = E/V0 (dimensionless energy)
  - l: Orbital angular momentum quantum number
   - λ = (2μ/ħ²) · V0 · R0² (dimensionless coupling)
   - α = a0/R0 (dimensionless diffuseness)
   - h_ρ = h/R0 (dimensionless step size)
   - rho-max: Maximum radius for integration (rho-max = r-max /rad)

   Returns: Vector of wavefunction values u(rho) at each grid point.

   Note:   - ρ = r/R0 (dimensionless radius)
   Note: For bound states, we expect u(r → ∞) → 0. This function
   just integrates; use find-bound-state-energy to find the correct energy."
  (let [               
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
       results)))


(defn bound-state-boundary-value [u r-max h]
  "Check the boundary condition for a bound state.
   
   For a true bound state, u(r_max) should be approximately 0.
   Returns the value of u at r_max (should be close to 0 for bound state)."
  (let [idx (min (dec (count u)) (int (/ r-max h)))]
    (get u idx)))

(defn count-nodes [u]
  "Count the number of nodes (zeros) in the wavefunction.
   This helps identify the principal quantum number n.
   Number of radial nodes = n - l - 1
   (e.g., 1s: 0 nodes, 2s: 1 node, 2p: 0 nodes, 3s: 2 nodes, 3p: 1 node, 3d: 0 nodes)
   
   Note: 
   - We skip the initial region where u ≈ 0 (near r=0) to avoid
     counting the boundary condition as a node.
   - We also exclude the last few points near r_max to avoid counting
     spurious nodes from the shooting method boundary condition.
   A node is where the wavefunction crosses zero AFTER it has started
   and BEFORE it reaches the boundary region."
  (let [;; Find where wavefunction starts (becomes significantly non-zero)
        threshold 1e-6
        start-idx (loop [i 0]
                    (if (or (>= i (count u))
                            (> (Math/abs (get u i)) threshold))
                      i
                      (recur (inc i))))
        ;; Exclude last 5% of points near r_max to avoid boundary artifacts
        ;; This prevents counting spurious nodes from shooting method
        end-idx (max start-idx (- (count u) (max 10 (int (* 0.05 (count u))))))
        ;; Need at least 3 points between start and end to detect nodes reliably
        start-idx (max 2 (min start-idx (- end-idx 3)))]
    (if (>= start-idx end-idx)
      0  ; Can't count nodes if we don't have enough points
      (loop [n 0
             i (inc start-idx)
             prev-val (get u start-idx)
             prev-sign (m/signum prev-val)]
        (if (>= i end-idx)
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

(defn scan-energy-range
  "Helper function to scan an energy range and compute wavefunctions.
   Returns a vector of candidate maps with :energy, :wavefunction, :boundary-value, :nodes."
  [E-start E-end num-steps V-params l r-max h]
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
        E-step (/ (- E-end E-start) num-steps)]
    (for [i (range (inc num-steps))]
      (let [E (+ E-start (* i E-step))
            u (solve-bound-state-numerov E l v0 rad diff h r-max)
            u-end (bound-state-boundary-value u r-max h)
            nodes (count-nodes u)]
        {:energy E
         :wavefunction u
         :boundary-value u-end
         :nodes nodes}))))

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
  (let [;; Scan all energies in range
        all-candidates (scan-energy-range E-start E-end num-steps V-params l r-max h)
        ;; Filter to only negative energies (bound states)
        negative-candidates (filter (fn [c] (< (:energy c) -0.01)) all-candidates)
        ;; Find candidates with exact node count
        candidates-with-nodes (filter (fn [c] (= (:nodes c) target-nodes)) negative-candidates)
        ;; Find candidates with small boundary value (< 10.0) - these are likely true bound states
        candidates-small-boundary (filter (fn [c] (< (Math/abs (:boundary-value c)) 10.0)) negative-candidates)
        ;; Debug: print summary if no candidates with correct nodes found
        _ (when (and (zero? target-nodes) (empty? candidates-with-nodes))
            (println (format "  Debug: No candidates with %d nodes found in range [%.2f, %.2f] MeV"
                           target-nodes E-start E-end))
            (println (format "  Debug: Found %d negative candidates, %d with small boundary value"
                           (count negative-candidates) (count candidates-small-boundary)))
            (when (seq negative-candidates)
              (let [node-distribution (frequencies (map :nodes negative-candidates))
                    best-by-boundary (apply min-key (fn [c] (Math/abs (:boundary-value c))) negative-candidates)]
                (println (format "  Debug: Node distribution: %s" node-distribution))
                (println (format "  Debug: Best candidate: E=%.2f MeV, nodes=%d, boundary=%.2e"
                               (:energy best-by-boundary) (:nodes best-by-boundary)
                               (:boundary-value best-by-boundary))))))
        ;; Find sign changes - these indicate where boundary value crosses zero (true bound states)
        sign-change-candidates (for [i (range (dec (count negative-candidates)))]
                                (let [curr (nth negative-candidates i)
                                      next (nth negative-candidates (inc i))]
                                  (when (not= (Math/signum (:boundary-value curr))
                                             (Math/signum (:boundary-value next)))
                                    ;; Return the candidate closer to zero crossing
                                    (if (< (Math/abs (:boundary-value curr))
                                          (Math/abs (:boundary-value next)))
                                      curr
                                      next))))
        sign-change-candidates (filter some? sign-change-candidates)
        
        ;; Select best candidate IGNORING node count - only look at boundary value:
        ;; 1. Sign changes - strongest indicator (where boundary crosses zero)
        ;; 2. Small boundary values - likely true bound states
        ;; 3. Minimum boundary value overall
        best (cond
               ;; Priority 1: Sign changes - these are where boundary value crosses zero
               (seq sign-change-candidates)
               (apply min-key (fn [c] (Math/abs (:boundary-value c))) sign-change-candidates)
               
               ;; Priority 2: Small boundary values - likely true bound states
               (seq candidates-small-boundary)
               (apply min-key (fn [c] (Math/abs (:boundary-value c))) candidates-small-boundary)
               
               ;; Priority 3: Minimum boundary value overall
               (seq negative-candidates)
               (apply min-key (fn [c] (Math/abs (:boundary-value c))) negative-candidates)
               
               ;; Fallback: No valid candidates found
               :else
               {:energy -1.0
                :wavefunction []
                :boundary-value 1e10
                :nodes -1
                :converged? false})]
    ;; Coarse search just finds candidates - don't check convergence here
    ;; Convergence is checked after refinement
    (assoc best :converged? false)))

(defn get-refinement-energy-range [E-guess v0 boundary-value]
  "Calculate energy range for refinement search.
   
   Parameters:
   - E-guess: Initial energy guess
   - v0: Potential depth
   - boundary-value: Current boundary value (if large, use wider range)
   
   Returns: [E-lo E-hi]
   
   Uses a narrower range to avoid jumping to different quantum states,
   but expands if boundary value is large (not a true bound state).
   For very large boundary values, search a much wider range to find sign changes."
  (let [;; Use much wider range if boundary value is very large (not a true bound state)
        ;; This helps find sign changes that might be far from the coarse candidate
        E-range (cond
                  (> (Math/abs boundary-value) 1e10)  ; Very large boundary value
                  (* v0 0.5)  ; Search ±50% of V0 to find sign changes
                  
                  (> (Math/abs boundary-value) 100.0)  ; Large boundary value
                  (min 50.0 (* v0 0.3))  ; Wider range: ±50 MeV or 30% of V0
                  
                  :else
                  (min 10.0 (* v0 0.2)))  ; Narrower range: ±10 MeV or 20% of V0
        E-lo (max (- E-guess E-range) (- v0))
        E-hi (min (+ E-guess E-range) -0.1)]
    [E-lo E-hi]))

(defn create-boundary-value-function [V-params l r-max h]
  "Create function f(E) = u(r_max) for root finding."
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)]
    (fn [E]
      (bound-state-boundary-value 
       (solve-bound-state-numerov E l v0 rad diff h r-max) r-max h))))

(defn validate-secant-root [root E-guess E-lo E-hi v0]
  "Validate and clamp secant root to valid range.
   
   Returns: validated root or nil if invalid"
  (let [max-deviation 20.0
        E-min-allowed (max E-lo (- E-guess max-deviation))
        E-max-allowed (min E-hi (+ E-guess max-deviation))
        clamped (max E-min-allowed (min E-max-allowed (min root -0.1)))]
    (when (and (< clamped (* -1.0 v0 0.05))  ; Must be at least 5% of well depth
               (< clamped -0.01)
               (> clamped (- v0)))
      clamped)))

(defn find-sign-change-range [f E-lo E-hi search-points]
  "Find energy range where boundary value changes sign.
   
   Returns: [E-lo E-hi] of sign change range, or nil if none found"
  (loop [i 0
         found-range nil]
    (if (or (>= i search-points) found-range)
      found-range
      (let [E-test (+ E-lo (* i (/ (- E-hi E-lo) (dec search-points))))
            u-test-val (f E-test)
            E-next (if (< i (dec search-points))
                     (+ E-lo (* (inc i) (/ (- E-hi E-lo) (dec search-points))))
                     E-hi)
            u-next-val (f E-next)]
        (if (not= (m/signum u-test-val) (m/signum u-next-val))
          (recur (inc i) [E-test E-next])
          (recur (inc i) found-range))))))

(defn create-refined-result [E-root target-nodes V-params l r-max h]
  "Create result map from refined energy."
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
        u-final (solve-bound-state-numerov E-root l v0 rad diff h r-max)
        u-final-val (bound-state-boundary-value u-final r-max h)
        nodes (count-nodes u-final)]
    {:energy E-root
     :wavefunction u-final
     :boundary-value u-final-val
     :nodes nodes
     :converged? (and (= nodes target-nodes)
                     (< (Math/abs u-final-val) 10.0))}))  ; Increased from 0.1 to 10.0

(defn refine-with-secant [f E-guess E-lo E-hi u-lo-val u-mid-val u-hi-val v0 tolerance target-nodes V-params l r-max h]
  "Try to refine using secant method.
   
   Returns: result map or nil if secant fails.
   IGNORES node count - only checks if boundary value is small."
  (let [E-secant-0 (if (< (Math/abs u-mid-val) (Math/abs u-lo-val)) E-guess E-lo)
        E-secant-1 (if (< (Math/abs u-hi-val) (Math/abs u-mid-val)) E-hi E-guess)
        secant-result (secant f E-secant-0 E-secant-1 tolerance 50)
        E-secant-root (when (:converged? secant-result)
                       (validate-secant-root (:root secant-result) E-guess E-lo E-hi v0))
        result (when (and (:converged? secant-result) E-secant-root)
                 (create-refined-result E-secant-root target-nodes V-params l r-max h))]
    ;; Return result if boundary value is reasonable (ignore node count)
    (when (and result (< (Math/abs (:boundary-value result)) 1e6))
      result)))

(defn refine-with-bisection [f E-lo E-hi tolerance target-nodes V-params l r-max h]
  "Try to refine using bisection method.
   
   Returns: result map or nil if bisection fails.
   IGNORES node count - only checks if boundary value is small."
  (let [sign-change-range (find-sign-change-range f E-lo E-hi 20)
        [E-bisect-lo E-bisect-hi] (or sign-change-range [E-lo E-hi])
        bisection-result (bisection f E-bisect-lo E-bisect-hi tolerance 100)
        result (when (:converged? bisection-result)
                 (create-refined-result (:root bisection-result) target-nodes V-params l r-max h))]
    ;; Return result if boundary value is reasonable (ignore node count)
    (when (and result (< (Math/abs (:boundary-value result)) 1e6))
      result)))

(defn refine-with-grid-search [E-lo E-hi target-nodes V-params l r-max h]
  "Fallback: find minimum boundary value using grid search.
   
   Returns: result map with smallest boundary value, or nil if none found.
   IGNORES node count - only looks for minimum boundary value."
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
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
        ;; Find candidate with smallest boundary value (ignore node count)
        best (when (seq candidates)
               (apply min-key :boundary-abs candidates))]
    (when (and best (< (:boundary-abs best) 1e6))
      {:energy (:energy best)
       :wavefunction (:wavefunction best)
       :boundary-value (:boundary-value best)
       :nodes (:nodes best)
       :converged? (< (Math/abs (:boundary-value best)) 10.0)})))

(defn has-sign-change? [u-lo-val u-mid-val u-hi-val]
  "Check if boundary values have opposite signs."
  (or (not= (m/signum u-lo-val) (m/signum u-hi-val))
      (not= (m/signum u-lo-val) (m/signum u-mid-val))
      (not= (m/signum u-mid-val) (m/signum u-hi-val))))

(defn refine-bound-state-energy
  "Refine bound state energy using secant method (or bisection as fallback).
   
   Parameters:
   - E-guess: Initial energy guess
   - target-nodes: Desired number of nodes
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - l: Orbital angular momentum
   - r-max: Maximum radius
   - h: Step size
   - tolerance: Energy convergence tolerance
   
   Returns: {:energy E, :wavefunction u, :boundary-value u-end, :nodes n, :converged? bool}
   or nil if no valid refinement found with correct node count.
   
   Uses secant method first (faster for smooth functions), falls back to bisection
   if secant doesn't converge or if boundary values have opposite signs."
  [E-guess target-nodes V-params l r-max h tolerance]
  (let [v0 (first V-params)
        rad (second V-params)
        diff (last V-params)
        ;; Get initial boundary value to determine refinement range
        u-guess (solve-bound-state-numerov E-guess l v0 rad diff h r-max)
        u-guess-val (bound-state-boundary-value u-guess r-max h)
        [E-lo E-hi] (get-refinement-energy-range E-guess v0 u-guess-val)
        f (create-boundary-value-function V-params l r-max h)
        u-lo (solve-bound-state-numerov E-lo l v0 rad diff h r-max)
        u-hi (solve-bound-state-numerov E-hi l v0 rad diff h r-max)
        u-mid (solve-bound-state-numerov E-guess l v0 rad diff h r-max)
        u-lo-val (bound-state-boundary-value u-lo r-max h)
        u-hi-val (bound-state-boundary-value u-hi r-max h)
        u-mid-val (bound-state-boundary-value u-mid r-max h)
        secant-result (refine-with-secant f E-guess E-lo E-hi u-lo-val u-mid-val u-hi-val v0 tolerance target-nodes V-params l r-max h)
        bisection-result (when (and (not secant-result) (has-sign-change? u-lo-val u-mid-val u-hi-val))
                          (refine-with-bisection f E-lo E-hi tolerance target-nodes V-params l r-max h))
        grid-result (when (and (not secant-result) (not bisection-result))
                     (refine-with-grid-search E-lo E-hi target-nodes V-params l r-max h))
        result (or secant-result bisection-result grid-result)]
    ;; Return result if boundary value is reasonable (ignore node count)
    (when (and result (< (Math/abs (:boundary-value result)) 1e6))
      result)))

(defn get-energy-search-range [n l v0]
  "Calculate energy search range for principal quantum number n and orbital angular momentum l.
   
   For bound states, we need to search from deep in the well (near -V0) up to near zero.
   Higher l states can be either deeper or shallower depending on the potential.
   
   Returns: [E-min E-max] in MeV"
  (let [;; For bound states, search from deep in well to near zero
        ;; Start deeper for higher n (more nodes) and account for l
        ;; For l > 0, we need to search a wider range since centrifugal barrier affects energy
        base-E-min (cond
                     (= n 1) (- (* v0 0.8))  ; Ground: search from 80% of V0
                     (= n 2) (- (* v0 0.6))  ; 2s/2p: search from 60% of V0
                     (= n 3) (- (* v0 0.4))  ; 3s/3p/3d: search from 40% of V0
                     :else (- (* v0 (- 0.8 (* (- n 1) 0.15)))))  ; Higher n: progressively higher
        base-E-max (cond
                     (= n 1) (- (* v0 0.2))  ; Ground: up to 20% of V0
                     (= n 2) (- (* v0 0.15))  ; 2s/2p: up to 15% of V0
                     (= n 3) (- (* v0 0.1))  ; 3s/3p/3d: up to 10% of V0
                     :else (- (* v0 (- 0.2 (* (- n 1) 0.02)))))  ; Higher n: closer to zero
        ;; For l > 0, extend the search range deeper (l=1 states can be quite deep)
        ;; and also shallower (centrifugal barrier pushes some states up)
        l-deepening (* l v0 0.1)  ; Each l extends search deeper by 10% of V0
        E-min (max (+ base-E-min (- l-deepening)) (- v0))  ; Extend deeper (more negative), but not beyond well depth
        E-max (min base-E-max -0.1)]  ; Don't go above zero
    [E-min E-max]))

(defn try-wider-search [v0 expected-nodes V-params l r-max h tolerance]
  "Try a wider energy search range.
   
   Returns: best result found, or nil if nothing found"
  (let [;; Wider range: search from 80% to 10% of V0
        E-wide-min (- (* v0 0.8))
        ;; Adjust upper bound for l: higher l states are at higher energies
        base-E-wide-max (- (* v0 0.1))
        l-adjustment (* l v0 0.05)  ; Same adjustment as in get-energy-search-range
        E-wide-max (min (+ base-E-wide-max l-adjustment) -0.1)
        wide-result (find-energy-with-nodes E-wide-min E-wide-max 200 expected-nodes 
                                            V-params l r-max h)]
    (if (< (:energy wide-result) -0.01)
      ;; Only refine if boundary value is reasonable (not huge) - IGNORE node count
      (let [refined (refine-bound-state-energy (:energy wide-result) expected-nodes V-params l r-max h tolerance)]
        ;; Only return if refined result has reasonable boundary value
        (when (and refined
                   (< (Math/abs (:boundary-value refined)) 1e6))  ; Reject huge boundary values
          refined))
      nil)))

(defn valid-energy? [E E-min E-max]
  "Check if energy is valid (negative and within range)."
  (and (< E -0.01)
       (>= E E-min)
       (<= E E-max)))

(defn refinement-improved? [refined coarse-result coarse-boundary expected-nodes]
  "Check if refinement significantly improved the result.
   
   IGNORES node count - only checks if boundary value improved."
  (let [refined-boundary (Math/abs (:boundary-value refined))]
    (< refined-boundary coarse-boundary)))    ; Must improve boundary value

(defn print-refinement-debug [coarse-boundary refined-boundary coarse-result refined-nodes expected-nodes]
  "Print debug information about refinement."
  (when (> coarse-boundary 10.0)
    (println (format "  Refinement: coarse boundary=%.2e, refined boundary=%.2e, improvement=%.1f%%"
                   coarse-boundary refined-boundary 
                   (* 100.0 (/ (- coarse-boundary refined-boundary) coarse-boundary)))
    (println (format "  Coarse nodes=%d, refined nodes=%d, expected=%d"
                   (:nodes coarse-result) refined-nodes expected-nodes)))))

(defn try-refinement-with-wide-search [E-guess expected-nodes coarse-result coarse-boundary
                                       E-search-min E-search-max v0 V-params l r-max h tolerance]
  "Try to refine the energy, with fallback to wider search if needed.
   
   Returns: refined result if boundary value improved (ignores node count)"
  (let [refined (refine-bound-state-energy E-guess expected-nodes V-params l r-max h tolerance)]
    (if refined
      (let [refined-boundary (Math/abs (:boundary-value refined))
            refined-energy (:energy refined)
            refined-nodes (:nodes refined)]
        (print-refinement-debug coarse-boundary refined-boundary coarse-result refined-nodes expected-nodes)
        ;; Accept refinement if it improves boundary value (ignore node count and energy range)
        ;; The energy might be outside the original search range but still valid
        (if (refinement-improved? refined coarse-result coarse-boundary expected-nodes)
          refined
          ;; If refinement doesn't improve, try wider search or return the better one
          (let [wide-refined (try-wider-search v0 expected-nodes V-params l r-max h tolerance)]
            (if (and wide-refined
                     (< (Math/abs (:boundary-value wide-refined)) coarse-boundary))
              wide-refined
              ;; Use the one with smaller boundary value (refined or coarse)
              (if (< refined-boundary coarse-boundary)
                refined
                coarse-result))))
      ;; If refinement returned nil, use coarse result
      coarse-result)))
)

(defn should-refine? [coarse-result expected-nodes]
  "Check if refinement should be attempted.
   IGNORES node count - only checks boundary value."
  (> (Math/abs (:boundary-value coarse-result)) 1.0))

(defn handle-invalid-energy [E-guess E-search-min E-search-max v0 expected-nodes V-params l r-max h tolerance coarse-result]
  "Handle case when coarse scan finds invalid energy."
  (println (format "Warning: Coarse scan found invalid energy %.6f MeV (expected range: [%.2f, %.2f] MeV)"
                 E-guess E-search-min E-search-max))
  (or (try-wider-search v0 expected-nodes V-params l r-max h tolerance)
      coarse-result))

(defn handle-wrong-nodes [v0 expected-nodes V-params l r-max h tolerance coarse-result]
  "Handle case when coarse scan finds wrong number of nodes."
  (or (try-wider-search v0 expected-nodes V-params l r-max h tolerance)
      (do
        (println (format "Warning: Could not find state with %d nodes. Found %d nodes at E=%.2f MeV"
                       expected-nodes (:nodes coarse-result) (:energy coarse-result)))
        coarse-result)))

(defn find-bound-state-energy
  "Find bound state energy using shooting method.
   
   Parameters:
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - l: Orbital angular momentum
   - n: Number of radial nodes (for nuclear potentials, not principal quantum number)
   - r-max: Maximum radius for integration
   - h: Step size
   - E-min: Minimum energy to search (default: -V0, the potential depth)
   - E-max: Maximum energy to search (default: -0.1 MeV, just below zero)
   - tolerance: Energy convergence tolerance (default: 0.01 MeV)
   
   Returns: {:energy E, :wavefunction u, :nodes n-nodes, :converged? bool}
   
   Algorithm:
   1. Coarse scan to find approximate energy with n radial nodes
   2. Refine using secant/bisection around best candidate
   3. For different n values, searches in different energy ranges
       because bound states are ordered: E(n=0) < E(n=1) < E(n=2) < ..."
  ([V-params l n r-max h]
   (let [v0 (first V-params)]
     (find-bound-state-energy V-params l n r-max h (- v0) -0.1 0.01)))
  ([V-params l n r-max h E-min E-max tolerance]
   (let [v0 (first V-params)
         expected-nodes n  ; For nuclear potentials, n is the number of radial nodes
         [E-search-min E-search-max] (get-energy-search-range n l v0)
         coarse-result (find-energy-with-nodes E-search-min E-search-max 150 expected-nodes 
                                               V-params l r-max h)
         E-guess (:energy coarse-result)
         coarse-boundary (Math/abs (:boundary-value coarse-result))
         E-guess-valid (valid-energy? E-guess E-search-min E-search-max)
         ;; Check if energy is at boundary of search range (might need wider search)
         at-boundary (or (< (Math/abs (- E-guess E-search-min)) 0.1)
                        (< (Math/abs (- E-guess E-search-max)) 0.1))
         ;; If boundary value is huge, it's definitely not a bound state
         huge-boundary (> coarse-boundary 1e10)]
     (cond
       (not E-guess-valid)
       (handle-invalid-energy E-guess E-search-min E-search-max v0 expected-nodes 
                             V-params l r-max h tolerance coarse-result)
       
       ;; If at boundary or huge boundary value, try wider search first
       (or at-boundary huge-boundary)
       (if-let [wide-result (try-wider-search v0 expected-nodes V-params l r-max h tolerance)]
         wide-result
         ;; If wider search fails, still try refinement
         (if (should-refine? coarse-result expected-nodes)
           (try-refinement-with-wide-search E-guess expected-nodes coarse-result coarse-boundary
                                            E-search-min E-search-max v0 V-params l r-max h tolerance)
           coarse-result))
       
       (should-refine? coarse-result expected-nodes)
       (try-refinement-with-wide-search E-guess expected-nodes coarse-result coarse-boundary
                                        E-search-min E-search-max v0 V-params l r-max h tolerance)
       
       :else
       (handle-wrong-nodes v0 expected-nodes V-params l r-max h tolerance coarse-result)))))

(defn normalize-bound-state [u h]
  "Normalize bound state wavefunction so that ∫₀^∞ |u(r)|² dr = 1.
   
   Uses Simpson's rule for integration: ∫ f(r) dr ≈ (h/3) * [f₀ + fₙ + 4∑f_odd + 2∑f_even]
   
   Parameters:
   - u: Wavefunction vector
   - h: Step size
   
   Returns: Normalized wavefunction vector"
  (when (or (nil? u) (empty? u))
    (throw (IllegalArgumentException. 
            (format "Cannot normalize empty or nil wavefunction. Wavefunction: %s" u))))
  (let [;; Calculate normalization integral: N² = ∫ u²(r) r² dr
        ;; For radial wavefunctions, normalization is ∫ u²(r) dr (not r²)
        ;; But in some conventions it's ∫ u²(r) r² dr - we'll use ∫ u²(r) dr
        integrand (mapv #(* % %) u)
        n (count integrand)]
    (when (< n 2)
      (throw (IllegalArgumentException. 
              (format "Wavefunction too short for normalization: %d points (need at least 2)" n))))
    (let [;; Simpson's rule
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
      (mapv #(* % norm-factor) u))))

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
   ;; For nuclear potentials, n represents the number of radial nodes (not principal quantum number)
   ;; No constraint like n > l applies - nuclear bound states are labeled by radial nodes
   (let [result (find-bound-state-energy V-params l n r-max h)
         wavefunction (:wavefunction result)]
     (when (or (nil? wavefunction) (empty? wavefunction))
       (throw (IllegalArgumentException. 
               (format "Failed to find bound state for n=%d, l=%d. Result: %s" 
                      n l (pr-str (select-keys result [:energy :nodes :boundary-value]))))))
     (let [u-norm (normalize-bound-state wavefunction h)]
       {:energy (:energy result)
        :wavefunction (:wavefunction result)
        :normalized-wavefunction u-norm
        :nodes (:nodes result)
        :boundary-value (:boundary-value result)
        :converged? (:converged? result)
        :quantum-numbers {:n n, :l l, :j j}
        :r-max r-max
        :h h}))))

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
