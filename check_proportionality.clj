;; Check if naive and Bessel solutions are proportional (scaled versions)

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)
(def r-max 10.0)

(println "=== CHECKING IF SOLUTIONS ARE PROPORTIONAL ===\n")

(let [u-bessel (solve-numerov e l v0 rad diff h r-max)
      u-naive (solve-numerov-naive e l v0 rad diff h r-max)
      
      ;; Check if u_naive = C * u_bessel for some constant C
      ;; Calculate C at different points
      idx-100 (min 100 (dec (count u-bessel)))
      idx-500 (min 500 (dec (count u-bessel)))
      idx-end (dec (count u-bessel))]
  
  (println "Checking if u_naive = C * u_bessel:")
  (doseq [[name idx] [["Early (r≈1 fm)" idx-100]
                      ["Mid (r≈5 fm)" idx-500]
                      ["End (r≈10 fm)" idx-end]]]
    (let [u-b (get u-bessel idx)
          u-n (get u-naive idx)
          c (if (zero? u-b) "N/A" (/ u-n u-b))
          ;; Check if ratio is constant
          expected-n (if (string? c) 0.0 (* c u-b))
          diff (Math/abs (- u-n expected-n))]
      (println (format "  %s:" name))
      (println (format "    u_bessel = %.8e" u-b))
      (println (format "    u_naive  = %.8e" u-n))
      (if (string? c)
        (println (format "    C = %s" c))
        (do
          (println (format "    C = u_n/u_b = %.8e" c))
          (println (format "    C * u_b = %.8e" expected-n))
          (println (format "    Difference: %.8e" diff))))
      (println ""))
  
  ;; Check R-matrix values
  (println "=== CHECKING R-MATRIX VALUES ===\n")
  (let [r-boundary 10.0
        bessel-extract (extract-wavefunction-at-boundary u-bessel h r-boundary)
        naive-extract (extract-wavefunction-at-boundary u-naive h r-boundary)
        u-b (:u bessel-extract)
        u-prime-b (:u-prime bessel-extract)
        r-actual (:r bessel-extract)
        u-n (:u naive-extract)
        u-prime-n (:u-prime naive-extract)
        R-bessel (/ u-b (* r-actual u-prime-b))
        R-naive (/ u-n (* r-actual u-prime-n))]
    (println (format "At r = %.1f fm:" r-boundary))
    (println (format "  Bessel: u = %.8e, u' = %.8e" u-b u-prime-b))
    (println (format "  Naive:  u = %.8e, u' = %.8e" u-n u-prime-n))
    (println (format "  Ratio u_n/u_b = %.8e" (/ u-n u-b)))
    (println (format "  Ratio u'_n/u'_b = %.8e" (/ u-prime-n u-prime-b)))
    (println (format "  R-matrix Bessel: %.10e" R-bessel))
    (println (format "  R-matrix Naive:  %.10e" R-naive))
    (println (format "  R-matrix difference: %.10e" (Math/abs (- R-bessel R-naive))))
    (println "")
    (if (< (Math/abs (- R-bessel R-naive)) 1e-10)
      (println "  ✓ R-matrices are identical (explains identical phase shifts!)")
      (println "  ✗ R-matrices differ")))))

