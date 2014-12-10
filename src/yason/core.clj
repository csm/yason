(ns yason.core
  (:require [clojure.java.io :refer [input-stream]])
  (:import [org.metastatic.sexp4j ExpressionList Atom CanonicalWriter CanonicalParser AdvancedWriter AdvancedWriter$Builder DisplayHint Primitives AdvancedParser Expression]
           [org.metastatic.sexp4j.mapper MapperException]
           [java.util Optional]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
           (clojure.lang BigInt)))

(defn encode-key
  [key]
  (if (keyword? key) (name key) key))

(defn encode
  "Encode the given object as an s-expression object."
  [obj]
  (cond
    (map? obj) (ExpressionList/list (into-array Expression
                                      (into [(Atom/atom ^Byte (byte \m))]
                                            (flatten (map #(list (Atom/atom (encode-key (first %)))
                                                                 (encode (second %))) obj)))))
    (or (seq? obj) (vector? obj)) (ExpressionList/list (into-array
                                                         (into [(Atom/atom ^Byte (byte \l))]
                                                           (map encode obj))))
    (set? obj) (ExpressionList/list (into-array
                                      (into [(Atom/atom ^Byte (byte \s))]
                                            (map encode obj))))
    (string? obj) (.withHint (Atom/atom ^String obj) (byte \S))
    (keyword? obj) (.withHint (Atom/atom ^String (name obj)) (byte \S))
    (nil? obj) (.withHint (new Atom (byte-array 0)) (byte \n))
    (instance? (Class/forName "[B") obj) (.withHint (new Atom obj) (byte \B))
    (instance? Boolean obj) (.withHint (Atom/atom ^Byte (if obj (byte 1) (byte 0))) (byte \z))
    (instance? Byte obj) (.withHint (Atom/atom ^Byte obj) (byte \b))
    (instance? Short obj) (.withHint (Atom/atom ^Short obj) (byte \s))
    (instance? Character obj) (.withHint (Atom/atom ^Character obj) (byte \c))
    (instance? Integer obj) (.withHint (Atom/atom ^Integer obj) (byte \i))
    (instance? Long obj) (cond
                           (and (>= obj Byte/MIN_VALUE) (<= obj Byte/MAX_VALUE))
                           (.withHint (Atom/atom ^Byte (byte obj)) (byte \b))
                           (and (>= obj Short/MIN_VALUE) (<= obj Short/MAX_VALUE))
                           (.withHint (Atom/atom ^Short (short obj)) (byte \s))
                           (and (>= obj Integer/MIN_VALUE) (<= obj Integer/MAX_VALUE))
                           (.withHint (Atom/atom ^Integer (int obj)) (byte \i))
                           true
                           (.withHint (new Atom (Primitives/bytes (long obj))) (byte \l)))
    (instance? Float obj) (.withHint (Atom/atom ^Float obj) (byte \f))
    (instance? Double obj) (.withHint (Atom/atom obj) (byte \d))
    (instance? BigInteger obj) (.withHint (new Atom (.toByteArray obj)) (byte \I))
    (instance? BigDecimal obj) (.withHint (Atom/atom (.toString obj)) (byte \D))
    (instance? BigInt obj) (.withHint (new Atom (.toByteArray (.toBigInteger obj))) (byte \I))
    true (throw (MapperException. (str "can't encode that object: " (type obj))))))

(defn decode
  [expr]
  (if (instance? Atom expr)
    (let [hint (if (.isPresent (.displayHint expr))
                 (char (.typeCode (.atom (.get (.displayHint expr)))))
                 \B)]
      (condp #(= %1 %2) hint
        \S (.stringValue expr)
        \B (.bytes expr)
        \n nil
        \z (if (.byteValue expr) true false)
        \b (.byteValue expr)
        \s (.shortValue expr)
        \c (.charValue expr)
        \i (.intValue expr)
        \l (.longValue expr)
        \f (.floatValue expr)
        \d (.doubleValue expr)
        \I (.bigIntegerValue expr)
        \D (.bigDecimalValue expr)
        (throw (MapperException. (str "can't decode hint: " (.typeCode hint))))))
    (condp #(= %1 %2) (char (.typeCode (.get expr 0)))
      \l (map decode (rest expr))
      \s (hash-set (map decode (rest expr)))
      \m (reduce #(assoc %1 (first %2) (second %2)) {}
           (map #(list (keyword (.stringValue (first %))) (decode (second %))) (partition 2 (rest expr)))))))

(defn encode-stream
  "Encode an object to an expression, then serialize it to the given output stream,
  according to format (and line length and indent size).

  The format argument may be :canonical or :advanced.
  The line-length and indent-size arguments are only used for :advanced format."
  ([obj output] (encode-stream obj output :canonical 0 0))
  ([obj output format line-length indent-size]
    (.writeExpression
      (condp = format
        :canonical (CanonicalWriter. output)
        :advanced (.build (.lineLength (.indentAmount (.outputStream (AdvancedWriter/create) output) indent-size) line-length)))
      (encode obj))))

(defn decode-stream
  ([buf] (decode-stream buf :canonical))
  ([buf format] (decode
                  (.parse (condp = format
                            :canonical (CanonicalParser. (input-stream buf))
                            :advanced (AdvancedParser. (input-stream buf)))))))

(defn encode-bytes
  "Encode an object into an expression, then serialize that expression
  in the canonical format."
  [obj]
  (let [out (ByteArrayOutputStream.)]
    (encode-stream obj out)
    (.toByteArray out)))

(defn decode-bytes
  "Decode a byte array, string, input stream, etc., using the canonical=
  expression format. Returns the object that was read."
  [buf]
  (decode-stream buf))

(defn encode-string
  "Encode an object into an expression, then serialize that expression
  in advanced format, returning a string."
  ([obj] (encode-string obj 80 2))
  ([obj line-length indent-size]
    (let [out (ByteArrayOutputStream.)]
      (.writeExpression (.build (.indentAmount (.lineLength (.outputStream (AdvancedWriter/create) out) line-length) indent-size)) (encode obj))
      (String. (.toByteArray out)))))

(defn decode-string
  "Decode a string expression in advanced format into an object."
  [buf]
  (decode-stream (.getBytes buf "UTF-8") :advanced))
