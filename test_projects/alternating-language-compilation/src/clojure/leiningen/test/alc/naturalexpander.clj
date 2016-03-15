;; SPOILER ALERT: This file implements a solution to Project Euler #65,
;; Convergence of e. (https://projecteuler.net/problem=65) If you intend
;; to solve this problem on your own, don't pay too much attention to
;; what the code is doing.

(ns leiningen.test.alc.naturalexpander
  "Expands a repeated fraction representing the natural number, \"e\"."
  (:import leiningen.test.alc.WrappedOnesIterator
           clojure.lang.Ratio))

(definterface INaturalExpander
  (^java.math.BigInteger getNumerator [])
  (^java.math.BigInteger getDenominator [])
  (expandBy [fractions]))

;; WrappedOnesIterator isn't thread-safe, so we need to make a new one
;; every time as a workaround
(defn natural-fraction-schedule []
  (cons 2
        (iterator-seq
         (WrappedOnesIterator. (.iterator (map #(* % 2) (drop 1 (range))))))))

(deftype NaturalExpander [^:unsynchronized-mutable return-value]
  INaturalExpander
  (^java.math.BigInteger getNumerator [self] (numerator return-value))
  (^java.math.BigInteger getDenominator [self] (denominator return-value))
  (expandBy [self fractions]
    (let [fraction-sched (reverse (take fractions (natural-fraction-schedule)))]
      (if (< (count fraction-sched) 2)
        (set! return-value (Ratio. (first fraction-sched) 1))
        (loop [previous (first fraction-sched)
               pending  (rest fraction-sched)]
          (if (empty? pending)
            (set! return-value previous)
            (recur (+ (/ 1 previous) (first pending)) (rest pending))))))))
