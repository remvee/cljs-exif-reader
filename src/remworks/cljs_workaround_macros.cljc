(ns remworks.cljs-workaround-macros
  (:refer-clojure :exclude [with-open]))

(defmacro with-open [bindings & body]
  `(let ~bindings ~@body))
