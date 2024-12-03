(ns rubberbuf.parse
  (:require #?(:cljs [cljs.reader :refer [read-string]])
            #?(:cljs [instaparse.core :as insta :refer-macros [defparser]])
            #?(:clj [instaparse.core :as insta :refer [defparser]])
            [clojure.string :refer [join]]
            [rubberbuf.parse-textformat :refer [xform-tf]]
            [rubberbuf.ebnf :refer [proto2-ebnf proto3-ebnf protoeditions-ebnf protover-ebnf textformat-ebnf]]))

(defn- make-msg-field [label type name fnum opts]
  [:field          label type name fnum opts])

(defn- make-map-field [ktype vtype name fnum opts]
  [:mapField       ktype vtype name fnum opts])

(defn- make-oneof-field [type name fnum opts]
  [:oneofField       type name fnum opts])

(defn- make-enum-field [name fnum opts]
  [:enumField       name fnum opts])

(defn- make-rpc [name ilabel input olabel output opts]
  [:rpc      name ilabel input olabel output (if (empty? opts) nil (into [] opts))])

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

(defn- xform-rpc-label
  ([] nil)
  ([form] (keyword form)))

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
    \"Rpc2\" :stream \"Msg1`\" :stream \"Msg2\"
    [:option :java_compiler \"javac\"]
    [:option :whatever false]"
  ([[_ a1] a2 a3 a4 a5 & opts]
   (make-rpc a1 a2 a3 a4 a5 opts)))

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
    :rpcLabel xform-rpc-label
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
    :sintLit (fn ([i] i) ([sign i] (if (= sign "-") (- i) i)))
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
  (str proto2-ebnf
       textformat-ebnf)
  :auto-whitespace void)

(defparser parser-3
  (str proto3-ebnf
       textformat-ebnf)
  :auto-whitespace void)

(defparser parser-editions
  (str protoeditions-ebnf
       textformat-ebnf)
  :auto-whitespace void)

(defparser parser-ver
  protover-ebnf
  :auto-whitespace void)

(defn- parse2 [text]
  (let [ast (parser-2 text)
        ast+line (insta/add-line-and-column-info-to-metadata text ast)]
    (insta/transform xform ast+line)))

(defn- parse3 [text]
  (let [ast (parser-3 text)
        ast+line (insta/add-line-and-column-info-to-metadata text ast)]
    (insta/transform xform ast+line)))

(defn- parse-edition [text]
  (let [ast (parser-editions text)
        ast+line (insta/add-line-and-column-info-to-metadata text ast)]
    (insta/transform xform ast+line)))

(defn parse [text]
  (let [version (parser-ver text)]
    (cond
      (= :edition (-> version second first)) (parse-edition text)
      (= [:proto [:syntax [:version "proto3"]]] version) (parse3 text)
      :else (parse2 text))))
