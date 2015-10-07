(ns remworks.exif-reader
  "Extract information from TIFF and JPEG images."
  (:require [remworks.data-view :refer [data-view] :as data-view]))

(def ^:private tag-names
  {0x00FE :new-subfile-type
   0x00FF :subfile-type
   0x0100 :image-width
   0x0101 :image-length
   0x0102 :bits-per-sample
   0x0103 :compression
   0x0106 :photometric-interpretation
   0x0107 :threshholding
   0x0108 :cell-width
   0x0109 :cell-length
   0x010A :fill-order
   0x010D :document-name
   0x010E :image-description
   0x010F :make
   0x0110 :model
   0x0111 :strip-offsets
   0x0112 :orientation
   0x0115 :samples-per-pixel
   0x0116 :rows-per-strip
   0x0117 :strip-byte-counts
   0x0118 :min-sample-value
   0x0119 :max-sample-value
   0x011A :x-resolution
   0x011B :y-resolution
   0x011C :planar-configuration
   0x011D :page-name
   0x011E :x-position
   0x011F :y-position
   0x0120 :free-offsets
   0x0121 :free-byte-counts
   0x0122 :gray-response-unit
   0x0123 :gray-response-curve
   0x0124 :t4-options
   0x0125 :t6-options
   0x0128 :resolution-unit
   0x012D :transfer-function
   0x0131 :software
   0x0132 :date-time
   0x013B :artist
   0x013C :host-computer
   0x013A :predictor
   0x013E :white-point
   0x013F :primary-chromaticities
   0x0140 :color-map
   0x0141 :halftone-hints
   0x0142 :tile-width
   0x0143 :tile-length
   0x0144 :tile-offsets
   0x0145 :tile-byte-counts
   0x0146 :bad-fax-lines
   0x0147 :clean-fax-data
   0x0148 :consecutive-bad-fax-lines
   0x014A :sub-ifds
   0x014C :ink-set
   0x014D :ink-names
   0x014E :number-of-inks
   0x0150 :dot-range
   0x0151 :target-printer
   0x0152 :extra-samples
   0x0156 :transfer-range
   0x0157 :clip-path
   0x0158 :x-clip-path-units
   0x0159 :y-clip-path-units
   0x015A :indexed
   0x015B :jpeg-tables
   0x015F :opi-proxy
   0x0190 :global-parameters-ifd
   0x0191 :profile-type
   0x0192 :fax-profile
   0x0193 :coding-methods
   0x0194 :version-year
   0x0195 :mode-number
   0x01B1 :decode
   0x01B2 :default-image-color
   0x0200 :jpegproc
   0x0201 :jpeg-interchange-format
   0x0202 :jpeg-interchange-format-length
   0x0203 :jpeg-restart-interval
   0x0205 :jpeg-lossless-predictors
   0x0206 :jpeg-point-transforms
   0x0207 :jpeg-q-tables
   0x0208 :jpeg-dc-tables
   0x0209 :jpeg-ac-tables
   0x0211 :ycb-cr-coefficients
   0x0212 :ycb-cr-sub-sampling
   0x0213 :ycb-cr-positioning
   0x0214 :reference-black-white
   0x022F :strip-row-counts
   0x800D :image-id
   0x87AC :image-layer
   0x8298 :copyright
   0x83BB :iptc

   ;; exif
   0x8769 :exif
   0x829A :exposure-time
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
   0xA420 :image-unique-id

   ;; gps
   0x8825 :gps
   0x0000 :gps-version-id
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
   0x001e :gps-differential})

(def ^:private type-lengths
  {3 2, 4 4, 5 8, 8 2, 9 4, 10 8})

(def ^:private nested-tags
  #{:exif :gps})

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

(defn- ifd [data offset le]
  (when-let [tag (tag-names (data-view/getUint16 data offset le))]
    (let [type (data-view/getUint16 data (+ 2 offset) le)
          count (data-view/getUint32 data (+ 4 offset) le)
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
        {:tag tag
         :value (tag-adapter
                 (if (= 2 type)
                   (type-adapter)
                   (if (> count 1)
                     (vec (map type-adapter (range 0 (* count length) length)))
                     (type-adapter 0))))}))))

(defn- ifds [data offset le]
  (let [offset (or offset (data-view/getUint32 data 4 le))
        num (data-view/getUint16 data offset le)
        next-offset (data-view/getUint32 data (+ offset 2 (* num 12)) le)
        res (for [offset (map #(+ offset 2 (* 12 %)) (range num))]
              (let [ifd (ifd data offset le)]
                (if (nested-tags (:tag ifd))
                  (ifds data (:value ifd) le)
                  ifd)))
        res (->> res
                 flatten
                 (filter identity))]
    (if (and next-offset
             (> next-offset 0)
             (< next-offset (data-view/getLength data)))
      (concat res (ifds data next-offset le))
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
                             (data-view (data-view/getBuffer data)
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