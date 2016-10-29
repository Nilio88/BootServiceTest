package com.sms1516.porcelli.daniele.bootservicetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.net.wifi.p2p.WifiP2pDevice;
import android.widget.TextView;
import android.content.IntentFilter;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();

    private TextView contactDetected;
    private LocalBroadcastManager mLocalBroadcastManager;
    private IntentFilter mIntentFilter;
    private ContactsMessagesReceiver mContactsMessagesReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "Sono in onCreate() della MainActivity.");

        setContentView(R.layout.activity_main);

        contactDetected = (TextView) findViewById(R.id.contactDetected_tv);
        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mContactsMessagesReceiver = new ContactsMessagesReceiver();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(CostantKeys.ACTION_SEND_CONTACT);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS);
        mIntentFilter.addAction(CostantKeys.ACTION_SEND_MESSAGE_FOR_CONTACTS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "Sono in onStart() di MainActivity.");
        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(LOG_TAG, "Sono in onResume() di MainActivity.");

        mLocalBroadcastManager.registerReceiver(mContactsMessagesReceiver, mIntentFilter);

        MyService.registerContactsListener(this);
        MyService.discoverServices(this);

        Log.i(LOG_TAG, "Avviata la scansione dei dispositivi.");

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Sono in onPause() di MainActivity.");
        contactDetected.setText("Non c'è nessuno");
        MyService.unRegisterContactsListener(this);

        mLocalBroadcastManager.unregisterReceiver(mContactsMessagesReceiver);

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "Sono in onStop() di MainActivity.");

        MyService.unRegisterContactsListener(this);

        mLocalBroadcastManager.unregisterReceiver(mContactsMessagesReceiver);

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Sono in onDestroy() di MainActivity.");

        MyService.unRegisterContactsListener(this);

        mLocalBroadcastManager.unregisterReceiver(mContactsMessagesReceiver);

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    private class ContactsMessagesReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(CostantKeys.ACTION_SEND_CONTACT)) {
                Log.i(LOG_TAG, "Rilevato un nuovo contatto, ora lo aggiungo.");

                WifiP2pDevice device = intent.getParcelableExtra(CostantKeys.ACTION_SEND_CONTACT_EXTRA);
                contactDetected.setText(device.deviceName);
            }
        }
    }
}
