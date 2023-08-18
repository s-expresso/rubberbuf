(ns rubberbuf.test-ebnf
  (:require [rubberbuf.ebnf :refer [proto2-ebnf proto3-ebnf protover-ebnf textformat-ebnf]]
            #?(:cljs [rubberbuf.util :include-macros true :refer [slurp]])
            [clojure.test :refer [is deftest run-tests]]))

(def p2 (slurp "resources/ebnf/proto2.ebnf"))
(def p3 (slurp "resources/ebnf/proto3.ebnf"))
(def pv (slurp "resources/ebnf/protover.ebnf"))
(def tf (slurp "resources/ebnf/textformat.ebnf"))

(deftest test-generated-ebnf
  (is (= p2 proto2-ebnf))
  (is (= p3 proto3-ebnf))
  (is (= pv protover-ebnf))
  (is (= tf textformat-ebnf)))

(run-tests)
