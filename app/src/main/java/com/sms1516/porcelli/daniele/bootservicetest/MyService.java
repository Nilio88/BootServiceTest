package com.sms1516.porcelli.daniele.bootservicetest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.os.SystemClock;
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
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class MyService extends Service {

    private static final String LOG_TAG = MyService.class.getName();
    private static final int PORT = 6770;

    //Costante che memorizza il tempo dato al dispositivo remoto entro il quale accettare la connessione Wi-Fi Direct.
    private static final long WIFI_DIRECT_CONNECTION_WAIT_TIME = 30000L;    //Al dispositivo remoto vengono dati 30 secondi entro i quali può accettare la connessione Wi-Fi Direct.

    //Costante usata per identificare il messaggio ricevuto dall'handler.
    private static final int REFUSED_WHAT = 1;

    //Costanti per le azioni e i parametri degli intent
    private static final String ACTION_REGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_CONTACTS_LISTENER";

    private static final String ACTION_DISCOVER_SERVICES = "com.sms1516.porcelli.daniele.wichat.action.DISCOVER_SERVICES";
    private static final String ACTION_UNREGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_CONTACTS_LISTENER";

    private static final String ACTION_CONNECT_TO_CLIENT = "com.sms1516.porcelli.daniele.wichat.action.CONNECT_TO_CLIENT";
    private static final String ACTION_CONNECT_TO_CLIENT_EXTRA = "com.sms1516.porcelli.daniele.wichat.extra.DEVICE";

    private static final String ACTION_DISCONNECT = "com.sms1516.porcelli.daniele.wichat.action.DISCONNECT";

    private static final String ACTION_REGISTER_MESSAGES_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_MESSAGES_LISTENER";

    private static final String ACTION_UNREGISTER_MESSAGES_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_MESSAGES_LISTENER";

    private static final String ACTION_SEND_MESSAGE = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE";
    private static final String ACTION_SEND_MESSAGE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE_EXTRA";
    private static final String ACTION_CHECK_CONTACT_AVAILABLE = "com.sms1516.porcelli.daniele.wichat.action.CHECK_CONTACT_AVAILABLE";

    private static final String ACTION_DELETE_MESSAGES = "com.sms1516.porcelli.daniele.wichat.action.DELETE_MESSAGES";
    private static final String ACTION_DELETE_MESSAGES_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.DELETE_MESSAGES_EXTRA";

    private static final String ACTION_WHO_IS_CONNECTED = "com.sms1516.porcelli.daniele.wichat.action.WHO_IS_CONNECTED";

    private static final String ACTION_CANCEL_CONNECT = "com.sms1516.porcelli.daniele.wichat.action.CANCEL_CONNECT";

    //Variabili d'istanza
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private IntentFilter mIntentFilter;
    private String conversingWith;  //Memorizzerà l'indirizzo MAC del dispositivo con cui l'utente sta conversando in questo momento
    private InetAddress remoteDeviceIPAddress;  //Memorizzerà l'indirizzo IP del dispositivo remoto
    private Thread mNsdService;
    private Thread serverThread;
    private Thread mHandlerThread;
    private ConnectThread connectThread;
    private boolean mContactsListener;
    private boolean mMessagesListener;
    private boolean mIRequested;    //Memorizzerà lo stato che indica se è stato l'utente a richiedere la connessione oppure no.
    private String thisDeviceAddress;
    private ChatConnection currentConnection;
    private MessagesStore mMessagesStore;

    //Dizionario che conserva le coppie (indirizzo, nome) per l'associazione di un
    //nome più amichevole al dispositivo individuato
    private final HashMap<String, String> buddies = new HashMap<>();

    //Handler che elabora il messaggio REFUSED_WHAT ricevuto quando
    //il dispositivo remoto ci mette più di 30 secondi per accettare
    //la connessione con il nostro dispositivo tramite Wi-Fi Direct.
    private Handler mHandler;


    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "Sono in onCreate() di MyService");

        //Inizializza il MessagesStore
        Log.i(LOG_TAG, "Sto inizializzando il MessagesStore");
        MessagesStore.initialize(this);
        Log.i(LOG_TAG, "MessagesStore inizializzato.");

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mMessagesStore = MessagesStore.getInstance();
        mHandlerThread = new HandlerThread();

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
            mHandlerThread.start();

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

            //Controlla che non ci sia già una connessione con un dispositivo remoto
            //Se non è stata trovata una connessione esistente con un dispositivo, la crea
            if (conversingWith == null) {

                //Memorizzo lo stato che è stato questo dispositivo a richiedere la connessione.
                mIRequested = true;

                //Si connette con il dispositivo tramite Wi-Fi direct
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device;
                config.wps.setup = WpsInfo.PBC;

                //Crea il messaggio per l'handler e invialo dopo 30 secondi a
                //partire da ora.
                Log.i(LOG_TAG, "Preparo il messaggio per l'handler.");

                final android.os.Message refusedMessage = mHandler.obtainMessage(REFUSED_WHAT);
                mHandler.sendMessageAtTime(refusedMessage, SystemClock.uptimeMillis() + WIFI_DIRECT_CONNECTION_WAIT_TIME);
                Log.i(LOG_TAG, "Tra 30 secondi arriverà il messaggio all'handler.");

                //Chiama il metodo di WifiP2pManager per stabilire una connessione Wi-Fi Direct con il dispositivo remoto.
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        //Viene mandato l'intent di broadcast WIFI_P2P_CONNECTION_CHANGED_ACTION

                        //Imposta conversingWith con l'indirizzo MAC del dispositivo remoto che l'utente
                        //ha cliccato.
                        conversingWith = device;

                        Log.i(LOG_TAG, "Sono riuscito a connettermi con il dispositivo remoto. Aspetto le informazioni di connessione.");

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
                                errore = "si è verificato un errore durante la connessione con il dispositivo remoto tramite Wi-Fi Direct.";
                                break;
                        }
                        Log.e(LOG_TAG, "Impossibile collegarsi con il client tramite Wi-Fi Direct: " + errore);

                        //Informa le activity/fragment che il dispositivo non è reperibile
                        if (mContactsListener) {
                            Intent intent = new Intent(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE);
                            intent.putExtra(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE_EXTRA, device);
                            mLocalBroadcastManager.sendBroadcast(intent);
                            Log.i(LOG_TAG, "Inviato intent per notificare la disconnessione di un dispositivo all'activity dei contatti.");
                        }

                    }
                });
            } else if (conversingWith != null && !Utils.isMacSimilar(conversingWith, device)) {

                //Invia l'intent per richiedere la disconnessione dal dispositivo corrente
                Intent disconnectIntent = new Intent(CostantKeys.ACTION_SEND_DISCONNECT_REQUEST);
                disconnectIntent.putExtra(CostantKeys.ACTION_SEND_DISCONNECT_REQUEST_EXTRA, conversingWith);
                mLocalBroadcastManager.sendBroadcast(disconnectIntent);
            }

            else if (Utils.isMacSimilar(conversingWith, device) && currentConnection != null) {

                //L'utente ha cliccato su un contatto con cui ha già stabilito una connessione,
                //quindi avvisa l'activity.
                Intent connectedIntent = new Intent(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
                connectedIntent.putExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA, conversingWith);
                mLocalBroadcastManager.sendBroadcast(connectedIntent);

            }
        }

        else if (intent.getAction().equals(ACTION_DISCONNECT)) {
            Log.i(LOG_TAG, "Il Service ha ricevuto la richiesta di disconnessione.");

            //Invoca il metodo per disconnettere la connessione con il dispositivo
            //remoto attuale (se presente)
            if (conversingWith != null) {
                Log.i(LOG_TAG, "Eseguo la disconnessione.");
                Log.i(LOG_TAG, "Chiudo la ChatConnection.");

                if (currentConnection != null) {
                    currentConnection.closeConnection();
                    currentConnection = null;
                    Log.i(LOG_TAG, "ChatConnection chiusa.");
                }

                remoteDeviceIPAddress = null;
                conversingWith = null;

                mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.i(LOG_TAG, "Connessione Wi-Fi Direct chiusa.");

                        //Invia l'intent di broadcast locale per informare l'activity dei contatti
                        //della riuscita disconnessione.
                        Intent disconnectSuccessIntent = new Intent(CostantKeys.ACTION_DISCONNECT_SUCCESSFUL);
                        mLocalBroadcastManager.sendBroadcastSync(disconnectSuccessIntent);

                        //A quanto pare, per poter permettere di connettersi ad altri dispositivi
                        //dopo essersi disconnessi da quello precedente, c'è bisogno riavviare
                        //la ricerca dei servizi.
                        Log.i(LOG_TAG, "Riavvio la ricerca dei servizi.");

                        discoverServices();
                    }

                    @Override
                    public void onFailure(int reasonCode) {

                        //Si è verificato un errore. Esso verrà registrato nel Log.
                        String errore = null;
                        switch (reasonCode) {
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
                        Log.i(LOG_TAG, "Disconnessione fallita: " + errore);
                    }
                });
            }
            else
                Log.i(LOG_TAG, "Disconnessione non eseguita: conversingWith è null.");
        }

        else if (intent.getAction().equals(ACTION_REGISTER_MESSAGES_LISTENER)) {

            //Registra il MessagesListener
            mMessagesListener = true;

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

            if (currentConnection != null) {
                //Invia il messaggio composto dall'utente
                Log.i(LOG_TAG, "Invio il messaggio.");
                currentConnection.sendMessage(message);
            }
            else
                Log.i(LOG_TAG, "Non è stato possibile inviare il messaggio: non c'è alcuna connessione.");
        }

        else if (intent.getAction().equals(ACTION_CHECK_CONTACT_AVAILABLE)) {

            //Controlla se la connessione con il contatto con cui si sta comunicando è ancora attiva,
            //quindi notifica l'activity/fragment del risultato.
            Intent contactAvailabilityIntent = new Intent(CostantKeys.ACTION_CONTACT_AVAILABILITY);
            contactAvailabilityIntent.putExtra(CostantKeys.ACTION_CONTACT_AVAILABILITY_EXTRA, conversingWith != null);
            mLocalBroadcastManager.sendBroadcast(contactAvailabilityIntent);
        }

        else if (intent.getAction().equals(ACTION_DELETE_MESSAGES)) {

            //Chiama il metodo di MessagesStore per cancellare il file
            //contenente la cronologia dei messaggi ricevuti (e inviati) dal
            //contatto il cui indirizzo MAC è fornito in input.
            String contatto = intent.getStringExtra(ACTION_DELETE_MESSAGES_EXTRA);

            Log.i(LOG_TAG, "Cancello la cronologia dei messaggi di: " + buddies.get(contatto));
            mMessagesStore.deleteMessages(contatto);
        }

        else if (intent.getAction().equals(ACTION_WHO_IS_CONNECTED)) {

            //Crea un intent da passare all'activity dei contatti con dentro
            //l'indirizzo MAC del contatto (dispositivo remoto) con il quale
            //il nostro dispositivo è già connesso. Se non è connesso con alcun
            //contatto, inserirà nell'intent il valore null (in pratica non fa altro
            //che inserire nell'intent il valore memorizzato in conversingWith).
            Log.i(LOG_TAG, "Restituisco all'activity l'indirizzo MAC del dispositivo già connesso (conversingWith).");

            Intent contactConnectedIntent = new Intent(CostantKeys.ACTION_CONTACT_CONNECTED);
            contactConnectedIntent.putExtra(CostantKeys.ACTION_CONTACT_CONNECTED_EXTRA, conversingWith);
            mLocalBroadcastManager.sendBroadcast(contactConnectedIntent);
        }

        else if (intent.getAction().equals(ACTION_CANCEL_CONNECT)) {

            //Invoca il metodo della classe WifiP2pManager cancelConnect()
            //per disdire la richiesta di connessione tramite Wi-Fi Direct con
            //il dispositivo remoto.
            Log.i(LOG_TAG, "Cancello la richiesta di connessione Wi-Fi Direct con il dispositivo remoto.");

            mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.i(LOG_TAG, "Richiesta di connessione cancellata con successo.");
                    conversingWith = null;
                    mIRequested = false;
                }

                @Override
                public void onFailure(int reason) {

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

                    Log.i(LOG_TAG, "Non è stato possibile cancellare la richiesta di connessione: " + errore + ".");
                }
            });
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

        //Per sicurezza, rimuovi ogni precedente richiesta dal WifiP2pManager
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

        ServerSocket server;

        //Implementazione del listener dei TXT record
        private final WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                Log.i(LOG_TAG, "Catturato un TXT Record. fullDomainName = " + fullDomainName);

                if (fullDomainName.contains(SERVICE_NAME.toLowerCase())) {
                    buddies.put(srcDevice.deviceAddress, txtRecordMap.get(NICKNAME));
                    //servicesConnectionInfo.put(srcDevice.deviceAddress, Integer.parseInt(txtRecordMap.get(LISTEN_PORT)));
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

            /*try {

                server = new ServerSocket(6770);

            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile avviare il server: " + ex.toString());
                return;
            }*/

            /*int port = server.getLocalPort();
            Log.i(LOG_TAG, "Ho creato il server. Porta: " + port);*/

            //Log.i(LOG_TAG, "Indirizzo IP di questo dispositivo: " + server.getInetAddress().toString());

            //Recupera l'indirizzo MAC di questo dispositivo per inserirlo nei messaggi che invierà
            Log.i(LOG_TAG, "Recupero l'indirizzo MAC del dispositivo.");

            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            thisDeviceAddress = info.getMacAddress().toLowerCase();
            Log.i(LOG_TAG, "Indirizzo MAC di questo dispositivo nel Service: " + thisDeviceAddress);

            //Ottiene il nome del proprietario di questo dispositivo Android
            Cursor cursor = getContentResolver().query(Profile.CONTENT_URI, null, null, null, null);
            cursor.moveToFirst();
            String proprietario = cursor.getString(cursor.getColumnIndex("display_name"));

            Log.i(LOG_TAG, "Nome proprietario del dispositivo: " + proprietario);

            //Crea il TXT record da inviare agli altri dispositivi che hanno installato WiChat
            Map<String, String> txtRecord = new HashMap<>();
            //txtRecord.put(LISTEN_PORT, String.valueOf(PORT));
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
            //discoverServices();

            //Avvia l'ascolto di connessioni in entrata
            //Nota: commentato perché è stato messo in un thread apposito
            /*while (!Thread.currentThread().isInterrupted()) {
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

            }*/
            /*Log.i(LOG_TAG, "NsdProviderThread fuori dal ciclo while.");
            try {
                server.close();
            }
            catch(IOException ex) {
                //Niente di importante da fare.
            }*/

            //Crea e avvia il server
            serverThread = new StartServerThread();
            serverThread.start();
        }

        /*public String getIPAddress() {
            return server.getInetAddress().toString();
        }*/
    }

    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {

        //Implementazione del ConnectionInfoListener per recuperare l'indirizzo IP
        //del dispositivo a cui si è appena connessi
        private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                Log.i(LOG_TAG, "Sono in onConnectionInfoAvailable(). Informazioni sulla connessione catturate.");

                //Recupero l'indirizzo IP di questo dispositivo
                InetAddress localIp = Utils.getLocalIPAddress();
                Log.i(LOG_TAG, "L'indirizzo IP di questo dispositivo recuperato è: " + localIp.getHostAddress());

                //Controlla se l'indirizzo IP locale recuperato è uguale a quello
                //del group owner. Nel caso in cui non siano uguali, allora deve
                //avviare la connessione tramite socket all'indirizzo IP del group
                //owner. Se, invece, sono uguali, rimane in attesa di una connessione
                //tramite socket da parte del dispositivo remoto.
                if (!info.groupOwnerAddress.equals(localIp)) {

                    remoteDeviceIPAddress = info.groupOwnerAddress;

                    //Un dispositivo remoto si è connesso al nostro.
                    Log.i(LOG_TAG, "Mi connetto con il group owner: " + remoteDeviceIPAddress.getHostAddress());

                    //Connettiti al dispositivo remoto che si è appena connesso a questo dispositivo
                    Log.i(LOG_TAG, "Ora avvio ConnectThread.");

                    connectThread = new ConnectThread();
                    connectThread.start();

                }

                else {

                    Log.i(LOG_TAG, "Sono io il group owner. Attendo la connessione socket.");
                }

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

                    //Interrompe il serverThread
                    if (serverThread != null && !serverThread.isInterrupted())
                        serverThread.interrupt();
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {

                //Questo dispositivo si è appena connesso/disconnesso con un altro tramite Wi-Fi Direct.
                //Recuperiamo le informazioni di connessione di conseguenza.
                if (mManager == null) {
                    return;
                }
                Log.i(LOG_TAG, "La connessione con il dispositivo remoto è cambiata.");

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.i(LOG_TAG, "Il dispositivo si è connesso con un dispositivo remoto.");

                    //Rimuovo il messaggio da inviare all'handler.
                    if (mIRequested) {
                        mHandler.removeCallbacksAndMessages(null);
                        Log.i(LOG_TAG, "Messaggio da inviare all'handler rimosso.");
                    }

                    //Si è appena connesso ad un dispositivo remoto: otteniamo le informazioni della connessione
                    mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                }
                else {
                    //Si tratta di una disconnessione dal dispositivo remoto.
                    Log.i(LOG_TAG, "Dispositivo remoto si è disconnesso.");

                    if (currentConnection != null) {
                        Log.i(LOG_TAG, "Imposto il currentConnection a null.");
                        currentConnection = null;
                    }

                    //Riavvia il serverThread se è interrotto
                    if (serverThread != null && !serverThread.isAlive()) {
                        Log.i(LOG_TAG, "Riavvio il serverThread.");

                        serverThread = new StartServerThread();
                        serverThread.start();
                    }

                    remoteDeviceIPAddress = null;

                    if (conversingWith != null) {

                        //Avvisa l'activity dei contatti che il dispositivo remoto ha chiuso la connessione
                        if (mContactsListener) {
                            Intent contactDisconnectedIntent = new Intent(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS);
                            contactDisconnectedIntent.putExtra(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA, conversingWith);
                            mLocalBroadcastManager.sendBroadcast(contactDisconnectedIntent);
                        }

                        //Avvisa l'activity/fragment della conversazione che il dispositivo remoto ha chiuso la connessione
                        if (mMessagesListener) {
                            Intent contactDisconnectedIntent = new Intent(CostantKeys.ACTION_CONTACT_DISCONNECTED);
                            mLocalBroadcastManager.sendBroadcast(contactDisconnectedIntent);
                        }

                        conversingWith = null;

                        //A quanto pare, per poter permettere di connettersi ad altri dispositivi
                        //dopo essersi disconnessi da quello precedente, c'è bisogno di riavviare
                        //la ricerca dei servizi.
                        Log.i(LOG_TAG, "Riavvio la ricerca dei servizi.");

                    }
                    //Ritorna a cercare i dispositivi nelle vicinanze, altrimenti
                    //nessun altro dispositivo remoto si potrà connettere con quello
                    //in uso.
                    discoverServices();

                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {

                //Recupera la nuova lista di contatti disponibili nel range del Wi-Fi
                //Nota: probabilmente neanche questo ci servirà, ma lo teniamo per sicurezza.
                //mManager.requestPeers(mChannel, peerListListener);
            }

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
        //private String macAddress;
        private SendingThread sendingThread;
        private ReceivingThread receivingThread;

        private static final int TIMEOUT = 5000;

        /**
         * Costruttore invocato dal server.
         *
         * @param socket Il socket che gestisce la connessione trai due dispositivi
         */
        public ChatConnection(Socket socket) throws IOException {
            Log.i(LOG_TAG, "Sono nel costruttore di ChatConnection per le connessioni ricevute.");

            connSocket = socket;
            Log.i(LOG_TAG, "Ho impostato connSocket.");

            Log.i(LOG_TAG, "Recupero l'indirizzo IP del dispositivo remoto al quale la socket è connessa.");
            remoteDeviceIPAddress = connSocket.getInetAddress();

            Log.i(LOG_TAG, "Indirizzo IP del dispositivo remoto: " + remoteDeviceIPAddress.getHostAddress());

            //Crea il thread per la ricezione dei messaggi
            Log.i(LOG_TAG, "Creo l'istanza di ReceivingThread.");
            receivingThread = new ReceivingThread();

            //Crea il thread per l'invio dei messaggi
            Log.i(LOG_TAG, "Creo l'istanza di SendingThread.");
            sendingThread = new SendingThread();

            Log.i(LOG_TAG, "Avvio ReceivingThread.");
            receivingThread.start();

            Log.i(LOG_TAG, "Avvio SendingThread.");
            sendingThread.start();
        }

        /**
         * Costruttore invocato quando si vuole instaurare una
         * connessione con il server del dispositivo remoto.
         *
         */
        public ChatConnection(/*InetAddress srvAddress, int srvPort, String macAddress*/) throws IOException {
            Log.i(LOG_TAG, "Sono nel costruttore di ChatConnection per le connessioni da effetturare.");

            //Poiché non è possibile eseguire operazioni di rete nel thread principale
            //dell'applicazione, il codice di questo costruttore viene eseguito in un thread
            //a parte, altrimenti verrà lanciata un'eccezione di tipo: android.os.NetworkOnMainThreadException.

            connSocket = new Socket();
            Log.i(LOG_TAG, "Socket inizializzata.");

            connSocket.bind(null);
            Log.i(LOG_TAG, "Socket legata ad un IP e ad una porta.");

            connSocket.connect(new InetSocketAddress(remoteDeviceIPAddress, PORT), TIMEOUT);
            Log.i(LOG_TAG, "Socket connessa.");

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
        /*public String getMacAddress() {
            return macAddress;
        }*/

        /**
         * Classe interna che rappresenta il thread che si mette in ascolto
         * di messaggi provenienti dal dispositivo remoto.
         */
        private class ReceivingThread extends Thread {

            //Variabili d'istanza
            private ObjectInputStream objectInputStream;

            /**
             * Costruttore principale del thread.
             *
             * @throws IOException se non riesce ad inizializzare lo stream di input.
             */

            public ReceivingThread() throws IOException {
                Log.i(LOG_TAG, "Sono nel costruttore di ReceivingThread.");

            }

            @Override
            public void run() {
                try {
                    Log.i(LOG_TAG, "Sono dentro al run() di ReceivingThread.");

                    objectInputStream = new ObjectInputStream(connSocket.getInputStream());
                    Log.i(LOG_TAG, "Costruito thread per la ricezione dei messaggi provenienti dal dispositivo remoto.");

                    while (!Thread.currentThread().isInterrupted()) {
                        try {

                            //Leggi il messaggio che hanno inviato
                            Message message = (Message) objectInputStream.readObject();
                            Log.i(LOG_TAG, "ReceivingThread ha ricevuto un messaggio.");

                            if (message != null) {
                                /*if (conversingWith == null) {
                                    conversingWith = message.getSender();
                                    Log.i(LOG_TAG, "Ottenuto messaggio DUMMY. conversingWith = " + conversingWith);

                                    //Invia l'intent all'activity dei contatti per informagli che la connessione tramite
                                    //socket è stata stabilita e che può, quindi, proseguire con l'avvio dell'activity/fragment
                                    //per la conversazione.
                                    Intent connectedIntent = new Intent(CostantKeys.ACTION_CONNECTION_RECEIVED);
                                    connectedIntent.putExtra(CostantKeys.ACTION_CONNECTION_RECEIVED_EXTRA, conversingWith);
                                    mLocalBroadcastManager.sendBroadcast(connectedIntent);
                                }*/

                                //if (!message.getText().equals(DUMMY_MESSAGE)) {

                                    //Manda il messaggio all'activity/fragment interessata se è registrata
                                    if (mMessagesListener) {
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
                                        mMessagesStore.saveMessage(message);
                                    }
                                    else {

                                        //Salva il messaggio nella memoria interna
                                        mMessagesStore.saveMessage(message);
                                    }
                                //}
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
                Log.i(LOG_TAG, "Sono nel costruttore di SendingThread.");

                //Inizializza la coda dei messaggi da inviare
                messagesQueue = new ArrayBlockingQueue<Message>(QUEUE_CAPACITY);

            }

            @Override
            public void run() {
                Log.i(LOG_TAG, "Sono dentro al SendingThread.");

                try {

                    //Inizializza lo stream di output
                    oos = new ObjectOutputStream(connSocket.getOutputStream());
                    Log.i(LOG_TAG, "Inizializzo l'ObjcetOutputStream.");

                    //Invia il messaggio DUMMY per far ottenere subito l'indirizzo MAC
                    //di questo dispositivo al dispositivo remoto.
                    //oos.writeObject(new Message(thisDeviceAddress, DUMMY_MESSAGE));
                    //oos.flush();
                    //Log.i(LOG_TAG, "Inviato messaggio DUMMY.");

                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            //Rimane in ascolto per eventuali messaggi da inviare
                            Message messageToSend = messagesQueue.take();

                            //Manda il messaggio appena ottenuto dalla coda dei messaggi
                            oos.writeObject(messageToSend);
                            oos.flush();
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
                }
                catch(IOException ex) {
                    //Si è verificato un errore durante l'inzializzazione dell'ObjectOutputStream
                    Log.i(LOG_TAG, "Si è verificato un errore durante l'inzializzazione dell'ObjectOutputStream:");
                    ex.printStackTrace();
                }

                //Chiudi lo stream di output
                try {
                    if (oos != null)
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
            /*if (mContactsListener) {
                Intent intent = new Intent(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS);
                intent.putExtra(CostantKeys.ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA, macAddress);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            else if (mMessagesListener && conversingWith.equals(macAddress)) {
                Intent intent = new Intent(CostantKeys.ACTION_CONTACT_DISCONNECTED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }*/

            //Rimuovi l'indirizzo MAC del dispositivo con cui si sta comunicando se è lo stesso di
            //questa connessione
            /*if (conversingWith != null && conversingWith.equals(macAddress))
                conversingWith = null;

            //Rimuove questa connessione dalla lista delle connessioni attive
            synchronized (connections) {
                connections.remove(this);
            }*/
        }
    }

    private class ConnectThread extends Thread {

        /*private InetAddress ip;
        private int port;
        private String macAddress;*/

        /*public ConnectThread(InetAddress ip, int port, String macAddress) {
            this.ip = ip;
            this.port = port;
            this.macAddress = macAddress;
        }*/

        @Override
        public void run() {
            try {
                currentConnection = new ChatConnection();
                //connections.add(chatConnection);
                Log.i(LOG_TAG, "Connessione con il dispositivo remoto riuscita.");

                //Ottieni l'indirizzo MAC del dispositivo remoto se non è ancora
                //stato impostato (questo dispositivo ha ricevuto una richiesta di conversazione).
                if (conversingWith == null)
                    conversingWith = Utils.getMac(remoteDeviceIPAddress.getHostAddress());

                Log.i(LOG_TAG, "Indirizzo MAC del dispositivo che ha richiesto la connessione: " + conversingWith);

                //Avvisa l'activity dei contatti della riuscita connessione con il
                //dispositivo remoto.
                if (mContactsListener) {
                    if (mIRequested) {
                        //Manda l'intent che avvia l'activity/fragment di conversazione.
                        Log.i(LOG_TAG, "Invio l'intent che avvia la conversazione.");

                        mIRequested = false;

                        Intent connectedIntent = new Intent(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
                        connectedIntent.putExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA, conversingWith);
                        mLocalBroadcastManager.sendBroadcast(connectedIntent);
                    }
                    else {
                        //Manda l'intent per la notifica che il contatto si è connesso a questo dispositivo.
                        Log.i(LOG_TAG, "Invio l'intent che indica che il contatto si è connesso con questo dispositivo.");

                        Intent connectedIntent = new Intent(CostantKeys.ACTION_CONNECTION_RECEIVED);
                        connectedIntent.putExtra(CostantKeys.ACTION_CONNECTION_RECEIVED_EXTRA, conversingWith);
                        mLocalBroadcastManager.sendBroadcast(connectedIntent);
                    }
                }

            }
            catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possibile connettersi con il dispositivo remoto: errore nella creazione di una ChatConnection.");
                ex.printStackTrace();

                if (mContactsListener) {
                    Intent intent = new Intent(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE);
                    intent.putExtra(CostantKeys.ACTION_CONTACT_NOT_AVAILABLE_EXTRA, conversingWith);
                    mLocalBroadcastManager.sendBroadcast(intent);
                }
            }
        }
    }

    /**
     * Thread avviato per inizializzare il ServerSocket per le connessioni in entrata.
     * È necessario inizializzare il ServerSocket in un thread altrimenti viene lanciata
     * l'eccezione
     */
    private class StartServerThread extends Thread {

        private ServerSocket server;

        @Override
        public void run() {
            try {
                Log.i(LOG_TAG, "Sono nel thread che crea il serverSocket.");
                server = new ServerSocket(PORT);
            }
            catch(IOException ex) {
                Log.i(LOG_TAG, "Non sono riuscito a istanziare il ServerSocket.");
                ex.printStackTrace();
                return;
            }
            try {
                Log.i(LOG_TAG, "Sto aspettando la richiesta di connessione da parte del client.");
                Socket client = server.accept();
                Log.i(LOG_TAG, "Client connesso.");
                currentConnection = new ChatConnection(client);

                Log.i(LOG_TAG, "Connessione da parte del dispositivo remoto riuscita.");

                //Recupera l'indirizzo MAC del dispositivo remoto se non
                //è stato impostato ancora (quando si riceve una richiesta
                //di connessione).
                if (conversingWith == null) {

                    conversingWith = Utils.getMac(remoteDeviceIPAddress.getHostAddress());
                    Log.i(LOG_TAG, "Recuperato indirizzo MAC del dispositivo remoto: conversingWith = " + conversingWith);
                }

                //Informa l'activity dei contatti che è stata
                //stabilita la connessione.
                if (mContactsListener) {
                    if (mIRequested) {
                        //Invia l'intent per l'avvio dell'activity/fragment per la conversazione.
                        Log.i(LOG_TAG, "Invio l'intent per l'avvio dell'activity/fragment della conversazione.");

                        mIRequested = false;

                        Intent connectedIntent = new Intent(CostantKeys.ACTION_CONNECTED_TO_DEVICE);
                        connectedIntent.putExtra(CostantKeys.ACTION_CONNECTED_TO_DEVICE_EXTRA, conversingWith);
                        mLocalBroadcastManager.sendBroadcast(connectedIntent);
                    }
                    else {
                        //Invia l'intent per la notifica della connessione del contatto.
                        Log.i(LOG_TAG, "Invio l'intent per la notifica di connessione con il dispositivo remoto.");

                        Intent connectedIntent = new Intent(CostantKeys.ACTION_CONNECTION_RECEIVED);
                        connectedIntent.putExtra(CostantKeys.ACTION_CONNECTION_RECEIVED_EXTRA, conversingWith);
                        mLocalBroadcastManager.sendBroadcast(connectedIntent);
                    }
                }
            }
            catch(IOException ex) {
                //Errore durante la connessione con il client
                conversingWith = null;
                Log.e(LOG_TAG, "Errore durante la connessione con il client: " + ex.toString());
                ex.printStackTrace();
            }

            try {
                server.close();
            }
            catch(IOException ex) {
                //Niente di importante da fare.
            }

        }
    }

    private class HandlerThread extends Thread {

        @Override
        public void run() {
            Looper.prepare();

            mHandler = new Handler () {

                @Override
                public void handleMessage(android.os.Message msg) {
                    switch(msg.what) {
                        case REFUSED_WHAT:

                            //Manda un intent all'activity dei contatti che notifica
                            //che il contatto non ha accettato la connessione Wi-Fi Direct
                            //entro il tempo limite di 30 secondi.
                            Log.i(LOG_TAG, "Sono nell'handler.");

                            if (currentConnection == null && mContactsListener) {
                                Log.i(LOG_TAG, "Il contatto non ha accettato la connessione.");
                                mIRequested = false;
                                Intent connectionRefusedIntent = new Intent(CostantKeys.ACTION_CONNECTION_REFUSED);
                                mLocalBroadcastManager.sendBroadcastSync(connectionRefusedIntent);
                                Log.i(LOG_TAG, "Intent ACTION_CONNECTION_REFUSED inviato.");
                            }
                            conversingWith = null;
                    }
                }
            };
            Looper.loop();
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

    public static void deleteMessages(Context context, String device) {
        Intent deleteMessagesIntent = new Intent(context, MyService.class);
        deleteMessagesIntent.setAction(ACTION_DELETE_MESSAGES);
        deleteMessagesIntent.putExtra(ACTION_DELETE_MESSAGES_EXTRA, device);
        context.startService(deleteMessagesIntent);
    }

    /**
     * Metodo statico invocato dall'activity dei contatti per confermare la disconnessione
     * Wi-Fi Direct con il dispositivo remoto.
     *
     * @param context Un'istanza di Context utilizzata per invocare il metodo startService().
     */
    public static void disconnect(Context context) {
        Intent disconnectIntent = new Intent(context, MyService.class);
        disconnectIntent.setAction(ACTION_DISCONNECT);
        context.startService(disconnectIntent);
    }

    /**
     * Metodo statico invocato dall'acitivty dei contatti per informare tale
     * activity se e a quale contatto è già connesso il dispositivo in uso.
     *
     * @param context Un'istanza di Context utilizzata per invocare il metodo startService().
     */
    public static void whoIsConnected(Context context) {
        Intent whoIsConnectedIntent = new Intent(context, MyService.class);
        whoIsConnectedIntent.setAction(ACTION_WHO_IS_CONNECTED);
        context.startService(whoIsConnectedIntent);
    }

    /**
     * Metodo statico invocato dall'activity dei contatti per cancellare
     * la richiesta di connessione Wi-Fi Direct con il dispositivo remoto.
     *
     * @param context Un'istanza di Context utilizzata per invocare il metodo startService().
     */
    public static void cancelConnect(Context context) {
        Intent cancelConnectIntent = new Intent(context, MyService.class);
        cancelConnectIntent.setAction(ACTION_CANCEL_CONNECT);
        context.startService(cancelConnectIntent);
    }
}
