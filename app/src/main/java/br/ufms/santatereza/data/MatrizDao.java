package br.ufms.santatereza.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MatrizDao {

    @Insert
    void inserirTodas(List<Matriz> matrizes);

    @Query("SELECT COUNT(*) FROM matrizes")
    int contar();

    @Query("SELECT m.*, (f.matrizId IS NOT NULL) AS favorita FROM matrizes m "
            + "LEFT JOIN favoritos f ON f.matrizId = m.id AND f.usuarioId = :usuarioId "
            + "ORDER BY m.nome")
    LiveData<List<MatrizItem>> observarTodas(long usuarioId);

    @Query("SELECT m.*, (f.matrizId IS NOT NULL) AS favorita FROM matrizes m "
            + "LEFT JOIN favoritos f ON f.matrizId = m.id AND f.usuarioId = :usuarioId "
            + "WHERE m.id = :matrizId LIMIT 1")
    LiveData<MatrizItem> observar(long matrizId, long usuarioId);
}
