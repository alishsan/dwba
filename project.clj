(defproject dwba "0.1.0-SNAPSHOT"
  :global-vars {*warn-on-reflection* false}
  :description "DWBA code for nuclear reactions"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [generateme/fastmath "3.0.0-alpha4-SNAPSHOT" :exclusions [com.github.haifengl/smile-mkl]]
                 [cljplot "0.0.4-SNAPSHOT"]]

  :repl-options {:init-ns dwba.core})
