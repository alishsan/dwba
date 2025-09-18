#!/usr/bin/env clojure

;; Generate datasets for scientific paper
;; Export theoretical and experimental data for analysis and plotting

(require '[fastmath.special :as spec])
(require '[fastmath.complex :as cplx])
(require '[clojure.data.json :as json])

;; Load our DWBA functions
(load-file "src/complex.clj")
(load-file "src/functions.clj")

(println "=== Generating Paper Datasets ===")

;; Physical constants
(def hbarc 197.7)  ; MeV·fm
(def mp 938.272)   ; proton mass (MeV/c²)
(def mAlpha 3727.379)  ; alpha mass (MeV/c²)
(def mu (/ (* mp mAlpha) (+ mp mAlpha)))  ; ≈ 745 MeV/c²
(def mass-factor (/ (* 2 mu) hbarc hbarc))

;; Kinematic conversion functions
(defn lab-to-cm-energy [E-lab]
  (* E-lab (/ mAlpha (+ mp mAlpha))))

(defn lab-to-cm-angle [theta-lab]
  (let [theta-lab-rad (* theta-lab (/ Math/PI 180))
        cos-theta-cm (Math/cos theta-lab-rad)
        sin-theta-cm (Math/sin theta-lab-rad)
        ratio (/ mp mAlpha)
        numerator (+ cos-theta-cm (* ratio sin-theta-cm))
        denominator (Math/sqrt (+ 1 (* 2 ratio cos-theta-cm) (* ratio ratio)))]
    (if (< (Math/abs numerator) denominator)
      (Math/acos (/ numerator denominator))
      (Math/acos (Math/signum numerator)))))

;; Theoretical cross-section calculation
(defn calculate-theoretical-cross-section [E-lab ws-params]
  (let [E-cm (lab-to-cm-energy E-lab)
        theta-cm (lab-to-cm-angle 165.0)
        k (Math/sqrt (* mass-factor E-cm))
        r-matrix-val (r-matrix-a E-cm ws-params 3.0 0)
        s-matrix-val (s-matrix0 E-cm ws-params 3.0 0)
        phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)
        sigma (* (/ 1.0 (* k k)) 
                 (Math/sin phase-shift-val)
                 (Math/sin phase-shift-val)
                 1e28)]
    {:energy-lab E-lab
     :energy-cm E-cm
     :angle-lab 165.0
     :angle-cm (* theta-cm (/ 180 Math/PI))
     :k k
     :r-matrix r-matrix-val
     :s-matrix s-matrix-val
     :phase-shift phase-shift-val
     :cross-section sigma}))

;; Experimental data
(def experimental-data
  [{:energy-lab 1.6 :cross-section 0.193 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 1.7 :cross-section 0.196 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 1.8 :cross-section 0.211 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 1.9 :cross-section 0.237 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.0 :cross-section 0.254 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.1 :cross-section 0.263 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.2 :cross-section 0.271 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.3 :cross-section 0.269 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.4 :cross-section 0.251 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.5 :cross-section 0.229 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.6 :cross-section 0.213 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.7 :cross-section 0.193 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.8 :cross-section 0.174 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 2.9 :cross-section 0.170 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 3.0 :cross-section 0.160 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 3.2 :cross-section 0.136 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 3.4 :cross-section 0.127 :unit "b/sr" :source "EXFOR D0557"}
   {:energy-lab 3.6 :cross-section 0.117 :unit "b/sr" :source "EXFOR D0557"}])

;; Generate theoretical data with different parameter sets
(def parameter-sets
  {:standard [40.0 2.0 0.6]  ; V0, R0, a0
   :optimized [45.0 2.2 0.5]  ; Example optimized parameters
   :alternative [35.0 1.8 0.7]})  ; Alternative parameters

(println "Generating theoretical data with different parameter sets...")

(def theoretical-datasets
  (into {}
        (for [[name params] parameter-sets]
          [name (map #(calculate-theoretical-cross-section (:energy-lab %) params) 
                     experimental-data)])))

;; Create comparison dataset
(def comparison-data
  (map (fn [exp-point]
         (let [energy (:energy-lab exp-point)
               theoretical-standard (first (filter #(= (:energy-lab %) energy) 
                                                   (theoretical-datasets :standard)))
               theoretical-optimized (first (filter #(= (:energy-lab %) energy) 
                                                    (theoretical-datasets :optimized)))
               theoretical-alternative (first (filter #(= (:energy-lab %) energy) 
                                                      (theoretical-datasets :alternative)))]
           {:energy-lab energy
            :experimental (:cross-section exp-point)
            :theoretical-standard (:cross-section theoretical-standard)
            :theoretical-optimized (:cross-section theoretical-optimized)
            :theoretical-alternative (:cross-section theoretical-alternative)
            :ratio-standard (/ (:cross-section theoretical-standard) (:cross-section exp-point))
            :ratio-optimized (/ (:cross-section theoretical-optimized) (:cross-section exp-point))
            :ratio-alternative (/ (:cross-section theoretical-alternative) (:cross-section exp-point))}))
       experimental-data))

;; Export data to JSON files
(println "Exporting data to JSON files...")

;; Export experimental data
(spit "paper_data/experimental_data.json"
      (json/write-str {:reaction "p + α → p + α"
                       :angle-lab 165.0
                       :data experimental-data}
                      :pretty true))

;; Export theoretical data
(spit "paper_data/theoretical_data.json"
      (json/write-str {:reaction "p + α → p + α"
                       :angle-lab 165.0
                       :parameter-sets parameter-sets
                       :datasets theoretical-datasets}
                      :pretty true))

;; Export comparison data
(spit "paper_data/comparison_data.json"
      (json/write-str {:reaction "p + α → p + α"
                       :angle-lab 165.0
                       :comparison comparison-data}
                      :pretty true))

;; Export physical constants
(spit "paper_data/physical_constants.json"
      (json/write-str {:hbarc hbarc
                       :proton-mass mp
                       :alpha-mass mAlpha
                       :reduced-mass mu
                       :mass-factor mass-factor}
                      :pretty true))

;; Generate summary statistics
(def summary-stats
  (let [ratios-standard (map :ratio-standard comparison-data)
        ratios-optimized (map :ratio-optimized comparison-data)
        ratios-alternative (map :ratio-alternative comparison-data)]
    {:standard-parameters
     {:mean-ratio (/ (reduce + ratios-standard) (count ratios-standard))
      :std-deviation (Math/sqrt (/ (reduce + (map #(Math/pow (- % (/ (reduce + ratios-standard) (count ratios-standard))) 2) ratios-standard)) (count ratios-standard)))
      :min-ratio (apply min ratios-standard)
      :max-ratio (apply max ratios-standard)}
     :optimized-parameters
     {:mean-ratio (/ (reduce + ratios-optimized) (count ratios-optimized))
      :std-deviation (Math/sqrt (/ (reduce + (map #(Math/pow (- % (/ (reduce + ratios-optimized) (count ratios-optimized))) 2) ratios-optimized)) (count ratios-optimized)))
      :min-ratio (apply min ratios-optimized)
      :max-ratio (apply max ratios-optimized)}
     :alternative-parameters
     {:mean-ratio (/ (reduce + ratios-alternative) (count ratios-alternative))
      :std-deviation (Math/sqrt (/ (reduce + (map #(Math/pow (- % (/ (reduce + ratios-alternative) (count ratios-alternative))) 2) ratios-alternative)) (count ratios-alternative)))
      :min-ratio (apply min ratios-alternative)
      :max-ratio (apply max ratios-alternative)}}))

(spit "paper_data/summary_statistics.json"
      (json/write-str summary-stats :pretty true))

(println "\n=== Generated Files ===")
(println "📁 paper_data/experimental_data.json - Experimental cross-sections")
(println "📁 paper_data/theoretical_data.json - Theoretical cross-sections")
(println "📁 paper_data/comparison_data.json - Side-by-side comparison")
(println "📁 paper_data/physical_constants.json - Physical constants used")
(println "📁 paper_data/summary_statistics.json - Statistical analysis")

(println "\n=== Summary Statistics ===")
(println "Standard parameters (V0=40, R0=2.0, a0=0.6):")
(println "  Mean ratio (T/E):" (format "%.3f" (get-in summary-stats [:standard-parameters :mean-ratio])))
(println "  Std deviation:" (format "%.3f" (get-in summary-stats [:standard-parameters :std-deviation])))

(println "\n✅ Paper datasets generated successfully!")
(println "🎯 Ready for scientific paper writing and publication!")
