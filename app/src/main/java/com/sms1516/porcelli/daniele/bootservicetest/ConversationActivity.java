package com.sms1516.porcelli.daniele.bootservicetest;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.w3c.dom.Text;

public class ConversationActivity extends AppCompatActivity {

    private ArrayAdapter<String> mMessagesAdapter;
    private ListView mListView;
    private EditText mMessageEditText;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mMessagesReceiver;
    private IntentFilter mIntentFilter;
    private String mContactMacAddress;
    private String mThisDeviceMacAddress;

    private static final String LOG_TAG = ConversationActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conversation);
        mListView = (ListView) findViewById(R.id.messages_listView);
        mMessagesAdapter = new ArrayAdapter<String>(this, R.layout.message_item);
        mListView.setAdapter(mMessagesAdapter);
        mMessageEditText = (EditText) findViewById(R.id.message_et);
        TextView contactNameTv = (TextView) findViewById(R.id.contact_name_tv);

        //Controlla se è stata avviata l'activity per la prima volta oppure se è stata ricreata
        //dopo aver ruotato il dispositivo
        if (savedInstanceState == null) {

            //Recupera i dati inviati dall'activity chiamante
            Intent intent = getIntent();
            contactNameTv.setText(intent.getCharSequenceExtra(CostantKeys.ACTION_START_CONVERSATION_ACTIVITY_EXTRA_NAME));
            mContactMacAddress = (String) intent.getCharSequenceExtra(CostantKeys.ACTION_START_CONVERSATION_ACTIVITY_EXTRA_MAC);

            //Recupera l'indirizzo MAC di questo dispositivo per inserirlo nei messaggi che invierà
            Log.i(LOG_TAG, "Recupero l'indirizzo MAC del dispositivo.");

            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            mThisDeviceMacAddress = info.getMacAddress();

            Log.i(LOG_TAG, "Indirizzo MAC recuperato: " + mThisDeviceMacAddress);
        }
        else {

            //Inserisci qui il codice per recuperare i dati salvati dall'activity
            //tramite il metodo onSaveInstanceState()
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mMessagesReceiver = new MessagesReceiver();

        mIntentFilter.addAction(CostantKeys.ACTION_SEND_MESSAGE);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_DISCONNECTED);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_AVAILABILITY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mLocalBroadcastManager.registerReceiver(mMessagesReceiver, mIntentFilter);

        //Controlla se il contatto con cui sta comunicando è ancora disponibile
        MyService.checkContactAvailable(this);

    }

    @Override
    protected void onPause() {
        super.onPause();

        MyService.unRegisterMessagesListener(this);
        mLocalBroadcastManager.unregisterReceiver(mMessagesReceiver);

    }

    public void sendMessage(View view) {

        //Crea un'istanza di Message e la invia al Service
        String testo = mMessageEditText.getText().toString();
        Message messaggio = new Message(mThisDeviceMacAddress, testo);
        mMessageEditText.setText("");
        Log.i(LOG_TAG, "Messaggio inviato al Service.");
        MyService.sendMessage(this, messaggio);
    }

    private class MessagesReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(CostantKeys.ACTION_SEND_MESSAGE)) {

                //È stato ricevuto un messaggio: visualizzalo nella recyclerView.
                //Per il momento ho usato una listView.
                Log.i(LOG_TAG, "Ricevuto un messaggio, ora lo visualizzo.");

                Message messaggioRicevuto = (Message) intent.getSerializableExtra(CostantKeys.ACTION_SEND_MESSAGE_EXTRA);
                mMessagesAdapter.add(messaggioRicevuto.getText());
                mMessagesAdapter.notifyDataSetChanged();
            }

            else if (action.equals(CostantKeys.ACTION_CONTACT_DISCONNECTED)) {
                Log.i(LOG_TAG, "Il contatto con cui si sta comunicando si è disconnesso.");

                //Il contatto con cui si sta comunicando risulta essersi disconnesso
                //(per via di diversi motivi: si è allontanato troppo, ha disattivato il Wi-Fi,
                //ha spento il dispositivo, etc...).

                //Non sono ancora certo su cosa fare in questo caso. Vedi un po' tu, Giancosimo,
                //come trattare questa situazione. Chiudere l'activity/fragment è una possibile soluzione,
                //ma è alquanto brusca...
                finish();
            }

            else if (action.equals(CostantKeys.ACTION_CONTACT_AVAILABILITY)) {
                Log.i(LOG_TAG, "Ricevuta la risposta alla richiesta di disponibilità del contatto.");

                //È arrivata la risposta da parte del Service riguardo alla richiesta
                //di controllare se il contatto sia ancora disponibile.
                //Estrae la risposta dall'intent.
                boolean connesso = intent.getBooleanExtra(CostantKeys.ACTION_CONTACT_AVAILABILITY_EXTRA, false);

                //Così come per ACTION_CONTACT_DISCONNECTED, vedi un po' cosa fare.
                //Ancora una volta, chiudere l'activity/fragment può essere una soluzione ma è
                //alquanto brusca...
                if (!connesso)
                    finish();
            }
        }
    }
}
