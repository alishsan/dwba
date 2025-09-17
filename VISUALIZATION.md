# DWBA Visualization Guide

This guide explains how to use matplotlib for visualizing DWBA calculations from your Clojure library.

## Overview

The visualization system consists of:
1. **Clojure data export** - Export calculation results to JSON
2. **Python visualization** - Create publication-quality plots with matplotlib
3. **Multiple plot types** - Phase shifts, R-matrices, potentials, cross-sections

## Quick Start

### 1. Export Data from Clojure
```bash
clojure -M export_data.clj
```
This creates JSON files in the `data/` directory:
- `phase_shifts.json` - Nuclear phase shifts vs energy
- `r_matrices.json` - R-matrix values
- `potentials.json` - Nuclear potential functions
- `cross_sections.json` - Total cross-sections

### 2. Generate Visualizations
```bash
python3 visualize_dwba.py
```
This creates publication-quality plots in the `plots/` directory.

### 3. View Results
```bash
python3 view_plots.py
```
Or open the PNG files directly in your image viewer.

## Generated Plots

### 1. Phase Shifts (`phase_shifts.png`)
- **Content**: Nuclear phase shifts vs energy for different angular momenta
- **Features**: L=0,1,2,3,4,5 curves with different colors
- **Physics**: Shows resonance structure and energy dependence
- **Use**: Validate against experimental data

### 2. R-Matrices (`r_matrices.png`)
- **Content**: R-matrix values with/without Coulomb interactions
- **Features**: Side-by-side comparison of nuclear vs Coulomb+nuclear
- **Physics**: Shows effect of Coulomb potential on R-matrix
- **Use**: Understand nuclear interaction effects

### 3. Potentials (`potentials.png`)
- **Content**: Woods-Saxon, Coulomb, and combined potentials vs radius
- **Features**: Three curves showing different potential contributions
- **Physics**: Visualizes nuclear potential shape and Coulomb effects
- **Use**: Understand potential model parameters

### 4. Cross-Sections (`cross_sections.png`)
- **Content**: Total cross-section vs energy (log scale)
- **Features**: Sum over all angular momenta
- **Physics**: Shows overall scattering strength
- **Use**: Compare with experimental cross-sections

### 5. Dashboard (`dwba_dashboard.png`)
- **Content**: Comprehensive overview of all calculations
- **Features**: Multi-panel layout with summary statistics
- **Physics**: Complete picture of nuclear physics calculations
- **Use**: Presentations, papers, overview

## Customization

### Modify Plot Styles
Edit `visualize_dwba.py` to customize:
- Colors and line styles
- Figure sizes and layouts
- Fonts and annotations
- Grid styles and transparency

### Add New Plot Types
Extend the `DWBAVisualizer` class:
```python
def plot_custom_analysis(self, save_path="plots/custom.png"):
    # Your custom plotting code here
    pass
```

### Export Different Data
Modify `export_data.clj` to export additional quantities:
- Angular distributions
- S-matrix elements
- Phase shift differences
- Parameter sensitivity data

## Dependencies

### Python Requirements
```bash
pip install matplotlib numpy seaborn
```

### Clojure Requirements
- Your DWBA library
- JSON export functionality

## File Structure
```
dwba/
├── data/                    # Exported JSON data
│   ├── phase_shifts.json
│   ├── r_matrices.json
│   ├── potentials.json
│   └── cross_sections.json
├── plots/                   # Generated visualizations
│   ├── phase_shifts.png
│   ├── r_matrices.png
│   ├── potentials.png
│   ├── cross_sections.png
│   └── dwba_dashboard.png
├── export_data.clj         # Clojure data export
├── visualize_dwba.py       # Python visualization
├── view_plots.py          # Plot viewer
└── VISUALIZATION.md       # This guide
```

## Advantages of This Approach

### 1. **Language Separation**
- **Clojure**: Excellent for mathematical calculations and data processing
- **Python**: Superior for visualization and data analysis
- **Best of both worlds**: Use each language for its strengths

### 2. **Accessibility**
- **Standard tools**: matplotlib is widely used in scientific computing
- **No Clojure dependency**: Anyone can view and modify plots
- **Cross-platform**: Works on any system with Python

### 3. **Flexibility**
- **Easy customization**: Modify plots without touching Clojure code
- **Multiple formats**: Export to PNG, PDF, SVG, etc.
- **Integration**: Works with Jupyter notebooks, web apps, etc.

### 4. **Publication Ready**
- **High quality**: 300 DPI output suitable for papers
- **Professional styling**: Clean, publication-ready appearance
- **Consistent branding**: Unified style across all plots

## Tips for Best Results

### 1. **Data Quality**
- Use sufficient energy points for smooth curves
- Include multiple angular momenta for comprehensive analysis
- Validate calculations before visualization

### 2. **Plot Design**
- Use consistent colors across related plots
- Add physics annotations and parameter values
- Include error bars when available
- Use appropriate scales (linear vs log)

### 3. **File Management**
- Keep data and plots organized in separate directories
- Use descriptive filenames with timestamps
- Version control your visualization scripts
- Document parameter choices

## Troubleshooting

### Common Issues
1. **Missing dependencies**: Install required Python packages
2. **Font warnings**: Ignore subscript warnings, plots still work
3. **File not found**: Run export_data.clj first
4. **Plot not displaying**: Check matplotlib backend settings

### Getting Help
- Check matplotlib documentation for advanced features
- Modify existing plots to learn the API
- Use matplotlib gallery for inspiration
- Ask for help with specific visualization needs

## Future Enhancements

### Potential Improvements
1. **Interactive plots**: Use plotly for web-based interactivity
2. **Animation**: Create animated plots showing parameter changes
3. **3D visualization**: Plot phase shifts in 3D (E, L, δ)
4. **Web dashboard**: Create a web interface for data exploration
5. **Export formats**: Add PDF, SVG, and other output formats

This visualization system makes your DWBA library much more competitive by providing professional-quality plots that are easy to generate, customize, and share!
