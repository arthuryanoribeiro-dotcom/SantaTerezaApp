package br.ufms.santatereza.ui;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.auth.LoginActivity;

/**
 * Base para todas as telas restritas. Se não houver sessão ativa, o usuário é levado
 * ao login e a tela é fechada. Isso impede acesso por atalhos, botão voltar após o
 * logout ou abertura direta de uma Activity.
 */
public abstract class BaseAutenticadaActivity extends AppCompatActivity {

    protected long usuarioId;

    /** Chame logo após super.onCreate(). Retorna false se a tela deve parar de carregar. */
    protected boolean garantirSessao() {
        SessionManager sessao = SessionManager.get(this);
        if (!sessao.estaLogado()) {
            irParaLogin();
            return false;
        }
        usuarioId = sessao.getUsuarioId();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!SessionManager.get(this).estaLogado()) irParaLogin();
    }

    protected void irParaLogin() {
        Intent i = new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    /** Encerra a sessão e limpa toda a pilha de telas. */
    public void fazerLogout() {
        SessionManager.get(this).encerrarSessao();
        irParaLogin();
    }
}
