#!/usr/bin/env clojure
;; Example usage of halo nuclei extensions to Bessel-Numerov method
;;
;; This file demonstrates:
;; 1. Extended asymptotic matching for halo nuclei
;; 2. Bound state calculations (¹¹Be, ⁸B)
;; 3. Coulomb interactions for charged halo nuclei
;; 4. ANC extraction
;; 5. Low-energy scattering
;; 6. Transfer reactions with global optical potentials (CH89 for protons, Daehnick80 for deuterons)

(require '[dwba.halo-nuclei :as halo]
         '[dwba.transfer :as transfer]
         '[dwba.inelastic :as inel]
         '[fastmath.core :as m]
         '[complex :refer [mag]])

(println "=== Halo Nuclei Calculations with Extended Bessel-Numerov Method ===\n")

;; ============================================================================
;; Example 1: ¹¹Be (Neutron Halo)
;; ============================================================================
(println "1. ¹¹Be Halo Nucleus Calculation")
(println "   Properties: E_b = 504 keV, l = 0, S_{1/2} state")
(println "   Core: ¹⁰Be, Halo: 1 neutron\n")

;; Use model-extracted ANC (no experimental ANC passed).
;; To use experimental ANC instead: (halo/example-11be :experimental-anc 0.78)
(let [result (halo/example-11be)
      anc (:anc result)
      r-match (:matching-radius result)
      E-b (:binding-energy result)]
  (println (format "   Binding Energy: %.3f MeV" E-b))
  (println (format "   Adaptive Matching Radius: %.2f fm" r-match))
  (println (format "   ANC: %.4f fm^(-1/2)" anc))
  (println (format "   (Use (halo/example-11be :experimental-anc 0.78) to prefer experimental value)"))
  (println ""))

;; ============================================================================
;; Example 2: ⁸B (Proton Halo)
;; ============================================================================
(println "2. ⁸B Proton Halo Nucleus Calculation")
(println "   Properties: E_b = 137 keV, l = 2, p-wave proton halo")
(println "   Core: ⁷Be, Halo: 1 proton\n")

;; To use experimental ANC: (halo/example-8b :experimental-anc 0.14)
(let [result (halo/example-8b)
      anc (:anc result)
      r-match (:matching-radius result)
      E-b (:binding-energy result)]
  (println (format "   Binding Energy: %.3f MeV" E-b))
  (println (format "   Adaptive Matching Radius: %.2f fm" r-match))
  (println (format "   ANC: %.4f fm^(-1/2)" anc))
  (println (format "   (Use :experimental-anc 0.14 when available from experiment)"))
  (println ""))

;; ============================================================================
;; Example 3: Decay Length Calculation
;; ============================================================================
(println "3. Decay Length Calculations")
(println "   Decay length κ⁻¹ = ℏ/√(2μ|E_b|) determines how far")
(println "   the halo wavefunction extends\n")

(let [cases [{:name "¹¹Be" :E-b 0.504 :mu 869.4}
             {:name "⁸B" :E-b 0.137 :mu 745.0}
             {:name "⁶He" :E-b 0.975 :mu 745.0}]]
  (doseq [case cases]
    (let [kappa-inv (halo/decay-length (:E-b case) (:mu case))]
      (println (format "   %s: E_b = %.3f MeV, κ⁻¹ = %.2f fm"
                       (:name case) (:E-b case) kappa-inv))))
  (println ""))

;; ============================================================================
;; Example 4: Coulomb Scattering
;; ============================================================================
(println "4. Coulomb + Nuclear Scattering (α+p at 10 MeV)")
(println "   Demonstrates Riccati-Bessel initialization with Coulomb effects\n")

(let [result (halo/example-coulomb-scattering)
      eta (:sommerfeld-parameter result)
      k (:wave-number result)
      E (:energy result)]
  (println (format "   Energy: %.1f MeV" E))
  (println (format "   Wave Number: %.4f fm⁻¹" k))
  (println (format "   Sommerfeld Parameter η: %.4f" eta))
  (println "   (η measures the strength of Coulomb interaction)")
  (println ""))

;; ============================================================================
;; Example 5: Low-Energy Scattering
;; ============================================================================
(println "5. Low-Energy Scattering (50 keV)")
(println "   Tests the method at very low energies near threshold\n")

(let [E 0.05 ; 50 keV
      l 0
      V-params [50.0 2.0 0.6]
      mu 869.4
      h 0.01
      r-max 50.0 ; Large radius for low energy
      
      u (halo/solve-low-energy-numerov E l V-params mu h r-max)
      max-u (apply max (map m/abs u))
      max-idx (first (keep-indexed
                      (fn [i val] (when (= (m/abs val) max-u) i))
                      u))
      r-max-u (* max-idx h)]
  (println (format "   Energy: %.3f MeV (50 keV)" E))
  (println (format "   Maximum wavefunction at r = %.2f fm" r-max-u))
  (println (format "   Wavefunction extends to r = %.1f fm" r-max))
  (println ""))

;; ============================================================================
;; Example 6: Adaptive Matching Radius
;; ============================================================================
(println "6. Adaptive Matching Radius Comparison")
(println "   Shows how matching radius adapts to binding energy\n")

(let [cases [{:name "Tightly bound" :E-b 5.0 :R 2.0 :mu 869.4}
             {:name "Halo nucleus" :E-b 0.3 :R 2.0 :mu 869.4}
             {:name "Very weak halo" :E-b 0.1 :R 2.0 :mu 869.4}]]
  (doseq [case cases]
    (let [r-match (halo/adaptive-matching-radius
                   (:R case) (:E-b case) (:mu case))
          kappa-inv (halo/decay-length (:E-b case) (:mu case))]
      (println (format "   %s (E_b = %.2f MeV):" (:name case) (:E-b case)))
      (println (format "      Decay length: %.2f fm" kappa-inv))
      (println (format "      Matching radius: %.2f fm" r-match))
      (println ""))))

;; ============================================================================
;; Example 7: Transfer Reaction (d,p) for ¹¹Be
;; ============================================================================
(println "7. Transfer Reaction: ¹⁰Be(d,p)¹¹Be (POST formulation)")
(println "   Demonstrates use of global optical potentials:")
(println "   - CH89 (Chapel Hill 89) for proton exit channel (automatic)")
(println "   - Daehnick 1980 for deuteron entrance channel (automatic)\n")

(let [;; Reaction: ¹⁰Be(d,p)¹¹Be
      ;; Entrance channel: deuteron + ¹⁰Be
      ;; Exit channel: proton + ¹¹Be
      
      ;; Reaction parameters
      E-lab 10.0 ; Lab energy (MeV)
      Q-value 4.49 ; Q-value for ¹⁰Be(d,p)¹¹Be (MeV)
      E-exit (- E-lab Q-value) ; Exit channel energy
      
      ;; Target and residual
      residual-A 11 ; ¹¹Be
      residual-Z 4
      
      ;; Angular momenta
      L-i 0 ; Entrance channel
      L-f 0 ; Exit channel
      
      ;; Numerical parameters
      h 0.01
      r-max 30.0
      
      ;; Get bound state wavefunction for ¹¹Be (final state - neutron bound in ¹¹Be)
      ;; The wavefunction is already normalized
      bound-state-result (halo/example-11be)
      phi-f (:wavefunction bound-state-result)
      
      ;; For initial bound state (neutron bound in deuteron), use a simple approximation
      ;; In a full calculation, this would be the deuteron wavefunction
      ;; Here we use a simplified exponential form: phi-i ≈ exp(-κ_d r) for l=0
      ;; where κ_d = √(2μ_d E_bd) / ℏ, with E_bd ≈ 2.225 MeV (deuteron binding)
      E-bd 2.225 ; Deuteron binding energy (MeV)
      mu-d 469.5 ; Reduced mass for n-p system (MeV/c²)
      kappa-d (Math/sqrt (/ (* 2.0 mu-d E-bd) (* halo/hbarc halo/hbarc)))
      n-steps (int (/ r-max h))
      phi-i (vec (map (fn [i]
                       (let [r (* i h)]
                         (if (zero? r)
                           0.0
                           (* r (Math/exp (* (- kappa-d) r))))))
                     (range n-steps)))
      ;; Normalize phi-i using the same method as normalize-bound-state
      phi-i-norm (halo/normalize-bound-state phi-i h)
      
      ;; Calculate distorted waves with automatic global potential selection
      ;; Proton exit channel automatically uses CH89
      chi-f (inel/distorted-wave-exit E-lab Q-value L-f nil h r-max
                                      :outgoing-type :p
                                      :residual-A residual-A
                                      :residual-Z residual-Z
                                      :E-lab E-exit
                                      :s 0.5)
      
      ;; Deuteron entrance channel automatically uses Daehnick80
      chi-i (inel/distorted-wave-entrance E-lab L-i nil h r-max
                                          :projectile-type :d
                                          :target-A 10
                                          :target-Z 4
                                          :E-lab E-lab
                                          :s 1.0)
      
      ;; Zero-range constant for (d,p) reaction
      D0 (transfer/zero-range-constant :d-p)
      
      ;; Transfer amplitude (POST formulation)
      ;; Note: Ensure all vectors have the same length
      min-len (min (count chi-i) (count chi-f) (count phi-i-norm) (count phi-f))
      T-amplitude (transfer/transfer-amplitude-post
                   (vec (take min-len chi-i))
                   (vec (take min-len chi-f))
                   (vec (take min-len phi-i-norm))
                   (vec (take min-len phi-f))
                   r-max h
                   :zero-range D0)
      
      ;; Calculate cross-section
      ;; Mass factors (2μ/ħ²) for entrance and exit channels
      ;; Entrance: deuteron + ¹⁰Be
      mu-i 1869.0 ; Reduced mass for d + ¹⁰Be (MeV/c², approximate)
      mass-factor-i (/ (* 2.0 mu-i) (* halo/hbarc halo/hbarc))
      ;; Exit: proton + ¹¹Be
      mu-f 869.4 ; Reduced mass for p + ¹¹Be (MeV/c², approximate)
      mass-factor-f (/ (* 2.0 mu-f) (* halo/hbarc halo/hbarc))
      
      ;; Wavenumbers (fm⁻¹)
      k-i (Math/sqrt (* mass-factor-i E-lab))
      k-f (Math/sqrt (* mass-factor-f E-exit))
      
      ;; Spectroscopic factor (typical value for single-particle transfer)
      S-factor 1.0
      
      ;; Calculate differential cross-section (fm²/sr)
      dsigma (transfer/transfer-differential-cross-section
              T-amplitude S-factor k-i k-f mass-factor-i mass-factor-f)
      
      ;; Convert to mb/sr (1 fm² = 10 mb)
      dsigma-mb (* dsigma 10.0)
      
      ;; Calculate cross-section at different angles (for table)
      ;; For L=0 transfer, angular distribution is isotropic (constant)
      angles (range 0 181 15) ; 0 to 180 degrees in 15° steps
      cross-sections (map (fn [theta-deg]
                           {:angle theta-deg :dsigma dsigma-mb})
                         angles)]
  
  (println (format "   Lab Energy: %.1f MeV" E-lab))
  (println (format "   Q-value: %.2f MeV" Q-value))
  (println (format "   Exit Channel Energy: %.2f MeV" E-exit))
  (println (format "   Transfer Amplitude: %.4e" (mag T-amplitude)))
  (println "   Note: Global potentials automatically selected:")
  (println "        - CH89 for proton exit channel")
  (println "        - Daehnick80 for deuteron entrance channel")
  (println "")
  (println "   Differential Cross-Section (dσ/dΩ):")
  (println "   " (apply str (repeat 50 "-")))
  (println (format "   %6s  %12s  %12s" "θ (deg)" "dσ/dΩ (mb/sr)" "dσ/dΩ (fm²/sr)"))
  (println "   " (apply str (repeat 50 "-")))
  (doseq [cs cross-sections]
    (println (format "   %6.0f  %12.4e  %12.4e"
                    (:angle cs)
                    (:dsigma cs)
                    (/ (:dsigma cs) 10.0))))
  (println "   " (apply str (repeat 50 "-")))
  (println (format "   Note: For L=0 transfer, angular distribution is isotropic"))
  (println (format "   Total cross-section (estimated): σ ≈ %.2f mb"
                   (* 4.0 Math/PI dsigma-mb)))
  (println ""))

(println "=== Calculations Complete ===")
(println "\nNote: These examples demonstrate the key features of the")
(println "extended Bessel-Numerov method for halo nuclei. For production")
(println "calculations, ensure proper convergence testing and validation")
(println "against experimental data.")
