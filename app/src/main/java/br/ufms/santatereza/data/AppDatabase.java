package br.ufms.santatereza.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import br.ufms.santatereza.util.AppExecutors;

@Database(
        entities = {Usuario.class, Matriz.class, Favorito.class, Visita.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instancia;

    public abstract UsuarioDao usuarioDao();
    public abstract MatrizDao matrizDao();
    public abstract FavoritoDao favoritoDao();
    public abstract VisitaDao visitaDao();

    public static AppDatabase get(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "santa_tereza.db")
                            // Se o aparelho tiver um banco de versão mais nova, recria em vez de fechar o app
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .addCallback(new Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    // Popula o catálogo sempre que estiver vazio (primeira execução ou banco recriado)
                                    AppExecutors.disco().execute(() -> {
                                        MatrizDao dao = instancia.matrizDao();
                                        if (dao.contar() == 0) {
                                            dao.inserirTodas(DadosIniciais.matrizes());
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return instancia;
    }
}
