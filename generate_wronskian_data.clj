;; Generate Wronskian stability data for the paper

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def r-max 10.0)

(println "=== GENERATING WRONSKIAN STABILITY DATA ===\n")

;; Test at different step sizes
(doseq [h [0.1 0.05 0.01]]
  (println (format "=== h = %.2f fm ===" h))
  
  (let [u-bessel (solve-numerov e l v0 rad diff h r-max)
        u-naive (solve-numerov-naive e l v0 rad diff h r-max)
        w-bessel (check-wronskian u-bessel e l v0 rad diff h)
        w-naive (check-wronskian u-naive e l v0 rad diff h)
        w-b-first (first w-bessel)
        w-n-first (first w-naive)
        w-b-drift (apply max (map #(Math/abs (- % w-b-first)) w-bessel))
        w-n-drift (apply max (map #(Math/abs (- % w-n-first)) w-naive))]
    
    (println (format "Bessel start:"))
    (println (format "  First Wronskian value: %.10e" w-b-first))
    (println (format "  Max drift: %.10e" w-b-drift))
    (println (format "  Relative drift: %.10e" (/ w-b-drift (Math/abs w-b-first))))
    (println "")
    (println (format "Naive start:"))
    (println (format "  First Wronskian value: %.10e" w-n-first))
    (println (format "  Max drift: %.10e" w-n-drift))
    (println (format "  Relative drift: %.10e" (/ w-n-drift (Math/abs w-n-first))))
    (println "")
    (println (format "Improvement factor: %.2f" (/ w-n-drift w-b-drift)))
    (println ""))))

