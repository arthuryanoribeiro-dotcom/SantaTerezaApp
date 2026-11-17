package br.ufms.santatereza.notificacao;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import br.ufms.santatereza.R;
import br.ufms.santatereza.ui.SplashActivity;

/** Cria os canais e emite as notificações, sempre verificando a permissão antes. */
public final class NotificationHelper {

    public static final String CANAL_LEMBRETES = "lembretes_visita";
    public static final String CANAL_CONTA = "conta_seguranca";

    private static final int ID_BOAS_VINDAS = 1;
    private static final int ID_SENHA = 2;

    private NotificationHelper() { }

    public static void criarCanais(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel lembretes = new NotificationChannel(CANAL_LEMBRETES,
                context.getString(R.string.canal_lembretes), NotificationManager.IMPORTANCE_HIGH);
        lembretes.setDescription(context.getString(R.string.canal_lembretes_desc));

        NotificationChannel conta = new NotificationChannel(CANAL_CONTA,
                context.getString(R.string.canal_conta), NotificationManager.IMPORTANCE_DEFAULT);
        conta.setDescription(context.getString(R.string.canal_conta_desc));

        nm.createNotificationChannel(lembretes);
        nm.createNotificationChannel(conta);
    }

    /** True se o app pode exibir notificações (permissão concedida e não bloqueadas pelo usuário). */
    public static boolean podeNotificar(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static PendingIntent abrirApp(Context context) {
        Intent intent = new Intent(context, SplashActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @SuppressLint("MissingPermission") // verificada em podeNotificar()
    private static void emitir(Context context, int id, String canal, String titulo, String texto) {
        if (!podeNotificar(context)) return; // Sem permissão: não tenta notificar (evita SecurityException)
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(abrirApp(context))
                .setAutoCancel(true)
                .setPriority(CANAL_LEMBRETES.equals(canal)
                        ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException ignorada) {
            // Permissão revogada entre a verificação e o envio: apenas ignora.
        }
    }

    public static void lembreteVisita(Context context, long visitaId, String motivo, String quando) {
        emitir(context, (int) (1000 + visitaId), CANAL_LEMBRETES,
                context.getString(R.string.notif_lembrete_titulo),
                context.getString(R.string.notif_lembrete_texto, motivo, quando));
    }

    public static void boasVindas(Context context, String nome) {
        emitir(context, ID_BOAS_VINDAS, CANAL_CONTA,
                context.getString(R.string.notif_boas_vindas_titulo),
                context.getString(R.string.notif_boas_vindas_texto, nome));
    }

    public static void senhaAlterada(Context context) {
        emitir(context, ID_SENHA, CANAL_CONTA,
                context.getString(R.string.notif_senha_titulo),
                context.getString(R.string.notif_senha_texto));
    }
}
