(ns remworks.exif-reader
  "Extract information from TIFF and JPEG images."
  (:require [remworks.data-view :refer [data-view] :as data-view]))

(def ^:private tag-names
  {nil {0x0100 :image-width
        0x0101 :image-length
        0x0102 :bits-per-sample
        0x0103 :compression
        0x0106 :photometric-interpretation
        0x010E :image-description
        0x010F :make
        0x0110 :model
        0x0111 :strip-offsets
        0x0112 :orientation
        0x0115 :samples-per-pixel
        0x0116 :rows-per-strip
        0x0117 :strip-byte-counts
        0x011A :x-resolution
        0x011B :y-resolution
        0x011C :planar-configuration
        0x0128 :resolution-unit
        0x012D :transfer-function
        0x0131 :software
        0x0132 :date-time
        0x013B :artist
        0x013E :white-point
        0x013F :primary-chromaticities
        0x0211 :ycb-cr-coefficients
        0x0212 :ycb-cr-sub-sampling
        0x0213 :ycb-cr-positioning
        0x0214 :reference-black-white
        0x02BC :xmp
        0x8298 :copyright
        0x8769 :exif
        0x8825 :gps}
   :exif {0x829A :exposure-time
          0x829D :f-number
          0x8822 :exposure-program
          0x8824 :spectral-sensitivity
          0x8827 :iso-speed-ratings
          0x8828 :oecf
          0x9000 :exif-version
          0x9003 :date-time-original
          0x9004 :date-time-digitized
          0x9101 :components-configuration
          0x9102 :compressed-bits-per-pixel
          0x9201 :shutter-speed-value
          0x9202 :aperture-value
          0x9203 :brightness-value
          0x9204 :exposure-bias-value
          0x9205 :max-aperture-value
          0x9206 :subject-distance
          0x9207 :metering-mode
          0x9208 :light-source
          0x9209 :flash
          0x920A :focal-length
          0x9214 :subject-area
          0x927C :maker-note
          0x9286 :user-comment
          0x9290 :subsec-time
          0x9291 :subsec-time-orginal
          0x9292 :subsec-time-digitized
          0xA000 :flashpix-version
          0xA001 :color-space
          0xA002 :pixel-x-dimension
          0xA003 :pixel-y-dimension
          0xA004 :related-sound-file
          0xA20B :flash-energy
          0xA20C :spatial-frequency-response
          0xA20E :focal-plane-x-resolution
          0xA20F :focal-plane-y-resolution
          0xA210 :focal-plane-resolution-unit
          0xA214 :subject-location
          0xA215 :exposure-index
          0xA217 :sensing-method
          0xA300 :file-source
          0xA301 :scene-type
          0xA302 :cfa-pattern
          0xA401 :custom-rendered
          0xA402 :exposure-mode
          0xA403 :white-balance
          0xA404 :digital-zoom-ratio
          0xA405 :focal-length-in-35mm-film
          0xA406 :scene-capture-type
          0xA407 :gain-control
          0xA408 :contrast
          0xA409 :saturation
          0xA40A :sharpness
          0xA40B :device-setting-description
          0xA40C :subject-distance-range
          0xA420 :image-unique-id}
   :gps {0x0000 :gps-version-id
         0x0001 :gps-latitude-ref
         0x0002 :gps-latitude
         0x0003 :gps-longitude-ref
         0x0004 :gps-longitude
         0x0005 :gps-altitude-ref
         0x0006 :gps-altitude
         0x0007 :gps-time-stamp
         0x0008 :gps-satellites
         0x0009 :gps-status
         0x000a :gps-measure-mode
         0x000b :gps-dop
         0x000c :gps-speed-ref
         0x000d :gps-speed
         0x000e :gps-track-ref
         0x000f :gps-track
         0x0010 :gps-img-direction-ref
         0x0011 :gps-img-direction
         0x0012 :gps-map-datum
         0x0013 :gps-dest-latitude-ref
         0x0014 :gps-dest-latitude
         0x0015 :gps-dest-longitude-ref
         0x0016 :gps-dest-longitude
         0x0017 :gps-dest-bearing-ref
         0x0018 :gps-dest-bearing
         0x0019 :gps-dest-distance-ref
         0x001a :gps-dest-distance
         0x001b :gps-processing-method
         0x001c :gps-area-information
         0x001d :gps-date-stamp
         0x001e :gps-differential
         0x001f :gps-h-positioning-error}})

(def ^:private tag-max-count (* 8 1024))

(def ^:private tag-max-counts
  {:image-width                 1
   :image-length                1
   :bits-per-sample             3
   :compression                 1
   :photometric-interpretation  1
   :image-description           tag-max-count
   :make                        tag-max-count
   :model                       tag-max-count
   :strip-offsets               tag-max-count
   :orientation                 1
   :samples-per-pixel           3
   :rows-per-strip              1
   :strip-byte-counts           tag-max-count
   :x-resolution                1
   :y-resolution                1
   :planar-configuration        1
   :resolution-unit             1
   :transfer-function           (* 3 256)
   :software                    tag-max-count
   :date-time                   24
   :artist                      tag-max-count
   :white-point                 2
   :primary-chromaticities      6
   :ycb-cr-coefficients         3
   :ycb-cr-sub-sampling         2
   :ycb-cr-positioning          1
   :reference-black-white       6
   :xmp                         tag-max-count
   :copyright                   tag-max-count
   :exif                        tag-max-count
   :gps                         tag-max-count
   :exposure-time               1
   :f-number                    1
   :exposure-program            1
   :spectral-sensitivity        tag-max-count
   :iso-speed-ratings           tag-max-count
   :oecf                        tag-max-count
   :exif-version                4
   :date-time-original          24
   :date-time-digitized         24
   :components-configuration    4
   :compressed-bits-per-pixel   1
   :shutter-speed-value         1
   :aperture-value              1
   :brightness-value            1
   :exposure-bias-value         1
   :max-aperture-value          1
   :subject-distance            1
   :metering-mode               1
   :light-source                1
   :flash                       1
   :focal-length                1
   :subject-area                4
   :maker-note                  tag-max-count
   :user-comment                tag-max-count
   :subsec-time                 tag-max-count
   :subsec-time-orginal         tag-max-count
   :subsec-time-digitized       tag-max-count
   :flashpix-version            4
   :color-space                 1
   :pixel-x-dimension           1
   :pixel-y-dimension           1
   :related-sound-file          13
   :flash-energy                1
   :spatial-frequency-response  tag-max-count
   :focal-plane-x-resolution    1
   :focal-plane-y-resolution    1
   :focal-plane-resolution-unit 1
   :subject-location            2
   :exposure-index              1
   :sensing-method              1
   :file-source                 1
   :scene-type                  1
   :cfa-pattern                 tag-max-count
   :custom-rendered             1
   :exposure-mode               1
   :white-balance               1
   :digital-zoom-ratio          1
   :focal-length-in-35mm-film   1
   :scene-capture-type          1
   :gain-control                1
   :contrast                    1
   :saturation                  1
   :sharpness                   1
   :device-setting-description  tag-max-count
   :subject-distance-range      1
   :image-unique-id             33
   :gps-version-id              4
   :gps-latitude-ref            2
   :gps-latitude                3
   :gps-longitude-ref           2
   :gps-longitude               3
   :gps-altitude-ref            2
   :gps-altitude                1
   :gps-time-stamp              3
   :gps-satellites              tag-max-count
   :gps-status                  2
   :gps-measure-mode            2
   :gps-dop                     1
   :gps-speed-ref               2
   :gps-speed                   1
   :gps-track-ref               2
   :gps-track                   1
   :gps-img-direction-ref       2
   :gps-img-direction           1
   :gps-map-datum               tag-max-count
   :gps-dest-latitude-ref       2
   :gps-dest-latitude           3
   :gps-dest-longitude-ref      2
   :gps-dest-longitude          3
   :gps-dest-bearing-ref        2
   :gps-dest-bearing            1
   :gps-dest-distance-ref       2
   :gps-dest-distance           1
   :gps-processing-method       tag-max-count
   :gps-area-information        tag-max-count
   :gps-date-stamp              11
   :gps-differential            1})

(def ^:private type-lengths
  {3 2, 4 4, 5 8, 8 2, 9 4, 10 8})

(defprotocol IValue
  (toNumber [_]))

(defrecord Rational [numerator denominator]
  IValue
  (toNumber [_] (/ numerator denominator))
  Object
  (toString [_] (str numerator "/" denominator)))

(defn- to-num [v]
  (if (satisfies? IValue v)
    (toNumber v)
    #?(:cljs (js/parseFloat v)
       :clj (Long/parseLong v))))

(defrecord Degrees [deg min sec]
  IValue
  (toNumber [_]
    (+ (to-num deg)
       (/ (to-num min) 60)
       (/ (to-num sec) (* 60 60))))
  Object
  (toString [this]
    (let [deg (toNumber this)
          min (* (- deg (int deg)) 60)
          sec (* (- min (int min)) 60)]
      (str (int deg) "°"
           (int min) "'"
           (int sec) "\""))))

(defn to-aperture-value [v]
  (/ (Math/round (* (Math/pow 1.4142 (to-num v)) 10)) 10))

(defn to-degrees [[d m s]]
  (Degrees. d m s))

(defn to-inst [v]
  (let [[_ year mon day hour min sec]
        (re-find #"^(\d{4}):(\d\d):(\d\d) (\d\d):(\d\d):(\d\d)$" v)]
    #?(:cljs (js/Date. (str year "-" mon "-" day "T" hour ":" min ":" sec))
       :clj (java.util.Date. (- (Long/parseLong year) 1900)
                             (dec (Long/parseLong mon))
                             (Long/parseLong day)
                             (Long/parseLong hour)
                             (Long/parseLong min)
                             (Long/parseLong sec)))))

(defn to-shutter-speed-value [v]
  (Rational. 1 (int (Math/pow 2 (to-num v)))))

(def ^:private tag-adapters
  {:aperture-value to-aperture-value
   :date-time to-inst
   :date-time-digitized to-inst
   :date-time-original to-inst
   :gps-dest-latitude to-degrees
   :gps-dest-longitude to-degrees
   :gps-latitude to-degrees
   :gps-longitude to-degrees
   :shutter-speed-value to-shutter-speed-value})

(defn- to-str [data offset length]
  (->> (range offset (+ offset length))
       (map #(data-view/getUint8 data %))
       (take-while (partial not= 0))
       (map char)
       (apply str)))

(defn- ifd [data offset le group]
  (when-let [tag (get-in tag-names [group (data-view/getUint16 data offset le)])]
    (let [type (data-view/getUint16 data (+ 2 offset) le)
          count (min (tag-max-counts tag)
                     (data-view/getUint32 data (+ 4 offset) le))
          length (or (type-lengths type) 1)
          offset (if (> (* count length) 4)
                   (data-view/getUint32 data (+ 8 offset) le)
                   (+ offset 8))
          tag-adapter (or (tag-adapters tag) identity)]
      (when-let [type-adapter
                 (case type
                   1 #(data-view/getUint8 data (+ offset %))
                   2 #(to-str data offset (* count length))
                   3 #(data-view/getUint16 data (+ offset %) le)
                   4 #(data-view/getUint32 data (+ offset %) le)
                   5 #(Rational. (data-view/getUint32 data (+ offset %) le)
                                 (data-view/getUint32 data (+ offset % 4) le))
                   6 #(data-view/getInt8 data (+ offset %))
                   8 #(data-view/getInt16 data (+ offset %) le)
                   9 #(data-view/getInt32 data (+ offset %) le)
                   10 #(Rational. (data-view/getInt32 data (+ offset %) le)
                                  (data-view/getInt32 data (+ offset % 4) le))
                   nil)]
        {:tag   tag
         :value (tag-adapter
                 (if (= 2 type)
                   (type-adapter)
                   (if (> count 1)
                     (vec (map type-adapter (range 0 (* count length) length)))
                     (type-adapter 0))))}))))

(defn- ifds [data offset le & [group]]
  (let [offset (or offset (data-view/getUint32 data 4 le))
        num (data-view/getUint16 data offset le)
        next-offset (data-view/getUint32 data (+ offset 2 (* num 12)) le)
        res (for [offset (map #(+ offset 2 (* 12 %)) (range num))]
              (let [ifd (ifd data offset le group)
                    tag (:tag ifd)]
                (if (and tag (tag-names tag))
                  (ifds data (:value ifd) le tag)
                  ifd)))
        res (->> res
                 flatten
                 (filter identity))]
    (if (and next-offset
             (> next-offset 0)
             (< next-offset (data-view/getLength data)))
      (concat res (ifds data next-offset le group))
      res)))

(defn from-tiff
  "Reading TIFF image information from a byte array."
  [data]
  (let [data (data-view data)]
    (when (and data
               (or (and (= "MM" (to-str data 0 2)) (= 42 (data-view/getUint16 data 2 false)))
                   (and (= "II" (to-str data 0 2)) (= 42 (data-view/getUint16 data 2 true)))))
      (reduce (fn [res {:keys [tag value]}] (assoc res tag value))
              {}
              (ifds data nil (= "II" (to-str data 0 2)))))))

(defn from-jpeg
  "Reading JPEG image information from a byte array."
  [data]
  (let [data (data-view data)]
    (when (= 0xFFD8 (data-view/getUint16 data 0 false))
      (let [length (data-view/getLength data)
            data (loop [offset 2]
                   (when (< offset length)
                     (if (= 0xFF (data-view/getUint8 data offset))
                       (let [marker (data-view/getUint8 data (+ 1 offset))]
                         (cond
                           (= 0xE1 marker)  ; APP1
                           (if (= "Exif" (to-str data (+ 4 offset) 4))
                             (data-view/slice data
                                              (+ 4 6 offset)
                                              (data-view/getUint16 data (+ 2 offset) false))
                             (recur (+ 4 offset (data-view/getUint16 data (+ 2 offset) false))))

                           (#{0xD9 0xDA} marker) ; EOI SOS
                           nil

                           :else
                           (recur (inc offset))))
                       (recur (inc offset)))))]
        (when data
          (from-tiff data))))))
