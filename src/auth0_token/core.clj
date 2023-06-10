(ns auth0-token.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [auth0-token.secrets :refer [secrets auth0-token-url]]
            [clj-time.core :as t]))

; TODO(austin) Init from persistance/redis
(def token-cache (atom {}))

;; Helper to check if token is expired
(defn expired? [token]
  (t/after? (t/now) (:expiration token)))

(defn create-expiration-date [exp-offset]
  (t/plus (t/now) (t/seconds (/ exp-offset 1000))))


(defn update-cache [environment token]
  (-> token
      (assoc :expiration (create-expiration-date (:expires_in token)))
      (->> (swap! token-cache assoc environment))))

;; Do some magic here idunno
(defn env-exists? [environment]
  true)

;; Generate the request body for the token
(defn request-body []
  (json/write-str
   {:client_id (:client_id secrets)
    :client_secret (:client_secret secrets)
    :audience "https://chiligrafx.com"
    :grant_type "client_credentials"}))

;; make the request for the token
(defn token-request []
  (client/post auth0-token-url
    {:headers {"content-type" "application/json"}
     :body (request-body)}))

;; Parse json to a map if token request is a 200
;; TODO(austin): If not 200 propogate the response body
(defn handle-request [request]
  (let [response (request)]
    (if (= (:status response) 200)
      (json/read-str (:body response) :key-fn keyword)
      {:success false :data "There was an error with the request for the token" :response response})))


(defn gen-token [environment]
  (if (env-exists? environment)
    (let [result (handle-request token-request)]
      (if (:success result)
        result
        (do
          (update-cache environment result)
          {:success true :data (:access_token result)})))
    {:success false :data (str "The specified environment doesn't exist: " (name environment))}))

(defn get-token [environment]
  (let [environment (keyword environment)
        possible-token (environment @token-cache)] ; check if there is a token in the cache
    (if (nil? possible-token) ; if nil? we don't have a token
      (gen-token environment)
      (if (expired? possible-token) ; check if the token in the cache is expired
        (gen-token environment)
        {:success true :cached true :data possible-token}))))


(get-token "ft-nostress")
