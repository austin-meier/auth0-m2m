(defproject auth0-token "0.1.0-SNAPSHOT"
  :description "Generating Auth0 Token for CHILI GraFx"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [com.cnuernber/charred "1.028"]
                 [clj-time "0.15.2"]]
  :repl-options {:init-ns auth0-token.core}
  :main auth0-token.core
  :aot [auth0-token.core])
