;; Copy and paste this into the REPL for parameter optimization

;; Load our DWBA functions
(load-file "src/complex.clj")
(load-file "src/functions.clj")

(println "=== DWBA Parameter Optimization ===")

;; Physical constants
(def hbarc 197.7)  ; MeV·fm
(def mp 938.272)   ; proton mass (MeV/c²)
(def mAlpha 3727.379)  ; alpha mass (MeV/c²)
(def mu (/ (* mp mAlpha) (+ mp mAlpha)))  ; ≈ 745 MeV/c²
(def mass-factor (/ (* 2 mu) hbarc hbarc))

;; Experimental data (subset for optimization)
(def exp-data-subset
  [{:energy-lab 2.0 :cross-section 0.254}
   {:energy-lab 2.5 :cross-section 0.229}
   {:energy-lab 3.0 :cross-section 0.160}])

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
(defn theoretical-sigma [E-lab ws-params]
  (let [E-cm (lab-to-cm-energy E-lab)
        theta-cm (lab-to-cm-angle 165.0)
        k (Math/sqrt (* mass-factor E-cm))
        phase-shift-val (phase-shift0 E-cm ws-params 3.0 0)
        sigma (* (/ 1.0 (* k k)) 
                 (Math/sin phase-shift-val)
                 (Math/sin phase-shift-val)
                 1e28)]
    sigma))

;; Calculate chi-squared for parameter set
(defn chi-squared [ws-params]
  (let [theoretical-values (map #(theoretical-sigma (:energy-lab %) ws-params) exp-data-subset)
        experimental-values (map :cross-section exp-data-subset)
        chi-sq (reduce + 
                (map (fn [theo exp] 
                       (let [diff (- theo exp)]
                         (* diff diff)))
                     theoretical-values 
                     experimental-values))]
    chi-sq))

;; Grid search optimization
(defn optimize-parameters []
  (println "Starting parameter optimization...")
  (let [v0-range (range 20 60 5)      ; V0: 20-55 MeV
        r0-range (range 1.5 3.0 0.2) ; R0: 1.5-2.9 fm
        a0-range (range 0.4 0.8 0.1) ; a0: 0.4-0.7 fm
        best-params (atom nil)
        best-chi-sq (atom Double/MAX_VALUE)
        test-count (atom 0)]
    
    (doseq [v0 v0-range]
      (doseq [r0 r0-range]
        (doseq [a0 a0-range]
          (let [ws-params [v0 r0 a0]
                chi-sq (chi-squared ws-params)]
            (swap! test-count inc)
            (when (< chi-sq @best-chi-sq)
              (reset! best-chi-sq chi-sq)
              (reset! best-params ws-params))
            (when (zero? (mod @test-count 10))  ; Progress indicator
              (println (format "Test %d: V0=%.1f, R0=%.2f, a0=%.2f, χ²=%.6f" 
                              @test-count v0 r0 a0 chi-sq)))))))
    
    {:best-params @best-params
     :best-chi-sq @best-chi-sq
     :total-tests @test-count}))

;; Run optimization
(println "=== Parameter Optimization Results ===")
(def optimization-result (optimize-parameters))

(println "\nBest parameters found:")
(println "V0 (MeV):" (first (:best-params optimization-result)))
(println "R0 (fm):" (second (:best-params optimization-result)))
(println "a0 (fm):" (nth (:best-params optimization-result) 2))
(println "Chi-squared:" (:best-chi-sq optimization-result))
(println "Total tests:" (:total-tests optimization-result))

;; Test optimized parameters
(println "\n=== Results with Optimized Parameters ===")
(println "Energy (Lab)\tTheoretical\tExperimental\tRatio")
(doseq [data-point exp-data-subset]
  (let [E-lab (:energy-lab data-point)
        theoretical (theoretical-sigma E-lab (:best-params optimization-result))
        experimental (:cross-section data-point)
        ratio (/ theoretical experimental)]
    (printf "%.1f\t\t%.6f\t\t%.3f\t\t%.3f\n" 
            E-lab theoretical experimental ratio)))

;; Test with different parameter sets for comparison
(println "\n=== Parameter Set Comparison ===")
(def param-sets
  [[40.0 2.0 0.6 "Standard"]
   [45.0 2.2 0.5 "Optimized"]
   [35.0 1.8 0.7 "Alternative"]
   [50.0 2.5 0.4 "High V0"]
   [30.0 1.5 0.8 "Low V0"]])

(doseq [[v0 r0 a0 name] param-sets]
  (let [ws-params [v0 r0 a0]
        chi-sq (chi-squared ws-params)
        ratios (map #(/ (theoretical-sigma (:energy-lab %) ws-params) (:cross-section %)) exp-data-subset)
        mean-ratio (/ (reduce + ratios) (count ratios))]
    (println (format "%s (V0=%.1f, R0=%.1f, a0=%.1f): χ²=%.6f, Mean ratio=%.3f" 
                    name v0 r0 a0 chi-sq mean-ratio))))

(println "\n✅ Parameter optimization completed!")
(println "🎯 Use the best parameters for better agreement!")
