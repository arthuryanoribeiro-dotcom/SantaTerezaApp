package br.ufms.santatereza.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** Matriz (vaca reprodutora) do catálogo da fazenda. */
@Entity(tableName = "matrizes")
public class Matriz {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String nome = "";
    @NonNull public String registro = "";
    /** Data de nascimento no formato yyyy-MM-dd. */
    @NonNull public String nascimento = "";
    public int pesoKg;
    public String pai;
    public String mae;
    /** FIV, TE ou IA. */
    public String origem;
    public double iabcz;
    public int deca;
    public String descricao;

    public Matriz() { }

    @Ignore
    public Matriz(@NonNull String nome, @NonNull String registro, @NonNull String nascimento,
                  int pesoKg, String pai, String mae, String origem,
                  double iabcz, int deca, String descricao) {
        this.nome = nome;
        this.registro = registro;
        this.nascimento = nascimento;
        this.pesoKg = pesoKg;
        this.pai = pai;
        this.mae = mae;
        this.origem = origem;
        this.iabcz = iabcz;
        this.deca = deca;
        this.descricao = descricao;
    }
}
