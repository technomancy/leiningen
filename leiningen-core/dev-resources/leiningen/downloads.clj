(use '[cemerick.pomegranate :only (add-dependencies)])

(add-dependencies :coordinates '[[clj-aws-s3 "0.3.6"]
                                 [tentacles "0.2.4"]]
                  :repositories (merge cemerick.pomegranate.aether/maven-central
                                       {"clojars" "http://clojars.org/repo"}))

(ns leiningen.downloads
  "Calculate download statistics from logs."
  (:require [aws.sdk.s3 :as s3]
            [clojure.java.io :as io]
            [tentacles.repos :as repo]
            [clojure.pprint :refer [pprint]]
            [leiningen.core.main :as main])
  (:import (java.io File)))

(defn ^:internal aws-cred []

  ;; in order to run, you need to define a map with the appropriate AWS
  ;; credentials in ~/.secrets/leiningen_downloads_aws_cred.clj:

  ;; {:access-key "AWS_ACCESS_KEY"
  ;;  :secret-key "AWS_SECRET_KEY"}
  (let [f (File. (System/getenv "HOME")
                 "/.secrets/leiningen_downloads_aws_cred.clj")]
    (if (.exists f)
      (read-string (slurp f))
      (main/abort "Missing credentials file:" f))))

(defn- list-all-objects
  [bucket & [objects next-marker]]
  (let [response (s3/list-objects (aws-cred) bucket {:marker next-marker})
        truncated? (:truncated? response)
        next-marker (:next-marker response)
        objects (concat objects (:objects response))]
    (if (not truncated?)
      objects
      (recur bucket [objects next-marker]))))

(defn- fetch-all-objects
  [bucket]
  (for [object (list-all-objects bucket)]
    (do
      (main/info (str "Processing: " (:key object)))
      (s3/get-object (aws-cred) bucket (:key object)))))

(defn- file-for-line
  [line]
  (let [[_ file] (re-find #"\"GET ([^ ]+) " line)]
    (if file
      (last (.split file "/")))))

(defn- ip-for-line
  [line]
  (re-find #"\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b" line))

(defn- status-for-line
  [line]
  (second (re-find #"\" (\d\d\d)" line)))

(defn- parse-files
  [content]
  (with-open [rdr (io/reader content)]
    (doall (for [line (line-seq rdr)]
             {:file (file-for-line line)
              :status (status-for-line line)
              :ip (ip-for-line line)}))))

(defn- s3-downloads
  []
  (flatten
   (for [logfile (map :content (fetch-all-objects "leiningen-logs"))]
     (filter #(and (get % :file) ;; file is present
                   (re-find #"\.jar\b" (get % :file)) ;; file is a jar
                   (= "200" (get % :status))) ;; and only HTTP 200 responses
             (parse-files logfile)))))

(defn- github-downloads
  []
  (reverse
   (sort-by #(first (vals %))
            (filter #(re-find #"\.jar$" (first (keys %)))
                    (let [downloads {}]
                      (for [download (repo/downloads "technomancy" "leiningen")]
                        (assoc downloads
                          (:name download)
                          (:download_count download))))))))

(defn ^:no-project-needed downloads [project]
  (let [s3-downloads (s3-downloads)
        s3-download-count (count s3-downloads)
        github-downloads (github-downloads)
        github-download-count
        (reduce + (map #(first (vals %)) github-downloads))]
    (println (str "GitHub Downloads: " github-download-count))
    (println (str "S3 Downloads: " s3-download-count))
    (println (str "Unique IP Addresses (S3 Downloads Only): "
                  (count (distinct (map :ip s3-downloads)))))
    (println (str "Total Downloads: "
                  (+ github-download-count s3-download-count)))
    (print "\n\n")
    (println "GitHub downloads by file:")
    (print "\n\n")
    (pprint github-downloads)
    (print "\n\n")
    (println "S3 downloads by file:")
    (print "\n\n")
    (pprint (frequencies (map :file s3-downloads)))
    (println ""))) ;; need this last println for some reason or else
;; the above doesn't print out using lein run...
