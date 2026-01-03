;; Calculate alpha-n p-wave phase shifts for energies 0-15 MeV
;; Look for resonances (phase shift passing through π/2)

(require '[functions :refer :all])

;; Alpha-n reduced mass
(def mu-alpha-n 750.4085864615)
(def mass-factor-alpha-n (/ (* 2 mu-alpha-n) hbarc hbarc))

;; Woods-Saxon parameters
(def ws-params [40.0 2.0 0.65])  ; [V₀, R, a]

;; Energy ranges: full range and fine-grained around potential resonances
(def E-full (map #(+ 0.5 (* % 0.1)) (range 0 146)))  ; 0.5 to 15.0 MeV, step 0.1
(def E-fine-1 (map #(+ 2.0 (* % 0.01)) (range 0 51)))  ; 2.0 to 2.5 MeV, step 0.01
(def E-fine-2 (map #(+ 14.0 (* % 0.01)) (range 0 101)))  ; 14.0 to 15.0 MeV, step 0.01

(println "=== ALPHA-N P-WAVE PHASE SHIFTS ===")
(println "Woods-Saxon: V₀=50 MeV, R=2.0 fm, a=0.6 fm")
(println "Reduced mass: 750.41 MeV/c²")
(println "")

;; Calculate phase shifts with Coulomb off
(with-redefs [mu mu-alpha-n
              mass-factor mass-factor-alpha-n
              Z1Z2ee 0.0]
  
  ;; Full range calculation
  (println "=== FULL RANGE (0.5 - 15.0 MeV, step 0.1) ===")
  (println "E(MeV)   delta_p(rad)   delta_p(deg)")
  (doseq [E E-full]
    (let [d (phase-shift E ws-params 1)
          w (- (mod (+ d Math/PI) (* 2 Math/PI)) Math/PI)]
      (printf "%5.2f     %8.5f      %8.3f\n" E w (* w (/ 180.0 Math/PI)))))
  
  (println "")
  (println "=== FINE-GRAINED: 2.0 - 2.5 MeV (step 0.01) ===")
  (println "Looking for resonance around first sign flip...")
  (println "E(MeV)   delta_p(rad)   delta_p(deg)   |delta_p|")
  (doseq [E E-fine-1]
    (let [d (phase-shift E ws-params 1)
          w (- (mod (+ d Math/PI) (* 2 Math/PI)) Math/PI)
          abs-delta (Math/abs w)]
      (printf "%5.2f     %8.5f      %8.3f      %8.3f\n" E w (* w (/ 180.0 Math/PI)) (* abs-delta (/ 180.0 Math/PI)))
      ;; Check if near π/2
      (when (and (> abs-delta 1.4) (< abs-delta 1.7))
        (println (str "  → Near π/2 at " (format "%.2f" E) " MeV!")))))
  
  (println "")
  (println "=== FINE-GRAINED: 14.0 - 15.0 MeV (step 0.01) ===")
  (println "Looking for resonance around second sign flip...")
  (println "E(MeV)   delta_p(rad)   delta_p(deg)   |delta_p|")
  (doseq [E E-fine-2]
    (let [d (phase-shift E ws-params 1)
          w (- (mod (+ d Math/PI) (* 2 Math/PI)) Math/PI)
          abs-delta (Math/abs w)]
      (printf "%5.2f     %8.5f      %8.3f      %8.3f\n" E w (* w (/ 180.0 Math/PI)) (* abs-delta (/ 180.0 Math/PI)))
      ;; Check if near π/2
      (when (and (> abs-delta 1.4) (< abs-delta 1.7))
        (println (str "  → Near π/2 at " (format "%.2f" E) " MeV!")))))
  
  (println "")
  (println "=== RESONANCE ANALYSIS ===")
  (println "Phase shift passing through π/2 indicates a resonance")
  (println "Sign flips occur at:")
  (println "  1. Between 2.0 and 2.5 MeV")
  (println "  2. Between 14.0 and 15.0 MeV")
  (println "")
  (println "Calculating exact resonance energies...")
  
  ;; Find energies where phase shift magnitude is closest to π/2
  (let [resonance-1 (first (sort-by #(Math/abs (- (Math/abs %) (/ Math/PI 2)))
                                     (map (fn [E]
                                            (let [d (phase-shift E ws-params 1)
                                                  w (- (mod (+ d Math/PI) (* 2 Math/PI)) Math/PI)]
                                              {:E E :delta w}))
                                          E-fine-1)))
        resonance-2 (first (sort-by #(Math/abs (- (Math/abs %) (/ Math/PI 2)))
                                     (map (fn [E]
                                            (let [d (phase-shift E ws-params 1)
                                                  w (- (mod (+ d Math/PI) (* 2 Math/PI)) Math/PI)]
                                              {:E E :delta w}))
                                          E-fine-2)))]
    (println (str "Resonance 1: E = " (format "%.3f" (:E resonance-1)) " MeV, δ = " 
                  (format "%.5f" (:delta resonance-1)) " rad (" 
                  (format "%.3f" (* (:delta resonance-1) (/ 180.0 Math/PI))) "°)"))
    (println (str "Resonance 2: E = " (format "%.3f" (:E resonance-2)) " MeV, δ = " 
                  (format "%.5f" (:delta resonance-2)) " rad (" 
                  (format "%.3f" (* (:delta resonance-2) (/ 180.0 Math/PI))) "°)"))))

