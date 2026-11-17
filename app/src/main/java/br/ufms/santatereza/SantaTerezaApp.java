package br.ufms.santatereza;

import android.app.Application;

import org.osmdroid.config.Configuration;

import java.io.File;

import br.ufms.santatereza.notificacao.NotificationHelper;

public class SantaTerezaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.criarCanais(this);

        // osmdroid: identifica o app para o servidor do OpenStreetMap e guarda o cache
        // de blocos do mapa na pasta privada do app (não exige permissão de armazenamento).
        Configuration.getInstance().setUserAgentValue(getPackageName());
        File base = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));
    }
}
