(ns rubberbuf.ast-postprocess
  (:require [com.rpl.specter :refer [collect-one cond-path declarepath if-path multi-path nthpath providepath putval select  subselect transform
                                     ALL ALL-WITH-META FIRST LAST NONE STAY]]
            [rubberbuf.ast-util :refer [enm? ext? field? grp? msg? msg-enm? msg-enm-grp-ext?]]))

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
