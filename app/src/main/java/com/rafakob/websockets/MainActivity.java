package com.rafakob.websockets;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.JsonElement;
import com.hosopy.actioncable.ActionCable;
import com.hosopy.actioncable.ActionCableException;
import com.hosopy.actioncable.Channel;
import com.hosopy.actioncable.Consumer;
import com.hosopy.actioncable.Subscription;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.WebSocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    Button sendButton;
    EditText editText;
    WebSocket webSocket;
    MaterialDialog dialog;

    WebSocketClient mWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendButton = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.editText);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webSocket.send(editText.getText().toString());
            }
        });


        // 1. Setup
        URI uri = null;
        try {
            uri = new URI("ws://staging.childcarecentersoftware.com/cable");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // 2. Create subscription
        Channel channel = new Channel("SchoolStudentsChannel");
        channel.addParam("room_id", 5);

        Consumer.Options options = new Consumer.Options();
        options.reconnection = true;
        options.reconnectionMaxAttempts = 5;
        options.reconnectionDelayMax = 1000 * 10;
        Consumer consumer = ActionCable.createConsumer(uri, options);

        Subscription subscription = consumer.getSubscriptions().create(channel);

        subscription
                .onConnected(new Subscription.ConnectedCallback() {
                    @Override
                    public void call() {
                        // Called when the subscription has been successfully completed
                        Log.d("Websocket", "onConnected");
                    }
                })
                .onRejected(new Subscription.RejectedCallback() {
                    @Override
                    public void call() {
                        // Called when the subscription is rejected by the server
                        Log.d("Websocket", "onRejected");
                    }
                })
                .onReceived(new Subscription.ReceivedCallback() {
                    @Override
                    public void call(JsonElement data) {
                        // Called when the subscription receives data from the server
                        Log.d("Websocket", "onReceived");
                    }
                })
                .onDisconnected(new Subscription.DisconnectedCallback() {
                    @Override
                    public void call() {
                        // Called when the subscription has been closed
                        Log.d("Websocket", "onDisconnected");
                    }
                })
                .onFailed(new Subscription.FailedCallback() {
                    @Override
                    public void call(ActionCableException e) {
                        // Called when the subscription encounters any error
                        Log.d("Websocket", "onFailed: " + e.toString());
                    }
                });

        // 3. Establish connection
//        consumer.connect();

        connectWebSocket();
    }

    private void connect() {
        AsyncHttpClient.getDefaultInstance().websocket(new AsyncHttpGet("http://echo.websocket.org"), null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                MainActivity.this.webSocket = webSocket;
                MainActivity.this.webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(final String s) {
                        System.out.println("I got a string: " + s);

                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(MainActivity.this)
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle("WebSocket Notification")
                                        .setContentText(s);

                        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mNotifyMgr.notify(101, mBuilder.build());


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }

                                dialog = new MaterialDialog.Builder(MainActivity.this)
                                        .title("WebSocket Dialog")
                                        .content(s)
                                        .positiveText("OK")
                                        .show();
                            }
                        });

                    }
                });
            }
        });


    }


    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://192.168.88.51:3000/cable");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("{\"command\":\"subscribe\",\"identifier\":\"{\\\"channel\\\":\\\"SchoolStudentsChannel\\\",\\\"room_id\\\":5}\"}");

//                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("Websocket", "onMesage " + s);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }
}
