// DWBA Web Dashboard JavaScript
class DWBADashboard {
    constructor() {
        // Always send API requests to the dashboard server (port 3000) unless we're already on it
        const dashboardUrl = 'http://localhost:3000';
        const apiFromQuery = new URLSearchParams(window.location.search).get('api');
        const onDashboard = window.location.origin === 'http://localhost:3000';
        this.apiBase = apiFromQuery || (onDashboard ? '' : dashboardUrl);
        this.currentData = null;
        this.initializeEventListeners();
        this.loadDefaultParameters();
        this.checkApiHealth();
        if (!onDashboard && !apiFromQuery) {
            this.showApiNotice(dashboardUrl);
        }
    }

    showApiNotice(dashboardUrl) {
        const statusDiv = document.getElementById('status-messages');
        if (!statusDiv) return;
        statusDiv.innerHTML = '<div class="alert alert-info mb-0" role="alert"><strong>Calculations use the dashboard server.</strong> Start it with: <code>cd web-dashboard && lein run</code>. Then <a href="' + dashboardUrl + '" class="alert-link">open this app from ' + dashboardUrl + '</a> so the page and API come from the same server.</div>';
    }

    async checkApiHealth() {
        try {
            const r = await fetch(`${this.apiBase}/api/health`);
            if (!r.ok) throw new Error('API not OK');
        } catch (e) {
            const statusDiv = document.getElementById('status-messages');
            if (statusDiv) {
                statusDiv.innerHTML = '<div class="error"><i class="fas fa-exclamation-triangle"></i> Dashboard API not reachable. Start the server: <code>cd web-dashboard && lein run</code>, then <a href="http://localhost:3000">open http://localhost:3000</a> in your browser.</div>';
            }
        }
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

        // Reset button
        document.getElementById('reset-btn')?.addEventListener('click', () => {
            this.resetParameters();
        });

        // Calculate buttons (one per tab) – bound in JS so no globals needed
        const calcButtons = [
            ['calculate-phase-btn', 'calculatePhase'],
            ['calculate-rmatrix-btn', 'calculateRMatrix'],
            ['calculate-potential-btn', 'calculatePotentials'],
            ['calculate-cross-section-btn', 'calculateCrossSections'],
            ['calculate-elastic-btn', 'calculateElastic'],
            ['calculate-inelastic-btn', 'calculateInelastic'],
            ['calculate-transfer-btn', 'calculateTransfer'],
            ['calculate-dashboard-btn', 'calculateDashboard']
        ];
        calcButtons.forEach(([id, method]) => {
            const btn = document.getElementById(id);
            if (btn) btn.addEventListener('click', () => this[method]());
        });
    }

    _setButtonLoading(btnId, loading) {
        const btn = document.getElementById(btnId);
        if (!btn) return;
        btn.disabled = loading;
        btn.innerHTML = loading ? '<i class="fas fa-spinner fa-spin"></i> Calculating...' : '<i class="fas fa-calculator"></i> Calculate';
    }

    async _post(url, params) {
        const fullUrl = `${this.apiBase}${url}`;
        const response = await fetch(fullUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(params)
        });
        const text = await response.text();
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${text.slice(0, 200)}`);
        }
        let result;
        try {
            result = JSON.parse(text);
        } catch (e) {
            throw new Error('Invalid JSON response: ' + text.slice(0, 200));
        }
        return result;
    }

    /** Normalize API response so we always have underscore keys (phase_shifts, r_matrices, etc.) */
    _normalizeData(data) {
        if (!data) return data;
        return {
            phase_shifts: data.phase_shifts || data.phaseShifts || data['phase-shifts'],
            r_matrices: data.r_matrices || data.rMatrices || data['r-matrices'],
            potentials: data.potentials,
            cross_sections: data.cross_sections || data.crossSections || data['cross-sections'],
            parameters: data.parameters,
            elastic: data.elastic,
            inelastic: data.inelastic,
            transfer: data.transfer
        };
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
        
        // Auto-hide after 8 seconds (longer so user sees success/error)
        setTimeout(() => {
            statusDiv.innerHTML = '';
        }, 8000);
    }

    async _runCoreCalculation(btnId) {
        const params = this.getParameters();
        if (params.energies.length === 0 || params.L_values.length === 0) {
            this.showStatus('Please provide valid energy range and angular momenta', 'error');
            return;
        }
        this._setButtonLoading(btnId, true);
        this.showStatus('Calculating...', 'info');
        const startTime = Date.now();
        try {
            const result = await this._post('/api/calculate', params);
            if (!result.success) throw new Error(result.error || 'Calculation failed');
            this.currentData = this._normalizeData(result.data);
            if (!this.currentData) throw new Error('No data in response');
            const n = (this.currentData.phase_shifts || []).length;
            console.log('Core calculation OK, data keys:', Object.keys(this.currentData), 'phase_shifts count:', n);
            if (n === 0) {
                this.showStatus('Calculation returned no data points. Check energy range and L-values.', 'error');
                return;
            }
            try {
                this.updateAllPlots();
                this.updateDashboardStats(Date.now() - startTime);
                this.showStatus(`Done in ${Date.now() - startTime}ms — ${n} points plotted.`, 'success');
            } catch (plotError) {
                console.error('Plot error:', plotError);
                this.showStatus(`Data received but plot failed: ${plotError.message}`, 'error');
            }
        } catch (error) {
            console.error('Calculation error:', error);
            let msg = error.message;
            if (msg.includes('404')) {
                msg = 'API not found. Start the server: cd web-dashboard && lein run — then open http://localhost:3000 in your browser (click the link above).';
            }
            this.showStatus(`Error: ${msg}`, 'error');
        } finally {
            this._setButtonLoading(btnId, false);
        }
    }

    async _runTabCalculation(btnId, path, dataKey) {
        const params = this.getParameters();
        if (params.energies.length === 0 || params.L_values.length === 0) {
            this.showStatus('Please provide valid energy range and angular momenta', 'error');
            return;
        }
        this._setButtonLoading(btnId, true);
        this.showStatus('Calculating...', 'info');
        const startTime = Date.now();
        try {
            const result = await this._post(path, params);
            if (!result.success) throw new Error(result.error || 'Calculation failed');
            this.currentData = this.currentData || {};
            const raw = result.data && (result.data[dataKey] || result.data[dataKey.replace(/_/g, '-')]);
            if (raw) this.currentData[dataKey] = raw;
            this.updateAllPlots();
            if (dataKey === 'elastic' || dataKey === 'inelastic' || dataKey === 'transfer') {
                this.showStatus(`Done in ${Date.now() - startTime}ms`, 'success');
            } else {
                this.updateDashboardStats(Date.now() - startTime);
                this.showStatus(`Done in ${Date.now() - startTime}ms`, 'success');
            }
        } catch (error) {
            console.error('Calculation error:', error);
            let msg = error.message;
            if (msg.includes('404')) {
                msg = 'API not found. Start the server: cd web-dashboard && lein run — then open http://localhost:3000 in your browser (click the link above).';
            }
            this.showStatus(`Error: ${msg}`, 'error');
        } finally {
            this._setButtonLoading(btnId, false);
        }
    }

    async calculatePhase() { await this._runCoreCalculation('calculate-phase-btn'); }
    async calculateRMatrix() { await this._runCoreCalculation('calculate-rmatrix-btn'); }
    async calculatePotentials() { await this._runCoreCalculation('calculate-potential-btn'); }
    async calculateCrossSections() { await this._runCoreCalculation('calculate-cross-section-btn'); }
    async calculateDashboard() { await this._runCoreCalculation('calculate-dashboard-btn'); }
    async calculateElastic() { await this._runTabCalculation('calculate-elastic-btn', '/api/elastic', 'elastic'); }
    async calculateInelastic() { await this._runTabCalculation('calculate-inelastic-btn', '/api/inelastic', 'inelastic'); }
    async calculateTransfer() { await this._runTabCalculation('calculate-transfer-btn', '/api/transfer', 'transfer'); }

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
        const data = this.currentData.phase_shifts || this.currentData['phase-shifts'] || this.currentData.phaseShifts;
        if (!data || !Array.isArray(data) || data.length === 0) {
            console.warn('plotPhaseShifts: no data', { hasData: !!data, length: data?.length });
            return;
        }
        const el = document.getElementById('phase-plot');
        if (!el) {
            console.error('plotPhaseShifts: div #phase-plot not found');
            return;
        }
        const traces = {};
        data.forEach(point => {
            const L = point.L;
            const phaseShift = point.phase_shift != null ? point.phase_shift : point.phaseShift;
            const energy = point.energy;
            if (L === undefined || (phaseShift === undefined && phaseShift !== 0)) return;
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
            traces[L].x.push(Number(energy));
            traces[L].y.push((Number(phaseShift) || 0) * 180 / Math.PI);
        });

        const plotData = Object.values(traces);
        if (plotData.length === 0) {
            console.warn('plotPhaseShifts: no traces after processing');
            return;
        }
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

        try {
            if (typeof Plotly === 'undefined') throw new Error('Plotly not loaded');
            Plotly.newPlot('phase-plot', plotData, layout, {responsive: true});
        } catch (err) {
            console.error('Plotly.newPlot failed:', err);
        }
    }

    plotRMatrices() {
        const data = this.currentData.r_matrices || this.currentData['r-matrices'];
        if (!data || !Array.isArray(data) || data.length === 0) return;
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
        if (!data || !Array.isArray(data) || data.length === 0) return;

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
        const data = this.currentData.cross_sections || this.currentData['cross-sections'];
        if (!data || !Array.isArray(data) || data.length === 0) return;

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
        const phaseData = this.currentData.phase_shifts || this.currentData['phase-shifts'];
        const potentialData = this.currentData.potentials;
        const crossSectionData = this.currentData.cross_sections || this.currentData['cross-sections'];
        if (!phaseData?.length || !potentialData?.length || !crossSectionData?.length) return;

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
        const phaseShifts = this.currentData.phase_shifts || this.currentData['phase-shifts'];
        if (!phaseShifts?.length) return;

        const totalPoints = phaseShifts.length;
        const energies = phaseShifts.map(p => p.energy);
        const LValues = [...new Set(phaseShifts.map(p => p.L))];

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

let dashboard;
document.addEventListener('DOMContentLoaded', () => {
    dashboard = new DWBADashboard();
});
