;; Test Numerov stability: Bessel start vs Naive start
;; Compare Wronskian drift for both initialization methods

(require '[functions :refer :all])

;; Test case from numerical_riccati.tex paper
(def test-params
  {:e 2.0      ; MeV
   :l 1
   :v0 46.23   ; MeV
   :rad 2.0    ; fm
   :diff 0.5   ; fm
   :r-max 10.0 ; fm
   })

(println "=== NUMEROV STABILITY ANALYSIS ===")
(println "Parameters from numerical_riccati.tex:")
(println "  E = 2.0 MeV, l = 1")
(println "  Woods-Saxon: V₀ = 46.23 MeV, R = 2.0 fm, a = 0.5 fm")
(println "")

;; Test with different step sizes
(let [h-values [0.1 0.05 0.01]
      results (map (fn [h]
                      (let [stats (calculate-stability-data 
                                     (:e test-params) (:l test-params)
                                     (:v0 test-params) (:rad test-params) 
                                     (:diff test-params) h (:r-max test-params))]
                        {:h h
                         :bessel-drift (:bessel-w-drift stats)
                         :naive-drift (:naive-w-drift stats)
                         :bessel-initial (:bessel-w-initial stats)
                         :naive-initial (:naive-w-initial stats)}))
                   h-values)]
  
  (println "Wronskian Stability Comparison:")
  (println (format "%-8s %-20s %-20s" "h (fm)" "Bessel Drift" "Naive Drift"))
  (println (apply str (repeat 60 "-")))
  (doseq [r results]
    (println (format "%-8.3f %-20.6e %-20.6e" 
                    (:h r) (:bessel-drift r) (:naive-drift r))))
  (println ""))

;; Detailed analysis for h = 0.01
(println "=== DETAILED ANALYSIS (h = 0.01) ===")
(let [stats (calculate-stability-data 2.0 1 46.23 2.0 0.5 0.01 10.0)]
  (println "Bessel Start:")
  (println "  Initial Wronskian:" (:bessel-w-initial stats))
  (println "  Max Wronskian Drift:" (:bessel-w-drift stats))
  (println "")
  (println "Naive Start:")
  (println "  Initial Wronskian:" (:naive-w-initial stats))
  (println "  Max Wronskian Drift:" (:naive-w-drift stats))
  (println "")
  (println "Improvement Factor:" 
           (if (> (:naive-w-drift stats) 0)
             (/ (:naive-w-drift stats) (:bessel-w-drift stats))
             "N/A (Bessel drift is zero)")))

