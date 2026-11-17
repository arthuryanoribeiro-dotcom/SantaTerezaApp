package br.ufms.santatereza.ui.agenda;

import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Visita;
import br.ufms.santatereza.notificacao.LembreteScheduler;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.Formatos;
import br.ufms.santatereza.util.UiUtil;

/** Formulário para agendar uma visita à fazenda e programar o lembrete (alarme). */
public class NovaVisitaDialog extends DialogFragment {

    public static final String TAG = "nova_visita";
    public static final String RESULTADO = "visita_salva";
    public static final String R_QUANDO = "quando";
    public static final String R_EXATO = "exato";

    private static final String ARG_MOTIVO = "motivo";
    private static final String ARG_OBS = "obs";
    private static final int[] ANTECEDENCIAS_MIN = {0, 15, 60, 1440};

    private TextInputLayout campoMotivo;
    private AutoCompleteTextView editMotivo, editAntecedencia;
    private TextInputLayout campoObs;
    private MaterialButton botaoData, botaoHora;
    private TextView textoErroData;

    // Data e hora escolhidas (-1 = ainda não escolhidas)
    private long dataUtcMillis = -1;
    private int hora = -1, minuto = -1;
    private int antecedenciaIdx = 0;

    public static NovaVisitaDialog nova(int motivoIdx, @Nullable String obs) {
        NovaVisitaDialog d = new NovaVisitaDialog();
        Bundle b = new Bundle();
        b.putInt(ARG_MOTIVO, motivoIdx);
        b.putString(ARG_OBS, obs);
        d.setArguments(b);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nova_visita, null);
        campoMotivo = v.findViewById(R.id.campoMotivo);
        editMotivo = v.findViewById(R.id.editMotivo);
        editAntecedencia = v.findViewById(R.id.editAntecedencia);
        campoObs = v.findViewById(R.id.campoObs);
        botaoData = v.findViewById(R.id.botaoData);
        botaoHora = v.findViewById(R.id.botaoHora);
        textoErroData = v.findViewById(R.id.textoErroData);

        String[] motivos = getResources().getStringArray(R.array.motivos_visita);
        String[] antecedencias = getResources().getStringArray(R.array.antecedencias);
        editMotivo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, motivos));
        editAntecedencia.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, antecedencias));
        editAntecedencia.setOnItemClickListener((p, view, pos, id) -> antecedenciaIdx = pos);
        editMotivo.setOnItemClickListener((p, view, pos, id) -> campoMotivo.setError(null));

        if (savedInstanceState != null) {
            dataUtcMillis = savedInstanceState.getLong("data", -1);
            hora = savedInstanceState.getInt("hora", -1);
            minuto = savedInstanceState.getInt("minuto", -1);
            antecedenciaIdx = savedInstanceState.getInt("antecedencia", 0);
            if (dataUtcMillis > 0) botaoData.setText(textoData(dataUtcMillis));
            if (hora >= 0) botaoHora.setText(textoHora());
        }

        Bundle args = getArguments();
        if (args != null && savedInstanceState == null) {
            int idx = args.getInt(ARG_MOTIVO, -1);
            if (idx >= 0 && idx < motivos.length) editMotivo.setText(motivos[idx], false);
            UiUtil.definirTexto(campoObs, args.getString(ARG_OBS));
        }

        editAntecedencia.setText(antecedencias[antecedenciaIdx], false);
        botaoData.setOnClickListener(b -> escolherData());
        botaoHora.setOnClickListener(b -> escolherHora());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.agenda_nova)
                .setView(v)
                .setPositiveButton(R.string.salvar, null) // listener definido no onShow para validar antes de fechar
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> salvar()));
        return dialog;
    }

    private void escolherData() {
        CalendarConstraints restricoes = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now()) // não permite datas passadas
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.agenda_data)
                .setSelection(dataUtcMillis > 0 ? dataUtcMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(restricoes)
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            dataUtcMillis = sel;
            botaoData.setText(textoData(sel));
            textoErroData.setVisibility(View.GONE);
        });
        picker.show(getChildFragmentManager(), "data");
    }

    private void escolherHora() {
        Calendar agora = Calendar.getInstance();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(requireContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(hora >= 0 ? hora : agora.get(Calendar.HOUR_OF_DAY))
                .setMinute(minuto >= 0 ? minuto : agora.get(Calendar.MINUTE))
                .setTitleText(R.string.agenda_hora)
                .build();
        picker.addOnPositiveButtonClickListener(b -> {
            hora = picker.getHour();
            minuto = picker.getMinute();
            botaoHora.setText(textoHora());
            textoErroData.setVisibility(View.GONE);
        });
        picker.show(getChildFragmentManager(), "hora");
    }

    private static String textoData(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        return String.format(Locale.ROOT, "Data: %02d/%02d/%04d",
                utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.MONTH) + 1, utc.get(Calendar.YEAR));
    }

    private String textoHora() {
        return String.format(Locale.ROOT, "Horário: %02d:%02d", hora, minuto);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("data", dataUtcMillis);
        out.putInt("hora", hora);
        out.putInt("minuto", minuto);
        out.putInt("antecedencia", antecedenciaIdx);
    }

    /** Junta a data (vinda em UTC do DatePicker) com a hora local escolhida. */
    private long montarDataHora() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(dataUtcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hora, minuto, 0);
        return local.getTimeInMillis();
    }

    private void mostrarErroData(int msg) {
        textoErroData.setText(msg);
        textoErroData.setVisibility(View.VISIBLE);
    }

    private void salvar() {
        String motivo = editMotivo.getText() == null ? "" : editMotivo.getText().toString().trim();
        boolean ok = true;
        if (motivo.isEmpty()) {
            UiUtil.erro(campoMotivo, R.string.erro_obrigatorio);
            campoMotivo.announceForAccessibility(getString(R.string.agenda_motivo) + ": " + getString(R.string.erro_obrigatorio));
            ok = false;
        }
        if (dataUtcMillis < 0 || hora < 0) {
            mostrarErroData(R.string.erro_obrigatorio);
            if (ok) textoErroData.announceForAccessibility("Data e horário: " + getString(R.string.erro_obrigatorio));
            ok = false;
        }
        if (!ok) return;

        long quando = montarDataHora();
        long agora = System.currentTimeMillis();
        if (quando <= agora) {
            mostrarErroData(R.string.agenda_erro_data_passada);
            return;
        }

        Visita v = new Visita();
        v.usuarioId = SessionManager.get(requireContext()).getUsuarioId();
        v.motivo = motivo;
        String obs = UiUtil.texto(campoObs);
        v.observacao = obs.isEmpty() ? null : obs;
        v.dataHora = quando;
        v.antecedenciaMin = ANTECEDENCIAS_MIN[antecedenciaIdx];
        // Se a antecedência cair no passado (visita muito próxima), avisa em alguns segundos
        v.lembreteEm = Math.max(quando - v.antecedenciaMin * 60_000L, agora + 10_000L);

        android.content.Context app = requireContext().getApplicationContext();
        AppExecutors.disco().execute(() -> {
            v.id = AppDatabase.get(app).visitaDao().inserir(v);
            boolean exato = LembreteScheduler.agendar(app, v);
            AppExecutors.principal(() -> {
                Bundle r = new Bundle();
                r.putString(R_QUANDO, Formatos.dataHora(quando));
                r.putBoolean(R_EXATO, exato);
                if (isAdded()) {
                    getParentFragmentManager().setFragmentResult(RESULTADO, r);
                    dismissAllowingStateLoss();
                }
            });
        });
    }
}
