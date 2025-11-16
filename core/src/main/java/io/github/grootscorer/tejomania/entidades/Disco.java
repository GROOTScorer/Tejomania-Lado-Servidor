package io.github.grootscorer.tejomania.entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import io.github.grootscorer.tejomania.utiles.ManejoDeAudio;
import java.util.Random;

public class Disco {
    private float posicionX, posicionY;
    private float velocidadX, velocidadY;
    private Circle hitboxDisco;

    float escalaX = 1.0f; // Valor por defecto
    float escalaY = 1.0f; // Valor por defecto
    float escalaFuente = 1.0f; // Valor por defecto

    private int RADIO_DISCO = 13;
    private  int maxVelocidad = 500;
    private long tiempoUltimoSonidoMazo = 0;
    private long tiempoUltimoSonidoDisco = 0;
    private static final long COOLDOWN_SONIDO_MAZO_MS = 300;
    private static final long COOLDOWN_SONIDO_DISCO_MS = 200;
    private Texture textura;

    private boolean haAnotadoGol = false;

    public Disco() {
        Random rand = new Random();
        int lado = rand.nextInt(2);
        this.posicionX = (lado == 0) ? 100 : 400;
        this.posicionY = 300;
        this.velocidadX = 0;
        this.velocidadY = 0;
        this.hitboxDisco = new Circle(posicionX + RADIO_DISCO, posicionY + RADIO_DISCO, RADIO_DISCO);
        this.haAnotadoGol = false;
    }

    public boolean colisionaConMazo(Mazo mazo) {
        Circle hitboxMazo = new Circle(mazo.getPosicionX() + mazo.getRadioMazo(), mazo.getPosicionY() + mazo.getRadioMazo(), mazo.getRadioMazo());
        return hitboxDisco.overlaps(hitboxMazo);
    }

    public void inicializarGraficos() {
        // Código GDX movido del inicializador de campos
        escalaX = (float) Gdx.graphics.getWidth() / 640f;
        escalaY = (float) Gdx.graphics.getHeight() / 480f;
        escalaFuente = Math.max(escalaX, escalaY);

        this.RADIO_DISCO = (int) (13 * escalaY); // Recalcular radio para el cliente

        // Inicializar textura
        if (this.textura == null) {
            this.textura = new Texture(Gdx.files.internal("imagenes/sprites/disco.png"));
        }

        // Actualizar hitbox con el nuevo radio
        this.hitboxDisco.setRadius(RADIO_DISCO);
        this.hitboxDisco.setPosition(posicionX + RADIO_DISCO, posicionY + RADIO_DISCO);
    }

    public void actualizarPosicion(float delta, float xCancha, float yCancha, float CANCHA_ANCHO, float CANCHA_ALTO) {
        this.posicionX += this.velocidadX * delta;
        this.posicionY += this.velocidadY * delta;

        float radioSemicirculo = CANCHA_ALTO / 4.5f;
        float centroSemicirculoY = yCancha + CANCHA_ALTO / 2f;
        float limiteInferiorArco = centroSemicirculoY - radioSemicirculo;
        float limiteSuperiorArco = centroSemicirculoY + radioSemicirculo;

        boolean discoEnAreaVerticalArco = (this.posicionY + RADIO_DISCO >= limiteInferiorArco) &&
            (this.posicionY + RADIO_DISCO <= limiteSuperiorArco);

        if (!discoEnAreaVerticalArco) {
            if (this.posicionX <= xCancha) {
                ManejoDeAudio.activarSonido((String.valueOf(Gdx.files.internal("audio/sonidos/sonido_golpe_pared.mp3"))));
                this.posicionX = xCancha;
                this.velocidadX = -this.velocidadX;
            }
            if (this.posicionX + (RADIO_DISCO * 2) >= xCancha + CANCHA_ANCHO) {
                ManejoDeAudio.activarSonido((String.valueOf(Gdx.files.internal("audio/sonidos/sonido_golpe_pared.mp3"))));
                this.posicionX = xCancha + CANCHA_ANCHO - (RADIO_DISCO * 2);
                this.velocidadX = -this.velocidadX;
            }
        }

        if (this.posicionY <= yCancha) {
            ManejoDeAudio.activarSonido((String.valueOf(Gdx.files.internal("audio/sonidos/sonido_golpe_pared.mp3"))));
            this.posicionY = yCancha;
            this.velocidadY = -this.velocidadY;
        }
        if (this.posicionY + (RADIO_DISCO * 2) >= yCancha + CANCHA_ALTO) {
            ManejoDeAudio.activarSonido((String.valueOf(Gdx.files.internal("audio/sonidos/sonido_golpe_pared.mp3"))));
            this.posicionY = yCancha + CANCHA_ALTO - (RADIO_DISCO * 2);
            this.velocidadY = -this.velocidadY;
        }

        this.hitboxDisco.setPosition(posicionX + RADIO_DISCO, posicionY + RADIO_DISCO);
    }

    public void manejarColision(Mazo mazo) {
        long tiempoActual = com.badlogic.gdx.utils.TimeUtils.millis();

        if (tiempoActual - tiempoUltimoSonidoMazo >= COOLDOWN_SONIDO_MAZO_MS) {
            ManejoDeAudio.activarSonido((String.valueOf(Gdx.files.internal("audio/sonidos/sonido_golpe_mazo.mp3"))));
            tiempoUltimoSonidoMazo = tiempoActual;
        }

        if (this.velocidadX == 0 && this.velocidadY == 0) {
            setVelocidadX(mazo.getVelocidadX() * 5f);
            setVelocidadY(mazo.getVelocidadY() * 5f);
        } else {
            float centroDiscoX = this.posicionX + RADIO_DISCO;
            float centroDiscoY = this.posicionY + RADIO_DISCO;
            float centroMazoX = mazo.getPosicionX() + mazo.getRadioMazo();
            float centroMazoY = mazo.getPosicionY() + mazo.getRadioMazo();

            float vectorX = centroDiscoX - centroMazoX;
            float vectorY = centroDiscoY - centroMazoY;

            float anguloColision = (float) Math.atan2(vectorY, vectorX);

            float velocidadActual = (float) Math.sqrt(velocidadX * velocidadX + velocidadY * velocidadY);

            float nuevaVelocidad = Math.max(40f, velocidadActual * 1.2f);

            float nuevoAngulo = anguloColision + (float)(Math.random() - 0.5) * 0.3f;

            setVelocidadX(nuevaVelocidad * (float) Math.cos(nuevoAngulo));
            setVelocidadY(nuevaVelocidad * (float) Math.sin(nuevoAngulo));
        }

        float centroDiscoX = this.posicionX + RADIO_DISCO;
        float centroDiscoY = this.posicionY + RADIO_DISCO;
        float centroMazoX = mazo.getPosicionX() + mazo.getRadioMazo();
        float centroMazoY = mazo.getPosicionY() + mazo.getRadioMazo();

        float vectorX = centroDiscoX - centroMazoX;
        float vectorY = centroDiscoY - centroMazoY;
        float longitud = (float) Math.sqrt(vectorX * vectorX + vectorY * vectorY);

        if (longitud > 0) {
            vectorX /= longitud;
            vectorY /= longitud;

            float distanciaSeparacion = RADIO_DISCO + mazo.getRadioMazo() + 10;

            float nuevoCentroDiscoX = centroMazoX + vectorX * distanciaSeparacion;
            float nuevoCentroDiscoY = centroMazoY + vectorY * distanciaSeparacion;

            this.posicionX = nuevoCentroDiscoX - RADIO_DISCO;
            this.posicionY = nuevoCentroDiscoY - RADIO_DISCO;
        }
    }

    public void dibujarConTextura(SpriteBatch batch) {
        if (textura != null) {
            int tamanio = RADIO_DISCO * 2;

            batch.draw(textura, posicionX, posicionY, tamanio, tamanio);
        }
    }

    public float getVelocidadX() {
        return this.velocidadX;
    }

    public void setVelocidadX(float velocidadX) {
        this.velocidadX = velocidadX;
        if(this.velocidadX > maxVelocidad) {
            this.velocidadX = maxVelocidad;
        }

        if(this.velocidadX < -maxVelocidad) {
            this.velocidadX = -maxVelocidad;
        }
    }

    public void setVelocidadY(float velocidadY) {
        this.velocidadY = velocidadY;

        if(this.velocidadY > maxVelocidad) {
            this.velocidadY = maxVelocidad;
        }

        if(this.velocidadY < -maxVelocidad) {
            this.velocidadY = -maxVelocidad;
        }
    }

    public float getVelocidadY() {
        return this.velocidadY;
    }

    public float getPosicionX() {
        return this.posicionX;
    }

    public float getPosicionY() {
        return this.posicionY;
    }

    public int getRadioDisco() {
        return this.RADIO_DISCO;
    }

    public Circle getHitbox() {
        return this.hitboxDisco;
    }

    public void dispose() {
        if (textura != null) {
            textura.dispose();
        }
    }

    public void setPosicion(float x, float y) {
        this.posicionX = x;
        this.posicionY = y;
        this.hitboxDisco.setPosition(posicionX + RADIO_DISCO, posicionY + RADIO_DISCO);
    }

    public boolean haAnotadoGol() {
        return haAnotadoGol;
    }

    public void marcarGolAnotado() {
        this.haAnotadoGol = true;
    }

    public void reiniciarEstadoGol() {
        this.haAnotadoGol = false;
    }

    public void setMaxVelocidad(int velocidad) {
        this.maxVelocidad = velocidad;
    }

    public void reposicionarDisco(Mazo mazo, float xCancha, float yCancha, float CANCHA_ANCHO, float CANCHA_ALTO) {
        if(colisionaConMazo(mazo)) {
            if(posicionX == xCancha && (posicionY == yCancha || posicionY + (RADIO_DISCO * 2) >= yCancha + CANCHA_ALTO)) {
                setPosicionX(posicionX + 16);
            }
        } else if(this.posicionX + (RADIO_DISCO * 2) >= xCancha + CANCHA_ANCHO && (posicionY == 80 || posicionY + (RADIO_DISCO * 2) >= yCancha + CANCHA_ALTO)) {
            setPosicionX(posicionX - 16);
        }
    }

    public void reposicionarEntreDosMazos(Mazo mazo1, Mazo mazo2, float CANCHA_ALTO) {
        if(colisionaConMazo(mazo1) && colisionaConMazo(mazo2)) {
            if(posicionY < CANCHA_ALTO - 15) {
                this.posicionY += 10;
            } else {
                this.posicionY -= 10;
            }
        }
    }

    public void setPosicionX(float x) {
        this.posicionX = x;
    }
}
