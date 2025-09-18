#!/usr/bin/env python3
"""
Plot comparison between DWBA calculations and real experimental data
EXFOR Entry D0557: Lu et al. (2009) - Alpha-proton elastic scattering at 165°
"""

import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Set style for publication-quality plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

# Experimental data from EXFOR D0557
experimental_data = {
    "entry": "D0557",
    "title": "Elastic scattering cross section of proton from helium at 165°",
    "authors": ["Y.F.Lu", "L.Q.Shi", "Z.J.He", "L.Zhang", "B.Zhang", "R.Hutton"],
    "institute": "3CPRFUD (Fudan University)",
    "angle": 165.0,
    "angle_error": 7.3,
    "energy_range": [1.6, 3.6],
    "data_points": [
        {"energy": 1.6, "cross_section": 0.193, "unit": "b/sr"},
        {"energy": 1.7, "cross_section": 0.196, "unit": "b/sr"},
        {"energy": 1.8, "cross_section": 0.211, "unit": "b/sr"},
        {"energy": 1.9, "cross_section": 0.237, "unit": "b/sr"},
        {"energy": 2.0, "cross_section": 0.254, "unit": "b/sr"},
        {"energy": 2.1, "cross_section": 0.263, "unit": "b/sr"},
        {"energy": 2.2, "cross_section": 0.271, "unit": "b/sr"},
        {"energy": 2.3, "cross_section": 0.269, "unit": "b/sr"},
        {"energy": 2.4, "cross_section": 0.251, "unit": "b/sr"},
        {"energy": 2.5, "cross_section": 0.229, "unit": "b/sr"},
        {"energy": 2.6, "cross_section": 0.213, "unit": "b/sr"},
        {"energy": 2.7, "cross_section": 0.193, "unit": "b/sr"},
        {"energy": 2.8, "cross_section": 0.174, "unit": "b/sr"},
        {"energy": 2.9, "cross_section": 0.170, "unit": "b/sr"},
        {"energy": 3.0, "cross_section": 0.160, "unit": "b/sr"},
        {"energy": 3.2, "cross_section": 0.136, "unit": "b/sr"},
        {"energy": 3.4, "cross_section": 0.127, "unit": "b/sr"},
        {"energy": 3.6, "cross_section": 0.117, "unit": "b/sr"}
    ]
}

# Theoretical data (from Clojure calculation)
theoretical_data = [
    {"energy": 1.6, "theoretical": 1.677005, "experimental": 0.193, "ratio": 8.689, "difference": 1.484005},
    {"energy": 1.7, "theoretical": 1.673209, "experimental": 0.196, "ratio": 8.537, "difference": 1.477209},
    {"energy": 1.8, "theoretical": 1.669476, "experimental": 0.211, "ratio": 7.912, "difference": 1.458476},
    {"energy": 1.9, "theoretical": 1.665860, "experimental": 0.237, "ratio": 7.029, "difference": 1.428860},
    {"energy": 2.0, "theoretical": 1.662425, "experimental": 0.254, "ratio": 6.545, "difference": 1.408425},
    {"energy": 2.1, "theoretical": 1.659247, "experimental": 0.263, "ratio": 6.309, "difference": 1.396247},
    {"energy": 2.2, "theoretical": 1.656415, "experimental": 0.271, "ratio": 6.112, "difference": 1.385415},
    {"energy": 2.3, "theoretical": 1.654033, "experimental": 0.269, "ratio": 6.149, "difference": 1.385033},
    {"energy": 2.4, "theoretical": 1.652223, "experimental": 0.251, "ratio": 6.583, "difference": 1.401223},
    {"energy": 2.5, "theoretical": 1.651124, "experimental": 0.229, "ratio": 7.210, "difference": 1.422124},
    {"energy": 2.6, "theoretical": 1.650896, "experimental": 0.213, "ratio": 7.751, "difference": 1.437896},
    {"energy": 2.7, "theoretical": 1.651721, "experimental": 0.193, "ratio": 8.558, "difference": 1.458721},
    {"energy": 2.8, "theoretical": 1.653802, "experimental": 0.174, "ratio": 9.505, "difference": 1.479802},
    {"energy": 2.9, "theoretical": 1.657365, "experimental": 0.170, "ratio": 9.749, "difference": 1.487365},
    {"energy": 3.0, "theoretical": 1.662659, "experimental": 0.160, "ratio": 10.392, "difference": 1.502659},
    {"energy": 3.2, "theoretical": 1.679513, "experimental": 0.136, "ratio": 12.349, "difference": 1.543513},
    {"energy": 3.4, "theoretical": 1.706624, "experimental": 0.127, "ratio": 13.438, "difference": 1.579624},
    {"energy": 3.6, "theoretical": 1.746202, "experimental": 0.117, "ratio": 14.925, "difference": 1.629202}
]

def plot_cross_section_comparison(save_path="plots/experimental_comparison.png"):
    """Plot theoretical vs experimental cross-sections"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    
    # Extract data
    energies = [point["energy"] for point in theoretical_data]
    theoretical = [point["theoretical"] for point in theoretical_data]
    experimental = [point["experimental"] for point in theoretical_data]
    ratios = [point["ratio"] for point in theoretical_data]
    
    # Plot 1: Cross-sections vs Energy
    ax1.plot(energies, theoretical, 'b-o', linewidth=2, markersize=6, 
            label='DWBA Theory', alpha=0.8)
    ax1.errorbar(energies, experimental, yerr=[x * 0.073 for x in experimental], 
                fmt='ro', capsize=5, capthick=2, markersize=6,
                label='Experiment (Lu et al. 2009)', alpha=0.8)
    
    ax1.set_xlabel('Proton Energy (MeV)', fontsize=12)
    ax1.set_ylabel('Differential Cross-Section (b/sr)', fontsize=12)
    ax1.set_title('Alpha-Proton Elastic Scattering at 165°\nDWBA vs Experimental Data', 
                 fontsize=14, fontweight='bold')
    ax1.set_yscale('log')
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=11)
    
    # Add experimental details
    ax1.text(0.02, 0.98, f'EXFOR Entry: {experimental_data["entry"]}\n'
                        f'Institute: {experimental_data["institute"]}\n'
                        f'Error: ±{experimental_data["angle_error"]}%', 
            transform=ax1.transAxes, fontsize=9, 
            verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
    
    # Plot 2: Ratio vs Energy
    ax2.plot(energies, ratios, 'g-s', linewidth=2, markersize=6, alpha=0.8)
    ax2.axhline(y=1.0, color='k', linestyle='--', alpha=0.5, label='Perfect Agreement')
    
    mean_ratio = np.mean(ratios)
    ax2.axhline(y=mean_ratio, color='r', linestyle='-', alpha=0.7, 
               label=f'Mean Ratio: {mean_ratio:.2f}')
    
    ax2.set_xlabel('Proton Energy (MeV)', fontsize=12)
    ax2.set_ylabel('Theory/Experiment Ratio', fontsize=12)
    ax2.set_title('Agreement Analysis\nLower is Better', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.legend(fontsize=11)
    
    # Add statistics
    ax2.text(0.02, 0.98, f'Mean Ratio: {mean_ratio:.2f}\n'
                        f'Range: {min(ratios):.2f} - {max(ratios):.2f}\n'
                        f'Std Dev: {np.std(ratios):.2f}', 
            transform=ax2.transAxes, fontsize=9, 
            verticalalignment='top', bbox=dict(boxstyle='round', facecolor='lightgreen', alpha=0.8))
    
    plt.tight_layout()
    Path("plots").mkdir(exist_ok=True)
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    plt.show()

def plot_parameter_optimization(save_path="plots/parameter_optimization.png"):
    """Plot parameter optimization recommendations"""
    fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))
    
    # Current parameters
    ws_params = [40.0, 2.0, 0.6]  # [V₀, R₀, a₀]
    
    # Parameter space visualization
    V0_range = np.linspace(20, 60, 20)
    R0_range = np.linspace(1.5, 3.0, 20)
    a0_range = np.linspace(0.3, 1.0, 20)
    
    # Placeholder plots showing parameter ranges
    ax1.plot(V0_range, [1.0] * len(V0_range), 'b-', linewidth=2, label='V₀ Range')
    ax1.axvline(x=ws_params[0], color='r', linestyle='--', linewidth=2, label=f'Current: {ws_params[0]} MeV')
    ax1.set_xlabel('V₀ (MeV)', fontsize=12)
    ax1.set_ylabel('Normalized Response', fontsize=12)
    ax1.set_title('Potential Depth Sensitivity', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.legend()
    
    ax2.plot(R0_range, [1.0] * len(R0_range), 'g-', linewidth=2, label='R₀ Range')
    ax2.axvline(x=ws_params[1], color='r', linestyle='--', linewidth=2, label=f'Current: {ws_params[1]} fm')
    ax2.set_xlabel('R₀ (fm)', fontsize=12)
    ax2.set_ylabel('Normalized Response', fontsize=12)
    ax2.set_title('Nuclear Radius Sensitivity', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.legend()
    
    ax3.plot(a0_range, [1.0] * len(a0_range), 'm-', linewidth=2, label='a₀ Range')
    ax3.axvline(x=ws_params[2], color='r', linestyle='--', linewidth=2, label=f'Current: {ws_params[2]} fm')
    ax3.set_xlabel('a₀ (fm)', fontsize=12)
    ax3.set_ylabel('Normalized Response', fontsize=12)
    ax3.set_title('Surface Diffuseness Sensitivity', fontsize=14, fontweight='bold')
    ax3.grid(True, alpha=0.3)
    ax3.legend()
    
    # Optimization recommendations
    ax4.text(0.1, 0.9, 'Parameter Optimization Recommendations:', 
            transform=ax4.transAxes, fontsize=14, fontweight='bold')
    ax4.text(0.1, 0.8, f'Current Parameters:', 
            transform=ax4.transAxes, fontsize=12, fontweight='bold')
    ax4.text(0.1, 0.75, f'  V₀ = {ws_params[0]} MeV', 
            transform=ax4.transAxes, fontsize=11)
    ax4.text(0.1, 0.7, f'  R₀ = {ws_params[1]} fm', 
            transform=ax4.transAxes, fontsize=11)
    ax4.text(0.1, 0.65, f'  a₀ = {ws_params[2]} fm', 
            transform=ax4.transAxes, fontsize=11)
    
    ax4.text(0.1, 0.5, 'Suggested Optimizations:', 
            transform=ax4.transAxes, fontsize=12, fontweight='bold')
    ax4.text(0.1, 0.45, '• Reduce V₀ to ~20-30 MeV', 
            transform=ax4.transAxes, fontsize=11)
    ax4.text(0.1, 0.4, '• Adjust R₀ to ~1.5-2.5 fm', 
            transform=ax4.transAxes, fontsize=11)
    ax4.text(0.1, 0.35, '• Optimize a₀ to ~0.4-0.8 fm', 
            transform=ax4.transAxes, fontsize=11)
    ax4.text(0.1, 0.3, '• Use χ² minimization', 
            transform=ax4.transAxes, fontsize=11)
    
    ax4.text(0.1, 0.15, 'Next Steps:', 
            transform=ax4.transAxes, fontsize=12, fontweight='bold')
    ax4.text(0.1, 0.1, '1. Implement parameter optimization', 
            transform=ax4.transAxes, fontsize=11)
    ax4.text(0.1, 0.05, '2. Use experimental data for fitting', 
            transform=ax4.transAxes, fontsize=11)
    
    ax4.set_xlim(0, 1)
    ax4.set_ylim(0, 1)
    ax4.axis('off')
    
    plt.tight_layout()
    Path("plots").mkdir(exist_ok=True)
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    plt.show()

def plot_experimental_details(save_path="plots/experimental_details.png"):
    """Plot experimental setup and data quality"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    
    # Energy distribution
    energies = [point["energy"] for point in theoretical_data]
    experimental = [point["experimental"] for point in theoretical_data]
    
    ax1.hist(energies, bins=10, alpha=0.7, color='skyblue', edgecolor='black')
    ax1.set_xlabel('Proton Energy (MeV)', fontsize=12)
    ax1.set_ylabel('Number of Data Points', fontsize=12)
    ax1.set_title('Experimental Energy Distribution', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    
    # Cross-section distribution
    ax2.hist(experimental, bins=10, alpha=0.7, color='lightcoral', edgecolor='black')
    ax2.set_xlabel('Cross-Section (b/sr)', fontsize=12)
    ax2.set_ylabel('Number of Data Points', fontsize=12)
    ax2.set_title('Experimental Cross-Section Distribution', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    
    # Add experimental info
    info_text = f"""Experimental Details:
    
Entry: {experimental_data['entry']}
Institute: {experimental_data['institute']}
Angle: {experimental_data['angle']}°
Energy Range: {experimental_data['energy_range'][0]}-{experimental_data['energy_range'][1]} MeV
Data Points: {len(theoretical_data)}
Total Error: ±{experimental_data['angle_error']}%

Authors: {', '.join(experimental_data['authors'][:3])} et al.
Reference: J. NIM B 267, 760 (2009)"""
    
    ax2.text(0.02, 0.98, info_text, transform=ax2.transAxes, fontsize=9, 
            verticalalignment='top', bbox=dict(boxstyle='round', facecolor='lightblue', alpha=0.8))
    
    plt.tight_layout()
    Path("plots").mkdir(exist_ok=True)
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    plt.show()

def generate_all_plots():
    """Generate all experimental comparison plots"""
    print("🔬 Generating experimental comparison visualizations...")
    print("📊 Using real experimental data from EXFOR D0557 (Lu et al. 2009)")
    
    plot_cross_section_comparison()
    plot_parameter_optimization()
    plot_experimental_details()
    
    print("\n✅ All experimental comparison plots generated!")
    print("📁 Plots saved in 'plots/' directory:")
    print("  - experimental_comparison.png")
    print("  - parameter_optimization.png") 
    print("  - experimental_details.png")
    
    # Print summary statistics
    ratios = [point["ratio"] for point in theoretical_data]
    mean_ratio = np.mean(ratios)
    
    print(f"\n📈 Statistical Summary:")
    print(f"  Mean Theory/Exp Ratio: {mean_ratio:.2f}")
    print(f"  Range: {min(ratios):.2f} - {max(ratios):.2f}")
    print(f"  Standard Deviation: {np.std(ratios):.2f}")
    
    if mean_ratio > 5.0:
        print("\n⚠️  Significant discrepancy detected!")
        print("   Parameter optimization strongly recommended")
    else:
        print("\n✅ Good agreement between theory and experiment!")

if __name__ == "__main__":
    generate_all_plots()
    
    print("\n🎯 Key Findings:")
    print("• Real experimental data successfully integrated")
    print("• DWBA calculations show systematic overestimation")
    print("• Parameter optimization needed for better agreement")
    print("• Excellent validation dataset for your DWBA code!")
    print("\n🚀 Ready for parameter fitting and optimization!")
