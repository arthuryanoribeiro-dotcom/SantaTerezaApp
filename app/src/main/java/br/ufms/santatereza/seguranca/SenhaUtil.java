package br.ufms.santatereza.seguranca;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Hash de senhas com BCrypt.
 * - Cada hash recebe um salt aleatório de 16 bytes, gerado pela própria biblioteca.
 * - O salt e o custo ficam embutidos no texto do hash ($2a$10$...), por isso não é
 *   preciso guardar o salt separadamente.
 * - É uma função de mão única: não existe forma de "descriptografar" a senha.
 */
public final class SenhaUtil {

    /** Custo 10 = 2^10 iterações: seguro e ainda rápido o bastante para celulares. */
    private static final int CUSTO = 10;

    private SenhaUtil() { }

    public static String gerarHash(String senha) {
        return BCrypt.withDefaults().hashToString(CUSTO, senha.toCharArray());
    }

    public static boolean conferir(String senhaDigitada, String hashSalvo) {
        if (senhaDigitada == null || hashSalvo == null) return false;
        return BCrypt.verifyer().verify(senhaDigitada.toCharArray(), hashSalvo).verified;
    }
}
