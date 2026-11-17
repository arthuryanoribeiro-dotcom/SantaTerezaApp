package br.ufms.santatereza.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.StringRes;

import com.google.android.material.textfield.TextInputLayout;

/** Pequenos utilitários de interface usados em vários formulários. */
public final class UiUtil {

    private UiUtil() { }

    /** Texto digitado no campo, sem espaços nas pontas. */
    public static String texto(TextInputLayout campo) {
        if (campo.getEditText() == null || campo.getEditText().getText() == null) return "";
        return campo.getEditText().getText().toString().trim();
    }

    /** Senha: não remove espaços, pois podem fazer parte dela. */
    public static String senha(TextInputLayout campo) {
        if (campo.getEditText() == null || campo.getEditText().getText() == null) return "";
        return campo.getEditText().getText().toString();
    }

    public static void definirTexto(TextInputLayout campo, String valor) {
        if (campo.getEditText() != null) campo.getEditText().setText(valor == null ? "" : valor);
    }

    /** Remove a mensagem de erro assim que o usuário volta a digitar. */
    public static void limparErroAoDigitar(TextInputLayout... campos) {
        for (TextInputLayout campo : campos) {
            if (campo.getEditText() == null) continue;
            campo.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    campo.setError(null);
                    campo.setErrorEnabled(false);
                }
            });
        }
    }

    public static void erro(TextInputLayout campo, @StringRes int mensagem) {
        campo.setErrorEnabled(true);
        campo.setError(campo.getContext().getString(mensagem));
    }

    /** Leva o foco ao primeiro campo com erro e o TalkBack lê a mensagem. */
    public static void focarPrimeiroErro(TextInputLayout... campos) {
        for (TextInputLayout c : campos) {
            if (c.getError() != null) {
                if (c.getEditText() != null) c.getEditText().requestFocus();
                c.announceForAccessibility(c.getError());
                return;
            }
        }
    }

    public static void esconderTeclado(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /** Abre a tela de configurações do app (para o usuário reativar uma permissão negada). */
    public static void abrirConfiguracoesDoApp(Context context) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
