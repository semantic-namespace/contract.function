(ns semantic-namespace.contract.function
  (:refer-clojure :exclude [def])
  (:require [clojure.spec.alpha :as s]
            [semantic-namespace.contract.docs :as docs]
            [semantic-namespace.contract.type :as contract.type]
            [semantic-namespace.contract :as contract]))

(s/def :function/impl fn?)

(s/def :function.execution/before fn?)

(s/def :function.execution/after fn?)
(def r (s/coll-of (s/or :compound-identity (s/coll-of qualified-keyword?)
                        :identity qualified-keyword?)))
(s/def :function/response r)
(s/def :function/context r)

(docs/def :function/impl "a function that receives one map with :function/context")

(docs/def :function/context
  "compound-id keys that will be (merged thus one map level only) the expected function/impl input arg 


   if compound-id is a function, the function definition :function/response is merged on the context (because we use compound-identies inside function/impl the identity collisions aren't possible)
" )

(docs/def :function/response
  "compound-id keys that will be (merged thus one map level only) the expected response spec keys" )

(docs/def :function.execution/before
  "prehandler(s), by default identity")

(docs/def :function.execution/after
  "posthandler(s), by default identity")

(contract.type/def #{:semantic-namespace/function} [:function/impl
                                                    :function/context
                                                    :function/response
                                                    :function.execution/before
                                                    :function.execution/after])

(defn def [id opts]
  (let [id (if (coll? id) id #{id})]
    (contract/def (into #{:semantic-namespace/function} id)
      (-> opts
          (update :function.execution/before #(or % identity))
          (update :function.execution/after #(or % identity))))))

(defn fetch [id]
  (contract/fetch (->> (if (coll? id) id #{id})
                       (into #{:semantic-namespace/function}))))

(def ^:dynamic context {:function/env :env/dev})
(defn execute [id & args]
  (let [before-fun (:function.execution/before (contract/fetch id))
        impl-fun (:function/impl (contract/fetch id))
        after-fun (:function.execution/after (contract/fetch id))]
    (when before-fun (before-fun {::args args ::context context ::id id}))
    (let [res (impl-fun {::args args ::arg (first args) ::context context ::id id})]
      (if after-fun (after-fun {::args args ::context context ::result res ::id id})
          (println "finishing " id "on env:  " (:function/env context)))
      res)))



