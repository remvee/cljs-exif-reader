(defproject remworks/cljs-exif-reader "0.1.0-SNAPSHOT"
  :description "Extract information from TIFF and JPEG images."

  :clojurescript? true

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3126"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :cljsbuild {:builds {:dev {:source-paths ["src" "dev"]
                             :compiler {:output-to "target/cljs/exif-reader.js"}}}})
