#!/usr/bin/env python3
"""
Simple DWBA Web Server
A minimal HTTP server for the DWBA dashboard
"""

import json
import math
import http.server
import socketserver
from urllib.parse import urlparse, parse_qs
import threading
import time

# Physical constants
HBARC = 197.7
MU = 745
MASS_FACTOR = (2 * MU) / (HBARC * HBARC)
Z1Z2EE = 2 * 1.44

def woods_saxon(r, V0, R0, a0):
    """Woods-Saxon potential"""
    return -V0 / (1 + math.exp((r - R0) / a0))

def coulomb_potential(r, r0):
    """Coulomb potential for charged sphere"""
    if r > r0:
        return Z1Z2EE / r
    else:
        return r * Z1Z2EE / (r0 * r0)

def r_matrix_nuclear_only(E, V0, R0, a0, a, L):
    """R-matrix calculation for nuclear potential only"""
    dr = 0.001
    N = int(a / dr)
    
    x = dr
    pot = 0
    d2udr2 = 1.0 / dr
    dudr = 1
    ur = dr
    n = 0
    
    while n < N:
        new_pot = woods_saxon(x, V0, R0, a0)
        new_d2udr2 = ((L * (L + 1)) / (x * x) + MASS_FACTOR * (new_pot - E)) * ur
        new_dudr = dudr + new_d2udr2 * dr
        new_ur = ur + new_dudr * dr
        
        x += dr
        pot = new_pot
        d2udr2 = new_d2udr2
        dudr = new_dudr
        ur = new_ur
        n += 1
    
    return ur / dudr

def r_matrix_coulomb_nuclear(E, V0, R0, a0, a, L):
    """R-matrix calculation for Coulomb + nuclear potential"""
    dr = 0.001
    N = int(a / dr)
    
    x = dr
    pot = 0
    d2udr2 = 1.0 / dr
    dudr = 1
    ur = dr
    n = 0
    
    while n < N:
        new_pot = coulomb_potential(x, R0) + woods_saxon(x, V0, R0, a0)
        new_d2udr2 = ((L * (L + 1)) / (x * x) + MASS_FACTOR * (new_pot - E)) * ur
        new_dudr = dudr + new_d2udr2 * dr
        new_ur = ur + new_dudr * dr
        
        x += dr
        pot = new_pot
        d2udr2 = new_d2udr2
        dudr = new_dudr
        ur = new_ur
        n += 1
    
    return ur / dudr / a

def nuclear_phase_shift(E, V0, R0, a0, a, L):
    """Calculate nuclear phase shift"""
    R_coulomb_nuclear = r_matrix_coulomb_nuclear(E, V0, R0, a0, a, L)
    R_coulomb_only = r_matrix_nuclear_only(E, 0, 2.0, 0.6, a, L)
    R_nuclear = R_coulomb_nuclear - R_coulomb_only
    return math.atan(R_nuclear)

class DWBAHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/api/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            response = {"status": "ok", "message": "DWBA Web Dashboard API"}
            self.wfile.write(json.dumps(response).encode())
            
        elif self.path == '/api/parameters':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            response = {
                "default_parameters": {
                    "energies": [5.0, 10.0, 15.0, 20.0, 25.0, 30.0],
                    "L_values": [0, 1, 2, 3, 4, 5],
                    "V0": 40.0,
                    "R0": 2.0,
                    "a0": 0.6,
                    "radius": 3.0
                },
                "parameter_ranges": {
                    "V0": {"min": -100.0, "max": 100.0, "step": 1.0},
                    "R0": {"min": 0.5, "max": 5.0, "step": 0.1},
                    "a0": {"min": 0.1, "max": 2.0, "step": 0.1},
                    "radius": {"min": 1.0, "max": 10.0, "step": 0.1}
                }
            }
            self.wfile.write(json.dumps(response).encode())
            
        else:
            # Serve static files
            super().do_GET()
    
    def do_POST(self):
        if self.path == '/api/calculate':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            
            try:
                params = json.loads(post_data.decode('utf-8'))
                
                energies = [float(e) for e in params['energies']]
                L_values = [int(L) for L in params['L_values']]
                V0 = float(params['V0'])
                R0 = float(params['R0'])
                a0 = float(params['a0'])
                radius = float(params['radius'])
                
                # Calculate phase shifts
                phase_shifts = []
                for E in energies:
                    for L in L_values:
                        phase_shift = nuclear_phase_shift(E, V0, R0, a0, radius, L)
                        phase_shifts.append({
                            "energy": E,
                            "L": L,
                            "phase_shift": phase_shift
                        })
                
                # Calculate R-matrices
                r_matrices = []
                for E in energies:
                    for L in L_values:
                        r_nuclear = r_matrix_nuclear_only(E, V0, R0, a0, radius, L)
                        r_coulomb_nuclear = r_matrix_coulomb_nuclear(E, V0, R0, a0, radius, L)
                        r_matrices.append({
                            "energy": E,
                            "L": L,
                            "r_nuclear": r_nuclear,
                            "r_coulomb_nuclear": r_coulomb_nuclear
                        })
                
                # Calculate potentials
                potentials = []
                radii = [r * 0.1 for r in range(1, 101)]  # 0.1 to 10.0 in steps of 0.1
                for r in radii:
                    ws = woods_saxon(r, V0, R0, a0)
                    coulomb = coulomb_potential(r, R0)
                    combined = ws + coulomb
                    potentials.append({
                        "radius": r,
                        "woods_saxon": ws,
                        "coulomb": coulomb,
                        "combined": combined
                    })
                
                # Calculate cross-sections
                cross_sections = []
                for E in energies:
                    total_cs = sum(math.pow(math.sin(nuclear_phase_shift(E, V0, R0, a0, radius, L)), 2) 
                                 for L in L_values)
                    cross_sections.append({
                        "energy": E,
                        "total_cross_section": total_cs
                    })
                
                response = {
                    "success": True,
                    "data": {
                        "phase_shifts": phase_shifts,
                        "r_matrices": r_matrices,
                        "potentials": potentials,
                        "cross_sections": cross_sections,
                        "parameters": {
                            "energies": energies,
                            "L_values": L_values,
                            "ws_params": [V0, R0, a0],
                            "radius": radius
                        }
                    }
                }
                
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps(response).encode())
                
            except Exception as e:
                self.send_response(500)
                self.send_header('Content-type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                response = {"success": False, "error": str(e)}
                self.wfile.write(json.dumps(response).encode())
        else:
            self.send_response(404)
            self.end_headers()
    
    def end_headers(self):
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()

def start_server(port=3000):
    """Start the DWBA web server"""
    with socketserver.TCPServer(("", port), DWBAHandler) as httpd:
        print(f"🚀 Starting DWBA Web Server on port {port}")
        print(f"🌐 Dashboard available at: http://localhost:{port}")
        print(f"📊 Test page: http://localhost:{port}/test-dashboard.html")
        print("Press Ctrl+C to stop the server")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Server stopped")

if __name__ == "__main__":
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 3000
    start_server(port)
