(require '[dwba.transfer :as t])

;; Test the shooting method for l=0 where we know there's a sign change
(println "=== Testing Shooting Method for l=0 (1s state) ===")
(println "Known sign change between -15 and -16 MeV")
(println "")

(let [V-params [50.0 2.0 0.6]
      l 0
      n 1  ; 1s state should have 0 radial nodes
      r-max 20.0
      h 0.001
      result (t/find-bound-state-energy V-params l n r-max h)]
  (println "=== Result ===")
  (println (format "Energy: %.6f MeV" (:energy result)))
  (println (format "Boundary value: %.2e" (:boundary-value result)))
  (println (format "Nodes: %d (expected: 0)" (:nodes result)))
  (println (format "Converged: %s" (:converged? result)))
  (println "")
  
  (when (:wavefunction result)
    (let [u (:wavefunction result)
          u-max (apply max (map Math/abs u))
          u-at-5 (nth u (int (/ 5.0 h)))
          u-at-10 (nth u (int (/ 10.0 h)))
          u-at-15 (nth u (int (/ 15.0 h)))]
      (println "=== Wavefunction Properties ===")
      (println (format "Length: %d points" (count u)))
      (println (format "Max |u|: %.2e" u-max))
      (println (format "u(5 fm): %.2e" u-at-5))
      (println (format "u(10 fm): %.2e" u-at-10))
      (println (format "u(15 fm): %.2e" u-at-15))
      (println (format "u(20 fm): %.2e" (last u))))))

