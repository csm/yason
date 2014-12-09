(ns yason.core-test
  (:require [clojure.test :refer :all]
            [yason.core :refer :all]))

(deftest encode-decode
  (testing "encode and decode a simple object"
    (let [obj {:foo "bar", :baz 42, :quux true, :bar [1 2 3 4]}
          enc (encode-bytes obj)
          obj2 (decode-bytes enc)]
      (is (= obj obj2)))))
