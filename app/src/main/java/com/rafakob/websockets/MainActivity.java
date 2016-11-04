package com.rafakob.websockets;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.WebSocket;

public class MainActivity extends AppCompatActivity {
    Button sendButton;
    EditText editText;
    WebSocket webSocket;
    MaterialDialog dialog;

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

        connect();
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
}
