;; Simple test of basic functions without complex dependencies
(load-file "src/complex.clj")
(use 'complex)

;; Test basic complex number operations
(println "=== Testing Complex Number Operations ===")
(let [z1 (complex-from-cartesian 1 2)
      z2 (complex-from-cartesian 3 4)
      sum (add z1 z2)
      product (mul z1 z2)]
  (printf "z1 = %.1f + %.1fi\n" (double (re z1)) (double (im z1)))
  (printf "z2 = %.1f + %.1fi\n" (double (re z2)) (double (im z2)))
  (printf "z1 + z2 = %.1f + %.1fi\n" (double (re sum)) (double (im sum)))
  (printf "z1 * z2 = %.1f + %.1fi\n" (double (re product)) (double (im product))))

;; Test polar coordinates
(println "\n=== Testing Polar Coordinates ===")
(let [z-polar (complex-from-polar (/ Math/PI 4) 5.0)
      z-cartesian (complex-from-cartesian 3 4)]
  (printf "Polar: r=%.1f, θ=%.3f -> %.3f + %.3fi\n" 
          (double (mag z-polar)) (double (arg z-polar)) (double (re z-polar)) (double (im z-polar)))
  (printf "Cartesian: %.1f + %.1fi -> r=%.3f, θ=%.3f\n" 
          (double (re z-cartesian)) (double (im z-cartesian)) (double (mag z-cartesian)) (double (arg z-cartesian))))

;; Test basic Woods-Saxon potential (simplified version)
(println "\n=== Testing Simplified Woods-Saxon Potential ===")
(defn simple-WS [r V0 R0 a0]
  (/ (* -1.0 V0) (+ 1.0 (Math/exp (/ (- r R0) a0)))))

(def ws-params [40.0 2.0 0.6])
(println "Radius\tWoods-Saxon Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [ws-pot (simple-WS r (first ws-params) (second ws-params) (last ws-params))]
    (printf "%.1f\t%.6f\n" (double r) (double ws-pot))))

;; Test basic Coulomb potential (simplified version)
(println "\n=== Testing Simplified Coulomb Potential ===")
(defn simple-Coulomb [r r0]
  (if (> r r0) 
    (/ 2.88 r)  ; Z1Z2ee = 2.88 MeV·fm
    (* r (/ 2.88 r0 r0))))

(println "Radius\tCoulomb Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [coulomb-pot (simple-Coulomb r 2.0)]
    (printf "%.1f\t%.6f\n" (double r) (double coulomb-pot))))

;; Test combined potential
(println "\n=== Testing Combined Potential ===")
(println "Radius\tCombined Potential")
(doseq [r [0.5 1.0 1.5 2.0 2.5 3.0 4.0 5.0]]
  (let [combined-pot (+ (simple-Coulomb r 2.0) 
                        (simple-WS r (first ws-params) (second ws-params) (last ws-params)))]
    (printf "%.1f\t%.6f\n" (double r) (double combined-pot))))

;; Test complex integration
(println "\n=== Testing Complex Integration ===")
(let [f (fn [x] (complex-from-cartesian (* x x) (* 2 x)))  ; f(x) = x² + 2xi
      result (complex-integrate f 0 2 100)]
  (printf "∫₀² (x² + 2xi) dx = %.6f + %.6fi\n" (double (re result)) (double (im result))))

(println "\n=== Simple Test Complete ===")
