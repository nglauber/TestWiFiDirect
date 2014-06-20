package nglauber.testewifidirect;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatService extends Service {
    public static final String TAG = "NGVL";

    public static final String ACTION_MESSAGE_RECEIVED = "action_message_received";
    public static final String EXTRA_SERVER_CLIENT = "server_or_client";
    public static final String EXTRA_IP_ADDRESS = "address";
    public static final String EXTRA_MESSAGE = "message";
    public static final int TYPE_SERVER = 1;
    public static final int TYPE_CLIENT = 2;
    private static final int PORT = 8888;

    private int type;
    boolean isRunning;
    
    private InputStream readStream;
    private OutputStream writeStream;
    private Socket mSocket;

    public ChatService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            if (!isRunning) {
                Log.d(TAG, "NOT RUNNING. Starting...");
                type = intent.getIntExtra(EXTRA_SERVER_CLIENT, TYPE_CLIENT);
                final String ipAddress = intent.getStringExtra(EXTRA_IP_ADDRESS);

                new Thread() {
                    @Override
                    public void run() {
                        Log.d(TAG, "init connection thread");
                        isRunning = true;
                        try {
                            if (type == TYPE_SERVER) {
                                Log.d(TAG, "start server socket");
                                ServerSocket serverSocketSocket = new ServerSocket(PORT);
                                mSocket = serverSocketSocket.accept();
                                Log.d(TAG, "client accepted");

                            } else {
                                Log.d(TAG, "connecting to server");
                                mSocket = new Socket();
                                mSocket.connect(new InetSocketAddress(ipAddress, PORT), 500);
                                Log.d(TAG, "connected to server");
                            }
                            readStream = mSocket.getInputStream();
                            writeStream = mSocket.getOutputStream();

                            sendMessageBroadcast(getString(R.string.msg_connected));

                            DataInputStream dis = new DataInputStream(readStream);
                            while (isRunning) {
                                Log.d(TAG, "waiting for mMessages...");
                                String s = dis.readUTF();
                                Log.d(TAG, "message received: " + s);
                                sendMessageBroadcast(s);
                            }

                        } catch (Throwable e) {
                            e.printStackTrace();
                            isRunning = false;

                            disconnect();
                        }
                        Log.d(TAG, "Thread stopped! Disconnected.");
                    }
                }.start();
            } else {
                final String message = intent.getStringExtra(EXTRA_MESSAGE);
                if (message != null && writeStream != null) {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            DataOutputStream dos = new DataOutputStream(writeStream);
                            try {
                                Log.d(TAG, "sending message: " + message);
                                dos.writeUTF(message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendMessageBroadcast(String s) {
        Intent it = new Intent(ACTION_MESSAGE_RECEIVED);
        it.putExtra(EXTRA_MESSAGE, s);
        sendBroadcast(it);
    }

    private void disconnect() {
        try {
            if (readStream != null) {
                readStream.close();
            }
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        try {
            if (writeStream != null) {
                writeStream.close();
            }
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}
