(defproject org.signaut/ring-jetty8-adapter "1.2.0"
            :description "Ring Jetty adapter."
            :url "https://github.com/ring-clojure/ring"
            :dependencies [[ring/ring-core "1.2.0"]
                           [ring/ring-servlet "1.2.0"]
                           [org.eclipse.jetty/jetty-server "9.0.5.v20130815"]]
            :plugins [[lein-clojars "0.9.1"]]
            :profiles
            {:dev {:dependencies [[clj-http "0.7.6"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})
