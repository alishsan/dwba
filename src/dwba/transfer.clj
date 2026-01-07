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
        ;; Select best candidate using priority:
        ;; 1. Candidates with correct node count AND small boundary value (< 100)
        ;; 2. Candidates with small boundary value (< 10.0) - true bound states
        ;; 3. Candidates with correct node count (even if boundary value is large)
        ;; 4. Best overall (weighted score)
        best (cond
               ;; Priority 1: Candidates with exact node count AND small boundary value
               (seq (filter (fn [c] (and (= (:nodes c) target-nodes)
                                        (< (Math/abs (:boundary-value c)) 100.0)))
                           candidates-with-nodes))
               (apply min-key (fn [c] (Math/abs (:boundary-value c)))
                      (filter (fn [c] (and (= (:nodes c) target-nodes)
                                          (< (Math/abs (:boundary-value c)) 100.0)))
                             candidates-with-nodes))
               
               ;; Priority 2: Candidates with small boundary value (< 10.0) - true bound states
               ;; This helps find true bound states even if node count is slightly off
               (seq candidates-small-boundary)
               (apply min-key (fn [c] (+ (* 5 (Math/abs (- (:nodes c) target-nodes)))
                                        (Math/abs (:boundary-value c))))
                      candidates-small-boundary)
               
               ;; Priority 3: Candidates with exact node count (even if boundary value is large)
               ;; These need refinement to find the true bound state
               ;; But prefer those with smaller boundary values
               (seq candidates-with-nodes)
               (let [best-by-boundary (apply min-key (fn [c] (Math/abs (:boundary-value c))) candidates-with-nodes)]
                 ;; Only use if boundary value is reasonable (< 1e5), otherwise it's likely not a bound state
                 (if (< (Math/abs (:boundary-value best-by-boundary)) 1e5)
                   best-by-boundary
                   ;; If all candidates have huge boundary values, still return the best one
                   ;; but it will need more aggressive refinement
                   best-by-boundary))
               
               ;; Priority 4: Best overall (weighted score)
               ;; Prefer candidates closer to target node count, but also consider boundary value
               (seq negative-candidates)
               (apply min-key (fn [c] (+ (* 10 (Math/abs (- (:nodes c) target-nodes)))
                                        (Math/abs (:boundary-value c))))
                      negative-candidates)
               
               ;; Fallback: No valid candidates found
               :else
               {:energy -1.0
                :wavefunction []
                :boundary-value 1e10
                :nodes -1
                :converged? false})]
    (assoc best :converged? (and (= (:nodes best) target-nodes)
                                (< (Math/abs (:boundary-value best)) 0.1)))))

(defn get-refinement-energy-range [E-guess v0 boundary-value]
  "Calculate energy range for refinement search.
   
   Parameters:
   - E-guess: Initial energy guess
   - v0: Potential depth
   - boundary-value: Current boundary value (if large, use wider range)
   
   Returns: [E-lo E-hi]
   
   Uses a narrower range to avoid jumping to different quantum states,
   but expands if boundary value is large (not a true bound state)."
  (let [;; Use wider range if boundary value is large (not a true bound state)
        E-range (if (> (Math/abs boundary-value) 100.0)
                  (min 20.0 (* v0 0.4))  ; Wider range: ±20 MeV or 40% of V0
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
                     (< (Math/abs u-final-val) 0.1))}))

(defn refine-with-secant [f E-guess E-lo E-hi u-lo-val u-mid-val u-hi-val v0 tolerance target-nodes V-params l r-max h]
  "Try to refine using secant method.
   
   Returns: result map or nil if secant fails or wrong node count"
  (let [E-secant-0 (if (< (Math/abs u-mid-val) (Math/abs u-lo-val)) E-guess E-lo)
        E-secant-1 (if (< (Math/abs u-hi-val) (Math/abs u-mid-val)) E-hi E-guess)
        secant-result (secant f E-secant-0 E-secant-1 tolerance 50)
        E-secant-root (when (:converged? secant-result)
                       (validate-secant-root (:root secant-result) E-guess E-lo E-hi v0))
        result (when (and (:converged? secant-result) E-secant-root)
                 (create-refined-result E-secant-root target-nodes V-params l r-max h))]
    ;; Only return if node count matches
    (when (and result (= (:nodes result) target-nodes))
      result)))

(defn refine-with-bisection [f E-lo E-hi tolerance target-nodes V-params l r-max h]
  "Try to refine using bisection method.
   
   Returns: result map or nil if bisection fails or wrong node count"
  (let [sign-change-range (find-sign-change-range f E-lo E-hi 20)
        [E-bisect-lo E-bisect-hi] (or sign-change-range [E-lo E-hi])
        bisection-result (bisection f E-bisect-lo E-bisect-hi tolerance 100)
        result (when (:converged? bisection-result)
                 (create-refined-result (:root bisection-result) target-nodes V-params l r-max h))]
    ;; Only return if node count matches
    (when (and result (= (:nodes result) target-nodes))
      result)))

(defn refine-with-grid-search [E-lo E-hi target-nodes V-params l r-max h]
  "Fallback: find minimum boundary value using grid search.
   
   Returns: result map with correct node count, or nil if none found"
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
        ;; Filter to candidates with correct node count only
        candidates-with-nodes (filter (fn [c] (= (:nodes c) target-nodes)) candidates)]
    (when (seq candidates-with-nodes)
      ;; If we have candidates with correct nodes, pick the one with smallest boundary value
      (let [best (apply min-key :boundary-abs candidates-with-nodes)]
        {:energy (:energy best)
         :wavefunction (:wavefunction best)
         :boundary-value (:boundary-value best)
         :nodes (:nodes best)
         :converged? (and (= (:nodes best) target-nodes)
                         (< (Math/abs (:boundary-value best)) 0.1))}))))

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
    ;; Only return result if it has the correct node count
    (when (and result (= (:nodes result) target-nodes))
      result)))

(defn get-energy-search-range [n v0]
  "Calculate energy search range for principal quantum number n.
   
   Returns: [E-min E-max] in MeV"
  [(cond
     (= n 1) (- (* v0 0.7))  ; Ground: search from -35 MeV (70% of V0)
     (= n 2) (- (* v0 0.5))  ; 2s: above ground, around -25 MeV
     (= n 3) (- (* v0 0.3))  ; 3s: above 2s, around -15 MeV
     :else (- (* v0 (- 0.7 (* (- n 1) 0.15)))))  ; Higher n: progressively higher
   (cond
     (= n 1) (- (* v0 0.3))  ; Ground: up to -15 MeV (30% of V0)
     (= n 2) (- (* v0 0.2))  ; 2s: up to -10 MeV
     (= n 3) (- (* v0 0.1))  ; 3s: up to -5 MeV
     :else (- (* v0 (- 0.3 (* (- n 1) 0.05)))))])  ; Higher n: closer to zero

(defn try-wider-search [v0 expected-nodes V-params l r-max h tolerance]
  "Try a wider energy search range.
   
   Returns: best result found, or nil if nothing found"
  (let [E-wide-min (- (* v0 0.8))  ; Wider range: -40 MeV
        E-wide-max (- (* v0 0.1))   ; Up to -5 MeV
        wide-result (find-energy-with-nodes E-wide-min E-wide-max 200 expected-nodes 
                                            V-params l r-max h)]
    (if (and (= (:nodes wide-result) expected-nodes)
             (< (:energy wide-result) -0.01))
      (refine-bound-state-energy (:energy wide-result) expected-nodes V-params l r-max h tolerance)
      nil)))

(defn valid-energy? [E E-min E-max]
  "Check if energy is valid (negative and within range)."
  (and (< E -0.01)
       (>= E E-min)
       (<= E E-max)))

(defn refinement-improved? [refined coarse-result coarse-boundary expected-nodes]
  "Check if refinement significantly improved the result.
   
   Returns true only if:
   1. Boundary value improved (smaller), AND
   2. Node count is correct"
  (let [refined-boundary (Math/abs (:boundary-value refined))
        refined-nodes (:nodes refined)]
    (and (= refined-nodes expected-nodes)          ; Must have correct node count
         (< refined-boundary coarse-boundary))))    ; Must improve boundary value

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
   
   Returns: refined result, but only if node count is correct"
  (let [refined (refine-bound-state-energy E-guess expected-nodes V-params l r-max h tolerance)]
    (if refined
      (let [refined-boundary (Math/abs (:boundary-value refined))
            refined-energy (:energy refined)
            refined-nodes (:nodes refined)]
        (print-refinement-debug coarse-boundary refined-boundary coarse-result refined-nodes expected-nodes)
        ;; Only accept refinement if it actually improves the result
        (if (and (= refined-nodes expected-nodes)
                 (valid-energy? refined-energy E-search-min E-search-max)
                 (refinement-improved? refined coarse-result coarse-boundary expected-nodes))
          refined
          ;; If refinement doesn't improve or has wrong nodes, try wider search or return coarse result
          (let [wide-refined (try-wider-search v0 expected-nodes V-params l r-max h tolerance)]
            (if (and wide-refined
                     (= (:nodes wide-refined) expected-nodes)
                     (< (Math/abs (:boundary-value wide-refined)) coarse-boundary))
              wide-refined
              ;; Prefer coarse result if it has correct nodes and refinement made it worse
              (if (and (= (:nodes coarse-result) expected-nodes)
                       (< refined-boundary coarse-boundary))
                ;; Refinement made it worse, use coarse result
                coarse-result
                ;; Both are bad, but use the one with smaller boundary value
                (if (< refined-boundary coarse-boundary)
                  refined
                  coarse-result)))))
      ;; If refinement returned nil (no valid result), use coarse result if it has correct nodes
      (if (= (:nodes coarse-result) expected-nodes)
        coarse-result
        ;; Last resort: try wider search
        (try-wider-search v0 expected-nodes V-params l r-max h tolerance)))))
)

(defn should-refine? [coarse-result expected-nodes]
  "Check if refinement should be attempted."
  (or (= (:nodes coarse-result) expected-nodes)
      (> (Math/abs (:boundary-value coarse-result)) 1.0)))

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
   - n: Principal quantum number (1, 2, 3, ...)
   - r-max: Maximum radius for integration
   - h: Step size
   - E-min: Minimum energy to search (default: -V0, the potential depth)
   - E-max: Maximum energy to search (default: -0.1 MeV, just below zero)
   - tolerance: Energy convergence tolerance (default: 0.01 MeV)
   
   Returns: {:energy E, :wavefunction u, :nodes n-nodes, :converged? bool}
   
   Algorithm:
   1. Coarse scan to find approximate energy with correct nodes
   2. Refine using secant/bisection around best candidate
   3. For different n values, searches in different energy ranges
       because bound states are ordered: E(n=1) < E(n=2) < E(n=3) < ..."
  ([V-params l n r-max h]
   (let [v0 (first V-params)]
     (find-bound-state-energy V-params l n r-max h (- v0) -0.1 0.01)))
  ([V-params l n r-max h E-min E-max tolerance]
   (let [v0 (first V-params)
         expected-nodes (- n 1)
         [E-search-min E-search-max] (get-energy-search-range n v0)
         coarse-result (find-energy-with-nodes E-search-min E-search-max 150 expected-nodes 
                                               V-params l r-max h)
         E-guess (:energy coarse-result)
         coarse-boundary (Math/abs (:boundary-value coarse-result))
         E-guess-valid (valid-energy? E-guess E-search-min E-search-max)]
     (cond
       (not E-guess-valid)
       (handle-invalid-energy E-guess E-search-min E-search-max v0 expected-nodes 
                             V-params l r-max h tolerance coarse-result)
       
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
