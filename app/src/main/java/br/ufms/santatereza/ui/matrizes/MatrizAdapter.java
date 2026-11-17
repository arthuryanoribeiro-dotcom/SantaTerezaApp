package br.ufms.santatereza.ui.matrizes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.Matriz;
import br.ufms.santatereza.data.MatrizItem;
import br.ufms.santatereza.util.Formatos;

public class MatrizAdapter extends ListAdapter<MatrizItem, MatrizAdapter.VH> {

    public interface Acoes {
        void abrir(MatrizItem item);
        void alternarFavorito(MatrizItem item);
    }

    private final Acoes acoes;

    public MatrizAdapter(Acoes acoes) {
        super(DIFF);
        this.acoes = acoes;
    }

    private static final DiffUtil.ItemCallback<MatrizItem> DIFF = new DiffUtil.ItemCallback<MatrizItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull MatrizItem a, @NonNull MatrizItem b) {
            return a.matriz.id == b.matriz.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull MatrizItem a, @NonNull MatrizItem b) {
            return a.favorita == b.favorita && a.matriz.nome.equals(b.matriz.nome)
                    && a.matriz.pesoKg == b.matriz.pesoKg;
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_matriz, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MatrizItem item = getItem(position);
        Matriz m = item.matriz;
        Context c = h.itemView.getContext();
        int meses = Formatos.idadeEmMeses(m.nascimento);

        h.nome.setText(m.nome);
        h.registro.setText(c.getString(R.string.detalhe_registro) + ": " + m.registro + " · iABCZ " + m.iabcz);
        h.resumo.setText(c.getString(R.string.matriz_resumo, m.origem, meses, m.pesoKg));

        // Descrição única do cartão para o TalkBack (em vez de ler cada texto separado)
        h.cartao.setContentDescription(c.getString(R.string.matriz_cd_item, m.nome, m.registro, meses,
                m.pesoKg, c.getString(item.favorita ? R.string.favorita : R.string.nao_favorita)));
        h.cartao.setOnClickListener(v -> acoes.abrir(item));

        h.botaoFavorito.setIconResource(item.favorita ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        h.botaoFavorito.setContentDescription(c.getString(
                item.favorita ? R.string.cd_desfavoritar : R.string.cd_favoritar, m.nome));
        h.botaoFavorito.setOnClickListener(v -> acoes.alternarFavorito(item));
    }

    static class VH extends RecyclerView.ViewHolder {
        final View cartao;
        final TextView nome, registro, resumo;
        final MaterialButton botaoFavorito;

        VH(@NonNull View v) {
            super(v);
            cartao = v.findViewById(R.id.cartao);
            nome = v.findViewById(R.id.nome);
            registro = v.findViewById(R.id.registro);
            resumo = v.findViewById(R.id.resumo);
            botaoFavorito = v.findViewById(R.id.botaoFavorito);
            // Os textos internos já estão na descrição do cartão
            v.findViewById(R.id.grupoTexto).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
    }
}
