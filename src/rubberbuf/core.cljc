(ns rubberbuf.core
  (:require
   [clojure.core :refer [tap>]]
   [com.rpl.specter :refer [ALL LAST select]]
   [rubberbuf.ast-preprocess :as preprocess]
   [rubberbuf.ast-util :refer [import?]]
   [rubberbuf.parse :refer [parse]]
   [rubberbuf.util :refer [loader raise]]))

(defn- protoc-
  [paths files loader auto-import registry]
  (let [asts (for [file files]
               (do (tap> (str "Compiling " file))
                   (let [pb-text (some #(loader (str % file)) paths)]
                     (if (nil? pb-text)
                       (raise (str "File not found: " file))
                       (parse pb-text)))))
        registry (conj registry (zipmap files asts))
        imports #(select [ALL ALL import? LAST] asts) ; wrap in lambda to defer execution
        new-files (if (true? auto-import)
                    (clojure.set/difference (set (imports)) (set (keys registry)))
                    [])]
    (if (empty? new-files) registry
        (protoc- paths new-files loader auto-import registry)))) ; recur


(defn protoc
  "Protobuf compiler that returns a registry of {file => AST}. Sample usage:   
   ```
   (def paths ['/home/user/my-pb/', '/opt/google/protobuf/src/'])

   (def files ['file1.proto', 'file2.proto'])

   (protoc paths files)  ;; same as (protoc paths files :auto-import true :normalize true)
   ;; {'file1.proto' [...], 'file2.proto' [...]}
   ```

   Arguments:
   paths - paths to search for files (and imports found during compilation if :auto-import is true)
   files - protobuf file names
   
   Optional Keyword Arguments:
   :auto-import - automatically and recursively crawl and compile all import files; default true
   :normalize   - (1) resolve field of message/enum type into its fully qualified name (e.g. message => my.package/nested.message)
                  (2) inject extend fields into target message as :field+
                  default true
   Using `:auto-import false` with `:normalize true` is not recommended as it causes resolution of qualified name to fail."
  [paths files & {:keys [auto-import normalize] :or {auto-import true normalize true}}]
  (let [rast (protoc- paths files loader auto-import {})]
    (if (true? normalize)
      (do
        (tap> "Normalizing registry of AST")
        (preprocess/normalize rast))
      rast)))
