package br.ufms.santatereza.util;

import android.text.TextUtils;
import android.util.Patterns;

/** Regras de validação dos formulários, centralizadas para manter mensagens consistentes. */
public final class Validador {

    private Validador() { }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static boolean emailValido(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean nomeValido(String nome) {
        return nome != null && nome.trim().length() >= 3;
    }

    /** Mínimo de 8 caracteres, contendo ao menos uma letra e um número. */
    public static boolean senhaForte(String senha) {
        if (senha == null || senha.length() < 8) return false;
        boolean temLetra = false, temNumero = false;
        for (char c : senha.toCharArray()) {
            if (Character.isLetter(c)) temLetra = true;
            if (Character.isDigit(c)) temNumero = true;
        }
        return temLetra && temNumero;
    }

    /** Telefone é opcional; se informado, deve ter 10 ou 11 dígitos (DDD + número). */
    public static boolean telefoneValido(String telefone) {
        if (TextUtils.isEmpty(telefone)) return true;
        String digitos = telefone.replaceAll("\\D", "");
        return digitos.length() == 10 || digitos.length() == 11;
    }

    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
