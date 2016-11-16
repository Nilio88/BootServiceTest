package com.sms1516.porcelli.daniele.bootservicetest;

import java.lang.RuntimeException;
/**
 * Questa classe rappresenta l'eccezione lanciata
 * dal MessagesStore quando si cerca di ottenerne l'istanza
 * senza averlo prima inizializzato.
 *
 * @author Daniele Porcelli
 */
public class MessagesStoreNotInitializedException extends RuntimeException {
}
