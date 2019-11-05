package tester;

import com.microsoft.azure.relay.HybridConnectionListener;
import com.microsoft.azure.relay.RelayConnectionStringBuilder;
import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import com.microsoft.azure.relay.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FileUtil;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * This is quickly written azure-relay-lib validating the following scenarios: 1) In case of any
 * kind of network issue the listener should reconnect with Relay HybridConnection 2) In case of
 * connection string secret is updated in Azure-Relay-HybridConnection, this application should
 * reconnect successfully
 * <p>
 * Testing was performed with Azure-Java-Lib compiled with the option of Token Renewal duration set
 * to 1 minute
 * <p>
 * Conclusions:
 * 1) HybridConnectionListener can't reconnect to hybridConnection by itself in case of
 * network connection issue (I am disabling/enabling the ethernet on my pc for testing). In case
 * listener went to offline status, it can't be opened again: IllegalStateException: Invalid
 * operation: Invalid operation. Cannot call open when it's already closed.
 * 2)
 * HybridConnectionListener offline handler is not triggered when network connection breaks.
 * Listener goes to 'offline' status, but handler is not triggered.
 * 3) There is a
 * org.eclipse.jetty.io.EofException  at org.eclipse.jetty.io.ChannelEndPoint.flush
 * (ChannelEndPoint.java:286)
 * exception frequently presented on token renewal
 * 4) In case of connection string is renewed, i
 * can't see any method/option to update connection string in TokenProvider, the only option is
 * building new HybridConnectionListener.
 */
public class WebsocketListener extends Thread {
    private static Logger logger = LoggerFactory.getLogger(WebsocketListener.class);

    private HybridConnectionListener listener;


    /**
     * constantly monitor listener online status, take action in case listener is offline
     */
    @Override
    public void run() {
        while (true) {
            try {
                if (listener != null && !listener.isOnline()) {
                    logger.info("listener got offline");
                    Thread.sleep(2000); // try to wait for auto-reconnection
                    if (!listener.isOnline()) {
                        //set to false to see that after listener went offline, it
                        // can't be opened again
                        startListener(true);
                    }
                } else if (listener != null && listener.isOnline()) {
                    logger.info("listener is online");
                } else if (listener == null) {
                    logger.info("listener not started, starting ...");
                    startListener(true);
                }
            } catch (Exception ex) {
                logger.error("fail to handle listener readiness", ex);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void startListener(boolean buildListener) {
        makeSureListenerIsClosed();

        try {
            logger.info("configuring hybrid connection listener");
            if (buildListener) {
                configureHybridConnectionListener();
            }
            listener.openAsync().join();
        } catch (Exception ex) {
            logger.error("failed to start listener", ex);
        }
    }

    void closeListener() {
        listener.close();
    }

    private void configureHybridConnectionListener() {
        String connectionString = FileUtil.getFileAsText("connectionString.txt");
        final RelayConnectionStringBuilder connectionParams =
                new RelayConnectionStringBuilder(connectionString);

        TokenProvider tokenProvider = TokenProvider.createSharedAccessSignatureTokenProvider(
                connectionParams.getSharedAccessKeyName(), connectionParams.getSharedAccessKey());

        try {
            listener = new HybridConnectionListener(
                    new URI(connectionParams.getEndpoint().toString()
                            + connectionParams.getEntityPath()), tokenProvider);
        } catch (URISyntaxException e) {
            logger.error("failed to instantiate hybrid connection listener", e);
        }

        listener.setOfflineHandler(throwable -> {
            logger.info("offline handler triggered, trying to reconnect...");
            startListener(false);
        });
        listener.setOnlineHandler(() -> logger.info("listener is online"));

        listener.setRequestHandler((context) -> {
            handleRequest(context);
        });
    }



    private void makeSureListenerIsClosed() {
        if (listener != null) {
            logger.info("closing the listener");
            try {
                listener.close();
            } catch (Exception ex) {
                logger.error("failed to close the listener", ex);
            }
        }
    }

    private void handleRequest(RelayedHttpListenerContext context) {
        logger.info("handling request, trackingId: {}",
                context.getTrackingContext().getTrackingId());
        RelayedHttpListenerResponse response = context.getResponse();
        response.setStatusCode(200);
        response.setStatusDescription("OK");
        response.close();
    }

    public static void main(String[] args) {
        WebsocketListener task = new WebsocketListener();
        task.start();
    }

}