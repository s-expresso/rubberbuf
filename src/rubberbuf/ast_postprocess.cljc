(ns rubberbuf.ast-postprocess
  (:require [com.rpl.specter :refer [collect-one cond-path declarepath if-path multi-path nthpath providepath putval select  subselect transform
                                     ALL ALL-WITH-META FIRST LAST NONE STAY]]
            [flatland.ordered.map :refer [ordered-map]]
            [rubberbuf.ast-util :refer [enm? ext? field? grp? msg? msg-enm? msg-enm-grp-ext?]]
            [rubberbuf.core :refer [protoc]]))

(declarepath WALK-TOP-LEVEL)
(declarepath WALK-EXTEND)
(declarepath WALK-GROUP)
(declarepath WALK-MESSAGE)
(declarepath GROUP->FIELD)

(providepath WALK-TOP-LEVEL [(cond-path enm? STAY
                                        ext? WALK-EXTEND
                                        msg? WALK-MESSAGE
                                        (fn [_] true) STAY)])

(providepath WALK-EXTEND [(multi-path [(subselect [ALL-WITH-META (cond-path #(not (grp? %)) STAY
                                                                            grp? GROUP->FIELD)])]
                                      [ALL-WITH-META (cond-path grp? WALK-GROUP)])])

(providepath WALK-GROUP [(multi-path [(subselect [ALL-WITH-META (cond-path #(not (or (msg-enm-grp-ext? %) (number? %) (= :optional %) (= :required %))) STAY
                                                                           grp? GROUP->FIELD)])]
                                     [(collect-one [ALL string?]) ALL-WITH-META (cond-path enm? STAY
                                                                                           ext? WALK-EXTEND
                                                                                           grp? WALK-GROUP
                                                                                           msg? WALK-MESSAGE)])])

(providepath WALK-MESSAGE [(multi-path [(subselect [ALL-WITH-META (cond-path #(not (msg-enm-grp-ext? %)) STAY
                                                                             grp? GROUP->FIELD)])]
                                       [(collect-one [ALL string?]) ALL-WITH-META (cond-path enm? STAY
                                                                                             ext? WALK-EXTEND
                                                                                             grp? WALK-GROUP
                                                                                             msg? WALK-MESSAGE)])])

(providepath GROUP->FIELD  [(putval :group-field) (collect-one (nthpath 1)) (collect-one (nthpath 2)) (collect-one (nthpath 2)) (collect-one (nthpath 3))
                            NONE]) ; NONE so this doesn't appear in recur path

(defn- unnest- [rast]
  (->> rast
       (select [ALL-WITH-META (collect-one FIRST) LAST ; collect key; nav to value
                (subselect [ALL-WITH-META WALK-TOP-LEVEL])])
       (into {})))

(defn- merge-path [form]
  (let [target (last form)
        path (clojure.string/join "." (drop-last form))
        merge-fn #(clojure.string/join "." [path %])]
    (cond (msg-enm? target) (transform (nthpath 1) merge-fn target)
          (grp?     target) (transform (nthpath 2) merge-fn target)
          (ext?     target) (transform [ALL-WITH-META field? (nthpath 3)] merge-fn target))))

(defn unnest [rast]
  (->> rast
       unnest-
       (transform [ALL-WITH-META LAST ALL-WITH-META
                   ; if first N items are string, they are path info found during `unnest`, hence use `merge-path` to merge into the last (i.e Nth + 1) item
                   (if-path #(string? (first %)) STAY)] merge-path)))

(defn- ->full-name [package name]
  (if (empty? package) name (str package "/" name)))

(defn- assoc-with [k v]
  (fn [old-val] (assoc (if (nil? old-val) (ordered-map) old-val) k v)))

(defn- mapify-content
  "Mapify rhs and conj into lhs and return result."
  [lhs rhs]
  (condp = (first rhs)
    ; [:enumField "ZERO" 0 nil]
    :enumField       (update lhs :enum-fields
                             (assoc-with (nth rhs 1) {:value (nth rhs 2)
                                                      :options (nth rhs 3)}))
    ; [:field :required :string "msg" 1 nil]
    :field           (update lhs :fields
                             (assoc-with (nth rhs 3) {:context (nth rhs 1)
                                                      :type (nth rhs 2)
                                                      :fid (nth rhs 4)
                                                      :options (nth rhs 5)}))
    ; [:field+ :required :string "msg" 1 nil]
    :field+           (update lhs :fields
                              (assoc-with (nth rhs 3) {:context (nth rhs 1)
                                                       :type (nth rhs 2)
                                                       :fid (nth rhs 4)
                                                       :is-extension true
                                                       :options (nth rhs 5)}))
    ; [:mapField :int32 :string "map_field" 2 nil]
    :mapField        (update lhs :fields
                             (assoc-with (nth rhs 3) {:context :map
                                                      :key-type (nth rhs 1)
                                                      :val-type (nth rhs 2)
                                                      :fid (nth rhs 4)
                                                      :options (nth rhs 5)}))
    ; [:oneof "identifier" [:oneofField :string "name" 3 nil] [:oneofField :int32 "id" 4 nil]]
    :oneof           (update lhs :fields
                             (assoc-with (nth rhs 1) (reduce mapify-content
                                                             {:context :oneof}
                                                             (drop 2 rhs))))
    ; [:oneofField :string "name" 3 nil]
    :oneofField      (update lhs :oneof-fields
                             (assoc-with (nth rhs 2) {:context :oneof-field
                                                      :type (nth rhs 1)
                                                      :fid (nth rhs 3)
                                                      :options (nth rhs 4)}))
    ;[:rpc "method3" :stream "nested.ReqXYZ" :stream "nested.RespXYZ" [[:option "deprecated" :true]]]
    :rpc             (update lhs :rpcs
                             (assoc-with (nth rhs 1) (reduce mapify-content
                                                             {:context :rpc
                                                              :request-spec (nth rhs 2)
                                                              :request (nth rhs 3)
                                                              :response-spec (nth rhs 4)
                                                              :response (nth rhs 5)}
                                                             (nth rhs 6))))
    ; [:option "allow_alias" :true]
    :option          (update lhs :options
                             #(conj (if (nil? %) [] %) [(nth rhs 1) (nth rhs 2)]))
    ; [:reserved-ranges 2 15 [9 11] [40 536870911]]
    :reserved-names  (update lhs :reserved-names
                             #(into (if (nil? %) [] %) (drop 1 rhs)))
    ; [:reserved-names "FOO" "BAR"]]]
    :reserved-ranges (update lhs :reserved-ranges
                             #(into (if (nil? %) [] %) (drop 1 rhs)))
    ; [:extensions [1000 536870911] ...]
    :extensions      (update lhs :extensions
                             #(into (if (nil? %) [] %) (drop 1 rhs)))))

(defn- mapify-enm|msg|svc [enm|msg|svc syntax package form]
  (let [name (->full-name package (second form))
        content (drop 2 form)]
    {name (into {:context enm|msg|svc,
                 :syntax syntax}
                (reduce mapify-content {} content))}))

(defn- mapify-ast
  [ast]
  (loop [idx 0, syntax "proto3", package "", options [], reg {}]
    (if (>= idx (count ast))
      (update-vals reg #(conj % {:file-options options})) ; duplicate field options into every enum, msg or svc entry
      (let [form (nth ast idx)]
        (condp = (first form)
          :syntax  (recur (inc idx) (last form)           package     options                      reg)
          :package (recur (inc idx) syntax                (last form) options                      reg)
          :option  (recur (inc idx) syntax                package     (conj options (drop 1 form)) reg)
          :message (recur (inc idx) syntax                package     options                      (conj reg (mapify-enm|msg|svc :message syntax package form)))
          :enum    (recur (inc idx) syntax                package     options                      (conj reg (mapify-enm|msg|svc :enum    syntax package form)))
          :service (recur (inc idx) syntax                package     options                      (conj reg (mapify-enm|msg|svc :service syntax package form)))
          (recur          (inc idx) syntax                package     options                      reg))))))

(defn mapify
  "Transforms the vector structure of the AST into a map of maps for ease of use. Input must be a registry of unnested
   ast (i.e. output of (rubberbuf.ast-postprocess/unnest (rubberbuf.core/protoc ...))). Output is a map of `name` =>
   `content`; `name` is a fully scoped name; `content` is an attribute-value map with a :context attribute guaranteed
   to be :message, :enum or :service, and has other attributes based on context."
  [unnested-rast]
  (->> unnested-rast
       vals
       (map mapify-ast)
       (reduce conj {})))
