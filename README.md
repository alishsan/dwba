# DWBA - Distorted Wave Born Approximation for Nuclear Reactions

A comprehensive Clojure library for performing DWBA (Distorted Wave Born Approximation) calculations in nuclear physics. This library provides tools for analyzing nuclear reactions, computing scattering cross-sections, and visualizing nuclear physics data.

## Features

### Core Functionality
- **Nuclear Potential Modeling**: Woods-Saxon potential implementation for realistic nuclear interactions
- **Scattering Theory**: Complete S-matrix and K-matrix calculations for nuclear scattering
- **R-Matrix Theory**: Advanced R-matrix calculations for nuclear reaction analysis
- **Phase Shift Analysis**: Comprehensive phase shift calculations for different angular momenta
- **Coulomb Interactions**: Full Coulomb potential implementation for charged particle reactions

### Mathematical Capabilities
- **Complex Number Operations**: Custom complex arithmetic with polar and Cartesian representations
- **Special Functions**: Implementation of Hankel functions, Bessel functions, and hypergeometric functions
- **Numerical Integration**: Complex-valued function integration using trapezoidal rule
- **Gamma Functions**: Complex gamma function implementation for advanced calculations

### Visualization
- **Data Plotting**: Integration with cljplot for scientific visualization
- **Phase Shift Plots**: Automatic generation of phase shift vs energy plots
- **Cross-Section Visualization**: Angular distribution and cross-section plotting capabilities
- **Interactive Web Dashboard**: Real-time parameter adjustment and plotting
- **Python/Matplotlib Integration**: Professional publication-quality plots

## Installation

### Prerequisites
- Java 8 or higher
- Leiningen 2.0 or higher

### From Source
```bash
git clone https://github.com/alishsan/dwba.git
cd dwba
lein deps
```

### Dependencies
The project uses the following key dependencies:
- **fastmath**: High-performance mathematical functions and special functions
- **cljplot**: Clojure plotting library for data visualization
- **clojure2d**: 2D graphics capabilities

## Quick Start

### Basic Usage

```clojure
(require '[dwba.core :refer :all])

;; Define Woods-Saxon potential parameters [V0, R0, a0]
(def ws-params [40.0 2.0 0.6])

;; Calculate R-matrix for given energy, potential, radius, and angular momentum
(def r-matrix (r-matrix-a 10.0 ws-params 3.0 0))

;; Calculate phase shift
(def phase (phase-shift 10.0 ws-params 3.0 0))

;; Calculate S-matrix
(def s-matrix (s-matrix 10.0 ws-params 0))
```

### Phase Shift Analysis

```clojure
;; Generate phase shift data for a range of energies
(def energies (range 1 51 1))
(def phase-data 
  (mapv (fn [E] [E (phase-shift E ws-params 3.0 0)]) energies))

;; Plot the results
(plot-phase phase-data)
```

### Cross-Section Calculations

```clojure
;; Calculate partial cross-sections for different angular momenta
(def L-values (range 20))
(def cross-sections
  (mapv (fn [L] [L (sigma-L0 10.0 ws-params 3.0 L)]) L-values))
```

## Web Dashboard

The library includes an interactive web dashboard for real-time parameter adjustment and visualization:

### Starting the Dashboard
```bash
# Start the web server
./start-dashboard.sh

# Or manually
cd web-dashboard
lein run
```

### Features
- **Real-time Parameter Adjustment**: Interactive sliders for Woods-Saxon parameters
- **Live Calculations**: Instant updates when parameters change
- **Interactive Plots**: Zoom, pan, and explore data with Plotly.js
- **Multiple Views**: Phase shifts, R-matrices, potentials, cross-sections
- **Responsive Design**: Works on desktop and mobile devices

### Access
Open your browser to: http://localhost:3000

### Testing
```bash
# Open test page
open test-dashboard.html

# Or test API directly
curl http://localhost:3000/api/health
```

## Python Visualization

For publication-quality plots, use the Python/matplotlib integration:

```bash
# Generate all plots
python3 visualize_dwba.py

# View plot information  
python3 view_plots.py
```

## API Reference

### Core Functions

#### Nuclear Potentials
- `WS [r [V0 R0 a0]]` - Woods-Saxon potential
- `Coulomb-pot [r r0]` - Coulomb potential for charged sphere
- `WS-complex [r [V0 R0 a0]]` - Complex Woods-Saxon potential

#### Scattering Calculations
- `r-matrix-a [E V a L]` - R-matrix calculation
- `s-matrix [E V L]` - S-matrix calculation
- `phase-shift [E V L]` - Phase shift calculation
- `sigma-L0 [E V a L]` - Partial cross-section calculation

#### Complex Number Operations
- `complex-from-cartesian [real imag]` - Create complex number
- `complex-from-polar [arg mag]` - Create complex number from polar coordinates
- `add`, `subt`, `mul`, `div` - Basic arithmetic operations
- `complex-integrate [f a b n]` - Complex function integration

### Physical Constants

The library includes important nuclear physics constants:
- `hbarc` = 197.7 MeV·fm (reduced Planck constant times speed of light)
- `mu` = 745 MeV/c² (reduced mass for alpha-proton system)
- `Z1Z2ee` = 2.88 MeV·fm (Coulomb interaction strength)

## Examples

### Example 1: Basic Phase Shift Calculation

```clojure
;; Calculate phase shifts for different energies
(def energies (range 1 21 1))
(def ws-params [40.0 2.0 0.6]) ; V0=40 MeV, R0=2 fm, a0=0.6 fm

(def phase-shifts
  (mapv (fn [E] 
    {:energy E 
     :phase-shift (phase-shift E ws-params 3.0 0)}) 
    energies))
```

### Example 2: Angular Distribution

```clojure
;; Calculate angular distribution for given energy
(def energy 20.0)
(def angles (range 0.01 3.14 0.01))

(def angular-distribution
  (mapv (fn [theta] 
    {:angle theta 
     :amplitude (ftheta-L energy ws-params 0 theta)}) 
    angles))
```

### Example 3: Complex Potential Analysis

```clojure
;; Use complex Woods-Saxon potential
(def complex-ws-params [40.0 2.0 0.6])
(def complex-potential (WS-complex 2.0 complex-ws-params))
```

## Visualization

The library includes plotting capabilities for scientific visualization:

```clojure
;; Plot phase shift data
(plot-phase phase-data)

;; Generate scatter plots
(-> (b/series [:scatter data {:color :red}])
    (b/preprocess-series)
    (b/add-axes :bottom)
    (b/add-axes :left)
    (r/render-lattice {:width 600 :height 300})
    (show))
```

## Development

### Running Tests
```bash
lein test
```

### REPL Development
```bash
lein repl
```

### Building
```bash
lein uberjar
```

## Theory Background

This library implements the Distorted Wave Born Approximation (DWBA) for nuclear reaction calculations. The DWBA is a first-order perturbation theory approach that accounts for the distortion of the incoming and outgoing waves by the nuclear potential.

Key theoretical components:
- **Woods-Saxon Potential**: Realistic nuclear potential shape
- **R-Matrix Theory**: Framework for nuclear reaction analysis
- **Coulomb Scattering**: Treatment of charged particle interactions
- **Phase Shift Analysis**: Extraction of nuclear structure information

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

Copyright © 2024 Alisher Sanetullaev

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.

## References

- Lane, A. M., & Thomas, R. G. (1958). R-matrix theory of nuclear reactions. Reviews of Modern Physics, 30(2), 257.
- Satchler, G. R. (1983). Direct nuclear reactions. Oxford University Press.
- Hodgson, P. E., Gadioli, E., & Gadioli Erba, E. (1997). Introductory nuclear physics. Oxford University Press.