;; You can write a 'new' task yourself without any extra plugins like
;; lein-newnew. What makes newnew so useful is the `templates` task for
;; listing templates and this file. The primary problem with writing your
;; own project scaffolding tools that are domain-specific is tht you
;; generally have to reimplement the same things every single time. With
;; lein-newnew, you have this little library that your templates can use.
;; It has all the things a template is likely to need:
;; * an easy way to generate files and namespaces
;; * a way to render files written with a flexible template language
;; * a way to get those files off of the classpath transparently
(ns leiningen.new.templates
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [stencil.core :as stencil]))

(defn project-name
  "Returns project name from (possibly group-qualified) name:

   mygroup/myproj => myproj
   myproj         => myproj"
  [s]
  (last (string/split s #"/")))

(defn slurp-resource
  "Reads the contents of a file on the classpath."
  [resource-path]
  (-> resource-path io/resource io/reader slurp))

(defn sanitize
  "Replace hyphens with underscores."
  [s]
  (string/replace s #"-" "_"))

(defn name-to-path
  "Constructs directory structure from fully qualified artifact name:

   myproject         creates src/myproject/* directory
   mygroup.myproject creates src/mygroup/myproject/* directory

   and so on. Uses platform-specific file separators."
  [s]
  (-> s sanitize (string/replace #"\." java.io.File/separator)))

(defn sanitize-ns
  "Returns project namespace name from (possibly group-qualified) project name:

   mygroup/myproj  => mygroup.myproj
   myproj          => myproj
   mygroup/my_proj => mygroup.my-proj"
  [s]
  (-> s
      (string/replace #"/" ".")
      (string/replace #"_" "-")))

(defn year
  "Get the current year. Useful for setting copyright years and such."
  [] (+ (.getYear (java.util.Date.)) 1900))

;; It'd be silly to expect people to pull in stencil just to render
;; a mustache string. We can just provide this function instead. In
;; doing so, it is much less likely that a template author will have
;; to pull in any external libraries. Though he is welcome to if he
;; needs.
(def render-text stencil/render-string)

;; Templates are expected to store their mustache template files in
;; `leiningen/new/<template>/`. We have our convention of where templates
;; will be on the classpath but we still have to know what the template's
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
    (let [path (string/join "/" ["leiningen" "new" (sanitize name) template])]
      (if data
        (render-text (slurp-resource path) data)
        (io/reader (io/resource path))))))

;; Our file-generating function, `->files` is very simple. We'd like
;; to keep it that way. Sometimes you need your file paths to be
;; templates as well. This function just renders a string that is the
;; path to where a file is supposed to be placed by a template.
;; It is private because you shouldn't have to call it yourself, since
;; `->files` does it for you.
(defn- template-path [name path data]
  (io/file name (render-text path data)))

(def ^{:dynamic true} *dir* nil)

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
  (let [dir (or *dir* name)]
    (if (or *dir* (.mkdir (io/file dir)))
      (doseq [path paths]
        (if (string? path)
          (.mkdirs (template-path dir path data))
          (let [[path content] path
                path (template-path dir path data)]
            (.mkdirs (.getParentFile path))
            (io/copy content (io/file path)))))
      (println "Could not create directory " dir ". Maybe it already exists?"))))
