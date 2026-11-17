package br.ufms.santatereza.seguranca;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Controle de sessão. O id do usuário logado fica em EncryptedSharedPreferences
 * (chaves e valores cifrados com AES-256, chave mestra protegida pelo Android Keystore).
 */
public final class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String ARQUIVO = "sessao_segura";
    private static final String CHAVE_USUARIO = "usuario_id";
    private static final String CHAVE_NOTIF_PERGUNTADA = "notif_perguntada";

    private static SessionManager instancia;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        SharedPreferences p;
        try {
            MasterKey chave = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            p = EncryptedSharedPreferences.create(context, ARQUIVO, chave,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            // Em raros aparelhos o Keystore falha; mantemos o app funcionando com
            // preferências privadas (acessíveis apenas por este app).
            Log.e(TAG, "Falha ao abrir preferências criptografadas", e);
            p = context.getSharedPreferences(ARQUIVO + "_fallback", Context.MODE_PRIVATE);
        }
        prefs = p;
    }

    public static synchronized SessionManager get(Context context) {
        if (instancia == null) {
            instancia = new SessionManager(context.getApplicationContext());
        }
        return instancia;
    }

    public void iniciarSessao(long usuarioId) {
        prefs.edit().putLong(CHAVE_USUARIO, usuarioId).apply();
    }

    public void encerrarSessao() {
        prefs.edit().remove(CHAVE_USUARIO).apply();
    }

    public boolean estaLogado() {
        return getUsuarioId() > 0;
    }

    public long getUsuarioId() {
        return prefs.getLong(CHAVE_USUARIO, -1L);
    }

    public boolean jaPerguntouNotificacao() {
        return prefs.getBoolean(CHAVE_NOTIF_PERGUNTADA, false);
    }

    public void marcarNotificacaoPerguntada() {
        prefs.edit().putBoolean(CHAVE_NOTIF_PERGUNTADA, true).apply();
    }
}
