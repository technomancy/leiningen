;; You can write a 'new' task yourself without any extra plugins like
;; lein-newnew. What makes lein-new so useful is the `templates` task for
;; listing templates and this file. The primary problem with writing your
;; own project scaffolding tools that are domain-specific is you
;; generally have to reimplement the same things every single time. With
;; lein-new, you have this little library that your templates can use.
;; It has all the things a template is likely to need:
;; * an easy way to generate files and namespaces
;; * a way to render files written with a flexible template language
;; * a way to get those files off of the classpath transparently
(ns leiningen.new.templates
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.user :as user]
            [leiningen.core.main :as main]
            [stencil.core :as stencil])
  (:import (java.util Calendar)))

(defn project-name
  "Returns project name from (possibly group-qualified) name:

  mygroup/myproj => myproj
  myproj         => myproj"
  [s]
  (last (string/split s #"/")))

(defn fix-line-separators
  "Replace all \\n with system specific line separators."
  [s]
  (let [line-sep (if (user/getenv "LEIN_NEW_UNIX_NEWLINES") "\n"
                     (user/getprop "line.separator"))]
    (string/replace s "\n" line-sep)))

(defn slurp-to-lf
  "Returns the entire contents of the given reader as a single string. Converts
  all line endings to \\n."
  [r]
  (let [sb (StringBuilder.)]
    (loop [s (.readLine r)]
      (if (nil? s)
        (str sb)
        (do
          (.append sb s)
          (.append sb "\n")
          (recur (.readLine r)))))))

(defn slurp-resource
  "Reads the contents of a resource. Temporarily converts line endings in the
  resource to \\n before converting them into system specific line separators
  using fix-line-separators."
  [resource]
  (if (string? resource) ; for 2.0.0 compatibility, can break in 3.0.0
    (-> resource io/resource io/reader slurp-to-lf fix-line-separators)
    (-> resource io/reader slurp-to-lf fix-line-separators)))

(defn sanitize
  "Replace hyphens with underscores."
  [s]
  (string/replace s "-" "_"))

(defn multi-segment
  "Make a namespace multi-segmented by adding another segment if necessary.
  The additional segment defaults to \"core\"."
  ([s] (multi-segment s "core"))
  ([s final-segment]
     (if (.contains s ".")
       s
       (format "%s.%s" s final-segment))))

(defn name-to-path
  "Constructs directory structure from fully qualified artifact name:

  \"foo-bar.baz\" becomes \"foo_bar/baz\"

  and so on. Uses platform-specific file separators."
  [s]
  (-> s sanitize (string/replace "." java.io.File/separator)))

(defn sanitize-ns
  "Returns project namespace name from (possibly group-qualified) project name:

  mygroup/myproj  => mygroup.myproj
  myproj          => myproj
  mygroup/my_proj => mygroup.my-proj"
  [s]
  (-> s
      (string/replace "/" ".")
      (string/replace "_" "-")))

(defn group-name
  "Returns group name from (a possibly unqualified) name:

  my.long.group/myproj => my.long.group
  mygroup/myproj       => mygroup
  myproj               => nil"
  [s]
  (let [grpseq (butlast (string/split (sanitize-ns s) #"\."))]
    (if (seq grpseq)
      (->> grpseq (interpose ".") (apply str)))))

(defn year
  "Get the current year. Useful for setting copyright years and such."
  [] (.get (Calendar/getInstance) Calendar/YEAR))

;; It'd be silly to expect people to pull in stencil just to render a mustache
;; string. We can just provide this function instead. In doing so, it is much
;; less likely that template authors will have to pull in any external
;; libraries. Though they are welcome to if they need.
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
  file is simply slurped and the content returned unchanged.
  
  render-fn - Optional rendering function that will be used in place of the
              default renderer. This allows rendering templates that contain
              tags that conflic with the Stencil renderer such as {{..}}."
  [name & [render-fn]]
  (let [render (or render-fn render-text)]
    (fn [template & [data]]
    (let [path (string/join "/" ["leiningen" "new" (sanitize name) template])]
      (if-let [resource (io/resource path)]
        (if data
          (render (slurp-resource resource) data)
          (io/reader resource))
        (main/abort (format "Template resource '%s' not found." path)))))))

;; Our file-generating function, `->files` is very simple. We'd like
;; to keep it that way. Sometimes you need your file paths to be
;; templates as well. This function just renders a string that is the
;; path to where a file is supposed to be placed by a template.
;; It is private because you shouldn't have to call it yourself, since
;; `->files` does it for you.
(defn- template-path [name path data]
  (io/file name (render-text path data)))

(def ^{:dynamic true} *dir* nil)
(def ^{:dynamic true} *force?* false)

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
  It will be turned into a File regardless. Any parent directories will be
  created automatically. Data should include a key for :name so that the project
  is created in the correct directory."
  [{:keys [name] :as data} & paths]
  (let [dir (or *dir*
                (-> (System/getProperty "leiningen.original.pwd")
                    (io/file name) (.getPath)))]
    (if (or (= "." dir) (.mkdir (io/file dir)) *force?*)
      (doseq [path paths]
        (if (string? path)
          (.mkdirs (template-path dir path data))
          (let [[path content & options] path
                path (template-path dir path data)
                options (apply hash-map options)]
            (.mkdirs (.getParentFile path))
            (io/copy content (io/file path))
            (when (:executable options)
              (.setExecutable path true)))))
      (main/info (str "Could not create directory " dir
                      ". Maybe it already exists?"
                      "  See also :force or --force")))))
