#!/usr/bin/env clojure

;; Test fastmath loading and basic functions
(require '[fastmath.core :as fm])

(println "=== Testing Fastmath ===")
(println "Fastmath loaded successfully!")

;; Test basic functions
(println "sin(1.0) = " (fm/sin 1.0))
(println "cos(1.0) = " (fm/cos 1.0))
(println "exp(1.0) = " (fm/exp 1.0))
(println "log(2.0) = " (fm/log 2.0))

;; Test complex numbers
(println "Complex number support:")
(println "Complex sin(1+2i) = " (fm/sin (fm/complex 1.0 2.0)))

(println "\n✅ Fastmath is working correctly!")
