# Analysis: Does `solve-bound-state-numerov` Reuse Numerov Functions?

## Summary

**Answer: Partially - it reuses `f-r-numerov` but NOT the Numerov integration loop.**

## Code Comparison

### `solve-numerov` in `src/functions.clj` (lines 115-154)
- **Purpose**: Solve Schrödinger equation for **scattering states** (positive energy)
- **Initialization**: Uses `bessel-start-l1` (Bessel function start)
- **Numerov step**: Custom implementation
- **Uses**: `f-r-numerov` from `functions.clj` ✓

### `solve-bound-state-numerov` in `src/dwba/transfer.clj` (lines 19-66)
- **Purpose**: Solve Schrödinger equation for **bound states** (negative energy)
- **Initialization**: Uses `bound-state-start` (power series: r^(l+1))
- **Numerov step**: **Duplicated** - same logic as `solve-numerov` but copied
- **Uses**: `f-r-numerov` from `functions.clj` ✓

## What's Reused?

✅ **`f-r-numerov`** - Both functions use this shared function to calculate the effective potential

❌ **Numerov step logic** - The integration loop is duplicated:
```clojure
;; Same in both functions:
numerator (+ (* 2.0 un) 
             (- un-1) 
             (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
denominator (- 1.0 (* h2-12 fn+1))
un+1 (/ numerator denominator)
```

## Differences

1. **Initialization**:
   - `solve-numerov`: `u1 = (bessel-start-l1 h q)` (for scattering)
   - `solve-bound-state-numerov`: `u1 = (bound-state-start h l)` (for bound states)

2. **Energy range**:
   - `solve-numerov`: Designed for positive energies (scattering)
   - `solve-bound-state-numerov`: Designed for negative energies (bound states)

3. **Purpose**:
   - `solve-numerov`: Find scattering wavefunctions
   - `solve-bound-state-numerov`: Find bound state wavefunctions

## Recommendation

The Numerov step logic could be extracted into a shared function:

```clojure
(defn numerov-step [un un-1 fn fn-1 fn+1 h2-12]
  "Single Numerov integration step"
  (let [numerator (+ (* 2.0 un) 
                     (- un-1) 
                     (* h2-12 (+ (* 10.0 fn un) (* fn-1 un-1))))
        denominator (- 1.0 (* h2-12 fn+1))]
    (/ numerator denominator)))
```

Then both `solve-numerov` and `solve-bound-state-numerov` could use this shared function, reducing code duplication.

## Testing

To test `solve-bound-state-numerov`:

1. **Basic integration**: Verify it produces a wavefunction vector
2. **Boundary condition**: Check u(r_max) for bound states
3. **Node counting**: Verify correct number of nodes
4. **Comparison**: Compare with `solve-numerov` at same energy (should be proportional)

## Current Status

- ✅ `solve-bound-state-numerov` works correctly
- ✅ Reuses `f-r-numerov` (good!)
- ⚠️ Duplicates Numerov step logic (could be refactored)
- ✅ Uses appropriate initialization for bound states

