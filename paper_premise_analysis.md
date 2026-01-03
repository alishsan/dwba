# Analysis: Paper Premise vs Our Results

## Paper's Main Claims

1. **Bessel start gives better phase shift accuracy** - reduces error by orders of magnitude
2. **Wronskian conservation** - Bessel start maintains W to 10^-14, naive doesn't
3. **Eliminates initialization shock** - prevents excitation of irregular solution

## Our Results

### What We Found:
1. ✅ **Phase shifts match R-matrix method** (1.55 rad) - good!
2. ⚠️  **Naive and Bessel give IDENTICAL phase shifts** by r=10 fm
3. ⚠️  **Wronskian drifts are large** (10^-2) not 10^-14
4. ⚠️  **Errors don't match paper** - ours are smaller but different

## Key Insight: The Premise is About Convergence Rate, Not Final Values

The paper's premise is **NOT** that naive and Bessel give different final answers. It's that:

1. **Convergence Rate**: Bessel start converges faster to the correct solution
2. **Stability**: Bessel start maintains Wronskian better (prevents numerical drift)
3. **Coarse Grid Accuracy**: At larger step sizes, Bessel start is more accurate

### Why Both Give Same Answer at r=10 fm

For a well-behaved ODE, **both methods should converge to the same physical solution** by the boundary. The difference is:
- **How quickly** they converge
- **How stable** the convergence is
- **How accurate** they are at intermediate steps

### What the Paper is Really Showing

The paper's Table 1 shows **errors at the same step size h**, not that the final answers differ. The claim is:
- At h=0.1: Naive has error 2.4×10^-3, Bessel has 1.1×10^-5 (200× better)
- At h=0.01: Naive has error 2.5×10^-5, Bessel has 7.5×10^-10 (33,000× better)

This means: **For the same computational cost (same h), Bessel start is more accurate.**

## Potential Issues with Our Implementation

1. **Wronskian Calculation**: Our drifts are 10^-2, not 10^-14
   - Might be a bug in `check-wronskian`
   - Or we're calculating it wrong

2. **Exact Reference**: We're using R-matrix method, paper might use fine Numerov
   - Could explain error differences

3. **Boundary Radius**: Paper might use smaller radius where differences are visible
   - Or compare at intermediate points

4. **Step Size Effects**: At h=0.01, both might have converged
   - Paper shows differences at h=0.1 and 0.05

## Conclusion

The paper's premise is **valid** - Bessel start should give:
- Better accuracy at coarse step sizes
- Better Wronskian conservation
- Faster convergence

But our results suggest:
- Both converge to same answer (expected)
- Wronskian conservation needs checking (our drifts are too large)
- Error comparison needs verification (might be reference method issue)

## Next Steps

1. **Fix Wronskian calculation** - should be 10^-14, not 10^-2
2. **Test at coarser step sizes** - h=0.1, 0.05 where differences should be visible
3. **Check exact reference** - verify what paper uses
4. **Test at intermediate radii** - see if differences appear earlier

