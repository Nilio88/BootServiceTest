package com.sms1516.porcelli.daniele.bootservicetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        final String LOG_TAG = BootReceiver.class.getName();
        Log.i(LOG_TAG, "action: " + action);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_REBOOT) || action.equals("android.intent.action.QUICKBOOT_POWERON")) {

            //Inizializza il MessagesStore
            Log.i(LOG_TAG, "Sto inizializzando il MessagesStore");
            MessagesStore.initialize(context);
            Log.i(LOG_TAG, "MessagesStore inizializzato.");

            //Avvia il servizio
            Intent serviceIntent = new Intent(context, MyService.class);
            Log.i(LOG_TAG, "Sto avviando MyService.");
            context.startService(serviceIntent);
            Log.i(LOG_TAG, "MyService avviato.");

        }
    }
}
