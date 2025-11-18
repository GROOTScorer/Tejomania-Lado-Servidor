package io.github.grootscorer.tejomania.redes;

import java.net.InetAddress;

/*
 * ClienteRed:
 *  - Representa a un cliente conectado al servidor UDP.
 *  - Almacena su IP, puerto y número de jugador asignado.
 *  - Genera un ID único basado en "IP:PUERTO" para identificarlo.
 */
public class ClienteRed {

    // Identificador único del cliente: "IP:PUERTO"
    private String id;

    // Número de jugador asignado (1 o 2)
    private int numeroJugador;

    // Dirección IP del cliente
    private InetAddress ip;

    // Puerto desde el cual el cliente envía paquetes
    private int puerto;

    public ClienteRed(int numeroJugador, InetAddress ip, int puerto) {

        // El servidor asigna el número de jugador
        this.numeroJugador = numeroJugador;

        // Identificador único del cliente basado en su IP y puerto
        this.id = ip.toString() + ":" + puerto;

        this.ip = ip;
        this.puerto = puerto;
    }

    public String getId() {
        return id;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPuerto() {
        return puerto;
    }

    public int getNumeroJugador() {
        return numeroJugador;
    }
}
