package br.ufms.santatereza.ui.matrizes;

import android.content.Context;

import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Favorito;
import br.ufms.santatereza.util.AppExecutors;

/** Marca/desmarca favoritos fora da thread principal. */
public final class FavoritosRepo {

    private FavoritosRepo() { }

    public static void definir(Context context, long usuarioId, long matrizId, boolean favorita) {
        Context app = context.getApplicationContext();
        AppExecutors.disco().execute(() -> {
            if (favorita) {
                AppDatabase.get(app).favoritoDao().inserir(new Favorito(usuarioId, matrizId));
            } else {
                AppDatabase.get(app).favoritoDao().remover(usuarioId, matrizId);
            }
        });
    }
}
