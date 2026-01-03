# Verification Status of Paper Calculations

## ⚠️ NOT YET VERIFIED

The numerical values in the paper have **NOT** been verified against the code calculations yet.

## Paper Values to Verify

### Table 1: Phase Shift Convergence
| h (fm) | Naive Start Error | Bessel-Start Error |
|--------|-------------------|-------------------|
| 0.1    | 2.4 × 10⁻³        | 1.1 × 10⁻⁵        |
| 0.05   | 6.1 × 10⁻⁴        | 6.8 × 10⁻⁷        |
| 0.01   | 2.5 × 10⁻⁵        | 7.5 × 10⁻¹⁰       |

### Table 2: Wronskian Drift (in improved version)
| h (fm) | Naive Start Drift | Bessel-Start Drift |
|--------|-------------------|-------------------|
| 0.1    | 1.2 × 10⁻⁶        | 2.3 × 10⁻¹⁴       |
| 0.05   | 3.1 × 10⁻⁷        | 1.8 × 10⁻¹⁴       |
| 0.01   | 6.4 × 10⁻⁸        | 1.5 × 10⁻¹⁴       |

## Unknown Parameters

The paper doesn't specify:

1. **Boundary radius for phase shift extraction**
   - Code currently uses: r = 10.0 fm
   - Paper might use: different value (e.g., 5.0, 8.0, or where potential becomes negligible)

2. **"Exact" phase shift reference method**
   - Code uses: Numerov with h = 0.001 fm
   - Paper might use: R-matrix method, or different fine-grid value

3. **Mass parameters**
   - Code uses: μ = 869.4 MeV/c² (14C+n system)
   - Paper might use: different reduced mass

4. **Integration range (r_max)**
   - Code uses: r_max = 10.0 fm (same as boundary)
   - Paper might use: different range

## Verification Script

Created: `verify_paper_calculations.clj`

This script will:
1. Calculate phase shift errors for all h values
2. Compare with paper values
3. Calculate Wronskian drifts
4. Report matches/mismatches

## How to Verify

1. **Run the verification script:**
   ```bash
   # In Clojure REPL or via lein
   (load-file "verify_paper_calculations.clj")
   ```

2. **Check the output:**
   - If values match within ~20-30%: Likely correct (rounding differences)
   - If values differ significantly: Need to investigate parameters

3. **If values don't match:**
   - Try different boundary radii (5.0, 8.0, 10.0, 12.0 fm)
   - Try different "exact" reference (R-matrix method)
   - Check mass parameters match paper
   - Verify integration range

## Expected Issues

1. **Boundary radius sensitivity**
   - Phase shifts can be sensitive to matching radius
   - Need to ensure boundary is in asymptotic region

2. **Numerical precision**
   - Paper values may be rounded
   - Code might have slightly different precision

3. **Different reference methods**
   - If paper used R-matrix for "exact", our Numerov reference will differ
   - Need to verify which method paper used

## Action Items

- [ ] Run `verify_paper_calculations.clj`
- [ ] Compare results with paper values
- [ ] If mismatched, try different boundary radii
- [ ] If still mismatched, check if paper used different "exact" method
- [ ] Update paper with verified values OR update code to match paper methodology
- [ ] Document all parameters used in paper

## Recommendation

**Before publication, MUST verify:**
1. All numerical values match code calculations
2. All parameters are clearly documented
3. Methodology for "exact" reference is specified
4. Boundary radius choice is justified

