(ns semantic-namespace.contract.function
  (:require [clojure.spec.alpha :as s]
            [semantic-namespace.contract.docs]
            [semantic-namespace.contract.type :as contract.type]
            [semantic-namespace.contract :as contract]))

(def ^:dynamic context {:function/env :env/dev})

(s/def :function/impl fn?)

(s/def :function.execution/before fn?)

(s/def :function.execution/after fn?)

(contract/def #{:function/impl :semantic-namespace/docs}
  {:docs/content
   (format "a function that will have as args %s"
           [::args
            ::context
            ::arg
            ::id])})

(contract/def #{:semantic-namespace/docs :function.execution/before}
  {:docs/content
   (format "an optional function logic to be executed before %s with this args %s" :function/impl
           [::args
            ::context
            ::arg
            ::id])})

(contract/def #{:semantic-namespace/docs :function.execution/after}
  {:docs/content
   (format "an optional function logic to be executed after %s with this args %s" :function/impl
           [::args
            ::context
            ::result
            ::arg
            ::id])})

(contract.type/def #{:semantic-namespace/function} [:function/impl :function.execution/before :function.execution/after])

(defn execute [id & args]
  (let [before-fun (:function.execution/before (contract/fetch id))
        impl-fun (:function/impl (contract/fetch id))
        after-fun (:function.execution/after (contract/fetch id))]
    (when before-fun (before-fun {::args args ::context context ::id id}))
    (let [res (impl-fun {::args args ::arg (first args) ::context context ::id id})]
      (if after-fun (after-fun {::args args ::context context ::result res ::id id})
          (println "finishing " id "on env:  " (:function/env context)))
      res)))
