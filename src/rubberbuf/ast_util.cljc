(ns rubberbuf.ast-util)

(defn- seq-type-of? [form & args]
  (and (seqable? form) (not-empty form) ((set args) (first form))))

(defn msg-enm-grp-pkg-ext? [form] (seq-type-of? form :message :enum :group :package :extend))
(defn msg-enm-grp-ext? [form] (seq-type-of? form :message :enum :group :extend))
(defn msg-enm? [form]         (seq-type-of? form :message :enum))
(defn msg? [form]             (seq-type-of? form :message))
(defn grp? [form]             (seq-type-of? form :group))
(defn enm? [form]             (seq-type-of? form :enum))
(defn ext? [form]             (seq-type-of? form :extend))
(defn pkg? [form]             (seq-type-of? form :package))
(defn import? [form]          (seq-type-of? form :import))
(defn msg-enm-grp? [form]     (seq-type-of? form :message :enum :group))
(defn unnestable? [form] (not (seq-type-of? form :message :enum :extend)))
(defn field? [form]           (seq-type-of? form :field))
(defn map-field? [form]       (seq-type-of? form :mapField))
(defn rpc? [form]             (seq-type-of? form :rpc))

(defn starts-with? [x & {:keys [num-element]}] #(and (seqable? %)
                                                     (if (fn? x) (x (first %)) (= (first %) x))
                                                     (or (nil? num-element) (= (count %) num-element))))

(defn dot-split [text] (if (empty? text) [] (clojure.string/split text #"\.")))

(defn slashdot-join [parts]
  (let [pkg (first parts)
        path (clojure.string/join "." (drop 1 parts))]
    (if (empty? pkg) path (clojure.string/join "/" [pkg path]))))

(defn ref-by?
  "Test if `subject` (full path msg) is a valid reference for `ref-target` (msg name) defined in `ref-from` (package)
   return true if subject's RHS == ref-target && (subject - ref-target) == ref-from's LHS.
   Example: ref-from:      [a b c d e x y]
            ref-target:    [x y Msg1]
            subject:       [a b c x y Msg1]         => true
            subject:       [a b c y Msg1]           => false (subject's RHS != ref-target)
            subject:       [a b c d e x y x y Msg1] => true
            subject:       [a b t x y Msg1]         => false ((subject - ref-target) != ref-from's LHS)
            subject:       [a b c d e x y Msg1]     => true"
  [ref-from ref-target subject]
  (let [subject-lhs-len (- (count subject) (count ref-target))
        subject-lhs (take subject-lhs-len subject)
        subject-rhs (drop subject-lhs-len subject)
        ref-from-lhs (take (count subject-lhs) ref-from)]
    (and (= subject-rhs ref-target)
         (= subject-lhs ref-from-lhs))))

