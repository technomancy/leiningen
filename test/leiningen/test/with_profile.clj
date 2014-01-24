(ns leiningen.test.with-profile
  (:use clojure.test leiningen.with-profile))

(defn- prj-map
  ([p] (prj-map p [:default]))
  ([p a]
     (let [p {:profiles p}, m {:without-profiles p, :active-profiles a}]
       (with-meta p m))))

(deftest test-profiles-in-group
  (doseq [[project pgroup expected]
          [[(prj-map {:default [:base :dev]}) "+foo" [:base, :dev, :foo]]
           [(prj-map {:default [:base :dev]}) "-dev" [:base]]
           [(prj-map {:default [:base :dev]}) "-dev,+foo" [:base, :foo]]
           [(prj-map {:default [:base :dev]}) "-default,+foo" [:foo]]
           [(prj-map {:default [:base :dev], :foo [:bar :baz]})
            "-default,+foo" [:bar :baz]]
           ;; TODO: drop support for partially-composite profiles in 3.0
           [(prj-map {:default [:base :dev], :foo [:bar {:gross true}]})
            "-default,+foo" [:foo]]]]
    (is (= expected (profiles-in-group project pgroup)))))
