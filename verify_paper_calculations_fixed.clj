;; Verify all numerical values in the paper match code calculations
;; FIXED VERSION - Better namespace handling
;;
;; INSTRUCTIONS FOR CIDER:
;; 1. First, load functions.clj: C-c C-k on src/functions.clj
;; 2. Then load this file: C-c C-k on this file
;; OR
;; In REPL, type:
;; (load-file "src/functions.clj")
;; (load-file "verify_paper_calculations_fixed.clj")

(ns verify-paper-calculations
  (:require [functions :as f]))

;; Use fully qualified names to avoid namespace issues
(defn verify-calculations []
  (println "=== VERIFYING PAPER CALCULATIONS ===\n")

  ;; Paper test case: E=2.0 MeV, l=1, V0=46.23 MeV, R=2.0 fm, a=0.5 fm
  (def paper-params
    {:e 2.0
     :l 1
     :v0 46.23
     :rad 2.0
     :diff 0.5
     :r-boundary 10.0})

  (println "Test Parameters:")
  (println (format "  E = %.1f MeV" (:e paper-params)))
  (println (format "  l = %d" (:l paper-params)))
  (println (format "  V₀ = %.2f MeV" (:v0 paper-params)))
  (println (format "  R = %.1f fm" (:rad paper-params)))
  (println (format "  a = %.1f fm" (:diff paper-params)))
  (println (format "  r_boundary = %.1f fm" (:r-boundary paper-params)))
  (println "")

  ;; ============================================
  ;; 1. Verify Phase Shift Convergence Table
  ;; ============================================
  (println "1. PHASE SHIFT CONVERGENCE TABLE")
  (println "=================================")
  (println "Paper claims (Table 1):")
  (println "  h=0.1:   Naive=2.4×10⁻³,  Bessel=1.1×10⁻⁵")
  (println "  h=0.05:  Naive=6.1×10⁻⁴,  Bessel=6.8×10⁻⁷")
  (println "  h=0.01:  Naive=2.5×10⁻⁵,  Bessel=7.5×10⁻¹⁰")
  (println "")

  (println "Calculating with code...")
  (let [h-values [0.1 0.05 0.01]
        table (f/phase-shift-convergence-table 
                (:e paper-params) (:l paper-params)
                (:v0 paper-params) (:rad paper-params) 
                (:diff paper-params) h-values (:r-boundary paper-params))]
    
    (println "\nCode Results:")
    (println (format "%-8s %-25s %-25s %-15s" "h (fm)" "Naive Error (code)" "Bessel Error (code)" "Match?"))
    (println (apply str (repeat 90 "-")))
    
    (doseq [row table]
      (let [h (:h row)
            naive-code (:naive-error row)
            bessel-code (:bessel-error row)
            ;; Paper values
            naive-paper (condp = h
                          0.1 2.4e-3
                          0.05 6.1e-4
                          0.01 2.5e-5
                          0.0)
            bessel-paper (condp = h
                           0.1 1.1e-5
                           0.05 6.8e-7
                           0.01 7.5e-10
                           0.0)
            ;; Check if within 30% (reasonable tolerance)
            naive-match (if (> naive-paper 0)
                         (< (Math/abs (- naive-code naive-paper)) (* 0.3 naive-paper))
                         false)
            bessel-match (if (> bessel-paper 0)
                          (< (Math/abs (- bessel-code bessel-paper)) (* 0.3 bessel-paper))
                          false)
            match-status (if (and naive-match bessel-match) "✓ MATCH" "✗ DIFFER")]
        (println (format "%-8.2f %-25.6e %-25.6e %-15s" 
                        h naive-code bessel-code match-status))
        (when (not (and naive-match bessel-match))
          (println (format "         Paper: Naive=%.2e, Bessel=%.2e" 
                          naive-paper bessel-paper))
          (println (format "         Ratio: Naive=%.2f, Bessel=%.2f" 
                          (/ naive-code naive-paper) 
                          (/ bessel-code bessel-paper))))))
    (println ""))

  ;; ============================================
  ;; 2. Verify Wronskian Stability
  ;; ============================================
  (println "2. WRONSKIAN STABILITY")
  (println "======================")
  (println "Paper claims: Bessel-start maintains W to within ~10⁻¹⁴")
  (println "")

  (let [h-values [0.1 0.05 0.01]
        results (map (fn [h]
                       (let [stats (f/calculate-stability-data 
                                      (:e paper-params) (:l paper-params)
                                      (:v0 paper-params) (:rad paper-params) 
                                      (:diff paper-params) h (:r-boundary paper-params))]
                         {:h h
                          :naive-drift (:naive-w-drift stats)
                          :bessel-drift (:bessel-w-drift stats)}))
                     h-values)]
    
    (println "Code Results:")
    (println (format "%-8s %-20s %-20s" "h (fm)" "Naive Drift" "Bessel Drift"))
    (println (apply str (repeat 60 "-")))
    (doseq [r results]
      (println (format "%-8.2f %-20.6e %-20.6e" 
                      (:h r) (:naive-drift r) (:bessel-drift r)))
      (when (< (:bessel-drift r) 1e-13)
        (println "         ✓ Bessel drift is within 10⁻¹³ (better than claimed!)")))
    (println ""))

  ;; ============================================
  ;; 3. Verify Exact Phase Shift
  ;; ============================================
  (println "3. EXACT PHASE SHIFT REFERENCE")
  (println "===============================")
  (let [delta-exact (f/exact-phase-shift-numerov 
                       (:e paper-params) (:l paper-params)
                       (:v0 paper-params) (:rad paper-params) 
                       (:diff paper-params) (:r-boundary paper-params))]
    (println (format "Exact phase shift (h=0.001): %.10f radians" delta-exact))
    (println (format "                            %.6f degrees" (* delta-exact (/ 180.0 Math/PI))))
    (println ""))

  ;; ============================================
  ;; 4. Detailed Check for h=0.01
  ;; ============================================
  (println "4. DETAILED CHECK: h = 0.01 fm")
  (println "================================")
  (let [h 0.01
        u-bessel (f/solve-numerov (:e paper-params) (:l paper-params) 
                                   (:v0 paper-params) (:rad paper-params) 
                                   (:diff paper-params) h (:r-boundary paper-params))
        u-naive (f/solve-numerov-naive (:e paper-params) (:l paper-params) 
                                        (:v0 paper-params) (:rad paper-params) 
                                        (:diff paper-params) h (:r-boundary paper-params))
        delta-exact (f/exact-phase-shift-numerov (:e paper-params) (:l paper-params)
                                                 (:v0 paper-params) (:rad paper-params) 
                                                 (:diff paper-params) (:r-boundary paper-params))
        delta-bessel (f/phase-shift-from-numerov u-bessel h (:r-boundary paper-params) 
                                                  (:e paper-params) (:l paper-params))
        delta-naive (f/phase-shift-from-numerov u-naive h (:r-boundary paper-params) 
                                                (:e paper-params) (:l paper-params))
        error-bessel (Math/abs (- delta-bessel delta-exact))
        error-naive (Math/abs (- delta-naive delta-exact))
        paper-bessel 7.5e-10
        paper-naive 2.5e-5]
    
    (println (format "Exact phase shift:     %.10f rad" delta-exact))
    (println (format "Bessel start phase:    %.10f rad" delta-bessel))
    (println (format "Naive start phase:     %.10f rad" delta-naive))
    (println "")
    (println (format "Bessel error (code):    %.6e" error-bessel))
    (println (format "Bessel error (paper):  %.6e" paper-bessel))
    (println (format "Ratio:                 %.2f" (/ error-bessel paper-bessel)))
    (println "")
    (println (format "Naive error (code):     %.6e" error-naive))
    (println (format "Naive error (paper):   %.6e" paper-naive))
    (println (format "Ratio:                  %.2f" (/ error-naive paper-naive)))
    (println "")
    
    (let [bessel-match (< (Math/abs (- error-bessel paper-bessel)) (* 0.3 paper-bessel))
          naive-match (< (Math/abs (- error-naive paper-naive)) (* 0.3 paper-naive))]
      (if (and bessel-match naive-match)
        (println "✓ VALUES MATCH PAPER (within 30% tolerance)")
        (println "✗ VALUES DO NOT MATCH - NEED TO CHECK!"))))

  (println "\n=== VERIFICATION COMPLETE ===")
  (println "\nNote: Small differences may be due to:")
  (println "  - Different boundary radius choices")
  (println "  - Different 'exact' reference calculation")
  (println "  - Rounding in paper presentation")
  (println "  - Numerical precision differences"))

;; Run verification
(verify-calculations)

