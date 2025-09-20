(ns dwba.kinematic-roundtrip-test
  (:require [clojure.test :refer :all]
            [functions :refer :all]))

;; Test parameters for p + ⁴He
(def mp 938.272)  ; MeV/c²
(def mHe 3727.379)  ; MeV/c²

;; Helper function for approximate equality
(defn approx= [a b tolerance]
  (< (Math/abs (- a b)) tolerance))

(defn test-roundtrip-conversion [theta-lab-deg m1 m2]
  "Test that lab-to-cm followed by cm-to-lab returns original angle"
  (let [theta-lab-rad (* theta-lab-deg Math/PI (/ 180.0))
        theta-cm-rad (lab-to-cm-angle theta-lab-rad m1 m2)
        theta-lab-back-rad (cm-to-lab-angle theta-cm-rad m1 m2)
        theta-lab-back-deg (* theta-lab-back-rad (/ 180.0) Math/PI)
        difference (Math/abs (- theta-lab-deg theta-lab-back-deg))]
    {:original-lab-deg theta-lab-deg
     :cm-rad theta-cm-rad
     :cm-deg (* theta-cm-rad (/ 180.0) Math/PI)
     :back-to-lab-deg theta-lab-back-deg
     :difference-deg difference
     :success? (< difference 0.001)}))

(deftest test-basic-roundtrip-conversion
  (testing "Basic round-trip conversion for p + ⁴He"
    (let [result (test-roundtrip-conversion 90 mp mHe)]
      (is (:success? result) 
          (str "Round-trip conversion failed: " 
               "Lab " (:original-lab-deg result) "° -> CM " (:cm-deg result) "° -> Lab " (:back-to-lab-deg result) "° | Diff: " (:difference-deg result) "°")))))

(deftest test-various-angles
  (testing "Round-trip conversion for various angles"
    (let [test-angles [0 30 60 90 120 150 165 180]
          results (map #(test-roundtrip-conversion % mp mHe) test-angles)
          failures (filter #(not (:success? %)) results)]
      (is (empty? failures) 
          (str "Round-trip conversion failed for angles: " 
               (map :original-lab-deg failures))))))

(deftest test-edge-cases
  (testing "Round-trip conversion for edge cases"
    (let [edge-cases [0.1 1 5 10 45 90 135 170 175 179 179.9]
          results (map #(test-roundtrip-conversion % mp mHe) edge-cases)
          failures (filter #(not (:success? %)) results)]
      (is (empty? failures) 
          (str "Round-trip conversion failed for edge cases: " 
               (map :original-lab-deg failures))))))

(deftest test-different-mass-ratios
  (testing "Round-trip conversion for different mass ratios"
    (let [mass-ratios [[938.272 3727.379]  ; p + ⁴He (0.25)
                       [938.272 938.272]   ; p + p (1.0)
                       [3727.379 938.272]  ; ⁴He + p (4.0)
                       [938.272 18756.0]]  ; p + heavy (0.05)
          results (map #(test-roundtrip-conversion 90 (first %) (second %)) mass-ratios)
          failures (filter #(not (:success? %)) results)]
      (is (empty? failures) 
          (str "Round-trip conversion failed for mass ratios: " 
               (map #(/ (first %) (+ (first %) (second %))) 
                    (map #(vector (first %) (second %)) 
                         (map #(vector (:original-lab-deg %) (:cm-deg %) (:back-to-lab-deg %)) failures))))))))

(deftest test-energy-conversion
  (testing "Energy conversion consistency"
    (let [E-lab 5.0  ; MeV
          E-cm (lab-to-cm-energy E-lab mp mHe)
          expected-ratio (/ mHe (+ mp mHe))]
      (is (approx= E-cm (* E-lab expected-ratio) 0.001)
          (str "Energy conversion failed: E-lab=" E-lab " MeV, E-cm=" E-cm " MeV, expected=" (* E-lab expected-ratio) " MeV")))))

;; Run tests with detailed output
(defn run-kinematic-tests []
  (println "=== KINEMATIC ROUND-TRIP CONVERSION TEST ===")
  (println "Testing p + ⁴He scattering (m1/m2 = 0.25)")
  (println)
  
  (let [test-angles [0 30 60 90 120 150 165 180]]
    (doseq [angle test-angles]
      (let [result (test-roundtrip-conversion angle mp mHe)]
        (println (format "Lab angle: %3d° -> CM: %6.2f° -> Lab: %6.2f° | Diff: %8.5f° | %s"
                         (:original-lab-deg result)
                         (:cm-deg result)
                         (:back-to-lab-deg result)
                         (:difference-deg result)
                         (if (:success? result) "✅ PASS" "❌ FAIL"))))))
  
  (println)
  (println "=== EDGE CASE TESTING ===")
  
  (let [edge-cases [0.1 1 5 10 45 90 135 170 175 179 179.9]]
    (doseq [angle edge-cases]
      (let [result (test-roundtrip-conversion angle mp mHe)]
        (when-not (:success? result)
          (println (format "❌ FAILED: Lab %6.2f° -> CM %6.2f° -> Lab %6.2f° | Diff: %8.5f°"
                           (:original-lab-deg result)
                           (:cm-deg result)
                           (:back-to-lab-deg result)
                           (:difference-deg result)))))))
  
  (println)
  (println "=== MASS RATIO TESTING ===")
  
  (let [mass-ratios [[938.272 3727.379]  ; p + ⁴He (0.25)
                     [938.272 938.272]   ; p + p (1.0)
                     [3727.379 938.272]  ; ⁴He + p (4.0)
                     [938.272 18756.0]]] ; p + heavy (0.05)
    (doseq [[m1 m2] mass-ratios]
      (let [ratio (/ m1 (+ m1 m2))
            result (test-roundtrip-conversion 90 m1 m2)]
        (println (format "m1/m2 = %5.3f: Lab 90° -> CM %6.2f° -> Lab %6.2f° | %s"
                         ratio
                         (:cm-deg result)
                         (:back-to-lab-deg result)
                         (if (:success? result) "✅ PASS" "❌ FAIL"))))))
  
  (println)
  (println "=== TEST COMPLETE ==="))
