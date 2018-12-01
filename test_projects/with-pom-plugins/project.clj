(defproject project-with-pom-plugins "0.1.0-SNAPSHOT"
  :pom-plugins [[two.parameter/simple-plugin "1.0.0"]
                [three.parameter/with-vec "1.0.1"
                 [:a 1 :a 2 :a 3]]
                [three.parameter/with-map "1.0.2"
                 {:a 1
                  :b 2
                  :c 3}]
                [three.parameter/with-list "1.0.3"
                 (:root
                   [:a 1]
                   [:b
                    [:c 2]
                    [:d 3]])]])
