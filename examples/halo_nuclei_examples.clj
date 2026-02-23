#!/usr/bin/env clojure
;; Example usage of halo nuclei extensions to Bessel-Numerov method
;;
;; This file demonstrates:
;; 1. Extended asymptotic matching for halo nuclei
;; 2. Bound state calculations (¹¹Be, ⁸B)
;; 3. Coulomb interactions for charged halo nuclei
;; 4. ANC extraction
;; 5. Low-energy scattering

(require '[dwba.halo-nuclei :as halo]
         '[fastmath.core :as m])

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

(println "=== Calculations Complete ===")
(println "\nNote: These examples demonstrate the key features of the")
(println "extended Bessel-Numerov method for halo nuclei. For production")
(println "calculations, ensure proper convergence testing and validation")
(println "against experimental data.")
