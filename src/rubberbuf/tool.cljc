(ns rubberbuf.tool
  (:require [clojure.core :refer [slurp spit]]))

(defn escape-ebnf [ebnf]
  (clojure.string/escape ebnf {\" "\\\\\""
                               \\ "\\\\\\\\"}))

(defn write-ebnf-cljc [& opts] ; dummy opts for keyword args supplied via clj -X
  (let [p2 (escape-ebnf (slurp "resources/ebnf/proto2.ebnf"))
        p3 (escape-ebnf (slurp "resources/ebnf/proto3.ebnf"))
        pv (escape-ebnf (slurp "resources/ebnf/protover.ebnf"))
        pe (escape-ebnf (slurp "resources/ebnf/protoeditions.ebnf"))
        tf (escape-ebnf (slurp "resources/ebnf/textformat.ebnf"))
        cljc-template (slurp "resources/ebnf/ebnf.cljc.template")
        ebnf-cljc (-> cljc-template
                      (clojure.string/replace-first #"<PROTO2>" p2)
                      (clojure.string/replace-first #"<PROTO3>" p3)
                      (clojure.string/replace-first #"<PROTOVER>" pv)
                      (clojure.string/replace-first #"<PROTOEDITIONS>" pe)
                      (clojure.string/replace-first #"<TEXTFORMAT>" tf))]
    (spit "src/rubberbuf/ebnf.cljc" ebnf-cljc)))
