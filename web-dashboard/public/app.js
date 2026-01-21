// DWBA Web Dashboard JavaScript
class DWBADashboard {
    constructor() {
        this.apiBase = '';
        this.currentData = null;
        this.initializeEventListeners();
        this.loadDefaultParameters();
    }

    initializeEventListeners() {
        // Parameter sliders
        ['V0', 'R0', 'a0', 'radius'].forEach(param => {
            const slider = document.getElementById(param);
            const valueDisplay = document.getElementById(`${param}-value`);
            
            slider.addEventListener('input', (e) => {
                const value = parseFloat(e.target.value);
                const unit = param === 'V0' ? 'MeV' : 'fm';
                valueDisplay.textContent = `${value} ${unit}`;
            });
        });

        // Calculate button
        document.getElementById('calculate-btn').addEventListener('click', () => {
            this.calculateDWBA();
        });

        // Reset button
        document.getElementById('reset-btn')?.addEventListener('click', () => {
            this.resetParameters();
        });
    }

    async loadDefaultParameters() {
        try {
            const response = await fetch(`${this.apiBase}/api/parameters`);
            const data = await response.json();
            
            if (data.default_parameters) {
                this.setParameters(data.default_parameters);
            }
        } catch (error) {
            console.error('Error loading default parameters:', error);
        }
    }

    setParameters(params) {
        document.getElementById('V0').value = params.V0;
        document.getElementById('R0').value = params.R0;
        document.getElementById('a0').value = params.a0;
        document.getElementById('radius').value = params.radius;
        document.getElementById('energy-range').value = params.energies.join(',');
        document.getElementById('L-values').value = params.L_values.join(',');
        
        if (params.E_ex !== undefined) document.getElementById('E_ex').value = params.E_ex;
        if (params.lambda !== undefined) document.getElementById('lambda').value = params.lambda;
        if (params.beta !== undefined) document.getElementById('beta').value = params.beta;
        if (params.reaction_type !== undefined) document.getElementById('reaction_type').value = params.reaction_type;
        
        // Update slider displays
        document.getElementById('V0-value').textContent = `${params.V0} MeV`;
        document.getElementById('R0-value').textContent = `${params.R0} fm`;
        document.getElementById('a0-value').textContent = `${params.a0} fm`;
        document.getElementById('radius-value').textContent = `${params.radius} fm`;
    }

    getParameters() {
        return {
            V0: parseFloat(document.getElementById('V0').value),
            R0: parseFloat(document.getElementById('R0').value),
            a0: parseFloat(document.getElementById('a0').value),
            radius: parseFloat(document.getElementById('radius').value),
            energies: document.getElementById('energy-range').value
                .split(',')
                .map(s => s.trim())
                .filter(s => s.length > 0),
            L_values: document.getElementById('L-values').value
                .split(',')
                .map(s => s.trim())
                .filter(s => s.length > 0),
            E_ex: parseFloat(document.getElementById('E_ex').value),
            lambda: parseInt(document.getElementById('lambda').value),
            beta: parseFloat(document.getElementById('beta').value),
            reaction_type: document.getElementById('reaction_type').value
        };
    }

    showStatus(message, type = 'info') {
        const statusDiv = document.getElementById('status-messages');
        const alertClass = type === 'error' ? 'error' : type === 'success' ? 'success' : 'alert-info';
        
        statusDiv.innerHTML = `
            <div class="${alertClass}">
                <i class="fas fa-${type === 'error' ? 'exclamation-triangle' : type === 'success' ? 'check-circle' : 'info-circle'}"></i>
                ${message}
            </div>
        `;
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            statusDiv.innerHTML = '';
        }, 5000);
    }

    async calculateDWBA() {
        const startTime = Date.now();
        const calculateBtn = document.getElementById('calculate-btn');
        
        try {
            // Show loading state
            calculateBtn.disabled = true;
            calculateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Calculating...';
            this.showStatus('Performing DWBA calculations...', 'info');

            const params = this.getParameters();
            
            // Validate parameters
            if (params.energies.length === 0 || params.L_values.length === 0) {
                throw new Error('Please provide valid energy range and angular momenta');
            }

            // Calculate all reaction types in parallel
            const [basicResult, elasticResult, inelasticResult, transferResult] = await Promise.all([
                fetch(`${this.apiBase}/api/calculate`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(params)
                }).then(r => r.json()),
                fetch(`${this.apiBase}/api/elastic`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(params)
                }).then(r => r.json()),
                fetch(`${this.apiBase}/api/inelastic`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(params)
                }).then(r => r.json()),
                fetch(`${this.apiBase}/api/transfer`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(params)
                }).then(r => r.json())
            ]);

            const calculationTime = Date.now() - startTime;

            // Combine all results
            if (basicResult.success) {
                this.currentData = basicResult.data;
                if (elasticResult.success) this.currentData.elastic = elasticResult.data.elastic;
                if (inelasticResult.success) this.currentData.inelastic = inelasticResult.data.inelastic;
                if (transferResult.success) this.currentData.transfer = transferResult.data.transfer;
                
                this.updateAllPlots();
                this.updateDashboardStats(calculationTime);
                this.showStatus(`Calculation completed successfully in ${calculationTime}ms`, 'success');
            } else {
                throw new Error(basicResult.error || 'Calculation failed');
            }

        } catch (error) {
            console.error('Calculation error:', error);
            this.showStatus(`Error: ${error.message}`, 'error');
        } finally {
            // Reset button state
            calculateBtn.disabled = false;
            calculateBtn.innerHTML = '<i class="fas fa-calculator"></i> Calculate DWBA';
        }
    }

    updateAllPlots() {
        if (!this.currentData) return;

        this.plotPhaseShifts();
        this.plotRMatrices();
        this.plotPotentials();
        this.plotCrossSections();
        if (this.currentData.elastic) this.plotElastic();
        if (this.currentData.inelastic) this.plotInelastic();
        if (this.currentData.transfer) this.plotTransfer();
        this.plotDashboard();
    }

    plotPhaseShifts() {
        const data = this.currentData.phase_shifts;
        const traces = {};

        // Group data by angular momentum
        data.forEach(point => {
            const L = point.L;
            if (!traces[L]) {
                traces[L] = {
                    x: [],
                    y: [],
                    name: `L = ${L}`,
                    type: 'scatter',
                    mode: 'lines+markers',
                    line: { width: 3 },
                    marker: { size: 6 }
                };
            }
            traces[L].x.push(point.energy);
            traces[L].y.push(point.phase_shift * 180 / Math.PI); // Convert to degrees
        });

        const plotData = Object.values(traces);
        const layout = {
            title: 'Nuclear Phase Shifts vs Energy',
            xaxis: { title: 'Energy (MeV)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'Phase Shift (degrees)', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            legend: { x: 0.02, y: 0.98 },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('phase-plot', plotData, layout, {responsive: true});
    }

    plotRMatrices() {
        const data = this.currentData.r_matrices;
        const traces = {};

        data.forEach(point => {
            const L = point.L;
            if (!traces[L]) {
                traces[L] = {
                    nuclear: { x: [], y: [], name: `L = ${L} (Nuclear)`, type: 'scatter', mode: 'lines+markers' },
                    coulomb_nuclear: { x: [], y: [], name: `L = ${L} (Coul+Nuc)`, type: 'scatter', mode: 'lines+markers', line: { dash: 'dash' } }
                };
            }
            traces[L].nuclear.x.push(point.energy);
            traces[L].nuclear.y.push(point.r_nuclear);
            traces[L].coulomb_nuclear.x.push(point.energy);
            traces[L].coulomb_nuclear.y.push(point.r_coulomb_nuclear);
        });

        const plotData = [];
        Object.values(traces).forEach(trace => {
            plotData.push(trace.nuclear, trace.coulomb_nuclear);
        });

        const layout = {
            title: 'R-Matrix Values Comparison',
            xaxis: { title: 'Energy (MeV)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'R-Matrix', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            legend: { x: 0.02, y: 0.98 },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('rmatrix-plot', plotData, layout, {responsive: true});
    }

    plotPotentials() {
        const data = this.currentData.potentials;
        
        const woodsSaxon = {
            x: data.map(p => p.radius),
            y: data.map(p => p.woods_saxon),
            name: 'Woods-Saxon',
            type: 'scatter',
            mode: 'lines',
            line: { color: 'blue', width: 3 }
        };

        const coulomb = {
            x: data.map(p => p.radius),
            y: data.map(p => p.coulomb),
            name: 'Coulomb',
            type: 'scatter',
            mode: 'lines',
            line: { color: 'red', width: 3 }
        };

        const combined = {
            x: data.map(p => p.radius),
            y: data.map(p => p.combined),
            name: 'Combined',
            type: 'scatter',
            mode: 'lines',
            line: { color: 'green', width: 3 }
        };

        const layout = {
            title: 'Nuclear Potentials vs Radius',
            xaxis: { title: 'Radius (fm)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'Potential (MeV)', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            legend: { x: 0.02, y: 0.98 },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('potential-plot', [woodsSaxon, coulomb, combined], layout, {responsive: true});
    }

    plotCrossSections() {
        const data = this.currentData.cross_sections;
        
        const trace = {
            x: data.map(p => p.energy),
            y: data.map(p => p.total_cross_section),
            name: 'Total Cross-Section',
            type: 'scatter',
            mode: 'lines+markers',
            line: { color: 'purple', width: 3 },
            marker: { size: 6 }
        };

        const layout = {
            title: 'Total Cross-Sections vs Energy',
            xaxis: { title: 'Energy (MeV)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'Cross-Section (arbitrary units)', type: 'log', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('cross-section-plot', [trace], layout, {responsive: true});
    }

    plotDashboard() {
        // Create a comprehensive dashboard with multiple subplots
        const phaseData = this.currentData.phase_shifts;
        const potentialData = this.currentData.potentials;
        const crossSectionData = this.currentData.cross_sections;

        // Phase shifts (grouped by L)
        const phaseTraces = {};
        phaseData.forEach(point => {
            const L = point.L;
            if (!phaseTraces[L]) {
                phaseTraces[L] = {
                    x: [],
                    y: [],
                    name: `L = ${L}`,
                    type: 'scatter',
                    mode: 'lines+markers',
                    showlegend: true
                };
            }
            phaseTraces[L].x.push(point.energy);
            phaseTraces[L].y.push(point.phase_shift * 180 / Math.PI);
        });

        const traces = [
            // Phase shifts
            ...Object.values(phaseTraces),
            // Potentials
            {
                x: potentialData.map(p => p.radius),
                y: potentialData.map(p => p.woods_saxon),
                name: 'Woods-Saxon',
                type: 'scatter',
                mode: 'lines',
                xaxis: 'x2',
                yaxis: 'y2',
                showlegend: false
            },
            {
                x: potentialData.map(p => p.radius),
                y: potentialData.map(p => p.coulomb),
                name: 'Coulomb',
                type: 'scatter',
                mode: 'lines',
                xaxis: 'x2',
                yaxis: 'y2',
                showlegend: false
            },
            // Cross-sections
            {
                x: crossSectionData.map(p => p.energy),
                y: crossSectionData.map(p => p.total_cross_section),
                name: 'Cross-Section',
                type: 'scatter',
                mode: 'lines',
                xaxis: 'x3',
                yaxis: 'y3',
                showlegend: false
            }
        ];

        const layout = {
            title: 'DWBA Comprehensive Dashboard',
            grid: {
                rows: 2,
                columns: 2,
                subplots: [
                    ['xy', 'x2y2'],
                    ['xy', 'x3y3']
                ]
            },
            xaxis: { title: 'Energy (MeV)', domain: [0, 0.45] },
            yaxis: { title: 'Phase Shift (degrees)', domain: [0.55, 1] },
            xaxis2: { title: 'Radius (fm)', domain: [0.55, 1] },
            yaxis2: { title: 'Potential (MeV)', domain: [0.55, 1] },
            xaxis3: { title: 'Energy (MeV)', domain: [0, 0.45] },
            yaxis3: { title: 'Cross-Section', type: 'log', domain: [0, 0.45] },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            margin: { t: 50, b: 30, l: 50, r: 30 }
        };

        Plotly.newPlot('dashboard-plot', traces, layout, {responsive: true});
    }

    plotElastic() {
        const data = this.currentData.elastic;
        if (!data || data.length === 0) return;

        const traces = {};
        
        // Group by energy
        data.forEach(point => {
            const E = point.energy;
            if (!traces[E]) {
                traces[E] = {
                    x: [],
                    y: [],
                    name: `E = ${E} MeV`,
                    type: 'scatter',
                    mode: 'lines+markers',
                    line: { width: 2 },
                    marker: { size: 4 }
                };
            }
            traces[E].x.push(point.angle);
            traces[E].y.push(point.differential_cross_section);
        });

        const plotData = Object.values(traces);
        const layout = {
            title: 'Elastic Scattering Differential Cross-Section',
            xaxis: { title: 'Scattering Angle (degrees)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'dσ/dΩ (fm²/sr)', type: 'log', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            legend: { x: 0.02, y: 0.98 },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('elastic-plot', plotData, layout, {responsive: true});
    }

    plotInelastic() {
        const data = this.currentData.inelastic;
        if (!data || data.length === 0) return;

        const traces = {};
        
        // Group by L
        data.forEach(point => {
            const L = point.L;
            if (!traces[L]) {
                traces[L] = {
                    x: [],
                    y: [],
                    name: `L = ${L}`,
                    type: 'scatter',
                    mode: 'lines+markers',
                    line: { width: 3 },
                    marker: { size: 6 }
                };
            }
            traces[L].x.push(point.energy);
            traces[L].y.push(point.differential_cross_section);
        });

        const plotData = Object.values(traces);
        const layout = {
            title: 'Inelastic Scattering Differential Cross-Section',
            xaxis: { title: 'Energy (MeV)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'dσ/dΩ (fm²/sr)', type: 'log', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            legend: { x: 0.02, y: 0.98 },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('inelastic-plot', plotData, layout, {responsive: true});
    }

    plotTransfer() {
        const data = this.currentData.transfer;
        if (!data || data.length === 0) return;

        const traces = {};
        
        // Group by L
        data.forEach(point => {
            const L = point.L;
            if (!traces[L]) {
                traces[L] = {
                    x: [],
                    y: [],
                    name: `L = ${L}`,
                    type: 'scatter',
                    mode: 'lines+markers',
                    line: { width: 3 },
                    marker: { size: 6 }
                };
            }
            traces[L].x.push(point.energy);
            traces[L].y.push(point.differential_cross_section);
        });

        const plotData = Object.values(traces);
        const layout = {
            title: 'Transfer Reaction Differential Cross-Section',
            xaxis: { title: 'Energy (MeV)', gridcolor: '#e0e0e0' },
            yaxis: { title: 'dσ/dΩ (fm²/sr)', type: 'log', gridcolor: '#e0e0e0' },
            plot_bgcolor: 'rgba(0,0,0,0)',
            paper_bgcolor: 'rgba(0,0,0,0)',
            font: { family: 'Arial, sans-serif' },
            legend: { x: 0.02, y: 0.98 },
            margin: { t: 50, b: 50, l: 60, r: 30 }
        };

        Plotly.newPlot('transfer-plot', plotData, layout, {responsive: true});
    }

    updateDashboardStats(calculationTime) {
        if (!this.currentData) return;

        const totalPoints = this.currentData.phase_shifts.length;
        const energies = this.currentData.phase_shifts.map(p => p.energy);
        const LValues = [...new Set(this.currentData.phase_shifts.map(p => p.L))];

        document.getElementById('total-points').textContent = totalPoints;
        document.getElementById('energy-range-display').textContent = 
            `${Math.min(...energies)}-${Math.max(...energies)}`;
        document.getElementById('L-count').textContent = LValues.length;
        document.getElementById('calculation-time').textContent = `${calculationTime}ms`;
    }

    resetParameters() {
        this.loadDefaultParameters();
        this.showStatus('Parameters reset to defaults', 'info');
    }
}

// Global functions for HTML onclick handlers
function calculateDWBA() {
    dashboard.calculateDWBA();
}

function resetParameters() {
    dashboard.resetParameters();
}

// Initialize dashboard when page loads
let dashboard;
document.addEventListener('DOMContentLoaded', () => {
    dashboard = new DWBADashboard();
});
