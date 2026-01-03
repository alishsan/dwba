# The Shooting Method for Bound States

## Overview

The **shooting method** is a numerical technique for finding eigenvalues (bound state energies) of the Schrödinger equation. It's called "shooting" because we "shoot" from one boundary, integrate the differential equation, and check if we "hit" the other boundary condition.

## The Problem

For bound states, we need to solve the radial Schrödinger equation:

```
-d²u/dr² + [V_eff(r) - E]u = 0
```

With boundary conditions:
- **u(0) = 0** (wavefunction must be zero at origin)
- **u(r → ∞) → 0** (wavefunction must decay at infinity)

The challenge: We don't know the energy E! We need to find the **eigenvalue** E such that both boundary conditions are satisfied.

## How the Shooting Method Works

### Step 1: Guess an Energy

Start with a guess for the energy E (must be negative for bound states).

### Step 2: "Shoot" from r=0

1. Start at r=0 with initial conditions:
   - u(0) = 0
   - u'(0) = 1 (or some normalization - exact value doesn't matter)

2. Integrate the Schrödinger equation from r=0 to r=r_max using Numerov's method

3. Check what happens at r=r_max:
   - If u(r_max) ≈ 0 → **Success!** We found a bound state
   - If u(r_max) > 0 → Energy too high (wavefunction growing)
   - If u(r_max) < 0 → Energy too low (wavefunction growing in opposite direction)

### Step 3: Adjust and Repeat

Use **bisection** (or another root-finding method) to adjust the energy:

```
If u(r_max) > 0: Try lower energy
If u(r_max) < 0: Try higher energy
```

Repeat until u(r_max) ≈ 0 (within tolerance).

## Visual Analogy

Think of it like shooting a cannon:

```
r=0 (cannon)                    r=r_max (target)
    |                                |
    |  u(r)                          |  u(r_max) = ?
    |                                |
    |  ────────────────────────────> |
    |                                |
```

- **Goal**: Find the right "angle" (energy) so the "projectile" (wavefunction) hits the target (u(r_max) = 0)
- **Too high energy**: Projectile overshoots (u(r_max) > 0)
- **Too low energy**: Projectile undershoots (u(r_max) < 0)
- **Correct energy**: Projectile hits target (u(r_max) ≈ 0)

## Implementation in Our Code

### Two-Stage Approach

Our implementation uses a **two-stage** shooting method:

#### Stage 1: Coarse Scan (`find-energy-with-nodes`)

1. **Scan energy range**: Test many energies from E_min to E_max
2. **For each energy**:
   - Integrate Numerov from r=0 to r_max
   - Count nodes in the wavefunction
   - Check boundary value u(r_max)
3. **Find best candidate**: Energy with correct number of nodes and smallest |u(r_max)|

#### Stage 2: Refinement (`refine-bound-state-energy`)

1. **Bisection around best candidate**: Start with E_guess from Stage 1
2. **Search range**: ±3 MeV around E_guess
3. **Bisection algorithm**:
   ```
   E_lo = E_guess - 3 MeV
   E_hi = E_guess + 3 MeV
   
   Loop:
     E_mid = (E_lo + E_hi) / 2
     Integrate at E_mid
     Check u(r_max)
     
     If u(r_max) and u(E_lo) have same sign:
       E_lo = E_mid  (root is in [E_mid, E_hi])
     Else:
       E_hi = E_mid  (root is in [E_lo, E_mid])
     
     Repeat until |E_hi - E_lo| < tolerance
   ```

## Why It Works

### Physics

For bound states:
- **E < 0**: Wavefunction decays exponentially at large r
- **E > 0**: Wavefunction oscillates (scattering state)
- **E = eigenvalue**: Wavefunction decays exactly to zero at infinity

### Mathematics

The boundary condition u(r_max) = 0 defines a **root-finding problem**:
- f(E) = u(r_max; E) = 0
- We use bisection to find the root

### Why Count Nodes?

The number of nodes (zeros) in the wavefunction identifies which bound state:
- **0 nodes** → 1s state (ground state)
- **1 node** → 2s state (first excited)
- **2 nodes** → 3s state (second excited)
- etc.

This helps us find the **correct** bound state, not just any one.

## Algorithm Flow

```
1. Start with energy range [E_min, E_max]
2. Coarse scan:
   - Test energies: E_min, E_min+ΔE, E_min+2ΔE, ..., E_max
   - For each: integrate → count nodes → check u(r_max)
   - Find energy with correct nodes
3. Refinement:
   - Use bisection around best candidate
   - Narrow down to exact energy where u(r_max) ≈ 0
4. Return: {energy, wavefunction, nodes, converged?}
```

## Key Functions

### `find-energy-with-nodes`
- **Purpose**: Coarse scan to find approximate energy
- **Method**: Test many energies, find one with correct nodes
- **Returns**: Best candidate energy

### `refine-bound-state-energy`
- **Purpose**: Refine energy to satisfy boundary condition
- **Method**: Bisection root-finding
- **Returns**: Refined energy with u(r_max) ≈ 0

### `solve-bound-state-numerov`
- **Purpose**: Integrate Schrödinger equation for given energy
- **Method**: Numerov algorithm
- **Returns**: Wavefunction u(r) at each grid point

## Example

For a 1s bound state (n=1, l=0):

1. **Coarse scan**: Test energies from -35 to -15 MeV
   - E = -30 MeV → u(r_max) = +1000, nodes = 0 ✓
   - E = -20 MeV → u(r_max) = -500, nodes = 0 ✓
   - Best: E ≈ -25 MeV (smallest |u(r_max)|)

2. **Refinement**: Bisect around -25 MeV
   - E = -25 MeV → u(r_max) = +50
   - E = -24 MeV → u(r_max) = -30
   - E = -24.5 MeV → u(r_max) = +10
   - E = -24.3 MeV → u(r_max) ≈ 0 ✓

3. **Result**: E = -24.3 MeV, u(r_max) ≈ 0, converged!

## Advantages

- **Simple**: Easy to understand and implement
- **Robust**: Works for various potentials
- **Flexible**: Can find any bound state by counting nodes
- **Accurate**: Bisection converges to exact eigenvalue

## Limitations

- **Slow**: Requires many integrations
- **Sensitive**: Boundary condition must be checked carefully
- **r_max dependency**: Need large enough r_max for accurate boundary check

## In Our Code

The shooting method is implemented in:
- `find-bound-state-energy` - Main function
- `find-energy-with-nodes` - Coarse scan stage
- `refine-bound-state-energy` - Refinement stage
- `solve-bound-state-numerov` - Integration step

## References

### Textbooks on Numerical Methods

1. **Press, W. H., Teukolsky, S. A., Vetterling, W. T., & Flannery, B. P.** (2007). *Numerical Recipes: The Art of Scientific Computing* (3rd ed.). Cambridge University Press.
   - Chapter 17: Two Point Boundary Value Problems
   - Section 17.2: Shooting to a Fitting Point
   - Section 17.3: Relaxation Methods

2. **Koonin, S. E., & Meredith, D. C.** (1990). *Computational Physics: Fortran Version*. Westview Press.
   - Chapter 3: Bound States and One-Dimensional Scattering
   - Section 3.1: The Shooting Method

3. **Landau, R. H., Páez, M. J., & Bordeianu, C. C.** (2015). *Computational Physics: Problem Solving with Python* (3rd ed.). Wiley-VCH.
   - Chapter 6: Quantum Mechanics
   - Section 6.2: Bound States

### Quantum Mechanics Textbooks

4. **Griffiths, D. J.** (2018). *Introduction to Quantum Mechanics* (3rd ed.). Cambridge University Press.
   - Chapter 2: Time-Independent Schrödinger Equation
   - Section 2.5: The Free Particle
   - Section 2.6: The Delta-Function Potential

5. **Sakurai, J. J., & Napolitano, J.** (2020). *Modern Quantum Mechanics* (3rd ed.). Cambridge University Press.
   - Chapter 3: Theory of Angular Momentum
   - Bound state calculations

### Nuclear Physics References

6. **Satchler, G. R.** (1983). *Direct Nuclear Reactions*. Oxford University Press.
   - Chapter 8: Transfer Reactions
   - Bound state wavefunctions for transfer reactions

7. **Hodgson, P. E., Gadioli, E., & Gadioli Erba, E.** (1997). *Introductory Nuclear Physics*. Oxford University Press.
   - Chapter 11: Nuclear Reactions
   - Bound states and form factors

### Numerical Methods for Schrödinger Equation

8. **Ixaru, L. G.** (1984). *Numerical Methods for Differential Equations and Applications*. Springer.
   - Chapter 4: Numerov's Method
   - Boundary value problems

9. **Johnson, B. R.** (1977). "The Multistep Method for Solving the Schrödinger Equation." *Journal of Chemical Physics*, 67(9), 4086-4093.
   - Numerov method for bound states

10. **Pruess, S., & Fulton, C. T.** (1993). "Mathematical Software for Sturm-Liouville Problems." *ACM Transactions on Mathematical Software*, 19(3), 360-376.
    - Shooting method implementation

### Online Resources

11. **Wikipedia: Shooting Method**
    - https://en.wikipedia.org/wiki/Shooting_method
    - General overview of the shooting method

12. **Wikipedia: Numerov's Method**
    - https://en.wikipedia.org/wiki/Numerov%27s_method
    - Numerical integration method used in our implementation

### Papers on Bound State Calculations

13. **Marquardt, N., & Cederbaum, L. S.** (2006). "Bound States in the Continuum." *Physical Review Letters*, 97(17), 173003.
    - Modern applications of bound state calculations

14. **Taut, M.** (1993). "Two Electrons in an External Oscillator Potential: Particular Analytic Solutions of a Coulomb Correlation Problem." *Physical Review A*, 48(5), 3561-3566.
    - Exact solutions for comparison with numerical methods
