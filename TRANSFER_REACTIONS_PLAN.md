# Plan for DWBA Single Nucleon Transfer Reactions

## Overview

This document outlines the plan to extend the current elastic scattering DWBA code to handle **single nucleon transfer reactions** such as (d,p), (d,n), (p,d), (α,t), etc.

## Current State

### ✅ What We Have (Elastic Scattering)
- **Distorted wave calculations** - solving Schrödinger equation for scattering states
- **R-matrix and S-matrix** calculations
- **Phase shift analysis**
- **Coulomb interactions** - full implementation
- **Woods-Saxon potentials**
- **Numerov integration** - robust numerical solver
- **Kinematic transformations** - lab ↔ CM frame conversions

### ❌ What We Need (Transfer Reactions)
- **Bound state wavefunctions** - solving for bound states of transferred nucleon
- **Transfer form factors** - overlap integrals between bound states
- **Finite-range or zero-range interactions** - transfer interaction potential
- **Post and prior formulations** - different ways to write transfer amplitude
- **Angular momentum coupling** - Clebsch-Gordan coefficients, Racah algebra
- **Spectroscopic factors** - nuclear structure information
- **Transfer cross-section formula** - different from elastic scattering

## Physics Background

### Single Nucleon Transfer Reactions

A transfer reaction has the form: **A(a,b)B** where:
- **a = b + n** (nucleon n is transferred)
- Example: **A(d,p)B** means deuteron → proton + neutron (neutron transferred)
- Example: **A(p,d)B** means proton → deuteron (neutron picked up)

### Key Differences from Elastic Scattering

1. **Bound States**: The transferred nucleon is in a bound state in both initial and final nuclei
2. **Overlap Integrals**: Need to calculate form factors from bound state wavefunctions
3. **Transfer Interaction**: The interaction that causes the transfer (usually approximated)
4. **Angular Momentum**: More complex coupling due to bound state quantum numbers

## Implementation Plan

### Phase 1: Bound State Wavefunctions (FIRST STEP) ⭐

**Goal**: Solve for bound state wavefunctions of the transferred nucleon

**What to implement**:
1. **Bound state solver** - modify Numerov to find bound states (negative energies)
2. **Eigenvalue search** - find energies where wavefunction → 0 at infinity
3. **Normalization** - normalize bound state wavefunctions
4. **Quantum numbers** - handle n, l, j quantum numbers

**Key functions needed**:
```clojure
(defn solve-bound-state [V-params n l j]
  "Solve for bound state with quantum numbers n, l, j"
  ;; Returns: {:energy E, :wavefunction u(r), :normalization N})

(defn find-bound-state-energy [V-params l j]
  "Find bound state energy using shooting method"
  ;; Search for energy where wavefunction → 0 at infinity)

(defn normalize-bound-state [u r-max]
  "Normalize bound state wavefunction: ∫|u|²dr = 1")
```

**Why this is first**: 
- Foundation for all transfer calculations
- Can reuse existing Numerov infrastructure
- Relatively straightforward extension
- Can test independently

### Phase 2: Transfer Form Factors

**Goal**: Calculate overlap integrals between bound states

**What to implement**:
1. **Form factor calculation** - ∫ φ*_f(r) φ_i(r) r² dr
2. **Radial integration** - integrate bound state products
3. **Angular part** - handle angular momentum coupling

**Key functions**:
```clojure
(defn transfer-form-factor [phi-i phi-j r-max]
  "Calculate overlap integral between bound states")

(defn form-factor-r [phi-i phi-j r]
  "Radial form factor at distance r")
```

### Phase 3: Zero-Range Approximation

**Goal**: Implement simplest transfer interaction model

**What to implement**:
1. **Zero-range interaction** - V_transfer = D₀ δ(r)
2. **Zero-range constant** - D₀ parameter (reaction-dependent)
3. **Simplified amplitude** - reduces to form factor evaluation

**Key functions**:
```clojure
(defn zero-range-constant [reaction-type]
  "Get D₀ for specific reaction type (d,p), (p,d), etc.")

(defn transfer-amplitude-zero-range [form-factor D0]
  "Calculate transfer amplitude in zero-range approximation")
```

### Phase 4: Post and Prior Formulations

**Goal**: Implement both post and prior forms of DWBA

**What to implement**:
1. **Post form** - interaction in exit channel
2. **Prior form** - interaction in entrance channel
3. **Equivalence checking** - verify post = prior (numerical test)

**Key functions**:
```clojure
(defn transfer-amplitude-post [chi-f V-transfer chi-i]
  "Post form: <χ_f|V_transfer|χ_i>")

(defn transfer-amplitude-prior [chi-f V-transfer chi-i]
  "Prior form: <χ_f|V_transfer|χ_i>")
```

### Phase 5: Angular Momentum Coupling

**Goal**: Handle complex angular momentum algebra

**What to implement**:
1. **Clebsch-Gordan coefficients** - for angular momentum coupling
2. **Racah coefficients** - for recoupling
3. **Angular distribution** - Y_lm spherical harmonics
4. **Total cross-section** - sum over magnetic substates

**Key functions**:
```clojure
(defn clebsch-gordan [j1 m1 j2 m2 J M]
  "Calculate Clebsch-Gordan coefficient")

(defn transfer-angular-distribution [amplitude theta phi]
  "Calculate angular distribution from transfer amplitude")
```

### Phase 6: Differential Cross-Section

**Goal**: Calculate dσ/dΩ for transfer reactions

**What to implement**:
1. **Cross-section formula** - |T|² with proper kinematics
2. **Spectroscopic factors** - S-factor multiplication
3. **Kinematic factors** - lab/CM frame conversions
4. **Integration** - total cross-section from differential

**Key functions**:
```clojure
(defn transfer-differential-cross-section [T-amplitude S-factor k-f k-i]
  "Calculate dσ/dΩ for transfer reaction")

(defn transfer-total-cross-section [differential-sigma]
  "Integrate to get total cross-section")
```

### Phase 7: Testing and Validation

**Goal**: Test with known transfer reactions

**What to implement**:
1. **Simple test case** - (d,p) on light nucleus
2. **Comparison with literature** - validate against published results
3. **Parameter sensitivity** - test D₀, spectroscopic factors
4. **Convergence tests** - check numerical accuracy

## First Step: Bound State Solver

### Detailed Implementation Plan

#### Step 1.1: Modify Numerov for Bound States

Current Numerov solves scattering states (positive energy). Need to:
- Handle negative energies (bound states)
- Find energy where wavefunction → 0 at large r
- Implement shooting method for eigenvalue search

#### Step 1.2: Eigenvalue Search Algorithm

Use **shooting method**:
1. Start with guess energy E
2. Integrate Numerov from r=0 to r_max
3. Check boundary condition: u(r_max) ≈ 0
4. Adjust E using bisection or Newton-Raphson
5. Repeat until convergence

#### Step 1.3: Normalization

Bound states must be normalized:
- ∫₀^∞ |u(r)|² dr = 1
- Use Simpson's rule or similar for integration

#### Step 1.4: Quantum Numbers

Handle quantum numbers:
- **n** - principal quantum number (1, 2, 3, ...)
- **l** - orbital angular momentum (0, 1, 2, ...)
- **j** - total angular momentum (l ± 1/2 for nucleons)

### Code Structure

Create new namespace: `src/dwba/transfer.clj`

```clojure
(ns dwba.transfer
  (:require [functions :refer :all]
            [complex :refer :all]))

;; Bound state solver
(defn solve-bound-state [V-params n l j E-guess r-max h]
  "Solve for bound state wavefunction"
  ...)

(defn find-bound-state-energy [V-params l j r-max h]
  "Find bound state energy using shooting method"
  ...)

(defn normalize-bound-state [u r-max h]
  "Normalize bound state: ∫|u|²dr = 1"
  ...)
```

### Testing Strategy

1. **Test with square well** - known analytical solutions
2. **Test with Woods-Saxon** - compare with literature
3. **Test normalization** - verify ∫|u|²dr = 1
4. **Test quantum numbers** - verify correct n, l, j states

## Dependencies and Resources

### Key References
- **Satchler, G.R.** - "Direct Nuclear Reactions" (Chapter 8: Transfer Reactions)
- **Hodgson, P.E.** - "Nuclear Reactions and Nuclear Structure" (Chapter 11)
- **Austern, N.** - "Direct Nuclear Reaction Theories"

### Mathematical Tools Needed
- Clebsch-Gordan coefficients (can use library or implement)
- Spherical harmonics (fastmath has these)
- Numerical integration (already have)

## Timeline Estimate

- **Phase 1 (Bound States)**: 1-2 weeks
- **Phase 2 (Form Factors)**: 1 week
- **Phase 3 (Zero-Range)**: 1 week
- **Phase 4 (Post/Prior)**: 1-2 weeks
- **Phase 5 (Angular Momentum)**: 2 weeks
- **Phase 6 (Cross-Section)**: 1 week
- **Phase 7 (Testing)**: Ongoing

**Total**: ~8-10 weeks for full implementation

## Success Criteria

✅ **Phase 1 Complete When**:
- Can solve for bound states with given quantum numbers
- Wavefunctions are properly normalized
- Energies match known values (e.g., from literature)
- Code is tested and documented

## Next Steps After Phase 1

Once bound states are working:
1. Test with simple nucleus (e.g., ¹⁶O)
2. Calculate form factors between states
3. Implement zero-range approximation
4. Calculate first transfer cross-section

---

**Status**: 
- ✅ Phase 1: Bound State Wavefunctions - COMPLETE
- ✅ Phase 2: Transfer Form Factors - COMPLETE
- ✅ Phase 3: Zero-Range Approximation - COMPLETE
- ✅ Phase 4: Post and Prior Formulations - COMPLETE
- ✅ Phase 5: Angular Momentum Coupling - COMPLETE
- ✅ Phase 6: Differential Cross-Section - COMPLETE
- ⏳ Phase 7: Testing and Validation - PARTIAL

