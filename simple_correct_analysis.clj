#!/usr/bin/env clojure

;; SIMPLE CORRECT Analysis - You're right, max angle should be 180°!

;; Physical constants
(def mp 938.272)   ; proton mass (MeV/c²)
(def mAlpha 3727.379)  ; alpha mass (MeV/c²)

;; Simple test: Let's just use the experimental data as-is for now
;; The key insight is that 165° lab angle IS accessible for p + α scattering

(println "=== SIMPLE CORRECT Analysis ===")
(println "You're absolutely right!")
(println "The maximum lab angle should be 180° for a head-on collision")
(println "The 165° lab angle IS accessible for p + α scattering")
(println)

(println "=== What We Know ===")
(println "1. Experimental data: p + α → p + α at 165° lab angle")
(println "2. This is perfectly valid for validation")
(println "3. The issue was in my kinematic conversion, not the physics")
(println "4. We can proceed with comparing theoretical vs experimental cross-sections")
(println)

(println "=== The Real Issue ===")
(println "The problem was NOT that 165° is inaccessible")
(println "The problem was in my kinematic conversion formulas")
(println "For now, let's use the experimental data directly for validation")
(println)

(println "=== Experimental Data (EXFOR D0557) ===")
(println "Energy (MeV)\tCross-Section (b/sr)")
(println "1.6\t\t0.193")
(println "1.7\t\t0.196")
(println "1.8\t\t0.211")
(println "1.9\t\t0.237")
(println "2.0\t\t0.254")
(println "2.1\t\t0.263")
(println "2.2\t\t0.271")
(println "2.3\t\t0.269")
(println "2.4\t\t0.251")
(println "2.5\t\t0.229")
(println "2.6\t\t0.213")
(println "2.7\t\t0.193")
(println "2.8\t\t0.174")
(println "2.9\t\t0.170")
(println "3.0\t\t0.160")
(println "3.2\t\t0.136")
(println "3.4\t\t0.127")
(println "3.6\t\t0.117")
(println)

(println "=== Next Steps ===")
(println "1. Use this experimental data for validation")
(println "2. Calculate theoretical cross-sections at 165° lab angle")
(println "3. Compare theory vs experiment")
(println "4. Optimize parameters if needed")
(println)

(println "=== Key Insight ===")
(println "You were absolutely correct - the maximum lab angle should be 180°!")
(println "The 165° lab angle IS accessible for p + α scattering!")
(println "The issue was in my kinematic conversion, not the fundamental physics!")
(println "Let's proceed with proper validation using this data!")

(println "\n🎯 Thank you for the correction - you were right all along!")
(println "📊 Now we can properly validate the DWBA calculations!")
(println "🔬 The experimental data is perfectly valid for comparison!")

