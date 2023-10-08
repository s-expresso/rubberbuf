(ns rubberbuf.ast-preprocess
  (:require [clojure.walk :refer [postwalk]]
            [com.rpl.specter :refer [collect collect-one cond-path declarepath if-path multi-path must nthpath providepath putval recursive-path select setval srange-dynamic subselect transform transformed
                                     ALL ALL-WITH-META END FIRST INDEXED-VALS LAST MAP-VALS NONE STAY STOP]]
            [rubberbuf.ast-util :refer [dot-split ext? field? import? grp? map-field? msg? msg-enm-grp-pkg-ext? oneof? oneof-field? pkg? ref-by? rpc? slashdot-join starts-with?]]
            [rubberbuf.util :refer [raise]]))

;; ------------------------------------------------------------------------------------------------
;; Extract referables into a data structure that is easy for look up
;; ------------------------------------------------------------------------------------------------

; specter recursive navigator
(def FIND-REF-WITH-PATH-INDEX
  (recursive-path [] p
                  (if-path sequential?
                           [INDEXED-VALS (collect-one FIRST) LAST
                            (cond-path msg-enm-grp-pkg-ext?
                                       [(collect-one FIRST)
                                      ; :group name is at position 2 but shifted to position 1 if unnested, so we test for string
                                        (if-path #(string? (second %))
                                                 (collect-one (nthpath 1))
                                                 (collect-one (nthpath 2)))
                                        (subselect [p])])])))

(defn rast->referables- [rast]
  (select [ALL (collect-one FIRST) LAST (subselect [FIND-REF-WITH-PATH-INDEX])] rast))

(defn rast->referables [rast]
  (->> rast
       rast->referables-
       (postwalk #(cond
                    ;; ((starts-with? string? :num-element 5) %)
                    ;; (into (subvec % 0 4) (nth % 4))
                    ((starts-with? number? :num-element 4) %)
                    (into (subvec % 0 3) (nth % 3))
                    :else %))))

(defn- is-meg? [form] (and (sequential? form) ((set [:message :group :enum]) (second form))))
(defn- is-ext? [form] (and (sequential? form) ((set [:extend])               (second form))))

; mutually recursive navigator to extract message/group/enum, including those nested within extend
(declarepath EXTRACT-REFERRABLES)
(declarepath NAV-THRU-EXTENDS)

(providepath EXTRACT-REFERRABLES [ALL (cond-path is-meg?
                                                 [(collect-one FIRST) (collect-one (nthpath 2)) EXTRACT-REFERRABLES]
                                                 is-ext?
                                                 [(collect-one FIRST) NAV-THRU-EXTENDS]
                                                 [string?]
                                                 NONE)])

(providepath NAV-THRU-EXTENDS [ALL (cond-path is-meg?
                                              [(collect-one FIRST) (collect-one (nthpath 2)) EXTRACT-REFERRABLES]
                                              is-ext?
                                              [(collect-one FIRST) NAV-THRU-EXTENDS])])


(defn referables->lookup [referables]
  (let [u-ref (select [ALL (collect-one FIRST) ; file name
                       (nthpath 1)
                       (if-path #(and (> (count %) 0) (= :package (second (first %))))
                                [(collect-one (nthpath 0 2)) EXTRACT-REFERRABLES]
                                [(putval "") EXTRACT-REFERRABLES])] referables)
        path (select [ALL (collect-one FIRST)
                      (srange-dynamic (fn [_] 1) #(count %))
                      (subselect [ALL string?])] u-ref)
        idx (select [ALL (subselect [ALL number?])] u-ref)]
    (group-by #(last (second %)) (map conj path idx))))


;; ------------------------------------------------------------------------------------------------
;; Extract extends from referables into a data structure that is easy for look up
;; ------------------------------------------------------------------------------------------------

; mutually recursive navigator to extract extend, including those nested within message/group/enum
(declarepath NAV-THRU-REFERRABLES)
(declarepath EXTRACT-EXTENDS)

(providepath NAV-THRU-REFERRABLES [ALL (cond-path is-meg?
                                                  [(collect-one FIRST) (collect-one (nthpath 2)) NAV-THRU-REFERRABLES]
                                                  is-ext?
                                                  [(collect-one FIRST) EXTRACT-EXTENDS])])
(providepath EXTRACT-EXTENDS [ALL (cond-path is-meg?
                                             [(collect-one FIRST) (collect-one (nthpath 2)) NAV-THRU-REFERRABLES]
                                             is-ext?
                                             [(collect-one FIRST) EXTRACT-EXTENDS]
                                             string?
                                             [STAY])])

(defn referables->extends [referables]
  (let [u-ref (select [ALL (collect-one FIRST) ; file name
                       (nthpath 1)
                       (if-path #(and (> (count %) 0) (= :package (second (first %))))
                                [(collect-one (nthpath 0 2)) EXTRACT-EXTENDS]
                                [(putval "") EXTRACT-EXTENDS])] referables)
        path (select [ALL (collect-one FIRST)
                      (srange-dynamic (fn [_] 1) #(count %))
                      (subselect [ALL string?])] u-ref)
        idx (select [ALL (subselect [ALL number?])] u-ref)]
    #_(group-by #(-> % second last dot-split last) (map conj path idx))
    (map conj path idx)))


(defn find-targets
  "@param lookup: output of rast->lookup\n
   @param package: e.g \"a.b.c.d\"\n
   @param target: e.g. [\"MsgA\" \"MsgB\" \"c.d.MsgX.Message\"]"
  [lookup package target]
  (if (clojure.string/starts-with? (last target) ".") ; fully qualified message name
    (let [target-nsname (dot-split (last target))     ; [c d MsgX Message]
          name-matches (get lookup (last target-nsname))
          targets (filter #(= (drop 1 target-nsname)
                              (into (-> % (nth 1) (nth 0) dot-split) ; package
                                    (->> (nth % 1) (drop 1)))) name-matches)]
      targets)
    (let [target-nsname (dot-split (last target)) ; [c d MsgX Message]
          target-nest-path (drop-last target)     ; [MsgA MsgB]
          package (dot-split package)             ; [a b c d]
          package+target-nest-path (into package target-nest-path) ; [a b c d MsgA MsgB]
          name-matches (get lookup (last target-nsname))
          targets (filter #(ref-by? package+target-nest-path target-nsname
                                    (into (-> % (nth 1) (nth 0) dot-split) ; package
                                          (->> (nth % 1) (drop 1))))
                          name-matches)]
      targets)))

(defn find-target
  "@param lookup/package/target: see find-targets
   @param includes: files to find target"
  [lookup package target includes]
  (let [targets (find-targets lookup package target)
        in-includes? (fn [x] (some #(= % x) includes))
        candidates (select [ALL #(in-includes? (first %))] targets)]
    (condp = (count candidates)
      0 (raise (str "Cannot find a viable target: " target))
      1 (first candidates)
      (reduce #(if (>= (count (second %1)) (count (second %2))) %1 %2) candidates))))

(defn rast-find-extend-target
  "@param ast: output of crawl\n
   @param lookup: output of rast->lookup\n
   @param extend: each item in output of rast->extends"
  [rast lookup extend]
  (let [filename (first extend)
        imports (select [(must filename) ALL import? LAST] rast)
        includes (into [filename] imports)
        package (->> extend second first)
        target  (->> extend second (drop 1))]
    (find-target lookup package target includes)))

(defn rast->msg-fields [rast filename path field-lhs]
  (select [(must filename) (apply nthpath path) ALL #(and (vector? %) (= (first %) :field))
           (transformed FIRST (fn [_] :field+))
           (transformed (nthpath 3) #(slashdot-join (conj field-lhs %)))] rast))

(defn ast-merge-extend [rast lookup extend-msg]
  (let [[ext-filename ext-path] [(first extend-msg) (last extend-msg)]
        ext-msg-field-lhs (->> extend-msg second drop-last (into []))
        ext-msg-fields (rast->msg-fields rast ext-filename ext-path ext-msg-field-lhs)
        extend-target (rast-find-extend-target rast lookup extend-msg)
        [tar-filename tar-path] [(first extend-target) (last extend-target)]]
    (setval [(must tar-filename) (apply nthpath tar-path) END] ext-msg-fields rast)))

(defn rast->rast-extended [ast lookup extends]
  (reduce #(ast-merge-extend %1 lookup %2) ast extends))

(def FIND-MSG-FIELDS
  (recursive-path [] p
                  (cond-path msg? [(collect-one (nthpath 1)) ALL-WITH-META p]
                             grp? [(collect-one (nthpath 2)) ALL-WITH-META p]
                             oneof? [ALL-WITH-META p]
                             oneof-field? [(nthpath 1) string? STAY]
                             field? [(nthpath 2) string? STAY]
                             map-field? [(nthpath 2) string? STAY]
                             rpc? [(multi-path [(nthpath 2) string? STAY]
                                               [(nthpath 3) string? STAY])]
                             sequential? [ALL-WITH-META p])))

(def PATH-TO-FIELD-MSG [ALL-WITH-META (collect-one FIRST) ; filename
                        LAST ; ast
                        (collect [ALL import? LAST]) ; imports
                        (collect-one [ALL pkg? (nthpath 1)]) ; package
                        FIND-MSG-FIELDS])

(defn rast->rast-resolved [rast lookup]
  (let [fmsg->full-path-fmsg (fn [file imports pkg & nested-msg]
                               (let [includes (into [file] imports)
                                     target (find-target lookup pkg nested-msg includes)]
                                 (slashdot-join (second target))))]
    (transform [PATH-TO-FIELD-MSG] fmsg->full-path-fmsg rast)))

(defn remove-extends
  "Walk `rast` and removes all [:extend ...]"
  [rast]
  (let [FIND-EXTENDS (recursive-path [] p [(cond-path ext? STAY
                                                      sequential? [ALL-WITH-META p])])]
    (setval [MAP-VALS FIND-EXTENDS] NONE rast)))

(defn normalize [rast]
  (let [referables (rast->referables rast)
        lookup     (referables->lookup referables)
        extends    (referables->extends referables)
        rast-r     (rast->rast-resolved rast lookup)
        rast-re    (rast->rast-extended rast-r lookup extends)
        rast2      (remove-extends rast-re)]
    rast2))
