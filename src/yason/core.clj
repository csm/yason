(ns yason.core
  (:import [org.metastatic.sexp4j ExpressionList Atom CanonicalWriter CanonicalParser AdvancedWriter]
           (org.metastatic.sexp4j.mapper MapperException)
           (java.util Optional)
           (java.io ByteArrayOutputStream ByteArrayInputStream)))

(defn *atom
  [obj]
  (prn "encoding " obj " a " (type obj))
  (if (keyword? obj)
    (org.metastatic.sexp4j.Atom/atom (str obj))
    (org.metastatic.sexp4j.Atom/atom obj)))

(defn encode
  "Encode the given object as an s-expression object."
  [obj]
  (cond
    (map? obj) (ExpressionList/list (into-array
                                      (into [(*atom (byte \m))]
                                            (flatten (map #(list (*atom (first %)) (encode (second %))) obj)))))
    (seq? obj) (ExpressionList/list (into [(*atom (byte \l))]
                                          (map encode obj)))
    (set? obj) (ExpressionList/list (into [(*atom (byte \s))]
                                          (map encode obj)))
    (string? obj) (.withHint (*atom obj) (byte \S))
    (keyword? obj) (.withHint (*atom (str obj)) (byte \S))
    (nil? obj) (.withHint (*atom (byte-array 0)) (byte \n))
    (instance? (Class/forName "[B") obj) (.withHint (*atom obj) (byte \B))
    (instance? Boolean obj) (.withHint (*atom (if obj (byte 1) (byte 0))) (byte \z))
    (instance? Byte obj) (.withHint (*atom obj) (byte \b))
    (instance? Short obj) (.withHint (*atom obj) (byte \s))
    (instance? Character obj) (.withHint (*atom obj) (byte \c))
    (instance? Integer obj) (.withHint (*atom obj) (byte \i))
    (instance? Long obj) (.withHint (*atom obj) (byte \l))
    (instance? Float obj) (.withHint (*atom obj) (byte \f))
    (instance? Double obj) (.withHint (*atom obj) (byte \d))
    (instance? BigInteger obj) (.withHint (*atom (.toByteArray obj)) (byte \I))
    (instance? BigDecimal obj) (.withHint (*atom (.toString obj)) (byte \D))
    true (throw (MapperException. "can't encode that object"))))

(defn decode
  [expr]
  (if (instance? Atom expr)
    (let [hint (.orElse (.displayHint expr) (*atom (byte \B)))]
      (condp #(= %1 %2) (.typeCode hint)
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
    (condp #(= %1 %2) (.typeCode (.get expr 0))
      \l (map decode (rest expr))
      \s (hash-set (map decode (rest expr)))
      \m (hash-map (map #(list (.stringValue (first %)) (encode (second %))) (rest expr))))))

(defn encode-bytes
  [obj]
  (let [out (ByteArrayOutputStream.)]
    (.writeExpression (CanonicalWriter. out) (encode obj))
    (.toByteArray out)))

(defn decode-bytes
  [buf]
  (let [in (ByteArrayInputStream. buf)]
    (.parse (CanonicalParser. in))))

(defn encode-string
  [obj]
  (let [out (ByteArrayOutputStream.)]
    (.writeExpression (.build (.outputStream (AdvancedWriter/create) out)) (encode obj))
    (String. (.toByteArray out))))