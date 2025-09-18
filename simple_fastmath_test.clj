;; Simple fastmath test
(require '[fastmath.core :as fm])

(println "Testing fastmath...")
(println "sin(1.0) = " (fm/sin 1.0))
(println "cos(1.0) = " (fm/cos 1.0))
(println "exp(1.0) = " (fm/exp 1.0))
(println "Fastmath is working!")
