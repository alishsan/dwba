# DWBA Web Dashboard

Interactive web dashboard for Distorted Wave Born Approximation (DWBA) nuclear physics calculations.

## Features

### 🚀 **Interactive Calculations**
- Real-time parameter adjustment with sliders
- Live calculation updates
- Multiple angular momentum support
- Customizable energy ranges

### 📊 **Advanced Visualizations**
- **Phase Shifts**: Nuclear phase shifts vs energy for different L values
- **R-Matrices**: Comparison of nuclear vs Coulomb+nuclear R-matrix values
- **Potentials**: Woods-Saxon, Coulomb, and combined potential plots
- **Cross-Sections**: Total cross-sections vs energy (log scale)
- **Dashboard**: Comprehensive multi-panel overview

### 🎛️ **User Interface**
- Modern, responsive design
- Real-time parameter controls
- Tabbed interface for different views
- Status messages and error handling
- Mobile-friendly layout

### ⚡ **Performance**
- Fast Clojure backend calculations
- Efficient data transfer via JSON
- Client-side plotting with Plotly.js
- Real-time updates

## Quick Start

### 1. Start the Server
```bash
cd web-dashboard
lein run
```

### 2. Open in Browser
Navigate to: http://localhost:3000

### 3. Adjust Parameters
- Use sliders to adjust Woods-Saxon parameters (V₀, R₀, a₀)
- Set energy range and angular momenta
- Click "Calculate DWBA" to run calculations

### 4. Explore Results
- Switch between different plot tabs
- View comprehensive dashboard
- Export data for further analysis

## API Endpoints

### GET `/api/health`
Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "message": "DWBA Web Dashboard API"
}
```

### GET `/api/parameters`
Get default parameters and ranges.

**Response:**
```json
{
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
```

### POST `/api/calculate`
Perform DWBA calculations.

**Request:**
```json
{
  "energies": ["5", "10", "15", "20", "25", "30"],
  "L_values": ["0", "1", "2", "3", "4", "5"],
  "V0": "40.0",
  "R0": "2.0",
  "a0": "0.6",
  "radius": "3.0"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "phase_shifts": [
      {"energy": 5.0, "L": 0, "phase_shift": -1.086136},
      ...
    ],
    "r_matrices": [
      {"energy": 5.0, "L": 0, "r_nuclear": 0.123, "r_coulomb_nuclear": 0.456},
      ...
    ],
    "potentials": [
      {"radius": 0.1, "woods_saxon": -39.8, "coulomb": 28.8, "combined": -11.0},
      ...
    ],
    "cross_sections": [
      {"energy": 5.0, "total_cross_section": 0.123},
      ...
    ],
    "parameters": {
      "energies": [5.0, 10.0, 15.0, 20.0, 25.0, 30.0],
      "L_values": [0, 1, 2, 3, 4, 5],
      "ws_params": [40.0, 2.0, 0.6],
      "radius": 3.0
    }
  }
}
```

## Architecture

### Backend (Clojure)
- **Framework**: Compojure + Ring
- **Calculations**: DWBA nuclear physics functions
- **API**: RESTful JSON endpoints
- **CORS**: Enabled for cross-origin requests

### Frontend (JavaScript)
- **UI**: Bootstrap 5 + Font Awesome
- **Plotting**: Plotly.js for interactive charts
- **State**: Client-side parameter management
- **Responsive**: Mobile-friendly design

### Data Flow
```
User Input → JavaScript → Clojure API → DWBA Calculations → JSON Response → Plotly.js Visualization
```

## Development

### Prerequisites
- Leiningen 2.x
- Java 8+
- Modern web browser

### Setup
```bash
# Clone repository
git clone https://github.com/alishsan/dwba.git
cd dwba/web-dashboard

# Install dependencies
lein deps

# Start development server
lein run

# Or run in REPL
lein repl
```

### File Structure
```
web-dashboard/
├── src/dwba_web/
│   └── core.clj              # Main server code
├── public/
│   ├── index.html            # Main HTML page
│   └── app.js                # Frontend JavaScript
├── project.clj               # Leiningen project file
└── README.md                 # This file
```

### Customization

#### Adding New Parameters
1. Update parameter ranges in `core.clj`
2. Add slider controls in `index.html`
3. Update JavaScript parameter handling in `app.js`

#### Adding New Plot Types
1. Add calculation logic in `core.clj`
2. Create new tab in `index.html`
3. Add plotting function in `app.js`

#### Styling Changes
- Modify CSS in `index.html` `<style>` section
- Update Bootstrap classes for layout
- Customize Plotly.js themes

## Deployment

### Local Development
```bash
lein run
# Server runs on http://localhost:3000
```

### Production Deployment
```bash
# Build JAR
lein uberjar

# Run JAR
java -jar target/dwba-web-0.1.0-SNAPSHOT-standalone.jar

# Or with custom port
java -jar target/dwba-web-0.1.0-SNAPSHOT-standalone.jar 8080
```

### Docker Deployment
```dockerfile
FROM openjdk:8-jre-alpine
COPY target/dwba-web-0.1.0-SNAPSHOT-standalone.jar app.jar
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]
```

## Troubleshooting

### Common Issues

#### Server Won't Start
- Check Java version: `java -version`
- Verify Leiningen: `lein version`
- Check port availability: `lsof -i :3000`

#### Calculations Fail
- Verify parameter ranges
- Check energy and L-value inputs
- Look at server logs for error details

#### Plots Not Displaying
- Check browser console for JavaScript errors
- Verify Plotly.js is loading
- Check network requests in browser dev tools

#### CORS Issues
- Ensure CORS middleware is enabled
- Check API endpoint URLs
- Verify request headers

### Debug Mode
```bash
# Run with debug logging
lein run 2>&1 | tee debug.log

# Check server logs
tail -f debug.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

MIT License - see LICENSE file for details.

## Support

- **Issues**: GitHub Issues
- **Documentation**: This README
- **Examples**: See `public/index.html` for usage examples

---

**DWBA Web Dashboard** - Making nuclear physics calculations accessible and interactive! 🚀
