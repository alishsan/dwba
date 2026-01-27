(require '[dwba.transfer :as t])
(require '[dwba.form-factors :as ff])
(require '[dwba.inelastic :as inel])
(require '[functions :refer [solve-numerov mass-factor]])
(require '[fastmath.core :as m])
(require '[complex :refer [mag re im]])

(println "=== 16O(p,d) Transfer Reaction Calculation ===")
(println "")

(let [;; Bound state parameters
      v0-i 62.0 R0-i 2.7 diff-i 0.6 r-max 20.0 h 0.01 l-i 1
      v0-f 50.0 R0-f 1.5 diff-f 0.6 l-f 0
      Es-i -15.67  ; Neutron bound in 16O
      Es-f -2.214  ; Neutron bound in deuteron
      
      m-f 0.048  ; Mass factor (2μ/ħ²) in MeV⁻¹·fm⁻²
      
      ;; Calculate bound state wavefunctions
      phi-i (t/solve-bound-state-numerov Es-i l-i v0-i R0-i diff-i m-f h r-max)
      phi-f (t/solve-bound-state-numerov Es-f l-f v0-f R0-f diff-f m-f h r-max)
      
      ;; Calculate normalized overlap (form factor)
      overlap-norm (ff/normalized-overlap phi-i phi-f r-max h)
      
      ;; Incident energy
      E-lab 15.0  ; Lab frame energy (MeV)
      
      ;; Masses (in MeV/c²)
      m-p 938.27      ; Proton mass
      m-16O 14899.0   ; 16O mass (approximate)
      m-d 1875.6      ; Deuteron mass
      m-15O 13975.0   ; 15O mass (approximate)
      
      ;; Reduced masses
      mu-i (/ (* m-p m-16O) (+ m-p m-16O))      ; Entrance channel: p+16O
      mu-f (/ (* m-d m-15O) (+ m-d m-15O))      ; Exit channel: d+15O
      
      ;; Mass factors
      mass-factor-i (/ (* 2.0 mu-i) (* 197.7 197.7))  ; 2μ/ħ² for entrance
      mass-factor-f (/ (* 2.0 mu-f) (* 197.7 197.7))  ; 2μ/ħ² for exit
      
      ;; CM frame energies
      E-CM-i (* E-lab (/ m-16O (+ m-16O m-p)))  ; Entrance CM energy
      Q-value (- (Math/abs Es-i) (Math/abs Es-f))  ; Q = |Es-i| - |Es-f|
      E-CM-f (+ E-CM-i Q-value)  ; Exit CM energy
      
      ;; Angular momenta
      L-i 0  ; Entrance channel angular momentum
      L-f 0  ; Exit channel angular momentum
      
      ;; Calculate distorted waves (simplified - using basic functions)
      chi-i (inel/distorted-wave-entrance E-CM-i L-i nil h r-max)
      chi-f (inel/distorted-wave-exit E-CM-i Q-value L-f nil h r-max)
      
      ;; Zero-range constant for (p,d) reaction
      D0 (t/zero-range-constant :p-d)  ; D₀ for (p,d) reaction
      T-post (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h :zero-range D0)
      
      ;; Wavenumbers
      k-i (Math/sqrt (* mass-factor-i E-CM-i))
      k-f (Math/sqrt (* mass-factor-f E-CM-f))
      
      ;; Spectroscopic factor
      S-factor 1.0
      
      ;; Calculate differential cross section
      dsigma (t/transfer-differential-cross-section T-post S-factor k-i k-f mass-factor-i mass-factor-f)]
  
  (println (format "Initial state (proton in 16O):"))
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
  (println (format "Entrance channel distorted wave length: %d points" (count chi-i)))
  (println (format "Exit channel distorted wave length: %d points" (count chi-f)))
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
  (println "")
  
  (println "Differential cross section:")
  (println (format "  dσ/dΩ = %.6e fm²/sr" dsigma))
  (println (format "  dσ/dΩ = %.6e mb/sr (1 mb = 10 fm²)" (* dsigma 10.0)))
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
  
  dsigma
  )
