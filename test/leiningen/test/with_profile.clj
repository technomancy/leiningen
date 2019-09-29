(ns leiningen.test.with-profile
  (:require [clojure.test :refer :all]
            [leiningen.core.main :as main]
            [leiningen.test.helper
             :refer [with-aliases-project
                     with-aliases2-project]
             :as lthelper]
            [leiningen.with-profile :refer :all]))

(defn- prj-map
  ([p] (prj-map p [:default]))
  ([p a]
   (let [p {:profiles p}
         m {:without-profiles p, :active-profiles a
            :profiles (:profiles p)}]
     (with-meta p m))))

(deftest test-profiles-in-group
  (doseq [[project pgroup expected]
          [[(prj-map {}) "+foo" [:base :system :user :provided :dev :foo]]
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

(deftest default-project-with-profiles
  (testing "outside project directory"
    (let [project (main/default-project)]
      (is (= nil (main/resolve-and-apply project ["project" "a"]))
          "base project")
      (testing "with a profile"
        (let [project (vary-meta project assoc-in [:profiles :a2] {:a 2})]
          (is (= [2]
                 (main/resolve-and-apply project
                                         ["with-profile" "+a2" "project" "a"]))
              "applies profile"))))))

(deftest with-profiles-aliases-test
  (testing "recursive aliases"
    (testing "no with-profile"
      (is (= 1 (main/resolve-and-apply with-aliases-project ["project" "a"]))
          "recursive alias")
      (is (= 1 (main/resolve-and-apply with-aliases-project ["projecta"]))
          "two level alias"))

    (testing "with-profile added profile"
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["with-profile" "+a2" "project" "a"]))
          "recursive partial alias")
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["with-profile" "+a2" "projecta"]))
          "recursive full alias"))

    (testing "with-profile set profile"
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["with-profile" "a2" "project" "a"]))
          "recursive partial alias")
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["with-profile" "a2" "projecta"]))
          "recursive full alias"))

    (testing "with-profile added in alias"
      (is (= [2]
             (main/resolve-and-apply with-aliases-project ["pa2project" "a"]))
          "recursive partial alias")
      (is (= [2]
             (main/resolve-and-apply with-aliases-project ["pa2projecta"]))
          "recursive full alias"))

    (testing "with-profile set in alias"
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["a2project" "a"]))
          "recursive partial alias")
      (is (= [2] (main/resolve-and-apply with-aliases-project ["a2projecta"]))
          "recursive full alias"))

    (testing "with alias in profile"
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["with-profile" "+a2" "inp-projecta"]))
          "with profile added")
      (is (= [2] (main/resolve-and-apply
                  with-aliases-project ["with-profile" "a2" "inp-projecta"]))
          "with profile set")))

  (testing "recursive across profiles"
    (is (= [2] (main/resolve-and-apply with-aliases2-project ["project" "a"]))
        "partial alias from user profile")
    (is (= [[2]] (main/resolve-and-apply with-aliases2-project ["projecta"]))
        "full alias from user profile")
    (is (= [2] (main/resolve-and-apply
                with-aliases2-project ["project-set" "a"]))
        "partial alias from user profile")
    ;; this should probably be [[2]]
    (is (= [2] (main/resolve-and-apply with-aliases2-project ["projecta-set"]))
        "full alias from user profile")))
