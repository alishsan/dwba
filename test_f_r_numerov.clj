;; Test f-r-numerov function
;; Tests the effective potential function for Numerov integration

(require '[functions :refer :all])

(println "=== TESTING f-r-numerov FUNCTION ===")
(println "")

;; Test parameters
(def test-params
  {:e 2.0      ; MeV
   :l 1
   :v0 46.23   ; MeV
   :rad 2.0    ; fm (R0)
   :diff 0.5   ; fm (a0)
   })

(defn test-r-zero []
  "Test that f-r-numerov returns infinity at r=0"
  (println "1. Testing r=0 (should return infinity):")
  (let [result (f-r-numerov 0.0 (:e test-params) (:l test-params) 
                            (:v0 test-params) (:rad test-params) (:diff test-params))]
    (println (format "   f-r-numerov(0) = %s" result))
    (if (Double/isInfinite result)
      (println "   ✓ PASS: Returns infinity as expected")
      (println "   ✗ FAIL: Should return infinity"))
    (println "")))

(defn test-basic-values []
  "Test f-r-numerov at various radii"
  (println "2. Testing f-r-numerov at various radii:")
  (println (format "   Parameters: E=%.2f MeV, l=%d, V0=%.2f MeV, R0=%.2f fm, a0=%.2f fm" 
                  (:e test-params) (:l test-params) (:v0 test-params) 
                  (:rad test-params) (:diff test-params)))
  (println "")
  (println (format "%-8s %-15s %-15s %-15s" "r (fm)" "f(r)" "V_WS(r)" "Centrifugal"))
  (println (apply str (repeat 70 "-")))
  
  (doseq [r [0.1 0.5 1.0 2.0 3.0 5.0 10.0]]
    (let [f-val (f-r-numerov r (:e test-params) (:l test-params) 
                            (:v0 test-params) (:rad test-params) (:diff test-params))
          v-ws (woods-saxon-numerov r (:v0 test-params) (:rad test-params) (:diff test-params))
          centrifugal (/ (* (:l test-params) (inc (:l test-params))) (* mass-factor r r))
          v-eff (+ v-ws centrifugal)]
      (println (format "%-8.2f %-15.6e %-15.6f %-15.6e" r f-val v-ws centrifugal))))
  (println ""))

(defn test-different-l []
  "Test f-r-numerov with different angular momenta"
  (println "3. Testing with different angular momenta (l=0, 1, 2):")
  (println (format "   r=2.0 fm, E=%.2f MeV" (:e test-params)))
  (println "")
  (println (format "%-8s %-15s" "l" "f(r)"))
  (println (apply str (repeat 30 "-")))
  
  (doseq [l [0 1 2]]
    (let [f-val (f-r-numerov 2.0 (:e test-params) l 
                            (:v0 test-params) (:rad test-params) (:diff test-params))]
      (println (format "%-8d %-15.6e" l f-val))))
  (println ""))

(defn test-different-energies []
  "Test f-r-numerov with different energies"
  (println "4. Testing with different energies:")
  (println (format "   r=2.0 fm, l=%d" (:l test-params)))
  (println "")
  (println (format "%-8s %-15s" "E (MeV)" "f(r)"))
  (println (apply str (repeat 30 "-")))
  
  (doseq [e [0.5 1.0 2.0 5.0 10.0]]
    (let [f-val (f-r-numerov 2.0 e (:l test-params) 
                            (:v0 test-params) (:rad test-params) (:diff test-params))]
      (println (format "%-8.2f %-15.6e" e f-val)))
  (println "")))

(defn test-formula-consistency []
  "Test that f-r-numerov matches the expected formula: mass-factor * (V_eff - E)"
  (println "5. Testing formula consistency:")
  (println "   f(r) = mass-factor * (V_eff - E)")
  (println "   where V_eff = V_WS + l(l+1)/(mass-factor * r^2)")
  (println "")
  
  (let [r 2.0
        e (:e test-params)
        l (:l test-params)
        v0 (:v0 test-params)
        rad (:rad test-params)
        diff (:diff test-params)
        f-computed (f-r-numerov r e l v0 rad diff)
        v-ws (woods-saxon-numerov r v0 rad diff)
        centrifugal (/ (* l (inc l)) (* mass-factor r r))
        v-eff (+ v-ws centrifugal)
        f-expected (* mass-factor (- v-eff e))
        error (Math/abs (- f-computed f-expected))]
    (println (format "   r = %.2f fm" r))
    (println (format "   V_WS = %.6f MeV" v-ws))
    (println (format "   Centrifugal = %.6e MeV" centrifugal))
    (println (format "   V_eff = %.6f MeV" v-eff))
    (println (format "   E = %.2f MeV" e))
    (println (format "   f(r) computed = %.6e" f-computed))
    (println (format "   f(r) expected = %.6e" f-expected))
    (println (format "   Error = %.6e" error))
    (if (< error 1e-10)
      (println "   ✓ PASS: Formula consistency check")
      (println "   ✗ FAIL: Formula mismatch"))
    (println "")))

;; Run all tests
(test-r-zero)
(test-basic-values)
(test-different-l)
(test-different-energies)
(test-formula-consistency)

(println "=== TEST COMPLETE ===")
