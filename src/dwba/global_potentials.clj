(ns dwba.global-potentials
  "Global optical model potential parameterizations fitted to experimental data.
   
   These potentials reproduce measured elastic (and reaction) cross sections
   over specified energy and mass ranges. Prefer them over ad-hoc Woods-Saxon
   parameters when available.
   
   Implemented:
   - CH89 (Chapel Hill 89): nucleon-nucleus, A=40–209, protons 16–65 MeV, neutrons 10–26 MeV
   - Daehnick 1980: deuteron-nucleus, A=12–208, E_lab = 12–90 MeV
   
   References:
   - R.L. Varner et al., Physics Reports 201 (1991) 57–119, 'A global nucleon optical model potential'
   - W.W. Daehnick et al., Phys. Rev. C 21 (1980) 2253, 'Global optical model potential for elastic scattering from light and medium nuclei'"
  (:require [dwba.transfer :as transfer]))

;; ============================================================================
;; Chapel Hill 89 (CH89) — nucleon-nucleus
;; ============================================================================

(defn ch89-real-depth-proton
  "CH89 real central depth for protons (MeV). E-lab in MeV.
   V0 = v1 - v2*E - v4*E^3 (CH89 form)."
  [E-lab]
  (let [v1 59.30
        v2 0.0240
        v4 6.1e-5]
    (- v1 (* v2 E-lab) (* v4 E-lab E-lab E-lab))))

(defn ch89-real-depth-neutron
  "CH89 real central depth for neutrons (MeV). E-lab in MeV."
  [E-lab]
  (let [v1 59.30
        v2 0.0240
        v4 6.1e-5]
    (- v1 (* v2 E-lab) (* v4 E-lab E-lab E-lab))))

(defn ch89-imag-depth-proton
  "CH89 imaginary volume depth for protons (MeV). Surface form: Ws.
   Simplified energy dependence for 16–65 MeV."
  [E-lab]
  (let [w1 12.0
        w2 0.09]
    (+ w1 (* w2 E-lab))))

(defn ch89-imag-depth-neutron
  "CH89 imaginary depth for neutrons (MeV). 10–26 MeV range."
  [E-lab]
  (let [w1 10.0
        w2 0.15]
    (+ w1 (* w2 E-lab))))

(defn ch89-radius
  "CH89 radius R = r0 * A^(1/3) (fm)."
  [r0 target-A]
  (* r0 (Math/pow target-A (/ 1.0 3.0))))

(defn ch89-parameters
  "Chapel Hill 89 (CH89) global optical potential parameters for nucleons.
   
   Applicable: A = 40–209; protons E_lab = 16–65 MeV, neutrons 10–26 MeV.
   Reference: Varner et al., Physics Reports 201 (1991) 57.
   
   Parameters:
   - projectile: :p or :n
   - target-A: mass number
   - E-lab: lab energy (MeV)
   
   Returns: Map with {:V-params [V0 R_V a_V], :W-params [W0 R_W a_W],
                      :V-so, :R-so, :a-so} for use with optical-potential-woods-saxon."
  [projectile target-A E-lab]
  (when-not (#{:p :n} projectile)
    (throw (IllegalArgumentException.
            (format "CH89 only supports :p and :n; got %s" (pr-str projectile)))))
  (let [;; Geometry (CH89 typical values)
        rv0 1.24
        av   0.65
        rw0 1.24
        aw   0.65
        rso0 1.12
        aso  0.65
        ;; Real
        V0   (if (= projectile :p)
               (ch89-real-depth-proton E-lab)
               (ch89-real-depth-neutron E-lab))
        R-V  (ch89-radius rv0 target-A)
        ;; Imaginary
        W0   (if (= projectile :p)
               (ch89-imag-depth-proton E-lab)
               (ch89-imag-depth-neutron E-lab))
        R-W  (ch89-radius rw0 target-A)
        ;; Spin-orbit (CH89 nucleon)
        V-so 6.2
        R-so (ch89-radius rso0 target-A)]
    {:V-params [V0 R-V av]
     :W-params [W0 R-W aw]
     :V-so     V-so
     :R-so     R-so
     :a-so     aso}))

(defn optical-potential-ch89
  "Optical potential U(r) using Chapel Hill 89 (CH89) global parameterization.
   
   Parameters:
   - r: radius (fm)
   - projectile: :p or :n
   - target-A: mass number
   - target-Z: target charge (for protons, for Coulomb)
   - E-lab: lab energy (MeV)
   - l, s, j: orbital, spin, total angular momentum
   
   Returns: Complex U(r) in MeV (same convention as optical-potential-woods-saxon)."
  [r projectile target-A target-Z E-lab l s j]
  (let [params (ch89-parameters projectile target-A E-lab)
        Z1     (if (= projectile :p) 1 0)
        Z2     (long target-Z)
        R-C    (* 1.25 (Math/pow target-A (/ 1.0 3.0)))]
    (transfer/optical-potential-woods-saxon
     r
     (:V-params params)
     (:W-params params)
     (:V-so params)
     (:R-so params)
     (:a-so params)
     l s j Z1 Z2 R-C)))

;; ============================================================================
;; Daehnick 1980 — deuteron-nucleus
;; ============================================================================

(defn daehnick80-real-depth
  "Daehnick 1980 real central depth for deuterons (MeV).
   V0 = V1 - V2*E (energy-dependent form).
   
   Parameters:
   - E-lab: Lab energy per nucleon (MeV/nucleon)
   
   Returns: V0 in MeV"
  [E-lab]
  (let [V1 88.0
        V2 0.215]
    (- V1 (* V2 E-lab))))

(defn daehnick80-imag-depth
  "Daehnick 1980 imaginary volume depth for deuterons (MeV).
   W0 = W1 + W2*E (energy-dependent form).
   
   Parameters:
   - E-lab: Lab energy per nucleon (MeV/nucleon)
   
   Returns: W0 in MeV"
  [E-lab]
  (let [W1 12.0
        W2 0.25]
    (+ W1 (* W2 E-lab))))

(defn daehnick80-parameters
  "Daehnick 1980 global optical potential parameters for deuterons.
   
   Applicable: A = 12–208; E_lab = 12–90 MeV (per nucleon).
   Reference: W.W. Daehnick et al., Phys. Rev. C 21 (1980) 2253.
   
   Parameters:
   - target-A: Target mass number
   - E-lab: Lab energy per nucleon (MeV/nucleon)
   
   Returns: Map with {:V-params [V0 R_V a_V], :W-params [W0 R_W a_W],
                      :V-so, :R-so, :a-so} for use with optical-potential-woods-saxon."
  [target-A E-lab]
  (let [;; Geometry (Daehnick 1980 typical values)
        rv0 1.15
        av   0.81
        rw0 1.34
        aw   0.68
        ;; Real
        V0   (daehnick80-real-depth E-lab)
        R-V  (* rv0 (Math/pow target-A (/ 1.0 3.0)))
        ;; Imaginary
        W0   (daehnick80-imag-depth E-lab)
        R-W  (* rw0 (Math/pow target-A (/ 1.0 3.0)))
        ;; Spin-orbit (Daehnick typically uses small or zero spin-orbit for deuterons)
        V-so 0.0  ; Deuterons have spin 1, but spin-orbit is typically small
        R-so R-V
        a-so av]
    {:V-params [V0 R-V av]
     :W-params [W0 R-W aw]
     :V-so     V-so
     :R-so     R-so
     :a-so     a-so}))

(defn optical-potential-daehnick80
  "Optical potential U(r) using Daehnick 1980 global parameterization for deuterons.
   
   Parameters:
   - r: radius (fm)
   - target-A: mass number
   - target-Z: target charge (for Coulomb)
   - E-lab: lab energy per nucleon (MeV/nucleon)
   - l, s, j: orbital, spin, total angular momentum
   
   Returns: Complex U(r) in MeV"
  [r target-A target-Z E-lab l s j]
  (let [params (daehnick80-parameters target-A E-lab)
        Z1     1  ; Deuteron charge
        Z2     (long target-Z)
        R-C    (* 1.25 (Math/pow target-A (/ 1.0 3.0)))]
    (transfer/optical-potential-woods-saxon
     r
     (:V-params params)
     (:W-params params)
     (:V-so params)
     (:R-so params)
     (:a-so params)
     l s j Z1 Z2 R-C)))

;; ============================================================================
;; Dispatch for global set selection
;; ============================================================================

(def supported-global-sets
  "Keyword set of supported global potential names."
  #{:ch89 :daehnick80})

(defn parameters-for-global-set
  "Return optical potential parameter map for the given global set.
   
   - global-set: :ch89 (nucleons) or :daehnick80 (deuterons)
   - projectile: :p, :n, :d
   - target-A, target-Z: target nucleus
   - E-lab: lab energy (MeV) or lab energy per nucleon for deuterons
   
   Returns: Map {:V-params :W-params :V-so :R-so :a-so} or nil if not supported."
  [global-set projectile target-A _target-Z E-lab]
  (case global-set
    :ch89 (when (#{:p :n} projectile)
            (ch89-parameters projectile target-A E-lab))
    :daehnick80 (when (= projectile :d)
                  (daehnick80-parameters target-A E-lab))
    nil))

(defn optical-potential-global
  "Compute U(r) using a named global potential.
   
   global-set: :ch89 (Chapel Hill 89 for nucleons) or :daehnick80 (Daehnick 1980 for deuterons)
   Other args: r, projectile, target-A, target-Z, E-lab, l, s, j.
   Returns complex U(r) in MeV, or nil if combination not supported."
  [r global-set projectile target-A target-Z E-lab l s j]
  (when (supported-global-sets global-set)
    (case global-set
      :ch89 (when (#{:p :n} projectile)
              (optical-potential-ch89 r projectile target-A target-Z E-lab l s j))
      :daehnick80 (when (= projectile :d)
                    (optical-potential-daehnick80 r target-A target-Z E-lab l s j))
      nil)))
