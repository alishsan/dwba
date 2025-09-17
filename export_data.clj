;; Export data from Clojure calculations for Python/matplotlib visualization
(load-file "src/complex.clj")
(use 'complex)

;; Physical constants
(def hbarc 197.7)
(def mu 745)
(def mass-factor (/ (* 2 mu) hbarc hbarc))
(def Z1Z2ee (* 2 1.44))

;; Potentials
(defn WS [r [V0 R0 a0]]
  (/ (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0)))))

(defn Coulomb-pot [r r0]
  (if (> r r0) (/ Z1Z2ee r) (* r (/ Z1Z2ee r0 r0))))

;; R-matrix calculations
(defn r-matrix-nuclear-only [E V a L]
  (let [dr 0.001
        N (int (/ a dr))]
    (loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr n 0]
      (if (>= n N)
        (/ ur dudr)
        (let [new-pot (WS x V)
              new-d2udr2 (* (+ (/ (* L (inc L)) (* x x)) (* mass-factor (- new-pot E))) ur)
              new-dudr (+ dudr (* new-d2udr2 dr))
              new-ur (+ ur (* new-dudr dr))]
          (recur (+ x dr) new-pot new-d2udr2 new-dudr new-ur (inc n)))))))

(defn r-matrix-coulomb-nuclear [E V a L]
  (let [dr 0.001
        N (int (/ a dr))
        R0 (second V)]
    (loop [x dr pot 0 d2udr2 (/ 1. dr) dudr 1 ur dr n 0]
      (if (>= n N)
        (/ ur dudr a)
        (let [new-pot (+ (Coulomb-pot x R0) (WS x V))
              new-d2udr2 (* (+ (/ (* L (inc L)) (* x x)) (* mass-factor (- new-pot E))) ur)
              new-dudr (+ dudr (* new-d2udr2 dr))
              new-ur (+ ur (* new-dudr dr))]
          (recur (+ x dr) new-pot new-d2udr2 new-dudr new-ur (inc n)))))))

(defn nuclear-phase-shift [E V a L]
  (let [R-coulomb-nuclear (r-matrix-coulomb-nuclear E V a L)
        R-coulomb-only (r-matrix-nuclear-only E [0 2.0 0.6] a L) ; Pure Coulomb
        R-nuclear (- R-coulomb-nuclear R-coulomb-only)
        phase-shift (Math/atan (/ R-nuclear 1.0))]
    phase-shift))

;; Generate comprehensive dataset
(defn generate-dataset []
  (let [ws-params [40.0 2.0 0.6]
        energies (range 1 51 1)
        L-values (range 0 6)
        radii (range 0.5 10.0 0.1)]
    
    ;; Phase shift data
    (def phase-shift-data
      (for [E energies
            L L-values]
        {:energy E :L L :phase-shift (nuclear-phase-shift E ws-params 3.0 L)}))
    
    ;; R-matrix data
    (def r-matrix-data
      (for [E energies
            L L-values]
        {:energy E :L L 
         :r-nuclear (r-matrix-nuclear-only E ws-params 3.0 L)
         :r-coulomb-nuclear (r-matrix-coulomb-nuclear E ws-params 3.0 L)}))
    
    ;; Potential data
    (def potential-data
      (for [r radii]
        {:radius r 
         :woods-saxon (WS r ws-params)
         :coulomb (Coulomb-pot r 2.0)
         :combined (+ (WS r ws-params) (Coulomb-pot r 2.0))}))
    
    ;; Cross-section data (simplified)
    (def cross-section-data
      (for [E energies]
        {:energy E 
         :total-cross-section (reduce + (map #(Math/pow (Math/sin (nuclear-phase-shift E ws-params 3.0 %)) 2) L-values))}))
    
    ;; Export to JSON
    (spit "data/phase_shifts.json" 
          (str "{\n  \"phase_shifts\": [\n"
               (clojure.string/join ",\n" 
                 (map #(str "    {\"energy\": " (:energy %) ", \"L\": " (:L %) ", \"phase_shift\": " (:phase-shift %) "}")
                      phase-shift-data))
               "\n  ]\n}"))
    
    (spit "data/r_matrices.json"
          (str "{\n  \"r_matrices\": [\n"
               (clojure.string/join ",\n"
                 (map #(str "    {\"energy\": " (:energy %) ", \"L\": " (:L %) ", \"r_nuclear\": " (:r-nuclear %) ", \"r_coulomb_nuclear\": " (:r-coulomb-nuclear %) "}")
                      r-matrix-data))
               "\n  ]\n}"))
    
    (spit "data/potentials.json"
          (str "{\n  \"potentials\": [\n"
               (clojure.string/join ",\n"
                 (map #(str "    {\"radius\": " (:radius %) ", \"woods_saxon\": " (:woods-saxon %) ", \"coulomb\": " (:coulomb %) ", \"combined\": " (:combined %) "}")
                      potential-data))
               "\n  ]\n}"))
    
    (spit "data/cross_sections.json"
          (str "{\n  \"cross_sections\": [\n"
               (clojure.string/join ",\n"
                 (map #(str "    {\"energy\": " (:energy %) ", \"total_cross_section\": " (:total-cross-section %) "}")
                      cross-section-data))
               "\n  ]\n}"))
    
    (println "Data exported to data/ directory")
    (println "Files created:")
    (println "  - phase_shifts.json")
    (println "  - r_matrices.json") 
    (println "  - potentials.json")
    (println "  - cross_sections.json")
    
    {:phase-shifts phase-shift-data
     :r-matrices r-matrix-data
     :potentials potential-data
     :cross-sections cross-section-data}))

;; Create data directory and export
(.mkdir (java.io.File. "data"))
(generate-dataset)

(println "\n=== Data Export Complete ===")
(println "Ready for Python/matplotlib visualization!")
