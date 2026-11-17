package br.ufms.santatereza.ui.agenda;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.Visita;
import br.ufms.santatereza.util.Formatos;

public class VisitaAdapter extends ListAdapter<Visita, VisitaAdapter.VH> {

    public interface Acoes {
        void excluir(Visita visita);
    }

    private final Acoes acoes;

    public VisitaAdapter(Acoes acoes) {
        super(new DiffUtil.ItemCallback<Visita>() {
            @Override
            public boolean areItemsTheSame(@NonNull Visita a, @NonNull Visita b) { return a.id == b.id; }

            @Override
            public boolean areContentsTheSame(@NonNull Visita a, @NonNull Visita b) {
                return a.dataHora == b.dataHora && a.motivo.equals(b.motivo)
                        && TextUtils.equals(a.observacao, b.observacao);
            }
        });
        this.acoes = acoes;
    }

    /** Texto amigável da antecedência do lembrete. */
    static String descreverAntecedencia(int minutos) {
        if (minutos == 0) return "no horário da visita";
        if (minutos < 60) return minutos + " minutos antes";
        if (minutos < 1440) return (minutos / 60) + (minutos == 60 ? " hora antes" : " horas antes");
        return (minutos / 1440) + (minutos == 1440 ? " dia antes" : " dias antes");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_visita, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Visita v = getItem(position);
        Context c = h.itemView.getContext();
        boolean futura = v.dataHora > System.currentTimeMillis();
        String quando = Formatos.dataHora(v.dataHora);
        String lembrete = "Lembrete " + descreverAntecedencia(v.antecedenciaMin);
        String status = c.getString(futura ? R.string.agenda_status_futura : R.string.agenda_status_passada);

        h.motivo.setText(v.motivo);
        h.dataHora.setText(quando);
        h.lembrete.setText(lembrete);
        h.lembrete.setVisibility(futura ? View.VISIBLE : View.GONE);
        h.obs.setText(v.observacao);
        h.obs.setVisibility(TextUtils.isEmpty(v.observacao) ? View.GONE : View.VISIBLE);
        h.status.setText(status);
        h.itemView.setAlpha(futura ? 1f : 0.75f);

        // Um único anúncio com todas as informações da visita
        String cd = c.getString(R.string.agenda_item_cd, v.motivo, quando, status,
                futura ? descreverAntecedencia(v.antecedenciaMin) : "não se aplica");
        if (!TextUtils.isEmpty(v.observacao)) cd += " Observações: " + v.observacao;
        h.grupoTexto.setContentDescription(cd);

        h.botaoExcluir.setContentDescription(c.getString(R.string.agenda_cd_excluir, quando));
        h.botaoExcluir.setOnClickListener(b -> acoes.excluir(v));
    }

    static class VH extends RecyclerView.ViewHolder {
        final View grupoTexto;
        final TextView motivo, dataHora, lembrete, obs;
        final Chip status;
        final MaterialButton botaoExcluir;

        VH(@NonNull View v) {
            super(v);
            grupoTexto = v.findViewById(R.id.grupoTexto);
            motivo = v.findViewById(R.id.motivo);
            dataHora = v.findViewById(R.id.dataHora);
            lembrete = v.findViewById(R.id.lembrete);
            obs = v.findViewById(R.id.obs);
            status = v.findViewById(R.id.status);
            botaoExcluir = v.findViewById(R.id.botaoExcluir);
        }
    }
}
