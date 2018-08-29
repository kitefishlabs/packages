(set-env!
  :resource-paths #{"resources"}
  :dependencies '[[cljsjs/boot-cljsjs "0.9.0"  :scope "test"]])

(require '[cljsjs.boot-cljsjs.packaging :refer :all]
         '[boot.core :as boot]
         '[boot.tmpdir :as tmpd]
         '[clojure.java.io :as io]
         '[boot.util :refer [sh]])

(def +lib-version+ "18.0.4")
(def +version+ (str +lib-version+ "-0"))

(task-options!
  pom {:project     'cljsjs/blockstack
       :version     +version+
       :description "Blockstack JS."
       :url         "https://blockstack.org"
       :scm         {:url "https://github.com/blockstack/blockstack.js"}
       :license     {"MIT" "http://opensource.org/licenses/MIT"}})

(deftask build-blockstack []
  (let [tmp (boot/tmp-dir!)]
    (with-pre-wrap
      fileset
      ;; Copy all files in fileset to temp directory
      (doseq [f (->> fileset boot/input-files)
              :let [target (io/file tmp (tmpd/path f))]]
        (io/make-parents target)
        (io/copy (tmpd/file f) target))
      (binding [boot.util/*sh-dir* (str (io/file tmp (format "blockstack-%s" +lib-version+)))]
        ((sh "npm" "install")))
      (-> fileset (boot/add-resource tmp) boot/commit!))))

(deftask package []
  (comp
    (download :url (str "https://github.com/blockstack/blockstack.js/archive/v" +lib-version+ ".zip")
              :unzip true)
    ; (build-blockstack)
    (sift :move {(re-pattern (str "^blockstack.js-" +lib-version+ "/dist/blockstack.js")) "cljsjs/blockstack/development/blockstack.inc.js"})
    ; (show :fileset true)
    ; (minify :in "cljsjs/development/blockstack.inc.js"
            ; :out "cljsjs/production/blockstack.min.js")
    (sift :include #{#"^cljsjs"})
    (deps-cljs :name "cljsjs.blockstack")
    (pom)
    (jar)))
