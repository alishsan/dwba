#!/usr/bin/env clojure

;; Simple DWBA Web Dashboard - Standalone version
;; Run with: clojure simple-dashboard.clj

;; Physical constants
(def hbarc 197.7)
(def mu 745)
(def mass-factor (/ (* 2 mu) hbarc hbarc))
(def Z1Z2ee (* 2 1.44))

;; Simple DWBA functions
(defn WS [r [V0 R0 a0]]
  (/ (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0)))))

(defn Coulomb-pot [r r0]
  (if (> r r0) (/ Z1Z2ee r) (* r (/ Z1Z2ee r0 r0))))

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
        R-coulomb-only (r-matrix-nuclear-only E [0 2.0 0.6] a L)
        R-nuclear (- R-coulomb-nuclear R-coulomb-only)
        phase-shift (Math/atan (/ R-nuclear 1.0))]
    phase-shift))

;; Generate sample data
(defn generate-sample-data []
  (let [energies (range 5 31 5)
        L-values [0 1 2 3 4 5]
        ws-params [40.0 2.0 0.6]
        radius 3.0]
    
    ;; Phase shifts
    (def phase-shift-data
      (for [E energies
            L L-values]
        {:energy E 
         :L L 
         :phase_shift (nuclear-phase-shift E ws-params radius L)}))
    
    ;; R-matrices
    (def r-matrix-data
      (for [E energies
            L L-values]
        {:energy E 
         :L L 
         :r_nuclear (r-matrix-nuclear-only E ws-params radius L)
         :r_coulomb_nuclear (r-matrix-coulomb-nuclear E ws-params radius L)}))
    
    ;; Potentials
    (def potential-data
      (let [radii (range 0.1 10.0 0.1)]
        (for [r radii]
          {:radius r 
           :woods_saxon (WS r ws-params)
           :coulomb (Coulomb-pot r (second ws-params))
           :combined (+ (WS r ws-params) (Coulomb-pot r (second ws-params)))})))
    
    ;; Cross-sections
    (def cross-section-data
      (for [E energies]
        {:energy E 
         :total_cross_section (reduce + (map #(Math/pow (Math/sin (nuclear-phase-shift E ws-params radius %)) 2) L-values))}))
    
    {:phase_shifts phase-shift-data
     :r_matrices r-matrix-data
     :potentials potential-data
     :cross_sections cross-section-data}))

;; Generate and save data
(println "Generating DWBA sample data...")
(def sample-data (generate-sample-data))

;; Create data directory
(.mkdir (java.io.File. "data"))

;; Save to JSON files (simple format)
(spit "data/phase_shifts.json" 
      (str "{\n  \"phase_shifts\": [\n"
           (clojure.string/join ",\n" 
             (map #(str "    {\"energy\": " (:energy %) ", \"L\": " (:L %) ", \"phase_shift\": " (:phase_shift %) "}")
                  (:phase_shifts sample-data)))
           "\n  ]\n}"))

(spit "data/r_matrices.json"
      (str "{\n  \"r_matrices\": [\n"
           (clojure.string/join ",\n"
             (map #(str "    {\"energy\": " (:energy %) ", \"L\": " (:L %) ", \"r_nuclear\": " (:r_nuclear %) ", \"r_coulomb_nuclear\": " (:r_coulomb_nuclear %) "}")
                  (:r_matrices sample-data)))
           "\n  ]\n}"))

(spit "data/potentials.json"
      (str "{\n  \"potentials\": [\n"
           (clojure.string/join ",\n"
             (map #(str "    {\"radius\": " (:radius %) ", \"woods_saxon\": " (:woods_saxon %) ", \"coulomb\": " (:coulomb %) ", \"combined\": " (:combined %) "}")
                  (:potentials sample-data)))
           "\n  ]\n}"))

(spit "data/cross_sections.json"
      (str "{\n  \"cross_sections\": [\n"
           (clojure.string/join ",\n"
             (map #(str "    {\"energy\": " (:energy %) ", \"total_cross_section\": " (:total_cross_section %) "}")
                  (:cross_sections sample-data)))
           "\n  ]\n}"))

(println "✅ Sample data generated successfully!")
(println "📊 Files created:")
(println "  - data/phase_shifts.json")
(println "  - data/r_matrices.json")
(println "  - data/potentials.json")
(println "  - data/cross_sections.json")
(println "")
(println "🌐 Now you can:")
(println "1. Run: python3 visualize_dwba.py")
(println "2. Open: test-dashboard.html")
(println "3. View the generated plots in plots/ directory")