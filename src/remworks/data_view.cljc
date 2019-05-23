(ns remworks.data-view
  #? (:clj (:import java.io.File
                    java.io.RandomAccessFile
                    java.nio.channels.FileChannel$MapMode)))

(defprotocol IDataView
  (getInt8 [_ offset])
  (getInt16 [_ offset little-endian])
  (getInt32 [_ offset little-endian])
  (getUint8 [_ offset])
  (getUint16 [_ offset little-endian])
  (getUint32 [_ offset little-endian])
  (getLength [_])
  (slice [_ offset len])
  (close [_]))

#? (:clj
    (do
      (defn- signed [n size]
        (let [bits (* 8 size)
              m    (bit-shift-left 1 (dec bits))]
          (if (zero? (bit-and m n))
            n
            (+ (dec (bit-shift-left 1 bits))
               (bit-not n)))))

      (defn- to-int [data offset size & [little-endian]]
        (when (< (+ offset size) (count data))
          (let [bytes ((if little-endian reverse identity)
                       (map #(bit-and (aget data %) 0xff)
                            (range offset (+ offset size))))]
            (loop [v 0, [n & r] bytes]
              (if n
                (recur (bit-or (bit-shift-left v 8) n) r)
                v)))))

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
        (slice [_ offset len]
          (DataViewImpl. (let [v (make-array Byte/TYPE len)]
                           (doseq [i (range 0 len)]
                             (aset v i (aget data (+ offset i))))
                           v)))
        (close [_] nil))

      (defn to-int-byte-buffer [^java.nio.ByteBuffer buffer ^long offset ^long size little-endian]
        (when (< (+ offset size) (.limit buffer))
          (let [bytes ((if little-endian reverse identity)
                       (map #(bit-and (byte (.get buffer ^int %)) 0xff)
                            (range offset (+ offset size))))]
            (loop [v 0, [n & r] bytes]
              (if n
                (recur (bit-or (bit-shift-left v 8) n) r)
                v)))))

      (deftype DataViewByteBufferImpl [file buffer]
        IDataView
        (getInt8 [_ offset]
          (signed (to-int-byte-buffer buffer offset 1 false) 1))
        (getInt16 [_ offset little-endian]
          (signed (to-int-byte-buffer buffer offset 2 little-endian) 2))
        (getInt32 [_ offset little-endian]
          (signed (to-int-byte-buffer buffer offset 4 little-endian) 4))
        (getUint8 [_ offset]
          (to-int-byte-buffer buffer offset 1 false))
        (getUint16 [_ offset little-endian]
          (to-int-byte-buffer buffer offset 2 little-endian))
        (getUint32 [_ offset little-endian]
          (to-int-byte-buffer buffer offset 4 little-endian))
        (getLength [_]
          (.limit buffer))
        (slice [_ offset len]
          (let [arr (byte-array len)]
            (.position buffer offset)
            (.get buffer arr 0 len)
            (DataViewByteBufferImpl. nil (java.nio.ByteBuffer/wrap arr))))
        (close [_]
          (when file (.close file))))

      (defmulti data-view #(if (satisfies? IDataView %) IDataView (class %)))
      (defmethod data-view IDataView ([data] data))
      (defmethod data-view (Class/forName "[B") ([data] (DataViewImpl. data)))
      (defmethod data-view File
        ([file]
         (let [file    (RandomAccessFile. file "r")
               channel (.getChannel file)]
           (DataViewByteBufferImpl. file (.map channel FileChannel$MapMode/READ_ONLY, 0 (.size channel)))))))

    :cljs
    (do
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
        (slice [_ offset len]
          (DataViewImpl. (js/DataView. (.-buffer data) offset len)))
        (close [_] nil))

      (defn data-view [arr]
        (if (satisfies? IDataView arr)
          arr
          (DataViewImpl. (js/DataView. arr))))))
