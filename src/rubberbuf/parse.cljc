(ns rubberbuf.parse
  (:require #?(:cljs [rubberbuf.util :include-macros true :refer [slurp]])
            #?(:cljs [cljs.pprint :refer [pprint]])
            #?(:cljs [cljs.reader :refer [read-string]])
            #?(:clj [clojure.pprint :refer [pprint]])
            [clojure.string :refer [join]]
            #?(:clj [clojure.java.io :as io])
            #?(:clj [instaparse.core :as insta :refer [defparser]])
            #?(:cljs [instaparse.core :as insta :refer-macros [defparser]])
            [rubberbuf.parse-textformat :refer [xform-tf]]))

(defn- make-msg-field [label type name fnum opts]
  [:field          label type name fnum opts])

(defn- make-map-field [ktype vtype name fnum opts]
  [:mapField       ktype vtype name fnum opts])

(defn- make-oneof-field [type name fnum opts]
  [:oneofField       type name fnum opts])

(defn- make-enum-field [name fnum opts]
  [:enumField       name fnum opts])

(defn- make-rpc [name input output opts]
  [:rpc      name input output (if (empty? opts) nil (into [] opts))])

(defn- make-message [name body]
  (into [:message name] body))

(defn- make-enum [name body]
  (into [:enum name] body))

(defn- make-group [label name fnum body]
  (into [:group label name fnum] body))

(defn- make-service [name body]
  (into [:service name] body))

(defn- xform-label
  ([] [:label nil])
  ([form] [:label (keyword form)]))

(defn- xform-message-field
  "xform ... of [:field ...] where ...
     [:label 'required'] ; [:label] implies 'optional'
     [:type :string]
     [:fieldName 'field1']
     [:fieldNumber 1]]
     [:options [:deprecated true]
               [:whatever   false]]"
  ([a1 a2 a3 a4]
   (xform-message-field a1 a2 a3 a4 nil))
  ([[_ a1] [_ a2] [_ a3] [_ a4] a5]
   (make-msg-field a1 a2 a3 a4 a5)))

(defn- xform-enum-field
  "xform [:enumField \"TWO\" 2 [:opts ...]]"
  ([fname num]
   (xform-enum-field fname num nil))
  ([fname num opts]
   (make-enum-field fname num opts)))

(defn- xform-mapField
  "xform ... of [:mapField ...] where ...
    [:keyType :uint32]
    [:valType [:priType 'string']]
    [:mapName 'kv1']
    [:fieldNumber 4]
    [:options [:deprecated true]
              [:whatever   false]]"
  ([a1 a2 a3 a4] (xform-mapField a1 a2 a3 a4 nil))
  ([a1 [_ a2] [_ a3] [_ a4] a5]
   (make-map-field a1 a2 a3 a4 a5)))

(defn- xform-ranges
  "xform
    [[:range 10] [:range 20 30] [:range 40 \"max\"]]"
  [ranges]
  (->> ranges
       (map #(if (= (count %) 3) [(second %) (last %)] (last %)))
       (clojure.walk/prewalk-replace {"max" 536870911}) ; max field number 2^29-1
       (vec)))

(defn- xform-names
  "xform ... of [:reserved-names ...] where ...
    [:fieldName :xyz] [:fieldName :abc]"
  [& names]
  (->> names
       (map #(-> % (second)))
       (into [:reserved-names])))

(defn- xform-oneofField
  "xform ... of [:oneofField ...] where ...
   [:type :string]
   [:fieldName :one_str]
   [:fieldNumber 6]
   [:options [:deprecated true]
             [:whatever   false]]"
  ([a1 a2 a3] (xform-oneofField a1 a2 a3 nil))
  ([[_ a1] [_ a2] [_ a3] a4] (make-oneof-field a1 a2 a3 a4)))

(defn- xform-rpc
  "xform ... of [:rpc ...] where ...
    :Rpc2 :Msg1 :Msg2
    [:option :java_compiler \"javac\"]
    [:option :whatever false]"
  ([[_ a1] a2 a3 & opts]
   (make-rpc a1 a2 a3 opts)))

(defn- xform-group
  "xfrom ... of [:group ...] where ... =
    [:label \"repeated\"]
    \"Result\"
    [:fieldNumber 1]
    [:field \"url\" 2 :string :required nil]
    [:field \"title\" 3 :string :optional nil]
    [:field \"snippets\" 4 :string :repeated nil]] ..."
  [[_ a1] a2 [_ a3] & args]
  (make-group a1 a2 a3 args))

(defn- xform-signedFloatLit
  [& args] (let [s (join "" args)]
             (case s
               "inf" ##Inf
               "nan" ##NaN
               (parse-double s))))

(defn- parseInt [val base]
  #?(:clj (Integer/parseInt val base))
  #?(:cljs (js/parseInt val base)))

(def ^:private xform
  (merge
   xform-tf
   {:proto #(into [] %&)
    ; names
    :ident identity
    :messageName identity
    :enumName identity
    :groupName identity
    :serviceName identity
    :oneofName identity
    ; syntax type
    :label xform-label
    :fullIdent str
    :keyType keyword
    :priType keyword
    :messageType (fn [& args] (join "" args))
    :returnType identity
    ; int literals
    :decimalLit read-string
    :octalLit read-string
    :hexLit read-string
    :intLit identity
    ; string literals
    :hexDigit2 identity
    :hexEscape #(char (parseInt % 16))
    :octalDigit3 identity
    :octEscape #(char (parseInt % 8))
    :charValue identity
    :strLit str
    ; other literals
    :boolLit parse-boolean
    :signedFloatLit xform-signedFloatLit
    ; top(ish) levels xforms
    :import (fn [& args] (if (= (count args) 2)
                           [:import (keyword (first args)) (second args)]
                           [:import (last args)]))
    :rpc xform-rpc
    :group xform-group
    :enum #(make-enum %1 (into [] %&))
    :message #(make-message %1 %&)
    :service #(make-service %1 (into [] %&))
    :extensions (fn [& args] (into [:extensions] (xform-ranges args)))
    :reserved-ranges (fn [& args] (into [:reserved-ranges] (xform-ranges args)))
    :reserved-names xform-names
    ; field
    :field xform-message-field
    :mapField xform-mapField
    :oneofField xform-oneofField
    :enumField xform-enum-field
    ; option
    :fieldOption #(into [] %&)
    :fieldOptions #(into [] %&)
    :optionName str
    ; textformat -- merged from xform-tf
    }))

(def ^:private void
  "Matches whitespaces and comments; to be used as :auto-whitespace param when
   creating instaparse parser."
  (insta/parser
   "void = { #'\\s+' | #'(\\/\\*)[\\s\\S]*?(\\*\\/)' | '//' #'.*' }"))

(defparser parser-2
  (str (slurp (io/resource "ebnf/proto2.ebnf"))
       (slurp (io/resource "ebnf/textformat.ebnf")))
  :auto-whitespace void)

(defparser parser-3
  (str (slurp (io/resource "ebnf/proto3.ebnf"))
       (slurp (io/resource "ebnf/textformat.ebnf")))
  :auto-whitespace void)

(defparser parser-ver
  (slurp (io/resource "ebnf/protover.ebnf"))
  :auto-whitespace void)

(defn- parse2 [text]
  (let [ast (parser-2 text)
        ast+line (insta/add-line-and-column-info-to-metadata text ast)]
    (insta/transform xform ast+line)))

(defn- parse3 [text]
  (let [ast (parser-3 text)
        ast+line (insta/add-line-and-column-info-to-metadata text ast)]
    (insta/transform xform ast+line)))

(defn parse [text]
  (if (= [:syntax [:version "proto3"]] (parser-ver text))
    (parse3 text)
    (parse2 text)))
