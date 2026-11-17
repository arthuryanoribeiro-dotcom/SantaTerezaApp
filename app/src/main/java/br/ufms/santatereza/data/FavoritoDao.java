package br.ufms.santatereza.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface FavoritoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void inserir(Favorito favorito);

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId AND matrizId = :matrizId")
    void remover(long usuarioId, long matrizId);
}
