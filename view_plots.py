#!/usr/bin/env python3
"""
Simple script to view generated DWBA plots
"""

import matplotlib.pyplot as plt
import matplotlib.image as mpimg
from pathlib import Path

def view_plot(plot_path, title):
    """Display a single plot"""
    img = mpimg.imread(plot_path)
    fig, ax = plt.subplots(figsize=(12, 8))
    ax.imshow(img)
    ax.axis('off')
    ax.set_title(title, fontsize=16, fontweight='bold')
    plt.tight_layout()
    plt.show()

def main():
    """View all generated plots"""
    plots_dir = Path("plots")
    
    if not plots_dir.exists():
        print("No plots directory found. Run visualize_dwba.py first.")
        return
    
    plot_files = {
        "phase_shifts.png": "Nuclear Phase Shifts vs Energy",
        "r_matrices.png": "R-Matrix Values Comparison", 
        "potentials.png": "Nuclear Potentials vs Radius",
        "cross_sections.png": "Total Cross-Sections vs Energy",
        "dwba_dashboard.png": "Comprehensive DWBA Dashboard"
    }
    
    print("DWBA Visualization Plots")
    print("=" * 40)
    
    for i, (filename, title) in enumerate(plot_files.items(), 1):
        plot_path = plots_dir / filename
        if plot_path.exists():
            print(f"{i}. {title}")
            print(f"   File: {plot_path}")
            print(f"   Size: {plot_path.stat().st_size / 1024:.1f} KB")
            print()
        else:
            print(f"{i}. {title} - NOT FOUND")
            print()
    
    print("To view individual plots, run:")
    print("  python3 -c \"from view_plots import view_plot; view_plot('plots/phase_shifts.png', 'Phase Shifts')\"")
    print()
    print("Or open the PNG files directly in your image viewer!")

if __name__ == "__main__":
    main()
