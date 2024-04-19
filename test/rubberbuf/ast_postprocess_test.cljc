(ns rubberbuf.ast-postprocess-test
  (:require [rubberbuf.ast-postprocess :refer [unnest]]
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
  (is (= pb3_rast1_unnested (unnest pb3_rast1))))
