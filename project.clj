(defproject iterargs "0.8.0"
  :description "A program that facilitates iterative arguments"
  :url "https://github.com/mat5n/iterargs"
  :license {:name "Eclipse Public License, Version 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  
  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.879"]
                 [org.clojure/core.async "1.3.618"]

                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [reagent "1.1.0"]

                 [medley "1.3.0"]
                 [hickory "0.7.1"]]

  :source-paths ["src"]

  :aliases {"fig"      ["trampoline" "run" "-m" "figwheel.main"]
            "fig:dev"  ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:prod" ["run" "-m" "figwheel.main" "-bo" "prod"]}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.13"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]
                                  [org.clojure/test.check "1.1.0"]]}})
