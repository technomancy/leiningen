(ns leiningen.test.with-profile
  (:use clojure.test leiningen.with-profile))

(defn- prj-map
  ([p] (prj-map p [:default]))
  ([p a]
     (let [p {:profiles p}, m {:without-profiles p, :active-profiles a}]
       (with-meta p m))))

(deftest test-profiles-in-group
  (doseq [[project pgroup expected]
          [[(prj-map {}) "+foo" [:base :system :downstream :user :provided :dev :foo]]
           [(prj-map {:default [:base :dev]}) "+foo" [:base, :dev, :foo]]
           [(prj-map {:default [:base :dev]}) "-dev" [:base]]
           [(prj-map {:default [:base :dev]}) "-dev,+foo" [:base, :foo]]
           [(prj-map {:default [:base :dev]}) "-default,+foo" [:foo]]
           [(prj-map {:default [:base :dev], :foo [:bar :baz]})
            "-default,+foo" [:bar :baz]]
           [(prj-map {:default [:base :dev], :dev [:foo]
                      :foo [:bar :baz], :baz [:zap]})
            "-default,+foo" [:bar :zap]]
           ;; TODO: drop support for partially-composite profiles in 3.0
           [(prj-map {:default [:base :dev], :foo [:bar {:gross true}]})
            "-default,+foo" [:foo]]]]
    (is (= expected (profiles-in-group project pgroup))))
  (testing "no +/- prefixes in arg"
    (let [project (prj-map {:default [:base :dev] :foo [:bar :baz]
                            :bar [:one :two] :baz [:three :four]})]
      (doseq [[pgroup expected]
              [["foo" [:one :two :three :four]]
               ["bar" [:one :two]]
               ["baz" [:three :four]]
               ["bar,baz" [:one :two :three :four]]
               ["baz,bar" [:three :four :one :two]]]]
        (is (= expected (profiles-in-group project pgroup)))))))
