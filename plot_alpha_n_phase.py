#!/usr/bin/env python3
"""
Plot alpha-n p-wave phase shifts vs energy
Highlight the two sign flips/resonances
"""

import matplotlib.pyplot as plt
import numpy as np

# Phase shift data from Clojure calculation
# Full range: 0.5 to 15.0 MeV, step 0.1
energies = [0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 4.0, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 5.0, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 6.0, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 7.0, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 8.0, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 9.0, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 10.0, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9, 11.0, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 12.0, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 13.0, 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7, 13.8, 13.9, 14.0, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 14.8, 14.9, 15.0]

phase_shifts_rad = [0.10935, 0.15215, 0.20307, 0.26279, 0.33181, 0.41032, 0.49799, 0.59371, 0.69553, 0.80073, 0.90611, 1.00852, 1.10528, 1.19454, 1.27530, 1.34731, 1.41084, 1.46650, 1.51505, 1.55729, -1.54759, -1.51570, -1.48799, -1.46390, -1.44296, -1.42474, -1.40890, -1.39512, -1.38315, -1.37277, -1.36378, -1.35603, -1.34936, -1.34365, -1.33880, -1.33472, -1.33132, -1.32853, -1.32629, -1.32455, -1.32325, -1.32235, -1.32181, -1.32161, -1.32170, -1.32206, -1.32267, -1.32350, -1.32454, -1.32576, -1.32716, -1.32870, -1.33039, -1.33221, -1.33415, -1.33619, -1.33834, -1.34057, -1.34288, -1.34528, -1.34774, -1.35026, -1.35284, -1.35547, -1.35815, -1.36087, -1.36363, -1.36643, -1.36925, -1.37211, -1.37499, -1.37790, -1.38083, -1.38377, -1.38673, -1.38970, -1.39269, -1.39568, -1.39869, -1.40170, -1.40472, -1.40774, -1.41076, -1.41379, -1.41681, -1.41984, -1.42287, -1.42589, -1.42891, -1.43193, -1.43494, -1.43795, -1.44095, -1.44395, -1.44694, -1.44992, -1.45289, -1.45586, -1.45882, -1.46177, -1.46471, -1.46764, -1.47056, -1.47347, -1.47637, -1.47926, -1.48214, -1.48500, -1.48786, -1.49070, -1.49354, -1.49636, -1.49917, -1.50197, -1.50475, -1.50753, -1.51029, -1.51304, -1.51578, -1.51850, -1.52121, -1.52391, -1.52660, -1.52928, -1.53194, -1.53459, -1.53723, -1.53985, -1.54246, -1.54506, -1.54765, -1.55022, -1.55279, -1.55534, -1.55787, -1.56040, -1.56291, -1.56541, -1.56790, -1.57037, 1.56876, 1.56631, 1.56387, 1.56144, 1.55903, 1.55663]

phase_shifts_deg = [p * 180.0 / np.pi for p in phase_shifts_rad]

# Create the plot
plt.style.use('seaborn-v0_8')
fig, ax = plt.subplots(figsize=(12, 8))

# Plot phase shifts
ax.plot(energies, phase_shifts_deg, 'b-', linewidth=2, label='P-wave phase shift (L=1)', marker='o', markersize=3)

# Add resonance lines (π/2 = 90°)
ax.axhline(y=90, color='r', linestyle='--', alpha=0.7, linewidth=1.5, label='π/2 (90°)')
ax.axhline(y=-90, color='r', linestyle=':', alpha=0.7, linewidth=1.5, label='-π/2 (-90°)')
ax.axhline(y=0, color='k', linestyle='-', alpha=0.3, linewidth=0.5)

# Highlight sign flip regions
ax.axvspan(2.0, 2.5, alpha=0.2, color='orange', label='Sign flip region 1')
ax.axvspan(14.0, 15.0, alpha=0.2, color='green', label='Sign flip region 2')

# Mark approximate resonance energies
ax.axvline(x=2.4, color='orange', linestyle='--', alpha=0.7, linewidth=1)
ax.axvline(x=14.4, color='green', linestyle='--', alpha=0.7, linewidth=1)

ax.set_xlabel('Energy (MeV)', fontsize=12)
ax.set_ylabel('Phase Shift (degrees)', fontsize=12)
ax.set_title('Alpha-N P-Wave Phase Shifts vs Energy\n(WS: V₀=50 MeV, R=2.0 fm, a=0.6 fm)', fontsize=14)
ax.grid(True, alpha=0.3)
ax.legend(loc='best', fontsize=10)
ax.set_xlim(0, 15.5)
ax.set_ylim(-95, 95)

# Add text annotations
ax.text(2.4, 85, 'Resonance 1\n~2.4 MeV', ha='center', fontsize=10, 
        bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
ax.text(14.4, 85, 'Resonance 2\n~14.4 MeV', ha='center', fontsize=10,
        bbox=dict(boxstyle='round', facecolor='lightgreen', alpha=0.5))

plt.tight_layout()

# Save the plot
output_file = 'alpha_n_phase_shifts.png'
plt.savefig(output_file, dpi=300, bbox_inches='tight')
print(f"Plot saved as: {output_file}")

# Show the plot
plt.show()

# Analysis
print("\n=== ALPHA-N P-WAVE PHASE SHIFT ANALYSIS ===")
print("Key observations:")
print("1. First sign flip: Between 2.0 and 2.5 MeV")
print("   - Phase shift goes from positive to negative")
print("   - Passes through π/2 (~90°) around 2.4 MeV → RESONANCE")
print("2. Second sign flip: Between 14.0 and 15.0 MeV")
print("   - Phase shift goes from negative to positive")
print("   - Passes through π/2 (~90°) around 14.4 MeV → RESONANCE")
print("\nBoth sign flips indicate p-wave resonances in the alpha-n system!")



