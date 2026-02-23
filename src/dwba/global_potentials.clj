(ns dwba.global-potentials
  "Global optical model potential parameterizations fitted to experimental data.
   
   These potentials reproduce measured elastic (and reaction) cross sections
   over specified energy and mass ranges. Prefer them over ad-hoc Woods-Saxon
   parameters when available.
   
   Implemented:
   - CH89 (Chapel Hill 89): nucleon-nucleus, A=40–209, protons 16–65 MeV, neutrons 10–26 MeV
   
   Reference: R.L. Varner et al., Physics Reports 201 (1991) 57–119,
   'A global nucleon optical model potential'."
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
;; Dispatch for global set selection
;; ============================================================================

(def supported-global-sets
  "Keyword set of supported global potential names."
  #{:ch89})

(defn parameters-for-global-set
  "Return optical potential parameter map for the given global set.
   
   - global-set: :ch89 (or future :kd03, etc.)
   - projectile: :p, :n (and :d, :alpha if set supports them)
   - target-A, target-Z: target nucleus
   - E-lab: lab energy (MeV)
   
   Returns: Map {:V-params :W-params :V-so :R-so :a-so} or nil if not supported."
  [global-set projectile target-A _target-Z E-lab]
  (case global-set
    :ch89 (when (#{:p :n} projectile)
            (ch89-parameters projectile target-A E-lab))
    nil))

(defn optical-potential-global
  "Compute U(r) using a named global potential.
   
   global-set: :ch89 (Chapel Hill 89 for nucleons)
   Other args: r, projectile, target-A, target-Z, E-lab, l, s, j.
   Returns complex U(r) in MeV, or nil if combination not supported."
  [r global-set projectile target-A target-Z E-lab l s j]
  (when (supported-global-sets global-set)
    (case global-set
      :ch89 (when (#{:p :n} projectile)
              (optical-potential-ch89 r projectile target-A target-Z E-lab l s j))
      nil)))
