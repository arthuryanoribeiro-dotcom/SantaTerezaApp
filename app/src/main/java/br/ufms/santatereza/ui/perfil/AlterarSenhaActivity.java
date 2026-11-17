package br.ufms.santatereza.ui.perfil;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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
import br.ufms.santatereza.ui.BaseAutenticadaActivity;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.UiUtil;
import br.ufms.santatereza.util.Validador;

/**
 * Troca de senha: confere a senha atual com BCrypt e grava um NOVO hash (com novo salt).
 * A senha em texto puro nunca é armazenada.
 */
public class AlterarSenhaActivity extends BaseAutenticadaActivity {

    private TextInputLayout campoAtual, campoNova, campoConfirmar;
    private MaterialButton botaoSalvar;
    private LinearProgressIndicator progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!garantirSessao()) return;
        setContentView(R.layout.activity_alterar_senha);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoAtual = findViewById(R.id.campoAtual);
        campoNova = findViewById(R.id.campoNova);
        campoConfirmar = findViewById(R.id.campoConfirmar);
        botaoSalvar = findViewById(R.id.botaoSalvar);
        progresso = findViewById(R.id.progresso);
        UiUtil.limparErroAoDigitar(campoAtual, campoNova, campoConfirmar);
        botaoSalvar.setOnClickListener(v -> salvar());
    }

    private void salvar() {
        UiUtil.esconderTeclado(botaoSalvar);
        String atual = UiUtil.senha(campoAtual);
        String nova = UiUtil.senha(campoNova);
        String confirmar = UiUtil.senha(campoConfirmar);

        boolean ok = true;
        if (atual.isEmpty()) { UiUtil.erro(campoAtual, R.string.erro_obrigatorio); ok = false; }
        if (nova.isEmpty()) { UiUtil.erro(campoNova, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.senhaForte(nova)) { UiUtil.erro(campoNova, R.string.erro_senha_fraca); ok = false; }
        else if (nova.equals(atual)) { UiUtil.erro(campoNova, R.string.erro_senha_igual); ok = false; }
        if (confirmar.isEmpty()) { UiUtil.erro(campoConfirmar, R.string.erro_obrigatorio); ok = false; }
        else if (!confirmar.equals(nova)) { UiUtil.erro(campoConfirmar, R.string.erro_senhas_diferentes); ok = false; }
        if (!ok) { UiUtil.focarPrimeiroErro(campoAtual, campoNova, campoConfirmar); return; }

        carregando(true);
        AppExecutors.disco().execute(() -> {
            UsuarioDao dao = AppDatabase.get(this).usuarioDao();
            Usuario u = dao.buscarPorId(usuarioId);
            boolean atualCorreta = u != null && SenhaUtil.conferir(atual, u.senhaHash);
            if (atualCorreta) dao.atualizarSenha(usuarioId, SenhaUtil.gerarHash(nova));
            AppExecutors.principal(() -> {
                if (isFinishing()) return;
                carregando(false);
                if (!atualCorreta) {
                    UiUtil.erro(campoAtual, R.string.erro_senha_atual);
                    UiUtil.focarPrimeiroErro(campoAtual);
                    return;
                }
                NotificationHelper.senhaAlterada(this);
                Toast.makeText(this, R.string.senha_alterada, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void carregando(boolean sim) {
        progresso.setVisibility(sim ? View.VISIBLE : View.GONE);
        botaoSalvar.setEnabled(!sim);
    }
}
