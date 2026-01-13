# Testing Finite Square Well Bound States for l=1, l=2

## Known Results and References

While specific tabulated values for finite square well bound states with l=1, l=2 are not readily available in standard online sources, here are approaches to validate the implementation:

### 1. **Physical Consistency Checks**

The implementation should satisfy:

- **Energy range**: All bound states have `0 < e_ratio < 1` (where `e_ratio = |E|/V0`)
- **Matching condition**: The matching error `f(xi, eta, l) = 0` should be satisfied to high precision
- **Centrifugal barrier effect**: For a given `z0`, higher `l` should have fewer bound states:
  - `N_bound(l=0) ≥ N_bound(l=1) ≥ N_bound(l=2)`
- **Well depth effect**: For a given `l`, deeper wells (larger `z0`) should have more bound states

### 2. **Asymptotic Limits**

#### Infinite Square Well Limit (z0 → ∞)

As `z0 → ∞`, the finite well approaches an infinite square well. For an infinite well, the bound state condition is:

```
xi = n*π  (for l=0)
```

For l>0, the zeros of spherical Bessel functions determine the bound states. The first few zeros are:

**l=1 (j_1 zeros)**:
- First zero: ~4.4934
- Second zero: ~7.7253
- Third zero: ~10.9041

**l=2 (j_2 zeros)**:
- First zero: ~5.7635
- Second zero: ~9.0950
- Third zero: ~12.3229

For a finite well with large `z0`, the bound states should approach these values.

### 3. **Cross-Validation Methods**

#### Method 1: Compare with Shooting Method
Use the Numerov shooting method (already implemented in `dwba.transfer`) to find bound states for the same `l` and `z0`, and compare energies.

#### Method 2: Verify Matching Condition
For each found bound state, directly evaluate:
```clojure
(finite-well-matching-error xi eta l)
```
This should be very close to zero (e.g., < 1e-6).

#### Method 3: Check Energy Ordering
Bound states should be ordered by energy (lowest first), and the number should increase with `z0`.

### 4. **Expected Behavior**

For `z0 = 6.0`:
- **l=0**: Should have 1-2 bound states (depending on well depth)
- **l=1**: Should have 0-1 bound states (centrifugal barrier reduces binding)
- **l=2**: Should have 0 bound states (strong centrifugal barrier)

For `z0 = 10.0`:
- **l=0**: Should have 2-3 bound states
- **l=1**: Should have 1-2 bound states
- **l=2**: Should have 0-1 bound states

### 5. **References to Check**

1. **Quantum Mechanics Textbooks**:
   - Griffiths, "Introduction to Quantum Mechanics" - Chapter on bound states
   - Shankar, "Principles of Quantum Mechanics" - Central potentials
   - Landau & Lifshitz, "Quantum Mechanics" - Bound states in central fields

2. **Nuclear Physics References**:
   - "Nuclear Physics" by Krane - Single-particle states
   - "Introduction to Nuclear Physics" by Cottingham & Greenwood

3. **Numerical Methods**:
   - "Computational Physics" by Thijssen - Bound state calculations
   - "Numerical Recipes" - Root finding and eigenvalue problems

### 6. **Test Implementation**

Run the validation test:
```bash
clojure -M -e "(load-file \"test_finite_well_validation.clj\")"
```

This will:
- Find all bound states for various `l` and `z0` combinations
- Validate physical consistency
- Check matching conditions
- Compare across `l` values to verify centrifugal barrier effect

### 7. **Known Analytical Results**

For **l=0**, the matching condition is:
```
xi * cot(xi) = eta
```

This can be solved analytically for specific cases. For **l>0**, the matching condition involves ratios of Bessel functions and is typically solved numerically.

### 8. **Validation Checklist**

- [ ] All bound states have `0.01 < e_ratio < 0.99`
- [ ] Matching error `|f(xi, eta, l)| < 1e-6` for converged states
- [ ] Number of bound states decreases with increasing `l` (for fixed `z0`)
- [ ] Number of bound states increases with increasing `z0` (for fixed `l`)
- [ ] States are sorted by energy (lowest first)
- [ ] No duplicate states (within tolerance)
- [ ] Discontinuities are correctly identified and excluded

