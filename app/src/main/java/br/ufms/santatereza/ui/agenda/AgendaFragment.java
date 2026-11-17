package br.ufms.santatereza.ui.agenda;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Visita;
import br.ufms.santatereza.notificacao.LembreteScheduler;
import br.ufms.santatereza.notificacao.NotificationHelper;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.MainActivity;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.UiUtil;

/** Aba Agenda: visitas agendadas com lembrete por alarme e notificação. */
public class AgendaFragment extends Fragment implements VisitaAdapter.Acoes {

    private VisitaAdapter adapter;
    private View textoVazio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Resultado do diálogo "Agendar visita"
        getChildFragmentManager().setFragmentResultListener(NovaVisitaDialog.RESULTADO, this, (k, r) -> {
            if (getView() == null) return;
            Snackbar.make(getView(), getString(R.string.agenda_salva, r.getString(NovaVisitaDialog.R_QUANDO)),
                    Snackbar.LENGTH_LONG).show();
            if (!NotificationHelper.podeNotificar(requireContext())) {
                avisarNotificacaoDesativada();
            } else if (!r.getBoolean(NovaVisitaDialog.R_EXATO)) {
                Snackbar.make(getView(), R.string.agenda_alarme_inexato, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        textoVazio = v.findViewById(R.id.textoVazio);
        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VisitaAdapter(this);
        lista.setAdapter(adapter);

        ExtendedFloatingActionButton fab = v.findViewById(R.id.botaoNova);
        fab.setOnClickListener(b -> abrirNovaVisita(-1, null));
        // Encolhe o botão ao rolar para não cobrir o conteúdo
        lista.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) fab.shrink(); else if (dy < 0) fab.extend();
            }
        });

        long uid = SessionManager.get(requireContext()).getUsuarioId();
        AppDatabase.get(requireContext()).visitaDao().observarDoUsuario(uid)
                .observe(getViewLifecycleOwner(), visitas -> {
                    adapter.submitList(visitas);
                    textoVazio.setVisibility(visitas == null || visitas.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    /** Abre o formulário; pode vir pré-preenchido (ex.: a partir da ficha de uma matriz). */
    public void abrirNovaVisita(int motivoIdx, @Nullable String obs) {
        if (getChildFragmentManager().findFragmentByTag(NovaVisitaDialog.TAG) != null) return;
        NovaVisitaDialog.nova(motivoIdx, obs).show(getChildFragmentManager(), NovaVisitaDialog.TAG);
    }

    private void avisarNotificacaoDesativada() {
        if (getView() == null) return;
        Snackbar.make(getView(), R.string.agenda_sem_notificacao, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.abrir_configuracoes, b -> {
                    boolean podePedir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
                    if (podePedir && getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).pedirPermissaoNotificacao();
                    } else {
                        UiUtil.abrirConfiguracoesDoApp(requireContext());
                    }
                })
                .show();
    }

    @Override
    public void excluir(Visita visita) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.agenda_excluir_titulo)
                .setMessage(R.string.agenda_excluir_msg)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.agenda_excluir, (d, w) -> {
                    android.content.Context app = requireContext().getApplicationContext();
                    LembreteScheduler.cancelar(app, visita.id);
                    AppExecutors.disco().execute(() -> AppDatabase.get(app).visitaDao().excluir(visita));
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.agenda_excluida, Snackbar.LENGTH_LONG)
                                .setAction(R.string.desfazer, b -> AppExecutors.disco().execute(() -> {
                                    AppDatabase.get(app).visitaDao().inserir(visita);
                                    LembreteScheduler.agendar(app, visita);
                                }))
                                .show();
                    }
                })
                .show();
    }
}
