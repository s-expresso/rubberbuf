rubberbuf
=========

rubberbuf is a Clojure(Script) library to parse protobuf definition (.proto) into abstract syntax tree (AST). If you want to use Clojure(Script) to transpile protobuf definitions into other langs like C/C++, Java, Python, etc, or to dynamically read/analyze protobuf definitions, then rubberbuf might just be the library for you.

## Usage
Add the following to deps.edn (or its equivalent for lein).
```edn
{:deps
 {s-expresso/rubberbuf {:git/url "https://github.com/s-expresso/rubberbuf"
                        :tag "v0.1.1"}}}
```
then call `rubberbuf.core.protoc` in code
```clojure
(ns example.core
  (:require [rubberbuf.core :refer [protoc]]))

(protoc ["/path/to/protobuf1/", "/path/to/protobuf2/"] ; paths
        ["file1.proto" "file2.proto"]) ; files
```
produces a registry of AST, i.e. map of file name to AST
```edn
{"file1.proto" [[:syntax "proto3"]
                [:package "a.b.c"]
                [:import "file3.proto"] ...]
 "file2.proto" [[:syntax "proto2"]
                [:package "x.y.z"] ...]
 "file3.proto" [...]}
```
Note rubberbuf will recursively load all imports and append them to the registry. Hence in above example the registry also contains `file3.proto` because `file1.proto` imported it.

## AST Format
rubberbuf's AST format follows the original protocol buffer definition closely and only adds some keywords or substitute some protobuf syntaxes with keywords. Interpretation of the AST is hence obvious and you can easily figure it out.

Some note worthy features are shown using example below.
```protobuf
// content of example.proto
syntax = 'proto2';
package my.package.ns;

enum Enm1 {
  option allow_alias = true;
  ZERO = 0;
  ONE = 1;
  ANOTHER_ONE = 1 [deprecated = true];
}

message MsgA {
  optional Enm1 field_a1 = 1;
  optional sint32 field_a2 = 2 [deprecated = true, default = 5];
  message MsgB {
    optional uint32 field_b1 = 1;
    extensions 1, 2, 1000 to 2000;
  }
  repeated MsgB field_a3 = 3;
  extend MsgB {
    optional bool ext_1 = 1000;
  }
}
```

will yield
```edn
{"example.proto"
 [[:syntax "proto2"]
  [:package "my.package.ns"]
  [:enum "Enm1"
    [:option "allow_alias" :true]
    [:enumField "ZERO" 0 nil]
    [:enumField "ONE" 1 nil]
    [:enumField "ANOTHER_ONE" 1 [["deprecated" :true]]]]
  [:message "MsgA"
    [:field :optional "my.package.ns/Enm1" "field_a1" 1 nil]
    [:field :optional :sint32 "field_a2" 2 [["deprecated" :true]
                                            ["default" 5]]]
    [:message "MsgB"
      [:field :optional :uint32 "field_b1" 1 nil]
      [:extensions 1 2 [1000 2000]]
      [:field+ :optional :bool "my.package.ns/MsgA.ext_1" 1000 nil]]
    [:field :repeated "my.package.ns/MsgA.MsgB" "field_a3" 3 nil]]]}
```
with the the following note worthy characteristics:
* `[:field ...]` and `[:enumField ...]` has a `nil` at the end if it has no field option
* for `[:field ...]` of primitive type you see a keyword like `:sint32`
* for `[:field ...]` of message/enum type you see a string like `"my.package.ns/Enm1"`, which is longer than original text `Enm1` in protobuf because it is resolved to a fully qualified message/enum name.
* content of `extend` is injected into the target message as `[:field+ ...]`
* `extend` is removed from AST since already injected

## Unsupported feature
* protobuf `group` type (deprecated by google)

## Known Issues
* no checking of contextual error, i.e. no checking that same field number/name is reused in the same message, etc.
* some types of protobuf syntax error can trick parser into looping indefinitely until out of memory

Above issues imply rubberbuf is not ready to be used as a standalone transpiler, but the issues can be worked around by using google's protoc (protocol buffer compiler) to validate the protobuf files before feeding into rubberbuf. This isn't ideal, but is a reasonable workaround especially if the need to invoke googles' protoc compiler already pre-exists.
