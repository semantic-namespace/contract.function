(ns semantic-namespace.contract.function-test
  (:require [clojure.test :refer (deftest testing is)]
            [semantic-namespace.contract.function :as contract.function]))

(contract.function/def :my-app.auth/jwt
  {:function.execution/after identity
   :di.component/deps []
   :function/context [::contract.function/id ::contract.function/arg]
   :function/response [::contract.function/res]
   :function.execution/before
   (fn [{:keys [::contract.function/id ::contract.function/arg]}]
     (println "before executing " id arg))
   :function/impl
   (fn [{:keys [::contract.function/arg]}]
     (str "jwt value "  arg))})

(deftest function-test
  (testing "how to def(ine) functions: id, impl and optional AOP related logic... check the repl to see the printed ouput"
    (contract.function/fetch :my-app.auth/jwt)
    (is (= "jwt value foo" (contract.function/execute #{:semantic-namespace/function :my-app.auth/jwt} "foo")))))
