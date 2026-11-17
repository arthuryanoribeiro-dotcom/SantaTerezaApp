package br.ufms.santatereza.ui.perfil;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Usuario;
import br.ufms.santatereza.data.UsuarioDao;
import br.ufms.santatereza.ui.BaseAutenticadaActivity;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.UiUtil;
import br.ufms.santatereza.util.Validador;

/** Edição de nome, e-mail, telefone e cidade, com persistência no Room. */
public class EditarPerfilActivity extends BaseAutenticadaActivity {

    private TextInputLayout campoNome, campoEmail, campoTelefone, campoCidade;
    private MaterialButton botaoSalvar;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!garantirSessao()) return;
        setContentView(R.layout.activity_editar_perfil);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoNome = findViewById(R.id.campoNome);
        campoEmail = findViewById(R.id.campoEmail);
        campoTelefone = findViewById(R.id.campoTelefone);
        campoCidade = findViewById(R.id.campoCidade);
        botaoSalvar = findViewById(R.id.botaoSalvar);
        UiUtil.limparErroAoDigitar(campoNome, campoEmail, campoTelefone);
        botaoSalvar.setEnabled(false);
        botaoSalvar.setOnClickListener(v -> salvar());

        AppExecutors.disco().execute(() -> {
            Usuario u = AppDatabase.get(this).usuarioDao().buscarPorId(usuarioId);
            AppExecutors.principal(() -> {
                if (u == null || isFinishing()) { if (u == null) fazerLogout(); return; }
                usuario = u;
                if (savedInstanceState == null) {
                    UiUtil.definirTexto(campoNome, u.nome);
                    UiUtil.definirTexto(campoEmail, u.email);
                    UiUtil.definirTexto(campoTelefone, u.telefone);
                    UiUtil.definirTexto(campoCidade, u.cidade);
                }
                botaoSalvar.setEnabled(true);
            });
        });
    }

    private void salvar() {
        UiUtil.esconderTeclado(botaoSalvar);
        String nome = UiUtil.texto(campoNome);
        String email = Validador.normalizarEmail(UiUtil.texto(campoEmail));
        String telefone = Validador.somenteDigitos(UiUtil.texto(campoTelefone));
        String cidade = UiUtil.texto(campoCidade);

        boolean ok = true;
        if (nome.isEmpty()) { UiUtil.erro(campoNome, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.nomeValido(nome)) { UiUtil.erro(campoNome, R.string.erro_nome_curto); ok = false; }
        if (email.isEmpty()) { UiUtil.erro(campoEmail, R.string.erro_obrigatorio); ok = false; }
        else if (!Validador.emailValido(email)) { UiUtil.erro(campoEmail, R.string.erro_email_invalido); ok = false; }
        if (!Validador.telefoneValido(telefone)) { UiUtil.erro(campoTelefone, R.string.erro_telefone); ok = false; }
        if (!ok) { UiUtil.focarPrimeiroErro(campoNome, campoEmail, campoTelefone); return; }

        botaoSalvar.setEnabled(false);
        AppExecutors.disco().execute(() -> {
            UsuarioDao dao = AppDatabase.get(this).usuarioDao();
            boolean emUso = dao.contarEmailEmUso(email, usuarioId) > 0;
            if (!emUso) {
                usuario.nome = nome;
                usuario.email = email;
                usuario.telefone = telefone.isEmpty() ? null : telefone;
                usuario.cidade = cidade.isEmpty() ? null : cidade;
                dao.atualizar(usuario);
            }
            AppExecutors.principal(() -> {
                if (isFinishing()) return;
                botaoSalvar.setEnabled(true);
                if (emUso) {
                    UiUtil.erro(campoEmail, R.string.erro_email_em_uso);
                    UiUtil.focarPrimeiroErro(campoEmail);
                } else {
                    Toast.makeText(this, R.string.perfil_salvo, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }
}
