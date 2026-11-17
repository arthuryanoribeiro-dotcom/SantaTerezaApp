package br.ufms.santatereza.ui.auth;

import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Usuario;
import br.ufms.santatereza.data.UsuarioDao;
import br.ufms.santatereza.notificacao.NotificationHelper;
import br.ufms.santatereza.seguranca.SenhaUtil;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.MainActivity;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.UiUtil;
import br.ufms.santatereza.util.Validador;

public class CadastroActivity extends AppCompatActivity {

    private TextInputLayout campoNome, campoEmail, campoTelefone, campoCidade, campoSenha, campoConfirmar;
    private MaterialButton botaoCriar;
    private LinearProgressIndicator progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoNome = findViewById(R.id.campoNome);
        campoEmail = findViewById(R.id.campoEmail);
        campoTelefone = findViewById(R.id.campoTelefone);
        campoCidade = findViewById(R.id.campoCidade);
        campoSenha = findViewById(R.id.campoSenha);
        campoConfirmar = findViewById(R.id.campoConfirmar);
        botaoCriar = findViewById(R.id.botaoCriar);
        progresso = findViewById(R.id.progresso);
        UiUtil.limparErroAoDigitar(campoNome, campoEmail, campoTelefone, campoSenha, campoConfirmar);

        botaoCriar.setOnClickListener(v -> cadastrar());
    }

    private void cadastrar() {
        UiUtil.esconderTeclado(botaoCriar);
        String nome = UiUtil.texto(campoNome);
        String email = Validador.normalizarEmail(UiUtil.texto(campoEmail));
        String telefone = Validador.somenteDigitos(UiUtil.texto(campoTelefone));
        String cidade = UiUtil.texto(campoCidade);
        String senha = UiUtil.senha(campoSenha);
        String confirmar = UiUtil.senha(campoConfirmar);

        boolean ok = true;
        if (nome.isEmpty()) { UiUtil.erro(campoNome, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.nomeValido(nome)) { UiUtil.erro(campoNome, R.string.erro_nome_curto); ok = false; }
        if (email.isEmpty()) { UiUtil.erro(campoEmail, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.emailValido(email)) { UiUtil.erro(campoEmail, R.string.erro_email_invalido); ok = false; }
        if (!Validador.telefoneValido(telefone)) { UiUtil.erro(campoTelefone, R.string.erro_telefone); ok = false; }
        if (senha.isEmpty()) { UiUtil.erro(campoSenha, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.senhaForte(senha)) { UiUtil.erro(campoSenha, R.string.erro_senha_fraca); ok = false; }
        if (confirmar.isEmpty()) { UiUtil.erro(campoConfirmar, R.string.erro_obrigatorio); ok = false; }
        else if (!confirmar.equals(senha)) { UiUtil.erro(campoConfirmar, R.string.erro_senhas_diferentes); ok = false; }
        if (!ok) {
            UiUtil.focarPrimeiroErro(campoNome, campoEmail, campoTelefone, campoSenha, campoConfirmar);
            return;
        }

        carregando(true);
        AppExecutors.disco().execute(() -> {
            UsuarioDao dao = AppDatabase.get(this).usuarioDao();
            long novoId = -1;
            boolean emailEmUso = dao.buscarPorEmail(email) != null;
            if (!emailEmUso) {
                Usuario u = new Usuario();
                u.nome = nome;
                u.email = email;
                u.senhaHash = SenhaUtil.gerarHash(senha); // apenas o hash é salvo
                u.telefone = telefone.isEmpty() ? null : telefone;
                u.cidade = cidade.isEmpty() ? null : cidade;
                u.criadoEm = System.currentTimeMillis();
                try {
                    novoId = dao.inserir(u);
                } catch (SQLiteConstraintException e) {
                    emailEmUso = true; // corrida rara: e-mail cadastrado ao mesmo tempo
                }
            }
            final long id = novoId;
            final boolean emUso = emailEmUso;
            AppExecutors.principal(() -> {
                if (isFinishing()) return;
                carregando(false);
                if (emUso) {
                    UiUtil.erro(campoEmail, R.string.erro_email_em_uso);
                    UiUtil.focarPrimeiroErro(campoEmail);
                    return;
                }
                SessionManager.get(this).iniciarSessao(id);
                NotificationHelper.boasVindas(this, nome.split(" ")[0]);
                Intent i = new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        });
    }

    private void carregando(boolean sim) {
        progresso.setVisibility(sim ? View.VISIBLE : View.GONE);
        botaoCriar.setEnabled(!sim);
        if (sim) progresso.announceForAccessibility(getString(R.string.criando_conta));
    }
}
