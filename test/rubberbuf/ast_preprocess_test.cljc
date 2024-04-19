(ns rubberbuf.ast-preprocess-test
  (:require [rubberbuf.ast-preprocess :refer [rast->rast-extended rast->rast-resolved rast->msg-fields rast->referables referables->extends referables->lookup]]
            [clojure.test :refer [is deftest run-tests]]))

;-------------------------------------------------------------------------------
; test resolution of message name in field to become fully scoped
(def pb3_rast1 {"p1.proto" [[:syntax "proto3"]
                            [:package "a.b.c"]
                            [:import "p2.proto"]
                            [:import "p3.proto"]
                            [:message "msg1"
                             [:message "msgA" [:field nil :uint32 1 nil]]
                             [:field :optional "msgA" "msgX_val" 1 nil]
                             [:field :optional "msgB" "msgY_val" 2 nil]
                             [:field :optional "msg2.msgB" "msgZ_val" 3 nil]
                             [:mapField :sint64 "msgA" "sint64_msg" 4 nil]
                             [:field :optional ".a.b.c.msg2.msgB" "msg0_val" 5 nil]]
                            [:message "msgA"] [:message "msgB"]]
                "p2.proto" [[:syntax "proto3"]
                            [:package "a.b.c"]
                            [:import "p1.proto"]
                            [:import "p3.proto"]
                            [:message "msg2"
                             [:message "msgB" [:field nil :uint32 1 nil]]
                             [:field :optional "msgA" "msgX_val" 1 nil]
                             [:field :optional "msgB" "msgY_val" 2 nil]
                             [:field :optional "msg1.msgA" "msgX_val" 3 nil]
                             [:mapField :sint64 "msgA" "sint64_msg" 4 nil]
                             [:extend "msg2"
                              [:field :optional "msgB" "msgB_val" 101]]]
                            [:message "msgA"] [:message "msgB"]
                            [:message
                             "msg1of"
                             [:oneof
                              "either"
                              [:oneofField "msg1" "msg1_val" 1 nil]
                              [:oneofField "msgA" "msga_val" 2 nil]
                              [:oneofField "msgB" "msgb_val" 3 nil]]]
                            [:service "svc"
                             [:rpc "method1" nil "msg1.msgA" nil "msg2.msgB" nil]
                             [:rpc "method2" :stream "msgA" :stream "msgB" nil]]]
                ; messages below shouldn't be referenced as others are better match
                "p3.proto" [[:syntax "proto3"]
                            [:message "msg1"]
                            [:message "msg2"]
                            [:message "msgA"]
                            [:message "msgB"]]})

(def pb3_rast1_referables [["p1.proto" [[1 :package "a.b.c"] [4 :message "msg1" [2 :message "msgA"]] [5 :message "msgA"] [6 :message "msgB"]]]
                           ["p2.proto" [[1 :package "a.b.c"] [4 :message "msg2" [2 :message "msgB"] [7 :extend "msg2"]] [5 :message "msgA"] [6 :message "msgB"] [7 :message "msg1of"]]]
                           ["p3.proto" [[1 :message "msg1"] [2 :message "msg2"] [3 :message "msgA"] [4 :message "msgB"]]]])

(def pb3_rast1_lookup {"msg1" [["p1.proto" ["a.b.c" "msg1"] [4]] ["p3.proto" ["" "msg1"] [1]]],
                       "msgA" [["p1.proto" ["a.b.c" "msg1" "msgA"] [4 2]] ["p1.proto" ["a.b.c" "msgA"] [5]] ["p2.proto" ["a.b.c" "msgA"] [5]] ["p3.proto" ["" "msgA"] [3]]],
                       "msgB" [["p1.proto" ["a.b.c" "msgB"] [6]] ["p2.proto" ["a.b.c" "msg2" "msgB"] [4 2]] ["p2.proto" ["a.b.c" "msgB"] [6]] ["p3.proto" ["" "msgB"] [4]]],
                       "msg2" [["p2.proto" ["a.b.c" "msg2"] [4]] ["p3.proto" ["" "msg2"] [2]]],
                       "msg1of" [["p2.proto" ["a.b.c" "msg1of"] [7]]]})

(def pb3_rast1_resolved
  {"p1.proto" [[:syntax "proto3"]
               [:package "a.b.c"]
               [:import "p2.proto"]
               [:import "p3.proto"]
               [:message "msg1"
                [:message "msgA"
                 [:field nil :uint32 1 nil]]
                [:field :optional "a.b.c/msg1.msgA" "msgX_val" 1 nil]
                [:field :optional "a.b.c/msgB" "msgY_val" 2 nil]
                [:field :optional "a.b.c/msg2.msgB" "msgZ_val" 3 nil]
                [:mapField :sint64 "a.b.c/msg1.msgA" "sint64_msg" 4 nil]
                [:field :optional "a.b.c/msg2.msgB" "msg0_val" 5 nil]]
               [:message "msgA"] [:message "msgB"]],
   "p2.proto" [[:syntax "proto3"]
               [:package "a.b.c"]
               [:import "p1.proto"]
               [:import "p3.proto"]
               [:message "msg2"
                [:message "msgB"
                 [:field nil :uint32 1 nil]]
                [:field :optional "a.b.c/msgA" "msgX_val" 1 nil]
                [:field :optional "a.b.c/msg2.msgB" "msgY_val" 2 nil]
                [:field :optional "a.b.c/msg1.msgA" "msgX_val" 3 nil]
                [:mapField :sint64 "a.b.c/msgA" "sint64_msg" 4 nil]
                [:extend "msg2"
                 [:field :optional "a.b.c/msg2.msgB" "msgB_val" 101]]]
               [:message "msgA"]
               [:message "msgB"]
               [:message "msg1of"
                [:oneof "either"
                 [:oneofField "a.b.c/msg1" "msg1_val" 1 nil]
                 [:oneofField "a.b.c/msgA" "msga_val" 2 nil]
                 [:oneofField "a.b.c/msgB" "msgb_val" 3 nil]]]
               [:service "svc"
                [:rpc "method1" nil "a.b.c/msg1.msgA" nil "a.b.c/msg2.msgB" nil]
                [:rpc "method2" :stream "a.b.c/msgA" :stream "a.b.c/msgB" nil]]],
   "p3.proto" [[:syntax "proto3"]
               [:message "msg1"]
               [:message "msg2"]
               [:message "msgA"]
               [:message "msgB"]]})

(deftest test-p3-rast1
  (is (= pb3_rast1_referables (rast->referables    pb3_rast1)))
  (is (= pb3_rast1_lookup     (referables->lookup  pb3_rast1_referables)))
  (is (= pb3_rast1_resolved   (rast->rast-resolved pb3_rast1
                                                   pb3_rast1_lookup))))

;-------------------------------------------------------------------------------
; test extending "msgA" where there are multiple messages of the same name
(def pb3_rast2 {"p1.proto" [[:syntax "proto3"]
                            [:package "a.b.c"]
                            [:message "msg1"
                             [:message "msgA"
                              [:message "msgA"
                               [:message "msgA"]]
                              [:extend "msgA"
                               [:field nil :uint64 "ex1" 100 nil]]]]
                            [:message "msgA"
                             [:message "msgA"
                              [:message "msgA"]]]]})

(def pb3_rast2_extended {"p1.proto"
                         [[:syntax "proto3"]
                          [:package "a.b.c"]
                          [:message "msg1"
                           [:message "msgA"
                            [:message "msgA"
                             [:message "msgA"]
                             [:field+ nil :uint64 "a.b.c/msg1.msgA.ex1" 100 nil]]  ;; :field+ means extended field
                            [:extend "msgA"
                             [:field nil :uint64 "ex1" 100 nil]]]]
                          [:message "msgA"
                           [:message "msgA"
                            [:message "msgA"]]]]})

(def pb3_rast2_referables (rast->referables    pb3_rast2))
(def pb3_rast2_lookup     (referables->lookup  pb3_rast2_referables))
(def pb3_rast2_extends    (referables->extends pb3_rast2_referables))

(deftest test-p3-rast2
  (is (= pb3_rast2_extended (rast->rast-extended pb3_rast2
                                                 pb3_rast2_lookup
                                                 pb3_rast2_extends))))

;-------------------------------------------------------------------------------
; test extending "extendable_msg" from different scopes
(def pb3_rast3 {"p1.proto" [[:syntax "proto3"]
                            [:package "a.b.c"]
                            [:message "extendable_msg"
                             [:field :optional :int32 "em1" 1 nil]
                             [:extensions [100 200]]]
                            [:extend "extendable_msg"
                             [:field :optional :uint32 "ext1" 100 nil]]]
                "p2.proto" [[:syntax "proto3"]
                            [:package "x.y.z"]
                            [:import "p1.proto"]
                            [:extend "a.b.c.extendable_msg"
                             [:field :optional :uint64 "ext2" 101 nil]]
                            [:message "m1"
                             [:extend "a.b.c.extendable_msg"
                              [:field :optional :sint32 "ext3" 102 nil]]]]
                "p3.proto" [[:syntax "proto3"]
                            [:import "p1.proto"]
                            [:extend "a.b.c.extendable_msg"
                             [:field :optional :sint64 "ext4" 103 nil]]
                            [:message "m2"
                             [:extend "a.b.c.extendable_msg"
                              [:field :optional :string "ext5" 104 nil]]]]})

(def pb3_rast3_extended {"p1.proto" [[:syntax "proto3"]
                                     [:package "a.b.c"]
                                     [:message "extendable_msg"
                                      [:field :optional :int32 "em1" 1 nil]
                                      [:extensions [100 200]]
                                      [:field+ :optional :uint32 "a.b.c/ext1" 100 nil]
                                      [:field+ :optional :uint64 "x.y.z/ext2" 101 nil]     ;; :field+ means extended field
                                      [:field+ :optional :sint32 "x.y.z/m1.ext3" 102 nil]  ;; :field+ means extended field
                                      [:field+ :optional :sint64 "ext4" 103 nil]           ;; :field+ means extended field
                                      [:field+ :optional :string "m2.ext5" 104 nil]]       ;; :field+ means extended field
                                     [:extend "extendable_msg"
                                      [:field :optional :uint32 "ext1" 100 nil]]]
                         "p2.proto" [[:syntax "proto3"]
                                     [:package "x.y.z"]
                                     [:import "p1.proto"]
                                     [:extend "a.b.c.extendable_msg"
                                      [:field :optional :uint64 "ext2" 101 nil]]
                                     [:message "m1"
                                      [:extend "a.b.c.extendable_msg"
                                       [:field :optional :sint32 "ext3" 102 nil]]]]
                         "p3.proto" [[:syntax "proto3"]
                                     [:import "p1.proto"]
                                     [:extend "a.b.c.extendable_msg"
                                      [:field :optional :sint64 "ext4" 103 nil]]
                                     [:message "m2"
                                      [:extend "a.b.c.extendable_msg"
                                       [:field :optional :string "ext5" 104 nil]]]]})

(def pb3_rast3_referables (rast->referables    pb3_rast3))
(def pb3_rast3_lookup     (referables->lookup  pb3_rast3_referables))
(def pb3_rast3_extends    (referables->extends pb3_rast3_referables))
(def pb3_rast3_resolved   (rast->rast-resolved pb3_rast3
                                               pb3_rast3_lookup))

(deftest test-p3-rast3
  (is (= pb3_rast3          pb3_rast3_resolved)) ; no difference because all fields are primitive types only
  (is (= pb3_rast3_extended (rast->rast-extended pb3_rast3 pb3_rast3_lookup
                                                 pb3_rast3_extends))))
