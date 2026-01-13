(require '[com.hypirion.clj-xchart :as xchart])

;; Test what functions are available
(println "Available functions in com.hypirion.clj-xchart:")
(println "Checking for common function names...")

;; Try to find the chart creation function
(try
  (println "xy-chart exists:" (resolve 'xchart/xy-chart))
  (catch Exception e (println "xy-chart not found")))

(try
  (println "chart exists:" (resolve 'xchart/chart))
  (catch Exception e (println "chart not found")))

;; List all public vars in the namespace
(println "\nAll public vars:")
(doseq [[sym var] (ns-publics 'com.hypirion.clj-xchart)]
  (println (str "  " sym)))

