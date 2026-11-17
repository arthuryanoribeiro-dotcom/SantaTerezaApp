package br.ufms.santatereza.ui.matrizes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.MatrizItem;
import br.ufms.santatereza.seguranca.SessionManager;

/** Aba Matrizes: catálogo com busca, filtro de favoritas e acesso à ficha de cada animal. */
public class MatrizesFragment extends Fragment implements MatrizAdapter.Acoes {

    private final List<MatrizItem> todas = new ArrayList<>();
    private MatrizAdapter adapter;
    private TextView textoVazio, textoContagem;
    private ChipGroup grupoFiltro;
    private String termo = "";
    private long usuarioId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matrizes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        usuarioId = SessionManager.get(requireContext()).getUsuarioId();
        textoVazio = v.findViewById(R.id.textoVazio);
        textoContagem = v.findViewById(R.id.textoContagem);
        grupoFiltro = v.findViewById(R.id.grupoFiltro);

        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MatrizAdapter(this);
        lista.setAdapter(adapter);

        TextInputLayout campoBusca = v.findViewById(R.id.campoBusca);
        if (campoBusca.getEditText() != null) {
            campoBusca.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    termo = normalizar(s.toString());
                    aplicarFiltro();
                }
            });
        }
        grupoFiltro.setOnCheckedStateChangeListener((g, ids) -> aplicarFiltro());

        AppDatabase.get(requireContext()).matrizDao().observarTodas(usuarioId)
                .observe(getViewLifecycleOwner(), itens -> {
                    todas.clear();
                    if (itens != null) todas.addAll(itens);
                    aplicarFiltro();
                });
    }

    /** Remove acentos e caixa para a busca encontrar "dalia" ao procurar "Dália". */
    private static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    private void aplicarFiltro() {
        boolean soFavoritas = grupoFiltro.getCheckedChipId() == R.id.chipFavoritas;
        List<MatrizItem> filtradas = new ArrayList<>();
        for (MatrizItem i : todas) {
            if (soFavoritas && !i.favorita) continue;
            if (!termo.isEmpty()
                    && !normalizar(i.matriz.nome).contains(termo)
                    && !normalizar(i.matriz.registro).contains(termo)) continue;
            filtradas.add(i);
        }
        adapter.submitList(filtradas);

        textoContagem.setText(getResources().getQuantityString(R.plurals.matrizes_contagem,
                filtradas.size(), filtradas.size()));
        boolean vazio = filtradas.isEmpty();
        textoVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
        textoVazio.setText(soFavoritas && termo.isEmpty()
                ? R.string.matrizes_vazio_favoritas : R.string.matrizes_vazio);
    }

    @Override
    public void abrir(MatrizItem item) {
        Intent i = new Intent(requireContext(), MatrizDetalheActivity.class)
                .putExtra(MatrizDetalheActivity.EXTRA_ID, item.matriz.id);
        startActivity(i);
    }

    @Override
    public void alternarFavorito(MatrizItem item) {
        boolean novo = !item.favorita;
        FavoritosRepo.definir(requireContext(), usuarioId, item.matriz.id, novo);
        String nome = item.matriz.nome;
        Snackbar.make(requireView(),
                        getString(novo ? R.string.adicionada_favoritas : R.string.removida_favoritas, nome),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.desfazer, b ->
                        FavoritosRepo.definir(requireContext(), usuarioId, item.matriz.id, !novo))
                .show();
    }
}
