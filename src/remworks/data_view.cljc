(ns remworks.data-view)

(defprotocol IDataView
  (getInt8 [_ offset])
  (getInt16 [_ offset little-endian])
  (getInt32 [_ offset little-endian])
  (getUint8 [_ offset])
  (getUint16 [_ offset little-endian])
  (getUint32 [_ offset little-endian])
  (getLength [_])
  (getBuffer [_]))

#? (:clj
    (defn- to-int [data offset size & [little-endian]]
      (when (< (+ offset size) (count data))
        (let [bytes ((if little-endian reverse identity)
                     (map #(bit-and (aget data %) 0xff)
                          (range offset (+ offset size))))]
          (loop [v 0, [n & r] bytes]
            (if n
              (recur (bit-or (bit-shift-left v 8) n) r)
              v))))))

#? (:clj
    (defn- signed [n size]
      (let [bits (* 8 size)
            m (bit-shift-left 1 (dec bits))]
        (if (zero? (bit-and m n))
          n
          (+ (dec (bit-shift-left 1 bits))
             (bit-not n))))))

#? (:clj
    (deftype DataViewImpl [data]
      IDataView
      (getInt8 [_ offset]
        (signed (to-int data offset 1) 1))
      (getInt16 [_ offset little-endian]
        (signed (to-int data offset 2 little-endian) 2))
      (getInt32 [_ offset little-endian]
        (signed (to-int data offset 4 little-endian) 4))
      (getUint8 [_ offset]
        (to-int data offset 1))
      (getUint16 [_ offset little-endian]
        (to-int data offset 2 little-endian))
      (getUint32 [_ offset little-endian]
        (to-int data offset 4 little-endian))
      (getLength [_]
        (alength data))
      (getBuffer [_]
        data))

    :cljs
    (deftype DataViewImpl [data]
      IDataView
      (getInt8 [_ offset]
        (when (< offset (.-byteLength data))
          (.getInt8 data offset)))
      (getInt16 [_ offset little-endian]
        (when (< (inc offset) (.-byteLength data))
          (.getInt16 data offset little-endian)))
      (getInt32 [_ offset little-endian]
        (when (< (+ offset 3) (.-byteLength data))
          (.getInt32 data offset little-endian)))
      (getUint8 [_ offset]
        (when (< offset (.-byteLength data))
          (.getUint8 data offset)))
      (getUint16 [_ offset little-endian]
        (when (< (inc offset) (.-byteLength data))
          (.getUint16 data offset little-endian)))
      (getUint32 [_ offset little-endian]
        (when (< (+ offset 3) (.-byteLength data))
          (.getUint32 data offset little-endian)))
      (getLength [_]
        (.-byteLength data))
      (getBuffer [_]
        (.-buffer data))))

#? (:clj
    (defn aslice [arr offset len]
      (let [v (make-array Byte/TYPE len)]
        (doseq [i (range 0 len)]
          (aset v i (aget arr (+ offset i))))
        v)))

(defn data-view
  ([arr]
   (if (satisfies? IDataView arr)
     arr
     #? (:cljs (DataViewImpl. (js/DataView. arr))
         :clj (DataViewImpl. arr))))
  ([arr offset length]
   #? (:cljs (DataViewImpl. (js/DataView. arr offset length))
       :clj (DataViewImpl. (aslice arr offset length)))))
