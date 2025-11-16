package io.github.grootscorer.tejomania.redes;

import java.net.InetAddress;

public class ClienteRed {
    private String id;
    private int numeroJugador;
    private InetAddress ip;
    private int puerto;

    public ClienteRed(int numeroJugador, InetAddress ip, int puerto) {
        this.numeroJugador = numeroJugador;
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
