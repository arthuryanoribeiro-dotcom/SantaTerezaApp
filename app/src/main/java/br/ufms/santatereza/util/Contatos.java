package br.ufms.santatereza.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import br.ufms.santatereza.R;

/** Dados de contato e localização da fazenda (fonte: fazendastatereza.com.br). */
public final class Contatos {

    public static final String TELEFONE = "+5564999818789";
    public static final String WHATSAPP = "5564999818789";
    public static final String EMAIL = "walneyyano@gmail.com";
    public static final String INSTAGRAM = "https://www.instagram.com/fazsantatereza";
    public static final String SITE = "https://fazendastatereza.com.br/";

    /*
     * Coordenadas da sede da Fazenda Santa Tereza: 16°20'40.4"S 50°17'45.6"W
     * (convertidas para graus decimais). CIDADE_* é o centro de São Luís de Montes Belos,
     * usado só como ponto de referência no mapa.
     */
    public static final double FAZENDA_LAT = -16.344556;
    public static final double FAZENDA_LON = -50.296000;
    public static final double CIDADE_LAT = -16.5253;
    public static final double CIDADE_LON = -50.3725;

    private Contatos() { }

    public static void abrir(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.sem_app_para_acao, Toast.LENGTH_LONG).show();
        }
    }

    public static void whatsapp(Context context, String mensagem) {
        Uri uri = Uri.parse("https://wa.me/" + WHATSAPP + "?text=" + Uri.encode(mensagem));
        abrir(context, new Intent(Intent.ACTION_VIEW, uri));
    }

    public static void ligar(Context context) {
        // ACTION_DIAL apenas abre o discador: não exige a permissão CALL_PHONE
        abrir(context, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + TELEFONE)));
    }

    public static void email(Context context) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + EMAIL))
                .putExtra(Intent.EXTRA_SUBJECT, "Contato pelo app Santa Tereza Nelore");
        abrir(context, i);
    }

    public static void link(Context context, String url) {
        abrir(context, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    /** Abre o app de mapas (Google Maps, Waze etc.) com a rota até a fazenda. */
    public static void comoChegar(Context context) {
        Uri geo = Uri.parse("geo:" + FAZENDA_LAT + "," + FAZENDA_LON + "?q="
                + FAZENDA_LAT + "," + FAZENDA_LON + "(" + Uri.encode("Fazenda Santa Tereza") + ")");
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (ActivityNotFoundException e) {
            // Sem app de mapas: abre no navegador
            link(context, "https://www.google.com/maps/dir/?api=1&destination="
                    + FAZENDA_LAT + "," + FAZENDA_LON);
        }
    }
}
