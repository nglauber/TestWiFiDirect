package nglauber.testewifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public static final String MINHA_TAG = "NGVL";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mConnectionListener;


    public WiFiDirectBroadcastReceiver(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            WifiP2pManager.PeerListListener listener,
            WifiP2pManager.ConnectionInfoListener connectionListener) {

        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mPeerListListener = listener;
        this.mConnectionListener = connectionListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            Log.d(MINHA_TAG, "onReceive::WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION");
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d(MINHA_TAG, "Wifi P2P is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                Log.e(MINHA_TAG, "Wifi P2P is not enabled");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(MINHA_TAG, "onReceive::WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION");
            if (mManager != null) {
                mManager.requestPeers(mChannel, mPeerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            Log.d(MINHA_TAG, "onReceive::WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION");
            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(MINHA_TAG, "networkInfo: "+ networkInfo);
            if (networkInfo.isConnected()) {
                Log.d(MINHA_TAG, "CONNECTED!!!");
                // We are connected with the other device, request connection
                // info to find group owner IP
                mManager.requestConnectionInfo(mChannel, mConnectionListener);
            } else {
                Intent it = new Intent(context, ChatService.class);
                context.stopService(it);
                Toast.makeText(context, "Disconnected", Toast.LENGTH_LONG).show();
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            Log.d(MINHA_TAG, "onReceive::WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice thisDevice = (WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(MINHA_TAG, "THIS DEVICE IS: "+ thisDevice);
        }
    }

}
