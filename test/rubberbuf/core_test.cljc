(ns rubberbuf.core-test
  (:require [rubberbuf.core :refer [protoc]]
            [clojure.test :refer [is deftest run-tests]]
            [clojure.walk :refer [postwalk]]))

(def ast (protoc ["resources/protobuf/"] ["simple.proto"]))

(deftest test-meta-contains-file-info
  (let [?fail (atom false)]
    (postwalk #(when-let [m (meta %)]
                 (when-not (contains? m :instaparse.gll/file)
                   (reset! ?fail true))) ast)
    (is (not @?fail))))


(def ast2 (protoc ["resources/protobuf/"] ["simple2.proto"]))

(deftest test-message-field-fully-resolved
  (is ast2 {"simple2.proto"
            [[:syntax "proto2"]
             [:package "simple2"]
             [:message
              "Msg"
              [:message "MsgNested" [:field :required :string "msg" 1 nil]]
              [:field :required "simple2/Msg.MsgNested" "msg_nested" 1 nil]]]}))
