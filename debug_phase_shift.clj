;; Debug phase shift calculation and compare naive vs Bessel

(require '[functions :refer :all])

(println "=== DEBUGGING PHASE SHIFT CALCULATION ===\n")

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)
(def r-boundary 10.0)

;; Test 1: Compare wavefunctions
(println "1. COMPARING WAVEFUNCTIONS")
(println "===========================")
(let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)]
  
  (println (format "Bessel: %d points, last 3: %s" 
                   (count u-bessel) (take-last 3 u-bessel)))
  (println (format "Naive:  %d points, last 3: %s" 
                   (count u-naive) (take-last 3 u-naive)))
  
  ;; Check if they're different
  (let [differences (map #(Math/abs (- %1 %2)) u-bessel u-naive)
        max-diff (apply max differences)
        first-5-diff (take 5 differences)]
    (println (format "Max difference: %.6e" max-diff))
    (println (format "First 5 differences: %s" first-5-diff))
    (if (> max-diff 1e-10)
      (println "✓ Wavefunctions are DIFFERENT (as expected)")
      (println "⚠️  WARNING: Wavefunctions are IDENTICAL!")))
  (println ""))

;; Test 2: Check R-matrices
(println "2. COMPARING R-MATRICES")
(println "========================")
(let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
      R-bessel (r-matrix-from-numerov u-bessel h r-boundary)
      R-naive (r-matrix-from-numerov u-naive h r-boundary)]
  
  (println (format "R-matrix Bessel: %.10e" R-bessel))
  (println (format "R-matrix Naive:  %.10e" R-naive))
  (println (format "Difference:      %.10e" (Math/abs (- R-bessel R-naive))))
  (println ""))

;; Test 3: Check phase shifts
(println "3. COMPARING PHASE SHIFTS")
(println "==========================")
(let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
      delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
      delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)
      k (m/sqrt (* mass-factor e))
      rho (* k r-boundary)]
  
  (println (format "k = %.6f fm⁻¹" k))
  (println (format "rho = k*r = %.6f" rho))
  (println (format "Phase shift Bessel: %.10f rad (%.2f deg)" 
                  delta-bessel (* delta-bessel (/ 180.0 Math/PI))))
  (println (format "Phase shift Naive:  %.10f rad (%.2f deg)" 
                  delta-naive (* delta-naive (/ 180.0 Math/PI))))
  (println (format "Difference:         %.10e rad" (Math/abs (- delta-bessel delta-naive))))
  (println "")
  
  ;; Check if phase shift is reasonable
  (let [abs-delta (Math/abs delta-bessel)]
    (if (< abs-delta Math/PI)
      (println "✓ Phase shift magnitude is reasonable")
      (println (format "⚠️  Phase shift is large: %.2f rad (%.1f deg)" 
                      abs-delta (* abs-delta (/ 180.0 Math/PI))))))
  (println ""))

;; Test 4: Check exact phase shift with different methods
(println "4. CHECKING EXACT PHASE SHIFT")
(println "==============================")
(let [h-fine 0.001
      u-fine (solve-numerov e l v0 rad diff h-fine r-boundary)
      delta-fine (phase-shift-from-numerov u-fine h-fine r-boundary e l)
      
      ;; Also try using R-matrix method for comparison
      V [v0 rad diff]
      delta-rmatrix (phase-shift0 e V r-boundary l)]
  
  (println (format "Fine Numerov (h=%.4f): %.10f rad" h-fine delta-fine))
  (println (format "R-matrix method:       %.10f rad" delta-rmatrix))
  (println (format "Difference:            %.10e rad" (Math/abs (- delta-fine delta-rmatrix))))
  (println ""))

;; Test 5: Check at different boundary radii
(println "5. CHECKING BOUNDARY RADIUS DEPENDENCE")
(println "======================================")
(doseq [r-bound [5.0 8.0 10.0 12.0 15.0]]
  (let [u-bessel (solve-numerov e l v0 rad diff h r-bound)
        delta (phase-shift-from-numerov u-bessel h r-bound e l)]
    (println (format "r = %.1f fm: delta = %.10f rad (%.2f deg)" 
                    r-bound delta (* delta (/ 180.0 Math/PI))))))

