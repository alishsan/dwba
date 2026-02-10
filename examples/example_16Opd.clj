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
;;
;; Use (load-file "examples/example_16Opd.clj") or run from project root.
;; Own namespace avoids alias conflicts with dwba.core when loaded from REPL.

(ns examples.example-16Opd
  (:require [dwba.transfer :as t]
            [dwba.form-factors :as ff]
            [dwba.inelastic :as inel]
            [functions :refer [solve-numerov mass-factor]]
            [fastmath.core :as m]
            [complex :as cx :refer [mag re im complex-cartesian add mul]]
            [incanter.core :as i]
            [incanter.charts :as c]
            [clojure.java.io :as io]))

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
      ;; Distorted Waves and Transfer Amplitudes (multiple L for angular dependence)
      ;; ============================================================================
      
      ;; Lab energies for optical potential calculation
      E-lab-i E-lab  ; Entrance channel lab energy
      E-lab-f (* E-CM-f (/ (+ m-d m-15O) m-15O))  ; Exit channel lab energy (approximate)
      
      ;; Include partial waves L = 0 .. L-max so dσ/dΩ depends on angle (e.g. drop at 70°)
      L-max 7
      D0 (t/zero-range-constant :p-d)  ; D₀ for (p,d) reaction
      ;; Build T-amplitudes map {L → T_L} for L = 0, 1, ..., L-max
      T-amplitudes (into {}
                        (for [L (range (inc L-max))]
                          (let [chi-i (inel/distorted-wave-entrance E-CM-i L nil h r-max
                                                                    :projectile-type :p
                                                                    :target-A 16
                                                                    :target-Z 8
                                                                    :E-lab E-lab-i
                                                                    :s 0.5
                                                                    :j (+ L 0.5)
                                                                    :mass-factor mass-factor-i)
                                chi-f (inel/distorted-wave-exit E-CM-i Q-value L nil h r-max
                                                                :outgoing-type :d
                                                                :residual-A 15
                                                                :residual-Z 8
                                                                :E-lab E-lab-f
                                                                :s 1
                                                                :j (inc L)
                                                                :mass-factor mass-factor-f)
                                T-L (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h
                                                               :zero-range D0)]
                            [L T-L])))
      
      ;; Keep L=0 waves for display (Step 3)
      L-i 0
      L-f 0
      chi-i (inel/distorted-wave-entrance E-CM-i L-i nil h r-max
                                          :projectile-type :p
                                          :target-A 16
                                          :target-Z 8
                                          :E-lab E-lab-i
                                          :s 0.5
                                          :j 0.5
                                          :mass-factor mass-factor-i)
      chi-f (inel/distorted-wave-exit E-CM-i Q-value L-f nil h r-max
                                      :outgoing-type :d
                                      :residual-A 15
                                      :residual-Z 8
                                      :E-lab E-lab-f
                                      :s 1
                                      :j 1
                                      :mass-factor mass-factor-f)
      T-post (get T-amplitudes 0)  ; Same as T for L=0
      
      ;; ============================================================================
      ;; Differential Cross Section
      ;; ============================================================================
      
      ;; Wavenumbers
      k-i (Math/sqrt (* mass-factor-i E-CM-i))
      k-f (Math/sqrt (* mass-factor-f E-CM-f))
      
      ;; Spectroscopic factor (typically 0 < S < 1, using 1.0 for this example)
      S-factor 1.0
      theta-deg 0.0  ; Forward angle (degrees)
      theta-rad (* theta-deg (/ Math/PI 180.0))
      ;; Pass l-i, l-f so only allowed L contribute (L selection: triangle + parity)
      dsigma (t/transfer-differential-cross-section-angular T-amplitudes S-factor k-i k-f 
                                                           theta-rad mass-factor-i mass-factor-f 0.0 l-i l-f)
      ;; At 70° (compare to experiment: ~0.5 mb/sr at 20 MeV)
      theta-70-deg 70.0
      theta-70-rad (* theta-70-deg (/ Math/PI 180.0))
      dsigma-70 (t/transfer-differential-cross-section-angular T-amplitudes S-factor k-i k-f 
                                                               theta-70-rad mass-factor-i mass-factor-f 0.0 l-i l-f)
      ;; Angles and DCS for plotting (0° to 180°, step 5°), dσ/dΩ in mb/sr
      angles-deg (range 0.0 181.0 5.0)
      angles-rad (mapv #(* % (/ Math/PI 180.0)) angles-deg)
      dsigma-mb-sr (mapv (fn [theta-rad]
                           (* 10.0 (t/transfer-differential-cross-section-angular T-amplitudes S-factor k-i k-f
                                                                                  theta-rad mass-factor-i mass-factor-f 0.0 l-i l-f)))
                         angles-rad)
      ;; Same angle set as web dashboard: 20° to 160° step 20° (CM)
      angles-output-deg (range 20.0 181.0 20.0)
      dsigma-at-angles (mapv (fn [theta-deg]
                               (let [theta-rad (* theta-deg (/ Math/PI 180.0))
                                     dsigma-fm2 (t/transfer-differential-cross-section-angular T-amplitudes S-factor k-i k-f
                                                                                               theta-rad mass-factor-i mass-factor-f 0.0 l-i l-f)]
                                 (* 10.0 dsigma-fm2)))
                             angles-output-deg)]
  
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
  
  (println "=== Step 4: Transfer Amplitudes (Post Formulation) ===")
  (println (format "Zero-range constant: D₀ = %.2f MeV·fm^(3/2)" D0))
  (println (format "Partial waves included: L = 0 .. %d" L-max))
  (println "")
  (println "Transfer amplitude L=0 (post formulation):")
  (if (number? T-post)
    (println (format "  T_0 = %.6e" T-post))
    (println (format "  T_0 = %.6e + i%.6e" (re T-post) (im T-post))))
  (println (format "  |T_0| = %.6e" (if (number? T-post) (Math/abs T-post) (mag T-post))))
  (println "")
  (println "|T_L| for all L (used in angular distribution):")
  (doseq [L (range (inc L-max))]
    (let [T-L (get T-amplitudes L)
          mag-T (if (number? T-L) (Math/abs T-L) (mag T-L))]
      (println (format "  L = %2d: |T_L| = %.6e" L mag-T))))
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
  (println (format "  dσ/dΩ(θ=%.1f°) = %.6e fm²/sr" theta-70-deg dsigma-70))
  (println (format "  dσ/dΩ(θ=%.1f°) = %.6e mb/sr (experiment at 20 MeV, 70°: ~0.5 mb/sr)" theta-70-deg (* dsigma-70 10.0)))
  (println "")
  (println "DCS at different angles (CM, same as web dashboard; mb/sr):")
  (println "  Angle (deg) | dσ/dΩ (mb/sr)")
  (doseq [[theta-deg dsigma-mb] (map vector angles-output-deg dsigma-at-angles)]
    (println (format "  θ = %5.1f°   | %.6e" theta-deg dsigma-mb)))
  (println "")

  (println "=== Summary ===")
  (println (format "Reaction: 16O(p,d)15O"))
  (println (format "Incident energy: E_lab = %.2f MeV" E-lab))
  (println (format "Normalized overlap: %.6f" overlap-norm))
  (println (format "Transfer amplitude: |T_post| = %.6e"
                  (if (number? T-post) (Math/abs T-post) (mag T-post))))
  (println (format "Differential cross section: dσ/dΩ(0°) = %.6e mb/sr" (* dsigma 10.0)))
  (println (format "Differential cross section: dσ/dΩ(70°) = %.6e mb/sr" (* dsigma-70 10.0)))
  (println "")
  (println "=== Calculation Complete ===")

  ;; Plot DCS vs angle and save to file
  (try
    (let [_ (io/make-parents (io/file "output/16Opd_dcs.png"))
          chart (c/xy-plot (vec angles-deg) (vec dsigma-mb-sr)
                           :title "16O(p,d)15O — Differential cross section"
                           :x-label "θ (deg)"
                           :y-label "dσ/dΩ (mb/sr)"
                           :series-label (format "E_lab = %.1f MeV" E-lab)
                           :legend true)]
      (i/save chart "output/16Opd_dcs.png" :width 800 :height 500)
      (println "Plot saved: output/16Opd_dcs.png"))
    (catch Exception e
      (println (format "Note: Could not save plot (%s). Create output/ directory or check Incanter." (.getMessage e)))))

  ;; Return the differential cross section
  dsigma
  )

