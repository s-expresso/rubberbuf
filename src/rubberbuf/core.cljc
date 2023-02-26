(ns rubberbuf.core
  (:require
   [com.rpl.specter :refer [ALL LAST select]]
   [rubberbuf.ast-preprocess :refer [normalize]]
   [rubberbuf.ast-util :refer [import?]]
   [rubberbuf.parse :refer [parse]]
   [rubberbuf.util :refer [loader raise]]))

(defn- protoc-
  [paths files loader registry]
  (let [asts (for [file files]
               (do (println "Compiling" file)
                   (let [pb-text (some #(loader (str % file)) paths)]
                     (if (nil? pb-text)
                       (raise (str "File not found: " file))
                       (parse pb-text)))))
        imports (select [ALL ALL import? LAST] asts)
        registry (conj registry (zipmap files asts))
        new-files (clojure.set/difference (set imports) (set (keys registry)))]
    (if (empty? new-files) registry
        (protoc- paths new-files loader registry)))) ; recur


(defn protoc
  "Protobuf compiler that returns a registry of {file => AST}, recursively compiling for all imports too so that references can be resolved.
   
   @paths: paths to search for files, e.g. ['/home/user/my-pb/', '/opt/google/protobuf/src/', 'http://my.website.com/protobuf/']
   @files: protobuf file names, e.g. ['descriptions.proto', 'my-proto.proto', 'dir1/dir2/my-proto.proto']"
  [paths files]
  (let [rast (protoc- paths files loader {})]
    (println "Normalizing registry of AST")
    (normalize rast)))
