package br.ufms.santatereza.data;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Resultado de consulta: matriz + indicação se é favorita do usuário logado. */
public class MatrizItem {

    @Embedded
    public Matriz matriz;

    @ColumnInfo(name = "favorita")
    public boolean favorita;
}
