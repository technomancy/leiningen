(defproject autoprofiles "0.0.1"
  :description "Test automatic activation of profiles via :active? expression"

  :profiles {

             :no_activation {:no_activation true }

             :literal_false {:active? false
                             :literal_false true }

             :literal_true {:active? true
                            :literal_true true }

             :expression_true {:active? (< 1 2)
                               :expression_true true }

             :expression_false {:active? (> 1 2)
                                :expression_false true }

             :fn_false {:active? (fn [project] (not (= (:name project) "autoprofiles" ))  )
                        :fn_false true }

             :fn_true {:active? (fn [project] (= (:name project) "autoprofiles" )  )
                       :fn_true true } } )
