(ns remworks.exif-reader
  "Extract information from TIFF and JPEG images.")

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
   0x8769 :exif

   ;; exif
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
   0xA420 :image-unique-id})

(def ^:private type-lengths
  {3 2, 4 4, 5 8, 8 2, 9 4, 10 8})

(defn- to-str [data offset length]
  (->> (range offset (+ offset length))
       (map #(.getUint8 data %))
       (map char)
       (apply str)))

(defn- ifd [data offset le]
  (when-let [tag (tag-names (.getUint16 data offset le))]
    (let [type (.getUint16 data (+ 2 offset) le)
          count (.getUint32 data (+ 4 offset) le)
          length (or (type-lengths type) 1)
          offset (if (> (* count length) 4)
                   (.getUint32 data (+ 8 offset) le)
                   (+ offset 8))]
      (when-let [adapter (case type
                           1 #(.getUint8 data (+ offset %) le)
                           2 #(to-str data offset (* count length))
                           3 #(.getUint16 data (+ offset %) le)
                           4 #(.getUint32 data (+ offset %) le)
                           5 #(str (.getUint32 data (+ offset %) le)
                                   "/"
                                   (.getUint32 data (+ offset % 4) le))
                           6 #(.getInt8 data (+ offset %) le)
                           8 #(.getInt16 data (+ offset %) le)
                           9 #(.getInt32 data (+ offset %) le)
                           10 #(str (.getInt32 data (+ offset %) le)
                                    "/"
                                    (.getInt32 data (+ offset % 4) le))
                           nil)]
        {:tag tag
         :value (if (= 2 type)
                  (adapter)
                  (if (> count 1)
                    (vec (map adapter (range 0 (* count length) length)))
                    (adapter 0)))}))))

(defn- ifds [data offset le]
  (let [offset (or offset (.getUint32 data 4 le))
        num (.getUint16 data offset le)
        next-offset (.getUint32 data (+ offset 2 (* num 12)) le)
        res (for [offset (map #(+ offset 2 (* 12 %)) (range num))]
              (let [ifd (ifd data offset le)]
                (if (= :exif (:tag ifd))
                  (ifds data (:value ifd) le)
                  ifd)))
        res (->> res
                 flatten
                 (filter identity))]
    (if (and next-offset
             (> next-offset 0)
             (< next-offset (.-byteLength data)))
      (concat res (ifds data next-offset le))
      res)))

(defn from-tiff
  "Reading TIFF image information from a js/DataView."
  [data]
  (when (and data
             (or (and (= "MM" (to-str data 0 2)) (= 42 (.getUint16 data 2 false)))
                 (and (= "II" (to-str data 0 2)) (= 42 (.getUint16 data 2 true)))))
    (reduce (fn [res {:keys [tag value]}] (assoc res tag value))
            {}
            (ifds data nil (= "II" (to-str data 0 2))))))

(defn from-jpeg
  "Reading JPEG image information from a js/DataView."
  [data]
  (when (and data
             (= 0xFFD8 (.getUint16 data 0)))
    (let [length (.-byteLength data)
          data (loop [offset 2]
                 (when (< offset length)
                   (if (= 0xFF (.getUint8 data offset))
                     (let [marker (.getUint8 data (+ 1 offset))]
                       (cond
                         (= 0xE1 marker)  ; APP1
                         (if (= "Exif" (to-str data (+ 4 offset) 4))
                           (js/DataView. (.-buffer data)
                                         (+ 4 6 offset)
                                         (.getUint16 data (+ 2 offset)))
                           (recur (+ 4 offset (.getUint16 data (+ 2 offset)))))

                         (#{0xD9 0xDA} marker) ; EOI SOS
                         nil

                         :else
                         (recur (inc offset))))
                     (recur (inc offset)))))]
      (from-tiff data))))
