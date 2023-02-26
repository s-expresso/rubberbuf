(ns rubberbuf.test-ast-preprocess
  (:require [rubberbuf.ast-unnest :refer [rast->rast-u]]
            [clojure.test :refer [is deftest run-tests]]))

;-------------------------------------------------------------------------------
; test resolution of message name in field to become fully scoped
(def pb3_rast1 {"p1.proto" [[:syntax "proto3"]
                            [:package "a.b.c"]
                            [:message "msg1"
                             [:message "msgA" [:field nil :uint32 1 nil]
                              [:message "msgA" [:field nil :uint32 1 nil]
                               [:message "msgA" [:field nil :uint32 1 nil]
                                [:message "msgA" [:field nil :uint32 1 nil]
                                 [:enum "enmA" ["ZERO" 0] ["ONE" 1]]]]]]]
                            [:message "msgA"] [:message "msgB"]]})

(def pb3_rast1_unnested
  {"p1.proto" [[:syntax "proto3"]
               [:package "a.b.c"]
               [:message "msg1"]
               [:message "msg1.msgA" [:field nil :uint32 1 nil]]
               [:message "msg1.msgA.msgA" [:field nil :uint32 1 nil]]
               [:message "msg1.msgA.msgA.msgA" [:field nil :uint32 1 nil]]
               [:message "msg1.msgA.msgA.msgA.msgA" [:field nil :uint32 1 nil]]
               [:enum "msg1.msgA.msgA.msgA.msgA.enmA" ["ZERO" 0] ["ONE" 1]]
               [:message "msgA"]
               [:message "msgB"]]})

(deftest test-p3-rast1
  (is (= pb3_rast1_unnested (rast->rast-u pb3_rast1))))

(run-tests)