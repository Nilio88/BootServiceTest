package com.sms1516.porcelli.daniele.bootservicetest;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.net.wifi.p2p.WifiP2pDevice;
import android.widget.TextView;
import android.content.IntentFilter;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();

    private TextView contactDetected;
    private LocalBroadcastManager mLocalBroadcastManager;
    private IntentFilter mIntentFilter;
    private ContactsMessagesReceiver mContactsMessagesReceiver;
    private String macAddress;

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
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE);
        mIntentFilter.addAction(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
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
        macAddress = null;
        MyService.unRegisterContactsListener(this);

        mLocalBroadcastManager.unregisterReceiver(mContactsMessagesReceiver);

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "Sono in onStop() di MainActivity.");

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Sono in onDestroy() di MainActivity.");

        String message = contactDetected == null ? "contactDetected è null." : "contactDetected inizializzato.";
        Log.i(LOG_TAG, message);
    }

    /**
     * Questo metodo non fa altro che avviare l'activity
     * per la conversazione con il dispositivo remoto.
     */
    private void openConversationActivity(View commonView, String nomeContatto, String indirizzoMAC) {
        Log.i(LOG_TAG, "Sto per aprire l'activity di conversazione.");

        //Crea l'intent per avviare l'activity di conversazione
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(CostantKeys.ACTION_START_CONVERSATION_ACTIVITY_EXTRA_NAME, nomeContatto);
        intent.putExtra(CostantKeys.ACTION_START_CONVERSATION_ACTIVITY_EXTRA_MAC, indirizzoMAC);

        //Controlla se la view in comune tra le activity è stata passata in questo metodo
        if (commonView != null) {

            //È stato passata la View che le due activity hanno in comune, quindi l'applicazione
            //è in esecuzione su un dispositivo con Android con API Level maggiore o uguale a 21.
            //Di conseguenza avvia l'activity di conversazione con una transizione animata.
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(this, commonView, getResources().getString(R.string.contact_transition_name)).toBundle();
            startActivity(intent, bundle);
        }

        else {

            //Avvia l'activity di conversazione senza transizione animata
            startActivity(intent);
        }
    }

    /**
     * Questo metodo verrà invocato ogni volta che si
     * clicca sul contatto con cui si intende comunicare.
     *
     * @param v La view che ha intercettato l'evento di click.
     */
    public void startConversation(View v) {
        //Inserisci il codice che recupera l'indirizzo MAC del contatto
        //che è stato cliccato nella recyclerView. Poiché questa classe visualizza solo il
        //dispositivo rilevato più di recente, per memorizzare il suo
        //indirizzo MAC basta solo una variabile (macAddress).
        Log.i(LOG_TAG, "Click sul contatto con cui comunicare.");

        //Inserisci qui il codice per avviare la progress bar

        if (macAddress != null)
            MyService.connectToClient(this, macAddress);
    }

    private class ContactsMessagesReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(CostantKeys.ACTION_SEND_CONTACT)) {
                Log.i(LOG_TAG, "Rilevato un nuovo contatto, ora lo aggiungo.");

                WifiP2pDevice device = intent.getParcelableExtra(CostantKeys.ACTION_SEND_CONTACT_EXTRA);
                contactDetected.setText(device.deviceName);
                macAddress = device.deviceAddress;
            }
            else if (action.equals(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE)) {

                //Il dispositivo a cui si sta cercando di connettersi non è più raggiungibile
                Log.i(LOG_TAG, "Il dispositivo con cui si vuole comunicare non è più disponibile.");

                //Inserisci qui il codice per chiudere al progress bar

                //Recupera l'indirizzo MAC del dispositivo con cui non si è riusciti a connettere
                String notAvailableDevice = intent.getStringExtra(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE_EXTRA);

                //Inserisci qui il codice per eliminare dalla recyclerView il dispositivo con
                //l'indirizzo MAC memorizzato in notAvailableDevice
                contactDetected.setText("Non c'è nessuno");
                macAddress = null;
            }

            else if (action.equals(CostantKeys.ACTION_CONNECTED_TO_DEVICE)) {

                //La connessione con il dispositivo remoto è stata stabilita con successo o era già stata stabilita
                Log.i(LOG_TAG, "Il dispositivo remoto è disponibile per comunicare.");

                //View del recyclerView che contiene le informazioni sul contatto
                View contactView = null;

                //Recupera l'indirizzo MAC del dispositivo remoto a cui si è connessi
                String remoteDevice = intent.getStringExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //Inserisci qui il codice per ottenere la View del contatto visualizzata nel recyclerView
                    //(mi serve per la transizione animata tra le activity)
                }

                //Inserisci qui il  codice per recuperare il nome del contatto con cui si vuole aprire
                //la conversazione
                String nomeContatto = null;

                //Avvia l'activity per la conversazione
                openConversationActivity(contactView, nomeContatto, remoteDevice);
            }

            else if (action.equals(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS)) {
                Log.i(LOG_TAG, "Un contatto si è disconnesso.");
                //Un contatto con cui si aveva una connessione stabilita con esso non è più reperibile
                //(si è allontanato troppo, ha disattivato il Wi-Fi, ha spento il dispositivo, etc...).
                String disconnectedDevice = intent.getStringExtra(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA);

                //Inserisci qui il codice per rimuovere dalla recyclerView il dispositivo che si è appena
                //disconnesso (usando l'indirizzo MAC memorizzato in disconnectedDevice).
                if (macAddress.equals(disconnectedDevice))
                    macAddress = null;
            }

            else if(action.equals(CostantKeys.ACTION_SEND_MESSAGE_FOR_CONTACTS)) {
                Log.i(LOG_TAG, "Ricevuto un messaggio in MainActivity.");

                //Un contatto ha inviato un messaggio a questo dispositivo ma l'activity di
                //conversazione non era attiva.
                //Recupera il messaggio
                Message message = (Message) intent.getSerializableExtra(CostantKeys.ACTION_SEND_MESSAGE_EXTRA);

                //Inserisci qui il codice che, in base al mittente del messaggio (che è l'indirizzo MAC
                //del dispositivo ottenuto invocando il metodo getSender() della classe Message),
                //incrementa il numero di messaggi non  letti ricevuti dal mittente visualizzato
                //nel recyclerView. Non è indispensabile: se ci riusciamo a implementare questa
                //caratteristica è bene, altrimenti non importa.
            }
        }
    }
}
