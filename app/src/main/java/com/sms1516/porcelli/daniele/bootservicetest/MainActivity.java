package com.sms1516.porcelli.daniele.bootservicetest;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.net.wifi.p2p.WifiP2pDevice;
import android.widget.TextView;
import android.content.IntentFilter;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();

    private static final String KEY_NOME_CONTATTO = "nome_contatto";
    private static final String KEY_INDIRIZZO_MAC = "indirizzo_mac";
    private static final String KEY_NUM_MESSAGGI = "num_messaggi";
    private static final String KEY_CONNESSO = "connesso";
    private static final String KEY_FIRSTRUN = "first_run";

    private TextView contactDetected;
    private TextView mNuoviMessaggi;
    private TextView mConnesso;
    private LocalBroadcastManager mLocalBroadcastManager;
    private MessagesStore mMessagesStore;
    private IntentFilter mIntentFilter;
    private ContactsMessagesReceiver mContactsMessagesReceiver;
    private String macAddress;  //Memorizza l'indirizzo MAC del dispositivo rilevato.
    private SharedPreferences mMessagesReceivedPrefs;    //Memorizzerà, per ciascun dispositivo rilevato, il numero di messaggi scambiati con esso.
    private int mNumMessaggi;   //Memorizzerà il numero dei messaggi ricevuti dal contatto ma non ancora letti.
    private boolean mFirstRun = true;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "Sono in onCreate() della MainActivity.");

        setContentView(R.layout.activity_main);

        contactDetected = (TextView) findViewById(R.id.contactDetected_tv);
        mNuoviMessaggi = (TextView) findViewById(R.id.num_nuovi_messaggi_tv);
        mConnesso = (TextView) findViewById(R.id.connesso_tv);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mContactsMessagesReceiver = new ContactsMessagesReceiver();
        mMessagesReceivedPrefs = getSharedPreferences(CostantKeys.RECEIVED_MESSAGES_PREFS, MODE_PRIVATE);
        mMessagesStore = MessagesStore.getInstance();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(CostantKeys.ACTION_SEND_CONTACT);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS);
        mIntentFilter.addAction(CostantKeys.ACTION_SEND_MESSAGE_FOR_CONTACTS);
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE);
        mIntentFilter.addAction(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
        mIntentFilter.addAction(CostantKeys.ACTION_SEND_DISCONNECT_REQUEST);    //Copia e incolla questo
        mIntentFilter.addAction(CostantKeys.ACTION_CONNECTION_RECEIVED);   //Copia e incolla questo
        mIntentFilter.addAction(CostantKeys.ACTION_CONTACT_CONNECTED);  //Copia e incolla questo
        mIntentFilter.addAction(CostantKeys.ACTION_CONNECTION_REFUSED); //Copia e incolla questo
        mIntentFilter.addAction(CostantKeys.ACTION_DISCONNECT_SUCCESSFUL);  //Copia e incolla questo

        if (savedInstanceState != null) {
            //Recupero il nome, il numero dei messaggi non letti e l'indirizzo MAC del dispositivo rilevato.
            contactDetected.setText(savedInstanceState.getCharSequence(KEY_NOME_CONTATTO));
            mNumMessaggi = savedInstanceState.getInt(KEY_NUM_MESSAGGI);
            if (mNumMessaggi > 0)
                mNuoviMessaggi.setText("(" + mNumMessaggi + ")");
            macAddress = savedInstanceState.getString(KEY_INDIRIZZO_MAC);
            mConnesso.setText(savedInstanceState.getString(KEY_CONNESSO));
            mFirstRun = savedInstanceState.getBoolean(KEY_FIRSTRUN);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "Sono in onStart() di MainActivity.");

        mLocalBroadcastManager.registerReceiver(mContactsMessagesReceiver, mIntentFilter);

        MyService.registerContactsListener(this);

        //Se questa Activity è stata avviata per la prima volta,
        //avvia la scansione dei dispositivi nelle vicinanze.
        if (mFirstRun) {
            MyService.whoIsConnected(this);
            mFirstRun = false;
        }

        Log.i(LOG_TAG, "Avviata la scansione dei dispositivi.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(LOG_TAG, "Sono in onResume() di MainActivity.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Sono in onPause() di MainActivity.");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "Sono in onStop() di MainActivity.");
        MyService.unRegisterContactsListener(this);

        mLocalBroadcastManager.unregisterReceiver(mContactsMessagesReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Sono in onDestroy() di MainActivity.");

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_NOME_CONTATTO, contactDetected.getText());
        outState.putInt(KEY_NUM_MESSAGGI, mNumMessaggi);
        outState.putCharSequence(KEY_INDIRIZZO_MAC, macAddress);
        outState.putCharSequence(KEY_CONNESSO, mConnesso.getText());
        outState.putBoolean(KEY_FIRSTRUN, mFirstRun);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.disconnect_mitem:
                MyService.disconnect(this);

                //Rimuovi la stringa "connesso" dalla TextView connesso_tv
                mConnesso.setText("");
                return true;

            case R.id.refresh_mitem:
                //Aggiona la lista dei contatti rilevati
                //Cancella prima la lista dei contatti rilevati.
                //(Nel mio caso si tratta di cancellare il contatto dalla textView
                //e di impostare a null la variabile macAddress).
                contactDetected.setText("Non c'è nessuno");
                macAddress = null;
                mConnesso.setText("");

                //Avvia la ricerca dei nuovi contatti
                MyService.discoverServices(this);
                return true;

            case R.id.refresh_service:
                //Riavvia il Service. Questo è necessario
                //poiché le API su cui si basa l'app
                //(WifiP2pManager) non sono completamente
                //affidabili (contengono bug).
                stopService(new Intent(MainActivity.this, MyService.class));
                startService(new Intent(MainActivity.this, MyService.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Questo metodo non fa altro che avviare l'activity
     * per la conversazione con il dispositivo remoto.
     */
    private void openConversationActivity(View commonView, String nomeContatto, String indirizzoMAC) {
        Log.i(LOG_TAG, "Sto per aprire l'activity di conversazione.");

        //Azzero il numero dei messaggi ricevuti dal contatto non ancora letti.
        mNumMessaggi = 0;
        mNuoviMessaggi.setText("");

        Log.i(LOG_TAG, "Indirizzo MAC che si sta passando alla ConversationActivity: " + indirizzoMAC);

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

    /**
     * Metodo usato principalemnte per leggere il contenuto
     * del file /proc/net/arp a scopo di debug. Non deve
     * essere presente nella versione finale.
     *
     * @param view La view che ha subìto l'evento onClick.
     */
    public void readArp(View view) {
        Utils.getMac(" ");
    }

    /**
     * Metodo invocato dal bottone per cancellare la cronologia dei messaggi
     * del contatto.
     *
     * @param view Il bottone premuto.
     */
    public void cancellaMessaggi(View view) {
        if (macAddress != null) {
            Log.i(LOG_TAG, "Premuto tasto per la cancellazione della cronologia dei messaggi.");
            MyService.deleteMessages(this, macAddress);

            //Cancella il numero di messaggi ricevuti dal contatto e letti dalle
            //Shared Preferences.
            SharedPreferences.Editor editor = mMessagesReceivedPrefs.edit();
            editor.putInt(macAddress, 0);
            Log.i(LOG_TAG, "Sto cancellando il numero dei messaggi letti dalle shared preferences sotto la chiave: " + macAddress);
            editor.apply();
        }
    }

    private class ContactsMessagesReceiver extends BroadcastReceiver {

        private String connectedTo; //Memorizza l'indirizzo MAC del dispositivo remoto con il quale si è già connessi

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(CostantKeys.ACTION_SEND_CONTACT)) {
                Log.i(LOG_TAG, "Rilevato un nuovo contatto, ora lo aggiungo.");

                WifiP2pDevice device = intent.getParcelableExtra(CostantKeys.ACTION_SEND_CONTACT_EXTRA);
                contactDetected.setText(device.deviceName);
                macAddress = device.deviceAddress;
                Log.i(LOG_TAG, "Indirizzo MAC del dispositivo rilevato: " + macAddress);

                //Controlla se il contatto appena rilevato è già connesso con
                //il nostro dispositivo.
                if (connectedTo != null && Utils.isMacSimilar(connectedTo, device.deviceAddress)) {

                    //Segnala che il dispositivo remoto appena rilevato è già connesso
                    //con quello nostro.
                    mConnesso.setText(" (Connesso)");
                }

                //Controlla se ci sono messaggi ricevuti da questo contatto ma non ancora letti.
                Log.i(LOG_TAG, "Recupero i messaggi ricevuti e letti nelle shared preferences sotto la chiave: " + device.deviceAddress);

                int numMessaggi = mMessagesReceivedPrefs.getInt(device.deviceAddress, 0);
                Log.i(LOG_TAG, "Numero di messaggi ricevuti da questo contatto e letti: " + numMessaggi);

                int messaggiCronologia = mMessagesStore.getMessagesCount(device.deviceAddress);
                Log.i(LOG_TAG, "Numero di messaggi presenti nel messagesStore per questo contatto: " + messaggiCronologia);

                int nuoviMessaggi = messaggiCronologia - numMessaggi;
                Log.i(LOG_TAG, "Messaggi non ancora letti da questo contatto: " + nuoviMessaggi);

                if (nuoviMessaggi > 0)
                    mNuoviMessaggi.setText(" (" + nuoviMessaggi + ")");
                mNumMessaggi = nuoviMessaggi;

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
                mConnesso.setText("");
                mNumMessaggi = 0;
                mNuoviMessaggi.setText("");
                macAddress = null;
            }

            else if (action.equals(CostantKeys.ACTION_CONNECTED_TO_DEVICE)) { //È cambiato un po'

                //La connessione con il dispositivo remoto è stata stabilita con successo o era già stata stabilita
                Log.i(LOG_TAG, "Il dispositivo remoto è disponibile per comunicare.");

                String mac = intent.getStringExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA);

                //Inserisci qui il codice per indicare nella recyclerView il contatto
                //con cui si è appena connessi.
                //(Nel mio caso, semplicemente aggiungo la stringa "(Connesso)" ma tu
                //crea qualcosa di più visivo, come cambiare il suo sfondo in blu...)

                //Nota: devi scandire l'intera recyclerView e confrontare ciascun indirizzo MAC
                //con quello recuperato dall'intent tramite la funzione Utils.getSimilarity().
                //Memorizza ogni risultato che ottieni dal confronto con ciascun indirizzo MAC
                //nella recyclerView e indica come contatto connesso quello che ha ottenuto il
                //risultato più basso.
                //Poiché io visualizzo solo il contatto rilevato più di recente, mi arrangio con
                //la funzione Utils.isMacSimilar().
                if (Utils.isMacSimilar(mac, macAddress)) {
                    mConnesso.setText(" (Connesso)");
                    connectedTo = macAddress;
                }

                //Inserisci qui il codice per ottenere il nome del contatto e la sua View
                //dalla recyclerView sfruttando l'indirizzo MAC appena estratto dall'intent
                //(la variabile locale "mac").
                View commonView = null;
                String nomeContatto = contactDetected.getText().toString();

                //Avvio l'activity di conversazione.
                openConversationActivity(commonView, nomeContatto, connectedTo);
            }

            else if (action.equals(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS)) {
                Log.i(LOG_TAG, "Un contatto si è disconnesso.");
                //Un contatto con cui si aveva una connessione stabilita con esso non è più reperibile
                //(si è allontanato troppo, ha disattivato il Wi-Fi, ha spento il dispositivo, etc...).
                String disconnectedDevice = intent.getStringExtra(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA);

                //Inserisci qui il codice per rimuovere dalla recyclerView il dispositivo che si è appena
                //disconnesso (usando l'indirizzo MAC memorizzato in disconnectedDevice).
                if (Utils.isMacSimilar(macAddress, disconnectedDevice)) {
                    mConnesso.setText("");
                    connectedTo = null;
                }
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
                //nel recyclerView. Per identificare la View del recyclerView da modificare, serviti
                //del metodo Utils.getSimilarity() e confronta la similarità degli indirizzi  MAC
                //del recyclerView con quello presente nel messaggio ricevuto. Quello che ottiene
                //il punteggio più basso è, molto probabilmente, il contatto da incrementare i messaggi
                //non ancora letti.
                // Non è indispensabile: se ci riusciamo a implementare questa
                //caratteristica è bene, altrimenti non importa.

                //Nel mio caso, ad esempio, mi sono arrangiato con una TextView
                if (Utils.isMacSimilar(macAddress, message.getSender())) {
                    mNumMessaggi++;
                    mNuoviMessaggi.setText("(" + mNumMessaggi + ")");
                }
            }

            else if(action.equals(CostantKeys.ACTION_SEND_DISCONNECT_REQUEST)) {    //Copia e incolla questo else if
                //Recupera l'indirizzo MAC del dispositivo remoto con cui bisogna disconnettersi
                String deviceToDisconect = intent.getStringExtra(CostantKeys.ACTION_SEND_DISCONNECT_REQUEST_EXTRA);

                //Qui bisogna far apparire una finestra di dialogo che indica che per connettersi con il contatto
                //selezionato, bisogna prima disconnettersi dal contatto attuale. Se l'utente preme "Si", allora
                //viene mandata la richiesta di disconnessione al Service, altrimenti tutto rimane come è.

                MyService.disconnect(context);
            }

            else if (action.equals(CostantKeys.ACTION_CONNECTION_RECEIVED)) {  //Copia e incolla questo else if

                //Questo intent indica la riuscita connessione di un dispositivo remoto
                //al nostro dispositivo. Quindi segnala nel recyclerView quale dispositivo remoto
                //si è connesso al nostro confrontando gli indirizzi MAC del recyclerView con
                //quello presente nell'intent ricevuto.
                String remoteDevice = intent.getStringExtra(CostantKeys.ACTION_CONNECTION_RECEIVED_EXTRA);

                //Ora confronta l'indirizzo MAC apppena ricavato con ciascun indirizzo MAC
                //presente nel recyclerView tramite la funzione Utils.getSimilarity(). Quindi
                //segnala come connesso il dispositivo nel recyclerView che ha ottenuto il risultato
                //più basso.

                //Io ora uso Utils.isMacSimilar() poiché rilevo solo un dispositivo alla volta (quello
                //rilevato più di recente).
                if (Utils.isMacSimilar(remoteDevice, macAddress)) {
                    mConnesso.setText(" (Connesso)");

                    //Attenzione: imposta sempre l'indirizzo MAC del contatto ottenuto dalla recyclerView.
                    //Non mettere quello memorizzato nell'intent!
                    connectedTo = macAddress;
                }
            }

            else if (action.equals(CostantKeys.ACTION_CONTACT_CONNECTED)) { //Copia e incolla questo else if

                //Questo intent viene inviato dalla classe Service per informare l'activity
                //quale contatto risulta essere connesso a questo dispositivo tramite il Wi-Fi Direct.
                connectedTo = intent.getStringExtra(CostantKeys.ACTION_CONTACT_CONNECTED_EXTRA);
                MyService.discoverServices(context);
            }
            
            else if (action.equals(CostantKeys.ACTION_CONNECTION_REFUSED)) {    //Copia e incolla questo else if
                
                //Il contatto con cui si vuole conversare non ha accettato la richiesta
                //di connessione Wi-Fi Direct. Avvisa l'utente.
                //Inserisci qui il codice che interrompe la progress bar e visualizza il messaggio
                //che comunica il rifiuto della connessione (preferibilmente in una dialogue).
                Log.i(LOG_TAG, "Ho ricevuto l'intent ACTION_CONNECTION_REFUSED.");
                Toast.makeText(context, "Connessione rifiutata.", Toast.LENGTH_SHORT).show();

                //Ritorna a cercare i dispositivi nelle vicinanze.
                MyService.discoverServices(context);
            }

            else if (action.equals(CostantKeys.ACTION_DISCONNECT_SUCCESSFUL)) { //Copia e incolla questo else if

                //Intent ricevuto dopo aver premuto su "Disconnetti" se la disconnessione
                //è avvenuta con successo.
                connectedTo = null;

                //Inserisci qui il codice che vuoi per notificare la riuscita disconnessione.

            }
        }
    }
}
