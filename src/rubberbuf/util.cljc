(ns rubberbuf.util)

(defn raise [err-txt]
  #?(:clj (throw (Exception. err-txt)))
  #?(:cljs (throw (js/Error err-txt))))

(defn loader [file]
  #?(:clj (try (slurp file)
               (catch Exception _ nil)))
  #?(:cljs (raise "loader not implemented")))
