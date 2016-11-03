package com.sms1516.porcelli.daniele.bootservicetest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Log;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.provider.ContactsContract.Profile;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.WifiP2pManager;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.database.Cursor;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class MyService extends Service {

    private static final String LOG_TAG = MyService.class.getName();

    //Costanti per le azioni e i parametri degli intent
    private static final String ACTION_REGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_CONTACTS_LISTENER";

    private static final String ACTION_DISCOVER_SERVICES = "com.sms1516.porcelli.daniele.wichat.action.DISCOVER_SERVICES";
    private static final String ACTION_UNREGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_CONTACTS_LISTENER";

    private static final String ACTION_CONNECT_TO_CLIENT = "com.sms1516.porcelli.daniele.wichat.action.CONNECT_TO_CLIENT";
    private static final String ACTION_CONNECT_TO_CLIENT_EXTRA = "com.sms1516.porcelli.daniele.wichat.extra.DEVICE";

    private static final String ACTION_REGISTER_MESSAGES_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_MESSAGES_LISTENER";
    private static final String ACTION_REGISTER_MESSAGES_LISTENER_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_MESSAGES_LISTENER";

    private static final String ACTION_UNREGISTER_MESSAGES_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_MESSAGES_LISTENER";

    private static final String ACTION_SEND_MESSAGE = "com.sms1516.porcelli.daniele.wichat.action.ACTION_SEND_MESSAGE";
    private static final String ACTION_SEND_MESSAGE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.ACTION_SEND_MESSAGE_EXTRA";
    private static final String ACTION_CHECK_CONTACT_AVAILABLE = "com.sms1516.porcelli.daniele.wichat.action.CHECK_CONTACT_AVAILABLE";

    //Variabili d'istanza
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private IntentFilter mIntentFilter;
    private String thisDeviceMAC;
    private String conversingWith;  //Memorizzerà l'indirizzo MAC del dispositivo con cui l'utente sta conversando in questo momento
    private Thread mNsdService;
    private boolean mContactsListener;
    private boolean mMessagesListener;
    private Map<String, Integer> servicesConnectionInfo = new HashMap<>();
    private List<ChatConnection> connections = new ArrayList<>();

    //Testo per identificare il messaggio dummy
    private static final String DUMMY_MESSAGE ="!DUMMYMESSAGE";


    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "Sono in onCreate() di MyService");

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //Crea l'intent filter per WifiP2pBroadCastReceiver
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Sono in onStartCommand.");

        if (intent == null || intent.getAction() == null) {
            Log.i(LOG_TAG, "Registro il WifiP2pBroadcastReceiver.");
            mReceiver = new WifiP2pBroadcastReceiver();
            registerReceiver(mReceiver, mIntentFilter);
            Log.i(LOG_TAG, "WifiP2pBroadcastReceiver registrato con successo.");
        }

        else if (intent.getAction().equals(ACTION_REGISTER_CONTACTS_LISTENER)) {
            Log.i(LOG_TAG, "Registro il contactsListener.");

            //Registra il ContactsListener
            mContactsListener = true;
        }

        else if (intent.getAction().equals(ACTION_DISCOVER_SERVICES)) {

            //Semplicemente chiama il metodo privato per la ricerca dei dispositivi nelle vicinanze
            discoverServices();
        }

        else if (intent.getAction().equals(ACTION_UNREGISTER_CONTACTS_LISTENER)) {
            Log.i(LOG_TAG, "Rimuovo il ContactsListener.");
            mContactsListener = false;
        }

        else if (intent.getAction().equals(ACTION_CONNECT_TO_CLIENT)) {
            Log.i(LOG_TAG, "Mi sto connettendo con il dispositivo remoto.");

            //Recupera l'indirizzo MAC del dispositivo a cui connettersi
            final String device = intent.getStringExtra(ACTION_CONNECT_TO_CLIENT_EXTRA);

            //Controlla che non ci sia già una connessione con il dispositivo remoto
            boolean alreadyConnected = false;

            for (ChatConnection chatConnection: connections) {
                if (chatConnection.getMacAddress().equals(device)) {
                    alreadyConnected = true;
                    break;
                }
            }

            //Se non è stata trovata una connessione esistente con il dispositivo, la crea
            if (!alreadyConnected) {

                //Si connette con il dispositivo tramite Wi-Fi direct
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device;
                config.wps.setup = WpsInfo.PBC;

                //Imposta il dispositivo da connettersi nel BroadcastReceiver
                ((WifiP2pBroadcastReceiver) mReceiver).setDeviceToConnect(device);

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        //Viene mandato l'intent di broadcast WIFI_P2P_CONNECTION_CHANGED_ACTION

                        Log.i(LOG_TAG, "Sono riuscito a connettermi con il dispositivo remoto. Aspetto le informazioni di connessione.");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(LOG_TAG, "Impossibile collegarsi con il client tramite Wi-Fi P2P.");

                        //Informa le activity/fragment che il dispositivo non è reperibile
                        if (mContactsListener) {
                            Intent intent = new Intent(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE);
                            intent.putExtra(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE_EXTRA, device);
                            mLocalBroadcastManager.sendBroadcast(intent);
                            Log.i(LOG_TAG, "Inviato intent per notificare la disconnessione di un dispositivo all'activity dei contatti.");
                        }

                    }
                });
            }

            else {
                //Invia l'intent per notificare che il dispositivo è già connesso
                Intent deviceConnectedIntent = new Intent(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
                deviceConnectedIntent.putExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA, device);
                conversingWith = device;
                mLocalBroadcastManager.sendBroadcast(deviceConnectedIntent);
            }
        }

        else if (intent.getAction().equals(ACTION_REGISTER_MESSAGES_LISTENER)) {

            //Registra il MessagesListener
            mMessagesListener = true;
            conversingWith = (String) intent.getSerializableExtra(ACTION_REGISTER_MESSAGES_LISTENER_EXTRA);

            Log.i(LOG_TAG, "Messages Listener registrato.");
        }

        else if (intent.getAction().equals(ACTION_UNREGISTER_MESSAGES_LISTENER)) {

            //Rimuovi il MessagesListener
            mMessagesListener = false;

            Log.i(LOG_TAG, "Messages Listener rimosso.");
        }

        else if (intent.getAction().equals(ACTION_SEND_MESSAGE)) {

            //Recupera il messaggio da inviare
            Message message = (Message) intent.getSerializableExtra(ACTION_SEND_MESSAGE_EXTRA);

            boolean messageSent = false;

            //Cerca la connessione con il dato destinatario
            for (ChatConnection conn : connections) {
                if (conn.getMacAddress().equals(conversingWith)) {

                    //Invia il messaggio a questa connessione
                    conn.sendMessage(message);
                    messageSent = true;
                }
            }

            if (!messageSent)
                Log.e(LOG_TAG, "Messaggio non inviato: non esiste alcuna connessione con " + conversingWith);
        }

        else if (intent.getAction().equals(ACTION_CHECK_CONTACT_AVAILABLE)) {

            //Controlla se la connessione con il contatto con cui si sta comunicando è ancora attiva,
            //quindi notifica l'activity/fragment del risultato.
            Intent contactAvailabilityIntent = new Intent(CostantKeys.ACTION_CONTACT_AVAILABILITY);
            contactAvailabilityIntent.putExtra(CostantKeys.ACTION_CONTACT_AVAILABILITY_EXTRA, conversingWith == null);
            mLocalBroadcastManager.sendBroadcast(contactAvailabilityIntent);
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Questo metodo si occupa di avviare la ricerca dei dispositivi
     * nelle vicinanze che hanno installato WiChat.
     */
    private void discoverServices() {
        Log.i(LOG_TAG, "Inizio la ricerca dei servizi nelle vicinanze.");

        //Registra la richiesta
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        //Per sicurezza, rimuovi ogni richiesta dal WifiP2pManager
        mManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Tutto ok. Nulla da fare
                Log.i(LOG_TAG, "Ho interrotto la ricerca precedente e ne inizio una nuova.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Impossibile rimuovere le richieste di servizio dal manager: clearServiceRequest failed.");
            }
        });

        mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Tutto ok. Nulla da fare.
                Log.i(LOG_TAG, "Service request aggiunto con successo.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Impossibile ottenere le informazioni di connessione: AddServiceRequest failed");
            }
        });

        //Avvia la ricerca di dispositivi nelle vicinanze con lo stesso servizio WiChat
        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Tutto bene. Nulla da fare.
                Log.i(LOG_TAG, "Ricerca dispositivi avviata.");
            }

            @Override
            public void onFailure(int reason) {

                //Si è verificato un errore. Esso verrà registrato nel Log.
                String errore = null;
                switch (reason) {
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        errore = "Wi-Fi P2P non supportato da questo dispositivo.";
                        break;
                    case WifiP2pManager.BUSY:
                        errore = "sistema troppo occupato per elaborare la richiesta.";
                        break;
                    default:
                        errore = "si è verificato un errore durante la registrazione del servizio WiChat.";
                        break;
                }

                Log.e(LOG_TAG, "Impossibile iniziare la ricerca dei peers: " + errore);
            }
        });
    }

    /**
     * Classe interna che rappresenta il thread da eseguire per attivare
     * il network service discovery per informare i dispositivi limitrofi
     * del servizio messo a disposizione da questa applicazione e ricevere
     * connessioni da questi ultimi.
     *
     * @author Daniele Porcelli
     */
    private class NsdProviderThread extends Thread {

        //Costanti che fungono da chiavi per il TXT record
        private static final String NICKNAME = "nickname";
        private static final String LISTEN_PORT = "listenport";

        //Costante del nome del servizio
        private static final String SERVICE_NAME = "WiChat";

        //Dizionario che conserva le coppie (indirizzo, nome) per l'associazione di un
        //nome più amichevole al dispositivo individuato
        private final HashMap<String, String> buddies = new HashMap<>();

        //Implementazione del listener dei TXT record
        private final WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                Log.i(LOG_TAG, "Catturato un TXT Record. fullDomainName = " + fullDomainName);

                if (fullDomainName.contains(SERVICE_NAME.toLowerCase())) {
                    buddies.put(srcDevice.deviceAddress, txtRecordMap.get(NICKNAME));
                    servicesConnectionInfo.put(srcDevice.deviceAddress, Integer.parseInt(txtRecordMap.get(LISTEN_PORT)));
                    Log.i(LOG_TAG, "Informazioni del TXT Record memorizzate correttamente.");
                }
            }
        };

        //Implementazione del listener del servizio
        private final WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                Log.i(LOG_TAG, "Individuato un servizio. instanceName = " + instanceName);

                if (instanceName.contains(SERVICE_NAME)) {

                    //Aggiorna il nome del dispositivo con il nome amichevole fornito dal TXT record
                    //(se ne è arrivato uno)
                    srcDevice.deviceName = buddies.containsKey(srcDevice.deviceAddress) ? buddies.get(srcDevice.deviceAddress) : srcDevice.deviceName;

                    //Avvisa il ContactsListener del nuovo dispositivo trovato
                    if (mContactsListener) {
                        Intent intent = new Intent(CostantKeys.ACTION_SEND_CONTACT);
                        intent.putExtra(CostantKeys.ACTION_SEND_CONTACT_EXTRA, srcDevice);
                        mLocalBroadcastManager.sendBroadcast(intent);

                        Log.i(LOG_TAG, "Dispositivo rilevato inviato all'activity.");
                    }
                }
            }
        };

        @Override
        public void run() {
            Log.i(LOG_TAG, "Sto eseguendo il NsdProviderThread.");

            //Ottiene il numero della prima porta disponibile
            ServerSocket server;
            try {
                server = new ServerSocket(0);
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile avviare il server: " + ex.toString());
                return;
            }

            int port = server.getLocalPort();
            Log.i(LOG_TAG, "Ho creato il server. Porta: " + port);

            //Ottiene il nome del proprietario di questo dispositivo Android
            Cursor cursor = getContentResolver().query(Profile.CONTENT_URI, null, null, null, null);
            cursor.moveToFirst();
            String proprietario = cursor.getString(cursor.getColumnIndex("display_name"));

            Log.i(LOG_TAG, "Nome proprietario del dispositivo: " + proprietario);

            //Crea il TXT record da inviare agli altri dispositivi che hanno installato WiChat
            Map<String, String> txtRecord = new HashMap<>();
            txtRecord.put(LISTEN_PORT, String.valueOf(port));
            txtRecord.put(NICKNAME, proprietario);

            //Crea l'oggetto che conterrà le informazioni riguardo il servizio
            WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, "_presence._tcp", txtRecord);

            //Registra il servizio appena creato
            mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //È andato tutto bene. Nulla da fare.
                    Log.i(LOG_TAG, "NSD registrato correttamente.");
                }

                @Override
                public void onFailure(int reason) {

                    //Si è verificato un errore. Esso verrà registrato nel Log.
                    String errore = null;
                    switch (reason) {
                        case WifiP2pManager.P2P_UNSUPPORTED:
                            errore = "Wi-Fi P2P non supportato da questo dispositivo.";
                            break;
                        case WifiP2pManager.BUSY:
                            errore = "Sistema troppo occupato per elaborare la richiesta.";
                            break;
                        default:
                            errore = "Si è verificato un errore durante la registrazione del servizio WiChat.";
                            break;
                    }
                    Log.e(LOG_TAG, errore);
                }
            });

            //Registra i listener per i TXT record e per i servizi provenienti dai dispositivi in vicinanza
            mManager.setDnsSdResponseListeners(mChannel, serviceListener, txtRecordListener);

            //Avvia la ricerca dei dispositivi nelle vicinanze (a quanto pare, se il dispositivo non inizia la ricerca, esso stesso non può
            //essere rilevato dagli altri dispositivi).
            discoverServices();

            //Avvia l'ascolto di connessioni in entrata
            while (!Thread.currentThread().isInterrupted()) {
                Log.i(LOG_TAG, "Sono nel ciclo while del NsdProviderThread.");
                Socket clientSocket = null;
                try {
                    clientSocket = server.accept();
                } catch (IOException ex) {
                    Log.e(LOG_TAG, "Impossibile avviare il server: " + ex.toString());
                    break;
                }
                try {
                    Log.i(LOG_TAG, "Ricevuta richiesta di connessione da parte di un dispositivo.");

                    ChatConnection chatConn = new ChatConnection(clientSocket);
                    synchronized (connections) {
                        connections.add(chatConn);
                    }
                    Log.i(LOG_TAG, "Connessione da parte del dispositivo remoto riuscita.");

                } catch (IOException ex) {
                    //Errore durante la connessione con il client
                    Log.e(LOG_TAG, "Errore durante la connessione con il client: " + ex.toString());

                    //Ritorna in ascolto di altri client
                    continue;
                }

            }
            Log.i(LOG_TAG, "NsdProviderThread fuori dal ciclo while.");
            try {
                server.close();
            }
            catch(IOException ex) {
                //Niente di importante da fare.
            }
        }
    }

    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {

        //Variabile per tener traccia del dispositivo a cui si sta connettendo.
        //L'ho inserito per poter ottenere l'indirizzo IP del dispositivo quando
        //viene chiamato requestConnectionInfo() dopo aver ricevuto l'intent di
        //broadcast WIFI_P2P_CONNECTION_CHANGED_ACTION
        private String device;

        //Implementazione del ConnectionInfoListener per recuperare l'indirizzo IP
        //del dispositivo a cui si è appena connessi
        private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                Log.i(LOG_TAG, "Sono in onConnectionInfoAvailable(). Informazioni sulla connessione catturate.");

                //Ottieni l'indirizzo IP
                InetAddress deviceIP = info.groupOwnerAddress;

                //Controlla se l'indirizzo MAC del dispositivo remoto a cui connettersi
                //è stato impostato. Se non è stato impostato, vuol dire che la connessione che
                //l'intent di broadcast WIFI_P2P_CONNECTION_CHANGED_ACTION è stato intercettato
                //per via di una connessione in entrata da parte di un dispositivo remoto.
                if (device == null) {

                    //Esci dal metodo onConnectionInfoAvailable()
                    Log.i(LOG_TAG, "Ricevuta una connessione da parte di un dispositivo remoto. Esco da onConnectionInfoAvailable().");
                    return;
                }

                //Ottieni la porta del servizio remoto
                int port = servicesConnectionInfo.get(device);
                Log.i(LOG_TAG, "Porta del dispositivo remoto: " + port);

                //Crea e avvia la connessione
                ConnectThread connectThread = new ConnectThread(deviceIP, port, device);
                connectThread.start();

                //Memorizza l'indirizzo MAC del dispositivo con il quale si vuole comunicare
                conversingWith = device;

                device = null;
            }

            /**
             * Questo metodo ha lo scopo di recuperare l'indirizzo MAC del dispositivo il cui indirizzo IP
             * è stato appena ottenuto nel metodo onConnectionInfoAvailable().
             *
             * Fonte: http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
             *
             * @param ip L'indirizzo IP del dispositivo remoto di cui si vuole conoscere l'indirizzo MAC.
             * @return L'indirizzo MAC del dispositivo remoto sotto forma di stringa.
             */
            private String getMAC(String ip) {
                BufferedReader br = null;

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line = null;

                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {

                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                return mac;

                            } else {
                                return null;
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                } finally {

                    try {
                        br.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;

            }
        };

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "Sono nel onReceive() di WifiP2pBroadcastReceiver.");

            String action = intent.getAction();
            Log.i(LOG_TAG, "Azione catturata dal WifiP2pBroadcastReceiver: " + intent.getAction());

            if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {

                //Controlla se il Wi-Fi P2P è attivo e supportato dal dispositivo
                int statoWiFi = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (statoWiFi == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.i(LOG_TAG, "Il Wi-Fi P2P è abilitato su questo dispositivo.");

                    if (thisDeviceMAC == null) {

                        //Ottiene l'indirizzo MAC di questo dispositivo
                        Log.i(LOG_TAG, "Recupero l'indirizzo MAC del dispositivo.");

                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        WifiInfo info = wifiManager.getConnectionInfo();
                        thisDeviceMAC = info.getMacAddress();

                        Log.i(LOG_TAG, "Indirizzo MAC recuperato: " + thisDeviceMAC);
                    }

                    //Avvia il thread per l'NSD
                    mNsdService = new NsdProviderThread();
                    mNsdService.start();

                }
                else {

                    //Nel caso il Wi-Fi è stato disattivato, interrompi il thread del NSD
                    Log.i(LOG_TAG, "Il Wi-Fi P2P è stato disabilitato su questo dispositivo.");

                    if (mNsdService != null && !mNsdService.isInterrupted()) {
                        mNsdService.interrupt();
                    }
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {

                //Questo dispositivo si è appena connesso con un altro tramite Wi-Fi direct.
                //Recuperiamo le informazioni di connessione di conseguenza.
                if (mManager == null) {
                    return;
                }
                Log.i(LOG_TAG, "La connessione con il dispositivo remoto è cambiata.");

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.i(LOG_TAG, "Il dispositivo si è connesso con un dispositivo remoto.");

                    //Si è appena connesso al dispositivo remoto, otteniamo le informazioni della connessione
                    mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {

                //Recupera la nuova lista di contatti disponibili nel range del Wi-Fi
                //Nota: probabilmente neanche questo ci servirà, ma lo teniamo per sicurezza.
                //mManager.requestPeers(mChannel, peerListListener);
            }

        }

        /**
         * Metodo invocato quando si vuole impostare il dispositivo di cui
         * si vuole ricavare l'indirizzo IP per il socket.
         *
         * @param device L'indirizzo MAC del dispositivo a cui ci si sta connettendo.
         */
        public void setDeviceToConnect(String device) {
            this.device = device;
        }
    }

    /**
     * Classe che si occupa di mantenere la connessione tra questo
     * dispositivo e quello con cui si è connesso (o che ha ricevuto la
     * connessione).
     *
     * @author Daniele Porcelli
     */
    private class ChatConnection {

        //Variabili d'istanza
        private Socket connSocket;
        private String macAddress;
        private SendingThread sendingThread;
        private ReceivingThread receivingThread;

        /**
         * Costruttore invocato dal server.
         *
         * @param socket Il socket che gestisce la connessione trai due dispositivi
         */
        public ChatConnection(Socket socket) throws IOException {
            Log.i(LOG_TAG, "Sono nel costruttore di ChatConnection per le connessioni ricevute.");

            connSocket = socket;

            //Crea il thread per la ricezione dei messaggi
            receivingThread = new ReceivingThread();

            //Crea il thread per l'invio dei messaggi
            sendingThread = new SendingThread();

            receivingThread.start();
            sendingThread.start();
        }

        /**
         * Costruttore invocato quando si vuole instaurare una
         * connessione con il server del dispositivo remoto.
         *
         * @param srvAddress L'indirizzo IP del dispositivo che ospita il server.
         * @param srvPort    La porta sul quale è in ascolto il server.
         * @param macAddress L'indirizzo MAC del dispositivo remoto in forma testuale.
         */
        public ChatConnection(InetAddress srvAddress, int srvPort, String macAddress) throws IOException {
            Log.i(LOG_TAG, "Sono nel costruttore di ChatConnection per le connessioni da effetturare.");

            //Poiché non è possibile eseguire operazioni di rete nel thread principale
            //dell'applicazione, il codice di questo costruttore viene eseguito in un thread
            //a parte, altrimenti verrà lanciata un'eccezione di tipo: android.os.NetworkOnMainThreadException.

            connSocket = new Socket(srvAddress, srvPort);

            this.macAddress = macAddress;

            //Crea il thread per la ricezione dei messaggi
            receivingThread = new ReceivingThread();

            //Crea il thread per l'invio dei messaggi
            sendingThread = new SendingThread();

            receivingThread.start();
            sendingThread.start();
        }


        /**
         * Spedisce il messaggio al thread designato all'invio dei messaggi (SendingThread).
         *
         * @param message Un'istanza di Message che rappresenta il messaggio composto dall'utente.
         */
        public void sendMessage(Message message) {
            sendingThread.deliverMessage(message);
        }

        /**
         * Nota: probabilmente non ci servirà.
         * Imposta l'indirizzo MAC del dispositivo remoto con cui si
         * è connessi.
         *
         * @param macAddress L'indirizzo MAC del dispositivo con cui si è connessi in forma di stringa.
         *
        public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        }*/

        /**
         * Restituisce l'indirizzo MAC del dispositivo remoto con il quale si è connessi.
         *
         * @return L'indirizzo MAC del dispositivo remoto in forma di stringa.
         */
        public String getMacAddress() {
            return macAddress;
        }

        /**
         * Classe interna che rappresenta il thread che si mette in ascolto
         * di messaggi provenienti dal dispositivo remoto.
         */
        private class ReceivingThread extends Thread {

            //Variabile d'istanza
            private ObjectInputStream objectInputStream;

            /**
             * Costruttore principale del thread.
             *
             * @throws IOException se non riesce ad inizializzare lo stream di input.
             */

            public ReceivingThread() throws IOException {

                objectInputStream = new ObjectInputStream(connSocket.getInputStream());
                Log.i(LOG_TAG, "Costruito thread per la ricezione dei messaggi provenienti dal dispositivo remoto.");

            }

            @Override
            public void run() {
                try {
                    Log.i(LOG_TAG, "Sono dentro al ReceivingThread.");

                    while (!Thread.currentThread().isInterrupted()) {
                        try {

                            //Leggi il messaggio che hanno inviato
                            Message message = (Message) objectInputStream.readObject();
                            Log.i(LOG_TAG, "ReceivingThread ha ricevuto un messaggio.");

                            if (message != null) {
                                if (macAddress == null)
                                    macAddress = message.getSender();

                                if (!message.getText().equals(DUMMY_MESSAGE)) {

                                    //Manda il messaggio all'activity/fragment interessata se è registrata
                                    if (mMessagesListener && conversingWith.equals(macAddress)) {
                                        Intent intent = new Intent(CostantKeys.ACTION_SEND_MESSAGE);
                                        intent.putExtra(CostantKeys.ACTION_SEND_MESSAGE_EXTRA, message);
                                        mLocalBroadcastManager.sendBroadcast(intent);
                                        Log.i(LOG_TAG, "Messaggio inviato all'activity di conversazione.");
                                    }
                                    else if (mContactsListener) {

                                        //Manda il messaggio all'activity/fragment principale che notificherà
                                        //l'arrivo di un nuovo messaggio
                                        Intent intent = new Intent(CostantKeys.ACTION_SEND_MESSAGE_FOR_CONTACTS);
                                        intent.putExtra(CostantKeys.ACTION_SEND_MESSAGE_EXTRA, message);
                                        mLocalBroadcastManager.sendBroadcast(intent);
                                        Log.i(LOG_TAG, "Messaggio inviato all'activity dei contatti.");

                                        //Salva il messaggio in memoria cosicché l'activity/fragment interessata
                                        //potrà recuperarlo e mostrarlo all'utente
                                        //mMessagesStore.saveMessage(message);
                                    }
                                    else {

                                        //Salva il messaggio nella memoria interna e nient'altro
                                        //mMessagesStore.saveMessage(message);
                                    }
                                }
                            }

                        } catch (ClassNotFoundException ex) {

                            //In caso di errore, interrompi il ciclo
                            Log.e(LOG_TAG, "Errore durante la ricezione del messaggio.");
                            break;
                        } catch (EOFException ex) {

                            //Questa eccezione indica che il dispositivo remoto ha chiuso lo stream
                            //di output. Quindi chiudi la connessione.
                            Log.e(LOG_TAG, "Il client si è disconnesso: " + ex.toString());
                            break;
                        }
                    }
                    Log.i(LOG_TAG, "Thread di ricezione interrotto. Ora chiudo la connessione.");
                    objectInputStream.close();
                    closeConnection();
                } catch (IOException ex) {
                    //Non è riuscito a chiudere lo stream
                    Log.e(LOG_TAG, "Impossibile chiudere lo stream di input dei messaggi: " + ex.toString());
                }
            }
        }

        /**
         * Thread che si occupa dell'invio dei messaggi al
         * dispositivo remoto a cui si è connessi.
         *
         * @author Daniele Porcelli
         */
        private class SendingThread extends Thread {

            //Variabili d'istanza
            private BlockingQueue<Message> messagesQueue;
            private ObjectOutputStream oos;

            //Costanti statiche
            private static final int QUEUE_CAPACITY = 10;

            /**
             * Costruttore del thread.
             *
             * @Throws IOException se non riesce ad inizializzare lo stream di output.
             */
            public SendingThread() throws IOException {
                Log.i(LOG_TAG, "Sono nel costruttore del thread di invio dei messaggi al dispositivo remoto.");

                //Inizializza la coda dei messaggi da inviare
                messagesQueue = new ArrayBlockingQueue<Message>(QUEUE_CAPACITY);

                //Inizializza lo stream di output
                oos = new ObjectOutputStream(connSocket.getOutputStream());
            }

            @Override
            public void run() {
                Log.i(LOG_TAG, "Sono dentro al SendingThread.");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //Rimane in ascolto per eventuali messaggi da inviare
                        Message messageToSend = messagesQueue.take();

                        //Manda il messaggio appena ottenuto dalla coda dei messaggi
                        oos.writeObject(messageToSend);
                        Log.i(LOG_TAG, "Messaggio inviato.");

                    } catch (IOException ex) {
                        //Errore durante l'invio del messaggio prelevato
                        Log.e(LOG_TAG, "Errore durante l'invio del messaggio: " + ex.toString());
                        break;

                    } catch (InterruptedException ex) {
                        //Si è verificata un'interruzione durante l'ottenimento
                        //del messaggio da inviare
                        Log.e(LOG_TAG, "Interruzione durante il prelevamento del messaggio da inviare: " + ex.toString());
                        break;
                    }
                }

                //Chiudi lo stream di output
                try {
                    oos.close();
                } catch (IOException ex) {

                    //Segnala l'eccezione, nulla di più
                    Log.e(LOG_TAG, "Errore durante la chiusura dello stream di output: " + ex.toString());
                }
            }

            /**
             * Inserisce nella coda dei messaggi il messaggio scritto dall'utente.
             *
             * @param message Il messaggio scritto dall'utente.
             */
            public void deliverMessage(Message message) {
                Log.i(LOG_TAG, "Messaggio aggiunto alla coda dei messaggi da inviare.");
                messagesQueue.add(message);
            }
        }

        public void closeConnection() {
            Log.i(LOG_TAG, "Sto chiudendo la connessione.");

            //Arresta i thread di ricezione e invio dei messaggi
            if (!sendingThread.isInterrupted())
                sendingThread.interrupt();
            if (!receivingThread.isInterrupted())
                receivingThread.interrupt();

            //Chiude il socket di comunicazione
            try {
                connSocket.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Errore durante la chiusura del socket: " + ex.toString());
            }

            //Informa le activity della disconnessione del dispositivo
            if (mContactsListener) {
                Intent intent = new Intent(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS);
                intent.putExtra(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA, macAddress);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            else if (mMessagesListener && conversingWith.equals(macAddress)) {
                Intent intent = new Intent(CostantKeys.ACTION_CONTACT_DISCONNECTED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            //Rimuovi l'indirizzo MAC del dispositivo con cui si sta comunicando se è lo stesso di
            //questa connessione
            if (conversingWith != null && conversingWith.equals(macAddress))
                conversingWith = null;

            //Rimuove questa connessione dalla lista delle connessioni attive
            synchronized (connections) {
                connections.remove(this);
            }
        }
    }

    private class ConnectThread extends Thread {

        private InetAddress ip;
        private int port;
        private String macAddress;

        public ConnectThread(InetAddress ip, int port, String macAddress) {
            this.ip = ip;
            this.port = port;
            this.macAddress = macAddress;
        }

        @Override
        public void run() {
            try {
                ChatConnection chatConnection = new ChatConnection(ip, port, macAddress);
                connections.add(chatConnection);
                Log.i(LOG_TAG, "Connessione con il dispositivo remoto riuscita.");

                //Invia il messaggio DUMMY per far registrare l'indirizzo MAC di questo dispositivo
                //al dispositivo destinatario.
                chatConnection.sendMessage(new Message(thisDeviceMAC, DUMMY_MESSAGE));
                Log.i(LOG_TAG, "Inviato messaggio dummy.");

                //Invia l'intent di broadcast che notifica la riuscita connessione con il dispositivo
                //remoto
                Intent intent = new Intent(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
                intent.putExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA, macAddress);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possibile connettersi con il dispositivo remoto: errore nella creazione di una ChatConnection.");
                ex.printStackTrace();

                if (mContactsListener) {
                    Intent intent = new Intent(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE);
                    intent.putExtra(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE_EXTRA, macAddress);
                    mLocalBroadcastManager.sendBroadcast(intent);
                }
            }
        }
    }

    public static void registerContactsListener(Context context) {
        Intent registerContactsListenerIntent = new Intent(context, MyService.class);
        registerContactsListenerIntent.setAction(ACTION_REGISTER_CONTACTS_LISTENER);
        context.startService(registerContactsListenerIntent);
    }

    public static void unRegisterContactsListener(Context context) {
        Intent unRegisterContactsListenerIntent = new Intent(context, MyService.class);
        unRegisterContactsListenerIntent.setAction(ACTION_UNREGISTER_CONTACTS_LISTENER);
        context.startService(unRegisterContactsListenerIntent);
    }

    public static void discoverServices(Context context) {
        Intent discoverServicesIntent = new Intent(context, MyService.class);
        discoverServicesIntent.setAction(ACTION_DISCOVER_SERVICES);
        context.startService(discoverServicesIntent);
    }

    /**
     * Metodo invocato dall'activity/fragment di conversazione per registrarsi come listener
     * dei messaggi in arrivo.
     *
     * @param context Un'istanza di tipo Context usata per invocare il servizio (startService() );
     */
    public static void registerMessagesListener(Context context) {
        Intent registerMessagesListenerIntent = new Intent(context, MyService.class);
        registerMessagesListenerIntent.setAction(ACTION_REGISTER_MESSAGES_LISTENER);
        context.startService(registerMessagesListenerIntent);
    }

    public static void unRegisterMessagesListener(Context context) {
        Intent unRegisterMessagesListenerIntent = new Intent(context, MyService.class);
        unRegisterMessagesListenerIntent.setAction(ACTION_UNREGISTER_MESSAGES_LISTENER);
        context.startService(unRegisterMessagesListenerIntent);
    }

    /**
     * Metodo statico invocato dall'activity principale per
     * connettersi e avviare una coversazione con il dispositivo
     * selezionato dall'utente.
     *
     * @param context L'oggetto di tipo Context che rappresenta il contesto dell'applicazione.
     * @param device  Indirizzo MAC del dispositivo a cui connettersi rappresentato in forma testuale.
     */
    public static void connectToClient(Context context, String device) {
        Intent connectToClientIntent = new Intent(context, MyService.class);
        connectToClientIntent.setAction(ACTION_CONNECT_TO_CLIENT);
        connectToClientIntent.putExtra(ACTION_CONNECT_TO_CLIENT_EXTRA, device);
        context.startService(connectToClientIntent);
    }

    public static void sendMessage(Context context, Message message) {
        Intent sendMessageIntent = new Intent(context, MyService.class);
        sendMessageIntent.setAction(ACTION_SEND_MESSAGE);
        sendMessageIntent.putExtra(ACTION_SEND_MESSAGE_EXTRA, message);
        context.startService(sendMessageIntent);
    }

    /**
     * Metodo statico invocato dall'activity/fragment di conversazione per controllare se
     * il contatto con cui si sta comunicando è ancora attivo (dopo che l'activity/fragment
     * è tornato attivo dallo stato di onStop() o onPause() ).
     *
     * @param context Un'istanza di Context usata per invocare il metodo startService().
     */
    public static void checkContactAvailable(Context context) {
        Intent checkContactIntent = new Intent(context, MyService.class);
        checkContactIntent.setAction(ACTION_CHECK_CONTACT_AVAILABLE);
        context.startService(checkContactIntent);
    }
}
