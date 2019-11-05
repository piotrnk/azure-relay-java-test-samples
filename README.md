# azure-relay-java-test-samples: WebsocketListener

This repo contains WebsocketListener class. This is quickly written azure-relay-java validating the following scenarios: 
1) In case of any
kind of network issue the listener should reconnect with Relay HybridConnection 
2) In case of connection string secret is updated in Azure-Relay-HybridConnection, 
this application should reconnect successfully

## Conclusions:
1) HybridConnectionListener can't reconnect to hybridConnection by itself in case of network connection issue  (I am disabling/enabling the ethernet on my pc for testing).
In case listener went to offline status, it can't be opened again as there is an exception: IllegalStateException: Invalid operation: Invalid operation. Cannot call open when it's already closed.

2) In case of short network connection failure, when I programmed building a new HybridConnectionListener object after listener got offline, the result is the hybrid connection work back but WebSocketClient Threads are multiplicated

3) HybridConnectionListener offline handler is not triggered when a network connection breaks.  Listener goes to 'offline' status, but handler is not triggered.

4) In case of connection string is renewed, I can't see any method/option to update connection string  in TokenProvider, the only option is building a new HybridConnectionListener. How this case should be handled? (see point 2 on opening a new listener object) 

5) Exception: org.eclipse.jetty.io.EofException at: ChannelEndPoint.flush(ChannelEndPoint.java:286) occur frequently on token renewal.
   This scenario can be tested with compiling azure-relay-java library with Token Renewal duration set to 1 minute.