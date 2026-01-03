#!/usr/bin/env python3
"""
Compare alpha-n p-wave phase shifts for V₀=40 MeV vs V₀=50 MeV
"""

import matplotlib.pyplot as plt
import numpy as np

# Phase shift data for V₀=50 MeV (from previous calculation)
energies = [0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0, 10.5, 11.0, 11.5, 12.0, 12.5, 13.0, 13.5, 14.0, 14.5, 15.0]

phase_50_rad = [0.10935, 0.41032, 0.90611, 1.34731, -1.54759, -1.42474, -1.36378, -1.33472, -1.32325, -1.32206, -1.32716, -1.33619, -1.34774, -1.36087, -1.37499, -1.38970, -1.40472, -1.41984, -1.43494, -1.44992, -1.46471, -1.47926, -1.49354, -1.50753, -1.52121, -1.53459, -1.54765, -1.56040, 1.56876, 1.55663]
phase_50_deg = [p * 180.0 / np.pi for p in phase_50_rad]

# Phase shift data for V₀=40 MeV (new calculation)
phase_40_rad = [0.03927, 0.11920, 0.23200, 0.37073, 0.52330, 0.67436, 0.81077, 0.92555, 1.01763, 1.08935, 1.14420, 1.18565, 1.21663, 1.23947, 1.25599, 1.26759, 1.27533, 1.28003, 1.28232, 1.28270, 1.28156, 1.27920, 1.27586, 1.27175, 1.26702, 1.26180, 1.25618, 1.25026, 1.24411, 1.23778]
phase_40_deg = [p * 180.0 / np.pi for p in phase_40_rad]

# Create the plot
plt.style.use('seaborn-v0_8')
fig, ax = plt.subplots(figsize=(14, 8))

# Plot phase shifts for both cases
ax.plot(energies, phase_50_deg, 'r-', linewidth=2.5, label='V₀=50 MeV', marker='o', markersize=4)
ax.plot(energies, phase_40_deg, 'b-', linewidth=2.5, label='V₀=40 MeV', marker='s', markersize=4)

# Add resonance lines (π/2 = 90°)
ax.axhline(y=90, color='k', linestyle='--', alpha=0.5, linewidth=1, label='π/2 (90°)')
ax.axhline(y=-90, color='k', linestyle=':', alpha=0.5, linewidth=1, label='-π/2 (-90°)')
ax.axhline(y=0, color='k', linestyle='-', alpha=0.3, linewidth=0.5)

# Highlight sign flip regions for V₀=50 MeV
ax.axvspan(2.0, 2.5, alpha=0.15, color='orange', label='Sign flip region 1 (V₀=50)')
ax.axvspan(14.0, 15.0, alpha=0.15, color='green', label='Sign flip region 2 (V₀=50)')

# Mark approximate resonance energies for V₀=50 MeV
ax.axvline(x=2.4, color='orange', linestyle='--', alpha=0.5, linewidth=1)
ax.axvline(x=14.4, color='green', linestyle='--', alpha=0.5, linewidth=1)

ax.set_xlabel('Energy (MeV)', fontsize=12)
ax.set_ylabel('Phase Shift (degrees)', fontsize=12)
ax.set_title('Alpha-N P-Wave Phase Shifts: V₀=40 MeV vs V₀=50 MeV\n(WS: R=2.0 fm, a=0.6 fm)', fontsize=14)
ax.grid(True, alpha=0.3)
ax.legend(loc='best', fontsize=10)
ax.set_xlim(0, 15.5)
ax.set_ylim(-95, 95)

# Add text annotations
ax.text(2.4, 85, 'Resonance 1\n~2.4 MeV\n(V₀=50 only)', ha='center', fontsize=9, 
        bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
ax.text(14.4, 85, 'Resonance 2\n~14.4 MeV\n(V₀=50 only)', ha='center', fontsize=9,
        bbox=dict(boxstyle='round', facecolor='lightgreen', alpha=0.5))

plt.tight_layout()

# Save the plot
output_file = 'alpha_n_phase_shifts_comparison.png'
plt.savefig(output_file, dpi=300, bbox_inches='tight')
print(f"Plot saved as: {output_file}")

# Show the plot
plt.show()

# Analysis
print("\n=== ALPHA-N P-WAVE PHASE SHIFT COMPARISON ===")
print("V₀=50 MeV:")
print("  - Two sign flips/resonances at ~2.4 MeV and ~14.4 MeV")
print("  - Phase shifts pass through π/2 → clear resonances")
print("  - Strong attractive potential")
print("\nV₀=40 MeV:")
print("  - No sign flips, all positive phase shifts")
print("  - Smooth increase from ~2.25° to ~70.9°")
print("  - No resonances in this energy range")
print("  - Weaker attractive potential")
print("\nConclusion: V₀=50 MeV creates bound/resonant states, V₀=40 MeV does not.")



