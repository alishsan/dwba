# Testing the Bound State Solver

## Quick Test in REPL

The bound state solver has been implemented and is ready to test. Here's how to run it:

### Step 1: Start the REPL

```bash
cd /Users/sanetulla/Development/Clojure/dwba
lein repl
```

### Step 2: Load and Test

```clojure
;; Load the transfer namespace
(require '[dwba.transfer :refer :all])
(require '[functions :refer :all])

;; Define Woods-Saxon parameters: [V0, R0, a0]
(def ws-params [50.0 2.0 0.6])

;; Find 1s bound state (n=1, l=0)
(def result (solve-bound-state ws-params 1 0 nil))

;; View the results
(plot-bound-state-info result)

;; Check the energy
(:energy result)

;; Get the normalized wavefunction
(:normalized-wavefunction result)

;; Check convergence
(:converged? result)
```

### Step 3: Test Different States

```clojure
;; 1p state (n=1, l=1)
(def result-1p (solve-bound-state ws-params 1 1 nil))
(plot-bound-state-info result-1p)

;; 2s state (n=2, l=0) - should have 1 node
(def result-2s (solve-bound-state ws-params 2 0 nil))
(plot-bound-state-info result-2s)
```

## Expected Results

For a Woods-Saxon well with V₀=50 MeV, R₀=2.0 fm, a₀=0.6 fm:

- **1s state**: Energy ≈ -30 to -40 MeV, 0 nodes
- **1p state**: Energy ≈ -20 to -30 MeV, 0 nodes  
- **2s state**: Energy ≈ -10 to -20 MeV, 1 node

## Verification Checks

### 1. Normalization
The wavefunction should be normalized: ∫|u|²dr = 1

```clojure
(let [u (:normalized-wavefunction result)
      h 0.01
      ;; Calculate ∫ u²(r) dr using Simpson's rule
      integrand (mapv #(* % %) u)
      n (count integrand)
      simpson-sum (loop [i 1 sum 0.0]
                    (if (>= i (dec n))
                      sum
                      (let [coeff (if (odd? i) 4.0 2.0)
                            term (* coeff (get integrand i))]
                        (recur (inc i) (+ sum term)))))
      integral (* (/ h 3.0) 
                  (+ (first integrand) 
                     (last integrand) 
                     simpson-sum))]
  (println (format "Normalization: ∫ u²(r) dr = %.8f" integral))
  (println (format "Error: %.2e" (Math/abs (- integral 1.0)))))
```

Should be close to 1.0 (error < 0.01).

### 2. Boundary Condition
For bound states, u(r_max) should be ≈ 0

```clojure
(let [u (:normalized-wavefunction result)
      r-max (:r-max result)
      h (:h result)
      idx (min (dec (count u)) (int (/ r-max h)))
      u-end (get u idx)]
  (println (format "u(r_max) = %.6e" u-end)))
```

Should be close to 0 (|u(r_max)| < 0.01).

### 3. Node Count
The number of nodes should match: nodes = n - 1

```clojure
(:nodes result)  ; Should be n - 1
```

## Troubleshooting

### If energy search doesn't converge:

1. **Adjust search range**: The default is [-V₀, -0.1]. Try:
   ```clojure
   (find-bound-state-energy ws-params 0 1 20.0 0.01 -45.0 -5.0 1e-6)
   ```

2. **Increase r_max**: Bound states decay slowly, may need larger r_max:
   ```clojure
   (solve-bound-state ws-params 1 0 nil 30.0 0.01)
   ```

3. **Check if bound state exists**: For very shallow wells, no bound states may exist.

### If normalization is off:

- Check that r_max is large enough (wavefunction should decay to ~0)
- Verify Simpson's rule integration is correct
- Check for numerical issues near r=0

## What to Look For

✅ **Success indicators**:
- Energy is negative (bound state)
- u(r_max) ≈ 0 (boundary condition satisfied)
- Normalization integral ≈ 1.0
- Node count = n - 1
- Wavefunction decays exponentially at large r

❌ **Warning signs**:
- Energy is positive (not a bound state)
- u(r_max) is large (boundary condition not satisfied)
- Normalization far from 1.0
- Wrong number of nodes

## Next Steps After Testing

Once bound states are working:

1. **Calculate form factors** - overlap integrals between bound states
2. **Implement zero-range approximation** - simplest transfer interaction
3. **Calculate transfer cross-sections** - full DWBA for transfer reactions

## Files

- `src/dwba/transfer.clj` - Implementation
- `test_bound_state_repl.clj` - Test script (load in REPL)
- `BOUND_STATE_IMPLEMENTATION.md` - Implementation details

