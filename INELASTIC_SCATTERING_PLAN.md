# Plan for DWBA Inelastic Scattering

## Overview

This document outlines the plan to extend the current elastic scattering DWBA code to handle **inelastic scattering reactions** where the target nucleus is excited to higher energy states.

## Current State

### ✅ What We Have (Elastic Scattering)
- **Distorted wave calculations** - solving Schrödinger equation for scattering states
- **R-matrix and S-matrix** calculations
- **Phase shift analysis**
- **Coulomb interactions** - full implementation
- **Woods-Saxon potentials**
- **Numerov integration** - robust numerical solver
- **Kinematic transformations** - lab ↔ CM frame conversions

### ✅ What We Have (Transfer Reactions)
- **Bound state wavefunctions** - solving for bound states
- **Transfer form factors** - overlap integrals between bound states
- **Zero-range and finite-range interactions**

### ❌ What We Need (Inelastic Scattering)
- **Transition form factors** - deformation parameters for nuclear excitations
- **Coupled channels calculations** - coupling between different channels
- **Collective model** - rotational and vibrational excitations
- **Single-particle excitations** - particle-hole excitations
- **Reduced matrix elements** - B(Eλ) values for electromagnetic transitions
- **Inelastic cross-section formula** - different from elastic scattering
- **Angular momentum coupling** - for excited states

## Physics Background

### Inelastic Scattering

Inelastic scattering has the form: **A(a,a')A*** where:
- **A** is the target nucleus in ground state
- **A*** is the target nucleus in an excited state
- **a** is the projectile (unchanged)
- Example: **¹²C(α,α')¹²C*** means alpha particle excites carbon-12

### Key Differences from Elastic Scattering

1. **Excited States**: The target nucleus is excited to higher energy states
2. **Transition Form Factors**: Need deformation parameters (β_λ) for collective excitations
3. **Coupled Channels**: Different channels (ground state, excited states) are coupled
4. **Energy Loss**: Projectile loses energy equal to excitation energy
5. **Angular Momentum**: More complex due to excited state quantum numbers

### Types of Excitations

1. **Collective Excitations**:
   - **Rotational bands**: Deformed nuclei (e.g., rare-earth, actinide regions)
   - **Vibrational bands**: Spherical nuclei (e.g., quadrupole vibrations)
   - Characterized by deformation parameters β_λ

2. **Single-Particle Excitations**:
   - Particle-hole excitations
   - Characterized by single-particle matrix elements

3. **Giant Resonances**:
   - Collective modes at higher energies
   - Isovector giant dipole resonance (IVGDR)
   - Isoscalar giant quadrupole resonance (ISGQR)

## Implementation Plan

### Phase 1: Transition Form Factors (FIRST STEP) ⭐

**Goal**: Calculate transition form factors for nuclear excitations

**What to implement**:
1. **Deformation parameters** - β_λ for multipole order λ
2. **Transition form factor** - F_λ(r) = β_λ · dV/dr (derivative of potential)
3. **Radial form factors** - for different multipolarities (λ = 2, 3, 4, ...)
4. **Reduced matrix elements** - B(Eλ) values

**Key functions needed**:
```clojure
(defn deformation-parameter [lambda nucleus-type]
  "Get deformation parameter β_λ for specific nucleus")

(defn transition-form-factor [r lambda beta V-params]
  "Calculate transition form factor F_λ(r) = β_λ · dV/dr")

(defn reduced-matrix-element [lambda J-i J-f]
  "Calculate reduced matrix element <J_f||M(λ)||J_i>")
```

**Why this is first**: 
- Foundation for all inelastic calculations
- Can reuse existing potential functions
- Relatively straightforward extension
- Can test independently

### Phase 2: Coupled Channels Framework

**Goal**: Implement coupled channels formalism

**What to implement**:
1. **Channel coupling** - coupling between ground and excited states
2. **Coupled Schrödinger equations** - system of coupled differential equations
3. **Channel wavefunctions** - solutions for each channel
4. **Coupling matrix** - matrix elements between channels

**Key functions**:
```clojure
(defn coupled-channels-system [channels coupling-matrix]
  "Set up coupled channels system")

(defn solve-coupled-channels [E channels coupling-matrix]
  "Solve coupled channels equations")
```

### Phase 3: Collective Model Excitations

**Goal**: Implement collective model for rotational and vibrational excitations

**What to implement**:
1. **Rotational model** - for deformed nuclei
2. **Vibrational model** - for spherical nuclei
3. **Deformed potential** - V(r,θ,φ) = V_0(r) + V_1(r)Y_λμ(θ,φ)
4. **Angular momentum coupling** - for rotational bands

**Key functions**:
```clojure
(defn deformed-potential [r theta phi lambda beta V-params]
  "Calculate deformed potential V(r,θ,φ)")

(defn rotational-band [I-gs K lambda]
  "Calculate rotational band structure")
```

### Phase 4: Inelastic Scattering Amplitude

**Goal**: Calculate inelastic scattering amplitude

**What to implement**:
1. **Inelastic amplitude** - T_inel = <χ_f|V_transition|χ_i>
2. **Distorted waves** - χ_i and χ_f for entrance and exit channels
3. **Transition potential** - V_transition = F_λ(r) · Y_λμ(θ,φ)
4. **Angular integration** - integrate over angular coordinates

**Key functions**:
```clojure
(defn inelastic-amplitude [chi-i chi-f V-transition lambda mu]
  "Calculate inelastic scattering amplitude")

(defn inelastic-cross-section [amplitude k-i k-f E-ex]
  "Calculate inelastic differential cross-section")
```

### Phase 5: Single-Particle Excitations

**Goal**: Handle single-particle (particle-hole) excitations

**What to implement**:
1. **Particle-hole states** - single-particle excitations
2. **Transition densities** - ρ_trans(r) for particle-hole transitions
3. **Form factors** - from transition densities
4. **Spectroscopic factors** - for single-particle strength

**Key functions**:
```clojure
(defn particle-hole-state [n1 l1 j1 n2 l2 j2]
  "Define particle-hole excitation")

(defn transition-density [phi-particle phi-hole]
  "Calculate transition density")
```

### Phase 6: Angular Distribution

**Goal**: Calculate angular distribution for inelastic scattering

**What to implement**:
1. **Angular distribution** - dσ/dΩ as function of angle
2. **Legendre polynomial expansion** - P_L(cos θ) terms
3. **Interference terms** - between different channels
4. **Angular momentum coupling** - Clebsch-Gordan coefficients

**Key functions**:
```clojure
(defn inelastic-angular-distribution [amplitudes theta]
  "Calculate angular distribution")

(defn legendre-expansion [coefficients theta]
  "Expand in Legendre polynomials")
```

### Phase 7: Testing and Validation

**Goal**: Test with known inelastic scattering data

**What to implement**:
1. **Simple test case** - (α,α') on light nucleus (e.g., ¹²C)
2. **Comparison with literature** - validate against published results
3. **Parameter sensitivity** - test β_λ, deformation parameters
4. **Convergence tests** - check numerical accuracy

## First Step: Transition Form Factors

### Detailed Implementation Plan

#### Step 1.1: Deformation Parameters

Deformation parameters β_λ characterize the strength of collective excitations:
- **β_2**: Quadrupole deformation (most common)
- **β_3**: Octupole deformation
- **β_4**: Hexadecapole deformation

Typical values:
- Spherical nuclei: β_2 ≈ 0.1-0.3
- Deformed nuclei: β_2 ≈ 0.2-0.4
- Strongly deformed: β_2 > 0.4

#### Step 1.2: Transition Form Factor

The transition form factor is:
```
F_λ(r) = β_λ · R_0 · dV/dr
```

where:
- β_λ is the deformation parameter
- R_0 is the nuclear radius
- dV/dr is the derivative of the Woods-Saxon potential

For Woods-Saxon potential:
```
V(r) = -V_0 / (1 + exp((r - R_0)/a_0))
dV/dr = (V_0/a_0) · exp((r - R_0)/a_0) / [1 + exp((r - R_0)/a_0)]²
```

#### Step 1.3: Multipole Expansion

The transition potential is expanded in spherical harmonics:
```
V_transition(r,θ,φ) = Σ_λμ F_λ(r) · Y_λμ(θ,φ)
```

#### Step 1.4: Reduced Matrix Elements

For electromagnetic transitions, need B(Eλ) values:
```
B(Eλ; J_i → J_f) = |<J_f||M(λ)||J_i>|² / (2J_i + 1)
```

### Code Structure

Create namespace: `src/dwba/inelastic.clj`

```clojure
(ns dwba.inelastic
  (:require [functions :refer :all]
            [fastmath.core :as m]
            [fastmath.special :as spec]))

;; Deformation parameters
(defn deformation-parameter [lambda nucleus-type]
  "Get β_λ for specific nucleus")

;; Transition form factors
(defn transition-form-factor [r lambda beta V-params]
  "Calculate F_λ(r) = β_λ · R_0 · dV/dr")

;; Reduced matrix elements
(defn reduced-matrix-element [lambda J-i J-f]
  "Calculate <J_f||M(λ)||J_i>")
```

### Testing Strategy

1. **Test with known β_2 values** - compare with literature
2. **Test form factor shape** - verify F_λ(r) has correct radial dependence
3. **Test normalization** - check consistency with B(Eλ) values
4. **Test different λ** - verify multipole expansion

## Dependencies and Resources

### Key References
- **Satchler, G.R.** - "Direct Nuclear Reactions" (Chapter 6: Inelastic Scattering)
- **Hodgson, P.E.** - "Nuclear Reactions and Nuclear Structure" (Chapter 9)
- **Tamura, T.** - "Coupled Reaction Channels"
- **Alder, K. & Winther, A.** - "Electromagnetic Excitation"

### Mathematical Tools Needed
- Spherical harmonics (fastmath has these)
- Legendre polynomials (fastmath has these)
- Clebsch-Gordan coefficients (for angular momentum coupling)
- Numerical integration (already have)

### Typical Parameters

**Deformation Parameters**:
- ¹²C: β_2 ≈ 0.2-0.3
- ²⁰⁸Pb: β_2 ≈ 0.05-0.1 (spherical)
- ¹⁵⁴Sm: β_2 ≈ 0.3-0.4 (deformed)

**Excitation Energies**:
- First 2⁺ state: typically 1-3 MeV for light nuclei
- First 3⁻ state: typically 2-5 MeV
- Giant resonances: 10-30 MeV

## Timeline Estimate

- **Phase 1 (Transition Form Factors)**: 1-2 weeks
- **Phase 2 (Coupled Channels)**: 2-3 weeks
- **Phase 3 (Collective Model)**: 2 weeks
- **Phase 4 (Inelastic Amplitude)**: 1-2 weeks
- **Phase 5 (Single-Particle)**: 1-2 weeks
- **Phase 6 (Angular Distribution)**: 1 week
- **Phase 7 (Testing)**: Ongoing

**Total**: ~10-15 weeks for full implementation

## Success Criteria

✅ **Phase 1 Complete When**:
- Can calculate transition form factors for different λ
- Form factors have correct radial dependence
- Deformation parameters match literature values
- Code is tested and documented

## Next Steps After Phase 1

Once transition form factors are working:
1. Test with simple nucleus (e.g., ¹²C)
2. Calculate inelastic amplitude
3. Compare with experimental data
4. Implement coupled channels

---

**Status**: Planning complete, ready to begin Phase 1 implementation.
