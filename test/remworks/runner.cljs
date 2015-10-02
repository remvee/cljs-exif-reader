(ns remworks.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [remworks.exif-reader-test]))

(doo-tests 'remworks.exif-reader-test)
