package com.sms1516.porcelli.daniele.bootservicetest;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Text;

public class ConversationActivity extends AppCompatActivity {

    private MessagesStore mMessagesStore;
    private ArrayAdapter<String> mMessagesAdapter;
    private ListView mListView;
    private EditText mMessageEditText;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mMessagesReceiver;
    private IntentFilter mIntentFilter;
    private String mContactMacAddress;
    private String mThisDeviceMacAddress;
    private SharedPreferences mMessagesReceivedPrefs;
    private int mNumMessaggi;   //Memorizza il numero di messaggi caricati dal file di cronologia di conversazione.

    private static final String KEY_NUM_MESSAGGI_CRONOLOGIA = "num_messaggio_cronologia";
    private static final String KEY_CONTACT_MAC = "contact_mac";
    private static final String KEY_THIS_DEVICE_MAC = "this_device_mac";

    private static final String LOG_TAG = ConversationActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMessagesStore = MessagesStore.getInstance();

        setContentView(R.layout.activity_conversation);
        mListView = (ListView) findViewById(R.id.messages_listView);
        mMessagesAdapter = new ArrayAdapter<String>(this, R.layout.message_item);
        mListView.setAdapter(mMessagesAdapter);
        mMessageEditText = (EditText) findViewById(R.id.message_et);
        TextView contactNameTv = (TextView) findViewById(R.id.contact_name_tv);
        mMessagesReceivedPrefs = getSharedPreferences(CostantKeys.RECEIVED_MESSAGES_PREFS, MODE_PRIVATE);

        //Controlla se è stata avviata l'activity per la prima volta oppure se è stata ricreata
        //dopo aver ruotato il dispositivo
        if (savedInstanceState == null) {

            //Recupera i dati inviati dall'activity chiamante
            Intent intent = getIntent();
            contactNameTv.setText(intent.getCharSequenceExtra(CostantKeys.ACTION_START_CONVERSATION_ACTIVITY_EXTRA_NAME));
            mContactMacAddress = (String) intent.getCharSequenceExtra(CostantKeys.ACTION_START_CONVERSATION_ACTIVITY_EXTRA_MAC);
            Log.i(LOG_TAG, "Indirizzo MAC ottenuto dalla MainActivity: " + mContactMacAddress);

            //Recupera l'indirizzo MAC di questo dispositivo per inserirlo nei messaggi che invierà
            Log.i(LOG_TAG, "Recupero l'indirizzo MAC del dispositivo.");

            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            mThisDeviceMacAddress = info.getMacAddress().toLowerCase();

            Log.i(LOG_TAG, "Indirizzo MAC recuperato: " + mThisDeviceMacAddress);
        }
        else {

            //Inserisci qui il codice per recuperare i dati salvati dall'activity
            //tramite il metodo onSaveInstanceState()

            mNumMessaggi = savedInstanceState.getInt(KEY_NUM_MESSAGGI_CRONOLOGIA);
            mThisDeviceMacAddress = savedInstanceState.getString(KEY_THIS_DEVICE_MAC);
            mContactMacAddress = savedInstanceState.getString(KEY_CONTACT_MAC);
        }

        Log.i(LOG_TAG, "In onCreate() mNumMessaggi = " + mNumMessaggi);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mMessagesReceiver = new MessagesReceiver();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(CostantKeys.ACTION_SEND_MESSAGE);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_DISCONNECTED);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_AVAILABILITY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "Sono in onResume() di ConversationActivity.");

        mLocalBroadcastManager.registerReceiver(mMessagesReceiver, mIntentFilter);

        //Recupera la cronologia dei messaggi inviati e ricevuti da questo contatto.
        List<Message> messaggi = mMessagesStore.loadMessagesList(mContactMacAddress);

        MyService.registerMessagesListener(this);

        //Inserisce i messaggi nell'adapter.
        for (int i = mNumMessaggi; i < messaggi.size(); i++) {
            Message messaggio = messaggi.get(i);
            mMessagesAdapter.add(messaggio.getText());
            mNumMessaggi++;
        }
        mMessagesAdapter.notifyDataSetChanged();

        Log.i(LOG_TAG, "In onResume() mNumMessaggi = " + mNumMessaggi);

        //Memorizza nelle Shared Preferences il numero dei messaggi letti dal file della cronologia.
        SharedPreferences.Editor editor = mMessagesReceivedPrefs.edit();
        editor.putInt(mContactMacAddress, mNumMessaggi);
        editor.apply();

        //Controlla se il contatto con cui sta comunicando è ancora disponibile
        MyService.checkContactAvailable(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Sono in onPause() di ConversationActivity.");

        Log.i(LOG_TAG, "In onPause() mNumMessaggi = " + mNumMessaggi);

        List<Message> messaggiNuovi = new ArrayList<>();

        MyService.unRegisterMessagesListener(this);

        //Salva i nuovi messaggi ricevuti nel file della cronologia
        for (int i = mNumMessaggi; i < mMessagesAdapter.getCount(); i++) {

            //Aggiunge il messaggio alla lista di messaggi da salvare.
            //ATTENZIONE: Io ho messo come mittente mContactMacAddress ad ogni messaggio,
            //ma dobbiamo mettere il giusto mittente di ogni messaggio presente nell'adapter.
            messaggiNuovi.add(new Message(mContactMacAddress, mMessagesAdapter.getItem(i)));
            mNumMessaggi++;
        }

        Log.i(LOG_TAG, "In onPause() dopo il salvataggio dei messaggi, mNumMessaggi = " + mNumMessaggi);

        if (messaggiNuovi.size() > 0)
            mMessagesStore.saveMessagesList(messaggiNuovi);

        mLocalBroadcastManager.unregisterReceiver(mMessagesReceiver);

        //Memorizza nelle Shared Preferences il numero dei messaggi letti dal file della cronologia.
        SharedPreferences.Editor editor = mMessagesReceivedPrefs.edit();
        editor.putInt(mContactMacAddress, mNumMessaggi);
        editor.apply();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_NUM_MESSAGGI_CRONOLOGIA, mNumMessaggi);
        outState.putString(KEY_CONTACT_MAC, mContactMacAddress);
        outState.putString(KEY_THIS_DEVICE_MAC, mThisDeviceMacAddress);
    }


    public void sendMessage(View view) {
        String testo = mMessageEditText.getText().toString();

        if (!testo.isEmpty()) {
            //Visualizza il messaggio appena composto nella lista dei messaggi
            mMessagesAdapter.add(testo);
            mMessagesAdapter.notifyDataSetChanged();

            //Crea un'istanza di Message e la invia al Service
            Message messaggio = new Message(mThisDeviceMacAddress, testo);
            mMessageEditText.setText("");
            Log.i(LOG_TAG, "Messaggio inviato al Service.");
            MyService.sendMessage(this, messaggio);
        }
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
                if (!connesso) {
                    Log.i(LOG_TAG, "Il contatto non è più connesso.");
                    finish();
                }

            }
        }
    }
}
