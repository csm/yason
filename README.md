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

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
