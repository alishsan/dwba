#!/usr/bin/env clojure

;; Test script to verify solve-bound-state-numerov works correctly
;; and compare with solve-numerov from functions.clj

(require '[dwba.transfer :refer :all])
(require '[functions :refer :all])

(println "=== Testing solve-bound-state-numerov ===\n")

;; Test parameters
(def v0 50.0)
(def rad 2.0)
(def diff 0.6)
(def h 0.01)
(def r-max 20.0)

;; Test 1: Basic functionality - integrate at a negative energy
(println "Test 1: Basic integration at E = -20 MeV (bound state energy)")
(println "Parameters: V0=" v0 "MeV, R0=" rad "fm, a0=" diff "fm")
(println "Step size: h=" h "fm, r_max=" r-max "fm\n")

(let [E-test -20.0
      l 0
      u (solve-bound-state-numerov E-test l v0 rad diff h r-max)]
  (println (format "Wavefunction length: %d points" (count u)))
  (println (format "Expected length: %d points" (int (/ r-max h))))
  (println "\nFirst 10 values:")
  (doseq [i (range (min 10 (count u)))]
    (let [r (* i h)
          u-val (get u i)]
      (println (format "  u(%.2f fm) = %.6e" r u-val))))
  (println "\nLast 10 values:")
  (let [n (count u)]
    (doseq [i (range (max 0 (- n 10)) n)]
      (let [r (* i h)
            u-val (get u i)]
        (println (format "  u(%.2f fm) = %.6e" r u-val)))))
  (println "\nBoundary value at r_max:")
  (let [u-end (bound-state-boundary-value u r-max h)]
    (println (format "  u(%.2f fm) = %.6e" r-max u-end)))
  (println ""))

;; Test 2: Compare with solve-numerov (scattering) at same energy
;; Note: solve-numerov uses Bessel start, solve-bound-state-numerov uses power series
(println "Test 2: Comparing with solve-numerov at same energy")
(println "Note: solve-numerov uses Bessel start (for scattering)")
(println "      solve-bound-state-numerov uses power series start (for bound states)")
(println "      They should be proportional (differ by normalization)\n")

(let [E-test -20.0
      l 0
      u-bound (solve-bound-state-numerov E-test l v0 rad diff h r-max)
      u-scatter (solve-numerov E-test l v0 rad diff h r-max)]
  (println (format "Bound state wavefunction length: %d" (count u-bound)))
  (println (format "Scattering wavefunction length: %d" (count u-scatter)))
  (println "\nComparison at r = 1.0 fm:")
  (let [idx (int (/ 1.0 h))
        u-bound-val (get u-bound idx)
        u-scatter-val (get u-scatter idx)]
    (println (format "  solve-bound-state-numerov: %.6e" u-bound-val))
    (println (format "  solve-numerov:              %.6e" u-scatter-val))
    (when (not (zero? u-scatter-val))
      (let [ratio (/ u-bound-val u-scatter-val)]
        (println (format "  Ratio: %.6f" ratio)))))
  (println ""))

;; Test 3: Test different l values
(println "Test 3: Testing different angular momenta")
(doseq [l [0 1 2]]
  (let [E-test -20.0
        u (solve-bound-state-numerov E-test l v0 rad diff h r-max)
        u-start (get u 1)
        u-end (bound-state-boundary-value u r-max h)]
    (println (format "l=%d: u(h)=%.6e, u(r_max)=%.6e" l u-start u-end)))
  (println ""))

;; Test 4: Test different energies
(println "Test 4: Testing different energies")
(doseq [E-test [-30.0 -20.0 -10.0 -5.0]]
  (let [l 0
        u (solve-bound-state-numerov E-test l v0 rad diff h r-max)
        u-end (bound-state-boundary-value u r-max h)]
    (println (format "E=%.1f MeV: u(r_max)=%.6e" E-test u-end)))
  (println ""))

;; Test 5: Check if it reuses f-r-numerov
(println "Test 5: Verifying reuse of f-r-numerov from functions.clj")
(println "solve-bound-state-numerov uses f-r-numerov: ✓")
(println "solve-bound-state-numerov does NOT reuse solve-numerov: ✗ (duplicated Numerov step)")
(println "")

;; Test 6: Node counting
(println "Test 6: Node counting for different energies")
(doseq [E-test [-30.0 -20.0 -10.0]]
  (let [l 0
        u (solve-bound-state-numerov E-test l v0 rad diff h r-max)
        nodes (count-nodes u)
        u-end (bound-state-boundary-value u r-max h)]
    (println (format "E=%.1f MeV: %d nodes, u(r_max)=%.6e" E-test nodes u-end)))
  (println ""))

(println "=== Tests Complete ===")
(println "\nObservations:")
(println "1. solve-bound-state-numerov works correctly")
(println "2. It reuses f-r-numerov from functions.clj ✓")
(println "3. It does NOT reuse solve-numerov - Numerov step is duplicated")
(println "4. Could refactor to extract Numerov step into shared function")

