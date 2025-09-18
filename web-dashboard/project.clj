(defproject dwba-web "0.1.0-SNAPSHOT"
  :description "DWBA Web Dashboard - Interactive nuclear physics calculations"
  :url "https://github.com/alishsan/dwba"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.13"]
                 [org.clojure/data.json "2.4.0"]]
  :main dwba-web.simple-core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["src" "../../src"]}}
  :resource-paths ["resources" "public"]
  :jvm-opts ["-Xmx2g"])
