package io.github.grootscorer.tejomania.interfaces;

import io.github.grootscorer.tejomania.estado.EstadoPartida;

public interface ControladorJuegoRed {
    void onGol(int direccion); // 1 = izquierda anotó, -1 = derecha anotó
    void onMoverMazo(int numeroJugador, float velocidadX, float velocidadY);
    void onIniciarJuego();
    void onActualizarPosicionDisco(float x, float y, float velX, float velY);
    void onActualizarPosicionMazo(int numeroJugador, float x, float y);
    void onActualizarPuntaje(int puntaje1, int puntaje2);
    void onFinalizarJuego(int ganador);
    void onActualizarTiempo(float tiempoRestante);

    default EstadoPartida getEstadoPartida() {
        return null;
    }
}
