package br.ufms.santatereza.ui.inicio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import br.ufms.santatereza.R;
import br.ufms.santatereza.api.Clima;
import br.ufms.santatereza.api.ClimaService;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.util.Contatos;
import br.ufms.santatereza.util.Formatos;
import br.ufms.santatereza.util.Rede;

/** Aba Início: saudação, clima na fazenda (API), história, leilões e contatos. */
public class InicioFragment extends Fragment {

    private View progressoClima, grupoClima;
    private TextView textoClimaResumo, textoClimaDetalhe, textoClimaErro;
    private ClimaService climaService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        TextView textoOla = v.findViewById(R.id.textoOla);
        long uid = SessionManager.get(requireContext()).getUsuarioId();
        AppDatabase.get(requireContext()).usuarioDao().observar(uid).observe(getViewLifecycleOwner(), u -> {
            if (u != null) textoOla.setText(getString(R.string.inicio_ola, u.nome.split(" ")[0]));
        });

        preencherTempo(v.findViewById(R.id.tempo2001), R.string.tempo_2001_ano, R.string.tempo_2001);
        preencherTempo(v.findViewById(R.id.tempo2011), R.string.tempo_2011_ano, R.string.tempo_2011);
        preencherTempo(v.findViewById(R.id.tempo2024), R.string.tempo_2024_ano, R.string.tempo_2024);

        v.findViewById(R.id.botaoWhatsapp).setOnClickListener(b ->
                Contatos.whatsapp(requireContext(), getString(R.string.whatsapp_padrao)));
        v.findViewById(R.id.botaoLigar).setOnClickListener(b -> Contatos.ligar(requireContext()));
        v.findViewById(R.id.botaoEmail).setOnClickListener(b -> Contatos.email(requireContext()));
        v.findViewById(R.id.botaoInstagram).setOnClickListener(b -> Contatos.link(requireContext(), Contatos.INSTAGRAM));
        v.findViewById(R.id.botaoSite).setOnClickListener(b -> Contatos.link(requireContext(), Contatos.SITE));

        progressoClima = v.findViewById(R.id.progressoClima);
        grupoClima = v.findViewById(R.id.grupoClima);
        textoClimaResumo = v.findViewById(R.id.textoClimaResumo);
        textoClimaDetalhe = v.findViewById(R.id.textoClimaDetalhe);
        textoClimaErro = v.findViewById(R.id.textoClimaErro);
        climaService = new ClimaService(requireContext());
        v.findViewById(R.id.botaoAtualizarClima).setOnClickListener(b -> carregarClima(true));
        carregarClima(false);
    }

    private void preencherTempo(View item, @StringRes int ano, @StringRes int texto) {
        ((TextView) item.findViewById(R.id.ano)).setText(ano);
        ((TextView) item.findViewById(R.id.descricao)).setText(texto);
        item.setContentDescription(getString(ano) + ": " + getString(texto));
    }

    private void carregarClima(boolean forcar) {
        if (forcar && !Rede.conectado(requireContext())) {
            mostrarErroClima(R.string.clima_offline);
            return;
        }
        progressoClima.setVisibility(View.VISIBLE);
        textoClimaErro.setVisibility(View.GONE);
        climaService.buscar(Contatos.FAZENDA_LAT, Contatos.FAZENDA_LON, forcar, new ClimaService.Callback() {
            @Override
            public void sucesso(Clima c) {
                if (!isAdded()) return;
                progressoClima.setVisibility(View.GONE);
                grupoClima.setVisibility(View.VISIBLE);
                textoClimaResumo.setText(getString(R.string.clima_resumo, c.descricao(), c.temperatura));
                textoClimaDetalhe.setText(getString(R.string.clima_detalhe, c.umidade, c.vento,
                        Formatos.hora(c.obtidoEm)));
                grupoClima.setContentDescription(getString(R.string.clima_cd, c.descricao(),
                        c.temperatura, c.umidade, c.vento));
            }

            @Override
            public void erro(Exception e) {
                if (!isAdded()) return;
                mostrarErroClima(Rede.conectado(requireContext())
                        ? R.string.clima_erro : R.string.clima_offline);
            }
        });
    }

    private void mostrarErroClima(@StringRes int msg) {
        progressoClima.setVisibility(View.GONE);
        textoClimaErro.setVisibility(View.VISIBLE);
        textoClimaErro.setText(msg);
    }
}
