package br.ufms.santatereza.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import br.ufms.santatereza.util.AppExecutors;

/**
 * Consumo de API HTTP (Open-Meteo, gratuita e sem chave): clima atual na fazenda.
 *
 * Economia de energia: a resposta fica em cache por 30 minutos; dentro desse período o
 * app não liga o rádio de rede de novo, mesmo que o usuário volte várias vezes à tela.
 */
public class ClimaService {

    public interface Callback {
        void sucesso(Clima clima);
        void erro(Exception e);
    }

    private static final long VALIDADE_CACHE_MS = 30 * 60 * 1000L;
    private static final String PREFS = "cache_clima";

    private final SharedPreferences cache;

    public ClimaService(Context context) {
        cache = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void buscar(double lat, double lon, boolean forcar, Callback cb) {
        if (!forcar) {
            Clima emCache = lerCache();
            if (emCache != null && System.currentTimeMillis() - emCache.obtidoEm < VALIDADE_CACHE_MS) {
                cb.sucesso(emCache);
                return;
            }
        }
        AppExecutors.rede().execute(() -> {
            HttpURLConnection con = null;
            try {
                String endereco = String.format(Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                                + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                                + "&timezone=America%%2FSao_Paulo", lat, lon);
                con = (HttpURLConnection) new URL(endereco).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                con.setRequestMethod("GET");
                int status = con.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP " + status);
                }
                String corpo = ler(con.getInputStream());
                Clima clima = interpretar(corpo);
                salvarCache(clima);
                AppExecutors.principal(() -> cb.sucesso(clima));
            } catch (Exception e) {
                AppExecutors.principal(() -> cb.erro(e));
            } finally {
                if (con != null) con.disconnect();
            }
        });
    }

    static Clima interpretar(String json) throws Exception {
        JSONObject atual = new JSONObject(json).getJSONObject("current");
        Clima c = new Clima();
        c.temperatura = atual.getDouble("temperature_2m");
        c.umidade = atual.getInt("relative_humidity_2m");
        c.vento = atual.getDouble("wind_speed_10m");
        c.codigo = atual.getInt("weather_code");
        c.obtidoEm = System.currentTimeMillis();
        return c;
    }

    private static String ler(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null) sb.append(linha);
        }
        return sb.toString();
    }

    private void salvarCache(Clima c) {
        cache.edit()
                .putFloat("temp", (float) c.temperatura)
                .putInt("umid", c.umidade)
                .putFloat("vento", (float) c.vento)
                .putInt("cod", c.codigo)
                .putLong("em", c.obtidoEm)
                .apply();
    }

    private Clima lerCache() {
        if (!cache.contains("em")) return null;
        Clima c = new Clima();
        c.temperatura = cache.getFloat("temp", 0);
        c.umidade = cache.getInt("umid", 0);
        c.vento = cache.getFloat("vento", 0);
        c.codigo = cache.getInt("cod", 0);
        c.obtidoEm = cache.getLong("em", 0);
        return c;
    }
}
