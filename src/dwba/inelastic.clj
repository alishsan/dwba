(ns dwba.inelastic
  "DWBA calculations for inelastic scattering reactions.
   
   Inelastic scattering involves the excitation of the target nucleus to excited states.
   Unlike elastic scattering where the nucleus remains in its ground state, inelastic
   scattering populates excited states through the interaction.
   
   Key features:
   - Coupled channels calculations
   - Transition form factors (deformation parameters)
   - Collective excitations (rotational, vibrational)
   - Single-particle excitations
   - Reduced matrix elements and B(Eλ) values
   
   This namespace implements the DWBA formalism for inelastic scattering.
   See INELASTIC_SCATTERING_PLAN.md for the detailed implementation plan."
  (:require [functions :refer [WS solve-numerov]]
            [fastmath.core :as m]
            [fastmath.polynomials :as poly]
            [complex :refer [re im mag complex-cartesian complex-polar add mul]]
            [dwba.transfer :as transfer]))

;; ============================================================================
;; PHASE 1: TRANSITION FORM FACTORS
;; ============================================================================

(defn deformation-parameter
  "Get deformation parameter β_λ for specific nucleus and multipole order.
   
   Parameters:
   - lambda: Multipole order (2 = quadrupole, 3 = octupole, 4 = hexadecapole, ...)
   - nucleus-type: Keyword or string identifying the nucleus
     Examples: :C12, :Pb208, :Sm154, or \"12C\", \"208Pb\", \"154Sm\"
   
   Returns: Deformation parameter β_λ (dimensionless)
   
   Typical values:
   - Spherical nuclei (e.g., ²⁰⁸Pb): β_2 ≈ 0.05-0.1
   - Light nuclei (e.g., ¹²C): β_2 ≈ 0.2-0.3
   - Deformed nuclei (e.g., ¹⁵⁴Sm): β_2 ≈ 0.3-0.4
   - Strongly deformed: β_2 > 0.4
   
   Note: This is a database of known values. For unknown nuclei, returns
   a default value based on typical ranges. Users can override with explicit values.
   
   Example:
   (deformation-parameter 2 :C12)  ; Returns β_2 for ¹²C
   (deformation-parameter 2 :Pb208) ; Returns β_2 for ²⁰⁸Pb"
  [lambda nucleus-type]
  (let [nucleus-key (if (keyword? nucleus-type) 
                     nucleus-type 
                     (keyword (str nucleus-type)))
        ;; Database of deformation parameters (β_2 values only)
        ;; For other multipole orders, use default values
        beta-database {:C12 0.25    ; ¹²C: β_2 ≈ 0.2-0.3
                       :O16 0.2     ; ¹⁶O: β_2 ≈ 0.2
                       :Ca40 0.1    ; ⁴⁰Ca: β_2 ≈ 0.1
                       :Pb208 0.06  ; ²⁰⁸Pb: β_2 ≈ 0.05-0.1
                       :Sn120 0.1   ; ¹²⁰Sn: β_2 ≈ 0.1
                       :Sm154 0.35  ; ¹⁵⁴Sm: β_2 ≈ 0.3-0.4
                       :Gd158 0.32} ; ¹⁵⁸Gd: β_2 ≈ 0.3
        beta-val (get beta-database nucleus-key)]
    ;; Only use database value for λ=2 (quadrupole)
    ;; For other multipole orders, use default values
    (if (and beta-val (= lambda 2))
      beta-val
      ;; Default values based on multipole order
      (cond
        (= lambda 2) 0.2   ; Default β_2 for quadrupole
        (= lambda 3) 0.1   ; Default β_3 for octupole
        (= lambda 4) 0.05  ; Default β_4 for hexadecapole
        :else 0.1))))     ; Default for other multipoles

(defn woods-saxon-derivative
  "Calculate derivative of Woods-Saxon potential: dV/dr.
   
   For Woods-Saxon potential:
   V(r) = -V0 / (1 + exp((r - R0)/a0))
   
   The derivative is:
   dV/dr = (V0/a0) · exp((r - R0)/a0) / [1 + exp((r - R0)/a0)]²
   
   Parameters:
   - r: Radial distance (fm)
   - V-params: Woods-Saxon parameters [V0, R0, a0]
     - V0: Potential depth (MeV)
     - R0: Radius parameter (fm)
     - a0: Diffuseness parameter (fm)
   
   Returns: dV/dr (MeV/fm)
   
   Example:
   (woods-saxon-derivative 3.0 [50.0 2.0 0.6])  ; dV/dr at r=3 fm"
  [r [V0 R0 a0]]
  (let [exp-arg (/ (- r R0) a0)
        exp-val (Math/exp exp-arg)
        denominator (+ 1.0 exp-val)
        denominator-squared (* denominator denominator)]
    (/ (* V0 exp-val) (* a0 denominator-squared))))

(defn transition-form-factor
  "Calculate transition form factor for inelastic scattering.
   
   The transition form factor is:
   F_λ(r) = β_λ · R_0 · dV/dr
   
   where:
   - β_λ is the deformation parameter
   - R_0 is the nuclear radius
   - dV/dr is the derivative of the Woods-Saxon potential
   
   Parameters:
   - r: Radial distance (fm)
   - lambda: Multipole order (2, 3, 4, ...)
   - beta: Deformation parameter β_λ (can be nil to look up from nucleus-type)
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - nucleus-type: (Optional) Nucleus identifier for looking up β_λ
   
   Returns: F_λ(r) (MeV/fm)
   
   Example:
   (transition-form-factor 3.0 2 0.25 [50.0 2.0 0.6])
   (transition-form-factor 3.0 2 nil [50.0 2.0 0.6] :C12)  ; Uses β_2 from database"
  ([r lambda beta V-params]
   (let [[_V0 R0 _a0] V-params
         dV-dr (woods-saxon-derivative r V-params)]
     (* beta R0 dV-dr)))
  ([r lambda beta V-params nucleus-type]
   (let [beta-val (if (nil? beta)
                   (deformation-parameter lambda nucleus-type)
                   beta)]
     (transition-form-factor r lambda beta-val V-params))))

(defn transition-form-factor-function
  "Calculate transition form factor as a function of radial distance.
   
   Returns a vector of F_λ(r) values at each radial point.
   
   Parameters:
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   - lambda: Multipole order
   - beta: Deformation parameter
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - nucleus-type: (Optional) Nucleus identifier
   
   Returns: Vector of F_λ(r) values
   
   Example:
   (transition-form-factor-function 20.0 0.01 2 0.25 [50.0 2.0 0.6])"
  ([r-max h lambda beta V-params]
   (let [steps (int (/ r-max h))
         r-values (map #(* % h) (range (inc steps)))]
     (mapv (fn [r] (transition-form-factor r lambda beta V-params)) r-values)))
  ([r-max h lambda beta V-params nucleus-type]
   (let [beta-val (if (nil? beta)
                   (deformation-parameter lambda nucleus-type)
                   beta)]
     (transition-form-factor-function r-max h lambda beta-val V-params))))

(defn reduced-matrix-element
  "Calculate reduced matrix element for electromagnetic transition.
   
   The reduced matrix element <J_f||M(λ)||J_i> is related to the B(Eλ) value:
   B(Eλ; J_i → J_f) = |<J_f||M(λ)||J_i>|² / (2J_i + 1)
   
   For collective excitations, the reduced matrix element can be approximated as:
   <J_f||M(λ)||J_i> ≈ β_λ · R_0^λ · (3/(4π)) · Z · e
   
   Parameters:
   - lambda: Multipole order
   - J-i: Initial angular momentum (total J) - not used in simplified calculation
   - J-f: Final angular momentum (total J) - not used in simplified calculation
   - beta: Deformation parameter β_λ
   - R0: Nuclear radius (fm)
   - Z: Atomic number (for electric transitions)
   
   Returns: |<J_f||M(λ)||J_i>| (in units of e·fm^λ)
   
   Note: This is a simplified calculation. Full calculation requires
   detailed nuclear structure information.
   
   Example:
   (reduced-matrix-element 2 0 2 0.25 2.0 6)  ; 2⁺ transition in ¹²C"
  [lambda _J-i _J-f beta R0 Z]
  (let [;; Geometric factor: (3/(4π)) for electric transitions
        geometric-factor (/ 3.0 (* 4.0 Math/PI))
        ;; R0^λ factor
        R0-lambda (m/pow R0 lambda)
        ;; Reduced matrix element magnitude
        matrix-element (* beta R0-lambda geometric-factor Z)]
    matrix-element))

(defn B-Elambda
  "Calculate B(Eλ) value for electromagnetic transition.
   
   B(Eλ) = |<J_f||M(λ)||J_i>|² / (2J_i + 1)
   
   Parameters:
   - lambda: Multipole order
   - J-i: Initial angular momentum
   - J-f: Final angular momentum
   - beta: Deformation parameter β_λ
   - R0: Nuclear radius (fm)
   - Z: Atomic number
   
   Returns: B(Eλ) in units of e²·fm^(2λ)
   
   Example:
   (B-Elambda 2 0 2 0.25 2.0 6)  ; B(E2) for 2⁺ transition in ¹²C"
  [lambda J-i J-f beta R0 Z]
  (let [reduced-matrix (reduced-matrix-element lambda J-i J-f beta R0 Z)
        reduced-matrix-squared (* reduced-matrix reduced-matrix)
        J-i-factor (inc (* 2 J-i))]
    (/ reduced-matrix-squared J-i-factor)))

(defn transition-strength
  "Calculate transition strength from B(Eλ) value.
   
   The transition strength is related to the transition probability.
   
   Parameters:
   - B-Elambda-val: B(Eλ) value (from B-Elambda function)
   - E-ex: Excitation energy (MeV)
   - lambda: Multipole order
   
   Returns: Transition strength (dimensionless or in appropriate units)
   
   Note: The exact formula depends on the type of transition and conventions used."
  [B-Elambda-val E-ex lambda]
  (* B-Elambda-val (m/pow E-ex lambda)))

;; ============================================================================
;; PHASE 2: COUPLED CHANNELS FRAMEWORK
;; ============================================================================

(defn channel-definition
  "Define a channel for coupled channels calculation.
   
   A channel represents a nuclear state (ground state or excited state).
   
   Parameters:
   - channel-id: Integer identifier (0 = ground state, 1, 2, ... = excited states)
   - E-ex: Excitation energy (MeV) - 0.0 for ground state
   - L: Orbital angular momentum
   - J: Total angular momentum (optional, for spin-dependent calculations)
   - V-params: Woods-Saxon parameters [V0, R0, a0] for this channel
   
   Returns: Channel map with {:id, :E-ex, :L, :J, :V-params}
   
   Example:
   (channel-definition 0 0.0 0 nil [50.0 2.0 0.6])  ; Ground state
   (channel-definition 1 4.44 2 nil [50.0 2.0 0.6])  ; First 2⁺ excited state"
  [channel-id E-ex L J V-params]
  {:id channel-id
   :E-ex E-ex
   :L L
   :J J
   :V-params V-params})

(defn coupling-matrix-element
  "Calculate coupling matrix element between two channels.
   
   The coupling potential V_αβ(r) couples channel α to channel β.
   For inelastic scattering, this is typically:
   V_αβ(r) = F_λ(r) · coupling-strength
   
   where F_λ(r) is the transition form factor.
   
   Parameters:
   - r: Radial distance (fm)
   - channel-alpha: Source channel (map from channel-definition)
   - channel-beta: Target channel (map from channel-definition)
   - lambda: Multipole order of the transition
   - beta: Deformation parameter β_λ
   - coupling-strength: Additional coupling strength factor (default: 1.0)
   
   Returns: V_αβ(r) in MeV
   
   Example:
   (let [ch0 (channel-definition 0 0.0 0 nil [50.0 2.0 0.6])
         ch1 (channel-definition 1 4.44 2 nil [50.0 2.0 0.6])]
     (coupling-matrix-element 2.0 ch0 ch1 2 0.25))"
  [r channel-alpha _channel-beta lambda beta coupling-strength]
  (let [V-params-alpha (:V-params channel-alpha)
        F-lambda-r (transition-form-factor r lambda beta V-params-alpha)]
    (* coupling-strength F-lambda-r)))

(defn channel-energy
  "Calculate channel energy for a given incident energy.
   
   For inelastic scattering:
   - Ground state channel (α=0): E_0 = E (incident energy)
   - Excited state channel (α>0): E_α = E - E_ex (energy after excitation)
   
   Parameters:
   - E-incident: Incident energy (MeV)
   - channel: Channel definition (map from channel-definition)
   
   Returns: Channel energy E_α (MeV)
   
   Example:
   (let [ch0 (channel-definition 0 0.0 0 nil [50.0 2.0 0.6])
         ch1 (channel-definition 1 4.44 2 nil [50.0 2.0 0.6])]
     [(channel-energy 10.0 ch0)  ; Returns 10.0
      (channel-energy 10.0 ch1)]) ; Returns 5.56"
  [E-incident channel]
  (- E-incident (:E-ex channel)))

(defn coupled-channels-potential-matrix
  "Calculate potential matrix for coupled channels at radial distance r.
   
   The potential matrix V(r) has elements:
   - Diagonal: V_αα(r) = V_α(r) (channel potential)
   - Off-diagonal: V_αβ(r) (coupling between channels)
   
   Parameters:
   - r: Radial distance (fm)
   - channels: Vector of channel definitions
   - coupling-specs: Vector of coupling specifications
     Each spec: {:from channel-id, :to channel-id, :lambda, :beta, :strength}
   - E-incident: Incident energy (MeV) - currently not used but kept for future use
   
   Returns: Matrix V(r) as a vector of vectors
   
   Example:
   (let [ch0 (channel-definition 0 0.0 0 nil [50.0 2.0 0.6])
         ch1 (channel-definition 1 4.44 2 nil [50.0 2.0 0.6])
         channels [ch0 ch1]
         couplings [{:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}]]
     (coupled-channels-potential-matrix 2.0 channels couplings 10.0))"
  [r channels coupling-specs _E-incident]
  (let [n-channels (count channels)
        ;; Initialize matrix with zeros
        matrix (vec (repeat n-channels (vec (repeat n-channels 0.0))))]
    (reduce (fn [m [i channel-i]]
              (reduce (fn [m2 [j channel-j]]
                        (let [val (if (= i j)
                                   ;; Diagonal: channel potential
                                   (let [V-params (:V-params channel-i)]
                                     ;; Woods-Saxon potential at channel energy
                                     (WS r V-params))
                                   ;; Off-diagonal: coupling potential
                                   (let [coupling-forward (first (filter #(and (= (:from %) i)
                                                                              (= (:to %) j))
                                                                       coupling-specs))
                                         coupling-backward (first (filter #(and (= (:from %) j)
                                                                               (= (:to %) i))
                                                                        coupling-specs))
                                         coupling (or coupling-forward coupling-backward)]
                                     (if coupling
                                       (coupling-matrix-element r channel-i channel-j
                                                               (:lambda coupling)
                                                               (:beta coupling)
                                                               (:strength coupling 1.0))
                                       0.0)))]
                          (assoc-in m2 [i j] val)))
                      m
                      (map-indexed vector channels)))
            matrix
            (map-indexed vector channels))))

(defn coupled-channels-f-matrix
  "Calculate f-matrix for Numerov integration in coupled channels.
   
   The f-matrix is: f_αβ(r) = (2μ/ħ²) · [V_αβ(r) + δ_αβ · (l_α(l_α+1)/(mass-factor·r²) - E_α)]
   
   For diagonal elements, this matches f-r-numerov:
   f_ii = mass-factor * V_ii + l(l+1)/r² - mass-factor * E_i
   
   Parameters:
   - r: Radial distance (fm)
   - channels: Vector of channel definitions
   - coupling-specs: Vector of coupling specifications
   - E-incident: Incident energy (MeV)
   - mass-factor: Mass factor (2μ/ħ²) in MeV⁻¹·fm⁻²
   
   Returns: f-matrix as vector of vectors
   
   Note: This is used in the Numerov integration for coupled channels.
   The diagonal elements use the same formula as f-r-numerov for consistency."
  [r channels coupling-specs E-incident mass-factor]
  (let [V-matrix (coupled-channels-potential-matrix r channels coupling-specs E-incident)
        n-channels (count channels)]
    (vec (for [i (range n-channels)]
          (vec (for [j (range n-channels)]
                 (let [channel-i (nth channels i)
                       L-i (:L channel-i)
                       E-channel-i (channel-energy E-incident channel-i)
                       V-ij (get-in V-matrix [i j])]
                   (if (= i j)
                     ;; Diagonal: use f-r-numerov formula for consistency
                     ;; f_ii = mass-factor * V_ii + l(l+1)/r² - mass-factor * E_i
                     ;; This matches: f-r-numerov(r, E, l, V0, R0, a0, mass-factor)
                     (if (zero? r)
                       Double/POSITIVE_INFINITY
                       (let [centrifugal (/ (* L-i (inc L-i)) (* mass-factor r r))
                             v-eff (+ V-ij centrifugal)]
                         (* mass-factor (- v-eff E-channel-i))))
                     ;; Off-diagonal: just coupling potential
                     (* mass-factor V-ij)))))))))

(defn solve-coupled-channels-numerov
  "Solve coupled channels equations using Numerov method.
   
   Solves the system of coupled differential equations:
   -d²u_α/dr² + Σ_β f_αβ(r) u_β = 0
   
   Parameters:
   - channels: Vector of channel definitions
   - coupling-specs: Vector of coupling specifications
   - E-incident: Incident energy (MeV)
   - mass-factor: Mass factor (2μ/ħ²) in MeV⁻¹·fm⁻²
   - h: Step size (fm)
   - r-max: Maximum radius (fm)
   
   Returns: Vector of channel wavefunctions, each a vector of values u_α(r)
   
   Example:
   (let [ch0 (channel-definition 0 0.0 0 nil [50.0 2.0 0.6])
         ch1 (channel-definition 1 4.44 2 nil [50.0 2.0 0.6])
         channels [ch0 ch1]
         couplings [{:from 0, :to 1, :lambda 2, :beta 0.25, :strength 1.0}]]
     (solve-coupled-channels-numerov channels couplings 10.0 0.0247 0.01 20.0))"
  [channels coupling-specs E-incident mass-factor h r-max]
  (let [n-channels (count channels)
        steps (int (/ r-max h))
        h2-12 (/ (* h h) 12.0)
        ;; Initialize wavefunctions: u_α(0) = 0, u_α(h) ≈ h^(L_α+1)
        ;; Each channel has its own initial condition based on L
        initial-conditions (vec (for [channel channels]
                                 (let [L (:L channel)
                                       u0 0.0
                                       u1 (m/pow h (inc L))]
                                   [u0 u1])))
        ;; Pre-calculate f-matrices at all radial points
        f-matrices (mapv (fn [r]
                          (coupled-channels-f-matrix r channels coupling-specs 
                                                    E-incident mass-factor))
                        (take (+ steps 2) (iterate #(+ % h) 0.0)))]
    ;; Numerov integration for coupled system
    (loop [n 1
           wavefunctions initial-conditions]
      (if (>= n (dec steps))
        ;; Return wavefunctions as vectors
        ;; wavefunctions is already a vector where each element is a channel's wavefunction vector
        wavefunctions
        (let [;; Current wavefunction values: u_α(n*h)
              u-n (vec (for [i (range n-channels)]
                        (nth (nth wavefunctions i) n)))
              u-n-1 (vec (for [i (range n-channels)]
                          (nth (nth wavefunctions i) (dec n))))
              ;; f-matrices at n-1, n, n+1
              f-n-1 (nth f-matrices (dec n))
              f-n (nth f-matrices n)
              f-n+1 (nth f-matrices (inc n))
              ;; Calculate u_α(n+1) for each channel
              u-n+1 (vec (for [alpha (range n-channels)]
                          (let [;; Numerov step for coupled system
                                ;; u_α(n+1) = [2u_α(n) - u_α(n-1) + h²/12 * Σ_β (10f_αβ(n)u_β(n) + f_αβ(n-1)u_β(n-1))] / (1 - h²/12 * f_αα(n+1))
                                sum-term (reduce + (for [beta (range n-channels)]
                                                    (let [f-ab-n (get-in f-n [alpha beta])
                                                          f-ab-n-1 (get-in f-n-1 [alpha beta])
                                                          u-beta-n (nth u-n beta)
                                                          u-beta-n-1 (nth u-n-1 beta)]
                                                      (* h2-12 (+ (* 10.0 f-ab-n u-beta-n)
                                                                 (* f-ab-n-1 u-beta-n-1))))))
                                numerator (+ (* 2.0 (nth u-n alpha))
                                            (- (nth u-n-1 alpha))
                                            sum-term)
                                denominator (- 1.0 (* h2-12 (get-in f-n+1 [alpha alpha])))]
                            (/ numerator denominator))))]
          (recur (inc n)
                 (vec (for [i (range n-channels)]
                        (conj (nth wavefunctions i) (nth u-n+1 i))))))))))

(defn channel-wavefunction
  "Extract wavefunction for a specific channel from coupled channels solution.
   
   Parameters:
   - coupled-solution: Result from solve-coupled-channels-numerov
   - channel-id: Channel identifier (0, 1, 2, ...)
   
   Returns: Vector of wavefunction values u_α(r) for the specified channel
   
   Example:
   (let [solution (solve-coupled-channels-numerov ...)
         u0 (channel-wavefunction solution 0)  ; Ground state
         u1 (channel-wavefunction solution 1)]  ; First excited state
     ...)"
  [coupled-solution channel-id]
  (nth coupled-solution channel-id))

;; ============================================================================
;; PHASE 3: COLLECTIVE MODEL EXCITATIONS
;; ============================================================================

(defn spherical-harmonic
  "Calculate spherical harmonic Y_λμ(θ,φ).
   
   Spherical harmonics are defined as:
   Y_λμ(θ,φ) = N_λμ · P_λ^|μ|(cos θ) · exp(iμφ)
   
   where:
   - N_λμ is the normalization factor
   - P_λ^|μ| is the associated Legendre polynomial
   - θ is the polar angle (0 to π)
   - φ is the azimuthal angle (0 to 2π)
   
   Parameters:
   - lambda: Multipole order (l)
   - mu: Magnetic quantum number (m, -λ ≤ μ ≤ λ)
   - theta: Polar angle (radians)
   - phi: Azimuthal angle (radians, optional, defaults to 0)
   
   Returns: Complex number Y_λμ(θ,φ)
   
   Note: For inelastic scattering, we often only need the real part or
   work with specific μ values. This implementation returns the full complex value.
   
   Example:
   (spherical-harmonic 2 0 (/ Math/PI 2) 0)  ; Y_20(π/2, 0)"
  ([lambda mu theta]
   (spherical-harmonic lambda mu theta 0.0))
  ([lambda mu theta phi]
   (let [cos-theta (m/cos theta)
         ;; Normalization factor: N_λμ = sqrt((2λ+1)/(4π) · (λ-|μ|)!/(λ+|μ|)!)
         ;; For simplicity, we'll use a simplified normalization
         norm-factor (Math/sqrt (/ (* (inc (* 2 lambda)) 
                                     (m/factorial (- lambda (Math/abs mu))))
                                  (* 4.0 Math/PI 
                                     (m/factorial (+ lambda (Math/abs mu))))))
         ;; Associated Legendre polynomial P_λ^|μ|(cos θ)
         ;; For μ=0, this is just the Legendre polynomial P_λ(cos θ)
         ;; For |μ|>0, we use the associated Legendre polynomial
         leg-value (if (zero? mu)
                   (poly/eval-legendre-P lambda cos-theta)
                   ;; For |μ|>0, we approximate using derivative relation
                   ;; P_λ^μ(x) = (-1)^μ (1-x²)^(μ/2) d^μ/dx^μ P_λ(x)
                   ;; Simplified: use fastmath's associated Legendre if available
                   (let [abs-mu (Math/abs mu)
                         sign-factor (if (even? abs-mu) 1.0 -1.0)
                         sin-theta (Math/sin theta)
                         x-factor (m/pow sin-theta abs-mu)]
                     (* sign-factor x-factor 
                        (poly/eval-legendre-P lambda cos-theta))))
         ;; Exponential factor: exp(iμφ)
         exp-factor (complex-polar 1.0 (* mu phi))]
     (if (and (number? norm-factor) (number? leg-value) (number? exp-factor))
       (* norm-factor leg-value exp-factor)
       (mul norm-factor leg-value exp-factor)))))

(defn deformed-potential
  "Calculate deformed potential V(r,θ,φ) for collective excitations.
   
   The deformed potential is:
   V(r,θ,φ) = V_0(r) + V_1(r) · Y_λμ(θ,φ)
   
   where:
   - V_0(r) is the spherical Woods-Saxon potential
   - V_1(r) = β_λ · R_0 · dV_0/dr is the transition form factor
   - Y_λμ(θ,φ) is the spherical harmonic
   
   Parameters:
   - r: Radial distance (fm)
   - theta: Polar angle (radians)
   - phi: Azimuthal angle (radians, optional, defaults to 0)
   - lambda: Multipole order
   - mu: Magnetic quantum number
   - beta: Deformation parameter β_λ
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   
   Returns: V(r,θ,φ) in MeV (complex number in general, but real for μ=0)
   
   Example:
   (deformed-potential 2.0 (/ Math/PI 2) 0 2 0 0.25 [50.0 2.0 0.6])"
  ([r theta lambda mu beta V-params]
   (deformed-potential r theta 0.0 lambda mu beta V-params))
  ([r theta phi lambda mu beta V-params]
   (let [V0-r (WS r V-params)  ; Spherical potential
         F-lambda-r (transition-form-factor r lambda beta V-params)  ; Transition form factor
         Y-lambda-mu (spherical-harmonic lambda mu theta phi)]  ; Spherical harmonic
     (let [F-Y-product (if (and (number? F-lambda-r) (number? Y-lambda-mu))
                        (* F-lambda-r Y-lambda-mu)
                        (mul F-lambda-r Y-lambda-mu))]
       (if (and (number? V0-r) (number? F-Y-product))
         (+ V0-r F-Y-product)
         (add V0-r F-Y-product))))))

(defn rotational-band-energy
  "Calculate energy levels in a rotational band.
   
   For a rotational band with ground state spin I_gs and projection K:
   E(I) = E_0 + (ħ²/(2J)) · [I(I+1) - K²]
   
   where:
   - E_0 is the bandhead energy
   - J is the moment of inertia
   - I is the total angular momentum
   - K is the projection of I on the symmetry axis
   
   Parameters:
   - I: Total angular momentum
   - I-gs: Ground state spin of the band
   - K: Projection quantum number (0 for even-even nuclei)
   - E-bandhead: Bandhead energy (MeV)
   - moment-of-inertia: Moment of inertia parameter (MeV⁻¹, optional)
   
   Returns: Energy E(I) in MeV
   
   Example:
   (rotational-band-energy 2 0 0 0.0)  ; 2⁺ state in ground state band"
  ([I _I-gs K E-bandhead]
   (rotational-band-energy I _I-gs K E-bandhead 0.01))  ; Default moment of inertia
  ([I _I-gs K E-bandhead moment-of-inertia]
   (let [;; Rotational energy formula: E(I) = E_0 + A·[I(I+1) - K²]
         ;; where A = ħ²/(2J) is related to moment of inertia
         A (/ 1.0 (* 2.0 moment-of-inertia))
         rotational-term (* A (- (* I (inc I)) (* K K)))]
     (+ E-bandhead rotational-term))))

(defn rotational-band-structure
  "Calculate energy levels for a rotational band.
   
   Returns a map with energy levels for different I values.
   
   Parameters:
   - I-gs: Ground state spin
   - K: Projection quantum number
   - E-bandhead: Bandhead energy (MeV)
   - max-I: Maximum I to calculate (optional, defaults to I-gs + 6)
   - moment-of-inertia: Moment of inertia parameter (optional)
   
   Returns: Map of {I → E(I)}
   
   Example:
   (rotational-band-structure 0 0 0.0)  ; Ground state band (0⁺, 2⁺, 4⁺, ...)"
  ([I-gs K E-bandhead]
   (rotational-band-structure I-gs K E-bandhead (+ I-gs 6)))
  ([I-gs K E-bandhead max-I]
   (rotational-band-structure I-gs K E-bandhead max-I 0.01))
  ([I-gs K E-bandhead max-I moment-of-inertia]
   (let [I-values (range I-gs (inc max-I) 2)]  ; Even-even: I = 0, 2, 4, ...
     (into {} (for [I I-values]
                [I (rotational-band-energy I I-gs K E-bandhead moment-of-inertia)])))))

(defn vibrational-band-energy
  "Calculate energy levels in a vibrational band.
   
   For a harmonic vibrator:
   E(n) = E_0 + n · ħω
   
   where:
   - n is the phonon number (0, 1, 2, ...)
   - ħω is the phonon energy
   
   Parameters:
   - n: Phonon number
   - E-bandhead: Bandhead energy (MeV)
   - phonon-energy: Phonon energy ħω (MeV)
   
   Returns: Energy E(n) in MeV
   
   Example:
   (vibrational-band-energy 1 0.0 2.0)  ; One-phonon state at 2.0 MeV"
  [n E-bandhead phonon-energy]
  (+ E-bandhead (* n phonon-energy)))

(defn vibrational-band-structure
  "Calculate energy levels for a vibrational band.
   
   Returns a map with energy levels for different phonon numbers.
   
   Parameters:
   - E-bandhead: Bandhead energy (MeV)
   - phonon-energy: Phonon energy ħω (MeV)
   - max-n: Maximum phonon number (optional, defaults to 3)
   
   Returns: Map of {n → E(n)}
   
   Example:
   (vibrational-band-structure 0.0 2.0)  ; Vibrational band with ħω = 2.0 MeV"
  ([E-bandhead phonon-energy]
   (vibrational-band-structure E-bandhead phonon-energy 3))
  ([E-bandhead phonon-energy max-n]
   (into {} (for [n (range (inc max-n))]
              [n (vibrational-band-energy n E-bandhead phonon-energy)]))))

(defn collective-excitation-type
  "Determine if a nucleus is better described by rotational or vibrational model.
   
   This is a simplified classification based on deformation parameter.
   
   Parameters:
   - beta-2: Quadrupole deformation parameter β_2
   
   Returns: :rotational, :vibrational, or :spherical
   
   Classification:
   - β_2 < 0.15: Spherical (vibrational model)
   - 0.15 ≤ β_2 < 0.25: Transitional (can use either)
   - β_2 ≥ 0.25: Deformed (rotational model)
   
   Example:
   (collective-excitation-type 0.25)  ; Returns :rotational
   (collective-excitation-type 0.1)   ; Returns :vibrational"
  [beta-2]
  (cond
    (< beta-2 0.15) :vibrational
    (< beta-2 0.25) :transitional
    :else :rotational))

;; ============================================================================
;; PHASE 4: INELASTIC SCATTERING AMPLITUDE
;; ============================================================================

(defn distorted-wave-entrance
  "Calculate distorted wave for entrance channel (elastic scattering).
   
   The distorted wave χ_i is the solution to the scattering problem
   in the entrance channel with incident energy E_i.
   
   This function supports both simple real Woods-Saxon potentials and
   full optical potentials (with imaginary, spin-orbit, and Coulomb terms).
   
   Parameters:
   - E-i: Incident energy in entrance channel (MeV)
   - L-i: Orbital angular momentum in entrance channel
   - V-params: Either:
     - Simple: [V0, R0, a0] for real Woods-Saxon potential
     - Or: Map with optical potential parameters (see below)
   - h: Step size (fm)
   - r-max: Maximum radius (fm)
   - Optional keyword arguments:
     - :optical-potential-fn - Function r → U(r) for full optical potential
     - :projectile-type - Projectile type (:p, :n, :d, :alpha) for optical potential
     - :target-A - Target mass number for optical potential
     - :target-Z - Target charge number for optical potential
     - :E-lab - Lab energy (MeV) for optical potential
     - :s - Spin (default: 0.5 for nucleons, 1 for deuterons)
     - :j - Total angular momentum (default: L + s)
     - :mass-factor - Mass factor (2μ/ħ²), defaults to functions/mass-factor
   
   Returns: Vector of distorted wave values χ_i(r) at each radial point
   
   Examples:
   ;; Simple real potential (backward compatible)
   (distorted-wave-entrance 10.0 0 [50.0 2.0 0.6] 0.01 20.0)
   
   ;; Full optical potential
   (distorted-wave-entrance 10.0 0 nil 0.01 20.0
                            :projectile-type :d
                            :target-A 11
                            :target-Z 3
                            :E-lab 14.2
                            :s 1
                            :j 1)"
  [E-i L-i V-params h r-max & {:keys [optical-potential-fn projectile-type target-A target-Z E-lab s j mass-factor]
                               :or {s 0.5}}]
  (cond
    ;; Use full optical potential if provided
    optical-potential-fn
    (let [j-val (or j (+ L-i s))
          mf (or mass-factor functions/mass-factor)]
      (transfer/distorted-wave-optical E-i L-i s j-val optical-potential-fn r-max h mf))
    
    ;; Use optical potential from parameters
    (and projectile-type target-A target-Z E-lab)
    (let [j-val (or j (+ L-i s))
          mf (or mass-factor functions/mass-factor)
          U-fn (fn [r] (transfer/optical-potential-entrance-channel r projectile-type target-A target-Z E-lab L-i s j-val))]
      (transfer/distorted-wave-optical E-i L-i s j-val U-fn r-max h mf))
    
    ;; Fall back to simple real potential (backward compatible)
    :else
    (let [[V0 R0 a0] V-params]
      (solve-numerov E-i L-i V0 R0 a0 h r-max))))

(defn distorted-wave-exit
  "Calculate distorted wave for exit channel (inelastic scattering).
   
   The distorted wave χ_f is the solution to the scattering problem
   in the exit channel with energy E_f = E_i - E_ex.
   
   This function supports both simple real Woods-Saxon potentials and
   full optical potentials (with imaginary, spin-orbit, and Coulomb terms).
   
   Parameters:
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - L-f: Orbital angular momentum in exit channel
   - V-params: Either:
     - Simple: [V0, R0, a0] for real Woods-Saxon potential
     - Or: Map with optical potential parameters (see below)
   - h: Step size (fm)
   - r-max: Maximum radius (fm)
   - Optional keyword arguments:
     - :optical-potential-fn - Function r → U(r) for full optical potential
     - :outgoing-type - Outgoing particle type (:p, :n, :d, :alpha) for optical potential
     - :residual-A - Residual nucleus mass number for optical potential
     - :residual-Z - Residual nucleus charge number for optical potential
     - :E-lab - Lab energy in exit channel (MeV) for optical potential
     - :s - Spin (default: 0.5 for nucleons, 1 for deuterons)
     - :j - Total angular momentum (default: L + s)
     - :mass-factor - Mass factor (2μ/ħ²), defaults to functions/mass-factor
   
   Returns: Vector of distorted wave values χ_f(r) at each radial point
   
   Examples:
   ;; Simple real potential (backward compatible)
   (distorted-wave-exit 10.0 4.44 2 [50.0 2.0 0.6] 0.01 20.0)
   
   ;; Full optical potential
   (distorted-wave-exit 10.0 4.44 2 nil 0.01 20.0
                        :outgoing-type :d
                        :residual-A 11
                        :residual-Z 3
                        :E-lab 9.91
                        :s 1
                        :j 2)"
  [E-i E-ex L-f V-params h r-max & {:keys [optical-potential-fn outgoing-type residual-A residual-Z E-lab s j mass-factor]
                                    :or {s 0.5}}]
  (let [E-f (- E-i E-ex)]
    (cond
      ;; Use full optical potential if provided
      optical-potential-fn
      (let [j-val (or j (+ L-f s))
            mf (or mass-factor functions/mass-factor)]
        (transfer/distorted-wave-optical E-f L-f s j-val optical-potential-fn r-max h mf))
      
      ;; Use optical potential from parameters
      (and outgoing-type residual-A residual-Z E-lab)
      (let [j-val (or j (+ L-f s))
            mf (or mass-factor functions/mass-factor)
            U-fn (fn [r] (transfer/optical-potential-exit-channel r outgoing-type residual-A residual-Z E-lab L-f s j-val))]
        (transfer/distorted-wave-optical E-f L-f s j-val U-fn r-max h mf))
      
      ;; Fall back to simple real potential (backward compatible)
      :else
      (let [[V0 R0 a0] V-params]
        (solve-numerov E-f L-f V0 R0 a0 h r-max)))))

(defn transition-potential-radial
  "Calculate transition potential at radial distance r.
   
   The transition potential is: V_transition(r) = F_λ(r)
   where F_λ(r) is the transition form factor.
   
   For full angular dependence: V_transition(r,θ,φ) = F_λ(r) · Y_λμ(θ,φ)
   
   Parameters:
   - r: Radial distance (fm)
   - lambda: Multipole order
   - mu: Magnetic quantum number (for angular dependence, use 0 for radial-only)
   - beta: Deformation parameter β_λ
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - theta: Polar angle (radians, optional, for angular dependence)
   - phi: Azimuthal angle (radians, optional, for angular dependence)
   
   Returns: V_transition(r) or V_transition(r,θ,φ) in MeV
   
   Example:
   (transition-potential-radial 2.0 2 0 0.25 [50.0 2.0 0.6])  ; Radial only
   (transition-potential-radial 2.0 2 0 0.25 [50.0 2.0 0.6] (/ Math/PI 2) 0)  ; With angular"
  ([r _lambda _mu beta V-params]
   ;; Radial-only: V_transition(r) = F_λ(r)
   (transition-form-factor r _lambda beta V-params))
  ([r lambda mu beta V-params theta phi]
   ;; With angular dependence: V_transition(r,θ,φ) = F_λ(r) · Y_λμ(θ,φ)
   (let [F-lambda-r (transition-form-factor r lambda beta V-params)
         Y-lambda-mu (spherical-harmonic lambda mu theta phi)]
     (if (and (number? F-lambda-r) (number? Y-lambda-mu))
       (* F-lambda-r Y-lambda-mu)
       (mul F-lambda-r Y-lambda-mu)))))

(defn inelastic-amplitude-radial
  "Calculate inelastic scattering amplitude (radial integration only).
   
   The inelastic amplitude is: T_inel = ∫ χ*_f(r) V_transition(r) χ_i(r) r² dr
   
   This is a simplified version that integrates only over radial coordinates.
   For full 3D integration including angular parts, use inelastic-amplitude-full.
   
   Parameters:
   - chi-i: Distorted wave in entrance channel (vector)
   - chi-f: Distorted wave in exit channel (vector)
   - V-transition-r: Transition potential as function of r (vector or function)
     - If vector: V_transition(r) values at each radial point
     - If function: V_transition(r) function
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: Inelastic scattering amplitude T_inel (complex number in general)
   
   Example:
   (let [chi-i (distorted-wave-entrance 10.0 0 [50.0 2.0 0.6] 0.01 20.0)
         chi-f (distorted-wave-exit 10.0 4.44 2 [50.0 2.0 0.6] 0.01 20.0)
         V-trans (mapv #(transition-potential-radial % 2 0 0.25 [50.0 2.0 0.6])
                      (map #(* % 0.01) (range (int (/ 20.0 0.01)))))]
     (inelastic-amplitude-radial chi-i chi-f V-trans 20.0 0.01))"
  [chi-i chi-f V-transition-r _r-max h]
  (let [n (min (count chi-i) (count chi-f))
        V-trans-vec (if (vector? V-transition-r)
                     V-transition-r
                     (mapv (fn [i] (let [r (* i h)]
                                    (V-transition-r r)))
                           (range n)))
        n (min n (count V-trans-vec))
        integrand (mapv (fn [i]
                         (let [r (* i h)
                               chi-i-val (get chi-i i)
                               chi-f-val (get chi-f i)
                               V-trans-val (get V-trans-vec i)
                               chi-f-conj (if (number? chi-f-val)
                                           chi-f-val
                                           (let [re-val (re chi-f-val)
                                                 im-val (im chi-f-val)]
                                             (complex-cartesian re-val (- im-val))))
                               ;; For complex numbers, need to handle multiplication properly
                               product (if (and (number? chi-i-val) (number? chi-f-conj) (number? V-trans-val))
                                        (* chi-f-conj V-trans-val chi-i-val)
                                        ;; Complex multiplication
                                        (let [chi-i-complex (if (number? chi-i-val)
                                                            (complex-cartesian chi-i-val 0.0)
                                                            chi-i-val)
                                              V-complex (if (number? V-trans-val)
                                                        (complex-cartesian V-trans-val 0.0)
                                                        V-trans-val)]
                                          (mul chi-f-conj V-complex chi-i-complex)))]
                           (let [r-squared (* r r)]
                             (if (number? product)
                               (* product r-squared)
                               (mul product r-squared)))))
                       (range n))
        ;; Simpson's rule integration
        simpson-sum (loop [i 1 sum 0.0]
                     (if (>= i (dec n))
                       sum
                       (let [coeff (if (odd? i) 4.0 2.0)
                             term-val (get integrand i)
                             term (if (number? term-val)
                                   (* coeff term-val)
                                   (mul coeff term-val))
                             sum-next (if (number? sum)
                                       (if (number? term)
                                         (+ sum term)
                                         (add sum term))
                                       (if (number? term)
                                         (add sum (complex-cartesian term 0.0))
                                         (add sum term)))]
                         (recur (inc i) sum-next))))
        first-val (first integrand)
        last-val (last integrand)
        first-complex (if (number? first-val)
                       (complex-cartesian first-val 0.0)
                       first-val)
        last-complex (if (number? last-val)
                      (complex-cartesian last-val 0.0)
                      last-val)
        sum-complex (if (number? simpson-sum)
                     (complex-cartesian simpson-sum 0.0)
                     simpson-sum)
        total-sum (add first-complex last-complex sum-complex)]
    (let [h-factor (/ h 3.0)]
      (if (number? total-sum)
        (* h-factor total-sum)
        (mul h-factor total-sum)))))

(defn inelastic-amplitude
  "Calculate inelastic scattering amplitude with angular integration.
   
   Full 3D integration: T_inel = ∫ χ*_f(r) V_transition(r,θ,φ) χ_i(r) r² sin(θ) dr dθ dφ
   
   For axially symmetric case (μ=0), the angular integration simplifies.
   
   Parameters:
   - chi-i: Distorted wave in entrance channel (vector)
   - chi-f: Distorted wave in exit channel (vector)
   - lambda: Multipole order
   - mu: Magnetic quantum number (typically 0 for axially symmetric)
   - beta: Deformation parameter β_λ
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: Inelastic scattering amplitude T_inel
   
   Example:
   (let [chi-i (distorted-wave-entrance 10.0 0 [50.0 2.0 0.6] 0.01 20.0)
         chi-f (distorted-wave-exit 10.0 4.44 2 [50.0 2.0 0.6] 0.01 20.0)]
     (inelastic-amplitude chi-i chi-f 2 0 0.25 [50.0 2.0 0.6] 20.0 0.01))"
  [chi-i chi-f lambda mu beta V-params r-max h]
  (let [n (min (count chi-i) (count chi-f))
        ;; For μ=0, angular integration gives factor of 4π
        ;; For μ≠0, need full angular integration (more complex)
        angular-factor (if (zero? mu)
                        (* 4.0 Math/PI)  ; ∫ Y_λ0(θ,φ) sin(θ) dθ dφ = 4π for normalized Y
                        1.0)  ; For μ≠0, need proper integration
        ;; Create transition potential vector
        V-trans-vec (mapv (fn [i]
                           (let [r (* i h)]
                             (transition-form-factor r lambda beta V-params)))
                         (range n))
        T-radial (inelastic-amplitude-radial chi-i chi-f V-trans-vec r-max h)]
    (if (number? T-radial)
      (* angular-factor T-radial)
      (mul angular-factor T-radial))))

(defn inelastic-differential-cross-section
  "Calculate differential cross-section for inelastic scattering.
   
   The differential cross-section is:
   dσ/dΩ = (μ_f/(2πħ²))² · (k_f/k_i) · |T_inel|²
   
   where:
   - μ_f is the reduced mass in exit channel
   - k_i, k_f are wavenumbers in entrance and exit channels
   - T_inel is the inelastic scattering amplitude
   
   Parameters:
   - T-inel: Inelastic scattering amplitude (from inelastic-amplitude)
   - k-i: Wavenumber in entrance channel (fm⁻¹)
   - k-f: Wavenumber in exit channel (fm⁻¹)
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - mass-factor: Mass factor (2μ/ħ²) in MeV⁻¹·fm⁻²
   
   Returns: dσ/dΩ in fm²/sr (can be converted to mb/sr by multiplying by 10)
   
   Example:
   (let [T-inel (inelastic-amplitude ...)
         k-i (Math/sqrt (* mass-factor 10.0))
         k-f (Math/sqrt (* mass-factor 5.56))]
     (inelastic-differential-cross-section T-inel k-i k-f 10.0 4.44 mass-factor))"
  [T-inel k-i k-f _E-i _E-ex mass-factor]
  (let [;; Reduced mass factor: μ/(2πħ²) = mass-factor/(4π)
        mu-factor (/ mass-factor (* 4.0 Math/PI))
        ;; Wavenumber ratio
        k-ratio (/ k-f k-i)
        ;; Amplitude squared
        T-squared (if (number? T-inel)
                   (* T-inel T-inel)
                   (let [T-mag-val (mag T-inel)]
                     (* T-mag-val T-mag-val)))]
    (* mu-factor mu-factor k-ratio T-squared)))

(defn inelastic-cross-section
  "Calculate inelastic differential cross-section (simplified version).
   
   This is a convenience function that calculates everything from basic parameters.
   
   Parameters:
   - chi-i: Distorted wave in entrance channel
   - chi-f: Distorted wave in exit channel
   - lambda: Multipole order
   - mu: Magnetic quantum number
   - beta: Deformation parameter β_λ
   - V-params: Woods-Saxon parameters
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   - mass-factor: Mass factor (2μ/ħ²)
   
   Returns: dσ/dΩ in fm²/sr
   
   Example:
   (let [chi-i (distorted-wave-entrance 10.0 0 [50.0 2.0 0.6] 0.01 20.0)
         chi-f (distorted-wave-exit 10.0 4.44 2 [50.0 2.0 0.6] 0.01 20.0)]
     (inelastic-cross-section chi-i chi-f 2 0 0.25 [50.0 2.0 0.6] 10.0 4.44 20.0 0.01 mass-factor))"
  [chi-i chi-f lambda mu beta V-params E-i E-ex r-max h mass-factor]
  (let [T-inel (inelastic-amplitude chi-i chi-f lambda mu beta V-params r-max h)
        k-i (Math/sqrt (* mass-factor E-i))
        E-f (- E-i E-ex)
        k-f (Math/sqrt (* mass-factor E-f))]
    (inelastic-differential-cross-section T-inel k-i k-f E-i E-ex mass-factor)))

;; ============================================================================
;; PHASE 5: SINGLE-PARTICLE EXCITATIONS
;; ============================================================================

(defn particle-hole-state
  "Define a particle-hole excitation state.
   
   A particle-hole excitation occurs when a nucleon is promoted from an occupied
   orbital (hole state) to an unoccupied orbital (particle state).
   
   Parameters:
   - particle-n: Principal quantum number for particle state
   - particle-l: Orbital angular momentum for particle state
   - particle-j: Total angular momentum for particle state (2j+1 format, e.g., 1 for j=0, 3 for j=1)
   - hole-n: Principal quantum number for hole state
   - hole-l: Orbital angular momentum for hole state
   - hole-j: Total angular momentum for hole state
   
   Returns: Map with particle and hole quantum numbers
   
   Example:
   (particle-hole-state 2 1 3 1 0 1)  ; Particle: 2p_{3/2}, Hole: 1s_{1/2}"
  [particle-n particle-l particle-j hole-n hole-l hole-j]
  {:type :particle-hole
   :particle {:n particle-n, :l particle-l, :j particle-j}
   :hole {:n hole-n, :l hole-l, :j hole-j}})

(defn transition-density
  "Calculate transition density for particle-hole excitation.
   
   The transition density is: ρ_trans(r) = φ*_particle(r) · φ_hole(r)
   
   where:
   - φ_particle is the wavefunction of the particle state
   - φ_hole is the wavefunction of the hole state
   
   Parameters:
   - phi-particle: Wavefunction vector for particle state (from solve-bound-state)
   - phi-hole: Wavefunction vector for hole state (from solve-bound-state)
   - h: Step size (fm) - must be same for both wavefunctions
   
   Returns: Vector of transition density values ρ_trans(r)
   
   Note: Both wavefunctions must be on the same radial grid.
   
   Example:
   (let [phi-p (solve-bound-state [50.0 2.0 0.6] 2 1 nil 20.0 0.01)
         phi-h (solve-bound-state [50.0 2.0 0.6] 1 0 nil 20.0 0.01)]
     (transition-density (:normalized-wavefunction phi-p) 
                       (:normalized-wavefunction phi-h) 0.01))"
  [phi-particle phi-hole _h]
  (let [n (min (count phi-particle) (count phi-hole))]
    (mapv (fn [i]
            (let [phi-p-val (get phi-particle i)
                  phi-h-val (get phi-hole i)]
              (if (and (number? phi-p-val) (number? phi-h-val))
                (* phi-p-val phi-h-val)
                ;; Handle complex numbers if needed
                (mul phi-p-val phi-h-val))))
          (range n))))

(defn transition-density-function
  "Calculate transition density as a function of radius.
   
   Returns a vector of [r, ρ_trans(r)] pairs.
   
   Parameters:
   - phi-particle: Wavefunction vector for particle state
   - phi-hole: Wavefunction vector for hole state
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: Vector of [r, ρ_trans(r)] pairs
   
   Example:
   (transition-density-function phi-p phi-h 20.0 0.01)"
  [phi-particle phi-hole _r-max h]
  (let [rho-trans (transition-density phi-particle phi-hole h)
        n (count rho-trans)]
    (mapv (fn [i]
            (let [r (* i h)]
              [r (get rho-trans i)]))
          (range n))))

(defn transition-form-factor-from-density
  "Calculate transition form factor from transition density.
   
   For single-particle excitations, the transition form factor is:
   F_λ(r) = ∫ ρ_trans(r') · V_coupling(r, r') dr'
   
   For local approximation (simplified):
   F_λ(r) ≈ V_coupling(r) · ρ_trans(r)
   
   where V_coupling is the coupling potential (typically derivative of Woods-Saxon).
   
   Parameters:
   - rho-trans: Transition density vector
   - _lambda: Multipole order (currently not used in simplified version)
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - _r-max: Maximum radius (fm) (currently not used)
   - h: Step size (fm)
   
   Returns: Vector of form factor values F_λ(r)
   
   Example:
   (let [rho-trans (transition-density phi-p phi-h 0.01)]
     (transition-form-factor-from-density rho-trans 2 [50.0 2.0 0.6] 20.0 0.01))"
  [rho-trans _lambda V-params _r-max h]
  (let [n (count rho-trans)
        [_V0 R0 _a0] V-params]
    (mapv (fn [i]
            (let [r (* i h)
                  rho-val (get rho-trans i)
                  ;; Use derivative of Woods-Saxon as coupling potential
                  dV-dr (woods-saxon-derivative r V-params)
                  ;; Form factor: F_λ(r) ≈ β_eff · R0 · dV/dr · ρ_trans(r)
                  ;; where β_eff is an effective deformation parameter
                  ;; For single-particle, we use a normalization factor
                  beta-eff 0.1]  ; Typical value for single-particle transitions
              (* beta-eff R0 dV-dr rho-val)))
          (range n))))

(defn spectroscopic-factor
  "Calculate spectroscopic factor for single-particle transition.
   
   The spectroscopic factor S is related to the overlap between the
   many-body wavefunction and the single-particle state.
   
   For particle-hole excitations:
   S = |<Ψ_f|a^†_particle a_hole|Ψ_i>|²
   
   In the single-particle model, this is approximated by:
   S ≈ (2j+1) · normalization_factor
   
   Parameters:
   - particle-j: Total angular momentum of particle state (2j+1 format)
   - hole-j: Total angular momentum of hole state (2j+1 format)
   - overlap-integral: Overlap between particle and hole wavefunctions (optional)
   - normalization-factor: Additional normalization (default: 1.0)
   
   Returns: Spectroscopic factor S (dimensionless)
   
   Example:
   (spectroscopic-factor 3 1)  ; Particle: j=1, Hole: j=0"
  ([particle-j hole-j]
   (spectroscopic-factor particle-j hole-j nil 1.0))
  ([particle-j hole-j overlap-integral]
   (spectroscopic-factor particle-j hole-j overlap-integral 1.0))
  ([particle-j hole-j overlap-integral normalization-factor]
   (let [;; Statistical factor: (2j+1) for particle
         stat-factor particle-j
         ;; Overlap factor (if provided)
         overlap-factor (if overlap-integral
                        (if (number? overlap-integral)
                          (Math/abs overlap-integral)
                          (mag overlap-integral))
                        1.0)]
     (* stat-factor overlap-factor normalization-factor))))

(defn single-particle-excitation-energy
  "Estimate excitation energy for single-particle transition.
   
   For particle-hole excitations:
   E_ex ≈ E_particle - E_hole
   
   where E_particle and E_hole are the single-particle energies.
   
   Parameters:
   - E-particle: Single-particle energy of particle state (MeV, negative for bound)
   - E-hole: Single-particle energy of hole state (MeV, negative for bound)
   - pairing-energy: Pairing energy correction (MeV, optional, default: 0.0)
   
   Returns: Excitation energy E_ex (MeV, positive)
   
   Example:
   (single-particle-excitation-energy -5.0 -10.0)  ; Returns 5.0 MeV"
  ([E-particle E-hole]
   (single-particle-excitation-energy E-particle E-hole 0.0))
  ([E-particle E-hole pairing-energy]
   ;; E_ex = |E_hole| - |E_particle| + pairing correction
   ;; Since both are negative, we take the difference
   (let [E-ex-raw (- (Math/abs E-hole) (Math/abs E-particle))]
     (+ E-ex-raw pairing-energy))))

(defn particle-hole-form-factor
  "Calculate form factor for particle-hole excitation (complete calculation).
   
   This combines transition density and coupling potential to get the full
   transition form factor for single-particle excitations.
   
   Parameters:
   - phi-particle: Wavefunction vector for particle state
   - phi-hole: Wavefunction vector for hole state
   - lambda: Multipole order
   - V-params: Woods-Saxon parameters [V0, R0, a0]
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: Vector of form factor values F_λ(r)
   
   Example:
   (let [phi-p (solve-bound-state [50.0 2.0 0.6] 2 1 nil 20.0 0.01)
         phi-h (solve-bound-state [50.0 2.0 0.6] 1 0 nil 20.0 0.01)]
     (particle-hole-form-factor (:normalized-wavefunction phi-p)
                               (:normalized-wavefunction phi-h)
                               2 [50.0 2.0 0.6] 20.0 0.01))"
  [phi-particle phi-hole lambda V-params r-max h]
  (let [rho-trans (transition-density phi-particle phi-hole h)]
    (transition-form-factor-from-density rho-trans lambda V-params r-max h)))

(defn single-particle-inelastic-amplitude
  "Calculate inelastic scattering amplitude for single-particle excitation.
   
   This uses the particle-hole form factor instead of collective deformation.
   
   Parameters:
   - chi-i: Distorted wave in entrance channel
   - chi-f: Distorted wave in exit channel
   - phi-particle: Wavefunction vector for particle state
   - phi-hole: Wavefunction vector for hole state
   - lambda: Multipole order
   - V-params: Woods-Saxon parameters
   - r-max: Maximum radius (fm)
   - h: Step size (fm)
   
   Returns: Inelastic scattering amplitude T_inel
   
   Example:
   (let [chi-i (distorted-wave-entrance 10.0 0 [50.0 2.0 0.6] 0.01 20.0)
         chi-f (distorted-wave-exit 10.0 5.0 2 [50.0 2.0 0.6] 0.01 20.0)
         phi-p (:normalized-wavefunction (solve-bound-state [50.0 2.0 0.6] 2 1 nil 20.0 0.01))
         phi-h (:normalized-wavefunction (solve-bound-state [50.0 2.0 0.6] 1 0 nil 20.0 0.01))]
     (single-particle-inelastic-amplitude chi-i chi-f phi-p phi-h 2 [50.0 2.0 0.6] 20.0 0.01))"
  [chi-i chi-f phi-particle phi-hole lambda V-params r-max h]
  (let [F-lambda-vec (particle-hole-form-factor phi-particle phi-hole lambda V-params r-max h)]
          (inelastic-amplitude-radial chi-i chi-f F-lambda-vec r-max h)))

;; ============================================================================
;; PHASE 6: ANGULAR DISTRIBUTION
;; ============================================================================

(defn clebsch-gordan
  "Calculate Clebsch-Gordan coefficient <j1 m1 j2 m2 | J M>.
   
   Clebsch-Gordan coefficients couple two angular momenta:
   |J M> = Σ_{m1,m2} <j1 m1 j2 m2 | J M> |j1 m1> |j2 m2>
   
   Parameters:
   - j1: First angular momentum (half-integer, e.g., 1/2, 1, 3/2, ...)
   - m1: First magnetic quantum number (-j1 ≤ m1 ≤ j1)
   - j2: Second angular momentum
   - m2: Second magnetic quantum number (-j2 ≤ m2 ≤ j2)
   - J: Total angular momentum
   - M: Total magnetic quantum number (M = m1 + m2)
   
   Returns: Clebsch-Gordan coefficient (real number)
   
   Note: This is a simplified implementation. For production use, consider
   using a specialized library for more accurate calculations.
   
   Example:
   (clebsch-gordan 1 0 1 0 2 0)  ; <1 0 1 0 | 2 0>"
  [j1 m1 j2 m2 J M]
  ;; Check selection rules
  (if (not= M (+ m1 m2))
    0.0  ; M must equal m1 + m2
    (if (or (< J (Math/abs (- j1 j2)))
            (> J (+ j1 j2)))
      0.0  ; Triangle inequality: |j1 - j2| ≤ J ≤ j1 + j2
      ;; Simplified calculation using Wigner 3-j symbol relation
      ;; CG = (-1)^(j1-j2+M) * sqrt(2J+1) * Wigner3j(j1 j2 J; m1 m2 -M)
      ;; For now, use a simplified approximation
      ;; In practice, this should use proper Wigner 3-j symbol calculation
      (let [sign (if (even? (int (- j1 j2 M))) 1.0 -1.0)
            sqrt-factor (Math/sqrt (inc (* 2 J)))
            ;; Simplified: use approximate formula
            ;; For specific cases, we can calculate exactly
            approx-value (cond
                          ;; Special case: J = j1 + j2, M = m1 + m2
                          (= J (+ j1 j2))
                          (* sign sqrt-factor 1.0)
                          ;; Special case: J = |j1 - j2|, M = m1 + m2
                          (= J (Math/abs (- j1 j2)))
                          (* sign sqrt-factor 1.0)
                          ;; General case: use approximate formula
                          :else
                          (let [j1-int (int j1)
                                j2-int (int j2)
                                J-int (int J)
                                fact-sum (m/factorial (+ j1-int j2-int J-int))
                                fact-diff1 (m/factorial (Math/abs (- j1-int j2-int J-int)))
                                fact-diff2 (m/factorial (Math/abs (- j2-int j1-int J-int)))]
                            (* sign sqrt-factor 
                               (Math/sqrt (/ (* (inc (* 2 j1)) (inc (* 2 j2)))
                                            (* (inc (* 2 J)) fact-sum fact-diff1 fact-diff2))))))]
        approx-value))))

(defn legendre-expansion
  "Expand a function in Legendre polynomials.
   
   Any function f(θ) can be expanded as:
   f(θ) = Σ_L a_L · P_L(cos θ)
   
   where P_L are Legendre polynomials and a_L are expansion coefficients.
   
   Parameters:
   - coefficients: Map or vector of expansion coefficients {L → a_L} or [a_0, a_1, a_2, ...]
   - theta: Polar angle (radians)
   
   Returns: Value of expanded function at angle θ
   
   Example:
   (legendre-expansion {0 1.0, 2 0.5} (/ Math/PI 2))  ; f(π/2) = 1.0·P_0 + 0.5·P_2"
  [coefficients theta]
  (let [cos-theta (m/cos theta)]
    (if (map? coefficients)
      ;; Map format: {L → a_L}
      (reduce + (map (fn [[L a-L]]
                     (* a-L (poly/eval-legendre-P L cos-theta)))
                   coefficients))
      ;; Vector format: [a_0, a_1, a_2, ...] where index = L
      (reduce + (map-indexed (fn [L a-L]
                              (* a-L (poly/eval-legendre-P L cos-theta)))
                            coefficients)))))

(defn legendre-coefficients
  "Calculate Legendre expansion coefficients from angular distribution.
   
   Given a function f(θ) sampled at discrete angles, calculate the coefficients
   a_L in the expansion: f(θ) = Σ_L a_L · P_L(cos θ)
   
   Uses orthogonality: a_L = (2L+1)/2 · ∫ f(θ) P_L(cos θ) sin(θ) dθ
   
   Parameters:
   - angular-data: Vector of [theta, f(theta)] pairs
   - L-max: Maximum L to include in expansion
   
   Returns: Map of {L → a_L} coefficients
   
   Example:
   (let [data [[0 1.0] [(/ Math/PI 2) 0.5] [Math/PI 0.1]]]
     (legendre-coefficients data 4))"
  [angular-data L-max]
  (let [n-points (count angular-data)
        ;; Simpson's rule integration
        integrate (fn [integrand]
                   (loop [i 1 sum 0.0]
                     (if (>= i (dec n-points))
                       sum
                       (let [coeff (if (odd? i) 4.0 2.0)
                             term (* coeff (get integrand i))]
                         (recur (inc i) (+ sum term))))))]
    (into {} (for [L (range (inc L-max))]
               (let [;; Calculate integrand: f(θ) · P_L(cos θ) · sin(θ)
                     integrand (mapv (fn [[theta f-val]]
                                      (let [cos-theta (m/cos theta)
                                            P-L (poly/eval-legendre-P L cos-theta)
                                            sin-theta (Math/sin theta)]
                                        (* f-val P-L sin-theta)))
                                    angular-data)
                     ;; Simpson's rule integration
                     integral (integrate integrand)
                     ;; Coefficient: a_L = (2L+1)/2 · integral
                     a-L (* (/ (inc (* 2 L)) 2.0) integral)]
                 [L a-L])))))

(defn inelastic-angular-distribution
  "Calculate angular distribution for inelastic scattering.
   
   The angular distribution is: dσ/dΩ(θ) = |f(θ)|²
   
   where f(θ) is the scattering amplitude as a function of angle.
   
   For inelastic scattering with multipole order λ:
   dσ/dΩ(θ) = (μ_f/(2πħ²))² · (k_f/k_i) · |T_inel(θ)|²
   
   Parameters:
   - T-inel: Inelastic scattering amplitude (can be function of θ or constant)
   - theta: Scattering angle (radians)
   - k-i: Wavenumber in entrance channel (fm⁻¹)
   - k-f: Wavenumber in exit channel (fm⁻¹)
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - mass-factor: Mass factor (2μ/ħ²)
   - lambda: Multipole order (optional, for angular dependence)
   - mu: Magnetic quantum number (optional, for angular dependence)
   
   Returns: dσ/dΩ(θ) in fm²/sr
   
   Example:
   (inelastic-angular-distribution T-inel (/ Math/PI 2) k-i k-f 10.0 4.44 mass-factor)"
  ([T-inel theta k-i k-f E-i E-ex mass-factor]
   (inelastic-angular-distribution T-inel theta k-i k-f E-i E-ex mass-factor nil nil))
  ([T-inel theta k-i k-f E-i E-ex mass-factor lambda mu]
   (let [;; Get amplitude at this angle
         T-theta (if (fn? T-inel)
                  (T-inel theta lambda mu)
                  T-inel)
         ;; Calculate differential cross-section
         dsigma (inelastic-differential-cross-section T-theta k-i k-f E-i E-ex mass-factor)]
     dsigma)))

(defn inelastic-angular-distribution-function
  "Calculate angular distribution as a function of angle.
   
   Returns a vector of [theta, dσ/dΩ(theta)] pairs.
   
   Parameters:
   - T-inel: Inelastic scattering amplitude (constant or function)
   - theta-values: Vector of angles (radians) to evaluate
   - k-i: Wavenumber in entrance channel
   - k-f: Wavenumber in exit channel
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - mass-factor: Mass factor
   - lambda: Multipole order (optional)
   - mu: Magnetic quantum number (optional)
   
   Returns: Vector of [theta, dσ/dΩ(theta)] pairs
   
   Example:
   (let [thetas (map #(* % (/ Math/PI 18)) (range 19))]  ; 0 to π in 10° steps
     (inelastic-angular-distribution-function T-inel thetas k-i k-f 10.0 4.44 mass-factor))"
  ([T-inel theta-values k-i k-f E-i E-ex mass-factor]
   (inelastic-angular-distribution-function T-inel theta-values k-i k-f E-i E-ex mass-factor nil nil))
  ([T-inel theta-values k-i k-f E-i E-ex mass-factor lambda mu]
   (mapv (fn [theta]
          (let [dsigma (inelastic-angular-distribution T-inel theta k-i k-f E-i E-ex 
                                                      mass-factor lambda mu)]
            [theta dsigma]))
        theta-values)))

(defn interference-term
  "Calculate interference term between two channels.
   
   For inelastic scattering with multiple channels, the total amplitude is:
   f_total(θ) = Σ_i f_i(θ)
   
   and the cross-section includes interference:
   dσ/dΩ = |f_total|² = Σ_i |f_i|² + 2·Re(Σ_{i<j} f_i* · f_j)
   
   Parameters:
   - amplitude-1: First scattering amplitude (complex or real)
   - amplitude-2: Second scattering amplitude (complex or real)
   
   Returns: Interference term 2·Re(f_1* · f_2)
   
   Example:
   (interference-term f1 f2)"
  [amplitude-1 amplitude-2]
  (let [f1-conj (if (number? amplitude-1)
                 amplitude-1
                 (let [re1 (re amplitude-1)
                       im1 (im amplitude-1)]
                   (complex-cartesian re1 (- im1))))
        product (if (and (number? f1-conj) (number? amplitude-2))
                 (* f1-conj amplitude-2)
                 (mul f1-conj amplitude-2))
        real-part (if (number? product)
                   product
                   (re product))]
    (* 2.0 real-part)))

(defn multi-channel-angular-distribution
  "Calculate angular distribution for multiple inelastic channels.
   
   Includes interference between channels.
   
   Parameters:
   - amplitudes: Vector of scattering amplitudes [f_1(θ), f_2(θ), ...]
     Each can be a function f(θ) or a constant
   - theta: Scattering angle (radians)
   - k-i: Wavenumber in entrance channel
   - k-f: Wavenumber in exit channel
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - mass-factor: Mass factor
   
   Returns: Total dσ/dΩ(θ) including interference
   
   Example:
   (multi-channel-angular-distribution [f1 f2] (/ Math/PI 2) k-i k-f 10.0 4.44 mass-factor)"
  [amplitudes theta k-i k-f E-i E-ex mass-factor]
  (let [;; Get amplitude values at this angle
        f-values (mapv (fn [f]
                       (if (fn? f)
                         (f theta)
                         f))
                     amplitudes)
        ;; Total amplitude: f_total = Σ_i f_i
        f-total (reduce add f-values)
        ;; Calculate cross-section
        dsigma (inelastic-differential-cross-section f-total k-i k-f E-i E-ex mass-factor)]
    dsigma))

(defn angular-distribution-legendre-expansion
  "Calculate angular distribution and expand in Legendre polynomials.
   
   This combines angular distribution calculation with Legendre expansion.
   
   Parameters:
   - T-inel: Inelastic scattering amplitude
   - theta-values: Vector of angles to sample
   - k-i: Wavenumber in entrance channel
   - k-f: Wavenumber in exit channel
   - E-i: Incident energy (MeV)
   - E-ex: Excitation energy (MeV)
   - mass-factor: Mass factor
   - L-max: Maximum L for Legendre expansion
   - lambda: Multipole order (optional)
   - mu: Magnetic quantum number (optional)
   
   Returns: Map with {:angular-data, :legendre-coefficients, :expansion-function}
   
   Example:
   (let [thetas (map #(* % (/ Math/PI 18)) (range 19))]
     (angular-distribution-legendre-expansion T-inel thetas k-i k-f 10.0 4.44 mass-factor 4))"
  ([T-inel theta-values k-i k-f E-i E-ex mass-factor L-max]
   (angular-distribution-legendre-expansion T-inel theta-values k-i k-f E-i E-ex 
                                           mass-factor L-max nil nil))
  ([T-inel theta-values k-i k-f E-i E-ex mass-factor L-max lambda mu]
   (let [;; Calculate angular distribution
         angular-data (inelastic-angular-distribution-function T-inel theta-values k-i k-f 
                                                               E-i E-ex mass-factor lambda mu)
         ;; Calculate Legendre coefficients
         coeffs (legendre-coefficients angular-data L-max)
         ;; Create expansion function
         expansion-fn (fn [theta]
                       (legendre-expansion coeffs theta))]
     {:angular-data angular-data
      :legendre-coefficients coeffs
      :expansion-function expansion-fn})))
