package br.ufms.santatereza.notificacao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Visita;
import br.ufms.santatereza.util.AppExecutors;

/** Alarmes são apagados quando o aparelho reinicia; aqui eles são reagendados. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final PendingResult resultado = goAsync();
        final Context app = context.getApplicationContext();
        AppExecutors.disco().execute(() -> {
            try {
                List<Visita> visitas = AppDatabase.get(app).visitaDao()
                        .listarComLembreteFuturo(System.currentTimeMillis());
                for (Visita v : visitas) LembreteScheduler.agendar(app, v);
            } finally {
                resultado.finish();
            }
        });
    }
}
