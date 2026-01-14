# How to Use Bound State Wavefunctions for Form Factors

## Overview

To calculate form factors (overlap integrals) for transfer reactions, you need the single-particle wavefunctions φ_i and φ_f. These are obtained using the `solve-bound-state` function from `dwba.transfer`.

## Getting φ_i and φ_f

### Step 1: Solve for Initial State (φ_i)

For a reaction like **A(d,p)B**, φ_i is the wavefunction of the nucleon in the initial state (e.g., in the deuteron):

```clojure
(require '[dwba.transfer :as t])
(require '[dwba.form-factors :as ff])

;; Example: φ_i = neutron in deuteron
;; Use deuteron potential parameters
(def V-params-deuteron [50.0 2.0 0.6])  ; [V0, R0, a0] in MeV, fm, fm

;; Solve for 1s bound state (n=1 means 0 radial nodes)
(def phi-i-result (t/solve-bound-state V-params-deuteron 1 0 nil 20.0 0.01))

;; Extract the normalized wavefunction
(def phi-i (:normalized-wavefunction phi-i-result))
;; phi-i is now a vector of wavefunction values u(r)
```

### Step 2: Solve for Final State (φ_f)

φ_f is the wavefunction of the nucleon bound in the final nucleus:

```clojure
;; Example: φ_f = neutron bound in final nucleus B
;; Use potential parameters for nucleus B
(def V-params-final [60.0 2.5 0.6])  ; Different potential for final nucleus

;; Solve for bound state (e.g., 1s state)
(def phi-f-result (t/solve-bound-state V-params-final 1 0 nil 20.0 0.01))

;; Extract the normalized wavefunction
(def phi-f (:normalized-wavefunction phi-f-result))
```

### Step 3: Calculate Overlap Integral

Now you can calculate the form factor (overlap integral):

```clojure
;; Calculate overlap integral
(def overlap (ff/overlap-integral phi-i phi-f 20.0 0.01))

;; Or get the normalized overlap coefficient
(def normalized-overlap (ff/normalized-overlap phi-i phi-f 20.0 0.01))

;; Or get the form factor function F(r) for plotting
(def form-factor-fn (ff/form-factor-function phi-i phi-f 0.01))
```

## Complete Example

```clojure
(require '[dwba.transfer :as t])
(require '[dwba.form-factors :as ff])

;; Define potentials for initial and final states
(def V-i [50.0 2.0 0.6])  ; Initial state potential
(def V-f [60.0 2.5 0.6])  ; Final state potential

;; Solve for bound states
(def result-i (t/solve-bound-state V-i 1 0 nil 20.0 0.01))
(def result-f (t/solve-bound-state V-f 1 0 nil 20.0 0.01))

;; Extract wavefunctions
(def phi-i (:normalized-wavefunction result-i))
(def phi-f (:normalized-wavefunction result-f))

;; Calculate overlap integral
(def overlap (ff/overlap-integral phi-i phi-f 20.0 0.01))

(println "Overlap integral:" overlap)
(println "Initial state energy:" (:energy result-i))
(println "Final state energy:" (:energy result-f))
```

## Important Notes

1. **Normalized Wavefunctions**: Always use `:normalized-wavefunction` from `solve-bound-state`, not `:wavefunction`. The normalized version satisfies ∫|u|²r²dr = 1.

2. **Same Grid**: Both wavefunctions should use the same `r-max` and `h` parameters for accurate integration.

3. **Quantum Numbers**: 
   - `n` = number of radial nodes + 1 (so n=1 means 0 nodes, n=2 means 1 node, etc.)
   - `l` = orbital angular momentum (0=s, 1=p, 2=d, ...)
   - `j` = total angular momentum (can be nil if spin-orbit not included)

4. **Potential Parameters**: The potential parameters [V0, R0, a0] should match the physical system:
   - For deuteron: typically V0 ≈ 50-70 MeV, R0 ≈ 2.0 fm
   - For bound states in nuclei: depends on the specific nucleus

## Using in Transfer Reactions

For a (d,p) reaction:
- **φ_i**: Neutron in deuteron → use deuteron potential
- **φ_f**: Neutron bound in final nucleus → use potential for that nucleus

The overlap integral O = ∫ φ*_f(r) φ_i(r) r² dr is then used in the transfer amplitude calculation.

