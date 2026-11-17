package br.ufms.santatereza.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executores compartilhados. Acesso ao banco, hash de senha e rede NUNCA rodam na
 * thread principal, para a interface não travar (usabilidade) e evitar ANR.
 */
public final class AppExecutors {

    private static final ExecutorService DISCO = Executors.newSingleThreadExecutor();
    private static final ExecutorService REDE = Executors.newFixedThreadPool(2);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private AppExecutors() { }

    public static ExecutorService disco() { return DISCO; }

    public static ExecutorService rede() { return REDE; }

    public static void principal(Runnable r) { PRINCIPAL.post(r); }
}
