# yason

*Real s-expressions mixed with fake s-expressions! What a country!*

A binary data format for semi-structured data. More soon.

Uses sexp4j under the hood: <https://github.com/csm/sexp4j/>

## Usage

    (require [yason.core :as yason])
    (def some-bytes (yason/encode-bytes {:some "object" :that-is "a map, set, list, vector, or primitive"}))
    (def obj (yason/decode-bytes some-bytes))

The format here is plain (Rivest) s-expressions, encoded in the following manner:

* Primitive types are written as atoms, and get a one-byte display hint:
    * Boolean, display hint `z`, one byte zero or one.
    * Byte, display hint `b`, one byte.
    * Short, display hint `s`, two bytes, big-endian (everything is big endian), two's complement (all ints two's complement).
    * Char, display hint `c`, two bytes, UTF-16.
    * Integer, display hint `i`, four bytes.
    * Long, display hint `l`, eight bytes.
    * Float, display hint `f`, four bytes, IEEE 754.
    * Double, display hint `d`, eight bytes, IEEE 754.
    * Strings, display hint `S`, UTF-8.
    * Byte arrays, display hint `B`, as-is.
    * BigInteger, display hint `I`, result of `toByteArray()`.
    * BigDecimal, display hint `D`, result of `toString()` (and thence, as UTF-8).
* Lists or vectors are written as lists; the first element is an atom with the byte `l`, followed by each element encoded. Thus, encoding and decoding may change the type of sequence.
* Sets are written similarly as lists or vectors, but the first element is an atom with byte `s`.
* Maps are written as a list of key/value pairs, with an atom with value `m` at the beginning.

Some examples, from the repl:

    user=> (require '[yason.core :refer :all])
    nil
    user=> (encode-string (byte 42))
    "[b]\"*\""
    user=> (encode-string (short 42))
    "[s]#002a#"
    user=> (encode-string \A)
    "[c]#0041#"
    user=> (encode-string (int 42))
    "[i]#0000002a#"
    user=> (encode-string (long 42)) ; note, longs are converted to what can contain the value
    "[b]\"*\""
    user=> (encode-string 400000000000)
    "[l]#0000005d21dba000#"
    user=> (encode-string 3.14159)
    "[d]#400921f9f01b866e#"
    user=> (encode-string (byte-array [1 2 3 4 5 6 7 8 9]))
    "[B]|AQIDBAUGBwgJ|"
    user=> (encode-string "serialize this!")
    "[S]\"serialize this!\""
    user=> (encode-string :keyword)
    "[S]keyword"
    user=> (encode-string (bigint 10000000000000000000000000000))
    "[I]|IE/OXj4lAmEQAAAA|"
    user=> (encode-string (bigdec 3.14159))
    "[D]\"3.14159\""
    user=> (encode-string [:a :list :of :items])
    "(l [S]a [S]list [S]of [S]items)"
    user=> (encode-string #{:a :set :of :items})
    "(s [S]set [S]items [S]of [S]a)"
    user=> (encode-string {:type "maps" :works-too true})
    "(m type [S]maps \"works-too\" [z]#01#)"

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
