package com.sms1516.porcelli.daniele.bootservicetest;

/**
 * Questa interfaccia conterrà le costanti chiave utilizzate
 * per scambiare dati tra le varie componenti dell'applicazione (mediante Intent e Bundle).
 *
 * @author Daniele Porcelli.
 */
public interface CostantKeys {

    //Chiave costante utilizzata per l'intent che invierà all'activity di visualizzazione dei contatti il contatto appena rilevato
    String ACTION_SEND_CONTACT = "com.sms1516.porcelli.daniele.wichat.action.SEND_CONTACT";

    //Chiave costante uilizzata per recuperare dall'intent il contatto appena rilevato
    String ACTION_SEND_CONTACT_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.SEND_CONTACT_EXTRA";

    //Chiave costante utilizzata per l'intent che ha il compito di notificare l'arrivo di un nuovo messaggio all'activity dei contatti
    String ACTION_SEND_MESSAGE_FOR_CONTACTS = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE_FOR_CONTACTS";

    //Chiave costante utlizzata per recuperare dall'intent il messaggio appena ricevuto
    String ACTION_SEND_DISCONNECT_REQUEST = "com.sms1516.porcelli.daniele.wichat.action.SEND_DISCONNECT_REQUEST";

    //Chiave costante utlizzata dall'intent ACTION_SEND_DISCONNECT_REQUEST per inserire e recuperare l'indirizzo MAC del dispositivo con il quale bisogna disconnettersi
    String ACTION_SEND_DISCONNECT_REQUEST_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.SEND_DISCONNECT_REQUEST_EXTRA";

    //Chiave costante utlizzata per l'intent che ha il compito di notificare la disconnessione di un contatto all'activity dei contatti
    String ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_DISCONNECTED_FOR_CONTACTS";

    //Chiave costante utilizzata per recuperare le informazioni del contatto che si è appena disconnesso
    String ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA";

    //Chiave costante utilizzata per l'intent che ha il compito di notificare, all'activity/fragment della conversazione, la disconnessione del contatto con cui si sta conversando
    String ACTION_CONTACT_DISCONNECTED = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_DISCONNECTED";

    //Chiave costante utilizzata per l'intent che informa l'activity dei contatti che la disconnessione richiesta è avvenuta con successo.
    String ACTION_DISCONNECT_SUCCESSFUL = "com.sms1516.porcelli.daniele.wichat.action.DISCONNECT_SUCCESSFUL";

    //Chiave costante utilizzata per l'intent che invierà il messaggio appena ricevuto all'activity della conversazione
    String ACTION_SEND_MESSAGE = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE";

    //Chiave costante utilizzata per recuperare il messaggio dall'intent da visualizzare nella conversazione
    String ACTION_SEND_MESSAGE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE_EXTRA";

    //Chiave costante utilizzata per l'intent che indica la riuscita connessione con il dispositivo
    String ACTION_CONNECTED_TO_DEVICE = "com.sms1516.porcelli.daniele.wichat.action.CONNECTED_TO_DEVICE";

    //Chiave costante utilizzata per inserire e recuperare l'indirizzo MAC dall'intent ACTION_CONNECTED_TO_DEVICE
    String ACTION_CONNECTED_TO_DEVICE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.CONNECTED_TO_DEVICE_EXTRA";

    //Chiave costante utilizzata per l'intent che indica la non reperibilità del dispositivo remoto
    String ACTION_CONTACT_NOT_AVAILABLE = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_NOT_AVAILABLE";

    //Chiave costante utilizzata per inserire e recuperare l'indirizzo MAC dall'intent ACTION_CONTACT_NOT_AVAILABLE
    String ACTION_CONTACT_NOT_AVAILABLE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_NOT_AVAILABLE_EXTRA";

    //Chiave costatnte utilizzata per l'intent che indica la riuscita connessione delle socket
    String ACTION_CONNECTION_RECEIVED = "com.sms1516.porcelli.daniele.wichat.action.CONNECTION_RECEIVED";

    //Chiave costante utilizzata dall'intent per recuperare e inserire l'indirizzo MAC del dispositivo con cui si è riuscito a stabilire la connessione con le socket
    String ACTION_CONNECTION_RECEIVED_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.CONNECTION_RECEIVED_EXTRA";

    //Chiave costante utilizzata nell'intent che avvia l'activity di conversazione per inserire e recuperare il nome del contatto con cui si vuole comunicare
    String ACTION_START_CONVERSATION_ACTIVITY_EXTRA_NAME = "com.sms1516.porcelli.daniele.wichat.action.START_CONVERSATION_ACTIVITY_EXTRA_NAME";

    //Chiave costante utilizzata nell'intent che avvia l'activity di conversazione per inserire e recuperare l'indirizzo MAC del contatto con cui si vuole comunicare
    String ACTION_START_CONVERSATION_ACTIVITY_EXTRA_MAC = "com.sms1516.porcelli.daniele.wichat.action.START_CONVERSATION_ACTIVITY_EXTRA_MAC";

    //Chiave costante utilizzata da WiChatService per notificare all'activity di conversazione se il contatto è ancora attivo
    String ACTION_CONTACT_AVAILABILITY = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_AVAILABILITY";

    //Chiave costante utilizzata per inserire e recuperare lo stato della disponibilità del contatto con cui si sta comunicando
    String ACTION_CONTACT_AVAILABILITY_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.START_CONVERSATION_ACTIVITY_EXTRA";

    //Chiave costante utilizzata per l'intent che notifica se e quale contatto è già connesso con questo dispositivo
    String ACTION_CONTACT_CONNECTED = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_CONNECTED";

    //Chiave costante utilizzata dall'intent di notifica ACTION_CONTACT_CONNECTED per inserire e recuperare l'indirizzo MAC del dispositivo già connesso
    String ACTION_CONTACT_CONNECTED_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_CONNECTED_EXTRA";

    //Chiave costante utilizzata per indicare l'intent di rifiuto della connessione da parte del contatto.
    String ACTION_CONNECTION_REFUSED = "com.sms1516.porcelli.daniele.wichat.action.CONNECTION_REFUSED";

    //Chiave utilizzata per identificare le Shared Preferences dove vengono registrati i numeri di messaggi non letti per ogni contatto rilevato.
    String RECEIVED_MESSAGES_PREFS = "received_messages_prefs";

}
