(defproject dwba-web "0.1.0-SNAPSHOT"
  :description "DWBA Web Dashboard - Interactive nuclear physics calculations"
  :url "https://github.com/alishsan/dwba"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/clojurescript "1.11.60"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [ring/ring-json "0.5.1" :exclusions [cheshire]]
                 [cheshire "5.11.0"]
                 [ring-cors "0.1.13"]
                 [org.clojure/data.json "2.4.0"]
                 [generateme/fastmath "3.0.0-alpha4-SNAPSHOT" :exclusions [com.github.haifengl/smile-mkl]]]
  :plugins [[lein-cljsbuild "1.1.8"]]
  :main dwba-web.simple-core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["src" "../src"]}}
  :resource-paths ["resources" "public"]
  :jvm-opts ["-Xmx2g"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src-cljs"]
                :compiler {:output-to "public/app.js"
                           :output-dir "public/js/out"
                           :optimizations :none
                           :source-map true
                           :asset-path "js/out"
                           :main dwba-web.dashboard}}
               {:id "prod"
                :source-paths ["src-cljs"]
                :compiler {:output-to "public/app.js"
                           :optimizations :advanced
                           :pretty-print false
                           :main dwba-web.dashboard}}]})
