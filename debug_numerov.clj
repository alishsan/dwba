;; Debug script to check Numerov indexing and phase shift extraction

(require '[functions :refer :all])

;; Test with small case
(let [e 2.0
      l 1
      v0 46.23
      rad 2.0
      diff 0.5
      h 0.1
      r-max 2.0  ; Small range for debugging
      steps (int (/ r-max h))
      q (Math/sqrt (* mass-factor (+ e v0)))]
  
  (println "=== DEBUGGING NUMEROV ===")
  (println (format "Steps: %d" steps))
  (println (format "h: %.3f" h))
  (println (format "q: %.6f" q))
  (println "")
  
  ;; Check Bessel start
  (let [u0 0.0
        u1 (bessel-start-l1 h q)
        fs (mapv #(f-r-numerov % e l v0 rad diff) 
                 (take (+ steps 2) (iterate #(+ % h) h)))]
    (println "Bessel start:")
    (println (format "  u0 = %.6e" u0))
    (println (format "  u1 = %.6e" u1))
    (println (format "  fs[0] = %.6e (should be f(h))" (get fs 0)))
    (println (format "  fs[1] = %.6e (should be f(2h))" (get fs 1)))
    (println "")
    
    ;; Check what r values correspond to
    (println "r values:")
    (doseq [i (range (min 5 (count fs)))]
      (let [r (* h (inc i))]  ; r = h, 2h, 3h, ...
        (println (format "  fs[%d] = f(%.3f) = %.6e" i r (get fs i)))))
    (println "")
    
    ;; Check if indexing is correct
    (println "Indexing check:")
    (println "  In loop: n=1 uses fs[n-1]=fs[0] (should be f(h))")
    (println "  In loop: n=1 uses fs[n]=fs[1] (should be f(2h))")
    (println "  In loop: n=1 uses fs[n+1]=fs[2] (should be f(3h))")
    (println "")
    
    ;; Check naive start
    (let [u0-naive 0.0
          u1-naive (Math/pow h (inc l))]
      (println "Naive start:")
      (println (format "  u0 = %.6e" u0-naive))
      (println (format "  u1 = %.6e" u1-naive))
      (println (format "  Difference from Bessel: %.6e" (Math/abs (- u1 u1-naive))))
      (println ""))))

;; Check phase shift extraction
(println "=== CHECKING PHASE SHIFT EXTRACTION ===")
(let [e 2.0
      l 1
      v0 46.23
      rad 2.0
      diff 0.5
      h 0.1
      r-boundary 10.0
      u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)]
  
  (println (format "Wavefunction lengths: Bessel=%d, Naive=%d" 
                   (count u-bessel) (count u-naive)))
  (println (format "First 5 values Bessel: %s" (take 5 u-bessel)))
  (println (format "First 5 values Naive:  %s" (take 5 u-naive)))
  (println "")
  
  ;; Check if they're identical
  (let [identical (every? #(< (Math/abs %) 1e-10) 
                          (map - u-bessel u-naive))]
    (if identical
      (println "⚠️  WARNING: Bessel and Naive wavefunctions are IDENTICAL!")
      (println "✓ Wavefunctions are different (as expected)"))
    (println ""))
  
  ;; Check R-matrix
  (let [R-bessel (r-matrix-from-numerov u-bessel h r-boundary)
        R-naive (r-matrix-from-numerov u-naive h r-boundary)]
    (println (format "R-matrix Bessel: %.10e" R-bessel))
    (println (format "R-matrix Naive:  %.10e" R-naive))
    (println (format "Difference:      %.10e" (Math/abs (- R-bessel R-naive))))
    (println ""))
  
  ;; Check phase shift
  (let [delta-bessel (phase-shift-from-numerov u-bessel h r-boundary e l)
        delta-naive (phase-shift-from-numerov u-naive h r-boundary e l)
        k (m/sqrt (* mass-factor e))
        rho (* k r-boundary)]
    (println (format "k = %.6f fm⁻¹" k))
    (println (format "rho = k*a = %.6f" rho))
    (println (format "Phase shift Bessel: %.10f rad (%.2f deg)" 
                    delta-bessel (* delta-bessel (/ 180.0 Math/PI))))
    (println (format "Phase shift Naive:  %.10f rad (%.2f deg)" 
                    delta-naive (* delta-naive (/ 180.0 Math/PI))))
    (println "")))

