package io.github.grootscorer.tejomania.redes;

import io.github.grootscorer.tejomania.interfaces.ControladorJuegoRed;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class HiloServidor extends Thread {
    private DatagramSocket socket;
    private int puertoServidor = 5555;
    private boolean finalizado = false;
    private final int MAX_CLIENTES = 2;
    private int clientesConectados = 0;
    private ArrayList<ClienteRed> clientes = new ArrayList<ClienteRed>();
    private ControladorJuegoRed controladorJuego;

    public HiloServidor(ControladorJuegoRed controladorJuego) {
        this.controladorJuego = controladorJuego;
        try {
            socket = new DatagramSocket(puertoServidor);
        } catch (SocketException e) {
            System.err.println("Error al crear socket del servidor: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        do {
            DatagramPacket paquete = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(paquete);
                procesarMensaje(paquete);
            } catch (IOException e) {
                if (!finalizado) {
                    System.err.println("Error al recibir paquete: " + e.getMessage());
                }
            }
        } while (!finalizado);
    }

    private void procesarMensaje(DatagramPacket paquete) {
        String mensaje = (new String(paquete.getData())).trim();
        String[] partes = mensaje.split(":");
        int indice = encontrarIndiceCliente(paquete);

        System.out.println("Mensaje recibido: " + mensaje);

        if (partes[0].equals("Conectar")) {
            if (indice != -1) {
                System.out.println("Cliente ya conectado");
                enviarMensaje("YaConectado", paquete.getAddress(), paquete.getPort());
                return;
            }

            if (clientesConectados < MAX_CLIENTES) {
                clientesConectados++;
                ClienteRed nuevoCliente = new ClienteRed(clientesConectados,
                    paquete.getAddress(), paquete.getPort());
                clientes.add(nuevoCliente);
                enviarMensaje("Conectado:" + clientesConectados,
                    paquete.getAddress(), paquete.getPort());

                if (clientesConectados == MAX_CLIENTES) {
                    for (ClienteRed cliente : clientes) {
                        enviarMensaje("Iniciar", cliente.getIp(), cliente.getPuerto());
                    }
                    controladorJuego.onIniciarJuego();
                }
            } else {
                enviarMensaje("Lleno", paquete.getAddress(), paquete.getPort());
            }
        } else if (indice == -1) {
            System.out.println("Cliente no conectado");
            enviarMensaje("NoConectado", paquete.getAddress(), paquete.getPort());
            return;
        } else {
            ClienteRed cliente = clientes.get(indice);
            procesarComando(partes, cliente);
        }
    }

    private void procesarComando(String[] partes, ClienteRed cliente) {
        if (partes[0].equals("MoverMazo")) {
            float velX = Float.parseFloat(partes[1]);
            float velY = Float.parseFloat(partes[2]);
            controladorJuego.onMoverMazo(cliente.getNumeroJugador(), velX, velY);
        }
    }

    private int encontrarIndiceCliente(DatagramPacket paquete) {
        int i = 0;
        int indiceCliente = -1;
        while (i < clientes.size() && indiceCliente == -1) {
            ClienteRed cliente = clientes.get(i);
            String id = paquete.getAddress().toString() + ":" + paquete.getPort();
            if (id.equals(cliente.getId())) {
                indiceCliente = i;
            }
            i++;
        }
        return indiceCliente;
    }

    public void enviarMensaje(String mensaje, InetAddress ipCliente, int puertoCliente) {
        byte[] mensajeBytes = mensaje.getBytes();
        DatagramPacket paquete = new DatagramPacket(mensajeBytes,
            mensajeBytes.length, ipCliente, puertoCliente);
        try {
            socket.send(paquete);
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }

    public void enviarMensajeATodos(String mensaje) {
        for (ClienteRed cliente : clientes) {
            enviarMensaje(mensaje, cliente.getIp(), cliente.getPuerto());
        }
    }

    public void desconectarClientes() {
        for (ClienteRed cliente : clientes) {
            enviarMensaje("Desconectar", cliente.getIp(), cliente.getPuerto());
        }
        this.clientes.clear();
        this.clientesConectados = 0;
    }

    public void terminar() {
        finalizado = true;

        try {
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
