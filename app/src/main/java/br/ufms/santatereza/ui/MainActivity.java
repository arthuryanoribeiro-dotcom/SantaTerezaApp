package br.ufms.santatereza.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import br.ufms.santatereza.R;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.agenda.AgendaFragment;
import br.ufms.santatereza.ui.inicio.InicioFragment;
import br.ufms.santatereza.ui.mapa.MapaFragment;
import br.ufms.santatereza.ui.matrizes.MatrizesFragment;
import br.ufms.santatereza.ui.perfil.PerfilFragment;

/**
 * Tela principal com navegação inferior (5 abas). Os fragmentos são mantidos em memória
 * com show/hide, preservando o estado de cada aba (rolagem, busca, mapa).
 */
public class MainActivity extends BaseAutenticadaActivity {

    public static final String EXTRA_ABA = "aba";
    public static final String EXTRA_NOVA_VISITA_OBS = "nova_visita_obs";

    private static final String ESTADO_ABA = "aba_atual";

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private int abaAtual = R.id.nav_inicio;

    private final ActivityResultLauncher<String> pedirNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (!concedida) {
                    Snackbar.make(findViewById(R.id.container), R.string.notif_negada, Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!garantirSessao()) return;
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            mostrarAba(item.getItemId());
            return true;
        });
        bottomNav.setOnItemReselectedListener(item -> { }); // tocar de novo na aba atual não recarrega

        if (savedInstanceState != null) {
            abaAtual = savedInstanceState.getInt(ESTADO_ABA, R.id.nav_inicio);
        }
        bottomNav.setSelectedItemId(abaAtual);
        mostrarAba(abaAtual);

        tratarIntent(getIntent());
        talvezPedirNotificacao();
        validarUsuarioDaSessao();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        tratarIntent(intent);
    }

    /** Permite abrir uma aba específica a partir de outra tela (ex.: "Agendar visita"). */
    private void tratarIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_ABA)) return;
        int aba = intent.getIntExtra(EXTRA_ABA, R.id.nav_inicio);
        bottomNav.setSelectedItemId(aba);
        String obs = intent.getStringExtra(EXTRA_NOVA_VISITA_OBS);
        if (aba == R.id.nav_agenda && obs != null) {
            // Aguarda a tela voltar ao primeiro plano antes de abrir o formulário
            bottomNav.post(() -> {
                Fragment f = getSupportFragmentManager().findFragmentByTag(tag(R.id.nav_agenda));
                if (f instanceof AgendaFragment && !isFinishing()) ((AgendaFragment) f).abrirNovaVisita(0, obs);
            });
        }
        intent.removeExtra(EXTRA_ABA);
    }

    private String tag(@IdRes int id) {
        return "aba_" + id;
    }

    private Fragment criar(@IdRes int id) {
        if (id == R.id.nav_matrizes) return new MatrizesFragment();
        if (id == R.id.nav_agenda) return new AgendaFragment();
        if (id == R.id.nav_mapa) return new MapaFragment();
        if (id == R.id.nav_perfil) return new PerfilFragment();
        return new InicioFragment();
    }

    private void mostrarAba(@IdRes int id) {
        abaAtual = id;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        for (Fragment f : fm.getFragments()) {
            if (f.getTag() != null && f.getTag().startsWith("aba_") && !f.isHidden()) tx.hide(f);
        }
        Fragment alvo = fm.findFragmentByTag(tag(id));
        if (alvo == null) {
            tx.add(R.id.container, criar(id), tag(id));
        } else {
            tx.show(alvo);
        }
        tx.commitNow();

        toolbar.setTitle(bottomNav.getMenu().findItem(id).getTitle());
        // Avisa o leitor de tela sobre a troca de seção
        View container = findViewById(R.id.container);
        container.announceForAccessibility(toolbar.getTitle());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_ABA, abaAtual);
    }

    /** Android 13+: pede a permissão de notificações uma única vez, explicando o motivo antes. */
    private void talvezPedirNotificacao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        SessionManager sessao = SessionManager.get(this);
        if (sessao.jaPerguntouNotificacao()) return;
        sessao.marcarNotificacaoPerguntada();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permissao_titulo)
                .setMessage(R.string.notif_rationale)
                .setPositiveButton(R.string.permitir, (d, w) ->
                        pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton(R.string.agora_nao, (d, w) ->
                        Snackbar.make(findViewById(R.id.container), R.string.notif_negada, Snackbar.LENGTH_LONG).show())
                .show();
    }

    /**
     * Se o usuário da sessão não existe mais no banco (ex.: banco recriado), encerra a
     * sessão e volta ao login, em vez de mostrar telas sem dados.
     */
    private void validarUsuarioDaSessao() {
        br.ufms.santatereza.util.AppExecutors.disco().execute(() -> {
            boolean existe = br.ufms.santatereza.data.AppDatabase.get(this)
                    .usuarioDao().buscarPorId(usuarioId) != null;
            if (!existe) br.ufms.santatereza.util.AppExecutors.principal(this::fazerLogout);
        });
    }

    /** Usado pelos fragmentos para pedir a permissão de notificação novamente. */
    public void pedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
