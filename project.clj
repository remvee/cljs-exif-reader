(defproject remworks/cljs-exif-reader "0.4.1"
  :description "Extract information from TIFF and JPEG images."

  :url "https://github.com/remvee/cljs-exif-reader"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :clojurescript? true

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.11"]]

  :doo {:build "test"
        :alias {:default [:node]}}

  :aliases {"test-clj"  "test"
            "test-cljs" ["doo" "node" "test" "once"]
            "test-all"  ["do" ["test-clj"] ["test-cljs"]]}

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
