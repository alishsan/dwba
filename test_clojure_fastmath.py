#!/usr/bin/env python3

"""
Test Clojure fastmath functionality using subprocess
"""

import subprocess
import sys
import os

def test_clojure_fastmath():
    """Test if Clojure with fastmath is working"""
    
    print("=== Testing Clojure with Fastmath ===")
    
    # Test basic fastmath functionality
    test_code = """
    (require '[fastmath.core :as fm])
    (println "Testing fastmath...")
    (println "sin(1.0) = " (fm/sin 1.0))
    (println "cos(1.0) = " (fm/cos 1.0))
    (println "exp(1.0) = " (fm/exp 1.0))
    (println "Fastmath is working!")
    """
    
    try:
        # Write test code to temporary file
        with open("temp_test.clj", "w") as f:
            f.write(test_code)
        
        # Run the test
        result = subprocess.run(
            ["clojure", "-M", "temp_test.clj"],
            capture_output=True,
            text=True,
            timeout=30
        )
        
        if result.returncode == 0:
            print("✅ Fastmath is working!")
            print("Output:", result.stdout)
            return True
        else:
            print("❌ Fastmath test failed!")
            print("Error:", result.stderr)
            return False
            
    except subprocess.TimeoutExpired:
        print("⏰ Test timed out - fastmath loading is slow")
        return False
    except Exception as e:
        print(f"❌ Error running test: {e}")
        return False
    finally:
        # Clean up
        if os.path.exists("temp_test.clj"):
            os.remove("temp_test.clj")

def test_dwba_functions():
    """Test DWBA functions that use fastmath"""
    
    print("\n=== Testing DWBA Functions ===")
    
    test_code = """
    (require '[fastmath.core :as fm])
    (require '[fastmath.special :as spec])
    
    ;; Load our DWBA functions
    (load-file "src/complex.clj")
    (load-file "src/functions.clj")
    
    (println "Testing DWBA functions...")
    (println "Woods-Saxon at r=2.0: " (WS 2.0 [40.0 2.0 0.6]))
    (println "R-matrix calculation: " (r-matrix-a 10.0 [40.0 2.0 0.6] 3.0 0))
    (println "DWBA functions are working!")
    """
    
    try:
        # Write test code to temporary file
        with open("temp_dwba_test.clj", "w") as f:
            f.write(test_code)
        
        # Run the test
        result = subprocess.run(
            ["clojure", "-M", "temp_dwba_test.clj"],
            capture_output=True,
            text=True,
            timeout=60
        )
        
        if result.returncode == 0:
            print("✅ DWBA functions are working!")
            print("Output:", result.stdout)
            return True
        else:
            print("❌ DWBA functions test failed!")
            print("Error:", result.stderr)
            return False
            
    except subprocess.TimeoutExpired:
        print("⏰ Test timed out - DWBA functions loading is slow")
        return False
    except Exception as e:
        print(f"❌ Error running DWBA test: {e}")
        return False
    finally:
        # Clean up
        if os.path.exists("temp_dwba_test.clj"):
            os.remove("temp_dwba_test.clj")

if __name__ == "__main__":
    print("🧪 Testing Clojure Fastmath Integration")
    
    # Test basic fastmath
    fastmath_ok = test_clojure_fastmath()
    
    if fastmath_ok:
        # Test DWBA functions
        dwba_ok = test_dwba_functions()
        
        if dwba_ok:
            print("\n🎯 All tests passed! Ready to proceed with theoretical vs experimental comparison!")
        else:
            print("\n⚠️  Fastmath works but DWBA functions have issues")
    else:
        print("\n❌ Fastmath is not working properly")
    
    print("\n📊 Next steps:")
    print("1. If tests pass, we can proceed with theoretical calculations")
    print("2. If tests fail, we need to debug the fastmath setup")
    print("3. We can also use the web dashboard as an alternative")
