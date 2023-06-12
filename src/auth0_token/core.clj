(ns auth0-token.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [auth0-token.secrets :refer [secrets auth0-token-url]]
            [clj-time.core :as t]))

(def token-cache (atom {}))

;; Helper to check if token is expired
(defn valid? [entry]
  (t/after? (t/now) (:entry/expiration entry)))

(defn now-plus-offset [offset]
  (t/plus (t/now) (t/seconds (/ offset 1000))))

(defn from-cache [key]
  (let [entry (key @token-cache)]
    (when (and entry (valid? entry))
      (:entry/value entry))))

(defn update-cache! [k v expires-in]
  (swap! token-cache assoc k
    {:entry/value v
     :entry/expiration (now-plus-offset expires-in)}))

;; Do some magic here idunno
(defn env-exists? [environment]
  true)


;; Parse json to a map if token request is a 200 otherwise throw
(defn auth0-token []
  (let [response (client/post auth0-token-url
                              {:headers {"content-type" "application/json"}
                               :body (json/write-str
                                      {:client_id (:client_id secrets)
                                       :client_secret (:client_secret secrets)
                                       :audience "https://chiligrafx.com"
                                       :grant_type "password"
                                       :username (:username secrets)
                                       :password (:password secrets)})})]
    (if (= (:status response) 200)
      (let [resp (json/read-str (:body response))]
        {:expires-in (get resp "expires_in")
         :token (get resp "access_token")})
      (throw (ex-info "Failed to get token from Auth0" {:response response})))))


(defn gen-token [environment]
  (if (env-exists? environment)
    (let [{:keys [token expires-in]} (auth0-token)]
      (update-cache! environment token expires-in)
      token)
    (throw (ex-info "The specified environment doesn't exist" {:data {:environment (name environment)}}))))

(defn get-token [environment]
  (let [environment (keyword environment)]
    (or (from-cache environment)
        (gen-token environment))))


(get-token "ft-nocool")
