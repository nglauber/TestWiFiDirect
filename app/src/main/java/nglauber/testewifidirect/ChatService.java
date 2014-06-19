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
    boolean isRunning;

    public static final String ACTION_MESSAGE_RECEIVED = "actionmessagereceived";
    public static final String EXTRA_SERVER_CLIENT = "server_or_client";
    public static final String EXTRA_IP_ADDRESS = "address";
    public static final String EXTRA_MESSAGE = "message";
    public static final int TYPE_SERVER = 1;
    public static final int TYPE_CLIENT = 2;
    private static final int PORT = 8888;
    private int type;

    private InputStream readStream;
    private OutputStream writeStream;

    public ChatService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("NGVL", "onStartCommand");
        if (!isRunning){
            Log.d("NGVL", "NOT RUNNING. Starting...");
            type = intent.getIntExtra(EXTRA_SERVER_CLIENT, TYPE_CLIENT);
            final String ipAddress = intent.getStringExtra(EXTRA_IP_ADDRESS);

            new Thread(){
                @Override
                public void run() {
                    Log.d("NGVL", "init connection thread");
                    Socket socket = null;
                    isRunning = true;
                    try {
                        if (type == TYPE_SERVER) {
                            Log.d("NGVL", "start server socket");
                            ServerSocket serverSocketSocket = new ServerSocket(PORT);
                            socket = serverSocketSocket.accept();
                            Log.d("NGVL", "client accepted");

                        } else {
                            Log.d("NGVL", "connecting to server");
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(ipAddress, PORT), 500);
                            Log.d("NGVL", "connected to server");
                        }
                        readStream = socket.getInputStream();
                        writeStream = socket.getOutputStream();

                        DataInputStream dis = new DataInputStream(readStream);
                        while (isRunning){
                            Log.d("NGVL", "waiting for messages...");
                            String s = dis.readUTF();
                            Log.d("NGVL", "message received: "+ s);
                            Intent it = new Intent(ACTION_MESSAGE_RECEIVED);
                            it.putExtra(EXTRA_MESSAGE, s);
                            sendBroadcast(it);
                        }

                    } catch (Throwable e){
                        e.printStackTrace();
                        isRunning = false;

                        try {
                            readStream.close();
                        } catch (IOException ioe){
                            ioe.printStackTrace();
                        }
                        try {
                            writeStream.close();
                        } catch (IOException ioe){
                            ioe.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException ioe){
                            ioe.printStackTrace();
                        }
                    }
                    Log.d("NGVL", "Thread stopped! Disconnected.");
                }
            }.start();
        } else {
            final String message = intent.getStringExtra(EXTRA_MESSAGE);
            if (message != null && writeStream != null){
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        DataOutputStream dos = new DataOutputStream(writeStream);
                        try {
                            Log.d("NGVL", "sending message: "+ message);
                            dos.writeUTF(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
