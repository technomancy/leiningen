(ns reflector.classy
  (:gen-class :extends java.lang.RuntimeException))

(defn init [xyz] (.getBytes xyz))

