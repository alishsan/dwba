;; Basic test of core functions
(load-file "src/complex.clj")
(load-file "src/functions.clj")

;; Test basic Coulomb potential
(println "=== Testing Coulomb Potential ===")
(println "Radius\tCoulomb Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [coulomb-pot (Coulomb-pot r 2.0)]
    (printf "%.1f\t%.6f\n" (double r) (double coulomb-pot))))

;; Test Woods-Saxon potential
(println "\n=== Testing Woods-Saxon Potential ===")
(println "Radius\tWoods-Saxon Potential")
(def ws-params [40.0 2.0 0.6])
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [ws-pot (WS r ws-params)]
    (printf "%.1f\t%.6f\n" (double r) (double ws-pot))))

;; Test combined potential
(println "\n=== Testing Combined Potential (Coulomb + Woods-Saxon) ===")
(println "Radius\tCombined Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [combined-pot (+ (Coulomb-pot r 2.0) (WS r ws-params))]
    (printf "%.1f\t%.6f\n" (double r) (double combined-pot))))

;; Test R-matrix calculations (no Coulomb)
(println "\n=== Testing R-Matrix Calculations (No Coulomb) ===")
(println "Energy\tL\tR-matrix")
(def test-energies [5 10 15 20 25])
(def test-L [0 1 2])
(doseq [E test-energies
        L test-L]
  (let [r-matrix-val (r-matrix-a E ws-params 3.0 L)]
    (printf "%.1f\t%d\t%.6f\n" 
            (double E) L 
            (double r-matrix-val))))

;; Test phase shifts (no Coulomb)
(println "\n=== Testing Phase Shifts (No Coulomb) ===")
(println "Energy\tL\tPhase Shift")
(doseq [E test-energies
        L test-L]
  (let [phase-val (phase-shift0 E ws-params 3.0 L)]
    (printf "%.1f\t%d\t%.6f\n" 
            (double E) L 
            (double (re phase-val)))))

(println "\n=== Basic Test Complete ===")
