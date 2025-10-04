(defproject com.github.semantic-namespace/contract.function "0.1.0-SNAPSHOT"
  :description "semantic-spec functions with AOP support"
  :url "https://github.com/semantic-spec/contract.function"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.semantic-namespace/contract "0.1.1-SNAPSHOT"]
                 [com.github.semantic-namespace/contract.docs "0.1.1-SNAPSHOT"]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :creds :gpg}]]
  :profiles {:dev {:source-paths   ["dev/src"]
                   :repl-options   {:init-ns dev}
                   :dependencies [[org.clojure/tools.namespace "1.4.4"]]}})
