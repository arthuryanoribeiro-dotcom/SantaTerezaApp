package br.ufms.santatereza.ui.mapa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import br.ufms.santatereza.R;
import br.ufms.santatereza.util.Contatos;
import br.ufms.santatereza.util.UiUtil;

/**
 * Aba Mapa (recurso obrigatório: Geolocalização).
 * - Mapa OpenStreetMap com a fazenda e a cidade de referência.
 * - Localização do usuário obtida UMA vez, sob demanda (sem rastreamento contínuo,
 *   para economizar bateria), com prioridade "balanceada".
 * - Distância até a fazenda calculada e escrita em texto (acessível ao TalkBack).
 */
public class MapaFragment extends Fragment {

    private static final GeoPoint FAZENDA = new GeoPoint(Contatos.FAZENDA_LAT, Contatos.FAZENDA_LON);
    private static final GeoPoint CIDADE = new GeoPoint(Contatos.CIDADE_LAT, Contatos.CIDADE_LON);

    private MapView mapa;
    private TextView textoStatus;
    private Marker marcadorUsuario;
    private Polyline linhaAteFazenda;
    private FusedLocationProviderClient localizacao;
    private CancellationTokenSource cancelamento;

    private final ActivityResultLauncher<String[]> pedirLocalizacao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::aoResponderPermissao);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        textoStatus = v.findViewById(R.id.textoStatus);
        localizacao = LocationServices.getFusedLocationProviderClient(requireContext());

        mapa = v.findViewById(R.id.mapa);
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.setTilesScaledToDpi(true); // textos do mapa legíveis em telas de alta densidade
        mapa.getController().setZoom(12.0);
        mapa.getController().setCenter(FAZENDA);

        adicionarMarcador(FAZENDA, getString(R.string.mapa_fazenda), getString(R.string.mapa_fazenda_end),
                R.drawable.ic_place, R.color.verde_pasto);
        adicionarMarcador(CIDADE, getString(R.string.mapa_cidade), "Referência", R.drawable.ic_place,
                R.color.marrom_terra);

        v.findViewById(R.id.botaoComoChegar).setOnClickListener(b -> Contatos.comoChegar(requireContext()));
        v.findViewById(R.id.botaoVerFazenda).setOnClickListener(b -> {
            mapa.getController().animateTo(FAZENDA, 14.0, 600L);
            b.announceForAccessibility(getString(R.string.mapa_fazenda));
        });
        v.findViewById(R.id.botaoMinhaLocalizacao).setOnClickListener(b -> solicitarLocalizacao());

        // Se a permissão já foi dada antes, mostra a distância uma vez ao abrir a aba
        if (temPermissao()) {
            obterLocalizacao();
        }
    }

    private Marker adicionarMarcador(GeoPoint p, String titulo, String descricao,
                                     @DrawableRes int icone, int cor) {
        Marker m = new Marker(mapa);
        m.setPosition(p);
        m.setTitle(titulo);
        m.setSnippet(descricao);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable d = ContextCompat.getDrawable(requireContext(), icone);
        if (d != null) {
            d = DrawableCompat.wrap(d.mutate());
            DrawableCompat.setTint(d, ContextCompat.getColor(requireContext(), cor));
            m.setIcon(d);
        }
        mapa.getOverlays().add(m);
        return m;
    }

    private boolean temPermissao() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarLocalizacao() {
        if (temPermissao()) {
            obterLocalizacao();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Usuário já negou uma vez: explica o motivo antes de pedir de novo
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.permissao_titulo)
                    .setMessage(R.string.mapa_rationale)
                    .setPositiveButton(R.string.permitir, (d, w) -> lancarPedido())
                    .setNegativeButton(R.string.agora_nao, null)
                    .show();
        } else {
            lancarPedido();
        }
    }

    private void lancarPedido() {
        pedirLocalizacao.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void aoResponderPermissao(Map<String, Boolean> resultado) {
        boolean fina = Boolean.TRUE.equals(resultado.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean aproximada = Boolean.TRUE.equals(resultado.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (fina || aproximada) {
            obterLocalizacao();
            return;
        }
        // Negada. Se o sistema não deixa mais perguntar, só pelas configurações.
        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            textoStatus.setText(R.string.mapa_permissao_negada_sempre);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.permissao_titulo)
                    .setMessage(R.string.mapa_permissao_negada_sempre)
                    .setPositiveButton(R.string.abrir_configuracoes, (d, w) ->
                            UiUtil.abrirConfiguracoesDoApp(requireContext()))
                    .setNegativeButton(R.string.agora_nao, null)
                    .show();
        } else {
            textoStatus.setText(R.string.mapa_sem_permissao);
        }
    }

    @SuppressLint("MissingPermission") // a permissão é verificada em temPermissao()
    private void obterLocalizacao() {
        if (!temPermissao()) return;
        textoStatus.setText(R.string.mapa_buscando);
        if (cancelamento != null) cancelamento.cancel();
        cancelamento = new CancellationTokenSource();
        try {
            localizacao.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancelamento.getToken())
                    .addOnSuccessListener(loc -> {
                        if (!isAdded()) return;
                        if (loc == null) textoStatus.setText(R.string.mapa_gps_desligado);
                        else mostrarUsuario(loc);
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) textoStatus.setText(R.string.mapa_gps_desligado);
                    });
        } catch (Exception e) { // ex.: aparelho sem Google Play Services
            textoStatus.setText(R.string.mapa_gps_desligado);
        }
    }

    private void mostrarUsuario(Location loc) {
        GeoPoint eu = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        if (marcadorUsuario == null) {
            marcadorUsuario = adicionarMarcador(eu, getString(R.string.mapa_voce), null,
                    R.drawable.ic_my_location, R.color.erro);
            marcadorUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        } else {
            marcadorUsuario.setPosition(eu);
        }

        if (linhaAteFazenda == null) {
            linhaAteFazenda = new Polyline(mapa);
            linhaAteFazenda.getOutlinePaint().setColor(ContextCompat.getColor(requireContext(), R.color.marrom_terra));
            linhaAteFazenda.getOutlinePaint().setStrokeWidth(6f);
            mapa.getOverlays().add(0, linhaAteFazenda);
        }
        linhaAteFazenda.setPoints(Arrays.asList(eu, FAZENDA));

        float[] metros = new float[1];
        Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), FAZENDA.getLatitude(), FAZENDA.getLongitude(), metros);
        double km = metros[0] / 1000.0;
        String texto = km < 0.5
                ? getString(R.string.mapa_distancia_perto)
                : getString(R.string.mapa_distancia, String.format(new Locale("pt", "BR"), "%.1f", km));
        boolean soAproximada = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (soAproximada) texto += " " + getString(R.string.mapa_aproximada);
        textoStatus.setText(texto);

        // Enquadra usuário e fazenda na tela
        mapa.post(() -> {
            try {
                org.osmdroid.util.BoundingBox caixa = org.osmdroid.util.BoundingBox.fromGeoPoints(Arrays.asList(eu, FAZENDA));
                mapa.zoomToBoundingBox(caixa.increaseByScale(1.4f), true);
            } catch (Exception ignorada) {
                mapa.getController().animateTo(eu);
            }
        });
        mapa.invalidate();
    }

    // Ciclo de vida do mapa: pausa o download/renderização quando a aba não está visível
    @Override
    public void onResume() {
        super.onResume();
        if (mapa != null && !isHidden()) mapa.onResume();
    }

    @Override
    public void onPause() {
        if (mapa != null) mapa.onPause();
        if (cancelamento != null) cancelamento.cancel();
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mapa == null) return;
        if (hidden) {
            mapa.onPause();
            if (cancelamento != null) cancelamento.cancel();
        } else {
            mapa.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        if (mapa != null) mapa.onDetach();
        mapa = null;
        marcadorUsuario = null;
        linhaAteFazenda = null;
        super.onDestroyView();
    }
}
