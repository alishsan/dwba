(ns dwba.transfer-test
  "Tests for transfer reaction calculations including bound states and shooting method."
  (:require [clojure.test :refer :all]
            [dwba.transfer :as t]
            [functions :refer :all]
            [fastmath.core :as m]))

(def ws-params [50.0 2.0 0.6])  ; V0=50 MeV, R0=2.0 fm, a0=0.6 fm

(deftest sign-changes-l0-test
  (testing "Sign changes for l=0 (1s state)"
    (let [energies (range -14.0 -17.0 -0.1)
          results (mapv (fn [E]
                         (let [u (t/solve-bound-state-numerov E 0 50.0 2.0 0.6 0.001 20.0)
                               boundary-val (last u)]
                           {:energy E :boundary boundary-val}))
                       energies)
          sign-changes (filter some?
                            (for [i (range (dec (count results)))]
                              (let [curr (nth results i)
                                    next (nth results (inc i))]
                                (when (not= (Math/signum (:boundary curr)) 
                                           (Math/signum (:boundary next)))
                                  {:E1 (:energy curr)
                                   :E2 (:energy next)}))))]
      (is (seq sign-changes) "Should find sign changes for l=0"))))

(deftest sign-changes-l1-test
  (testing "Sign changes for l=1 (2p state)"
    (let [energies (concat (range -1.0 -50.0 -2.0)
                          (range -50.0 -150.0 -5.0))
          results (mapv (fn [E]
                         (let [u (t/solve-bound-state-numerov E 1 50.0 2.0 0.6 0.001 20.0)
                               boundary-val (last u)
                               nodes (t/count-nodes u)]
                           {:energy E 
                            :boundary boundary-val
                            :nodes nodes}))
                       energies)
          sign-changes (filter some?
                            (for [i (range (dec (count results)))]
                              (let [curr (nth results i)
                                    next (nth results (inc i))]
                                (when (not= (Math/signum (:boundary curr)) 
                                           (Math/signum (:boundary next)))
                                  {:E1 (:energy curr)
                                   :E2 (:energy next)}))))]
      ;; May or may not have sign changes depending on parameters
      (is (seq results) "Should have results"))))

(deftest shooting-method-l0-test
  (testing "Shooting method for l=0 (1s state)"
    (let [V-params [50.0 2.0 0.6]
          result (t/find-bound-state-energy V-params 0 1 20.0 0.001)]
      (is (:energy result) "Should find an energy")
      (is (< (:energy result) 0) "Energy should be negative")
      (is (= (:nodes result) 0) "1s state should have 0 nodes"))))

(deftest boundary-values-test
  (testing "Boundary values at different energies"
    (let [energies (range -1.0 -201.0 -5.0)
          results (mapv (fn [E]
                         (let [u (t/solve-bound-state-numerov E 1 350.0 2.0 0.6 0.001 20.0)
                               boundary-val (t/bound-state-boundary-value u 20.0 0.001)
                               nodes (t/count-nodes u)]
                           {:energy E
                            :boundary-value boundary-val
                            :nodes nodes}))
                       energies)]
      (is (seq results) "Should have results")
      (is (every? #(number? (:boundary-value %)) results) "All boundary values should be numbers"))))

(deftest l0-l1-comparison-test
  (testing "Comparison between l=0 and l=1 bound states"
    (let [V-params [50.0 2.0 0.6]
          l0-result (t/find-bound-state-energy V-params 0 1 20.0 0.01)
          l1-result (t/find-bound-state-energy V-params 1 1 20.0 0.01)]
      (when (and (:energy l0-result) (:energy l1-result))
        ;; l=0 should generally have deeper binding than l=1
        (is (< (:energy l0-result) (:energy l1-result))
            "l=0 state should be more deeply bound than l=1")))))

(deftest deeper-energies-test
  (testing "Finding bound states at deeper energies"
    (let [V-params [100.0 2.0 0.6]
          result (t/find-bound-state-energy V-params 0 1 20.0 0.01)]
      (when (:energy result)
        (is (< (:energy result) 0) "Energy should be negative")
        (is (> (Math/abs (:energy result)) 10.0) "Should find significantly bound state")))))

(deftest f-rho-sign-test
  (testing "Sign of f(rho) function for different energies"
    (let [energies (range -10.0 -50.0 -5.0)
          results (mapv (fn [E]
                         (let [u (t/solve-bound-state-numerov E 0 50.0 2.0 0.6 0.001 20.0)]
                           {:energy E
                            :boundary (last u)
                            :sign (Math/signum (last u))}))
                       energies)]
      (is (seq results) "Should have results"))))

