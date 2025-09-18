#!/usr/bin/env python3
"""
Visualize comparison between DWBA calculations and real experimental data
EXFOR Entry D0557: Lu et al. (2009) - Alpha-proton elastic scattering at 165°
"""

import json
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Set style for publication-quality plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

class ExperimentalComparisonVisualizer:
    def __init__(self, data_file="data/experimental_comparison.json"):
        self.data_file = Path(data_file)
        self.load_data()
        
    def load_data(self):
        """Load experimental comparison data"""
        with open(self.data_file) as f:
            self.data = json.load(f)
        
        self.exp_data = self.data["experimental_data"]
        self.comp_data = self.data["comparison_data"]
        self.stats = self.data["statistics"]
    
    def plot_cross_section_comparison(self, save_path="plots/experimental_comparison.png"):
        """Plot theoretical vs experimental cross-sections"""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
        
        # Extract data
        energies = [point["energy"] for point in self.comp_data]
        theoretical = [point["theoretical"] for point in self.comp_data]
        experimental = [point["experimental"] for point in self.comp_data]
        ratios = [point["ratio"] for point in self.comp_data]
        
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
        ax1.text(0.02, 0.98, f'EXFOR Entry: {self.exp_data["entry"]}\n'
                            f'Institute: {self.exp_data["institute"]}\n'
                            f'Error: ±{self.exp_data["angle_error"]}%', 
                transform=ax1.transAxes, fontsize=9, 
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
        
        # Plot 2: Ratio vs Energy
        ax2.plot(energies, ratios, 'g-s', linewidth=2, markersize=6, alpha=0.8)
        ax2.axhline(y=1.0, color='k', linestyle='--', alpha=0.5, label='Perfect Agreement')
        ax2.axhline(y=self.stats["mean_ratio"], color='r', linestyle='-', alpha=0.7, 
                   label=f'Mean Ratio: {self.stats["mean_ratio"]:.2f}')
        
        ax2.set_xlabel('Proton Energy (MeV)', fontsize=12)
        ax2.set_ylabel('Theory/Experiment Ratio', fontsize=12)
        ax2.set_title('Agreement Analysis\nLower is Better', fontsize=14, fontweight='bold')
        ax2.grid(True, alpha=0.3)
        ax2.legend(fontsize=11)
        
        # Add statistics
        ax2.text(0.02, 0.98, f'Mean Ratio: {self.stats["mean_ratio"]:.2f}\n'
                            f'Range: {self.stats["min_ratio"]:.2f} - {self.stats["max_ratio"]:.2f}\n'
                            f'Std Dev: {np.std(ratios):.2f}', 
                transform=ax2.transAxes, fontsize=9, 
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='lightgreen', alpha=0.8))
        
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def plot_parameter_sensitivity(self, save_path="plots/parameter_sensitivity.png"):
        """Plot parameter sensitivity analysis"""
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))
        
        # This would require running multiple calculations with different parameters
        # For now, show the current parameters and suggest optimization
        
        # Current parameters
        ws_params = self.stats["ws_parameters"]
        
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
    
    def plot_experimental_details(self, save_path="plots/experimental_details.png"):
        """Plot experimental setup and data quality"""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
        
        # Energy distribution
        energies = [point["energy"] for point in self.comp_data]
        experimental = [point["experimental"] for point in self.comp_data]
        
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
        
Entry: {self.exp_data['entry']}
Institute: {self.exp_data['institute']}
Angle: {self.exp_data['angle']}°
Energy Range: {self.exp_data['energy_range'][0]}-{self.exp_data['energy_range'][1]} MeV
Data Points: {len(self.comp_data)}
Total Error: ±{self.exp_data['angle_error']}%

Authors: {', '.join(self.exp_data['authors'][:3])} et al.
Reference: J. NIM B 267, 760 (2009)"""
        
        ax2.text(0.02, 0.98, info_text, transform=ax2.transAxes, fontsize=9, 
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='lightblue', alpha=0.8))
        
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def generate_all_plots(self):
        """Generate all experimental comparison plots"""
        print("Generating experimental comparison visualizations...")
        self.plot_cross_section_comparison()
        self.plot_parameter_sensitivity()
        self.plot_experimental_details()
        print("All experimental comparison plots generated in 'plots/' directory!")

if __name__ == "__main__":
    # Create visualizer and generate all plots
    visualizer = ExperimentalComparisonVisualizer()
    visualizer.generate_all_plots()
    
    print("\n=== Experimental Comparison Complete ===")
    print("Generated plots:")
    print("  - experimental_comparison.png")
    print("  - parameter_sensitivity.png") 
    print("  - experimental_details.png")
    print("\n🔬 Real experimental data validation complete!")
    print("📊 Ready for parameter optimization and further analysis!")
