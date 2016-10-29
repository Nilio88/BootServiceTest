package com.sms1516.porcelli.daniele.bootservicetest;

import android.net.wifi.p2p.WifiP2pDevice;

import java.io.Serializable;

/**
 * Questa interfaccia verrà usata dal servizio WiChatService
 * per astrarre l'attività che si registrerà come listener dei
 * dispositivi trovati tramite il network service discovery
 * e dei messaggi ricevuti.
 *
 * @author Daniele Porcelli
 */
public interface ContactsListener extends Serializable {

    /**
     * Metodo di callback invocato quando la lista dei contatti
     * è cambiata.
     *
     * @param contact Il dispositivo trovato nell'area del Wi-Fi.
     */
    public void onContactFound(WifiP2pDevice contact);

    /**
     * Metodo di callback invocato per segnalare all'activity
     * che mantiene la lista dei contatti trovati che un dispositivo
     * si è appena disconnesso (probabilmente è andato fuori dal campo del
     * sengale Wi-Fi).
     *
     * @param device L'indirizzo MAC del dispositivo appena scollegato.
     */
    public void onContactDisconnected(String device);
}