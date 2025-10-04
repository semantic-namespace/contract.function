(ns semantic-namespace.contract.function-test
  (:require [clojure.test :refer (deftest testing is)]
            [semantic-namespace.contract :as contract]
            [semantic-namespace.contract.function :as contract.function]))
  

(deftest function-test
  (testing "how to def(ine) functions: id, impl and optional AOP related logic... check the repl to see the printed ouput"
    (contract/def #{:semantic-namespace/function :my-app.auth/jwt}
      {:function.execution/after identity
       :function.execution/before
       (fn [{:keys [::contract.function/id ::contract.function/arg]}]
         (println "before executing " id arg))
       :function/impl
       (fn [{:keys [::contract.function/arg]}]
         (str "jwt value "  arg))})

    (contract/fetch  #{:semantic-namespace/function :my-app.auth/jwt})
    (is (= "jwt value foo" (contract.function/execute #{:semantic-namespace/function :my-app.auth/jwt} "foo")))))
