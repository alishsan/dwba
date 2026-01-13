(require '[functions :refer :all]
         '[fastmath.core :as m]
         '[clojure.java.io :as io])

;; Helper function to calculate z0 from physical parameters
(defn calculate-z0 [V0 R0]
  "Calculate dimensionless well depth parameter z0 from physical parameters.
   
   Parameters:
   - V0: Well depth in MeV
   - R0: Well radius in fm
   
   Returns: z0 = sqrt(2m·V0·R0²/ℏ²)
   
   Uses the mass factor from functions.clj: mass-factor = 2μ/(ℏc)²
   For a square well, z0 = sqrt(mass-factor · V0 · R0²)"
  (let [mass-factor (/ (* 2 869.4) (* 197.7 197.7))  ; 2μ/(ℏc)² for 14C+n
        z0 (m/sqrt (* mass-factor V0 (* R0 R0)))]
    z0))

(println "=== Generating Finite Square Well Bound State Reference Table ===")
(println "This table can be used to cross-check numerical calculations")
(println "For Woods-Saxon wells, use small diffuseness (a0 << R0) to approximate square well\n")

;; Test parameters: z0 values and l values
(def z0-values [2.0 3.0 4.0 5.0 6.0 7.0 8.0 10.0 12.0 15.0])
(def l-values [0 1 2])

;; Format a number for table output
(defn format-val [x precision]
  (if (Double/isNaN x)
    "NaN"
    (format (str "%." precision "f") x)))

;; Generate table for a specific l
(defn generate-table-for-l [l z0-values]
  (let [results (for [z0 z0-values]
                   (let [bound-states (find-all-bound-states l z0)
                         valid-states (filter :converged? bound-states)]
                     {:z0 z0
                      :states valid-states
                      :count (count valid-states)}))
        max-states (apply max (map :count results))]
    
    (println (format "\n=== l = %d ===" l))
    (println "")
    (println "| z0  | N | State | e-ratio | |E|/V0 | xi (ka) | eta (κa) | Matching Error |")
    (println "|-----|---|-------|---------|-------|---------|----------|----------------|")
    
    (doseq [result results]
      (let [z0 (:z0 result)
            states (:states result)
            count (:count result)]
        (if (zero? count)
          (println (format "| %.1f | 0 | -     | -       | -     | -       | -        | -              |" z0))
          (doseq [[idx state] (map-indexed vector states)]
            (let [e-ratio (:e-ratio state)
                  xi (:xi state)
                  eta (:eta state)
                  error (:matching-error state)]
              (if (zero? idx)
                (println (format "| %.1f | %d | %d     | %s | %s | %s | %s | %s |"
                                z0 count (inc idx)
                                (format-val e-ratio 6)
                                (format-val e-ratio 6)
                                (format-val xi 6)
                                (format-val eta 6)
                                (format "%.2e" error)))
                (println (format "|     |   | %d     | %s | %s | %s | %s | %s |"
                                (inc idx)
                                (format-val e-ratio 6)
                                (format-val e-ratio 6)
                                (format-val xi 6)
                                (format-val eta 6)
                                (format "%.2e" error)))))))))
    
    (println "")
    (println "**Notes:**")
    (println "- N = number of bound states found")
    (println "- e-ratio = |E|/V0 (dimensionless energy, 0 < e-ratio < 1)")
    (println "- xi = k·a where k = sqrt(2m(E+V0))/ℏ (inside well)")
    (println "- eta = κ·a where κ = sqrt(2m|E|)/ℏ (outside well)")
    (println "- Matching error = f(xi, eta, l) (should be ≈ 0 for true bound states)")
    (println "")))

;; Generate comprehensive table
(println "Generating reference table...")
(println "")

(doseq [l l-values]
  (generate-table-for-l l z0-values))

;; Also generate a summary table
(println "\n=== Summary: Number of Bound States ===")
(println "")
(println "| z0  | l=0 | l=1 | l=2 |")
(println "|-----|-----|-----|-----|")

(doseq [z0 z0-values]
  (let [counts (map (fn [l]
                      (count (filter :converged? (find-all-bound-states l z0))))
                    l-values)]
    (println (format "| %.1f | %3d | %3d | %3d |" z0 (nth counts 0) (nth counts 1) (nth counts 2)))))

(println "")
(println "=== Usage for Woods-Saxon Cross-Check ===")
(println "")
(println "To cross-check Woods-Saxon calculations:")
(println "1. Use small diffuseness: a0 << R0 (e.g., a0 = 0.01 fm, R0 = 2.0 fm)")
(println "2. Calculate z0 = sqrt(2m·V0·R0²/ℏ²) for your parameters")
(println "   For 14C+n system: z0 = sqrt(mass-factor · V0 · R0²)")
(println "   where mass-factor = 2μ/(ℏc)² ≈ 0.0445 (MeV·fm²)⁻¹")
(println "3. Find the corresponding row in the table above")
(println "4. Compare your calculated bound state energy with the e-ratio from the table")
(println "")
(println "Example:")
(println "  For V0 = 50 MeV, R0 = 2.0 fm, a0 = 0.01 fm (14C+n system):")
(println (format "  z0 = sqrt(0.0445 · 50 · 2²) ≈ %.2f" (calculate-z0 50.0 2.0)))
(println "  Look up z0 ≈ 3.0 in the table to find expected bound states")
(println "")
(println "Quick Reference: z0 for Common Parameters (14C+n system):")
(println "")
(println "| V0 (MeV) | R0 (fm) | z0 |")
(println "|----------|---------|-----|")
(doseq [[V0 R0] [[30.0 1.5] [30.0 2.0] [30.0 2.5]
                [50.0 1.5] [50.0 2.0] [50.0 2.5]
                [70.0 1.5] [70.0 2.0] [70.0 2.5]]]
  (let [z0 (calculate-z0 V0 R0)]
    (println (format "| %6.1f | %5.1f | %.2f |" V0 R0 z0))))
(println "")

;; Save to file
(let [output-file "FINITE_WELL_REFERENCE_TABLE.md"
      content (with-out-str
                (println "# Finite Square Well Bound State Reference Table")
                (println "")
                (println "This table provides reference values for bound state energies in a finite square well.")
                (println "It can be used to cross-check numerical calculations, including Woods-Saxon wells")
                (println "with small diffuseness (a0 << R0).")
                (println "")
                (println "## Parameters")
                (println "")
                (println "- **z0**: Dimensionless well depth parameter = sqrt(2m·V0·a²/ℏ²)")
                (println "- **l**: Orbital angular momentum quantum number")
                (println "- **e-ratio**: Dimensionless energy = |E|/V0")
                (println "- **xi**: Dimensionless wave number inside well = k·a")
                (println "- **eta**: Dimensionless decay parameter outside well = κ·a")
                (println "")
                (println "## Reference Values")
                (println "")
                (doseq [l l-values]
                  (generate-table-for-l l z0-values))
                (println "\n## Summary: Number of Bound States")
                (println "")
                (println "| z0  | l=0 | l=1 | l=2 |")
                (println "|-----|-----|-----|-----|")
                (doseq [z0 z0-values]
                  (let [counts (map (fn [l]
                                      (count (filter :converged? (find-all-bound-states l z0))))
                                    l-values)]
                    (println (format "| %.1f | %3d | %3d | %3d |" z0 (nth counts 0) (nth counts 1) (nth counts 2)))))
                (println "")
                (println "## Usage for Woods-Saxon Cross-Check")
                (println "")
                (println "To cross-check Woods-Saxon calculations:")
                (println "1. Use small diffuseness: a0 << R0 (e.g., a0 = 0.01 fm, R0 = 2.0 fm)")
                (println "2. Calculate z0 = sqrt(2m·V0·R0²/ℏ²) for your parameters")
                (println "   - For 14C+n system: z0 = sqrt(mass-factor · V0 · R0²)")
                (println "   - mass-factor = 2μ/(ℏc)² ≈ 0.0445 (MeV·fm²)⁻¹ for 14C+n")
                (println "3. Find the corresponding row in the table above")
                (println "4. Compare your calculated bound state energy with the e-ratio from the table")
                (println "")
                (println "### Example")
                (println "")
                (println "For V0 = 50 MeV, R0 = 2.0 fm, a0 = 0.01 fm (14C+n system):")
                (println "- z0 = sqrt(0.0445 · 50 · 2²) ≈ sqrt(8.9) ≈ 2.98 ≈ 3.0")
                (println "- Look up z0 = 3.0 in the table to find expected bound states")
                (println "")
                (println "### Conversion from e-ratio to Physical Energy")
                (println "")
                (println "The e-ratio in the table is |E|/V0, so:")
                (println "- Physical bound state energy: E = -V0 · e-ratio")
                (println "- Example: For e-ratio = 0.5 and V0 = 50 MeV, E = -25 MeV")
                (println "")
                (println "### Quick Reference: z0 for Common Parameters (14C+n system)")
                (println "")
                (println "| V0 (MeV) | R0 (fm) | z0 |")
                (println "|----------|---------|-----|")
                (doseq [[V0 R0] [[30.0 1.5] [30.0 2.0] [30.0 2.5]
                                [50.0 1.5] [50.0 2.0] [50.0 2.5]
                                [70.0 1.5] [70.0 2.0] [70.0 2.5]]]
                  (let [z0 (calculate-z0 V0 R0)]
                    (println (format "| %6.1f | %5.1f | %.2f |" V0 R0 z0))))
                (println "")
                (println "## Notes")
                (println "")
                (println "- All energies are in units of the well depth V0")
                (println "- Matching errors should be < 1e-6 for converged states")
                (println "- States are ordered by energy (lowest first)")
                (println "- For higher l, fewer bound states exist due to centrifugal barrier")
                (println ""))]
  (spit output-file content)
  (println (format "Reference table saved to %s" output-file)))

(println "")
(println "=== Generation Complete ===")

