(ns rubberbuf.ast-postprocess-test
  (:require [rubberbuf.ast-postprocess :refer [unnest mapify]]
            [clojure.test :refer [is deftest run-tests]]))

;-------------------------------------------------------------------------------
; test resolution of message name in field to become fully scoped
(def pb3_rast1 {"p1.proto" [[:syntax "proto3"]
                            [:package "a.b.c"]
                            [:message "msg1"
                             [:message "msgA" [:field "a" :uint32 1 nil]
                              [:message "msgA" [:field "a" :uint32 1 nil]
                               [:message "msgA" [:field "a" :uint32 1 nil]
                                [:message "msgA" [:field "a" :uint32 1 nil]
                                 [:enum "enmA" ["ZERO" 0] ["ONE" 1]]]]]]]
                            [:message "msgA" [:field "m" "a.b.c/msg1" 1 nil]] [:message "msgB"]]})

(def pb3_rast1_unnested
  {"p1.proto" [[:syntax "proto3"]
               [:package "a.b.c"]
               [:message "msg1"]
               [:message "msg1.msgA" [:field "a" :uint32 1 nil]]
               [:message "msg1.msgA.msgA" [:field "a" :uint32 1 nil]]
               [:message "msg1.msgA.msgA.msgA" [:field "a" :uint32 1 nil]]
               [:message "msg1.msgA.msgA.msgA.msgA" [:field "a" :uint32 1 nil]]
               [:enum "msg1.msgA.msgA.msgA.msgA.enmA" ["ZERO" 0] ["ONE" 1]]
               [:message "msgA" [:field "m" "a.b.c/msg1" 1 nil]]
               [:message "msgB"]]})

(deftest test-p3-rast1
  (is (= pb3_rast1_unnested (unnest pb3_rast1))))

(def pb3_unnested1
  {"p1.proto"
   [[:syntax "proto2"]
    [:package "my.package.ns"]
    [:message
     "Msg"
     [:field :required :string "msg" 1 nil]
     [:mapField :int32 :string "map_field" 2 nil]
     [:oneof "identifier" [:oneofField :string "name" 3 nil] [:oneofField :int32 "id" 4 nil]]]
    [:enum
     "enm"
     [:option "allow_alias" :true]
     [:enumField "ZERO" 0 nil]
     [:enumField "ONE" 1 nil]
     [:enumField "ANOTHER_ONE" 1 nil]
     [:enumField "TWO" 2 nil]
     [:enumField "THREE" 3 [["deprecated" :true]]]
     [:reserved-ranges 2 15 [9 11] [40 536870911]]
     [:reserved-names "FOO" "BAR"]]
    [:message "ReqABC"
     [:extensions [4 1000]]
     [:field+ :optional :string "my.ns.ext" 4 nil]]
    [:message "RespABC"]
    [:service
     "svc"
     [:option "deprecated" :true]
     [:rpc "method1" nil "simple/ReqABC" nil "simple/RespABC" [[:option "deprecated" :true]]]]]})

(def pb3_unnested1_mapified
  {"my.package.ns/Msg"
   {:context :message,
    :syntax "proto2",
    :fields
    {"msg" {:context :required, :type :string, :fid 1, :options nil},
     "map_field" {:context :map, :key-type :int32, :val-type :string, :fid 2, :options nil},
     "identifier"
     {:context :oneof,
      :oneof-fields
      {"name" {:context :oneof-field, :type :string, :fid 3, :options nil},
       "id" {:context :oneof-field, :type :int32, :fid 4, :options nil}}}}
    :file-options []},
   "my.package.ns/enm"
   {:context :enum,
    :syntax "proto2",
    :options [["allow_alias" :true]],
    :enum-fields
    {"ZERO" {:value 0, :options nil},
     "ONE" {:value 1, :options nil},
     "ANOTHER_ONE" {:value 1, :options nil},
     "TWO" {:value 2, :options nil},
     "THREE" {:value 3, :options [["deprecated" :true]]}},
    :reserved-ranges [2 15 [9 11] [40 536870911]],
    :reserved-names ["FOO" "BAR"]
    :file-options []},
   "my.package.ns/ReqABC"
   {:context :message
    :syntax "proto2",
    :extensions [[4 1000]]
    :fields {"my.ns.ext" {:context :optional, :type :string, :is-extension true,:fid 4, :options nil}}
    :file-options []},
   "my.package.ns/RespABC" {:context :message
                            :syntax "proto2",
                            :file-options []},
   "my.package.ns/svc"
   {:context :service,
    :syntax "proto2",
    :options [["deprecated" :true]],
    :rpcs
    {"method1"
     {:context :rpc,
      :request-spec nil,
      :request "simple/ReqABC",
      :response-spec nil,
      :response "simple/RespABC",
      :options [["deprecated" :true]]}}
    :file-options []}})

(deftest test-p3-unnested1-mapify
  (is (= pb3_unnested1_mapified (mapify pb3_unnested1))))

(def pb3_unnested2
  {"p1.proto"
   [[:syntax "proto2"]
    [:package "my.package.ns"]
    [:option "testing1" 1]
    [:option "testing2" 2]
    [:message "Msg"
     [:field :required :string "val" 1 nil]]
    [:message "ReqABC"
     [:field :optional "Msg" "msg" 1 nil]]]})

(def pb3_unnested2_mapified
  {"my.package.ns/Msg"
   {:context :message,
    :syntax "proto2"
    :file-options [["testing1" 1] ["testing2" 2]]
    :fields
    {"val" {:context :required, :type :string, :fid 1, :options nil}}},
   "my.package.ns/ReqABC"
   {:context :message
    :syntax "proto2"
    :file-options [["testing1" 1] ["testing2" 2]]
    :fields {"msg" {:context :optional, :type "Msg", :fid 1, :options nil}}}})

(deftest test-p3-unnested2-mapify
  (is (= pb3_unnested2_mapified (mapify pb3_unnested2))))
  
