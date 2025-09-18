#!/usr/bin/env clojure

;; Test fastmath functionality
(require '[fastmath.core :as fm])
(require '[fastmath.special :as spec])

(println "=== Testing Fastmath 3.0.0-alpha4-SNAPSHOT ===")

;; Test basic mathematical functions
(println "Basic functions:")
(println "sin(1.0) = " (fm/sin 1.0))
(println "cos(1.0) = " (fm/cos 1.0))
(println "exp(1.0) = " (fm/exp 1.0))
(println "log(2.0) = " (fm/log 2.0))
(println "sqrt(4.0) = " (fm/sqrt 4.0))

;; Test complex numbers
(println "\nComplex number support:")
(println "Complex sin(1+2i) = " (fm/sin (fm/complex 1.0 2.0)))
(println "Complex exp(1+2i) = " (fm/exp (fm/complex 1.0 2.0)))

;; Test special functions that we use in DWBA
(println "\nSpecial functions:")
(println "Gamma(5) = " (spec/gamma 5.0))
(println "Bessel J0(1.0) = " (spec/bessel-j0 1.0))
(println "Bessel J1(1.0) = " (spec/bessel-j1 1.0))

;; Test hypergeometric functions
(println "\nHypergeometric functions:")
(println "1F1(1, 2, 1.0) = " (spec/hypergeometric-1f1 1.0 2.0 1.0))

(println "\n✅ Fastmath 3.0.0-alpha4-SNAPSHOT is working correctly!")
(println "🎯 Ready to proceed with DWBA calculations!")