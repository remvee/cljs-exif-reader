(defproject remworks/cljs-exif-reader "0.1.0"
  :description "Extract information from TIFF and JPEG images."

  :url "https://github.com/remvee/cljs-exif-reader"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :clojurescript? true

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]]

  :plugins [[lein-cljsbuild "1.1.0"]]

  :cljsbuild {:builds {:dev {:source-paths ["src" "dev"]
                             :compiler {:output-dir "target/cljs"
                                        :output-to "target/cljs/exif-reader.js"}}}})
