package br.ufms.santatereza.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VisitaDao {

    @Insert
    long inserir(Visita visita);

    @Delete
    void excluir(Visita visita);

    @Query("SELECT * FROM visitas WHERE usuarioId = :usuarioId ORDER BY dataHora DESC")
    LiveData<List<Visita>> observarDoUsuario(long usuarioId);

    @Query("SELECT * FROM visitas WHERE id = :id LIMIT 1")
    Visita buscarPorId(long id);

    /** Usado pelo BootReceiver para reagendar alarmes depois de reiniciar o aparelho. */
    @Query("SELECT * FROM visitas WHERE lembreteEm > :agora")
    List<Visita> listarComLembreteFuturo(long agora);
}
