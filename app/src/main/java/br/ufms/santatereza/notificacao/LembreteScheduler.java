package br.ufms.santatereza.notificacao;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import br.ufms.santatereza.data.Visita;

/**
 * Agenda e cancela os alarmes dos lembretes de visita (recurso obrigatório: Alarmes).
 * Usa alarme exato quando permitido; caso contrário, cai para um alarme inexato que
 * o sistema agrupa com outros (melhor para a bateria).
 */
public final class LembreteScheduler {

    public static final String EXTRA_VISITA_ID = "visita_id";

    private LembreteScheduler() { }

    private static PendingIntent pendingIntent(Context context, long visitaId) {
        Intent intent = new Intent(context, LembreteReceiver.class)
                .putExtra(EXTRA_VISITA_ID, visitaId);
        return PendingIntent.getBroadcast(context, (int) visitaId, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static boolean podeAgendarExato(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return false;
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
    }

    /** @return true se foi agendado como alarme exato. */
    @SuppressLint("ScheduleExactAlarm") // verificado em podeAgendarExato()
    public static boolean agendar(Context context, Visita visita) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null || visita.lembreteEm <= System.currentTimeMillis()) return false;
        PendingIntent pi = pendingIntent(context, visita.id);
        if (podeAgendarExato(context)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, visita.lembreteEm, pi);
                return true;
            } catch (SecurityException e) {
                // permissão revogada no meio do caminho: usa o alarme inexato abaixo
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, visita.lembreteEm, pi);
        return false;
    }

    public static void cancelar(Context context, long visitaId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pendingIntent(context, visitaId));
    }
}
