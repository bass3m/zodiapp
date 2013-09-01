(defproject zodiapp "0.1.0-SNAPSHOT"
  :description "Don't push your luck"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src-clj"]
  :min-lein-version "2.0.0"
  :main zodiapp.core
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.0"]
                             [lein-ancient "0.4.4"]]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.6"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [org.clojure/clojurescript "0.0-1853"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]
                 [com.novemberain/monger "1.5.0"]
                 [clj-time "0.6.0"]]
  :plugins  [[lein-cljsbuild "0.3.2"]]
  :cljsbuild  {
    :builds [{
          :source-paths ["src-cljs"]
          :compiler {
            :output-to "resources/public/js/main.js"
            :optimizations :whitespace
            :pretty-print true}}]}
  :repl-options {
  :prompt (fn [ns] (str "\u001B[35m[\u001B[34m" ns "\u001B[35m]\u001B[33mclj-λ)\u001B[m " ))
  :welcome (println "Welcome to Clojure!")})
