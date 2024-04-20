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
