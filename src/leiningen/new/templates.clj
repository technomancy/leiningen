;; This API provides: 
;; * an easy way to generate files and namespaces
;; * a way to render files written with a flexible template language
;; * a way to get those files off of the classpath transparently
(ns leiningen.new.templates
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [stencil.core :as stencil]))

;; It is really easy to get resources off of the classpath in Clojure
;; these days.
(defn slurp-resource
  "Reads the contents of a file on the classpath."
  [resource-name]
  (-> resource-name .getPath io/resource io/reader slurp))

;; This is so common that it really is necessary to provide a way to do it
;; easily.
(defn sanitize
  "Replace hyphens with underscores."
  [s]
  (string/replace s #"-" "_"))

;; It'd be silly to expect people to pull in stencil just to render
;; a mustache string. We can just provide this function instead. In
;; doing so, it is much more likely that a template author will have
;; to pull in any external libraries. Though he is welcome to if he
;; needs.
(def render-text stencil/render-string)

;; Templates are expected to store their mustache template files in
;; `leiningen/new/<template>/`. We have our convention of where templates
;; will be on the classpath  but we still have to know what the template's
;; name is in order to know where this directory is and thus where to look
;; for mustache template files. Since we're likely to be rendering a number
;; of templates, we don't want to have to pass the name of the template every
;; single time. We've also avoided magic so far, so a dynamic var and accompanying
;; macro to set it is not in our game plan. Instead, our function for rendering
;; templates on the classpath will be a function returned from this higher-order
;; function. This way, we can say the name of our template just once and our
;; render function will always know.
(defn renderer
  "Create a renderer function that looks for mustache templates in the
   right place given the name of your template. If no data is passed, the
   file is simply slurped and the content returned unchanged."
  [name]
  (fn [template & [data]]
    (let [text (slurp-resource (io/file "leiningen" "new" name template))]
      (if data
        (render-text text data)
        text))))

;; Our file-generating function, `->files` is very simple. We'd like
;; to keep it that way. Sometimes you need your file paths to be
;; templates as well. This function just renders a string that is the
;; path to where a file is supposed to be placed by a template.
;; It is private because you shouldn't have to call it yourself, since
;; `->files` does it for you.
(defn- template-path [name path data]
  (io/file name (render-text path data)))

;; A template, at its core, is meant to generate files and directories that
;; represent a project. This is our way of doing that. `->files` is basically
;; a mini-DSL for generating files. It takes your mustache template data and
;; any number of vectors or strings. It iterates through those arguments and
;; when it sees a vector, it treats the first element as the path to spit to
;; and the second element as the contents to put there. If it encounters a
;; string, it treats it as an empty directory that should be created. Any parent
;; directories for any of our generated files and directories are created
;; automatically. All paths are considered mustache templates and are rendered
;; with our data. Of course, this doesn't effect paths that don't have templates
;; in them, so it is all transparent unless you need it.
(defn ->files
  "Generate a file with content. path can be a java.io.File or string.
   It will be turned into a File regardless. Any parent directories will
   be created automatically. Data should include a key for :name so that
   the project is created in the correct directory"
  [{:keys [name] :as data} & paths]
  (if (.mkdir (io/file name))
    (doseq [path paths]
      (if (string? path)
        (.mkdirs (template-path name path data))
        (let [[path content] path
              path (template-path name path data)]
          (.mkdirs (.getParentFile path))
          (spit path content))))
    (println "Directory" name "already exists!")))
