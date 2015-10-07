(ns remworks.exif-reader-test
  (:require-macros [cljs.test :refer [is deftest]])
  (:require [cljs.test :as t]
            [cljs.nodejs :as nodejs]
            [remworks.exif-reader :as exif-reader]))

(def fs (nodejs/require "fs"))

(defn data-from-file [filename]
  (.-buffer (js/Uint8Array. (.readFileSync fs filename))))

(deftest basic
  (let [exif (-> "resources/test/exif.jpg" data-from-file exif-reader/from-jpeg)]
    (is (= "Canon PowerShot G3" (:model exif)))))

(deftest gps
  (let [exif (-> "resources/test/gps.tif" data-from-file exif-reader/from-tiff)]
    (is (= "6°47'19\"" (str (:gps-longitude exif))))
    (is (= "53°33'19\"" (str (:gps-latitude exif))))))
