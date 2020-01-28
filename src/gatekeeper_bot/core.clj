(ns gatekeeper-bot.core
  (:require [discord.bot :as bot]
            [discord.http :as http]
            [discord.config :as config]
            [clojure.string :as s])
  (:gen-class))

; for testing purposes
; (require '(discord [bot :as bot] [config :as config]))
; (def test-bot (bot/create-bot (config/get-bot-name) (config/get-prefix)))
; (def bot-auth (-> test-bot :client :auth))
; guild id is from (:guild-id message)

(defn add-user
  "Adds a user to a given channel
   Takes the bot's auth, the channel id, and user id"
  [auth cid uid]
  (http/discord-request
    :edit-permissions
    auth
    :channel cid
    :overwrite uid
    :json {:allow 0xc00, :type "member"}))


(defn get-text-channels
  "Get the guild's text channels as a map indexed by names as keywords"
  [auth guild-id]
  (let [all-chans  (http/discord-request :get-guild-channels auth :guild guild-id)
        text-chans (filter #(= 0 (:type %)) all-chans)] ; type 0 => text channel
        (reduce #(assoc %1 (keyword (:name %2)) %2) {} text-chans)))

; save the guild-id so !join can be a direct message
(def server-snowflake (atom "Set a :snowflake:"))

(bot/defcommand set-server-snowflake
  [client message]
  (let [words (s/split (:content message) #" ")]
    (if (>= (.length (get words 0)) 1)
      (do
        (println "message: " message)
        (reset! server-snowflake (get words 0))
        (bot/say ":snowman:")))))

(bot/defcommand get-server-snowflake
  [client message]
  (bot/say (str ":snowflake: " @server-snowflake " :snowflake:")))

(defn dm?
  "Determine if a message is a DM"
  [message]
  (nil? (-> message :channel :guild-id)))

(defn make-big
  "Makes the text to be displayed in a big way on Discord"
  [text]
  (reduce #(str %1 " :regional_indicator_" %2 ": ") "" text))

; for fun
(defn fibo
  "BigInt fibonacci function"
  ([n] (fibo n 0N 1N))
  ([n a b]
    (if (<= n 0)
      a
      (recur (- n 1) b (+ a b)))))

; a fun command
(bot/defcommand fibonacci
  [client message]
  (try
    (let [n (Integer/parseInt (:content message))]
      (if (or (> n 2020) (< n 0))
        (bot/pm "‾\\_(ツ)_/‾")
        (bot/pm (str "The " n "th fibonacci number is " (fibo n)))))
    (catch Exception e (bot/pm (str (make-big "bad") ":hash:"))))
  (if (not (dm? message))
    (bot/delete message)))

; Something like !join my-class-channel 123456
; Where `my-class-channel` is the name of the channel
; and 123456 are the last digits of the channel's snowflake
(bot/defcommand join
  [client message]
  (let [auth (:auth client)
        gid @server-snowflake
        uid (-> message :author :id)
        words (s/split (:content message) #" ")
        to-join (keyword (get words 0))
        code (get words 1)]
        (println "Join message:" message)
        (println "Join client:" client)
        (if (and (not (nil? code)) (> (.length code) 5)) ; make sure 6 digit code
          (let [channels (get-text-channels auth gid)
                cid (-> channels to-join :id)]
                ; check if the channel and code is good
                (if (and (not (nil? cid)) (s/ends-with? cid code))
                  (do 
                    (add-user auth cid uid)
                    (bot/pm ":ok: :thumbsup:"))
                  (bot/pm (make-big "nope"))))
          (bot/pm (str (make-big "code") ":question:")))
        ; lastly clean up the user's message if not in DM
        (if (not (dm? message))
          (bot/delete message))))

(bot/defcommand test
  [client message]
  (println "Bot received the test command")
  (println "client is:" client)
  (println "message is:" message)
  (bot/say "test complete!"))

(bot/defcommand css 
  [client message]
  (bot/say "https://media.giphy.com/media/13FrpeVH09Zrb2/giphy.gif")
  (bot/delete message))

(defn -main
  [& args]
  (println "Starting GatekeeperBot")
  (bot/start))
