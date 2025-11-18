package io.github.grootscorer.tejomania.redes;

import io.github.grootscorer.tejomania.estado.EstadoPartida;
import io.github.grootscorer.tejomania.interfaces.ControladorJuegoRed;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/*
 *  - Atiende conexiones UDP de clientes.
 *  - Maneja hasta MAX_CLIENTES jugadores.
 *  - Procesa mensajes recibidos y envía respuestas.
 *  - Notifica al controlador del juego los eventos importantes.
 */
public class HiloServidor extends Thread {

    // Socket UDP que usará el servidor para recibir y enviar paquetes
    private DatagramSocket socket;

    // Puerto en el que escucha el servidor
    private int puertoServidor = 5555;

    // Controla si el hilo debe terminar
    private boolean finalizado = false;

    // Máximo número de clientes permitidos en la partida
    private final int MAX_CLIENTES = 2;

    // Cantidad actual de clientes conectados
    private int clientesConectados = 0;

    // Lista con los clientes conectados
    private ArrayList<ClienteRed> clientes = new ArrayList<>();

    // Referencia al controlador del juego (para notificar acciones)
    private ControladorJuegoRed controladorJuego;public HiloServidor(ControladorJuegoRed controladorJuego) {
        this.controladorJuego = controladorJuego;
        try {
            // Se crea el socket escuchando en el puerto definido
            socket = new DatagramSocket(puertoServidor);
        } catch (SocketException e) {
            System.err.println("Error al crear socket del servidor: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Bucle principal del hilo: escucha paquetes hasta que finalizado sea true
        do {
            // Buffer para recibir datos de hasta 1024 bytes
            DatagramPacket paquete = new DatagramPacket(new byte[1024], 1024);
            try {
                // Espera bloqueante: se queda esperando un paquete UDP
                socket.receive(paquete);
                // Se procesa el mensaje recibido
                procesarMensaje(paquete);
            } catch (IOException e) {
                if (!finalizado) {
                    System.err.println("Error al recibir paquete: " + e.getMessage());
                }
            }
        } while (!finalizado);
    }

    // Procesa un paquete recibido desde un cliente.
    private void procesarMensaje(DatagramPacket paquete) {
        // Convierte los bytes en un String y elimina espacios extra
        String mensaje = (new String(paquete.getData())).trim();

        // Divide el mensaje por ":" para interpretar comandos
        String[] partes = mensaje.split(":");

        // Busca si el cliente ya está registrado
        int indice = encontrarIndiceCliente(paquete);

        System.out.println("Mensaje recibido: " + mensaje);

        // ---- MANEJO DE CONEXIÓN ----
        if (partes[0].equals("Conectar")) {

            // El cliente ya está conectado
            if (indice != -1) {
                System.out.println("Cliente ya conectado");
                enviarMensaje("YaConectado", paquete.getAddress(), paquete.getPort());
                return;
            }

            // Aún hay espacio para más clientes
            if (clientesConectados < MAX_CLIENTES) {
                clientesConectados++;

                // Crea nuevo cliente con número de jugador (1 o 2)
                ClienteRed nuevoCliente = new ClienteRed(
                    clientesConectados,
                    paquete.getAddress(),
                    paquete.getPort()
                );

                clientes.add(nuevoCliente);

                // Envía número de jugador Y configuración del juego
                enviarConfiguracionInicial(nuevoCliente);

                // Si ya están los dos clientes conectados, inicia partida
                if (clientesConectados == MAX_CLIENTES) {

                    // Avisar a todos los clientes que el juego inicia
                    for (ClienteRed cliente : clientes) {
                        enviarMensaje("Iniciar", cliente.getIp(), cliente.getPuerto());
                    }

                    // Notificar al controlador que debe iniciar el juego
                    controladorJuego.onIniciarJuego();
                }

            } else {
                // El servidor está lleno
                enviarMensaje("Lleno", paquete.getAddress(), paquete.getPort());
            }

        }
        // Si el cliente no está conectado y no es petición de conexión
        else if (indice == -1) {
            System.out.println("Cliente no conectado");
            enviarMensaje("NoConectado", paquete.getAddress(), paquete.getPort());
            return;
        }
        // Comandos de un cliente ya registradp
        else {
            ClienteRed cliente = clientes.get(indice);
            procesarComando(partes, cliente);
        }
    }

    // Interpreta acciones del juego enviadas por un cliente.
    private void procesarComando(String[] partes, ClienteRed cliente) {
        // Comando para mover el mazo del jugador
        if (partes[0].equals("MoverMazo")) {
            // Se parsean las velocidades enviadas
            float velX = Float.parseFloat(partes[1]);
            float velY = Float.parseFloat(partes[2]);

            // Se notifica al controlador del juego
            controladorJuego.onMoverMazo(cliente.getNumeroJugador(), velX, velY);
        }
    }

    // Busca un cliente en la lista por su IP y puerto.
    private int encontrarIndiceCliente(DatagramPacket paquete) {
        int i = 0;
        int indiceCliente = -1;
        while (i < clientes.size() && indiceCliente == -1) {
            ClienteRed cliente = clientes.get(i);

            // Crea un identificador basado en IP:PUERTO
            String id = paquete.getAddress().toString() + ":" + paquete.getPort();

            // Se compara con el ID del cliente almacenado
            if (id.equals(cliente.getId())) {
                indiceCliente = i;
            }
            i++;
        }
        return indiceCliente;
    }

    private void enviarConfiguracionInicial(ClienteRed cliente) {
        // Formato: "Conectado:numJugador:tiempoRestante:jugandoPorTiempo:jugandoPorPuntaje:puntajeGanador:obstaculos:tirosEspeciales:modificadores:cancha"

        EstadoPartida estado = controladorJuego.getEstadoPartida();

        String mensaje = "Conectado:" +
            cliente.getNumeroJugador() + ":" +
            estado.getTiempoRestante() + ":" +
            estado.isJugandoPorTiempo() + ":" +
            estado.isJugandoPorPuntaje() + ":" +
            estado.getPuntajeGanador() + ":" +
            estado.isJugarConObstaculos() + ":" +
            estado.isJugarConTirosEspeciales() + ":" +
            estado.isJugarConModificadores() + ":" +
            estado.getCanchaSeleccionada();

        enviarMensaje(mensaje, cliente.getIp(), cliente.getPuerto());
    }

    // Envía un mensaje UDP a un cliente específico.
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

    // Envía un mensaje a todos los clientes conectados.
    public void enviarMensajeATodos(String mensaje) {
        for (ClienteRed cliente : clientes) {
            enviarMensaje(mensaje, cliente.getIp(), cliente.getPuerto());
        }
    }

    // Ordena la desconexión de todos los clientes.
    public void desconectarClientes() {
        for (ClienteRed cliente : clientes) {
            enviarMensaje("Desconectar", cliente.getIp(), cliente.getPuerto());
        }
        this.clientes.clear();
        this.clientesConectados = 0;
    }

    // Termina el hilo y cierra el socket.
    public void terminar() {
        finalizado = true;

        try {
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
