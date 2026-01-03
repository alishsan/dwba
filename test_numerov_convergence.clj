;; Test Numerov convergence analysis
;; Compare fine-grid solution with test solutions

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

(println "=== NUMEROV CONVERGENCE TEST ===")
(println "Parameters from numerical_riccati.tex:")
(println "  E = 2.0 MeV, l = 1")
(println "  Woods-Saxon: V₀ = 46.23 MeV, R = 2.0 fm, a = 0.5 fm")
(println "")

;; Test with different step sizes
(let [h-fine 0.001
      h-values [0.1 0.05 0.01]
      results (map (fn [h-test]
                     (let [result (numerov-convergence-test 
                                    (:e test-params) (:l test-params)
                                    (:v0 test-params) (:rad test-params) 
                                    (:diff test-params) h-fine h-test (:r-max test-params))]
                       {:h h-test
                        :max-error (:max-error result)
                        :mean-error (:mean-error result)
                        :points-compared (:downsampled-count result)}))
                   h-values)]
  
  (println "Convergence Results:")
  (println (format "%-8s %-15s %-15s %-15s" "h (fm)" "Max Error" "Mean Error" "Points"))
  (println (apply str (repeat 60 "-")))
  (doseq [r results]
    (println (format "%-8.3f %-15.6e %-15.6e %-15d" 
                    (:h r) (:max-error r) (:mean-error r) (:points-compared r))))
  (println ""))

;; Direct test as provided by user
(println "=== DIRECT ERROR CALCULATION ===")
(let [h-fine 0.001
      h-test 0.01
      u-true (solve-numerov 2.0 1 46.23 2.0 0.5 h-fine 10.0)
      u-test (solve-numerov 2.0 1 46.23 2.0 0.5 h-test 10.0)
      downsample-factor (int (/ h-test h-fine))
      u-true-downsampled (take-nth downsample-factor u-true)
      min-len (min (count u-true-downsampled) (count u-test))
      errors (calculate-error (take min-len u-true-downsampled) 
                              (take min-len u-test))
      max-error (apply max errors)]
  (println "Max absolute error:" max-error)
  (println "Mean absolute error:" (/ (reduce + errors) (count errors)))
  (println "Number of points compared:" min-len)
  (println "Fine grid points:" (count u-true))
  (println "Test grid points:" (count u-test)))

