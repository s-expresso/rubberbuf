(ns rubberbuf.main
  (:require
   #?(:cljs [cljs.pprint :refer [pprint]])
   #?(:clj [clojure.pprint :refer [pprint]])
   [com.rpl.specter :refer [select walker ALL]]
   [rubberbuf.ast-preprocess :refer [rast->rast-extended rast->rast-resolved rast->referables referables->extends referables->lookup remove-extends]]
   [rubberbuf.ast-unnest :refer [rast->rast-u]]
   [rubberbuf.parse :refer [parse]]
   [rubberbuf.util :refer [slurp]]))

;; (defn crawl
;;   [path file registry depth]
;;   (if (or (registry file) (< depth 0))
;;     registry
;;     (let [ast (parse (slurp (str path "/" file))) ; TODO: this slurp isn't cljs compatible
;;           imports (select [ALL #(= (first %) :import) (walker string?)] ast)
;;           registry (conj registry [file ast])]
;;       (reduce #(crawl path %2 %1 (dec depth)) registry imports))))

;; (def rast (crawl "resources/protobuf/extend" "extend.proto" {} 1))
;; (def referables (rast->referables rast))
;; (def lookup (referables->lookup referables))
;; (def extends (referables->extends referables))
;; (def rast-r (rast->rast-resolved rast lookup))
;; (def rast-re (rast->rast-extended rast-r lookup extends))
;; (def rast-reu (rast->rast-u rast-re))
;; (def rast2 (remove-extends rast-reu))

;; (binding [*print-meta* true]
;;   (pprint rast-reu))

;; (pprint rast-reu)

;; (pprint rast2)


(def fmd (slurp "/home/cheewah.seetoh/workspace0/clojure-gng/gng/resources/proto/src/protobuf/fmd.proto"))
;; (def ems (slurp "/home/cheewah.seetoh/workspace1/higgs/protobuf/ems.proto"))
;; (def alert (slurp "/home/cheewah.seetoh/workspace1/higgs/protobuf/alert.proto"))
;; (def crypto (slurp "/home/cheewah.seetoh/workspace1/higgs/protobuf/crypto.proto"))
;; (def core (slurp "/home/cheewah.seetoh/workspace1/higgs/protobuf/core.proto"))

(parse "
syntax = 'proto2';
enum Type {
      HEX_20 = 0x20;
      OCT_020 = 020;
      DEC_20 = 20;
}")

(parse fmd)