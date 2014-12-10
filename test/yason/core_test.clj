(ns yason.core-test
  (:require [clojure.test :refer :all]
            [yason.core :refer :all]
            [yason.util :refer [hexdump-str]]))

(deftest encode-decode
  (testing "encode and decode a simple object"
    (let [obj {:foo "bar", :baz 42, :quux true, :bar [1 2 3 4]}
          enc (encode-bytes obj)
          obj2 (decode-bytes enc)]
      (is (= obj obj2)))))

(deftest test-boolean
  (testing "boolean serialization"
    (let [true-enc (encode-bytes true)
          false-enc (encode-bytes false)
          true-dec (decode-bytes true-enc)
          false-dec (decode-bytes false-enc)]
      (is (true? true-dec) (hexdump-str true-enc))
      (is (false? false-dec) (hexdump-str false-enc)))))

(deftest test-byte
  (testing "byte serialization"
    (doseq [i (map byte (range Byte/MIN_VALUE Byte/MAX_VALUE))]
      (is (= i (decode-bytes (encode-bytes i))) (hexdump-str (encode-bytes i))))))