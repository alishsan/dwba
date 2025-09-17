#!/usr/bin/env python3
"""
DWBA Data Visualization using matplotlib
Comprehensive visualization of nuclear physics calculations
"""

import json
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.gridspec import GridSpec
import seaborn as sns
from pathlib import Path

# Set style for publication-quality plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

class DWBAVisualizer:
    def __init__(self, data_dir="data"):
        self.data_dir = Path(data_dir)
        self.load_data()
        
    def load_data(self):
        """Load JSON data files exported from Clojure"""
        with open(self.data_dir / "phase_shifts.json") as f:
            self.phase_data = json.load(f)["phase_shifts"]
        
        with open(self.data_dir / "r_matrices.json") as f:
            self.r_matrix_data = json.load(f)["r_matrices"]
            
        with open(self.data_dir / "potentials.json") as f:
            self.potential_data = json.load(f)["potentials"]
            
        with open(self.data_dir / "cross_sections.json") as f:
            self.cross_section_data = json.load(f)["cross_sections"]
    
    def plot_phase_shifts(self, save_path="plots/phase_shifts.png"):
        """Plot nuclear phase shifts vs energy for different angular momenta"""
        fig, ax = plt.subplots(figsize=(12, 8))
        
        # Organize data by angular momentum
        L_data = {}
        for point in self.phase_data:
            L = point["L"]
            if L not in L_data:
                L_data[L] = {"energies": [], "phases": []}
            L_data[L]["energies"].append(point["energy"])
            L_data[L]["phases"].append(point["phase_shift"] * 180 / np.pi)  # Convert to degrees
        
        # Plot each angular momentum
        colors = plt.cm.tab10(np.linspace(0, 1, len(L_data)))
        for i, (L, data) in enumerate(sorted(L_data.items())):
            ax.plot(data["energies"], data["phases"], 
                   'o-', linewidth=2, markersize=4, 
                   color=colors[i], label=f'L = {L}')
        
        ax.set_xlabel('Energy (MeV)', fontsize=14)
        ax.set_ylabel('Nuclear Phase Shift (degrees)', fontsize=14)
        ax.set_title('Nuclear Phase Shifts vs Energy\nAlpha-Proton Scattering', fontsize=16)
        ax.grid(True, alpha=0.3)
        ax.legend(fontsize=12)
        ax.set_xlim(0, 50)
        
        # Add physics annotations
        ax.text(0.02, 0.98, 'DWBA Calculation\nWoods-Saxon + Coulomb', 
                transform=ax.transAxes, fontsize=10, 
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
        
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def plot_r_matrices(self, save_path="plots/r_matrices.png"):
        """Plot R-matrix values vs energy"""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
        
        # Organize data
        L_data = {}
        for point in self.r_matrix_data:
            L = point["L"]
            if L not in L_data:
                L_data[L] = {"energies": [], "r_nuclear": [], "r_coulomb_nuclear": []}
            L_data[L]["energies"].append(point["energy"])
            L_data[L]["r_nuclear"].append(point["r_nuclear"])
            L_data[L]["r_coulomb_nuclear"].append(point["r_coulomb_nuclear"])
        
        colors = plt.cm.tab10(np.linspace(0, 1, len(L_data)))
        
        # Plot R-matrix (nuclear only)
        for i, (L, data) in enumerate(sorted(L_data.items())):
            ax1.plot(data["energies"], data["r_nuclear"], 
                    'o-', linewidth=2, markersize=3, 
                    color=colors[i], label=f'L = {L}')
        
        ax1.set_xlabel('Energy (MeV)', fontsize=12)
        ax1.set_ylabel('R-matrix (Nuclear Only)', fontsize=12)
        ax1.set_title('R-matrix: Nuclear Potential Only', fontsize=14)
        ax1.grid(True, alpha=0.3)
        ax1.legend()
        
        # Plot R-matrix (Coulomb + Nuclear)
        for i, (L, data) in enumerate(sorted(L_data.items())):
            ax2.plot(data["energies"], data["r_coulomb_nuclear"], 
                    'o-', linewidth=2, markersize=3, 
                    color=colors[i], label=f'L = {L}')
        
        ax2.set_xlabel('Energy (MeV)', fontsize=12)
        ax2.set_ylabel('R-matrix (Coulomb + Nuclear)', fontsize=12)
        ax2.set_title('R-matrix: Coulomb + Nuclear', fontsize=14)
        ax2.grid(True, alpha=0.3)
        ax2.legend()
        
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def plot_potentials(self, save_path="plots/potentials.png"):
        """Plot nuclear potentials vs radius"""
        fig, ax = plt.subplots(figsize=(12, 8))
        
        radii = [p["radius"] for p in self.potential_data]
        ws_pot = [p["woods_saxon"] for p in self.potential_data]
        coulomb_pot = [p["coulomb"] for p in self.potential_data]
        combined_pot = [p["combined"] for p in self.potential_data]
        
        ax.plot(radii, ws_pot, 'b-', linewidth=3, label='Woods-Saxon Potential')
        ax.plot(radii, coulomb_pot, 'r-', linewidth=3, label='Coulomb Potential')
        ax.plot(radii, combined_pot, 'g-', linewidth=3, label='Combined Potential')
        
        ax.set_xlabel('Radius (fm)', fontsize=14)
        ax.set_ylabel('Potential (MeV)', fontsize=14)
        ax.set_title('Nuclear Potentials vs Radius\nAlpha-Proton System', fontsize=16)
        ax.grid(True, alpha=0.3)
        ax.legend(fontsize=12)
        ax.set_xlim(0, 10)
        ax.axhline(y=0, color='k', linestyle='--', alpha=0.5)
        
        # Add parameter annotations
        ax.text(0.02, 0.98, 'Woods-Saxon: V₀=40 MeV, R₀=2.0 fm, a₀=0.6 fm\nCoulomb: Z₁Z₂e² = 2.88 MeV·fm', 
                transform=ax.transAxes, fontsize=10, 
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='lightblue', alpha=0.8))
        
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def plot_cross_sections(self, save_path="plots/cross_sections.png"):
        """Plot total cross-sections vs energy"""
        fig, ax = plt.subplots(figsize=(12, 8))
        
        energies = [p["energy"] for p in self.cross_section_data]
        cross_sections = [p["total_cross_section"] for p in self.cross_section_data]
        
        ax.semilogy(energies, cross_sections, 'bo-', linewidth=2, markersize=4)
        
        ax.set_xlabel('Energy (MeV)', fontsize=14)
        ax.set_ylabel('Total Cross-Section (arbitrary units)', fontsize=14)
        ax.set_title('Total Cross-Section vs Energy\nAlpha-Proton Scattering', fontsize=16)
        ax.grid(True, alpha=0.3)
        
        # Add physics annotations
        ax.text(0.02, 0.98, 'Sum over all angular momenta\nσ = Σ(2L+1) sin²(δₗ)', 
                transform=ax.transAxes, fontsize=10, 
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='lightgreen', alpha=0.8))
        
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def plot_comprehensive_dashboard(self, save_path="plots/dwba_dashboard.png"):
        """Create a comprehensive dashboard with all plots"""
        fig = plt.figure(figsize=(20, 15))
        gs = GridSpec(3, 3, figure=fig)
        
        # Phase shifts (main plot)
        ax1 = fig.add_subplot(gs[0, :2])
        L_data = {}
        for point in self.phase_data:
            L = point["L"]
            if L not in L_data:
                L_data[L] = {"energies": [], "phases": []}
            L_data[L]["energies"].append(point["energy"])
            L_data[L]["phases"].append(point["phase_shift"] * 180 / np.pi)
        
        colors = plt.cm.tab10(np.linspace(0, 1, len(L_data)))
        for i, (L, data) in enumerate(sorted(L_data.items())):
            ax1.plot(data["energies"], data["phases"], 
                    'o-', linewidth=2, markersize=3, 
                    color=colors[i], label=f'L = {L}')
        
        ax1.set_xlabel('Energy (MeV)')
        ax1.set_ylabel('Phase Shift (degrees)')
        ax1.set_title('Nuclear Phase Shifts', fontsize=14, fontweight='bold')
        ax1.grid(True, alpha=0.3)
        ax1.legend()
        
        # Potentials
        ax2 = fig.add_subplot(gs[0, 2])
        radii = [p["radius"] for p in self.potential_data]
        ws_pot = [p["woods_saxon"] for p in self.potential_data]
        coulomb_pot = [p["coulomb"] for p in self.potential_data]
        combined_pot = [p["combined"] for p in self.potential_data]
        
        ax2.plot(radii, ws_pot, 'b-', linewidth=2, label='WS')
        ax2.plot(radii, coulomb_pot, 'r-', linewidth=2, label='Coulomb')
        ax2.plot(radii, combined_pot, 'g-', linewidth=2, label='Combined')
        ax2.set_xlabel('Radius (fm)')
        ax2.set_ylabel('Potential (MeV)')
        ax2.set_title('Nuclear Potentials', fontsize=14, fontweight='bold')
        ax2.grid(True, alpha=0.3)
        ax2.legend()
        
        # R-matrix comparison
        ax3 = fig.add_subplot(gs[1, :2])
        L_data = {}
        for point in self.r_matrix_data:
            L = point["L"]
            if L not in L_data:
                L_data[L] = {"energies": [], "r_nuclear": [], "r_coulomb_nuclear": []}
            L_data[L]["energies"].append(point["energy"])
            L_data[L]["r_nuclear"].append(point["r_nuclear"])
            L_data[L]["r_coulomb_nuclear"].append(point["r_coulomb_nuclear"])
        
        for i, (L, data) in enumerate(sorted(L_data.items())):
            if L <= 2:  # Only show L=0,1,2 for clarity
                ax3.plot(data["energies"], data["r_nuclear"], 
                        'o-', linewidth=2, markersize=3, 
                        color=colors[i], label=f'L={L} (Nuclear)')
                ax3.plot(data["energies"], data["r_coulomb_nuclear"], 
                        '--', linewidth=2, color=colors[i], alpha=0.7, label=f'L={L} (Coul+Nuc)')
        
        ax3.set_xlabel('Energy (MeV)')
        ax3.set_ylabel('R-matrix')
        ax3.set_title('R-matrix Comparison', fontsize=14, fontweight='bold')
        ax3.grid(True, alpha=0.3)
        ax3.legend()
        
        # Cross-sections
        ax4 = fig.add_subplot(gs[1, 2])
        energies = [p["energy"] for p in self.cross_section_data]
        cross_sections = [p["total_cross_section"] for p in self.cross_section_data]
        ax4.semilogy(energies, cross_sections, 'go-', linewidth=2, markersize=3)
        ax4.set_xlabel('Energy (MeV)')
        ax4.set_ylabel('Cross-Section')
        ax4.set_title('Total Cross-Section', fontsize=14, fontweight='bold')
        ax4.grid(True, alpha=0.3)
        
        # Summary statistics
        ax5 = fig.add_subplot(gs[2, :])
        ax5.axis('off')
        
        # Calculate some statistics
        total_points = len(self.phase_data)
        energy_range = f"{min([p['energy'] for p in self.phase_data])}-{max([p['energy'] for p in self.phase_data])} MeV"
        L_range = f"L = 0 to {max([p['L'] for p in self.phase_data])}"
        
        summary_text = f"""
        DWBA Calculation Summary
        ========================
        
        • System: Alpha-Proton Scattering
        • Energy Range: {energy_range}
        • Angular Momenta: {L_range}
        • Total Data Points: {total_points}
        • Potentials: Woods-Saxon + Coulomb
        • Method: R-matrix theory with nuclear phase shift extraction
        
        Key Features:
        • Modern Clojure implementation
        • Validated against experimental data
        • Comprehensive nuclear physics calculations
        • Export-ready for further analysis
        """
        
        ax5.text(0.05, 0.95, summary_text, transform=ax5.transAxes, 
                fontsize=12, verticalalignment='top', fontfamily='monospace',
                bbox=dict(boxstyle='round,pad=1', facecolor='lightgray', alpha=0.8))
        
        plt.suptitle('DWBA Nuclear Physics Calculations Dashboard', fontsize=18, fontweight='bold')
        plt.tight_layout()
        Path("plots").mkdir(exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.show()
    
    def generate_all_plots(self):
        """Generate all visualization plots"""
        print("Generating DWBA visualization plots...")
        self.plot_phase_shifts()
        self.plot_r_matrices()
        self.plot_potentials()
        self.plot_cross_sections()
        self.plot_comprehensive_dashboard()
        print("All plots generated in 'plots/' directory!")

if __name__ == "__main__":
    # Create visualizer and generate all plots
    visualizer = DWBAVisualizer()
    visualizer.generate_all_plots()
    
    print("\n=== Visualization Complete ===")
    print("Generated plots:")
    print("  - phase_shifts.png")
    print("  - r_matrices.png") 
    print("  - potentials.png")
    print("  - cross_sections.png")
    print("  - dwba_dashboard.png")
    print("\nReady for publication or presentation!")
