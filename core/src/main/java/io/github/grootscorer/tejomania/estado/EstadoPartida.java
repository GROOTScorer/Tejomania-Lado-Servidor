package io.github.grootscorer.tejomania.estado;

public class EstadoPartida {
    private String jugador1 = "Jugador 1";
    private String jugador2 = "Jugador 2";
    private int puntaje1;
    private int puntaje2;
    private float tiempoRestante = 300;
    private boolean jugandoPorTiempo = true;
    private boolean jugandoPorPuntaje = false;
    private boolean jugarConObstaculos = false;
    private boolean jugarConTirosEspeciales = false;
    private boolean jugarConModificadores = false;
    private String canchaSeleccionada = "Cancha estandar";

    public String getJugador1() {
        return this.jugador1;
    }

    public String getJugador2() {
        return this.jugador2;
    }

    public int getPuntaje1() {
        return this.puntaje1;
    }

    public void setPuntaje1(int puntaje1) {
        this.puntaje1 = puntaje1;
    }

    public int getPuntaje2() {
        return this.puntaje2;
    }

    public void setPuntaje2(int puntaje2) {
        this.puntaje2 = puntaje2;
    }

    public void agregarGolJugador1() {
        this.puntaje1++;
    }

    public void agregarGolJugador2() {
        this.puntaje2++;
    }

    public float getTiempoRestante() {
        return this.tiempoRestante;
    }

    public void actualizarTiempo(float delta) {
        if (jugandoPorTiempo && tiempoRestante > 0) {
            tiempoRestante -= delta;
            if (tiempoRestante < 0) tiempoRestante = 0;
        }
    }

    public boolean isJugandoPorTiempo() {
        return this.jugandoPorTiempo;
    }

    public String getCanchaSeleccionada() {
        return this.canchaSeleccionada;
    }
}
