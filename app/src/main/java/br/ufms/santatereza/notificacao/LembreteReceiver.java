package br.ufms.santatereza.notificacao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Visita;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.Formatos;

/** Recebe o disparo do alarme e mostra a notificação de lembrete da visita. */
public class LembreteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long visitaId = intent.getLongExtra(LembreteScheduler.EXTRA_VISITA_ID, -1);
        if (visitaId <= 0) return;
        final PendingResult resultado = goAsync();
        final Context app = context.getApplicationContext();
        AppExecutors.disco().execute(() -> {
            try {
                Visita v = AppDatabase.get(app).visitaDao().buscarPorId(visitaId);
                if (v != null) { // visita pode ter sido excluída
                    NotificationHelper.lembreteVisita(app, v.id, v.motivo,
                            Formatos.dataHora(v.dataHora));
                }
            } finally {
                resultado.finish();
            }
        });
    }
}
