;; Check if naive and Bessel starts are actually different

(require '[functions :refer :all])

(def e 2.0)
(def l 1)
(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def h 0.01)

(println "=== CHECKING INITIALIZATION DIFFERENCES ===\n")

;; Check initial values
(let [q (Math/sqrt (* mass-factor (+ e v0)))
      u0-bessel 0.0
      u1-bessel (bessel-start-l1 h q)
      u0-naive 0.0
      u1-naive (naive-start-l1 h)]
  (println "Initial conditions:")
  (println (format "  Bessel: u(0) = %.10e, u(h) = %.10e" u0-bessel u1-bessel))
  (println (format "  Naive:  u(0) = %.10e, u(h) = %.10e" u0-naive u1-naive))
  (println (format "  Difference at r=h: %.10e" (Math/abs (- u1-bessel u1-naive))))
  (println "")

  ;; Check what q is
  (println (format "q = sqrt(2m(E+V0)/hbar²) = %.10e" q))
  (println (format "q*h = %.10e" (* q h)))
  (println "")

  ;; Check Bessel expansion
  (let [qh (* q h)
        bessel-exact (/ (- (Math/sin qh) (* qh (Math/cos qh))) qh)
        bessel-approx (+ (/ (* qh qh) 3.0) (/ (* qh qh qh qh) 30.0))]
    (println "Bessel function values:")
    (println (format "  Exact F₁(qh): %.10e" bessel-exact))
    (println (format "  Approx (qh)²/3 - (qh)⁴/30: %.10e" bessel-approx))
    (println (format "  Our bessel-start-l1: %.10e" u1-bessel))
    (println "")))

;; Check wavefunctions at early steps
(println "=== WAVEFUNCTION VALUES AT EARLY STEPS ===\n")
(let [r-max 1.0  ; Only go to 1 fm to see early differences
      u-bessel (solve-numerov e l v0 rad diff h r-max)
      u-naive (solve-numerov-naive e l v0 rad diff h r-max)]
  (println (format "Comparing first 10 points (r = 0 to %.1f fm):" r-max))
  (doseq [i (range (min 10 (count u-bessel) (count u-naive)))]
    (let [r (* i h)
          u-b (get u-bessel i)
          u-n (get u-naive i)
          diff (Math/abs (- u-b u-n))]
      (println (format "  r=%.3f: Bessel=%.8e, Naive=%.8e, Diff=%.8e" 
                       r u-b u-n diff)))))

;; Check if they converge
(println "\n=== CHECKING CONVERGENCE ===\n")
(let [r-max 10.0
      u-bessel (solve-numerov e l v0 rad diff h r-max)
      u-naive (solve-numerov-naive e l v0 rad diff h r-max)
      idx-100 (min 100 (dec (count u-bessel)))
      idx-500 (min 500 (dec (count u-bessel)))
      idx-end (dec (count u-bessel))]
  (println "Wavefunction differences at different points:")
  (doseq [[name idx] [["Early (r≈1 fm)" idx-100]
                      ["Mid (r≈5 fm)" idx-500]
                      ["End (r≈10 fm)" idx-end]]]
    (let [u-b (get u-bessel idx)
          u-n (get u-naive idx)
          diff (Math/abs (- u-b u-n))
          ratio (if (zero? u-b) "N/A" (format "%.2e" (/ diff (Math/abs u-b))))]
      (println (format "  %s: |u_b - u_n| = %.8e (relative: %s)" name diff ratio)))))

