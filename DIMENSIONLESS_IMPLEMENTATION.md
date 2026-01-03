# Dimensionless Implementation of `solve-bound-state-numerov`

## Overview

The `solve-bound-state-numerov` function has been rewritten to use **dimensionless variables** internally. This improves numerical stability and makes the code more general.

## Dimensionless Variables

### Physical to Dimensionless Conversion

| Physical Variable | Symbol | Dimensionless Variable | Symbol | Conversion |
|-------------------|--------|------------------------|--------|------------|
| Radius | r (fm) | ρ | rho | ρ = r/R₀ |
| Energy | E (MeV) | ε | epsilon | ε = E/V₀ |
| Diffuseness | a₀ (fm) | α | alpha | α = a₀/R₀ |
| Step size | h (fm) | h_ρ | h-rho | h_ρ = h/R₀ |
| Max radius | r_max (fm) | ρ_max | rho-max | ρ_max = r_max/R₀ |

### Dimensionless Coupling Parameter

The key dimensionless parameter is:
```
λ = (2μ/ħ²) · V₀ · R₀²
```

This combines the mass factor, potential depth, and length scale into a single dimensionless number.

## Dimensionless Schrödinger Equation

The radial Schrödinger equation in dimensionless form is:

```
-d²u/dρ² + f(ρ)u = 0
```

where:

```
f(ρ) = λ · [v(ρ) + l(l+1)/(ρ²) - ε]
```

and:

```
v(ρ) = -1/(1 + exp((ρ-1)/α))
```

is the dimensionless Woods-Saxon potential.

## Implementation Details

### New Functions

1. **`woods-saxon-dimensionless [rho alpha]`**
   - Computes dimensionless Woods-Saxon potential
   - v(ρ) = -1/(1 + exp((ρ-1)/α))

2. **`f-rho-numerov-dimensionless [rho epsilon l lambda alpha]`**
   - Computes f(ρ) for Numerov integration
   - f(ρ) = λ · [v(ρ) + l(l+1)/(ρ²) - ε]

3. **`bound-state-start [rho l]`**
   - Updated to work with dimensionless radius
   - u(ρ) ≈ ρ^(l+1) for small ρ

### Modified Function

**`solve-bound-state-numerov [e l v0 rad diff h r-max]`**
- Still accepts physical units (MeV, fm) for compatibility
- Converts to dimensionless variables internally
- Performs Numerov integration in dimensionless space
- Returns wavefunction in physical units (same as before)

## Advantages

1. **Numerical Stability**: Dimensionless variables are typically O(1), reducing numerical errors
2. **Generality**: The same code works for different physical scales
3. **Clarity**: The physics is clearer in dimensionless form
4. **Testing**: Easier to test with simple dimensionless values

## Example

For typical nuclear parameters:
- V₀ = 50 MeV
- R₀ = 2.0 fm
- a₀ = 0.6 fm
- E = -20 MeV

The dimensionless values are:
- ε = -20/50 = -0.4
- α = 0.6/2.0 = 0.3
- λ = mass-factor × 50 × 2.0² ≈ 0.044 × 200 ≈ 8.8
- h_ρ = 0.01/2.0 = 0.005

## Backward Compatibility

The function signature remains the same - it still accepts physical units:
```clojure
(solve-bound-state-numerov -20.0 0 50.0 2.0 0.6 0.01 20.0)
```

The conversion to dimensionless variables is handled internally, so existing code continues to work.

## Testing

The test file `test_solve_bound_state_numerov.clj` has been updated to reflect the dimensionless implementation. All tests should pass with the same results as before, but now the internal calculations are more numerically stable.

