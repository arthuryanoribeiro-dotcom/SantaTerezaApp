package br.ufms.santatereza.data;

import androidx.room.Entity;

/** Relação N:N entre usuário e matrizes favoritas. */
@Entity(tableName = "favoritos", primaryKeys = {"usuarioId", "matrizId"})
public class Favorito {
    public long usuarioId;
    public long matrizId;

    public Favorito(long usuarioId, long matrizId) {
        this.usuarioId = usuarioId;
        this.matrizId = matrizId;
    }
}
