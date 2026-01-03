;; Debug NaN issues in Numerov and phase shift extraction

(require '[functions :refer :all])

(println "=== DEBUGGING NaN ISSUES ===\n")

;; Test parameters
(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)
(def r-boundary 10.0)

;; Test 1: Check if Numerov runs
(println "1. Testing Numerov integration...")
(try
  (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)]
    (println (format "  ✓ Numerov completed: %d points" (count u-bessel)))
    (println (format "  First 5 values: %s" (take 5 u-bessel)))
    (println (format "  Last 5 values: %s" (take-last 5 u-bessel)))
    (println (format "  Any NaN? %s" (some #(Double/isNaN %) u-bessel)))
    (println (format "  Any Inf? %s" (some #(Double/isInfinite %) u-bessel))))
  (catch Exception ex
    (println (format "  ✗ Error: %s" (.getMessage ex)))))

(println "")

;; Test 2: Check R-matrix calculation
(println "2. Testing R-matrix extraction...")
(try
  (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
        {:keys [u u-prime r index]} (extract-wavefunction-at-boundary u-bessel h r-boundary)]
    (println (format "  u(a) = %.10e" u))
    (println (format "  u'(a) = %.10e" u-prime))
    (println (format "  r = %.3f" r))
    (println (format "  index = %d" index))
    (println (format "  u'(a) is zero? %s" (zero? u-prime)))
    (println (format "  u'(a) is NaN? %s" (Double/isNaN u-prime)))
    
    (let [R (/ u (* r-boundary u-prime))]
      (println (format "  R = %.10e" R))
      (println (format "  R is NaN? %s" (Double/isNaN R)))
      (println (format "  R is Inf? %s" (Double/isInfinite R)))))
  (catch Exception ex
    (println (format "  ✗ Error: %s" (.getMessage ex)))))

(println "")

;; Test 3: Check phase shift calculation
(println "3. Testing phase shift extraction...")
(try
  (let [u-bessel (solve-numerov e l v0 rad diff h r-boundary)
        R (r-matrix-from-numerov u-bessel h r-boundary)
        k (m/sqrt (* mass-factor e))
        rho (* k r-boundary)]
    (println (format "  k = %.10e" k))
    (println (format "  rho = k*a = %.10e" rho))
    (println (format "  R = %.10e" R))
    
    (let [j-l (f-func l rho)
          j-l-prime (f-func-deriv l rho)
          y-l (g-func l rho)
          y-l-prime (g-func-deriv l rho)]
      (println (format "  j_l(rho) = %.10e" j-l))
      (println (format "  j_l'(rho) = %.10e" j-l-prime))
      (println (format "  y_l(rho) = %.10e" y-l))
      (println (format "  y_l'(rho) = %.10e" y-l-prime))
      
      (let [numerator (- (* R r-boundary j-l-prime) j-l)
            denominator (- (* R r-boundary y-l-prime) y-l)]
        (println (format "  numerator = %.10e" numerator))
        (println (format "  denominator = %.10e" denominator))
        (println (format "  denominator is zero? %s" (zero? denominator)))
        (println (format "  denominator is NaN? %s" (Double/isNaN denominator)))
        
        (let [delta (Math/atan2 numerator denominator)]
          (println (format "  delta = %.10e" delta))
          (println (format "  delta is NaN? %s" (Double/isNaN delta)))))))
  (catch Exception ex
    (println (format "  ✗ Error: %s" (.getMessage ex)))
    (.printStackTrace ex)))

(println "")

;; Test 4: Check exact phase shift
(println "4. Testing exact phase shift...")
(try
  (let [h-fine 0.001
        u (solve-numerov e l v0 rad diff h-fine r-boundary)]
    (println (format "  Fine grid: %d points" (count u)))
    (let [delta (phase-shift-from-numerov u h-fine r-boundary e l)]
      (println (format "  delta = %.10e" delta))
      (println (format "  delta is NaN? %s" (Double/isNaN delta)))))
  (catch Exception ex
    (println (format "  ✗ Error: %s" (.getMessage ex)))
    (.printStackTrace ex)))

