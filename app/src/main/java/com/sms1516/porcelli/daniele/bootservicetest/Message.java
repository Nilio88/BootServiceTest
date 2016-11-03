package com.sms1516.porcelli.daniele.bootservicetest;

import java.io.Serializable;

/**
 * Questa classe rappresentà l'entità messaggio.
 * Un messaggio è definito da un mittente e da
 * un testo.
 *
 * @author Daniele Porcelli
 */
public class Message implements Serializable {

    private String sender;
    private String text;

    /**
     * Costruttore di Message
     * @param sender Il mittente del messaggio.
     * @param text Il testo che il mittente vuole mandare al suo interlocutore.
     */
    public Message(String sender, String text) {
        this.sender = sender;
        this.text = text;
    }

    /**
     * Metodo accessore che restituisce il mittente del
     * messaggio.
     *
     * @return Il mittente del messaggio.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Metodo accessore che restituisce il testo inviato
     * dal mittente.
     *
     * @return Il messaggio inviato dal mittente.
     */
    public String getText() {
        return text;
    }
}