# Analysis: Can We Perform the Calculations from numerical_riccati.tex?

## Paper Requirements

The paper describes:
1. **Test Case**: Woods-Saxon potential with V₀ = 46.23 MeV, R = 2.0 fm, aₛ = 0.5 fm
2. **Parameters**: l=1, E=2 MeV
3. **Numerov Integration** with Riccati-Bessel initialization for l=1
4. **Wronskian Conservation Check**
5. **Phase Shift Calculation** from the Numerov solution
6. **Comparison**: Naive start (r^(l+1)) vs Bessel-start
7. **Convergence Table**: Phase shift errors for h = 0.1, 0.05, 0.01 fm

## Current Code Capabilities

### ✅ Available Functions

1. **`solve-numerov`** - Numerov integration with Bessel start
   - ✅ Uses `bessel-start-l1` for l=1 initialization
   - ✅ Implements the Numerov algorithm correctly
   - ✅ Works with Woods-Saxon potential

2. **`check-wronskian`** - Wronskian conservation check
   - ✅ Implements discrete Wronskian formula
   - ⚠️  Note: Current implementation has a bug (references `(get u n)` twice)

3. **`bessel-start-l1`** - Riccati-Bessel power series for l=1
   - ✅ Implements F₁(qr) ≈ (qr)²/3 - (qr)⁴/30

4. **Woods-Saxon functions** - Multiple implementations available
   - ✅ `WS` - Standard Woods-Saxon
   - ✅ `woods-saxon-numerov` - For Numerov method

### ❌ Missing Functions

1. **Phase Shift Extraction from Numerov Solution**
   - Current `phase-shift` functions use R-matrix method, not Numerov
   - Need: Function to extract phase shift from wavefunction at boundary
   - Method: Match u(r) and u'(r) at boundary to free-space solutions

2. **Naive Start Implementation**
   - Need: Power series start u(r) ≈ C r^(l+1) for comparison
   - For l=1: u(r) ≈ C r²

3. **Phase Shift Error Calculation**
   - Need: "Exact" phase shift reference (likely from very fine Numerov or R-matrix)
   - Need: Error = |δ_calc - δ_exact|

4. **Convergence Analysis**
   - Need: Function to calculate phase shifts for different step sizes
   - Need: Comparison table generation

## Required Additions

### 1. Naive Start Function
```clojure
(defn naive-start-l1 [r]
  "Naive power series start: u(r) ≈ r^2 for l=1"
  (* r r))
```

### 2. Numerov with Naive Start
```clojure
(defn solve-numerov-naive [e l v0 rad diff h r-max]
  "Numerov with naive r^(l+1) start"
  ;; Similar to solve-numerov but uses naive-start-l1
)
```

### 3. Phase Shift from Numerov Wavefunction
```clojure
(defn phase-shift-from-numerov [u u-prime r-boundary k l]
  "Extract phase shift from Numerov solution at boundary"
  ;; Match to: u = A * (j_l(kr) * cos(δ) - y_l(kr) * sin(δ))
  ;; Extract δ from u and u' at r = r-boundary
)
```

### 4. Exact Phase Shift Reference
```clojure
(defn exact-phase-shift [e v0 rad diff l]
  "Calculate 'exact' phase shift using very fine Numerov or R-matrix"
  ;; Use very small h (e.g., 0.001) or R-matrix method
)
```

### 5. Convergence Analysis
```clojure
(defn phase-shift-convergence [e l v0 rad diff h-values r-max]
  "Calculate phase shift errors for different step sizes"
  ;; Returns table of [h, error-naive, error-bessel]
)
```

## Implementation Plan

1. **Fix Wronskian Check Bug** - Correct the formula
2. **Add Naive Start** - Implement r^(l+1) initialization
3. **Add Phase Shift Extraction** - Match wavefunction to free-space solutions
4. **Add Exact Reference** - Use fine Numerov or existing R-matrix
5. **Add Convergence Function** - Generate comparison table

## Test Case from Paper

```clojure
(def test-params
  {:e 2.0      ; MeV
   :l 1
   :v0 46.23   ; MeV
   :rad 2.0    ; fm
   :diff 0.5   ; fm
   :h-values [0.1 0.05 0.01]  ; fm
   :r-max 10.0 ; fm
   })
```

## Expected Results (from paper)

| h (fm) | Naive Start Error | Bessel-Start Error |
|--------|-------------------|-------------------|
| 0.1    | 2.4 × 10⁻³        | 1.1 × 10⁻⁵        |
| 0.05   | 6.1 × 10⁻⁴        | 6.8 × 10⁻⁷        |
| 0.01   | 2.5 × 10⁻⁵        | 7.5 × 10⁻¹⁰       |

## Detailed Code Review

### Wronskian Check Bug
Line 151 in `check-wronskian` has:
```clojure
(* (- 1.0 (* h2-12 fn)) un (get u n))
```
This should probably be `un-1` instead of `(get u n)`, since we already have `un` defined. The Wronskian formula should compare u_n and u_{n-1}.

### Phase Shift Extraction Strategy

The paper requires extracting phase shifts directly from the Numerov wavefunction. The standard approach:

1. **At boundary r = a**: Get u(a) and u'(a) from Numerov
2. **Match to free-space solution**: 
   ```
   u(r) = A * [j_l(kr) * cos(δ) - y_l(kr) * sin(δ)]
   ```
   where j_l and y_l are spherical Bessel functions (f-func and g-func in our code)

3. **Extract phase shift δ** from:
   ```
   tan(δ) = (k * u'(a) * j_l(ka) - u(a) * j_l'(ka)) / 
            (k * u'(a) * y_l(ka) - u(a) * y_l'(ka))
   ```

Alternatively, we can use R-matrix approach:
- Calculate R = u(a) / (a * u'(a))
- Use existing `s-matrix0` or `phase-shift0` functions with this R value

### Available Building Blocks

✅ **Spherical Bessel functions**: `f-func` (j_l) and `g-func` (y_l)  
✅ **Derivatives**: `f-func-deriv` and `g-func-deriv`  
✅ **R-matrix to phase-shift**: `phase-shift0` function exists  
✅ **Numerical derivative**: `deriv` function available  

## Conclusion (UPDATED)

**Current Status**: ~85% complete ✅
- ✅ Numerov integration with Bessel start
- ✅ Numerov integration with Naive start
- ✅ Wronskian check (bug fixed)
- ✅ Stability comparison function
- ✅ Convergence testing function
- ⚠️  Phase shift extraction: Can be done manually, needs wrapper function
- ❌ Phase shift convergence table generator (needs implementation)

**Can We Do the Calculations?** 
- **MOSTLY YES** - Core functionality is complete:
  1. ✅ Wronskian bug fixed
  2. ✅ Naive start implemented (`solve-numerov-naive`)
  3. ✅ Stability analysis implemented (`calculate-stability-data`)
  4. ⚠️  Phase shift extraction: Can be done but needs convenience function
  5. ❌ Convergence table: Needs implementation

**What Works Now**:
- ✅ Run Numerov with both Bessel and Naive starts
- ✅ Check Wronskian stability for both methods
- ✅ Compare wavefunction convergence
- ✅ Calculate stability statistics

**What's Missing**:
- ⚠️  Direct phase shift extraction from Numerov (can work around)
- ❌ Automatic convergence table generation

**Recommended Approach for Phase Shifts**: 
1. Extract u(a) and u'(a) from Numerov solution at boundary
2. Calculate R-matrix: R = u(a) / (a * u'(a))
3. Use existing `phase-shift0` or implement direct matching

**Next Steps**: 
1. ✅ ~~Fix Wronskian bug~~ DONE
2. ✅ ~~Implement naive start~~ DONE
3. ⚠️  Add convenience function to extract phase shift from Numerov wavefunction
4. ❌ Add convergence table generator function

**Estimated Time to 100%**: ~1.5 hours

