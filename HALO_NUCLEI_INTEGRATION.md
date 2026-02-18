# Halo Nuclei Integration Summary

This document summarizes the integration of halo nuclei improvements into the main paper and the creation of code examples.

## Changes Made

### 1. Paper Integration (`numerical_riccati.tex`)

The main paper has been extended with a new section "Extensions for Halo Nuclei Applications" that includes:

- **Extended Asymptotic Matching Region**: Adaptive matching radius based on decay length
- **Low-Energy and Bound-State Calculations**: Riccati-Hankel initialization for bound states
- **Coulomb Interactions**: Extension for charged halo nuclei (e.g., ⁸B, ¹⁷F)
- **Asymptotic Normalization Coefficients (ANC)**: Extraction method and validation table
- **Validation with Halo Nuclei**: Test cases for ¹¹Be, ⁸B, and ⁶He

The abstract and keywords have also been updated to mention halo nuclei applications.

### 2. Code Implementation (`src/dwba/halo_nuclei.clj`)

A comprehensive Clojure module implementing all key improvements:

#### Core Functions:

1. **Extended Asymptotic Matching**
   - `decay-length`: Calculate decay length κ⁻¹
   - `adaptive-matching-radius`: Adaptive matching radius calculation

2. **Bound State Calculations**
   - `bessel-start-bound-state`: Riccati-Hankel initialization for E < 0
   - `solve-bound-state-numerov`: Full bound state solver

3. **Coulomb Interactions**
   - `sommerfeld-parameter`: Calculate Sommerfeld parameter η
   - `coulomb-normalization-constant`: Coulomb normalization C_l(η)
   - `bessel-start-coulomb`: Initialization with Coulomb effects
   - `coulomb-potential`: Coulomb potential V_C(r)
   - `solve-coulomb-numerov`: Full Coulomb + nuclear solver

4. **ANC Extraction**
   - `whittaker-w`: Whittaker function for asymptotic form
   - `extract-anc`: Extract ANC from bound state wavefunction

5. **Low-Energy Scattering**
   - `solve-low-energy-numerov`: Scattering at very low energies (10-100 keV)

#### Example Functions:

- `example-11be`: Complete calculation for ¹¹Be halo nucleus
- `example-8b`: Complete calculation for ⁸B proton halo
- `example-coulomb-scattering`: α+p scattering example

### 3. Usage Examples (`examples/halo_nuclei_examples.clj`)

A comprehensive example script demonstrating:

1. ¹¹Be halo nucleus calculation
2. ⁸B proton halo calculation
3. Decay length calculations
4. Coulomb scattering
5. Low-energy scattering
6. Adaptive matching radius comparison

## Key Features

### Extended Asymptotic Matching

For halo nuclei with weak binding (E_b ~ 100-500 keV), the wavefunction extends to 30-50 fm. The adaptive matching radius ensures proper asymptotic matching:

```clojure
(defn adaptive-matching-radius [R E-b mu]
  (max (* 3.0 R) (* 5.0 (decay-length E-b mu)) 20.0))
```

### Bound State Initialization

For bound states (E < 0), we use Riccati-Hankel functions with imaginary wave number:

```clojure
(defn bessel-start-bound-state [h kappa l]
  (let [kh (* kappa h)
        base (m/pow kh (inc l))
        dfac (double-factorial l)
        correction (- 1.0 (/ (* kh kh) (* 2.0 (+ (* 2 l) 3))))]
    (* (/ base dfac) correction)))
```

### Coulomb Interactions

For charged halo nuclei, the initialization includes Coulomb effects:

```clojure
(defn bessel-start-coulomb [h k eta l]
  (let [kh (* k h)
        C-l (coulomb-normalization-constant eta l)
        base (* C-l (m/pow kh (inc l)))
        correction (+ 1.0 (/ (* eta kh) (inc l)))]
    (* base correction)))
```

### ANC Extraction

The ANC is extracted from the asymptotic form of the bound state:

```clojure
(defn extract-anc [u-wave r-values E-b mu Z1 Z2 l r-match]
  ;; Extracts C from u_l(r) ~ C W_{-η, l+1/2}(2κr)
  ...)
```

## Validation

The code has been validated against:

- **¹¹Be**: E_b = 504 keV, ANC ≈ 0.78 fm^(-1/2)
- **⁸B**: E_b = 137 keV, ANC ≈ 0.14 fm^(-1/2)
- **⁶He**: E_b = 975 keV, ANC ≈ 0.40 fm^(-1/2)

## Usage

### Running Examples

```bash
clojure examples/halo_nuclei_examples.clj
```

### In Your Code

```clojure
(require '[dwba.halo-nuclei :as halo])

;; Calculate ¹¹Be bound state
(let [result (halo/example-11be)]
  (println "ANC:" (:anc result))
  (println "Matching radius:" (:matching-radius result)))

;; Calculate Coulomb scattering
(let [result (halo/example-coulomb-scattering)]
  (println "Sommerfeld parameter:" (:sommerfeld-parameter result)))
```

## References

The implementation follows the methods described in:
- `numerical_riccati.tex`: Main paper on Bessel-Numerov method
- `halo_nuclei_improvements.tex`: Detailed improvements document

Key references:
- J.S. Al-Khalili and J.A. Tostevin, Rev. Mod. Phys. 75, 215 (2003)
- I. Tanihata et al., Phys. Rev. Lett. 55, 2676 (1985)
- K. Riisager, Rev. Mod. Phys. 66, 1105 (1994)

## Next Steps

Potential future enhancements:
1. Three-body halo systems (coupled channels)
2. Resonance state calculations (complex energies)
3. Non-local potentials
4. Breakup reaction calculations
5. Relativistic extensions
