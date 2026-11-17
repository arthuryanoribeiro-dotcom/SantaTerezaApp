package br.ufms.santatereza.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Usuario;
import br.ufms.santatereza.seguranca.SenhaUtil;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.MainActivity;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.UiUtil;
import br.ufms.santatereza.util.Validador;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout campoEmail, campoSenha;
    private MaterialButton botaoEntrar;
    private LinearProgressIndicator progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Se já existe sessão (ex.: voltou para esta tela), vai direto ao app
        if (SessionManager.get(this).estaLogado()) {
            abrirPrincipal();
            return;
        }
        setContentView(R.layout.activity_login);

        campoEmail = findViewById(R.id.campoEmail);
        campoSenha = findViewById(R.id.campoSenha);
        botaoEntrar = findViewById(R.id.botaoEntrar);
        progresso = findViewById(R.id.progresso);
        UiUtil.limparErroAoDigitar(campoEmail, campoSenha);

        botaoEntrar.setOnClickListener(v -> entrar());
        // "Concluir" no teclado já tenta entrar (menos toques = usabilidade)
        TextView editSenha = findViewById(R.id.editSenha);
        editSenha.setOnEditorActionListener((tv, acao, ev) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) { entrar(); return true; }
            return false;
        });
        findViewById(R.id.botaoIrCadastro).setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class)));
    }

    private void entrar() {
        UiUtil.esconderTeclado(botaoEntrar);
        String email = Validador.normalizarEmail(UiUtil.texto(campoEmail));
        String senha = UiUtil.senha(campoSenha);

        boolean ok = true;
        if (email.isEmpty()) { UiUtil.erro(campoEmail, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.emailValido(email)) { UiUtil.erro(campoEmail, R.string.erro_email_invalido); ok = false; }
        if (senha.isEmpty()) { UiUtil.erro(campoSenha, R.string.erro_obrigatorio); ok = false; }
        if (!ok) { UiUtil.focarPrimeiroErro(campoEmail, campoSenha); return; }

        carregando(true);
        AppExecutors.disco().execute(() -> {
            Usuario u = AppDatabase.get(this).usuarioDao().buscarPorEmail(email);
            // A mensagem de erro é a mesma para e-mail inexistente e senha errada,
            // para não revelar quais e-mails estão cadastrados.
            boolean valido = u != null && SenhaUtil.conferir(senha, u.senhaHash);
            AppExecutors.principal(() -> {
                if (isFinishing()) return;
                carregando(false);
                if (valido) {
                    SessionManager.get(this).iniciarSessao(u.id);
                    abrirPrincipal();
                } else {
                    UiUtil.erro(campoSenha, R.string.erro_login);
                    UiUtil.focarPrimeiroErro(campoSenha);
                }
            });
        });
    }

    private void carregando(boolean sim) {
        progresso.setVisibility(sim ? View.VISIBLE : View.GONE);
        botaoEntrar.setEnabled(!sim);
        if (sim) progresso.announceForAccessibility(getString(R.string.entrando));
    }

    private void abrirPrincipal() {
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
