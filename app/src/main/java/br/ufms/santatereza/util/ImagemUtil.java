package br.ufms.santatereza.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Salva a foto de perfil no armazenamento INTERNO do app (pasta privada "fotos/"),
 * reduzida para no máximo 1024 px e com a rotação corrigida. O caminho do arquivo
 * é o que fica persistido no Room.
 */
public final class ImagemUtil {

    private static final int LADO_MAXIMO = 1024;

    private ImagemUtil() { }

    public static File pastaFotos(Context context) {
        File pasta = new File(context.getFilesDir(), "fotos");
        if (!pasta.exists()) //noinspection ResultOfMethodCallIgnored
            pasta.mkdirs();
        return pasta;
    }

    /** Arquivo temporário onde o app de câmera grava a foto. */
    public static File arquivoTemporarioCamera(Context context) {
        return new File(pastaFotos(context), "camera_temp.jpg");
    }

    /** Processa a imagem de origem e devolve o caminho do arquivo final salvo. */
    public static String salvarFotoPerfil(Context context, Uri origem, long usuarioId) throws Exception {
        // 1) Lê apenas as dimensões para calcular a redução (economiza memória)
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(origem)) {
            BitmapFactory.decodeStream(in, null, op);
        }
        if (op.outWidth <= 0 || op.outHeight <= 0) throw new Exception("Imagem inválida");

        int amostra = 1;
        while (op.outWidth / (amostra * 2) >= LADO_MAXIMO || op.outHeight / (amostra * 2) >= LADO_MAXIMO) {
            amostra *= 2;
        }

        // 2) Decodifica reduzida
        BitmapFactory.Options op2 = new BitmapFactory.Options();
        op2.inSampleSize = amostra;
        Bitmap bmp;
        try (InputStream in = context.getContentResolver().openInputStream(origem)) {
            bmp = BitmapFactory.decodeStream(in, null, op2);
        }
        if (bmp == null) throw new Exception("Falha ao decodificar");

        // 3) Corrige a rotação informada no EXIF (comum em fotos da câmera)
        int graus = 0;
        try (InputStream in = context.getContentResolver().openInputStream(origem)) {
            if (in != null) {
                int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                if (o == ExifInterface.ORIENTATION_ROTATE_90) graus = 90;
                else if (o == ExifInterface.ORIENTATION_ROTATE_180) graus = 180;
                else if (o == ExifInterface.ORIENTATION_ROTATE_270) graus = 270;
            }
        } catch (Exception ignorada) { /* sem EXIF */ }
        if (graus != 0) {
            Matrix m = new Matrix();
            m.postRotate(graus);
            Bitmap girado = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (girado != bmp) bmp.recycle();
            bmp = girado;
        }

        // 4) Grava o JPEG final e apaga fotos antigas deste usuário
        File pasta = pastaFotos(context);
        File destino = new File(pasta, "perfil_" + usuarioId + "_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(destino)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
        }
        bmp.recycle();
        apagarFotosAntigas(context, usuarioId, destino);
        return destino.getAbsolutePath();
    }

    public static void apagarFotosAntigas(Context context, long usuarioId, File manter) {
        File[] arquivos = pastaFotos(context).listFiles();
        if (arquivos == null) return;
        for (File f : arquivos) {
            if (f.getName().startsWith("perfil_" + usuarioId + "_")
                    && (manter == null || !f.equals(manter))) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    /** Carrega a foto já reduzida para exibir no perfil. */
    public static Bitmap carregar(String caminho, int ladoDesejado) {
        if (caminho == null || !new File(caminho).exists()) return null;
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caminho, op);
        int amostra = 1;
        while (op.outWidth / (amostra * 2) >= ladoDesejado && op.outHeight / (amostra * 2) >= ladoDesejado) {
            amostra *= 2;
        }
        BitmapFactory.Options op2 = new BitmapFactory.Options();
        op2.inSampleSize = amostra;
        return BitmapFactory.decodeFile(caminho, op2);
    }
}
