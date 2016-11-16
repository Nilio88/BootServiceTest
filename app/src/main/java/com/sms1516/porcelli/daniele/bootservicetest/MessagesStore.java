package com.sms1516.porcelli.daniele.bootservicetest;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.LinkedList;

import android.util.Log;

/**
 * Questa classe si occupa di salvare i messaggi
 * ricevuti (sia quelli letti che quelli ancora da leggere)
 * nella memoria di massa del dispositivo.
 */
public class MessagesStore {

    //Variabili d'istanza
    private Context context;

    //Variabili statiche
    private static MessagesStore instance;

    //Costante per il Log
    private static final String LOG_TAG = MessagesStore.class.getName();

    /**
     * Costruttore della classe.
     *
     * @param context Oggetto di tipo Context per ottenere le directory dove salvare i messaggi.
     */
    private MessagesStore(Context context) {
        this.context = context;
    }

    /**
     * Inizializza il MessagesStore. Questo metodo deve essere invocato
     * prima di ottenerne l'istanza.
     *
     * @param context Oggetto di tipo Context per ottenere le directory dove salvare i messaggi.
     */
    public static void initialize(Context context) {
        if (instance == null)
            instance = new MessagesStore(context);
    }

    /**
     * Salva in memoria interna il messaggio ricevuto.
     *
     * @param message Il messaggio ricevuto
     */
    public synchronized void saveMessage(Message message) {
        File messagesFile = null;

        //Controlla se esiste già il file dove memorizzare il messaggio
        File[] files = context.getFilesDir().listFiles();

        //Scandisce i file presenti nella directory base dell'applicazione.
        //Se trova un file, controlla se il suo nome è simile all'indirizzo MAC
        //del mittente del messaggio da memorizzare e, se è simile, lo memorizza
        //lì dentro. Se trova una directory, essa verrà ignorata. Se nessun file
        //viene ritrovato, verrà creato uno nuovo.

        boolean trovato = false;
        int i;
        for (i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                //Controlla se il nome del file è simile all'indirizzo MAC
                //del mittente.
                if (Utils.isMacSimilar(files[i].getName(), message.getSender().replace(":", ""))) {
                    trovato = true;
                    break;
                }
            }
        }

        if (!trovato) {
            Log.i(LOG_TAG, "File non trovato. Ne creo uno nuovo.");

            //Il file che memorizza i messaggi provenienti dal mittente non è stato
            //trovato. Ne crea uno nuovo.
            try {
                messagesFile = new File(context.getFilesDir(), message.getSender().replace(":", ""));
                messagesFile.createNewFile();
            }
            catch (IOException ex) {
                //Non è stato possibile creare il file
                Log.e(LOG_TAG, "Impossibile creare il file: " + ex.toString());
                ex.printStackTrace();
                return;
            }

            //Scrivo il messaggi sul file appena creato
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            try {
                fos = new FileOutputStream(messagesFile, true);
                oos = new ObjectOutputStream(fos);
            } catch (FileNotFoundException ex) {
                //File non trovato
                Log.e(LOG_TAG, "Impossibile salvare il messaggio: file " + messagesFile.toString() + " non trovato.");
                return;
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile inizializzare lo stream di output per salvare il messaggio: " + ex.toString());
                return;
            }

            try {
                //Scrive il messaggio sul file
                oos.writeObject(message);
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile salvare il messaggio sul file " + messagesFile.toString() + ": " + ex.toString());
                return;
            }

            try {
                oos.flush();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possibile fare il flush dello stream di output: " + ex.toString());
            }

            try {
                oos.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possiblie chiudere il file: " + ex.toString());
            }
        }
        else {

            //Salva il riferimento al file trovato.
            messagesFile = files[i];

            //Salva il messaggio sul file trovato con la specializzazione
            //di ObjectOutputStream: AppendingObjectOutputStream.
            FileOutputStream fos = null;
            AppendingObjectOutputStream oos = null;
            try {
                fos = new FileOutputStream(messagesFile, true);
                oos = new AppendingObjectOutputStream(fos);
            } catch (FileNotFoundException ex) {
                //File non trovato
                Log.e(LOG_TAG, "Impossibile salvare il messaggio: file " + messagesFile.toString() + " non trovato.");
                return;
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile inizializzare lo stream di output per salvare il messaggio: " + ex.toString());
                return;
            }

            try {
                //Scrive il messaggio sul file
                oos.writeObject(message);
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile salvare il messaggio sul file " + messagesFile.toString() + ": " + ex.toString());
                return;
            }

            try {
                oos.flush();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possibile fare il flush dello stream di output: " + ex.toString());
            }

            try {
                oos.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possiblie chiudere il file: " + ex.toString());
            }
        }

    }

    /**
     * Salva una lista di messaggi nella memoria interna.
     *
     * @param messageList La lista di messaggi da salvare.
     */
    public synchronized void saveMessagesList(List<Message> messageList) {
        File messagesFile = null;

        //Controlla se esiste già il file dove memorizzare i messaggi presenti
        //nella lista.
        File[] files = context.getFilesDir().listFiles();

        //Scandisce i file presenti nella directory base dell'applicazione.
        //Se trova un file, controlla se il suo nome è simile all'indirizzo MAC
        //del mittente dei messaggi da memorizzare e, se è simile, li memorizza
        //lì dentro. Se trova una directory, essa verrà ignorata. Se nessun file
        //viene ritrovato, ne verrà creato uno nuovo.

        boolean trovato = false;
        int i;
        for (i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                //Controlla se il nome del file è simile all'indirizzo MAC
                //del mittente.
                if (Utils.isMacSimilar(files[i].getName(), messageList.get(0).getSender().replace(":", ""))) {
                    trovato = true;
                    break;
                }
            }
        }

        if (!trovato) {
            Log.i(LOG_TAG, "File non trovato. Ne creo uno nuovo.");

            //Il file che memorizza i messaggi provenienti dal mittente non è stato
            //trovato. Ne crea uno nuovo.
            try {
                messagesFile = new File(context.getFilesDir(), messageList.get(0).getSender().replace(":", ""));
                messagesFile.createNewFile();
            } catch (IOException ex) {
                //Non è stato possibile creare il file
                Log.e(LOG_TAG, "Impossibile creare il file: " + ex.toString());
                ex.printStackTrace();
                return;
            }

            //Salva i messaggi sul file appena creato usando
            //l'ObjectOutputStream.
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            try {
                fos = new FileOutputStream(messagesFile, true);
                oos = new ObjectOutputStream(fos);
            } catch (FileNotFoundException ex) {
                //File non trovato
                Log.e(LOG_TAG, "Impossibile salvare il messaggio: file " + messagesFile.toString() + " non trovato.");
                return;
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile inizializzare lo stream di output per salvare il messaggio: " + ex.toString());
                return;
            }

            //Scrive i messaggi sul file
            for (Message message : messageList) {
                try {
                    oos.writeObject(message);
                } catch (IOException ex) {
                    Log.e(LOG_TAG, "Impossibile salvare il messaggio nel file " + messagesFile.toString() + ": " + ex.toString());
                }
            }

            try {
                oos.flush();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possibile fare il flush dello stream di output: " + ex.toString());
            }

            try {
                oos.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possiblie chiudere il file: " + ex.toString());
            }
        }

        else {
            //Ottieni il riferimento al file trovato.
            messagesFile = files[i];

            //Salva i messaggi nel file trovato utilizzando
            //la specializzazione di ObjectOutputStream: AppendingObjectOutputStream.
            FileOutputStream fos = null;
            AppendingObjectOutputStream oos = null;
            try {
                fos = new FileOutputStream(messagesFile, true);
                oos = new AppendingObjectOutputStream(fos);
            } catch (FileNotFoundException ex) {
                //File non trovato
                Log.e(LOG_TAG, "Impossibile salvare il messaggio: file " + messagesFile.toString() + " non trovato.");
                return;
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile inizializzare lo stream di output per salvare il messaggio: " + ex.toString());
                return;
            }

            //Scrive i messaggi sul file
            for (Message message : messageList) {
                try {
                    oos.writeObject(message);
                } catch (IOException ex) {
                    Log.e(LOG_TAG, "Impossibile salvare il messaggio nel file " + messagesFile.toString() + ": " + ex.toString());
                }
            }

            try {
                oos.flush();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possibile fare il flush dello stream di output: " + ex.toString());
            }

            try {
                oos.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Non è stato possiblie chiudere il file: " + ex.toString());
            }

        }

    }

    /**
     * Restituisce la lista dei messaggi ricevuti dal (e inviati al)
     * dispositivo indicato dal suo indirizzo MAC.
     *
     * @param device L'indirizzo MAC del dispositivo di cui si vuole ottenere
     *               la cronologia dei messaggi ricevuti/inviati (compresi anche i
     *               messaggi ricevuti ma non ancora letti).
     * @return Un'istanza della classe List contenente tutti i messaggi ricevuti/inviati
     * al dispositivo.
     */
    public synchronized List<Message> loadMessagesList(String device) {
        File messagesFile = null;
        List<Message> messagesList = new LinkedList<>();

        //Scandisce i file presenti nella directory base dell'applicazione.
        //Se trova un file, controlla se il suo nome è simile all'indirizzo MAC
        //del contatto di cui caricare la cronologia dei messaggi e, se è simile,
        //memorizza i messaggi presenti in una lista che restituirà al metodo chiamate.
        //Se trova una directory, essa verrà ignorata. Se nessun file viene ritrovato,
        //verrà restituita una lista di messaggi vuota.
        File[] files = context.getFilesDir().listFiles();

        boolean trovato = false;
        int i;
        for (i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                //Controlla se il nome del file è simile all'indirizzo MAC
                //del contatto.
                if (Utils.isMacSimilar(files[i].getName(), device.replace(":", ""))) {
                    trovato = true;
                    break;
                }
            }
        }

        if (!trovato) {
            Log.i(LOG_TAG, "File "+ device.replace(":", "") + " non trovato.");

            //Il file non è stato trovato. Restituisce una lista vuota.
            return messagesList;

        }
        else {
            //Ottieni il riferimento al file trovato.
            messagesFile = files[i];
            Log.i(LOG_TAG, "File cronologia trovato: " + messagesFile.getAbsolutePath());
        }

        //Avvia la lettura dei messaggi presenti nel file
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(messagesFile);
        } catch (FileNotFoundException ex) {
            Log.e(LOG_TAG, "Impossibile recuperare i messaggi salvati: file " + messagesFile.toString() + " non trovato.");
            return messagesList;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(fis);
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Impossibile inizializzare lo stream di input per leggere i messaggi memorizzati: " + ex.toString());
            return messagesList;
        }

        //Aggiunge i messaggi letti dal file alla lista dei messaggi.
        try {
            for (Message message = (Message) ois.readObject(); ; message = (Message) ois.readObject()) {
                messagesList.add(message);
            }
        } catch (OptionalDataException ex) {
            Log.e(LOG_TAG, "Impossibile caricare tutti i messaggi dalla memoria interna: " + ex.toString());
        } catch (ClassNotFoundException ex) {
            Log.e(LOG_TAG, "Impossibile caricare tutti i messaggi dalla memoria interna: " + ex.toString());
        } catch (EOFException ex) {
            //Si è raggiunto la fine del file. Nulla di male.
            Log.i(LOG_TAG, "Raggiunta la fine del file. Ora resituisco la lista dei messaggi.");
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Impossibile caricare tutti i messaggi dalla memoria interna: " + ex.toString());
        }

        return messagesList;
    }

    /**
     * Questo metodo permette di cancellare un file contenente la
     * cronologia dei messaggi ricevuti (e inviati) da un contatto
     * il cui indirizzo MAC è fornito in input.
     *
     * @param device L'indirizzo MAC del contatto di cui si vuole cancellare la cronologia dei messaggi.
     */
    public synchronized void deleteMessages(String device) {

        //Scandisce i file presenti nella directory base dell'applicazione.
        //Se trova un file, controlla se il suo nome è simile all'indirizzo MAC
        //del contatto di cui cancellare la cronologia dei messaggi e, se è simile,
        //lo cancella. Se trova una directory, essa verrà ignorata. Se nessun file viene
        //ritrovato, il metodo termina.
        File[] files = context.getFilesDir().listFiles();

        boolean trovato = false;
        int i;
        for (i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                //Controlla se il nome del file è simile all'indirizzo MAC
                //del contatto.
                if (Utils.isMacSimilar(files[i].getName(), device.replace(":", ""))) {
                    trovato = true;
                    break;
                }
            }
        }

        if (!trovato) {
            Log.i(LOG_TAG, "File "+ device.replace(":", "") + " non trovato.");

            //Il file non è stato trovato.

        }
        else {
            //Cancella il file.
            if (files[i].delete())
                Log.i(LOG_TAG, "Cronologia di " + device.replace(":", "") + " cancellata.");
            else
                Log.i(LOG_TAG, "Non è stato possibile cancellare la cronologia di " + device.replace(":", "") + ".");
        }
    }

    /**
     * Restituisce il numero di messaggi presenti nel file di cronologia della
     * conversazione del contatto il cui indirizzo MAC è dato in input.
     *
     * @param device L'indirizzo MAC del dispositivo di cui si vuole ottenere il numero di messaggi della conversazione.
     * @return Il numero di messaggi presenti nel file di cronologia di conversazione (conterrà anche i messaggi ricevuti e non ancora letti).
     */
    public synchronized int getMessagesCount(String device) {
        int numMessaggi = 0;
        File messagesFile = null;

        //Scandisce i file presenti nella directory base dell'applicazione.
        //Se trova un file, controlla se il suo nome è simile all'indirizzo MAC
        //del contatto di cui contare i messaggi della cronologia e, se è simile,
        //li conta e restituisce il numero dei messaggi trovati. Se trova una directory,
        //essa verrà ignorata. Se nessun file viene ritrovato, il metodo termina.
        File[] files = context.getFilesDir().listFiles();

        boolean trovato = false;
        int i;
        for (i = 0; i < files.length; i++) {

            if (files[i].isFile()) {
                //Controlla se il nome del file è simile all'indirizzo MAC
                //del contatto.
                if (Utils.isMacSimilar(files[i].getName(), device.replace(":", ""))) {
                    trovato = true;
                    break;
                }
            }
        }

        if (!trovato) {
            Log.i(LOG_TAG, "File "+ device.replace(":", "") + " non trovato.");

            //Il file non è stato trovato.
            return numMessaggi;
        }
        else {
            messagesFile = files[i];

            //Conta quanti messaggi sono presenti nel file.
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(messagesFile);
            } catch (FileNotFoundException ex) {
                Log.e(LOG_TAG, "Impossibile recuperare i messaggi salvati: file " + messagesFile.toString() + " non trovato.");
                return numMessaggi;
            }

            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(fis);
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Impossibile inizializzare lo stream di input per leggere i messaggi memorizzati: " + ex.toString());
                return numMessaggi;
            }

            while(true) {
                try {
                    ois.readObject();
                    numMessaggi++;
                }
                catch (OptionalDataException | ClassNotFoundException ex) {
                    Log.i(LOG_TAG, "Errore durante la conta dei messaggi nel file: " + ex.toString());
                }
                catch (IOException ex) {
                    Log.i(LOG_TAG, "Raggiunta la fine del file per la conta.");
                    break;
                }
            }
            return numMessaggi;
        }
    }

    /**
     * Restituisce l'istanza di MessageStore da utilizzare per salvare
     * e caricare i messaggi.
     *
     * @return L'unica istanza di MessageStore.
     * @throws MessagesStoreNotInitializedException Eccezione non controllata lanciata quando
     *                                              si cerca di ottenere l'istanza di MessageStore senza prima averla inizializzata
     *                                              con MessagesStore.initialize(Context).
     */
    public static MessagesStore getInstance() throws MessagesStoreNotInitializedException {
        if (instance != null)
            return instance;
        throw new MessagesStoreNotInitializedException();
    }

    /**
     * Questa classe interna verrà istanziata  e utilizzata per
     * aggiungere, ad un file contenente dei messaggi, i nuovi messaggi
     * arrivati. Questa classe è necessaria poiché la classe ObjectOutputStream
     * crea un nuovo header nel file ogni volta che si scrive su di esso dopo la sua apertura,
     * portando così al lancio dell'eccezione "java.io.StreamCorruptedException: Wrong format: ac"
     * durante la sua successiva lettura con l'ObjectInputStream.readObject(). Per ovviare a
     * questo inconveniente, è necessario quindi realizzare una specializzazione di
     * ObjectOutputStream che non crei un nuovo header dopo l'apertura di un file
     * già esistente.
     *
     * Fonte: http://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream
     */
    private class AppendingObjectOutputStream extends ObjectOutputStream {

        public AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            reset();
        }
    }
}
