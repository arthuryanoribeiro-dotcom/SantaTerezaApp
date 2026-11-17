package br.ufms.santatereza.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Usuário do aplicativo. A senha NUNCA é guardada em texto puro:
 * o campo senhaHash armazena apenas o hash BCrypt (que já contém o salt).
 */
@Entity(tableName = "usuarios", indices = {@Index(value = "email", unique = true)})
public class Usuario {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nome = "";

    /** Sempre armazenado em minúsculas, sem espaços nas pontas. */
    @NonNull
    public String email = "";

    /** Hash BCrypt no formato $2a$custo$salt+hash. */
    @NonNull
    public String senhaHash = "";

    public String telefone;
    public String cidade;

    /** Caminho absoluto da foto de perfil no armazenamento interno (privado) do app. */
    public String fotoPath;

    public long criadoEm;
}
