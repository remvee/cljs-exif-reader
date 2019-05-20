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
    (is (= "Canon PowerShot G3" (:model exif)))
    (is (= 4.5 (double (:aperture-value exif))))
    (is (= "1/1244" (str (:shutter-speed-value exif))))))

(deftest gps
  (let [exif (-> "resources/test/gps.tif" data-from-file sut/from-tiff)]
    (is (= "6°47'19\"" (str (:gps-longitude exif))))
    (is (= "53°33'19\"" (str (:gps-latitude exif))))))

(deftest malformed
  (doseq [name ["malformed.jpg"
                "multiple-app1.jpg"]]
    (let [m (-> (str "resources/test/" name) data-from-file sut/from-jpeg)]
      (is (identity m) (str "expect " name " to yield some values"))))
  (doseq [name ["apple-aperture-1.5.exif"
                "bad-shutter_speed_value.exif"
                "endless-loop.exif"
                "negative-exposure-bias-value.exif"
                "out-of-range.exif"
                "weird_date.exif"]]
    (let [m (-> (str "resources/test/" name) data-from-file sut/from-tiff)]
      (is (identity m) (str "expect " name " to yield some values")))))