(ns rubberbuf.ebnf-test
  "These tests are clj only (no cljs) `clj -X:write-ebnf-cljc` is used to generate rubberbuf/ebnf.cljc to contain strings that are identical to the .ebnf,
   hence we only need to use clj to verify the content is really identical (file access on javascript is restricted due to sandbox and not fun)"
  (:require [rubberbuf.ebnf :refer [proto2-ebnf proto3-ebnf protover-ebnf textformat-ebnf]]
            [rubberbuf.util :refer [loader]]
            [clojure.test :refer [is deftest run-tests]]))

(def p2 (loader "resources/ebnf/proto2.ebnf"))
(def p3 (loader "resources/ebnf/proto3.ebnf"))
(def pv (loader "resources/ebnf/protover.ebnf"))
(def tf (loader "resources/ebnf/textformat.ebnf"))

(deftest test-generated-ebnf
  (is (= p2 proto2-ebnf))
  (is (= p3 proto3-ebnf))
  (is (= pv protover-ebnf))
  (is (= tf textformat-ebnf)))
