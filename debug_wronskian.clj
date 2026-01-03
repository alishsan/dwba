;; Debug Wronskian calculation - should be ~10^-14, not 10^-2

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)
(def r-max 10.0)

(println "=== DEBUGGING WRONSKIAN CALCULATION ===\n")

;; Test Wronskian for Bessel start
(let [u-bessel (solve-numerov e l v0 rad diff h r-max)
      w-bessel (check-wronskian u-bessel e l v0 rad diff h)
      w-first (first w-bessel)
      w-diffs (map #(Math/abs (- % w-first)) w-bessel)
      w-max-diff (apply max w-diffs)]
  (println "Bessel Start Wronskian:")
  (println (format "  First value: %.10e" w-first))
  (println (format "  Number of points: %d" (count w-bessel)))
  (println (format "  First 5 values: %s" (take 5 w-bessel)))
  (println (format "  Last 5 values: %s" (take-last 5 w-bessel)))
  (println (format "  Max deviation from first: %.10e" w-max-diff))
  (println (format "  Should be ~10^-14, got: %.2e" w-max-diff))
  (println ""))

;; Test Wronskian for Naive start
(let [u-naive (solve-numerov-naive e l v0 rad diff h r-max)
      w-naive (check-wronskian u-naive e l v0 rad diff h)
      w-first (first w-naive)
      w-diffs (map #(Math/abs (- % w-first)) w-naive)
      w-max-diff (apply max w-diffs)]
  (println "Naive Start Wronskian:")
  (println (format "  First value: %.10e" w-first))
  (println (format "  Number of points: %d" (count w-naive)))
  (println (format "  First 5 values: %s" (take 5 w-naive)))
  (println (format "  Last 5 values: %s" (take-last 5 w-naive)))
  (println (format "  Max deviation from first: %.10e" w-max-diff))
  (println ""))

;; Check the Wronskian formula
(println "=== CHECKING WRONSKIAN FORMULA ===")
(println "Paper formula: W_n = (1 - h²/12 f_{n+1}) u_{n+1} v_n - (1 - h²/12 f_n) u_n v_{n+1}")
(println "For single solution, this checks conservation of a related quantity.")
(println "")
(println "Our formula uses: u_n, u_{n+1}, u_{n-1}")
(println "This might be checking a different quantity than the paper's Wronskian.")

