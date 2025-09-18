#!/bin/bash

echo "🚀 Starting DWBA Web Dashboard..."
echo "=================================="

# Check if we're in the right directory
if [ ! -f "web-dashboard/project.clj" ]; then
    echo "❌ Error: Please run this script from the dwba root directory"
    exit 1
fi

# Navigate to web-dashboard directory
cd web-dashboard

# Check if Leiningen is installed
if ! command -v lein &> /dev/null; then
    echo "❌ Error: Leiningen is not installed"
    echo "Please install Leiningen: https://leiningen.org/"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed"
    echo "Please install Java 8 or higher"
    exit 1
fi

echo "✅ Prerequisites check passed"
echo "📦 Installing dependencies..."

# Install dependencies
lein deps

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to install dependencies"
    exit 1
fi

echo "✅ Dependencies installed"
echo "🌐 Starting web server on port 3000..."
echo ""
echo "Dashboard will be available at: http://localhost:3000"
echo "Press Ctrl+C to stop the server"
echo ""

# Start the server
lein run
