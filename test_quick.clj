(require '[functions :refer :all])

(let [e 2.0 l 1 v0 46.23 rad 2.0 diff 0.5 h 0.01 r-boundary 10.0
      u-bessel (solve-numerov e l v0 rad diff h r-boundary)
      u-naive (solve-numerov-naive e l v0 rad diff h r-boundary)
      idx (int (/ r-boundary h))
      u-b-at-boundary (get u-bessel idx)
      u-n-at-boundary (get u-naive idx)
      u-b-prime (/ (- (get u-bessel (inc idx)) (get u-bessel (dec idx))) (* 2 h))
      u-n-prime (/ (- (get u-naive (inc idx)) (get u-naive (dec idx))) (* 2 h))]
  (println "Direct comparison at boundary:")
  (println (format "Bessel: u=%.10e u'=%.10e" u-b-at-boundary u-b-prime))
  (println (format "Naive:  u=%.10e u'=%.10e" u-n-at-boundary u-n-prime))
  (println (format "u diff: %.10e" (Math/abs (- u-b-at-boundary u-n-at-boundary))))
  (println (format "u' diff: %.10e" (Math/abs (- u-b-prime u-n-prime))))
  (println (format "R-bessel: %.10e" (/ u-b-at-boundary (* r-boundary u-b-prime))))
  (println (format "R-naive:  %.10e" (/ u-n-at-boundary (* r-boundary u-n-prime)))))
