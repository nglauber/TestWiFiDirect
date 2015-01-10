package nglauber.testewifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;


public class MyActivity extends Activity
        implements  WifiP2pManager.PeerListListener,
                    WifiP2pManager.ConnectionInfoListener {

    public static final String TAG = MyActivity.class.getSimpleName();
    IntentFilter mIntentFilter;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;

    IntentFilter mMessageFilter;
    MessageReceived messageReceived;

    List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();
    Spinner spnDevices;
    ArrayAdapter<WifiP2pDevice> mDevicesAdapter;

    ArrayAdapter<String> mMessagesAdapter;
    List<String> mMessages = new ArrayList<String>();
    ListView mLstMessages;
    EditText mEdtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initUI();

        mMessageFilter = new IntentFilter();
        mMessageFilter.addAction(ChatService.ACTION_MESSAGE_RECEIVED);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        messageReceived = new MessageReceived();
        registerReceiver(messageReceived, mMessageFilter);

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        unregisterReceiver(messageReceived);
        disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_connect:
                connect(mPeers.get(spnDevices.getSelectedItemPosition()));
                break;
            case R.id.action_discover_peers:
                discoverPeers();
                break;
            case R.id.action_disconnect:
                disconnect();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendMessage(View v){
        String message = mEdtMessage.getText().toString();
        mEdtMessage.getText().clear();

        Intent it = new Intent(this, ChatService.class);
        it.putExtra(ChatService.EXTRA_MESSAGE, message);
        startService(it);

        mMessages.add(getString(R.string.me_message, message));
        mMessagesAdapter.notifyDataSetChanged();
    }

    /// INTERFACE WifiP2pManager.PeerListListener
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        Log.d(TAG, "onPeersAvailable");
        // Out with the old, in with the new.
        mPeers.clear();
        mPeers.addAll(peerList.getDeviceList());
        mDevicesAdapter.notifyDataSetChanged();

        Log.d(TAG, "----------------------------------");
        for (WifiP2pDevice device : mPeers){
            Log.d(TAG, "Device found: "+ device.deviceName +" ("+ device.deviceAddress +")");
        }

        if (mPeers.size() == 0) {
            Log.d(TAG, "No devices found");
            showToast(R.string.msg_no_devices_found);
        }
    }

    //  INTERFACE WifiP2pManager.ConnectionInfoListener
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d(TAG, "onConnectionInfoAvailable");
        // InetAddress from WifiP2pInfo struct.
        String groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        Log.d(TAG, "address: " + groupOwnerAddress);

        showToast(R.string.msg_connection_available);
        // After the group negotiation, we can determine the group owner.
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
            Log.d(TAG, "I AM THE OWNER");
            Intent it = new Intent(this, ChatService.class);
            it.putExtra(ChatService.EXTRA_SERVER_CLIENT, ChatService.TYPE_SERVER);
            startService(it);

        } else if (wifiP2pInfo.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
            Log.d(TAG, "I AM ON THE GROUP");
            Intent it = new Intent(this, ChatService.class);
            it.putExtra(ChatService.EXTRA_SERVER_CLIENT, ChatService.TYPE_CLIENT);
            it.putExtra(ChatService.EXTRA_IP_ADDRESS, groupOwnerAddress);
            startService(it);
        }
    }

    /// PRIVATE METHODS
    private void discoverPeers(){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "discoverPeers::onSuccess");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "discoverPeers::onFailure = " + reasonCode);
                showToast(R.string.msg_fail_to_find_devices);
                switch (reasonCode) {
                    case WifiP2pManager.BUSY:
                        Log.d(TAG, "BUSY = " + reasonCode);
                        break;
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        Log.d(TAG, "UNSUPPORTED = " + reasonCode);
                        break;
                    case WifiP2pManager.ERROR:
                        Log.d(TAG, "ERROR = " + reasonCode);
                        break;
                }
            }
        });
    }

    private void connect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.d(TAG, "connect::onSuccess");
                showToast(R.string.msg_connecting);

                mMessages.clear();
                mMessagesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "connect::onFailure");
                showToast(R.string.msg_fail_connect);
            }
        });
    }

    private void disconnect() {
        Log.d(TAG, "disconnect");
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "disconnect::removeGroup=onSuccess");

                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "disconnect::removeGroup=onFailure -> " + reason);
                            }
                        });
                    }
                }
            });
        }
    }

    private void initUI(){
        mDevicesAdapter = new ArrayAdapter<WifiP2pDevice>(this, android.R.layout.simple_spinner_item, mPeers);
        mDevicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDevices = (Spinner)findViewById(R.id.spinner);
        spnDevices.setAdapter(mDevicesAdapter);

        mEdtMessage = (EditText)findViewById(R.id.editText);
        mMessagesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMessages);
        mLstMessages = (ListView)findViewById(R.id.listView);
        mLstMessages.setAdapter(mMessagesAdapter);

    }

    private void showToast(int m){
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    // Broadcast that receives mMessages sent by remote device
    class MessageReceived extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mMessages.add(getString(R.string.remote_message, intent.getStringExtra(ChatService.EXTRA_MESSAGE)));
            mMessagesAdapter.notifyDataSetChanged();
        }
    }
}
