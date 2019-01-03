(defproject remworks/cljs-exif-reader "0.3.0-SNAPSHOT"
  :description "Extract information from TIFF and JPEG images."

  :url "https://github.com/remvee/cljs-exif-reader"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :clojurescript? true

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.10"]]

  :doo {:build "test"
        :alias {:default [:node]}}

  :aliases {"test" ["doo" "node" "test" "once"]}

  :cljsbuild {:builds {:dev  {:source-paths ["src" "dev"]
                              :compiler     {:output-dir "target/cljs"
                                             :output-to  "target/cljs/exif-reader.js"}}
                       :test {:source-paths ["src" "test"]
                              :compiler     {:optimizations :none
                                             :output-dir    "target/test"
                                             :output-to     "target/test/exif-reader-test.js"
                                             :target        :nodejs
                                             :main          remworks.runner
                                             :hashbang      false}}}})
