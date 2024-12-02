(ns rubberbuf.readme-example-test
  (:require [rubberbuf.ast-preprocess :refer [normalize]]
            [rubberbuf.parse :refer [parse]]
            [clojure.test :refer [is deftest run-tests]]))

(def pb2_example "
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
}")

(def pb2_example_rast
  {"pb2_example.proto"
   [[:syntax "proto2"]
    [:package "my.package.ns"]
    [:enum "Enm1"
     [:option "allow_alias" :true]
     [:enumField "ZERO" 0 nil]
     [:enumField "ONE" 1 nil]
     [:enumField "ANOTHER_ONE" 1 [["deprecated" :true]]]]
    [:message "MsgA"
     [:field :optional "Enm1" "field_a1" 1 nil]
     [:field :optional :sint32 "field_a2" 2 [["deprecated" :true]
                                             ["default" 5]]]
     [:message "MsgB"
      [:field :optional :uint32 "field_b1" 1 nil]
      [:extensions 1 2 [1000 2000]]]
     [:field :repeated "MsgB" "field_a3" 3 nil]
     [:extend "MsgB"
      [:field :optional :bool "ext_1" 1000 nil]]]]})

(deftest test-readme-example
  (is (= {"pb2_example.proto" (parse pb2_example)} pb2_example_rast)))

(def pb2_example_normalized_rast
  {"pb2_example.proto"
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
     [:field :repeated "my.package.ns/MsgA.MsgB" "field_a3" 3 nil]]]})

(deftest test-readme-normalized-example
  (is (= (normalize {"pb2_example.proto" (parse pb2_example)}) pb2_example_normalized_rast)))
