(ns rubberbuf.util
  #?(:cljs (:require [cljs-node-io.core :as io])))

(defn raise [err-txt]
  #?(:clj (throw (Exception. err-txt)))
  #?(:cljs (throw (js/Error err-txt))))

(defn loader [file]
  #?(:clj (try (slurp file)
               (catch Exception _ nil)))
  #?(:cljs (try (io/slurp file)
                (catch js/Error _ nil))))
