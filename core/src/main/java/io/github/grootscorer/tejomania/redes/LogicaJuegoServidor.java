package io.github.grootscorer.tejomania.redes;

import com.badlogic.gdx.Gdx;
import io.github.grootscorer.tejomania.entidades.Disco;
import io.github.grootscorer.tejomania.entidades.Mazo;
import io.github.grootscorer.tejomania.estado.EstadoPartida;
import io.github.grootscorer.tejomania.interfaces.ControladorJuegoRed;

import java.util.ArrayList;
import java.util.List;

public class LogicaJuegoServidor implements ControladorJuegoRed {
    private HiloServidor hiloServidor;

    private Disco disco;
    private Mazo mazo1, mazo2;

    private EstadoPartida estadoPartida;
    private boolean juegoIniciado = false;
    private boolean juegoTerminado = false;
    private boolean pausaGol = false;
    private float tiempoPausaGol = 0;
    private final float DURACION_PAUSA_GOL = 1.0f;

    private final float escalaX;
    private final float escalaY;
    private final int CANCHA_ANCHO;
    private final int CANCHA_ALTO;
    private final float xCancha;
    private final float yCancha;

    private float tiempoDesdeUltimaSincronizacion = 0;
    private final float INTERVALO_SINCRONIZACION = 0.05f; // Para menor saturación de red

    private ControladorJuegoRed callbackServidor;

    public LogicaJuegoServidor(EstadoPartida estadoPartida, float escalaX, float escalaY,
                               ControladorJuegoRed callbackServidor) {
        this.estadoPartida = estadoPartida;
        this.escalaX = escalaX;
        this.escalaY = escalaY;
        this.callbackServidor = callbackServidor;

        this.CANCHA_ANCHO = (int) (540 * escalaX);
        this.CANCHA_ALTO = (int) (320 * escalaY);
        this.xCancha = (Gdx.graphics.getWidth() - CANCHA_ANCHO) / 2f;
        this.yCancha = (Gdx.graphics.getHeight() - CANCHA_ALTO) / 2f;

        inicializarServidor();
    }

    private void inicializarServidor() {
        hiloServidor = new HiloServidor(this);
        hiloServidor.start();

        disco = new Disco();
        mazo1 = new Mazo();
        mazo2 = new Mazo();

        colocarPosicionInicial();

        System.out.println("Servidor iniciado. Esperando jugadores...");
    }

    public void actualizar(float delta) {
        if (!juegoIniciado || juegoTerminado) {
            return;
        }

        if (pausaGol) {
            tiempoPausaGol += delta;
            if (tiempoPausaGol >= DURACION_PAUSA_GOL) {
                pausaGol = false;
                tiempoPausaGol = 0;
                sincronizarEstadoConClientes(); // Sincroniza las posiciones al final de la pausa
            }
            return;
        }

        estadoPartida.actualizarTiempo(delta);

        actualizarFisica(delta);

        verificarGoles();
        verificarFinDeJuego();

                tiempoDesdeUltimaSincronizacion += delta;
                if (tiempoDesdeUltimaSincronizacion >= INTERVALO_SINCRONIZACION) {
                    sincronizarEstadoConClientes();
                    tiempoDesdeUltimaSincronizacion = 0;
                }
    }

    private void actualizarFisica(float delta) {
        mazo1.actualizarAnimacion(delta);
        mazo2.actualizarAnimacion(delta);

        procesarColisionesDisco(disco, delta);

        disco.reposicionarDisco(mazo1, xCancha, yCancha, CANCHA_ANCHO, CANCHA_ALTO);
        disco.reposicionarEntreDosMazos(mazo1, mazo2, CANCHA_ALTO);
    }

    private void procesarColisionesDisco(Disco disco, float delta) {
        if (disco.haAnotadoGol() || pausaGol) {
            return;
        }

        if (disco.colisionaConMazo(mazo1)) {
            disco.reposicionarEntreDosMazos(mazo1, mazo2, CANCHA_ALTO);
            disco.manejarColision(mazo1);
        }

        if (disco.colisionaConMazo(mazo2)) {
            disco.reposicionarEntreDosMazos(mazo1, mazo2, CANCHA_ALTO);
            disco.manejarColision(mazo2);
        }

        disco.actualizarPosicion(delta, xCancha, yCancha, CANCHA_ANCHO, CANCHA_ALTO);
    }

    private void verificarGoles() {
        List<Disco> discosQueAnotaron = new ArrayList<>();

        verificarGolDisco(disco, discosQueAnotaron);

        for (Disco disco : discosQueAnotaron) {
            disco.marcarGolAnotado();
        }

        if (disco.haAnotadoGol()) {
            reiniciarTrasGolCompleto();
        }
    }

    private void verificarGolDisco(Disco disco, List<Disco> discosQueAnotaron) {
        if (disco.haAnotadoGol()) {
            return;
        }

        float radioSemicirculo = CANCHA_ALTO / 4.5f;
        float centroSemicirculoY = yCancha + CANCHA_ALTO / 2f;
        float limiteInferiorGol = centroSemicirculoY - radioSemicirculo;
        float limiteSuperiorGol = centroSemicirculoY + radioSemicirculo;

        boolean discoEnAreaVerticalGol = (disco.getPosicionY() + disco.getRadioDisco() >= limiteInferiorGol) &&
            (disco.getPosicionY() + disco.getRadioDisco() <= limiteSuperiorGol);

        if (disco.getPosicionX() + disco.getRadioDisco() * 2 < xCancha) {
            if (discoEnAreaVerticalGol) {
                anotarGol(2);
                discosQueAnotaron.add(disco);
            }
        }
        else if (disco.getPosicionX() > xCancha + CANCHA_ANCHO) {
            if (discoEnAreaVerticalGol) {
                anotarGol(1);
                discosQueAnotaron.add(disco);
            }
        }
    }

    private void anotarGol(int jugadorQueAnota) {
        if (jugadorQueAnota == 1) {
            estadoPartida.agregarGolJugador1();
        } else {
            estadoPartida.agregarGolJugador2();
        }

        hiloServidor.enviarMensajeATodos("Gol:" + jugadorQueAnota);
        enviarPuntajes();

        // Notificar al callback del servidor
        if (callbackServidor != null) {
            callbackServidor.onGol(jugadorQueAnota);
            callbackServidor.onActualizarPuntaje(estadoPartida.getPuntaje1(), estadoPartida.getPuntaje2());
        }
    }

    private void reiniciarTrasGolCompleto() {
        disco.reiniciarEstadoGol();
        reiniciarPosicionesTrasGol();

        pausaGol = true;
        tiempoPausaGol = 0;

        sincronizarEstadoConClientes();
    }

    private void reiniciarPosicionesTrasGol() {
        disco.setPosicion(xCancha + CANCHA_ANCHO / 2f - disco.getRadioDisco(),
            yCancha + CANCHA_ALTO / 2f - disco.getRadioDisco());
        disco.setVelocidadX(0);
        disco.setVelocidadY(0);
        disco.setMaxVelocidad(500);

        float offsetMazos = 50 * Math.min(escalaX, escalaY);

        mazo1.setPosicion((int)(xCancha + offsetMazos - mazo1.getRadioMazo()),
            (int)(yCancha + CANCHA_ALTO / 2f - mazo1.getRadioMazo()));
        mazo1.setVelocidadX(0);
        mazo1.setVelocidadY(0);

        mazo2.setPosicion((int)(xCancha + CANCHA_ANCHO - offsetMazos - mazo2.getRadioMazo()),
            (int)(yCancha + CANCHA_ALTO / 2f - mazo2.getRadioMazo()));
        mazo2.setVelocidadX(0);
        mazo2.setVelocidadY(0);
    }

    private void colocarPosicionInicial() {
        disco.setPosicion(xCancha + CANCHA_ANCHO / 2f - disco.getRadioDisco(),
            yCancha + CANCHA_ALTO / 2f - disco.getRadioDisco());
        disco.setVelocidadX(0);
        disco.setVelocidadY(0);

        float offsetMazos = 50 * Math.min(escalaX, escalaY);

        mazo1.setPosicion((int)(xCancha + offsetMazos - mazo1.getRadioMazo()),
            (int)(yCancha + CANCHA_ALTO / 2f - mazo1.getRadioMazo()));
        mazo1.setVelocidadX(0);
        mazo1.setVelocidadY(0);

        mazo2.setPosicion((int)(xCancha + CANCHA_ANCHO - offsetMazos - mazo2.getRadioMazo()),
            (int)(yCancha + CANCHA_ALTO / 2f - mazo2.getRadioMazo()));
        mazo2.setVelocidadX(0);
        mazo2.setVelocidadY(0);
    }

    private void verificarFinDeJuego() {
        boolean finPorTiempo = estadoPartida.isJugandoPorTiempo() &&
            estadoPartida.getTiempoRestante() <= 0;

        if (finPorTiempo) {
            finalizarJuego();
        }
    }

    private void finalizarJuego() {
        if (juegoTerminado) {
            return;
        }

        juegoTerminado = true;

        int ganador;
        if (estadoPartida.getPuntaje1() > estadoPartida.getPuntaje2()) {
            ganador = 1;
        } else if (estadoPartida.getPuntaje2() > estadoPartida.getPuntaje1()) {
            ganador = 2;
        } else {
            ganador = 0;
        }

        hiloServidor.enviarMensajeATodos("FinalizarJuego:" + ganador);

        // Notificar al callback del servidor
        if (callbackServidor != null) {
            callbackServidor.onFinalizarJuego(ganador);
        }

        System.out.println("Juego finalizado. Ganador: " +
            (ganador == 0 ? "Empate" : "Jugador " + ganador));
    }

    private void sincronizarEstadoConClientes() {
        enviarActualizacionDisco();
        sincronizarPosiciones();
        enviarTiempoRestante();

        if (callbackServidor != null) {
            callbackServidor.onActualizarPosicionDisco(
                disco.getPosicionX(),
                disco.getPosicionY(),
                disco.getVelocidadX(),
                disco.getVelocidadY()
            );
            callbackServidor.onActualizarPosicionMazo(1, mazo1.getPosicionX(), mazo1.getPosicionY());
            callbackServidor.onActualizarPosicionMazo(2, mazo2.getPosicionX(), mazo2.getPosicionY());
        }
    }

    private void enviarTiempoRestante() {
        String mensaje = "ActualizarTiempo:" + estadoPartida.getTiempoRestante();
        hiloServidor.enviarMensajeATodos(mensaje);
    }

    private void enviarActualizacionDisco() {
        String mensaje = "ActualizarPosicion:Disco:" +
            disco.getPosicionX() + ":" +
            disco.getPosicionY() + ":" +
            disco.getVelocidadX() + ":" +
            disco.getVelocidadY();
        hiloServidor.enviarMensajeATodos(mensaje);
    }

    private void enviarPuntajes() {
        String mensaje = "ActualizarPuntaje:" +
            estadoPartida.getPuntaje1() + ":" +
            estadoPartida.getPuntaje2();
        hiloServidor.enviarMensajeATodos(mensaje);
    }

    @Override
    public void onIniciarJuego() {
        System.out.println("¡Ambos jugadores conectados! Iniciando juego...");
        juegoIniciado = true;

        sincronizarEstadoConClientes();
        enviarPuntajes();

        // Notificar al callback del servidor
        if (callbackServidor != null) {
            callbackServidor.onIniciarJuego();
        }
    }

    @Override
    public void onMoverMazo(int numJugador, float velocidadX, float velocidadY) {
        aplicarMovimientoMazo(numJugador, velocidadX, velocidadY);
    }

    public void aplicarMovimientoMazo(int numeroJugador, float velX, float velY) {
        Mazo mazo = (numeroJugador == 1) ? mazo1 : mazo2;

        if (mazo == null) {
            return;
        }

        // Aplicar velocidades
        mazo.setVelocidadX(velX);
        mazo.setVelocidadY(velY);

        // Calcular límites según el jugador
        float limiteIzq, limiteDer;
        if (numeroJugador == 1) {
            limiteIzq = xCancha;
            limiteDer = xCancha + CANCHA_ANCHO / 2f;
        } else {
            limiteIzq = xCancha + CANCHA_ANCHO / 2f;
            limiteDer = xCancha + CANCHA_ANCHO;
        }

        // Actualizar posición con validaciones de límites (SERVER-SIDE)
        mazo.actualizarPosicion((int) limiteIzq, (int) limiteDer,
            (int) yCancha, (int) (yCancha + CANCHA_ALTO));

        // Enviar la posición actualizada a TODOS los clientes
        enviarActualizacionPosicionMazo(numeroJugador, mazo);
    }

    public void actualizarTodosLosMazos() {
        // Mazo 1
        if (mazo1 != null) {
            float limiteIzq1 = xCancha;
            float limiteDer1 = xCancha + CANCHA_ANCHO / 2f;
            mazo1.actualizarPosicion((int) limiteIzq1, (int) limiteDer1,
                (int) yCancha, (int) (yCancha + CANCHA_ALTO));
        }

        // Mazo 2
        if (mazo2 != null) {
            float limiteIzq2 = xCancha + CANCHA_ANCHO / 2f;
            float limiteDer2 = xCancha + CANCHA_ANCHO;
            mazo2.actualizarPosicion((int) limiteIzq2, (int) limiteDer2,
                (int) yCancha, (int) (yCancha + CANCHA_ALTO));
        }
    }

    private void enviarActualizacionPosicionMazo(int numeroJugador, Mazo mazo) {
        // Formato: "ActualizarPosicion:Mazo:numJugador:x:y"
        String mensaje = "ActualizarPosicion:Mazo:" + numeroJugador + ":" +
            mazo.getPosicionX() + ":" + mazo.getPosicionY();
        hiloServidor.enviarMensajeATodos(mensaje);
    }

    //Envía las posiciones de todos los mazos
    public void sincronizarPosiciones() {
        if (mazo1 != null) {
            enviarActualizacionPosicionMazo(1, mazo1);
        }
        if (mazo2 != null) {
            enviarActualizacionPosicionMazo(2, mazo2);
        }
    }

    @Override
    public void onActualizarPosicionDisco(float x, float y, float velX, float velY) {
        // No usado en servidor
    }

    @Override
    public void onActualizarPosicionMazo(int numeroJugador, float x, float y) {
        // No usado en servidor
    }

    @Override
    public void onActualizarPuntaje(int puntaje1, int puntaje2) {
        // No usado en servidor
    }

    @Override
    public void onGol(int direccion) {
        // No usado en servidor
    }

    @Override
    public void onFinalizarJuego(int ganador) {
        // No usado en servidor
    }

    @Override
    public void onActualizarTiempo(float tiempoRestante) {

    }

    public void detener() {
        if (hiloServidor != null) {
            hiloServidor.desconectarClientes();
            hiloServidor.terminar();
        }
    }

    public boolean isJuegoIniciado() {
        return juegoIniciado;
    }

    public boolean isJuegoTerminado() {
        return juegoTerminado;
    }

    public EstadoPartida getEstadoPartida() {
        return estadoPartida;
    }

    public HiloServidor getServidorThread() {
        return this.hiloServidor;
    }
}
