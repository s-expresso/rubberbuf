(ns rubberbuf.parse-textformat
  (:require #?(:cljs [cljs.reader :refer [read-string]])
            #?(:cljs [instaparse.core :as insta :refer-macros [defparser]])
            #?(:clj [instaparse.core :as insta :refer [defparser]])
            [clojure.string :refer [join]]
            [rubberbuf.ebnf :refer [textformat-ebnf]]))

(def ^:private void
  "Matches whitespaces and comments; to be used as :auto-whitespace param when
   creating instaparse parser."
  (insta/parser
   "void = { #'\\s+' | '#' #'.*' }"))

(defparser parser-tf
  textformat-ebnf
  :auto-whitespace void
  :start :tf_Field)

(defn merge-tuple-into-map
  "(merge-tuple-into-map {}           [:a 4])       => {:a [4]}
   (merge-tuple-into-map {:a [1 2 3]} [:a 4])       => {:a [1 2 3 4]}
   (merge-tuple-into-map {:a [1]}     [:a [2 3 4]]) => {:a [1 2 3 4]}"
  [m [k v]]
  (let [f (if (sequential? v) into conj)
        map-val (get m k nil)]
    (cond (nil? map-val) (assoc m k v)
          (sequential? map-val) (assoc m k (f map-val v))
          :else (assoc m k (f [map-val] v)))))

(defn xform-string [& strs]
  (->> strs
       (map #(subs % 1 (dec (count %)))) ; strip leading/trailing quote
       (clojure.string/join)))

(def xform-tf
  {:tf_FieldName #(str (join %&))
   :tf_ScalarField #(into [] %&)
   :tf_MessageField #(into [] %&)
   :tf_ScalarValue identity
   :tf_String xform-string
   :tf_Float #(parse-double (join %&))
   :tf_Identifier keyword
   :tf_SignedIdentifier #(keyword (join %&))
   :tf_DecSignedInteger #(parse-long (join %&))
   :tf_DecUnsignedInteger parse-long
   :tf_HexSignedInteger #(read-string (join %&))
   :tf_HexUnsignedInteger #(read-string (join %&))
   :tf_OctSignedInteger #(read-string (join %&))
   :tf_OctUnsignedInteger #(read-string (join %&))
   :tf_ScalarList #(into [] %&)
   :tf_MessageList #(into [] %&)
   :tf_Message #(reduce merge-tuple-into-map {} %&)
   :tf_Field identity})

(defn parse [text]
  (let [ast (parser-tf text)
        ast+line (insta/add-line-and-column-info-to-metadata text ast)]
    (insta/transform xform-tf ast+line)))
