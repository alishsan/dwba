(ns dwba.transfer-amplitude-test
  "Tests for transfer-amplitude-post with complex wavefunctions."
  (:require [clojure.test :refer :all]
            [dwba.transfer :as t]
            [complex :as c :refer [re im mag complex-cartesian complex-conjugate]]))

(def ws-params [50.0 2.0 0.6])  ; V0=50 MeV, R0=2.0 fm, a0=0.6 fm
(def r-max 20.0)
(def h 0.01)
(def mass-factor 0.048)

(deftest transfer-amplitude-post-real-waves-test
  "Test transfer-amplitude-post with real wavefunctions (typical case)"
  (testing "Transfer amplitude with real bound states and real distorted waves"
    (let [;; Real bound state wavefunctions
          phi-i (t/solve-bound-state-numerov -15.0 1 50.0 2.0 0.6 mass-factor h r-max)
          phi-f (t/solve-bound-state-numerov -2.0 0 50.0 2.0 0.6 mass-factor h r-max)
          ;; Simple real distorted waves (plane waves for testing)
          chi-i (vec (take (count phi-i) (repeat 1.0)))
          chi-f (vec (take (count phi-f) (repeat 1.0)))
          D0 -122.4]
      (let [T-post (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h :zero-range D0)]
        (is (or (number? T-post) (c/complex? T-post)) "Should return a number or complex")
        (let [T-real (if (c/complex? T-post) (re T-post) T-post)
              T-imag (if (c/complex? T-post) (im T-post) 0.0)]
          (is (not (Double/isNaN T-real)) "Real part should not be NaN")
          (is (not (Double/isNaN T-imag)) "Imaginary part should not be NaN")
          (is (not (Double/isInfinite T-real)) "Real part should not be infinite")
          (is (not (Double/isInfinite T-imag)) "Imaginary part should not be infinite"))))))

(deftest transfer-amplitude-post-complex-chi-test
  "Test transfer-amplitude-post with complex distorted waves (typical case)"
  (testing "Transfer amplitude with real bound states and complex distorted waves"
    (let [;; Real bound state wavefunctions
          phi-i (t/solve-bound-state-numerov -15.0 1 50.0 2.0 0.6 mass-factor h r-max)
          phi-f (t/solve-bound-state-numerov -2.0 0 50.0 2.0 0.6 mass-factor h r-max)
          ;; Complex distorted waves (simulate optical potential result)
          chi-i (vec (map (fn [x] (complex-cartesian x (* 0.1 x))) 
                         (take (count phi-i) (range 0.0 1.0 0.01))))
          chi-f (vec (map (fn [x] (complex-cartesian x (* 0.1 x))) 
                         (take (count phi-f) (range 0.0 1.0 0.01))))
          D0 -122.4]
      (let [T-post (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h :zero-range D0)]
        (is (or (number? T-post) (c/complex? T-post)) "Should return a number or complex")
        (when (c/complex? T-post)
          (is (not (Double/isNaN (re T-post))) "Real part should not be NaN")
          (is (not (Double/isNaN (im T-post))) "Imaginary part should not be NaN"))))))

(deftest transfer-amplitude-post-complex-phi-test
  "Test transfer-amplitude-post with complex bound state wavefunctions"
  (testing "Transfer amplitude with complex bound states (unusual but should work)"
    (let [;; Complex bound state wavefunctions (simulated)
          phi-i-real (t/solve-bound-state-numerov -15.0 1 50.0 2.0 0.6 mass-factor h r-max)
          phi-f-real (t/solve-bound-state-numerov -2.0 0 50.0 2.0 0.6 mass-factor h r-max)
          ;; Add small imaginary parts
          phi-i (vec (map (fn [x] (complex-cartesian x (* 0.01 x))) phi-i-real))
          phi-f (vec (map (fn [x] (complex-cartesian x (* 0.01 x))) phi-f-real))
          ;; Complex distorted waves
          chi-i (vec (map (fn [x] (complex-cartesian x (* 0.1 x))) 
                         (take (count phi-i) (range 0.0 1.0 0.01))))
          chi-f (vec (map (fn [x] (complex-cartesian x (* 0.1 x))) 
                         (take (count phi-f) (range 0.0 1.0 0.01))))
          D0 -122.4]
      (let [T-post (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h :zero-range D0)]
        (is (or (number? T-post) (c/complex? T-post)) "Should return a number or complex")
        (when (c/complex? T-post)
          (is (not (Double/isNaN (re T-post))) "Real part should not be NaN")
          (is (not (Double/isNaN (im T-post))) "Imaginary part should not be NaN"))))))

(deftest transfer-amplitude-post-finite-range-test
  "Test transfer-amplitude-post with finite-range interaction"
  (testing "Transfer amplitude with finite-range Yukawa interaction"
    (let [phi-i (t/solve-bound-state-numerov -15.0 1 50.0 2.0 0.6 mass-factor h r-max)
          phi-f (t/solve-bound-state-numerov -2.0 0 50.0 2.0 0.6 mass-factor h r-max)
          chi-i (vec (take (count phi-i) (repeat 1.0)))
          chi-f (vec (take (count phi-f) (repeat 1.0)))
          finite-params {:V0 50.0 :form-factor :yukawa :range-param 0.7}]
      (let [T-post (t/transfer-amplitude-post chi-i chi-f phi-i phi-f r-max h :finite-range finite-params)]
        (is (or (number? T-post) (c/complex? T-post)) "Should return a number or complex")
        (let [T-real (if (c/complex? T-post) (re T-post) T-post)
              T-imag (if (c/complex? T-post) (im T-post) 0.0)]
          (is (not (Double/isNaN T-real)) "Real part should not be NaN")
          (is (not (Double/isNaN T-imag)) "Imaginary part should not be NaN"))))))

(deftest complex-conjugate-real-test
  "Test that complex-conjugate works correctly for real numbers"
  (testing "complex-conjugate of real number should return the same number"
    (let [x 5.0
          x-conj (complex-conjugate x)]
      (is (= x (re x-conj)) "Real part should be the same")
      (is (< (Math/abs (im x-conj)) 1e-10) "Imaginary part should be approximately zero"))))

(deftest complex-conjugate-complex-test
  "Test that complex-conjugate works correctly for complex numbers"
  (testing "complex-conjugate of complex number should flip imaginary part"
    (let [x (complex-cartesian 3.0 4.0)
          x-conj (complex-conjugate x)]
      (is (= 3.0 (re x-conj)) "Real part should be unchanged")
      (is (= -4.0 (im x-conj)) "Imaginary part should be negated"))))

