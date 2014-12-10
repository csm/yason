(ns yason.util
  (:require [clojure.string :refer [join]]))

(defn printable-char?
  [c]
  (and (>= (int c) 0x20) (< (int c) 0x7f)))

(defn printable-char
  [b]
  (let [c (char (bit-and (int b) 0xff))]
    (if (printable-char? c)
      c \.)))

(defn hexdump
  [b]
  (map-indexed
    (fn [i part]
      (let [chars (filter #(not (nil? %)) part)
            pad (filter #(nil? %) part)]
        (str (format "%08x  " (* i 16))
             (join " " (map #(format "%02x" (bit-and % 0xff)) chars))
             (join (repeat (count pad) "   "))
             "  "
             (join (map printable-char chars)))))
    (partition 16 1 (repeat 16 nil) b)))

(defn hexdump-str
  [b]
  (join "\n" (hexdump b)))