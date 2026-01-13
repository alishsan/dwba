(require '[dwba.transfer :as t]
         '[functions :refer :all]
         '[fastmath.core :as m]
         '[clojure.java.io :as io])

(println "=== Generating Woods-Saxon vs Finite Square Well Comparison Table ===")
(println "This table compares bound state energies for small diffuseness (a0 << R0)\n")

;; Helper function to calculate z0
(defn calculate-z0 [V0 R0]
  (let [mass-factor (/ (* 2 869.4) (* 197.7 197.7))
        z0 (m/sqrt (* mass-factor V0 (* R0 R0)))]
    z0))

;; Test parameters
(def z0-values [3.0 4.0 5.0 6.0 7.0 8.0 10.0])
(def l-values [0 1 2])
(def a0-small 0.01)  ; Very small diffuseness to approximate square well
(def R0-fixed 2.0)    ; Fixed R0 for consistency

;; Calculate V0 from z0
(defn calculate-V0 [z0 R0]
  (let [mass-factor (/ (* 2 869.4) (* 197.7 197.7))
        V0 (/ (* z0 z0) (* mass-factor (* R0 R0)))]
    V0))

(defn find-ws-bound-states [V0 R0 a0 l max-n]
  "Find bound states using Woods-Saxon solver."
  (for [n (range 1 (inc max-n))
        :let [result (try
                       (t/solve-bound-state [V0 R0 a0] n l nil 20.0 0.01)
                       (catch Exception e nil))]
        :when result]
    {:n n
     :energy (:energy result)
     :e-ratio (/ (m/abs (:energy result)) V0)
     :nodes (:nodes result)
     :converged? (:converged? result)}))

(defn generate-comparison-table []
  (let [output (java.io.StringWriter.)]
    (binding [*out* output]
      (println "# Woods-Saxon vs Finite Square Well Comparison")
      (println "")
      (println "This table compares bound state energies for Woods-Saxon potential with")
      (println "very small diffuseness (a0 = 0.01 fm) against finite square well results.")
      (println "")
      (println "## Parameters")
      (println "")
      (println "- **R0**: Fixed at 2.0 fm")
      (println "- **a0**: 0.01 fm (very small to approximate square well)")
      (println "- **V0**: Calculated from z0 = sqrt(mass-factor · V0 · R0²)")
      (println "- **z0**: Dimensionless well depth parameter")
      (println "")
      (println "## Comparison Tables")
      (println "")
      
      (doseq [l l-values]
        (println (format "### l = %d" l))
        (println "")
        (println "| z0 | V0 (MeV) | Method | State | e-ratio | |E| (MeV) | Difference |")
        (println "|----|----------|--------|-------|---------|----------|------------|")
        
        (doseq [z0 z0-values]
          (let [V0 (calculate-V0 z0 R0-fixed)
                V-params [V0 R0-fixed a0-small]
                ;; Get finite square well reference
                ref-states (filter :converged? (find-all-bound-states l z0))
                ;; Get Woods-Saxon results
                ws-states (find-ws-bound-states V0 R0-fixed a0-small l (max 3 (count ref-states)))
                max-states (max (count ref-states) (count ws-states))]
            
            (if (zero? max-states)
              (println (format "| %.1f | %.1f | - | - | - | - | - |" z0 V0))
              (let [paired-states (map vector
                                      (take max-states ref-states)
                                      (take max-states ws-states))]
                (doseq [[idx [ref-state ws-state]] (map-indexed vector paired-states)]
                  (let [ref-e-ratio (if ref-state (:e-ratio ref-state) nil)
                        ws-e-ratio (if ws-state (:e-ratio ws-state) nil)
                        ref-energy (if ref-e-ratio (* V0 ref-e-ratio) nil)
                        ws-energy (if ws-e-ratio (* V0 ws-e-ratio) nil)
                        diff (if (and ref-e-ratio ws-e-ratio)
                               (m/abs (- ref-e-ratio ws-e-ratio))
                               nil)
                        rel-diff (if (and diff ref-e-ratio (> ref-e-ratio 0))
                                  (* 100.0 (/ diff ref-e-ratio))
                                  nil)]
                    (if (zero? idx)
                      (println (format "| %.1f | %.1f | Finite | %d | %s | %s | - |"
                                      z0 V0 (inc idx)
                                      (if ref-e-ratio (format "%.6f" ref-e-ratio) "-")
                                      (if ref-energy (format "%.3f" ref-energy) "-")))
                      (println (format "|     |        | Finite | %d | %s | %s | - |"
                                      (inc idx)
                                      (if ref-e-ratio (format "%.6f" ref-e-ratio) "-")
                                      (if ref-energy (format "%.3f" ref-energy) "-"))))
                    (println (format "|     |        | WS     | %d | %s | %s | %s |"
                                    (inc idx)
                                    (if ws-e-ratio (format "%.6f" ws-e-ratio) "-")
                                    (if ws-energy (format "%.3f" ws-energy) "-")
                                    (if rel-diff (format "%.2f%%" rel-diff) "-")))))))
            
            (println ""))))
      
      (println "## Notes")
      (println "")
      (println "- **e-ratio**: Dimensionless energy = |E|/V0")
      (println "- **Difference**: Relative difference between WS and finite square well e-ratios")
      (println "- For good agreement, differences should be < 5%")
      (println "- Larger differences may indicate:")
      (println "  • Finite diffuseness effects (even a0=0.01 fm is not exactly zero)")
      (println "  • Numerical precision limitations")
      (println "  • Boundary condition differences")
      (println "")))
    (.toString output)))

;; Generate and save table
(let [table-content (generate-comparison-table)
      output-file "WS_FINITE_WELL_COMPARISON.md"]
  (spit output-file table-content)
  (println table-content)
  (println (format "\nComparison table saved to %s" output-file)))

