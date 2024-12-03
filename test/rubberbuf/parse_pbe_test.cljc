(ns rubberbuf.parse-pbe-test
  (:require [rubberbuf.parse :refer [parse]]
            [clojure.test :refer [is deftest run-tests]]))

(defn pe [pb-text ast]
  (is (= (parse (str " edition = '2023';\n" pb-text)) (conj [[:edition "2023"]] ast))))

(deftest test-pe-top-level
  (is (= (parse "edition = '2023';") [[:edition "2023"]]))
  (pe "import 'abc.proto';"        [:import "abc.proto"])
  (pe "import weak 'abc.proto';"   [:import :weak "abc.proto"])
  (pe "import public 'abc.proto';" [:import :public "abc.proto"])
  (pe "package my.package.ns;"     [:package "my.package.ns"])
  (pe "option abc = 1;"            [:option "abc" 1])
  (pe "option a.b.c = 1;"          [:option "a.b.c" 1])
  (pe "option (abc) = 1;"          [:option "(abc)" 1])
  (pe "option (a.b.c) = 1;"        [:option "(a.b.c)" 1])
  (pe "message ABC {}"             [:message "ABC"])
  (pe "enum ABC {}"                [:enum "ABC"])
  (pe "service ABC {}"             [:service "ABC"]))

;-------------------------------------------------------------------------------
(def pbeditions_int_literals "
  edition = '2023';
  enum E {
        DEC_20 = 20;
        HEX_20 = 0x20;
        OCT_20 = 020;
  }

  message M {
    optional int32 dec_20 = 20;
    optional int32 hex_20 = 0x20;
    optional int32 oct_20 = 020;
  }")

(def pbeditions_int_literals_ast [[:edition "2023"]
                           [:enum "E"
                            [:enumField "DEC_20" 20 nil]
                            [:enumField "HEX_20" 32 nil]
                            [:enumField "OCT_20" 16 nil]]
                           [:message "M"
                            [:field :optional :int32 "dec_20" 20 nil]
                            [:field :optional :int32 "hex_20" 32 nil]
                            [:field :optional :int32 "oct_20" 16 nil]]])

;-------------------------------------------------------------------------------
(def pbeditions_enm1 "
edition = '2023';
enum enm {
  option allow_alias = true;
  ZERO = 0;
  ONE = 1;
  ANOTHER_ONE = 1;
  TWO = 2;
  THREE = 3 [deprecated = true];
  reserved 2, 15, 9 to 11, 40 to max;
  reserved FOO, BAR;
}")

(def pbeditions_enm1_ast
  [[:edition "2023"]
   [:enum "enm"
    [:option "allow_alias" :true]
    [:enumField "ZERO" 0 nil]
    [:enumField "ONE" 1 nil]
    [:enumField "ANOTHER_ONE" 1 nil]
    [:enumField "TWO" 2 nil]
    [:enumField "THREE" 3 [["deprecated" :true]]]
    [:reserved-ranges 2 15 [9 11] [40 536870911]]
    [:reserved-names "FOO" "BAR"]]])

;-------------------------------------------------------------------------------
(def pbeditions_msg1 "
edition = '2023';
message msg {
  option deprecated = true;
  option (test) =  {a: 1, b: 2, c: 3};
  optional double double_val = 1 [deprecated = true];
           float float_val = 2;
  repeated int32 int32_val = 3 [default = 123];
  optional int64 int64_val = 4;
           uint32 uint32_val = 5 [(my.custom.opt) = 'quick fox'];
  repeated uint64 uint64_val = 6;
  optional sint32 sint32_val = 7 [(test) = {a: 1, b: 2, c: 3}];
           sint64 sint64_val = 8;
  repeated fixed32 fixed32_val = 9 [(test) = {a: {b: {c: 3}}}];
  optional fixed64 fixed64_val = 10;
           sfixed32 sfixed32_val = 11 [(test) = 1, (test) = 2];
  repeated sfixed64 sfixed64_val = 12;
  optional bool bool_val = 13 [(test) = [1, 2, 3, 4]];
           string string_val = 14;
  repeated bytes bytes_val = 15 [(test) = {a: 1, a: 4}];
  optional msg msg_val = 16;

  reserved name1, name2, name3;
  reserved name4, name5, name6;
  reserved 100, 200 to 299;
  reserved 1000, 2000 to max;
}
")

(def pbeditions_msg1_ast
  [[:edition "2023"]
   [:message
    "msg"
    [:option "deprecated" :true]
    [:option "(test)" {"a" 1, "b" 2, "c" 3}]
    [:field :optional :double "double_val" 1 [["deprecated" :true]]]
    [:field nil :float "float_val" 2 nil]
    [:field :repeated :int32 "int32_val" 3 [["default" 123]]]
    [:field :optional :int64 "int64_val" 4 nil]
    [:field nil :uint32 "uint32_val" 5 [["(my.custom.opt)" "quick fox"]]]
    [:field :repeated :uint64 "uint64_val" 6 nil]
    [:field :optional :sint32 "sint32_val" 7 [["(test)" {"a" 1, "b" 2, "c" 3}]]]
    [:field nil :sint64 "sint64_val" 8 nil]
    [:field :repeated :fixed32 "fixed32_val" 9 [["(test)" {"a" {"b" {"c" 3}}}]]]
    [:field :optional :fixed64 "fixed64_val" 10 nil]
    [:field nil :sfixed32 "sfixed32_val" 11 [["(test)" 1] ["(test)" 2]]]
    [:field :repeated :sfixed64 "sfixed64_val" 12 nil]
    [:field :optional :bool "bool_val" 13 [["(test)" [1 2 3 4]]]]
    [:field nil :string "string_val" 14 nil]
    [:field :repeated :bytes "bytes_val" 15 [["(test)" {"a" [1 4]}]]]
    [:field :optional "msg" "msg_val" 16 nil]
    [:reserved-names "name1" "name2" "name3"]
    [:reserved-names "name4" "name5" "name6"]
    [:reserved-ranges 100 [200 299]]
    [:reserved-ranges 1000 [2000 536870911]]]])

;-------------------------------------------------------------------------------
(def pbeditions_msg2 "
edition = '2023';
message msg {
  map<string, uint32> map_val1 = 16;
  map<string, msg>    map_val2 = 17 [deprecated = true];
}
")

(def pbeditions_msg2_ast
  [[:edition "2023"]
   [:message "msg"
    [:mapField :string :uint32 "map_val1" 16 nil]
    [:mapField :string "msg" "map_val2" 17 [["deprecated" :true]]]]])

;-------------------------------------------------------------------------------
(def pbeditions_msg3 "
edition = '2023';
message msg {
  oneof test_oneof {
   double double_val = 1 [deprecated = true];
   float float_val = 2;
   int32 int32_val = 3 [default = 123];
   int64 int64_val = 4;
   uint32 uint32_val = 5 [(my.custom.opt) = 'quick fox'];
   uint64 uint64_val = 6;
   sint32 sint32_val = 7 [(test) = {a: 1, b: 2, c: 3}];
   sint64 sint64_val = 8;
   fixed32 fixed32_val = 9 [(test) = {a: {b: {c: 3}}}];
   fixed64 fixed64_val = 10;
   sfixed32 sfixed32_val = 11 [(test) = 1, (test) = 2];
   sfixed64 sfixed64_val = 12;
   bool bool_val = 13 [(test) = [1, 2, 3, 4]];
   string string_val = 14;
   bytes bytes_val = 15 [(test) = {a: 1, a: 4}];
   msg msg_val = 16;
 }
}
")

(def pbeditions_msg3_ast
  [[:edition "2023"]
   [:message
    "msg"
    [:oneof
     "test_oneof"
     [:oneofField :double "double_val" 1 [["deprecated" :true]]]
     [:oneofField :float "float_val" 2 nil]
     [:oneofField :int32 "int32_val" 3 [["default" 123]]]
     [:oneofField :int64 "int64_val" 4 nil]
     [:oneofField :uint32 "uint32_val" 5 [["(my.custom.opt)" "quick fox"]]]
     [:oneofField :uint64 "uint64_val" 6 nil]
     [:oneofField :sint32 "sint32_val" 7 [["(test)" {"a" 1, "b" 2, "c" 3}]]]
     [:oneofField :sint64 "sint64_val" 8 nil]
     [:oneofField :fixed32 "fixed32_val" 9 [["(test)" {"a" {"b" {"c" 3}}}]]]
     [:oneofField :fixed64 "fixed64_val" 10 nil]
     [:oneofField :sfixed32 "sfixed32_val" 11 [["(test)" 1] ["(test)" 2]]]
     [:oneofField :sfixed64 "sfixed64_val" 12 nil]
     [:oneofField :bool "bool_val" 13 [["(test)" [1 2 3 4]]]]
     [:oneofField :string "string_val" 14 nil]
     [:oneofField :bytes "bytes_val" 15 [["(test)" {"a" [1 4]}]]]
     [:oneofField "msg" "msg_val" 16 nil]]]])

;-------------------------------------------------------------------------------
(def pbeditions_msg4 "
edition = '2023';
message msg {
  double double_val = 1;
  enum enm {
    ZERO = 0;
    ONE = 1;
  }
  optional enm enum_val = 2;
}
")

(def pbeditions_msg4_ast
  [[:edition "2023"]
   [:message "msg"
    [:field nil :double "double_val" 1 nil]
    [:enum "enm"
     [:enumField "ZERO" 0 nil]
     [:enumField "ONE" 1 nil]]
    [:field :optional "enm" "enum_val" 2 nil]]])

;-------------------------------------------------------------------------------
(def pbeditions_msg5 "
edition = '2023';
message msg {
  double double_val = 1;
  message inner_msg {
    double double_val = 1;
  }
  optional inner_msg msg_val = 2;
  optional .msg.inner_msg msg_val2 = 3;
}
")

(def pbeditions_msg5_ast
  [[:edition "2023"]
   [:message "msg"
    [:field nil :double "double_val" 1 nil]
    [:message "inner_msg"
     [:field nil :double "double_val" 1 nil]]
    [:field :optional "inner_msg" "msg_val" 2 nil]
    [:field :optional ".msg.inner_msg" "msg_val2" 3 nil]]])

;-------------------------------------------------------------------------------
(def pbeditions_extend "
edition = '2023';
extend msg {
  double double_val = 1;
  repeated enm enum_val = 2;
}
")

(def pbeditions_extend_ast
  [[:edition "2023"]
   [:extend "msg"
    [:field nil :double "double_val" 1 nil]
    [:field :repeated "enm" "enum_val" 2 nil]]])

;-------------------------------------------------------------------------------
(def pbeditions_service "
edition = '2023';
service svc {
  option deprecated = true;
  rpc method1(ReqABC) returns (RespABC) {
    option deprecated = true;
  }
  rpc method2(nested.ReqXYZ) returns (nested.RespXYZ);
  rpc method3(stream nested.ReqXYZ) returns (stream nested.RespXYZ);
}
")

(def pbeditions_service_ast
  [[:edition "2023"]
   [:service "svc"
    [:option "deprecated" :true]
    [:rpc "method1" nil "ReqABC" nil "RespABC" [[:option "deprecated" :true]]]
    [:rpc "method2" nil "nested.ReqXYZ" nil "nested.RespXYZ" nil]
    [:rpc "method3" :stream "nested.ReqXYZ" :stream "nested.RespXYZ" nil]]])

;-------------------------------------------------------------------------------

(deftest test-p-message
  ; int literals
  (is (= (parse pbeditions_int_literals) pbeditions_int_literals_ast))
  ; enum
  (is (= (parse pbeditions_enm1) pbeditions_enm1_ast))
  ;; ; message
  (is (= (parse pbeditions_msg1) pbeditions_msg1_ast))
  (is (= (parse pbeditions_msg2) pbeditions_msg2_ast))
  (is (= (parse pbeditions_msg3) pbeditions_msg3_ast))
  (is (= (parse pbeditions_msg4) pbeditions_msg4_ast))
  (is (= (parse pbeditions_msg5) pbeditions_msg5_ast))
  ; extend
  (is (= (parse pbeditions_extend) pbeditions_extend_ast))
  ; service
  (is (= (parse pbeditions_service) pbeditions_service_ast)))
