;; Example: 16O(p,d) Transfer Reaction
;; 
;; This example calculates the form factor, transfer amplitude (post formulation),
;; and differential cross section for the 16O(p,d) reaction.
;;
;; Reaction: 16O(p,d)15O
;; - Initial: neutron bound in 16O (l=1, E=-15.67 MeV)
;; - Final: neutron bound in deuteron (l=0, E=-2.214 MeV)

;;
;; Reference parameters:
;; - 16O: R0 = 2.7 fm, V0 = 62 MeV, a0 = 0.6 fm, l=1
;; - Deuteron: R0 = 1.5 fm, V0 = 50 MeV, a0 = 0.6 fm, l=0

(require '[dwba.transfer :as t])
(require '[dwba.form-factors :as ff])
(require '[dwba.inelastic :as inel])
(require '[functions :refer [solve-numerov mass-factor]])
(require '[fastmath.core :as m])
(require '[complex :as c :refer [mag re im complex-cartesian add mul]])

(println "=== 16O(p,d) Transfer Reaction Calculation ===")
(println "")

;; ============================================================================
;; Bound State Wavefunctions
;; ============================================================================

(println "=== Step 1: Bound State Wavefunctions ===")

(let [;; Bound state parameters
      v0-i 62.0 R0-i 2.7 diff-i 0.6 r-max 20.0 h 0.01 l-i 1
      v0-f 50.0 R0-f 1.5 diff-f 0.6 l-f 0
      Es-i -15.67  ; Neutron bound in 16O
      Es-f -2.214  ; Neutron bound in deuteron
      
      ;; Mass factor for bound states (using standard value from functions.clj)
      ;; For bound states, we typically use the reduced mass of the bound system
      ;; Using a typical value: 2μ/ħ² ≈ 0.048 MeV⁻¹·fm⁻² for nucleon-nucleus systems
      m-f 0.048  ; Mass factor (2μ/ħ²) in MeV⁻¹·fm⁻²
      
      ;; Calculate bound state wavefunctions
      phi-i-raw (t/solve-bound-state-numerov Es-i l-i v0-i R0-i diff-i m-f h r-max)
      phi-f-raw (t/solve-bound-state-numerov Es-f l-f v0-f R0-f diff-f m-f h r-max)
      ;; Normalize bound state wavefunctions
      phi-i (t/normalize-bound-state phi-i-raw h)
      phi-f (t/normalize-bound-state phi-f-raw h)
      
      ;; Calculate normalized overlap (form factor)
      overlap-norm (ff/normalized-overlap phi-i phi-f r-max h)
      
      ;; ============================================================================
      ;; Reaction Parameters
      ;; ============================================================================
      
      ;; Incident energy (typical for (p,d) reactions: 10-20 MeV)
      E-lab 20.0  ; Lab frame energy (MeV)
      
      ;; Masses (in MeV/c²)
      m-p  938.27     ; Proton mass
      m-16O 14899.0   ; 16O mass (approximate)
      m-d 1876.136      ; Deuteron mass
      m-15O 13975.0   ; 15O mass (approximate)
      
      ;; Reduced masses
      mu-i (/ (* m-p m-16O) (+ m-p m-16O))      ; Entrance channel: p+16O
      mu-f (/ (* m-d m-15O) (+ m-d m-15O))      ; Exit channel: d+15O
      
      ;; Mass factors
      mass-factor-i (/ (* 2.0 mu-i) (* 197.7 197.7))  ; 2μ/ħ² for entrance
      mass-factor-f (/ (* 2.0 mu-f) (* 197.7 197.7))  ; 2μ/ħ² for exit
      
      ;; CM frame energies
      E-CM-i (* E-lab (/ m-16O (+ m-16O m-p)))  ; Entrance CM energy
      ;; Q-value for 16O(p,d)15O reaction
      ;; Q = (m_p + m_16O - m_d - m_15O) * c²
      ;; Or using binding energies: Q = |Es-i| - |Es-f|
      ;; where Es-i is the neutron separation energy from 16O (negative, bound)
      ;; and Es-f is the deuteron binding energy (negative, bound)
      ;; For (p,d) pickup: Q = B_n(16O) - B_d = |Es-i| - |Es-f|
      ;; Note: Q is typically NEGATIVE for pickup reactions (endothermic)
      ;; The reaction is kinematically allowed if E_CM_i + Q > 0
      Q-value  (+ m-p m-16O (- m-d) (- m-15O))
      E-CM-f (+ E-CM-i Q-value)  ; Exit CM energy = E_i + Q (Q can be negative for endothermic reactions)
      
      ;; ============================================================================
      ;; Distorted Waves (with Coulomb potential)
      ;; ============================================================================
      
      ;; Calculate distorted waves with Coulomb potential
      ;; Entrance channel: p + 16O (Z1=1, Z2=8)
      ;; Exit channel: d + 15O (Z1=1, Z2=8)
      L-i 0  ; Entrance channel angular momentum
      L-f 0  ; Exit channel angular momentum
      
      ;; Lab energies for optical potential calculation
      E-lab-i E-lab  ; Entrance channel lab energy
      E-lab-f (* E-CM-f (/ (+ m-d m-15O) m-15O))  ; Exit channel lab energy (approximate)
      
      ;; Calculate distorted waves with optical potentials including Coulomb
      chi-i (inel/distorted-wave-entrance E-CM-i L-i nil h r-max
                                          :projectile-type :p
                                          :target-A 16
                                          :target-Z 8
                                          :E-lab E-lab-i
                                          :s 0.5  ; Proton spin
                                          :j 0.5  ; Total angular momentum (L=0, s=0.5)
                                          :mass-factor mass-factor-i)
      
      chi-f (inel/distorted-wave-exit E-CM-i Q-value L-f nil h r-max
                                      :outgoing-type :d
                                      :residual-A 15
                                      :residual-Z 8
                                      :E-lab E-lab-f
                                      :s 1      ; Deuteron spin
                                      :j 1      ; Total angular momentum (L=0, s=1)
                                      :mass-factor mass-factor-f)
      
      ;; ============================================================================
      ;; Transfer Amplitude (Post Formulation)
      ;; ============================================================================
      
      ;; Zero-range constant for (p,d) reaction
      D0 (t/zero-range-constant :p-d)  ; D₀ for (p,d) reaction
      T-post (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h 
                                        :zero-range D0)
      
      ;; ============================================================================
      ;; Differential Cross Section
      ;; ============================================================================
      
      ;; Wavenumbers
      k-i (Math/sqrt (* mass-factor-i E-CM-i))
      k-f (Math/sqrt (* mass-factor-f E-CM-f))
      
      ;; Spectroscopic factor (typically 0 < S < 1, using 1.0 for this example)
      S-factor 1.0
      
      ;; Calculate differential cross section at specific angle
      ;; For L=0 transfer, we use {0 → T_post} as the amplitude map
      theta-deg 0.0  ; Scattering angle in degrees (0° = forward)
      theta-rad (* theta-deg (/ Math/PI 180.0))  ; Convert to radians
      T-amplitudes {0 T-post}  ; Map of {L → T_L}, for L=0 only
      ;; Use angular-dependent cross section function
      dsigma (t/transfer-differential-cross-section-angular T-amplitudes S-factor k-i k-f 
                                                           theta-rad mass-factor-i mass-factor-f)]
  
  ;; ============================================================================
  ;; Output
  ;; ============================================================================
  
  (println (format "Initial state (neutron in 16O):"))
  (println (format "  Energy: E_i = %.2f MeV" Es-i))
  (println (format "  Angular momentum: l_i = %d" l-i))
  (println (format "  Wavefunction length: %d points" (count phi-i)))
  (println "")
  
  (println (format "Final state (neutron in deuteron):"))
  (println (format "  Energy: E_f = %.2f MeV" Es-f))
  (println (format "  Angular momentum: l_f = %d" l-f))
  (println (format "  Wavefunction length: %d points" (count phi-f)))
  (println "")
  
  (println (format "Normalized overlap (form factor): %.6f" overlap-norm))
  (println "")
  
  (println "=== Step 2: Reaction Parameters ===")
  (println (format "Incident energy: E_lab = %.2f MeV" E-lab))
  (println (format "Entrance CM energy: E_CM_i = %.2f MeV" E-CM-i))
  (println (format "Exit CM energy: E_CM_f = %.2f MeV" E-CM-f))
  (println (format "Q-value: Q = %.2f MeV" Q-value))
  (println "")
  
  (println "=== Step 3: Distorted Waves ===")
  (println (format "Entrance channel distorted wave:"))
  (println (format "  Energy: E_i = %.2f MeV (CM)" E-CM-i))
  (println (format "  Angular momentum: L_i = %d" L-i))
  (println (format "  Wavefunction length: %d points" (count chi-i)))
  (println "")
  
  (println (format "Exit channel distorted wave:"))
  (println (format "  Energy: E_f = %.2f MeV (CM)" E-CM-f))
  (println (format "  Angular momentum: L_f = %d" L-f))
  (println (format "  Wavefunction length: %d points" (count chi-f)))
  ;; Check magnitude of distorted waves
  (let [chi-i-max (apply max (map #(if (number? %) (Math/abs %) (mag %)) chi-i))
        chi-f-max (apply max (map #(if (number? %) (Math/abs %) (mag %)) chi-f))
        chi-i-avg (let [sum (reduce + (map #(if (number? %) (Math/abs %) (mag %)) chi-i))]
                    (/ sum (count chi-i)))
        chi-f-avg (let [sum (reduce + (map #(if (number? %) (Math/abs %) (mag %)) chi-f))]
                    (/ sum (count chi-f)))
        ;; Calculate integral of chi-i* · chi-f
        n-chi (min (count chi-i) (count chi-f))
        integrand-chi (mapv (fn [i]
                              (let [r (* i h)
                                    chi-i-val (get chi-i i)
                                    chi-f-val (get chi-f i)
                                    chi-i-conj (if (number? chi-i-val)
                                                chi-i-val
                                                (complex-cartesian (re chi-i-val) (- (im chi-i-val))))
                                    product (if (and (number? chi-i-conj) (number? chi-f-val))
                                             (* chi-i-conj chi-f-val)
                                             (mul chi-i-conj chi-f-val))]
                                product))
                            (range n-chi))
        simpson-sum-chi (loop [i 1 sum (complex-cartesian 0.0 0.0)]
                         (if (>= i (dec n-chi))
                           sum
                           (let [coeff (if (odd? i) 4.0 2.0)
                                 term-val (get integrand-chi i)
                                 coeff-complex (complex-cartesian coeff 0.0)
                                 term (mul coeff-complex term-val)]
                             (recur (inc i) (add sum term)))))
        first-term-chi (get integrand-chi 0)
        last-term-chi (get integrand-chi (dec n-chi))
        h-over-3-complex (complex-cartesian (/ h 3.0) 0.0)
        integral-chi (mul h-over-3-complex
                         (add first-term-chi last-term-chi simpson-sum-chi))]
    (println (format "  chi-i max magnitude: %.6e" chi-i-max))
    (println (format "  chi-f max magnitude: %.6e" chi-f-max))
    (println (format "  chi-i avg magnitude: %.6e" chi-i-avg))
    (println (format "  chi-f avg magnitude: %.6e" chi-f-avg))
    (println (format "  ∫ χ*_i · χ_f dr = %.6e + i%.6e" (re integral-chi) (im integral-chi)))
    (println (format "  |∫ χ*_i · χ_f dr| = %.6e" (mag integral-chi))))
  (println "")
  
  (println "=== Step 4: Transfer Amplitude (Post Formulation) ===")
  (println (format "Zero-range constant: D₀ = %.2f MeV·fm^(3/2)" D0))
  (println "")
  
  (println "Transfer amplitude (post formulation):")
  (if (number? T-post)
    (println (format "  T_post = %.6e" T-post))
    (println (format "  T_post = %.6e + i%.6e" (re T-post) (im T-post))))
  (println (format "  |T_post| = %.6e" (if (number? T-post) (Math/abs T-post) (mag T-post))))
  (println "")
  
  (println "=== Step 5: Differential Cross Section ===")
  (println (format "Wavenumbers:"))
  (println (format "  k_i (entrance): %.4f fm⁻¹" k-i))
  (println (format "  k_f (exit): %.4f fm⁻¹" k-f))
  (println (format "  Ratio k_f/k_i: %.4f" (/ k-f k-i)))
  (println (format "Spectroscopic factor: S = %.2f" S-factor))
  (println (format "Scattering angle: θ = %.1f° (%.4f rad)" theta-deg theta-rad))
  (println "")
  
  (println "Differential cross section:")
  (println (format "  dσ/dΩ(θ=%.1f°) = %.6e fm²/sr" theta-deg dsigma))
  (println (format "  dσ/dΩ(θ=%.1f°) = %.6e mb/sr (1 mb = 10 fm²)" theta-deg (* dsigma 10.0)))
  (println "")
  
  (println "=== Summary ===")
  (println (format "Reaction: 16O(p,d)15O"))
  (println (format "Incident energy: E_lab = %.2f MeV" E-lab))
  (println (format "Normalized overlap: %.6f" overlap-norm))
  (println (format "Transfer amplitude: |T_post| = %.6e" 
                  (if (number? T-post) (Math/abs T-post) (mag T-post))))
  (println (format "Differential cross section: dσ/dΩ = %.6e mb/sr" (* dsigma 10.0)))
  (println "")
  (println "=== Calculation Complete ===")
  
  ;; Return the differential cross section
  dsigma
  )

