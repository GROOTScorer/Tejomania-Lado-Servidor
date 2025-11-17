package io.github.grootscorer.tejomania.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.grootscorer.tejomania.Principal;
import io.github.grootscorer.tejomania.entidades.Disco;
import io.github.grootscorer.tejomania.entidades.Mazo;
import io.github.grootscorer.tejomania.estado.EstadoPartida;
import io.github.grootscorer.tejomania.interfaces.ControladorJuegoRed;
import io.github.grootscorer.tejomania.redes.LogicaJuegoServidor;
import io.github.grootscorer.tejomania.utiles.ManejoDeAudio;

public class PantallaJuegoServidor extends ScreenAdapter implements ControladorJuegoRed {
    private Stage stage;
    private Principal juego;

    private Disco disco;
    private Mazo mazo1, mazo2;

    private Skin skin;
    private EstadoPartida estadoPartida = new EstadoPartida();
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;

    private LogicaJuegoServidor logicaServidor;

    private boolean esperandoConexion = true;
    private boolean juegoIniciado = false;
    private boolean juegoTerminado = false;

    private Label labelEspera;
    private Label labelPuntaje;
    private Label labelGanador;
    private Label labelTiempo;

    private String rutaRelativaMazoRojo = "assets/imagenes/sprites/mazo_rojo.png";
    private String rutaAbsoutaMazoRojo = Gdx.files.internal(rutaRelativaMazoRojo).file().getAbsolutePath();

    private String rutaRelativaMazoAzul = "assets/imagenes/sprites/mazo_azul.png";
    private String rutaAbsolutaMazoAzul = Gdx.files.internal(rutaRelativaMazoAzul).file().getAbsolutePath();

    private final Texture mazoRojo = new Texture(Gdx.files.internal(rutaAbsoutaMazoRojo));
    private final Texture mazoAzul = new Texture(Gdx.files.internal(rutaAbsolutaMazoAzul));

    float escalaX = (float) Gdx.graphics.getWidth() / 640f;
    float escalaY = (float) Gdx.graphics.getHeight() / 480f;
    float escalaFuente = Math.max(escalaX, escalaY);

    private final int CANCHA_ANCHO = (int) (540 * escalaX);
    private final int CANCHA_ALTO = (int) (320 * escalaY);
    float xCancha = (Gdx.graphics.getWidth() - CANCHA_ANCHO) / 2f;
    float yCancha = (Gdx.graphics.getHeight() - CANCHA_ALTO) / 2f;

    public PantallaJuegoServidor(Principal juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        String rutaRelativaSkin = "assets/ui/uiskin.json";
        String rutaAbsolutaSkin = Gdx.files.internal(rutaRelativaSkin).file().getAbsolutePath();
        skin = new Skin(Gdx.files.internal(rutaAbsolutaSkin));
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        disco = new Disco();
        mazo1 = new Mazo();
        mazo2 = new Mazo();

        disco.inicializarGraficos();

        mazo1.setTextura(mazoAzul);
        mazo2.setTextura(mazoRojo);

        logicaServidor = new LogicaJuegoServidor(estadoPartida, escalaX, escalaY, this);

        labelEspera = new Label("Esperando...", skin, "default");
        labelEspera.setColor(Color.GREEN);
        labelEspera.setFontScale(escalaFuente * 2.0f);
        labelEspera.setPosition(
            Gdx.graphics.getWidth() / 2f - labelEspera.getWidth(),
            Gdx.graphics.getHeight() / 2f + 30
        );
        stage.addActor(labelEspera);

        labelTiempo = new Label("5:00", skin, "default");
        labelTiempo.setColor(Color.WHITE);
        labelTiempo.setFontScale(escalaFuente * 1.2f);
        labelTiempo.setPosition(
            Gdx.graphics.getWidth() / 2f - labelTiempo.getWidth() / 2f,
            Gdx.graphics.getHeight() - 75
        );
        labelTiempo.setVisible(false);
        stage.addActor(labelTiempo);

        labelPuntaje = new Label("0 - 0", skin, "default");
        labelPuntaje.setColor(Color.WHITE);
        labelPuntaje.setFontScale(escalaFuente * 1.5f);
        labelPuntaje.setPosition(
            Gdx.graphics.getWidth() / 2f - labelPuntaje.getWidth() / 2f,
            Gdx.graphics.getHeight() - 50
        );
        labelPuntaje.setVisible(false);
        stage.addActor(labelPuntaje);

        System.out.println("Servidor creado. Esperando conexión...");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (esperandoConexion) {
            stage.act(delta);
            stage.draw();

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                volverAlMenu();
            }
            return;
        }

        if (juegoIniciado && !juegoTerminado) {
            logicaServidor.actualizar(delta);

            mazo1.actualizarAnimacion(delta);
            mazo2.actualizarAnimacion(delta);

            actualizarLabelTiempo();
        }

        dibujarCancha();

        batch.begin();
        mazo1.dibujarConTextura(batch);
        mazo2.dibujarConTextura(batch);
        disco.dibujarConTextura(batch);
        batch.end();

        stage.act(delta);
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            volverAlMenu();
        }
    }

    private void dibujarCancha() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.WHITE);

        shapeRenderer.rect(xCancha, yCancha, CANCHA_ANCHO, CANCHA_ALTO);
        shapeRenderer.end();

        dibujarLineasCancha();
    }

    private void dibujarLineasCancha() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);

        float grosorLinea = Math.max(1f, 2f * Math.min(escalaX, escalaY));
        Gdx.gl.glLineWidth(grosorLinea);

        float mitadCanchaX = xCancha + CANCHA_ANCHO / 2f;
        shapeRenderer.line(mitadCanchaX, yCancha, mitadCanchaX, yCancha + CANCHA_ALTO);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.WHITE);

        float radioY = CANCHA_ALTO / 4.5f;
        float radioX = radioY / 1.4f;

        float centroIzquierdoX = xCancha;
        float centroIzquierdoY = yCancha + CANCHA_ALTO / 2f;

        int segmentos = Math.max(16, (int)(32 * Math.min(escalaX, escalaY)));
        for (int i = 0; i < segmentos; i++) {
            float angulo1 = (float) (-Math.PI / 2 + (Math.PI * i) / segmentos);
            float angulo2 = (float) (-Math.PI / 2 + (Math.PI * (i + 1)) / segmentos);

            float x1 = centroIzquierdoX + radioX * (float) Math.cos(angulo1);
            float y1 = centroIzquierdoY + radioY * (float) Math.sin(angulo1);
            float x2 = centroIzquierdoX + radioX * (float) Math.cos(angulo2);
            float y2 = centroIzquierdoY + radioY * (float) Math.sin(angulo2);

            shapeRenderer.triangle(centroIzquierdoX, centroIzquierdoY, x1, y1, x2, y2);
        }

        float centroDerechoX = xCancha + CANCHA_ANCHO;
        float centroDerechoY = yCancha + CANCHA_ALTO / 2f;

        for (int i = 0; i < segmentos; i++) {
            float angulo1 = (float) (Math.PI / 2 + (Math.PI * i) / segmentos);
            float angulo2 = (float) (Math.PI / 2 + (Math.PI * (i + 1)) / segmentos);

            float x1 = centroDerechoX + radioX * (float) Math.cos(angulo1);
            float y1 = centroDerechoY + radioY * (float) Math.sin(angulo1);
            float x2 = centroDerechoX + radioX * (float) Math.cos(angulo2);
            float y2 = centroDerechoY + radioY * (float) Math.sin(angulo2);

            shapeRenderer.triangle(centroDerechoX, centroDerechoY, x1, y1, x2, y2);
        }

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);

        for (int i = 0; i < segmentos; i++) {
            float angulo1 = (float) (-Math.PI / 2 + (Math.PI * i) / segmentos);
            float angulo2 = (float) (-Math.PI / 2 + (Math.PI * (i + 1)) / segmentos);

            float x1 = centroIzquierdoX + radioX * (float) Math.cos(angulo1);
            float y1 = centroIzquierdoY + radioY * (float) Math.sin(angulo1);
            float x2 = centroIzquierdoX + radioX * (float) Math.cos(angulo2);
            float y2 = centroIzquierdoY + radioY * (float) Math.sin(angulo2);

            shapeRenderer.line(x1, y1, x2, y2);
        }

        for (int i = 0; i < segmentos; i++) {
            float angulo1 = (float) (Math.PI / 2 + (Math.PI * i) / segmentos);
            float angulo2 = (float) (Math.PI / 2 + (Math.PI * (i + 1)) / segmentos);

            float x1 = centroDerechoX + radioX * (float) Math.cos(angulo1);
            float y1 = centroDerechoY + radioY * (float) Math.sin(angulo1);
            float x2 = centroDerechoX + radioX * (float) Math.cos(angulo2);
            float y2 = centroDerechoY + radioY * (float) Math.sin(angulo2);

            shapeRenderer.line(x1, y1, x2, y2);
        }

        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
    }

    @Override
    public void onIniciarJuego() {
        System.out.println("¡Jugador 2 conectado! Iniciando juego...");

        // Actualizar las variables de control
        esperandoConexion = false;
        juegoIniciado = true;

        // Ocultar label de espera
        labelEspera.setVisible(false);

        // Mostrar puntaje
        labelPuntaje.setVisible(true);
        actualizarLabelPuntaje();

        labelTiempo.setVisible(true);
    }

    @Override
    public void onActualizarPosicionDisco(float x, float y, float velX, float velY) {
        disco.setPosicion(x, y);
        disco.setVelocidadX(velX);
        disco.setVelocidadY(velY);

        disco.getHitbox().setPosition(x + disco.getRadioDisco(), y + disco.getRadioDisco());
    }

    @Override
    public void onActualizarPosicionMazo(int numeroJugador, float x, float y) {
        if (numeroJugador == 1) {
            mazo1.setPosicion((int) x, (int) y);
        } else if (numeroJugador == 2) {
            mazo2.setPosicion((int) x, (int) y);
        }
    }

    @Override
    public void onActualizarPuntaje(int puntaje1, int puntaje2) {
        estadoPartida.setPuntaje1(puntaje1);
        estadoPartida.setPuntaje2(puntaje2);
        actualizarLabelPuntaje();
    }

    private void actualizarLabelPuntaje() {
        String puntajeTexto = formatearPuntaje(estadoPartida.getPuntaje1()) +
            " - " +
            formatearPuntaje(estadoPartida.getPuntaje2());
        labelPuntaje.setText(puntajeTexto);
        labelPuntaje.setPosition(
            Gdx.graphics.getWidth() / 2f - labelPuntaje.getWidth() / 2f,
            Gdx.graphics.getHeight() - 50
        );
    }

    private void actualizarLabelTiempo() {
        int tiempoRestante = (int) estadoPartida.getTiempoRestante();
        int minutos = tiempoRestante / 60;
        int segundos = tiempoRestante % 60;
        String tiempoTexto = String.format("%d:%02d", minutos, segundos);
        labelTiempo.setText(tiempoTexto);
        labelTiempo.setPosition(
            Gdx.graphics.getWidth() / 2f - labelTiempo.getWidth() / 2f,
            Gdx.graphics.getHeight() - 75
        );
    }

    private String formatearPuntaje(int puntaje) {
        return puntaje < 10 ? "" + puntaje : String.valueOf(puntaje);
    }

    @Override
    public void onGol(int direccion) {
        String rutaRelativaSonido = "assets/audio/sonidos/sonido_gol.mp3";
        String rutaAbsolutaSonido = Gdx.files.internal(rutaRelativaSonido).file().getAbsolutePath();

        ManejoDeAudio.activarSonido(String.valueOf(Gdx.files.internal(rutaAbsolutaSonido)));
    }

    @Override
    public void onMoverMazo(int numeroJugador, float velocidadX, float velocidadY) {
        // No usado directamente aquí
    }

    @Override
    public void onFinalizarJuego(int ganador) {
        juegoTerminado = true;

        String textoGanador;
        if (estadoPartida.getPuntaje1() > estadoPartida.getPuntaje2()) {
            textoGanador = "JUGADOR 1 GANA";
        } else if (estadoPartida.getPuntaje2() > estadoPartida.getPuntaje1()) {
            textoGanador = "JUGADOR 2 GANA";
        } else {
            textoGanador = "EMPATE";
        }

        Table tableGanador = new Table();
        stage.addActor(tableGanador);

        labelGanador = new Label(textoGanador, skin, "default");
        labelGanador.setColor(Color.RED);
        labelGanador.setFontScale(escalaFuente * 4.0f);

        tableGanador.add(labelGanador).padBottom(50 * escalaFuente).padLeft(Gdx.graphics.getWidth() / 2f);

        tableGanador.addAction(Actions.sequence(
            Actions.delay(3f),
            Actions.run(() -> {
                cerrarServidor();
                Gdx.app.exit();
            })
        ));
    }

    private void volverAlMenu() {
        if (logicaServidor != null) {
            logicaServidor.detener();
        }
    }

    @Override
    public void onActualizarTiempo(float tiempoRestante) {
        // No usado en servidor
    }

    private void cerrarServidor() {
        if (logicaServidor != null) {
            logicaServidor.detener();
            logicaServidor = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (logicaServidor != null) {
            logicaServidor.detener();
        }
        stage.dispose();
        skin.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        disco.dispose();
        mazoRojo.dispose();
        mazoAzul.dispose();
    }
}
