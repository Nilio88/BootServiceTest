package com.sms1516.porcelli.daniele.bootservicetest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Questa classe conterrà dei metodi statici che si
 * riveleranno utili per l'app.
 */
public class Utils {

    private static final String LOG_TAG = Utils.class.getName();
    /**
     * Recupera dal file /proc/net/arp l'indirizzo MAC del dispositivo avente
     * come indirizzo IP quello passato in input.
     *
     * Fonte: http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
     *
     * @param ip
     * @return
     */
    public static String getMac(String ip) {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = null;
            Log.i(LOG_TAG, "********************");
            while ((line = br.readLine()) != null) {
                Log.i(LOG_TAG, line);
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
            Log.i(LOG_TAG, "********************");
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

    /**
     * Questo metodo ha il compito di confrontare due indirizzi MAC in formato stringa
     * e predire se essi sono simili fra loro.
     *
     * @param firstMac Il primo indirizzo MAC da confrontare con il secondo.
     * @param secondMac L'indirizzo MAC da confrontare con il primo.
     * @return true se i due indirizzi sono simili (hanno un numero limitato di caratteri differenti tra loro); false, altrimenti.
     */
    public static boolean isMacSimilar(String firstMac, String secondMac) {
        final int NUMERO_MASSIMO_CARATTERI_DIFFERENTI = 3;  //Possiamo anche cambiarlo se l'app finale fa troppi errori.

        Log.i(LOG_TAG, "Primo indirizzo MAC: " + firstMac);
        Log.i(LOG_TAG, "Secondo indirizzo MAC: " + secondMac);

        //Converto le stringhe in array di caratteri per poterli scandire
        char[] primoMac = firstMac.toCharArray();
        char[] secondoMac = secondMac.toCharArray();

        //Variabile che conterà il numero di caratteri differenti
        int caratteriDifferenti = 0;

        //Inizia il confronto
        for (int i = 0; i < primoMac.length; i++) {
            if (primoMac[i] != secondoMac[i])
                caratteriDifferenti++;
        }

        //Se il numero di caratteri differenti è maggiore del limite massimo, allora
        //i due indirizzi MAC corrispondono (probabilmente) a due diversi dispositivi.
        if (caratteriDifferenti <= NUMERO_MASSIMO_CARATTERI_DIFFERENTI)
            return true;
        return false;
    }

    /**
     * Questo metodo calcola e restituisce la similarità tra due indirizzi MAC.
     * La similarità è misurata dal numero di caratteri differenti tra gli indirizzi:
     * più è basso il risultato, più è alta la probabilità che si tratti dello stesso
     * dispositivo.
     *
     * @param firstMac Il primo indirizzo MAC da confrontare.
     * @param secondMac Il secondo indirizzo MAC da confrontare.
     * @return Un valore intero compreso tra 0 e 12.
     */
    public static int getSimilarity(String firstMac, String secondMac) {
        Log.i(LOG_TAG, "Primo indirizzo MAC: " + firstMac);
        Log.i(LOG_TAG, "Secondo indirizzo MAC: " + secondMac);

        //Converto le stringhe in array di caratteri per poterli scandire
        char[] primoMac = firstMac.toCharArray();
        char[] secondoMac = secondMac.toCharArray();

        //Variabile che conterà il numero di caratteri differenti
        int caratteriDifferenti = 0;

        //Inizia il confronto
        for (int i = 0; i < primoMac.length; i++) {
            if (primoMac[i] != secondoMac[i])
                caratteriDifferenti++;
        }

        return caratteriDifferenti;
    }

    /**
     * Ottiene l'indirizzo IP del dispositivo associato alla sua interfaccia
     * del Wi-Fi Direct.
     *
     * modificato da:
     *
     * http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
     *
     * @return Indirizzo IP del dispositivo associato alla sua interfaccia del Wi-Fi Direct.
     */
    public static InetAddress getLocalIPAddress() {
        //Costante che denota l'identificativo di un'interfaccia di tipo Wi-Fi Direct.
        final String p2pInt = "p2p-wlan0";

        try {
            //Scandisce tutte le interfacce di rete presenti nel dispositivo Android.
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();

                //Per ogni interfaccia di rete del dispositivo, scandisce tutti gli indirizzi IP ad esso associati.
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    String iface = intf.getName();
                    Log.i(LOG_TAG, "Interfaccia di rete analizzata: " + iface);

                    //Se l'indirizzo IP recuperato appartiene all'interfaccia Wi-Fi Direct
                    //del dispositivo, restituiscilo alla funzione chiamante.
                    if(iface.matches(".*" +p2pInt+ ".*")){
                        Log.i(LOG_TAG, "Interfaccia di rete trovata. Ora scandisco i suoi indirizzi IP.");
                        Log.i(LOG_TAG, "Indirizzo IP scandito: " + inetAddress.getHostAddress());

                        if (inetAddress instanceof Inet4Address) {
                            Log.i(LOG_TAG, "L'indirizzo IP recuperato in getLocalIPAddress() è di tipo IPv4.");
                            return inetAddress;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(LOG_TAG, "Impossibile recuperare l'indirizzo IP del dispositivo in uso: " + ex.toString());
            ex.printStackTrace();
        }
        return null;
    }
}
