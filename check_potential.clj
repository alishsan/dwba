;; Check if boundary is in asymptotic region

(require '[functions :refer :all])

(def v0 46.23)
(def rad 2.0)
(def diff 0.5)
(def e 2.0)

(println "=== CHECKING POTENTIAL AT BOUNDARY ===\n")

;; Check potential values
(doseq [r [5.0 8.0 10.0 12.0 15.0 20.0 30.0]]
  (let [v (woods-saxon-numerov r v0 rad diff)
        v-over-e (/ v e)]
    (println (format "r = %5.1f fm: V = %10.6f MeV, V/E = %8.4f" r v v-over-e))
    (if (< (Math/abs v-over-e) 0.01)
      (println "         ✓ Potential is < 1% of energy (asymptotic region)")
      (println "         ✗ Potential is still significant!"))))

(println "")
(println "For asymptotic matching, we need V << E")
(println "Typically want |V| < 0.01*E or |V| < 0.1 MeV")

