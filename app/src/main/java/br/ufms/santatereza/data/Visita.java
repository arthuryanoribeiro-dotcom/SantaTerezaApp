package br.ufms.santatereza.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Visita agendada à fazenda, com lembrete por alarme + notificação. */
@Entity(tableName = "visitas", indices = {@Index("usuarioId")})
public class Visita {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long usuarioId;

    @NonNull
    public String motivo = "";

    public String observacao;

    /** Data e hora da visita (epoch em milissegundos). */
    public long dataHora;

    /** Antecedência do lembrete, em minutos (0 = no horário). */
    public int antecedenciaMin;

    /** Momento exato em que o alarme deve disparar. */
    public long lembreteEm;
}
