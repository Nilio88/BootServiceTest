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

import java.util.ArrayList;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class MyService extends Service {

    private static final String LOG_TAG = MyService.class.getName();

    private static final String ACTION_REGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_CONTACTS_LISTENER";
    private static final String ACTION_REGISTER_CONTACTS_LISTENER_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.REGISTER_CONTACTS_LISTENER_EXTRA";

    private static final String ACTION_DISCOVER_SERVICES = "com.sms1516.porcelli.daniele.wichat.action.DISCOVER_SERVICES";
    private static final String ACTION_UNREGISTER_CONTACTS_LISTENER = "com.sms1516.porcelli.daniele.wichat.action.UNREGISTER_CONTACTS_LISTENER";

    //Variabili d'istanza
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private IntentFilter mIntentFilter;
    private String thisDeviceMAC;
    private Thread mNsdService;
    private boolean mContactsListener;
    private Map<String, Integer> servicesConnectionInfo = new HashMap<>();

    public MyService() {
    }

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

        else if (intent.getAction().equals(ACTION_UNREGISTER_CONTACTS_LISTENER)) {
            Log.i(LOG_TAG, "Rimuovo il ContactsListener.");
            mContactsListener = false;
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

                if (fullDomainName.contains(SERVICE_NAME)) {
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
                /*try {
                    ChatConnection chatConn = new ChatConnection(clientSocket);
                    synchronized (connections) {
                        connections.add(chatConn);
                    }
                } catch (IOException ex) {
                    //Errore durante la connessione con il client
                    Log.e(LOG_TAG, "Errore durante la connessione con il client: " + ex.toString());

                    //Ritorna in ascolto di altri client
                    continue;
                }*/

            }
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
        /*private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {

                //Ottieni l'indirizzo IP
                InetAddress deviceIP = info.groupOwnerAddress;

                //Ottieni la porta del servizio remoto
                int port = servicesConnectionInfo.get(device);

                //Crea e avvia la connessione
                try {
                    ChatConnection chatConnection = new ChatConnection(deviceIP, port, device);
                    connections.add(chatConnection);

                    //Invia il messaggio DUMMY per far registrare l'indirizzo MAC di questo dispositivo
                    //al dispositivo destinatario.
                    chatConnection.sendMessage(new Message(thisDeviceMAC, DUMMY_MESSAGE));

                } catch (IOException ex) {
                    Log.e(LOG_TAG, "Non è stato possibile connettersi con " + device + ": " + ex.toString());
                    mContactsListener.onContactDisconnected(device);
                }
            }
        };*/

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "Sono nel onReceive() di WifiP2pBroadcastReceiver.");

            String action = intent.getAction();
            Log.i(LOG_TAG, "azione catturata dal WifiP2pBroadcastReceiver: " + intent.getAction());

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
                    //mManager.requestConnectionInfo(mChannel, connectionInfoListener);
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
}
