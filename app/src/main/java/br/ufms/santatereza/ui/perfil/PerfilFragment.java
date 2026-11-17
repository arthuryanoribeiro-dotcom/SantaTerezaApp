package br.ufms.santatereza.ui.perfil;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

import br.ufms.santatereza.R;
import br.ufms.santatereza.data.AppDatabase;
import br.ufms.santatereza.data.Usuario;
import br.ufms.santatereza.seguranca.SessionManager;
import br.ufms.santatereza.ui.BaseAutenticadaActivity;
import br.ufms.santatereza.util.AppExecutors;
import br.ufms.santatereza.util.Formatos;
import br.ufms.santatereza.util.ImagemUtil;
import br.ufms.santatereza.util.UiUtil;

/**
 * Aba Perfil: dados do usuário, foto (câmera ou galeria), edição, troca de senha e logout.
 */
public class PerfilFragment extends Fragment {

    private long usuarioId;
    private Usuario usuario;
    private ImageView foto;
    private TextView nome, email, telefone, cidade, membroDesde;
    private Uri uriCamera;

    /** Câmera: o app de câmera grava a foto no arquivo privado indicado via FileProvider. */
    private final ActivityResultLauncher<Uri> tirarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), sucesso -> {
                if (Boolean.TRUE.equals(sucesso) && uriCamera != null) processarFoto(uriCamera);
                else mensagem(R.string.foto_cancelada);
            });

    /** Galeria: Photo Picker do Android (não exige permissão de leitura de arquivos). */
    private final ActivityResultLauncher<PickVisualMediaRequest> escolherDaGaleria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) processarFoto(uri);
                else mensagem(R.string.foto_cancelada);
            });

    private final ActivityResultLauncher<String> pedirCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    abrirCamera();
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Negada permanentemente: oferece as configurações
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.permissao_titulo)
                            .setMessage(R.string.camera_negada_sempre)
                            .setPositiveButton(R.string.abrir_configuracoes, (d, w) ->
                                    UiUtil.abrirConfiguracoesDoApp(requireContext()))
                            .setNeutralButton(R.string.foto_galeria, (d, w) -> abrirGaleria())
                            .setNegativeButton(R.string.cancelar, null)
                            .show();
                } else {
                    mensagem(R.string.camera_negada);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        usuarioId = SessionManager.get(requireContext()).getUsuarioId();
        if (savedInstanceState != null) {
            String u = savedInstanceState.getString("uri_camera");
            if (u != null) uriCamera = Uri.parse(u);
        }
        foto = v.findViewById(R.id.foto);
        nome = v.findViewById(R.id.nome);
        email = v.findViewById(R.id.email);
        telefone = v.findViewById(R.id.telefone);
        cidade = v.findViewById(R.id.cidade);
        membroDesde = v.findViewById(R.id.membroDesde);

        foto.setOnClickListener(b -> opcoesFoto());
        v.findViewById(R.id.botaoAlterarFoto).setOnClickListener(b -> opcoesFoto());
        v.findViewById(R.id.botaoEditar).setOnClickListener(b ->
                startActivity(new Intent(requireContext(), EditarPerfilActivity.class)));
        v.findViewById(R.id.botaoSenha).setOnClickListener(b ->
                startActivity(new Intent(requireContext(), AlterarSenhaActivity.class)));
        v.findViewById(R.id.botaoSair).setOnClickListener(b -> confirmarSaida());

        AppDatabase.get(requireContext()).usuarioDao().observar(usuarioId)
                .observe(getViewLifecycleOwner(), this::preencher);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (uriCamera != null) out.putString("uri_camera", uriCamera.toString());
    }

    private void preencher(Usuario u) {
        if (u == null) return;
        usuario = u;
        nome.setText(u.nome);
        email.setText(u.email);
        telefone.setText(u.telefone == null ? getString(R.string.perfil_sem_telefone) : formatarTelefone(u.telefone));
        cidade.setText(u.cidade == null ? getString(R.string.perfil_sem_cidade) : u.cidade);
        membroDesde.setText(getString(R.string.perfil_membro_desde, Formatos.data(u.criadoEm)));

        Bitmap bmp = ImagemUtil.carregar(u.fotoPath, 384);
        if (bmp != null) {
            foto.setImageBitmap(bmp);
            foto.setImageTintList(null);
            foto.setContentDescription(getString(R.string.cd_foto_perfil, u.nome));
        } else {
            foto.setImageResource(R.drawable.ic_person);
            foto.setImageTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.verde_pasto)));
            foto.setContentDescription(getString(R.string.cd_foto_perfil_vazia));
        }
    }

    private static String formatarTelefone(String d) {
        if (d.length() == 11) return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        if (d.length() == 10) return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        return d;
    }

    private void opcoesFoto() {
        boolean temFoto = usuario != null && usuario.fotoPath != null;
        String[] opcoes = temFoto
                ? new String[]{getString(R.string.foto_camera), getString(R.string.foto_galeria), getString(R.string.foto_remover)}
                : new String[]{getString(R.string.foto_camera), getString(R.string.foto_galeria)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perfil_alterar_foto)
                .setItems(opcoes, (d, i) -> {
                    if (i == 0) solicitarCamera();
                    else if (i == 1) abrirGaleria();
                    else removerFoto();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void solicitarCamera() {
        if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            mensagem(R.string.camera_indisponivel);
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.permissao_titulo)
                    .setMessage(R.string.camera_rationale)
                    .setPositiveButton(R.string.permitir, (d, w) -> pedirCamera.launch(Manifest.permission.CAMERA))
                    .setNegativeButton(R.string.agora_nao, null)
                    .show();
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamera() {
        File arquivo = ImagemUtil.arquivoTemporarioCamera(requireContext());
        uriCamera = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", arquivo);
        try {
            tirarFoto.launch(uriCamera);
        } catch (Exception e) {
            mensagem(R.string.camera_indisponivel);
        }
    }

    private void abrirGaleria() {
        escolherDaGaleria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void processarFoto(Uri origem) {
        Context app = requireContext().getApplicationContext();
        AppExecutors.disco().execute(() -> {
            try {
                String caminho = ImagemUtil.salvarFotoPerfil(app, origem, usuarioId);
                AppDatabase.get(app).usuarioDao().atualizarFoto(usuarioId, caminho); // persiste no Room
                //noinspection ResultOfMethodCallIgnored
                ImagemUtil.arquivoTemporarioCamera(app).delete();
                AppExecutors.principal(() -> mensagem(R.string.foto_atualizada));
            } catch (Exception e) {
                AppExecutors.principal(() -> mensagem(R.string.foto_erro));
            }
        });
    }

    private void removerFoto() {
        Context app = requireContext().getApplicationContext();
        AppExecutors.disco().execute(() -> {
            AppDatabase.get(app).usuarioDao().atualizarFoto(usuarioId, null);
            ImagemUtil.apagarFotosAntigas(app, usuarioId, null);
            AppExecutors.principal(() -> mensagem(R.string.foto_removida));
        });
    }

    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sair_titulo)
                .setMessage(R.string.sair_msg)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.sair, (d, w) -> {
                    if (getActivity() instanceof BaseAutenticadaActivity) {
                        ((BaseAutenticadaActivity) getActivity()).fazerLogout();
                    }
                })
                .show();
    }

    private void mensagem(@StringRes int msg) {
        if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
    }
}
