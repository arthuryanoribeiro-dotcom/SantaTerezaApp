package br.ufms.santatereza.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long inserir(Usuario usuario);

    @Update
    int atualizar(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    Usuario buscarPorEmail(String email);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    Usuario buscarPorId(long id);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    LiveData<Usuario> observar(long id);

    @Query("SELECT COUNT(*) FROM usuarios WHERE email = :email AND id != :idIgnorado")
    int contarEmailEmUso(String email, long idIgnorado);

    @Query("UPDATE usuarios SET fotoPath = :caminho WHERE id = :id")
    void atualizarFoto(long id, String caminho);

    @Query("UPDATE usuarios SET senhaHash = :hash WHERE id = :id")
    void atualizarSenha(long id, String hash);
}
