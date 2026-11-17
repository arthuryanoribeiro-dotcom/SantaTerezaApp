package br.ufms.santatereza.ui.matrizes;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Matriz;
import br.ufms.santatereza.data.MatrizItem;
import br.ufms.santatereza.ui.BaseAutenticadaActivity;
import br.ufms.santatereza.ui.MainActivity;
import br.ufms.santatereza.util.Contatos;
import br.ufms.santatereza.util.Formatos;

/** Ficha completa da matriz, com leitura em voz alta, favoritar e contato de interesse. */
public class MatrizDetalheActivity extends BaseAutenticadaActivity {

    public static final String EXTRA_ID = "matriz_id";

    private MatrizItem atual;
    private TextToSpeech tts;
    private boolean ttsPronto = false;
    private boolean lendo = false;
    private MaterialButton botaoOuvir, botaoFavorito;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!garantirSessao()) return;
        setContentView(R.layout.activity_matriz_detalhe);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        botaoOuvir = findViewById(R.id.botaoOuvir);
        botaoFavorito = findViewById(R.id.botaoFavorito);

        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        AppDatabase.get(this).matrizDao().observar(id, usuarioId).observe(this, item -> {
            if (item == null) { finish(); return; }
            atual = item;
            preencher(item, toolbar);
        });

        botaoFavorito.setOnClickListener(v -> {
            if (atual == null) return;
            boolean novo = !atual.favorita;
            FavoritosRepo.definir(this, usuarioId, atual.matriz.id, novo);
            v.announceForAccessibility(getString(novo ? R.string.adicionada_favoritas
                    : R.string.removida_favoritas, atual.matriz.nome));
        });

        findViewById(R.id.botaoInteresse).setOnClickListener(v -> {
            if (atual == null) return;
            Contatos.whatsapp(this, getString(R.string.whatsapp_interesse,
                    atual.matriz.nome, atual.matriz.registro));
        });

        findViewById(R.id.botaoAgendar).setOnClickListener(v -> {
            if (atual == null) return;
            Intent i = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_ABA, R.id.nav_agenda)
                    .putExtra(MainActivity.EXTRA_NOVA_VISITA_OBS,
                            "Ver a matriz " + atual.matriz.nome + " (" + atual.matriz.registro + ")");
            startActivity(i);
            finish();
        });

        // Leitura em voz alta (acessibilidade para baixa visão ou pouca familiaridade com leitura)
        botaoOuvir.setEnabled(false);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("pt", "BR"));
                ttsPronto = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
            }
            runOnUiThread(() -> botaoOuvir.setEnabled(true));
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) { }
            @Override public void onDone(String s) { runOnUiThread(() -> atualizarBotaoOuvir(false)); }
            @Override public void onError(String s) { runOnUiThread(() -> atualizarBotaoOuvir(false)); }
        });
        botaoOuvir.setOnClickListener(v -> alternarLeitura());
    }

    private void preencher(MatrizItem item, MaterialToolbar toolbar) {
        Matriz m = item.matriz;
        toolbar.setTitle(m.nome);
        ((TextView) findViewById(R.id.nome)).setText(m.nome);
        ((TextView) findViewById(R.id.registro)).setText(getString(R.string.detalhe_registro) + ": " + m.registro);
        ((TextView) findViewById(R.id.descricao)).setText(m.descricao);

        LinearLayout tabela = findViewById(R.id.tabelaDados);
        tabela.removeAllViews();
        int meses = Formatos.idadeEmMeses(m.nascimento);
        adicionarLinha(tabela, R.string.detalhe_nascimento, Formatos.dataIsoParaBr(m.nascimento) + " (" + meses + " meses)");
        adicionarLinha(tabela, R.string.detalhe_peso, m.pesoKg + " kg");
        adicionarLinha(tabela, R.string.detalhe_origem, descreverOrigem(m.origem));
        adicionarLinha(tabela, R.string.detalhe_pai, m.pai);
        adicionarLinha(tabela, R.string.detalhe_mae, m.mae);
        adicionarLinha(tabela, R.string.detalhe_iabcz, String.valueOf(m.iabcz));
        adicionarLinha(tabela, R.string.detalhe_deca, m.deca + " (quanto menor, melhor)");

        botaoFavorito.setText(item.favorita ? R.string.favorita : R.string.nao_favorita);
        botaoFavorito.setIconResource(item.favorita ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        botaoFavorito.setContentDescription(getString(
                item.favorita ? R.string.cd_desfavoritar : R.string.cd_favoritar, m.nome));
    }

    private String descreverOrigem(String sigla) {
        if ("FIV".equals(sigla)) return "FIV – fertilização in vitro";
        if ("TE".equals(sigla)) return "TE – transferência de embrião";
        if ("IA".equals(sigla)) return "IA – inseminação artificial";
        return sigla;
    }

    private void adicionarLinha(LinearLayout tabela, int rotulo, String valor) {
        View linha = LayoutInflater.from(this).inflate(R.layout.item_dado, tabela, false);
        ((TextView) linha.findViewById(R.id.rotulo)).setText(rotulo);
        ((TextView) linha.findViewById(R.id.valor)).setText(valor);
        linha.setContentDescription(getString(rotulo) + ": " + valor);
        tabela.addView(linha);
    }

    private void alternarLeitura() {
        if (!ttsPronto) {
            Toast.makeText(this, R.string.tts_indisponivel, Toast.LENGTH_LONG).show();
            return;
        }
        if (lendo) {
            tts.stop();
            atualizarBotaoOuvir(false);
            return;
        }
        if (atual == null) return;
        Matriz m = atual.matriz;
        String texto = "Matriz " + m.nome + ". Registro " + m.registro + ". Peso " + m.pesoKg
                + " quilos. Origem: " + descreverOrigem(m.origem) + ". " + m.descricao;
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "matriz_" + m.id);
        atualizarBotaoOuvir(true);
    }

    private void atualizarBotaoOuvir(boolean lendoAgora) {
        lendo = lendoAgora;
        botaoOuvir.setText(lendoAgora ? R.string.detalhe_parar : R.string.detalhe_ouvir);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tts != null && lendo) {
            tts.stop();
            atualizarBotaoOuvir(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown(); // libera o mecanismo de voz (economia de recursos)
        super.onDestroy();
    }
}
