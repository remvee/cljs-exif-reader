(ns remworks.exif-reader
  "Extract information from TIFF and JPEG images."
  #?(:cljs (:require-macros [remworks.cljs-workaround-macros :refer [with-open]])
     :clj (:import java.util.Date))
  (:require [remworks.data-view :refer [data-view] :as data-view]))

(def ^:private tag-names
  {nil   {0x0100 :image-width
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
   :gps  {0x0000 :gps-version-id
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

(defn- tag-max-counts [k]
  (get {:image-width                 1
        :image-length                1
        :bits-per-sample             3
        :compression                 1
        :photometric-interpretation  1
        :orientation                 1
        :samples-per-pixel           3
        :rows-per-strip              1
        :x-resolution                1
        :y-resolution                1
        :planar-configuration        1
        :resolution-unit             1
        :transfer-function           (* 3 256)
        :date-time                   24
        :white-point                 2
        :primary-chromaticities      6
        :ycb-cr-coefficients         3
        :ycb-cr-sub-sampling         2
        :ycb-cr-positioning          1
        :reference-black-white       6
        :exposure-time               1
        :f-number                    1
        :exposure-program            1
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
        :flashpix-version            4
        :color-space                 1
        :pixel-x-dimension           1
        :pixel-y-dimension           1
        :related-sound-file          13
        :flash-energy                1
        :focal-plane-x-resolution    1
        :focal-plane-y-resolution    1
        :focal-plane-resolution-unit 1
        :subject-location            2
        :exposure-index              1
        :sensing-method              1
        :file-source                 1
        :scene-type                  1
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
        :gps-status                  2
        :gps-measure-mode            2
        :gps-dop                     1
        :gps-speed-ref               2
        :gps-speed                   1
        :gps-track-ref               2
        :gps-track                   1
        :gps-img-direction-ref       2
        :gps-img-direction           1
        :gps-dest-latitude-ref       2
        :gps-dest-latitude           3
        :gps-dest-longitude-ref      2
        :gps-dest-longitude          3
        :gps-dest-bearing-ref        2
        :gps-dest-bearing            1
        :gps-dest-distance-ref       2
        :gps-dest-distance           1
        :gps-date-stamp              11
        :gps-differential            1}
       k
       tag-max-count))

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

(defn- to-rational [n d]
  (when #?(:cljs true
           :clj (and (instance? Number n)
                     (instance? Number d)))
    (if (zero? d) ##Inf (Rational. n d))))

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
  (when (instance? Rational v)
    (/ (Math/round (* (Math/pow 1.4142 (to-num v)) 10)) 10)))

(defn to-degrees [[d m s]]
  (when (and (instance? Rational d) (instance? Rational m) (instance? Rational s))
    (Degrees. d m s)))

(defn to-inst [v]
  (when-let [[_ year mon day hour min sec]
             (re-find #"(\d{4}):(\d\d):(\d\d) (\d\d):(\d\d):(\d\d)" v)]
    #?(:cljs (js/Date. (str year "-" mon "-" day "T" hour ":" min ":" sec))
       :clj (Date. (- (Long/parseLong year) 1900)
                   (dec (Long/parseLong mon))
                   (Long/parseLong day)
                   (Long/parseLong hour)
                   (Long/parseLong min)
                   (Long/parseLong sec)))))

(defn to-shutter-speed-value [v]
  (when (instance? Rational v)
    (to-rational 1 (Math/round (Math/pow 2 (to-num v))))))

(def ^:private tag-adapters
  {:aperture-value      to-aperture-value
   :date-time           to-inst
   :date-time-digitized to-inst
   :date-time-original  to-inst
   :gps-dest-latitude   to-degrees
   :gps-dest-longitude  to-degrees
   :gps-latitude        to-degrees
   :gps-longitude       to-degrees
   :shutter-speed-value to-shutter-speed-value})

(defn- to-str
  ([data offset length]
   (->> (range offset (min (data-view/getLength data)
                           (+ offset length)))
        (map #(data-view/getUint8 data %))
        (take-while (partial not= 0))
        (map char)
        (apply str)))
  ([data]
   (to-str data 0 (data-view/getLength data))))

(defn- ifd [data offset le group]
  (when-let [tag (get-in tag-names [group (data-view/getUint16 data offset le)])]
    (let [type        (data-view/getUint16 data (+ 2 offset) le)
          count       (min (tag-max-counts tag)
                           (data-view/getUint32 data (+ 4 offset) le))
          length      (or (type-lengths type) 1)
          offset      (if (> (* count length) 4)
                        (data-view/getUint32 data (+ 8 offset) le)
                        (+ offset 8))
          tag-adapter (or (tag-adapters tag) identity)]
      (when-let [type-adapter
                 (case type
                   1  #(data-view/getUint8 data (+ offset %))
                   2  #(to-str data offset (* count length))
                   3  #(data-view/getUint16 data (+ offset %) le)
                   4  #(data-view/getUint32 data (+ offset %) le)
                   5  #(to-rational (data-view/getUint32 data (+ offset %) le)
                                    (data-view/getUint32 data (+ offset % 4) le))
                   6  #(data-view/getInt8 data (+ offset %))
                   8  #(data-view/getInt16 data (+ offset %) le)
                   9  #(data-view/getInt32 data (+ offset %) le)
                   10 #(to-rational (data-view/getInt32 data (+ offset %) le)
                                    (data-view/getInt32 data (+ offset % 4) le))
                   nil)]
        {:tag   tag
         :value (tag-adapter
                 (if (= 2 type)
                   (type-adapter)
                   (if (> count 1)
                     (vec (map type-adapter (range 0 (* count length) length)))
                     (type-adapter 0))))}))))

(defn- ifds [data offset le & {:keys [group seen-offsets] :or {seen-offsets #{}}}]
  (let [offset      (or offset (data-view/getUint32 data 4 le))
        num         (data-view/getUint16 data offset le)
        next-offset (data-view/getUint32 data (+ offset 2 (* num 12)) le)
        res         (for [offset (map #(+ offset 2 (* 12 %)) (range num))]
                      (let [ifd         (ifd data offset le group)
                            tag         (:tag ifd)
                            next-offset (:value ifd)]
                        (if (and tag (tag-names tag)
                                 (not (seen-offsets next-offset)))
                          (ifds data (:value ifd) le
                                :group tag
                                :seen-offsets (conj seen-offsets (:value ifd)))
                          ifd)))
        res         (->> res
                         flatten
                         (filter identity))]
    (if (and next-offset
             (> next-offset 0)
             (< next-offset (data-view/getLength data))
             (not (seen-offsets next-offset)))
      (concat res (ifds data next-offset le
                        :group group
                        :seen-offsets (conj seen-offsets next-offset)))
      res)))

(defn from-tiff
  "Reading TIFF image information from a byte array."
  [data]
  (with-open [data (data-view data)]
    (when (and data
               (or (and (= "MM" (to-str data 0 2)) (= 42 (data-view/getUint16 data 2 false)))
                   (and (= "II" (to-str data 0 2)) (= 42 (data-view/getUint16 data 2 true)))))
      (reduce (fn [res {:keys [tag value]}] (assoc res tag value))
              {}
              (ifds data nil (= "II" (to-str data 0 2)))))))

(def ^:private sof-marker? #{0xFFC0 0xFFC1 0xFFC2 0xFFC3
                             0xFFC5 0xFFC6 0xFFC7
                             0xFFC9 0xFFCA 0xFFCB
                             0xFFCD 0xFFCE 0xFFCF})

(defn from-jpeg
  "Reading JPEG image information from a byte array."
  [data]
  (with-open [data (data-view data)]
    (when (= 0xFFD8 (data-view/getUint16 data 0 false)) ; expect a SOI marker
      (let [length (data-view/getLength data)
            data
            (loop [offset 2, props nil]
              (if (< (+ offset 2) length)
                (let [marker       (data-view/getUint16 data offset false)
                      frame-length (data-view/getUint16 data (+ offset 2) false)
                      next-offset  (+ offset frame-length 2)]
                  (cond
                    ;; frame has abnormal length; giving up and return what we collected
                    (or (< frame-length 2)
                        (< length next-offset))
                    props

                    ;; SOF
                    (sof-marker? marker)
                    (recur next-offset
                           (update props :jpeg assoc
                                   :bits (data-view/getUint8 data (+ offset 4))
                                   :height (data-view/getUint16 data (+ offset 5) false)
                                   :width (data-view/getUint16 data (+ offset 7) false)))

                    ;; COM
                    (= 0xFFFE marker)
                    (let [frame-data (data-view/slice data (+ offset 4) (- frame-length 2))]
                      (recur next-offset
                             (update-in props [:jpeg :comments] (fnil conj []) (to-str frame-data))))

                    ;; APP1
                    (= 0xFFE1 marker)
                    (let [frame-data (data-view/slice data (+ offset 4) (- frame-length 2))]
                      (recur next-offset
                             (if (and (not (:exif props)) (= "Exif" (to-str frame-data 0 4)))
                               (assoc props :exif
                                      (data-view/slice frame-data 6 (- (data-view/getLength frame-data) 6)))
                               props)))

                    ;; EOI SOS; nothing interesting beyond this point
                    (#{0xFFD9 0xFFDA} marker)
                    props

                    ;; some other frame; skip it
                    (<= 0xFF00 marker 0xFFFF)
                    (recur next-offset props)

                    :else ;; not a marker; giving up and return what we collected
                    props))

                ;; didn't see EOI or SOS return what we collected
                props))]

        (when data
          (into (select-keys data [:jpeg])
                (when-let [exif (:exif data)]
                  (from-tiff exif))))))))
