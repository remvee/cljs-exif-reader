(ns remworks.exif-reader-test
  #?(:cljs (:require-macros [cljs.test :refer [is deftest]]))
  #?(:cljs (:require [cljs.test :as t]
                     [cljs.nodejs :as nodejs]
                     [remworks.exif-reader :as sut])
     :clj (:require [clojure.java.io :as io]
                    [clojure.test :refer :all]
                    [remworks.exif-reader :as sut])))

#?(:cljs
   (do
     (def fs (nodejs/require "fs"))
     (defn data-from-file [filename]
       (->> filename (.readFileSync fs) (js/Uint8Array.) .-buffer)))
   :clj
   (defn data-from-file [filename]
     (java.io.File. filename)))

(deftest basic
  (let [exif (-> "resources/test/exif.jpg" data-from-file sut/from-jpeg)]
    (is (= "Canon PowerShot G3" (:model exif)))))

(deftest gps
  (let [exif (-> "resources/test/gps.tif" data-from-file sut/from-tiff)]
    (is (= "6°47'19\"" (str (:gps-longitude exif))))
    (is (= "53°33'19\"" (str (:gps-latitude exif))))))

(deftest malformed
  (let [exif (-> "resources/test/malformed.jpg" data-from-file sut/from-jpeg)]
    (is (= "DMC-FZ1000" (:model exif)))))
