# Finite Square Well Bound State Reference Table

This table provides reference values for bound state energies in a finite square well.
It can be used to cross-check numerical calculations, including Woods-Saxon wells
with small diffuseness (a0 << R0).

## Parameters

- **z0**: Dimensionless well depth parameter = sqrt(2m·V0·a²/ℏ²)
- **l**: Orbital angular momentum quantum number
- **e-ratio**: Dimensionless energy = |E|/V0
- **xi**: Dimensionless wave number inside well = k·a
- **eta**: Dimensionless decay parameter outside well = κ·a

## Reference Values


=== l = 0 ===

| z0  | N | State | e-ratio | |E|/V0 | xi (ka) | eta (κa) | Matching Error |
|-----|---|-------|---------|-------|---------|----------|----------------|
| 2.0 | 1 | 1     | 0.101775 | 0.101775 | 1.895494 | 0.638045 | -6.78e-09 |
| 3.0 | 1 | 1     | 0.422976 | 0.422976 | 2.278863 | 1.951098 | -7.07e-09 |
| 4.0 | 1 | 1     | 0.617279 | 0.617279 | 2.474577 | 3.142685 | -1.31e-08 |
| 5.0 | 2 | 1     | 0.037131 | 0.037131 | 4.906295 | 0.963467 | -3.60e-08 |
|     |   | 2     | 0.730486 | 0.730486 | 2.595739 | 4.273422 | -3.59e-08 |
| 6.0 | 2 | 1     | 0.241370 | 0.241370 | 5.225964 | 2.947763 | -3.33e-08 |
|     |   | 2     | 0.800670 | 0.800670 | 2.678783 | 5.368810 | -3.35e-08 |
| 7.0 | 2 | 1     | 0.404520 | 0.404520 | 5.401717 | 4.452130 | -3.28e-08 |
|     |   | 2     | 0.846841 | 0.846841 | 2.739489 | 6.441677 | -3.10e-08 |
| 8.0 | 3 | 1     | 0.010641 | 0.010641 | 7.957321 | 0.825248 | -7.75e-08 |
|     |   | 2     | 0.523650 | 0.523650 | 5.521446 | 5.789096 | -7.12e-08 |
|     |   | 3     | 0.878730 | 0.878730 | 2.785902 | 7.499250 | -1.13e-07 |
| 10.0 | 3 | 1     | 0.290496 | 0.290496 | 8.423204 | 5.389771 | -6.92e-08 |
|     |   | 2     | 0.677466 | 0.677466 | 5.679208 | 8.230832 | -6.16e-08 |
|     |   | 3     | 0.918641 | 0.918641 | 2.852342 | 9.584579 | -1.01e-07 |
| 12.0 | 4 | 1     | 0.108384 | 0.108384 | 11.331049 | 3.950611 | -9.05e-08 |
|     |   | 2     | 0.483636 | 0.483636 | 8.623013 | 8.345277 | -9.53e-08 |
|     |   | 3     | 0.767951 | 0.767951 | 5.780575 | 10.515938 | -1.11e-07 |
|     |   | 4     | 0.941690 | 0.941690 | 2.897707 | 11.644883 | -6.55e-08 |
| 15.0 | 5 | 1     | 0.076235 | 0.076235 | 14.416907 | 4.141592 | -1.42e-07 |
|     |   | 2     | 0.394257 | 0.394257 | 11.674425 | 9.418482 | -1.34e-07 |
|     |   | 3     | 0.655978 | 0.655978 | 8.798006 | 12.148872 | -1.96e-07 |
|     |   | 4     | 0.846317 | 0.846317 | 5.880355 | 13.799327 | -1.61e-07 |
|     |   | 5     | 0.961478 | 0.961478 | 2.944041 | 14.708250 | 3.89e-08 |

**Notes:**
- N = number of bound states found
- e-ratio = |E|/V0 (dimensionless energy, 0 < e-ratio < 1)
- xi = k·a where k = sqrt(2m(E+V0))/ℏ (inside well)
- eta = κ·a where κ = sqrt(2m|E|)/ℏ (outside well)
- Matching error = f(xi, eta, l) (should be ≈ 0 for true bound states)


=== l = 1 ===

| z0  | N | State | e-ratio | |E|/V0 | xi (ka) | eta (κa) | Matching Error |
|-----|---|-------|---------|-------|---------|----------|----------------|
| 2.0 | 0 | -     | -       | -     | -       | -        | -              |
| 3.0 | 0 | -     | -       | -     | -       | -        | -              |
| 4.0 | 0 | -     | -       | -     | -       | -        | -              |
| 5.0 | 0 | -     | -       | -     | -       | -        | -              |
| 6.0 | 0 | -     | -       | -     | -       | -        | -              |
| 7.0 | 1 | 1     | 0.127308 | 0.127308 | 6.539259 | 2.497618 | -4.12e-09 |
| 8.0 | 1 | 1     | 0.291551 | 0.291551 | 6.733555 | 4.319634 | -1.57e-08 |
| 10.0 | 1 | 1     | 0.073776 | 0.073776 | 9.624051 | 2.716180 | -1.73e-09 |
| 12.0 | 1 | 1     | 0.314823 | 0.314823 | 9.933049 | 6.733092 | -6.27e-09 |
| 15.0 | 1 | 1     | 0.246741 | 0.246741 | 13.018577 | 7.450950 | -1.04e-07 |

**Notes:**
- N = number of bound states found
- e-ratio = |E|/V0 (dimensionless energy, 0 < e-ratio < 1)
- xi = k·a where k = sqrt(2m(E+V0))/ℏ (inside well)
- eta = κ·a where κ = sqrt(2m|E|)/ℏ (outside well)
- Matching error = f(xi, eta, l) (should be ≈ 0 for true bound states)


=== l = 2 ===

| z0  | N | State | e-ratio | |E|/V0 | xi (ka) | eta (κa) | Matching Error |
|-----|---|-------|---------|-------|---------|----------|----------------|
| 2.0 | 0 | -     | -       | -     | -       | -        | -              |
| 3.0 | 0 | -     | -       | -     | -       | -        | -              |
| 4.0 | 0 | -     | -       | -     | -       | -        | -              |
| 5.0 | 1 | 1     | 0.134146 | 0.134146 | 4.652564 | 1.831297 | 2.26e-09 |
| 6.0 | 0 | -     | -       | -     | -       | -        | -              |
| 7.0 | 0 | -     | -       | -     | -       | -        | -              |
| 8.0 | 1 | 1     | 0.045770 | 0.045770 | 7.814775 | 1.711517 | -3.78e-10 |
| 10.0 | 1 | 1     | 0.334727 | 0.334727 | 8.156427 | 5.785559 | -2.92e-08 |
| 12.0 | 1 | 1     | 0.136216 | 0.136216 | 11.152798 | 4.428893 | -5.88e-09 |
| 15.0 | 2 | 1     | 0.094256 | 0.094256 | 14.275588 | 4.605169 | -6.71e-08 |
|     |   | 2     | 0.416357 | 0.416357 | 11.459482 | 9.678857 | -7.14e-08 |

**Notes:**
- N = number of bound states found
- e-ratio = |E|/V0 (dimensionless energy, 0 < e-ratio < 1)
- xi = k·a where k = sqrt(2m(E+V0))/ℏ (inside well)
- eta = κ·a where κ = sqrt(2m|E|)/ℏ (outside well)
- Matching error = f(xi, eta, l) (should be ≈ 0 for true bound states)


## Summary: Number of Bound States

| z0  | l=0 | l=1 | l=2 |
|-----|-----|-----|-----|
| 2.0 |   1 |   0 |   0 |
| 3.0 |   1 |   0 |   0 |
| 4.0 |   1 |   0 |   0 |
| 5.0 |   2 |   0 |   1 |
| 6.0 |   2 |   0 |   0 |
| 7.0 |   2 |   1 |   0 |
| 8.0 |   3 |   1 |   1 |
| 10.0 |   3 |   1 |   1 |
| 12.0 |   4 |   1 |   1 |
| 15.0 |   5 |   1 |   2 |

## Usage for Woods-Saxon Cross-Check

To cross-check Woods-Saxon calculations:
1. Use small diffuseness: a0 << R0 (e.g., a0 = 0.01 fm, R0 = 2.0 fm)
2. Calculate z0 = sqrt(2m·V0·R0²/ℏ²) for your parameters
   - For 14C+n system: z0 = sqrt(mass-factor · V0 · R0²)
   - mass-factor = 2μ/(ℏc)² ≈ 0.0445 (MeV·fm²)⁻¹ for 14C+n
3. Find the corresponding row in the table above
4. Compare your calculated bound state energy with the e-ratio from the table

### Example

For V0 = 50 MeV, R0 = 2.0 fm, a0 = 0.01 fm (14C+n system):
- z0 = sqrt(0.0445 · 50 · 2²) ≈ sqrt(8.9) ≈ 2.98 ≈ 3.0
- Look up z0 = 3.0 in the table to find expected bound states

### Conversion from e-ratio to Physical Energy

The e-ratio in the table is |E|/V0, so:
- Physical bound state energy: E = -V0 · e-ratio
- Example: For e-ratio = 0.5 and V0 = 50 MeV, E = -25 MeV

### Quick Reference: z0 for Common Parameters (14C+n system)

| V0 (MeV) | R0 (fm) | z0 |
|----------|---------|-----|
|   30.0 |   1.5 | 1.73 |
|   30.0 |   2.0 | 2.31 |
|   30.0 |   2.5 | 2.89 |
|   50.0 |   1.5 | 2.24 |
|   50.0 |   2.0 | 2.98 |
|   50.0 |   2.5 | 3.73 |
|   70.0 |   1.5 | 2.65 |
|   70.0 |   2.0 | 3.53 |
|   70.0 |   2.5 | 4.41 |

## Notes

- All energies are in units of the well depth V0
- Matching errors should be < 1e-6 for converged states
- States are ordered by energy (lowest first)
- For higher l, fewer bound states exist due to centrifugal barrier

