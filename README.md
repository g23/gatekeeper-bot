# gatekeeper-bot

A simple Discord bot for making it easy to add people to private channels by giving them a code.

## Usage

Add you bot's token in the `data/settings/settings.json`

Then add the bot to your Discord server (guild)

Then tell the bot what Discord guild id to work in. Message the bot:

```
!set-server-snowflake 12345
```

It should reply with a snowman. You can check it with `!get-server-snowflake`

Then you use the bot to easily add people to a private channel by the command:

```
!join some-channel 567890
```

Where `some-channel` is the channel name and `567890` are the last 6 digits of the channel's id. You can find this number as the last 6 digits of the url when viewing the channel in the browser.


## Some fixes may be needed

If your server is small, the bot should connect just fine. However, if the server has many channels and many people and many roles, then Discord may send a large message and this may result in some problems. These problems are due to the discord.clj library using gniazdo websocket library which uses Jetty 9.3.8. Jetty will throw a MessageTooLargeException if the message size on the websocket is larger than 65536 bytes. This was very tricky to solve because this constant did not seem to be easily (if at all) configurable from library functions (three libraries deep).

To make it work, I used a program called `DirtyJoe` (which is excellent) to go into the WebSocketPolicy.class and modify it's constants. In there is the 65536 constant so changing that to a bigger size should work. 

To do that, go to your .m2 folder (either at `~/.m2` or `<username>/.m2` for Windows), make a copy of the Jetty websocket api jar. Rename it to a .zip file and unzip it. Then open the WebSocketPolicy.class file with `DirtyJoe` and change the constant to a bigger number. Then you can use `java -uf websocket.jar org/eclipse/.../WebSocketPolicy.class` to add the modified class file back into the jar. You may also need to edit gniazdo's core.clj and modify the `client` function to call `.setMaxTextMessageBufferSize` before returning the client i.e.

```
(let [c (WebSocketClient.)]
    (.setMaxTextMessageBufferSize c 1000000)
    c)
```

And again use `java -uf gniazdo.jar src/core.clj` to patch the jar. Hopefully these modifications won't be necessary, as they aren't on small Discord servers, but if they are, this is a solution.
