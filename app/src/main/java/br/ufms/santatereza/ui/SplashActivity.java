package br.ufms.santatereza.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.auth.LoginActivity;

/**
 * Tela de abertura com a SplashScreen API. Não usa atraso artificial: a splash fica
 * visível apenas enquanto o app inicializa e já encaminha conforme o estado da sessão.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        boolean logado = SessionManager.get(this).estaLogado();
        Intent destino = new Intent(this, logado ? MainActivity.class : LoginActivity.class);
        startActivity(destino);
        finish();
    }
}
