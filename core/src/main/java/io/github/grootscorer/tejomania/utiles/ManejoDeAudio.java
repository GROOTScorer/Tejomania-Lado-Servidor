package io.github.grootscorer.tejomania.utiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class ManejoDeAudio {
    private static boolean sonidoActivado = true;
    private static float volumenSonido = 1;

    public static void activarSonido(String ruta) {
        if (!sonidoActivado) return;
        Sound sonido = Gdx.audio.newSound(Gdx.files.internal(ruta));
        sonido.play(volumenSonido);
    }
}
