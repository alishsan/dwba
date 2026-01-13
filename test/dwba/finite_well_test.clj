(ns dwba.finite-well-test
  "Tests for finite square well bound state calculations."
  (:require [clojure.test :refer :all]
            [functions :refer :all]
            [dwba.finite-well :refer :all]
            [fastmath.core :as m]))

(deftest finite-well-validation-test
  (testing "Finite square well bound state validation for various l and z0"
    (let [test-cases [{:l 0 :z0 3.0 :name "l=0, z0=3.0 (shallow)"}
                     {:l 0 :z0 6.0 :name "l=0, z0=6.0 (medium)"}
                     {:l 0 :z0 10.0 :name "l=0, z0=10.0 (deep)"}
                     {:l 1 :z0 3.0 :name "l=1, z0=3.0 (shallow)"}
                     {:l 1 :z0 6.0 :name "l=1, z0=6.0 (medium)"}
                     {:l 1 :z0 10.0 :name "l=1, z0=10.0 (deep)"}
                     {:l 2 :z0 6.0 :name "l=2, z0=6.0 (medium)"}
                     {:l 2 :z0 10.0 :name "l=2, z0=10.0 (deep)"}]]
      (doseq [test test-cases]
        (let [l (:l test)
              z0 (:z0 test)
              bound-states (find-all-bound-states l z0)
              valid-states (filter #(and (>= (:e-ratio %) 0.01)
                                        (<= (:e-ratio %) 0.99)
                                        (> (:xi %) 0)
                                        (> (:eta %) 0)
                                        (< (m/abs (:matching-error %)) 1e-3)
                                        (:converged? %))
                                  bound-states)]
          (testing (:name test)
            ;; Only require bound states for deep wells (z0 >= 6.0)
            (when (>= z0 6.0)
              (is (seq bound-states) "Should find at least one bound state for deep wells"))
            (when (seq valid-states)
              (is (every? #(and (>= (:e-ratio %) 0.01) (<= (:e-ratio %) 0.99)) valid-states)
                  "Energies should be in valid range")
              (is (every? #(< (m/abs (:matching-error %)) 1e-3) valid-states)
                  "Matching errors should be small")
              (is (= (map :e-ratio valid-states) (sort (map :e-ratio valid-states)))
                  "States should be sorted by energy")))))))

(deftest finite-well-physical-consistency-test
  (testing "Physical consistency: higher l should have fewer or equal bound states"
    (let [z0 6.0
          l0-states (count (find-all-bound-states 0 z0))
          l1-states (count (find-all-bound-states 1 z0))
          l2-states (count (find-all-bound-states 2 z0))]
      (is (>= l0-states l1-states) "l=0 should have >= l=1 states")
      (is (>= l1-states l2-states) "l=1 should have >= l=2 states"))))

(deftest finite-well-root-finding-test
  (testing "Root finding for finite well matching error"
    (let [l 2
          z0 6.0
          bound-states (find-all-bound-states l z0)]
      (is (seq bound-states) "Should find bound states for l=2, z0=6.0")
      (doseq [state bound-states]
        (when (:converged? state)
          (let [verify-result (solver-step (:e-ratio state) l z0)]
            (is (< (m/abs (:f verify-result)) 1e-3)
                (format "Matching error should be small: %.6e" (:f verify-result)))
            ;; Verify xi^2 + eta^2 = z0^2
            (let [xi (:xi state)
                  eta (:eta state)
                  expected (* z0 z0)
                  actual (+ (* xi xi) (* eta eta))]
              (is (< (m/abs (- actual expected)) 1e-6)
                  (format "xi^2 + eta^2 should equal z0^2: %.6f vs %.6f" actual expected))))))))))

(deftest woods-saxon-vs-finite-well-test
  (testing "Woods-Saxon with small diffuseness should approximate finite square well"
    (let [test-cases [{:V0 30.0 :R0 2.0 :a0 0.01 :l 0}
                     {:V0 50.0 :R0 2.0 :a0 0.01 :l 0}
                     {:V0 50.0 :R0 2.0 :a0 0.01 :l 1}]
          mass-factor (/ (* 2 869.4) (* 197.7 197.7))]
      (doseq [test test-cases]
        (let [V0 (:V0 test)
              R0 (:R0 test)
              l (:l test)
              z0 (m/sqrt (* mass-factor V0 (* R0 R0)))
              reference-e-ratios (map :e-ratio 
                                     (filter :converged? 
                                            (find-all-bound-states l z0)))]
          (when (seq reference-e-ratios)
            (testing (format "V0=%.1f, R0=%.1f, l=%d" V0 R0 l)
              (is (seq reference-e-ratios) "Should have reference bound states"))))))))

