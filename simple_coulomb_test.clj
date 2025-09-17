(ns simple-coulomb-test
  (:require [complex :as c]
            [functions :as f]))

(defn run-coulomb-tests []

;; Test basic Coulomb potential
(println "=== Testing Coulomb Potential ===")
(println "Radius\tCoulomb Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [coulomb-pot (f/Coulomb-pot r 2.0)]
    (printf "%.1f\t%.6f\n" (double r) (double coulomb-pot))))

;; Test Woods-Saxon potential
(println "\n=== Testing Woods-Saxon Potential ===")
(println "Radius\tWoods-Saxon Potential")
(def ws-params [40.0 2.0 0.6])
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [ws-pot (f/WS r ws-params)]
    (printf "%.1f\t%.6f\n" (double r) (double ws-pot))))

;; Test combined potential
(println "\n=== Testing Combined Potential (Coulomb + Woods-Saxon) ===")
(println "Radius\tCombined Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [combined-pot (+ (f/Coulomb-pot r 2.0) (f/WS r ws-params))]
    (printf "%.1f\t%.6f\n" (double r) (double combined-pot))))

;; Test R-matrix calculations
(println "\n=== Testing R-Matrix Calculations ===")
(println "Energy\tL\tR-matrix (no Coulomb)\tR-matrix (with Coulomb)")
(def test-energies [5 10 15 20 25])
(def test-L [0 1 2])
(doseq [E test-energies
        L test-L]
  (let [r-matrix-no-coulomb (f/r-matrix-a E ws-params 3.0 L)
        r-matrix-with-coulomb (f/r-matrix E ws-params L)]
    (printf "%.1f\t%d\t%.6f\t\t%.6f\n" 
            (double E) L 
            (double r-matrix-no-coulomb) 
            (double r-matrix-with-coulomb))))

;; Test phase shifts
(println "\n=== Testing Phase Shifts ===")
(println "Energy\tL\tPhase (no Coulomb)\tPhase (with Coulomb)\tDifference")
(doseq [E test-energies
        L test-L]
  (let [phase-no-coulomb (f/phase-shift0 E ws-params 3.0 L)
        phase-with-coulomb (f/phase-shift E ws-params L)
        difference (- (c/re phase-with-coulomb) (c/re phase-no-coulomb))]
    (printf "%.1f\t%d\t%.6f\t\t%.6f\t\t%.6f\n" 
            (double E) L 
            (double (c/re phase-no-coulomb)) 
            (double (c/re phase-with-coulomb))
            (double difference))))

;; Test S-matrix
(println "\n=== Testing S-Matrix ===")
(println "Energy\tL\tS-matrix (no Coulomb)\tS-matrix (with Coulomb)")
(doseq [E test-energies
        L test-L]
  (let [s-matrix-no-coulomb (f/s-matrix0 E ws-params 3.0 L)
        s-matrix-with-coulomb (f/s-matrix E ws-params L)]
    (printf "%.1f\t%d\t%.6f\t\t%.6f\n" 
            (double E) L 
            (double (c/mag s-matrix-no-coulomb)) 
            (double (c/mag s-matrix-with-coulomb)))))

;; Test cross-sections
(println "\n=== Testing Cross-Sections ===")
(println "Energy\tL\tCross-section (no Coulomb)\tCross-section (with Coulomb)")
(doseq [E test-energies
        L test-L]
  (let [sigma-no-coulomb (f/sigma-L0 E ws-params 3.0 L)
        sigma-with-coulomb (let [s (f/s-matrix E ws-params L)]
                             (* (/ 2 E) Math/PI (+ (* 2 L) 1) 
                                (Math/pow (c/mag (c/subt2 1. s)) 2)))]
    (printf "%.1f\t%d\t%.6f\t\t\t%.6f\n" 
            (double E) L 
            (double sigma-no-coulomb) 
            (double sigma-with-coulomb))))

(println "\n=== Coulomb Scattering Test Complete ==="))

;; Run the tests
(run-coulomb-tests)
