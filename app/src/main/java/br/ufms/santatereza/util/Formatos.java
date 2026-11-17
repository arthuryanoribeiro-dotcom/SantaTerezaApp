package br.ufms.santatereza.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Formatação de datas no padrão brasileiro. */
public final class Formatos {

    private static final Locale BR = new Locale("pt", "BR");

    private Formatos() { }

    public static String dataHora(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", BR).format(new Date(millis));
    }

    public static String data(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", BR).format(new Date(millis));
    }

    public static String hora(long millis) {
        return new SimpleDateFormat("HH:mm", BR).format(new Date(millis));
    }

    /** Converte "yyyy-MM-dd" em "dd/MM/yyyy". */
    public static String dataIsoParaBr(String iso) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", BR).parse(iso);
            return d == null ? iso : data(d.getTime());
        } catch (Exception e) {
            return iso;
        }
    }

    /** Idade em meses completos a partir de uma data "yyyy-MM-dd". */
    public static int idadeEmMeses(String iso) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", BR).parse(iso);
            if (d == null) return 0;
            Calendar nasc = Calendar.getInstance();
            nasc.setTime(d);
            Calendar hoje = Calendar.getInstance();
            int meses = (hoje.get(Calendar.YEAR) - nasc.get(Calendar.YEAR)) * 12
                    + (hoje.get(Calendar.MONTH) - nasc.get(Calendar.MONTH));
            if (hoje.get(Calendar.DAY_OF_MONTH) < nasc.get(Calendar.DAY_OF_MONTH)) meses--;
            return Math.max(meses, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
