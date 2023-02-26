(ns rubberbuf.util
  (:refer-clojure :exclude [slurp]))

(defn raise [err-txt]
  #?(:clj (throw (Exception. err-txt)))
  #?(:cljs (throw (js/Error err-txt))))

(defmacro slurp [file]
  (clojure.core/slurp file))

(defn loader [file]
  #?(:clj (try (clojure.core/slurp file)
               (catch Exception _ nil)))
  #?(:cljs (raise "loader not implemented")))
