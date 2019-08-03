(defproject project-with-aliases "0.1.0-SNAPSHOT"
  :aliases {"p" ["echo" "p"]
            "a2p" ["with-profile" "+a2" "p"]
            "pp" ["with-profile" "+a2" "echo" "pp"]
            "ppp" ["with-profile" "+a2" "echo" "ppp"]
            "echo" ["echo" "hello"]

            "project" ["project"]
            "projecta" ["project" "a"]

            "pa2project" ["with-profile" "+a2" "project"]
            "pa2projecta" ["with-profile" "+a2" "project" "a"]

            "a2project" ["with-profile" "a2" "project"]
            "a2projecta" ["with-profile" "a2" "project" "a"]}
  :a 1
  :profiles {:a2 {:aliases {"q" ["echo" "q"]
                            "inp-projecta" ["project" "a"]}
                  :a 2}})
