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
    String ACTION_SEND_MESSAGE_FOR_CONTACTS_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE_FOR_CONTACTS_EXTRA";

    //Chiave costante utlizzata per l'intent che ha il compito di notificare la disconnessione di un contatto all'activity dei contatti
    String ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_DISCONNECTED_FOR_CONTACTS";

    //Chiave costante utilizzata per recuperare le informazioni del contatto che si è appena disconnesso
    String ACTION_CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_DISCONNECTED_FOR_CONTACTS_EXTRA";

    //Chiave costante utilizzata per l'intent che ha il compito di notificare la disconnessione del contatto con cui si sta conversando
    String ACTION_CONTACT_DISCONNECTED = "com.sms1516.porcelli.daniele.wichat.action.CONTACT_DISCONNECTED";

    //Chiave costante utilizzata per l'intent che invierà il messaggio appena ricevuto all'activity della conversazione
    String ACTION_SEND_MESSAGE = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE";

    //Chiave costante utilizzata per recuperare il messaggio dall'intent da visualizzare nella conversazione
    String ACTION_SEND_MESSAGE_EXTRA = "com.sms1516.porcelli.daniele.wichat.action.SEND_MESSAGE_EXTRA";



}
