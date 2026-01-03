# Bound State Solver Implementation

## Summary

The bound state wavefunction solver has been implemented in `src/dwba/transfer.clj`. This is the **first step** towards implementing DWBA calculations for single nucleon transfer reactions.

## What Was Implemented

### Core Functions

1. **`solve-bound-state-numerov`** - Numerov integration for bound states
   - Handles negative energies (bound states)
   - Uses power series start: u(r) ≈ r^(l+1)
   - Integrates from r=0 to r_max

2. **`find-bound-state-energy`** - Shooting method for eigenvalue search
   - Uses bisection to find energy where u(r_max) ≈ 0
   - Counts nodes to verify quantum number n
   - Returns energy, wavefunction, and convergence info

3. **`normalize-bound-state`** - Normalizes wavefunctions
   - Uses Simpson's rule for integration
   - Ensures ∫ u²(r) dr = 1

4. **`solve-bound-state`** - Main user-facing function
   - Finds bound state with given quantum numbers (n, l, j)
   - Returns complete solution with normalized wavefunction
   - Handles all the details automatically

### Utility Functions

- **`bound-state-start`** - Power series initialization
- **`bound-state-boundary-value`** - Check boundary condition
- **`count-nodes`** - Count nodes in wavefunction (identifies n)
- **`bound-state-energy-approx`** - Rough energy estimate
- **`plot-bound-state-info`** - Print solution information

## Usage Example

```clojure
(require '[dwba.transfer :refer :all])
(require '[functions :refer :all])

;; Define Woods-Saxon potential: [V0, R0, a0]
(def ws-params [50.0 2.0 0.6])

;; Find 1s bound state (n=1, l=0)
(def result (solve-bound-state ws-params 1 0 nil 20.0 0.01))

;; Access results
(:energy result)                    ; Bound state energy
(:normalized-wavefunction result)   ; Normalized wavefunction
(:nodes result)                     ; Number of nodes
(:converged? result)                ; Convergence status
```

## Testing

A test script is provided in `test_bound_state.clj` that demonstrates:
- Finding 1s, 1p, and 2s bound states
- Energy estimation
- Normalization verification
- Node counting

## Key Features

### 1. Shooting Method
The solver uses a **shooting method** with bisection to find the correct energy:
- Start with energy range [E_min, E_max]
- Integrate Numerov and check u(r_max)
- Adjust energy until u(r_max) ≈ 0
- Verify node count matches quantum number n

### 2. Quantum Numbers
- **n** - Principal quantum number (1, 2, 3, ...)
- **l** - Orbital angular momentum (0, 1, 2, ...)
- **j** - Total angular momentum (currently not used, reserved for spin-orbit)

### 3. Normalization
Bound states are normalized so that:
```
∫₀^∞ |u(r)|² dr = 1
```

### 4. Boundary Conditions
For bound states:
- u(0) = 0 (regular at origin)
- u(r → ∞) → 0 (exponentially decaying)

## Physics Notes

### Bound States vs Scattering States

**Scattering States (existing code)**:
- Positive energy E > 0
- Wavefunction oscillates at large r
- Boundary condition: match to Hankel functions

**Bound States (new code)**:
- Negative energy E < 0
- Wavefunction decays exponentially at large r
- Boundary condition: u(r_max) ≈ 0

### Woods-Saxon Potential

The bound states are calculated in a Woods-Saxon well:
```
V(r) = -V₀ / (1 + exp((r - R₀)/a₀))
```

Where:
- V₀ = potential depth (typically 30-60 MeV)
- R₀ = nuclear radius (typically 1-3 fm)
- a₀ = surface diffuseness (typically 0.5-0.7 fm)

## Next Steps

Now that bound states are implemented, the next phases are:

1. **Phase 2**: Transfer Form Factors
   - Calculate overlap integrals between bound states
   - Needed for transfer amplitude

2. **Phase 3**: Zero-Range Approximation
   - Implement simplest transfer interaction model
   - V_transfer = D₀ δ(r)

3. **Phase 4**: Post and Prior Formulations
   - Implement both forms of DWBA transfer amplitude

4. **Phase 5**: Angular Momentum Coupling
   - Clebsch-Gordan coefficients
   - Racah algebra

5. **Phase 6**: Differential Cross-Section
   - Calculate dσ/dΩ for transfer reactions

## Files Created

- `src/dwba/transfer.clj` - Main implementation (350+ lines)
- `test_bound_state.clj` - Test script with examples
- `BOUND_STATE_IMPLEMENTATION.md` - This document

## Status

✅ **Phase 1 Complete**: Bound state solver is implemented and ready for use.

The implementation follows the plan outlined in `TRANSFER_REACTIONS_PLAN.md` and provides a solid foundation for transfer reaction calculations.

