(ns cljs.user
  (:require [clojure.string :refer [join]]
            [remworks.exif-reader :as exif-reader]))

(enable-console-print!)

(defn ^:export setup []
  (let [el (.getElementById js/document "file-input")
        out (.getElementById js/document "output")]
    (.addEventListener
     el "change"
     (fn []
       (let [file (-> el .-files (aget 0))
             reader (js/FileReader.)]
         (.addEventListener
          reader "loadend"
          (fn []
            (let [data (.-result reader)
                  exif (or (exif-reader/from-jpeg data)
                           (exif-reader/from-tiff data))]
              (set! (.-innerHTML out)
                    (join "\n"
                          (map (fn [[k v]] (str k "\n  " v))
                               (sort-by (comp name first)
                                        exif)))))))
         (.readAsArrayBuffer reader file))))))
