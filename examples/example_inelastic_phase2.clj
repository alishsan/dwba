;; Example: Phase 2 - Coupled Channels Framework for Inelastic Scattering
;;
;; This demonstrates the use of coupled channels calculations for inelastic scattering.

(require '[dwba.inelastic :as inel])
(require '[functions :refer [mass-factor]])

;; ============================================================================
;; Example 1: Define Channels
;; ============================================================================

(println "=== Example 1: Define Channels ===")

;; Define ground state channel (elastic channel)
(def ch0 (inel/channel-definition 0 0.0 0 nil [50.0 2.0 0.6]))
(println "Ground state channel:" ch0)

;; Define first excited state (2⁺ state at 4.44 MeV in ¹²C)
(def ch1 (inel/channel-definition 1 4.44 2 nil [50.0 2.0 0.6]))
(println "First excited state (2⁺):" ch1)

;; Define second excited state (3⁻ state at 9.64 MeV in ¹²C)
(def ch2 (inel/channel-definition 2 9.64 3 nil [50.0 2.0 0.6]))
(println "Second excited state (3⁻):" ch2)
(println "")

;; ============================================================================
;; Example 2: Channel Energies
;; ============================================================================

(println "=== Example 2: Channel Energies ===")

(def E-incident 10.0)  ; Incident energy in MeV

(println "Incident energy:" E-incident "MeV")
(println "Ground state channel energy:" (inel/channel-energy E-incident ch0) "MeV")
(println "First excited state channel energy:" (inel/channel-energy E-incident ch1) "MeV")
(println "Second excited state channel energy:" (inel/channel-energy E-incident ch2) "MeV")
(println "")

;; ============================================================================
;; Example 3: Coupling Matrix Elements
;; ============================================================================

(println "=== Example 3: Coupling Matrix Elements ===")

;; Define coupling between ground state and first excited state
;; Quadrupole (λ=2) transition with β_2 = 0.25
(def beta-2 0.25)
(def coupling-01 {:from 0, :to 1, :lambda 2, :beta beta-2, :strength 1.0})

(println "Coupling 0→1 (quadrupole):" coupling-01)

;; Calculate coupling potential at different radii
(println "Coupling potential V_01(r):")
(doseq [r [1.0 2.0 3.0 5.0]]
  (let [V-coupling (inel/coupling-matrix-element r ch0 ch1 2 beta-2 1.0)]
    (println (format "  V_01(%.1f fm) = %.4f MeV" r V-coupling))))
(println "")

;; ============================================================================
;; Example 4: Potential Matrix
;; ============================================================================

(println "=== Example 4: Potential Matrix ===")

;; Two-channel system: ground state + first excited state
(def channels-2ch [ch0 ch1])
(def couplings-2ch [coupling-01])

(println "Two-channel system:")
(println "  Channel 0: Ground state (L=0)")
(println "  Channel 1: First 2⁺ state (L=2, E_ex=4.44 MeV)")

;; Calculate potential matrix at r=2.0 fm
(def V-matrix (inel/coupled-channels-potential-matrix 2.0 channels-2ch couplings-2ch E-incident))
(println "Potential matrix V(r=2.0 fm):")
(doseq [i (range (count V-matrix))]
  (doseq [j (range (count (nth V-matrix i)))]
    (println (format "  V[%d,%d] = %.4f MeV" i j (get-in V-matrix [i j])))))
(println "")

;; ============================================================================
;; Example 5: Solve Coupled Channels
;; ============================================================================

(println "=== Example 5: Solve Coupled Channels ===")

;; Solve coupled channels equations
(def h 0.01)  ; Step size (fm)
(def r-max 20.0)  ; Maximum radius (fm)
(def mass-fac mass-factor)  ; Mass factor from functions namespace

(println "Solving coupled channels system...")
(println "  Step size: h =" h "fm")
(println "  Maximum radius: r_max =" r-max "fm")
(println "  Incident energy: E =" E-incident "MeV")

(def coupled-solution (inel/solve-coupled-channels-numerov 
                       channels-2ch couplings-2ch E-incident mass-fac h r-max))

(println "Solution obtained!")
(println "  Number of channels:" (count coupled-solution))
(println "  Wavefunction length per channel:" (count (first coupled-solution)))
(println "")

;; ============================================================================
;; Example 6: Extract Channel Wavefunctions
;; ============================================================================

(println "=== Example 6: Extract Channel Wavefunctions ===")

;; Extract wavefunctions for each channel
(def u0 (inel/channel-wavefunction coupled-solution 0))
(def u1 (inel/channel-wavefunction coupled-solution 1))

(println "Ground state wavefunction (channel 0):")
(println "  Length:" (count u0))
(println "  First few values:" (take 5 u0))
(println "  Value at r=2.0 fm:" (nth u0 (int (/ 2.0 h))))

(println "Excited state wavefunction (channel 1):")
(println "  Length:" (count u1))
(println "  First few values:" (take 5 u1))
(println "  Value at r=2.0 fm:" (nth u1 (int (/ 2.0 h))))
(println "")

;; ============================================================================
;; Example 7: Three-Channel System
;; ============================================================================

(println "=== Example 7: Three-Channel System ===")

;; Three-channel system: ground state + two excited states
(def channels-3ch [ch0 ch1 ch2])
(def coupling-02 {:from 0, :to 2, :lambda 3, :beta 0.1, :strength 1.0})
(def couplings-3ch [coupling-01 coupling-02])

(println "Three-channel system:")
(println "  Channel 0: Ground state (L=0)")
(println "  Channel 1: First 2⁺ state (L=2, E_ex=4.44 MeV)")
(println "  Channel 2: First 3⁻ state (L=3, E_ex=9.64 MeV)")

;; Calculate potential matrix
(def V-matrix-3ch (inel/coupled-channels-potential-matrix 2.0 channels-3ch couplings-3ch E-incident))
(println "Potential matrix V(r=2.0 fm) [3x3]:")
(doseq [i (range 3)]
  (println (format "  [%.4f %.4f %.4f]" 
                  (get-in V-matrix-3ch [i 0])
                  (get-in V-matrix-3ch [i 1])
                  (get-in V-matrix-3ch [i 2]))))
(println "")

(println "=== Examples Complete ===")
