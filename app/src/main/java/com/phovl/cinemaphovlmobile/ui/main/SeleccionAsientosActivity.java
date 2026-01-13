    package com.phovl.cinemaphovlmobile.ui.main;

    import android.content.ContentValues;
    import android.content.Intent;
    import android.graphics.Bitmap;
    import android.graphics.Canvas;
    import android.graphics.Paint;
    import android.graphics.pdf.PdfDocument;
    import android.net.Uri;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
    import android.provider.MediaStore;
    import android.util.DisplayMetrics;
    import android.util.Log;
    import android.view.View;
    import android.widget.Button;
    import android.widget.ProgressBar;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.GridLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.google.zxing.BarcodeFormat;
    import com.google.zxing.common.BitMatrix;
    import com.google.zxing.qrcode.QRCodeWriter;
    import com.phovl.cinemaphovlmobile.R;
    import com.phovl.cinemaphovlmobile.adapter.AsientoAdapter;
    import com.phovl.cinemaphovlmobile.model.Asiento;
    import com.phovl.cinemaphovlmobile.session.SessionManager;
    import com.phovl.cinemaphovlmobile.util.GridSpacingItemDecoration;

    import java.io.File;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.io.OutputStream;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Random;
    import java.util.UUID;

    public class SeleccionAsientosActivity extends AppCompatActivity {

        private static final String TAG = "SeleccionAsientos";

        private RecyclerView recyclerAsientos;
        private AsientoAdapter asientoAdapter;
        private ProgressBar progressBar;
        private Button btnConfirmar;
        private TextView txtSelectedCount;

        private int idFuncion;
        private int totalBoletos;
        private int totalPrecio;

        private SessionManager sessionManager;

        // Configuración de la sala (filas)
        private static final char[] FILAS = {
                'A','B','C','D','E','F','G','H','I','J','K','L'
        };

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_seleccion_asientos);

            sessionManager = new SessionManager(this);

            idFuncion = getIntent().getIntExtra("idFuncion", 0);
            totalBoletos = getIntent().getIntExtra("totalBoletos", 0);
            totalPrecio = getIntent().getIntExtra("totalPrecio", 0);

            recyclerAsientos = findViewById(R.id.recycler_asientos_grid);
            progressBar = findViewById(R.id.progress_asientos);
            btnConfirmar = findViewById(R.id.btn_confirmar_asientos);
            txtSelectedCount = findViewById(R.id.txt_selected_count);

            // Calcular columnas dinámicamente según ancho de pantalla
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int minItemDp = 56; // tamaño deseado por asiento en dp
            int screenDp = (int) (dm.widthPixels / dm.density);
            int calculatedColumns = Math.max(6, screenDp / minItemDp); // al menos 6 columnas
            int spanCount = Math.min(calculatedColumns, 12); // no más de 12

            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
            recyclerAsientos.setLayoutManager(gridLayoutManager);
            recyclerAsientos.setHasFixedSize(true);

            // spacing en dp -> px
            int spacingDp = 10;
            int spacingPx = (int) (spacingDp * dm.density + 0.5f);

            // Añadir ItemDecoration para separación uniforme
            recyclerAsientos.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacingPx, true));

            // Padding para que los bordes no queden pegados
            recyclerAsientos.setPadding(spacingPx, spacingPx, spacingPx, spacingPx);
            recyclerAsientos.setClipToPadding(false);

            // Generar asientos dinámicamente usando spanCount como columnas
            List<Asiento> asientos = generarAsientosMock(spanCount);

            // Log para depuración: confirmar que la lista tiene elementos
            Log.d(TAG, "asientos.size() = " + asientos.size() + " (spanCount=" + spanCount + ")");

            asientoAdapter = new AsientoAdapter(asientos, seleccionados -> {
                runOnUiThread(() -> {
                    txtSelectedCount.setText("Boletos seleccionados: " + seleccionados.size());
                    btnConfirmar.setEnabled(seleccionados.size() == totalBoletos && totalBoletos > 0);
                });
            }, totalBoletos, spanCount, spacingPx);

            // Asignar adapter y forzar actualización visual
            recyclerAsientos.setAdapter(asientoAdapter);
            asientoAdapter.notifyDataSetChanged();

            btnConfirmar.setOnClickListener(v -> {
                List<Asiento> seleccionados = asientoAdapter.getSeleccionados();
                if (seleccionados.size() != totalBoletos) {
                    Toast.makeText(this, "Selecciona exactamente " + totalBoletos + " asientos", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Generar QR reales y PDF localmente en Descargas
                btnConfirmar.setEnabled(false);
                showLoading(true);
                List<String> qrCodes = generarQrReales(seleccionados.size());
                generarPdfCompraEnDescargas(seleccionados, qrCodes);
                showLoading(false);
                btnConfirmar.setEnabled(true);
            });

            // Estado inicial
            txtSelectedCount.setText("Boletos seleccionados: 0");
            btnConfirmar.setEnabled(false);
        }

        /**
         * Genera asientos mock y marca algunos como ocupados aleatoriamente.
         * Ahora recibe el número de columnas (spanCount) para generar la grilla correcta.
         */
        private List<Asiento> generarAsientosMock(int spanCount) {
            List<Asiento> list = new ArrayList<>();
            Random rnd = new Random(12345); // semilla fija para reproducibilidad
            for (char fila : FILAS) {
                for (int c = 1; c <= spanCount; c++) {
                    String id = fila + String.valueOf(c);
                    boolean ocupado = rnd.nextInt(100) < 15;
                    list.add(new Asiento(id, ocupado));
                }
            }
            return list;
        }

        /**
         * Genera códigos QR "reales" (UUIDs) que luego se convierten en imágenes dentro del PDF.
         * El contenido del QR es un UUID único por asiento.
         */
        private List<String> generarQrReales(int cantidad) {
            List<String> qr = new ArrayList<>();
            for (int i = 0; i < cantidad; i++) {
                qr.add(UUID.randomUUID().toString());
            }
            return qr;
        }

        private void showLoading(boolean show) {
            runOnUiThread(() -> progressBar.setVisibility(show ? View.VISIBLE : View.GONE));
        }

        /**
         * Genera el PDF y lo guarda en la carpeta Descargas/CinemaPHOVL usando MediaStore (Android 10+).
         * Para Android < 10 escribe en Environment.DIRECTORY_DOWNLOADS (requiere permiso WRITE_EXTERNAL_STORAGE).
         * Dibuja también la imagen QR junto al texto del asiento.
         */
        private void generarPdfCompraEnDescargas(List<Asiento> seleccionados, List<String> qrCodes) {
            if (seleccionados == null || seleccionados.isEmpty()) {
                Toast.makeText(this, "No hay asientos seleccionados para generar PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            String displayName = "ticket_compra_funcion_" + idFuncion + "_" + System.currentTimeMillis() + ".pdf";

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setTextSize(14f);

            int marginLeft = 40;
            int x = marginLeft;
            int y = 60;

            canvas.drawText("Cinema PHOVL - Comprobante de compra", x, y, paint);
            y += 30;
            canvas.drawText("Función ID: " + idFuncion, x, y, paint);
            y += 20;
            canvas.drawText("Usuario ID: " + sessionManager.getUserId(), x, y, paint);
            y += 20;
            canvas.drawText("Total boletos solicitados: " + totalBoletos, x, y, paint);
            y += 20;
            canvas.drawText("Total pagado estimado: $" + totalPrecio + " MXN", x, y, paint);
            y += 30;

            canvas.drawText("Asientos seleccionados:", x, y, paint);
            y += 20;

            // Tamaño del QR en px (aprox)
            int qrSizePx = 80;

            for (int i = 0; i < seleccionados.size(); i++) {
                Asiento a = seleccionados.get(i);
                String qrContent = (i < qrCodes.size()) ? qrCodes.get(i) : UUID.randomUUID().toString();

                // Dibujar texto del asiento
                canvas.drawText("- Asiento: " + a.getId(), x, y + 12, paint);

                // Generar bitmap QR y dibujarlo a la derecha del texto
                try {
                    Bitmap qrBitmap = generarBitmapQr(qrContent, qrSizePx, qrSizePx);
                    if (qrBitmap != null) {
                        int qrX = pageInfo.getPageWidth() - marginLeft - qrSizePx; // alineado a la derecha
                        int qrY = y - 6; // ajustar verticalmente
                        canvas.drawBitmap(qrBitmap, qrX, qrY, null);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error generando QR para " + a.getId() + ": " + e.getMessage(), e);
                }

                // Añadir el contenido del QR como texto pequeño debajo del asiento (opcional)
                paint.setTextSize(10f);
                canvas.drawText("QR: " + qrContent, x + 120, y + 12, paint);
                paint.setTextSize(14f);

                y += Math.max(qrSizePx, 18) + 8;

                if (y > pageInfo.getPageHeight() - 80) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    paint.setTextSize(14f);
                    y = 60;
                }
            }

            document.finishPage(page);

            OutputStream out = null;
            Uri uri = null;
            boolean savedToDownloads = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CinemaPHOVL");
                    uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IOException("No se pudo crear URI en MediaStore");
                    out = getContentResolver().openOutputStream(uri);
                } else {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File folder = new File(downloadsDir, "CinemaPHOVL");
                    if (!folder.exists()) folder.mkdirs();
                    File file = new File(folder, displayName);
                    out = new FileOutputStream(file);
                    uri = Uri.fromFile(file);
                }

                if (out == null) throw new IOException("OutputStream es null");
                document.writeTo(out);
                out.flush();

                String msg = "PDF guardado en Descargas: " + (uri != null ? uri.toString() : displayName);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                Log.d(TAG, "PDF guardado: " + msg);
                savedToDownloads = true;

                // Navegar a MainActivity y limpiar la pila para evitar volver a la selección
                Intent intent = new Intent(SeleccionAsientosActivity.this, com.phovl.cinemaphovlmobile.ui.main.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            } catch (IOException e) {
                Log.e(TAG, "Error al guardar PDF en Descargas", e);
                Toast.makeText(this, "Error al guardar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Fallback: intentar guardar en getExternalFilesDir como antes
                try {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    if (dir != null) {
                        if (!dir.exists()) dir.mkdirs();
                        File file = new File(dir, displayName);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            document.writeTo(fos);
                            String msg = "PDF guardado en almacenamiento de la app: " + file.getAbsolutePath();
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "PDF guardado (fallback): " + msg);

                            // Navegar a MainActivity también en fallback
                            Intent intent = new Intent(SeleccionAsientosActivity.this, com.phovl.cinemaphovlmobile.ui.main.MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Fallback guardar PDF falló", ex);
                    Toast.makeText(this, "No se pudo guardar el PDF", Toast.LENGTH_LONG).show();
                }
            } finally {
                try { if (out != null) out.close(); } catch (IOException ignored) {}
                document.close();
            }
        }

        /**
         * Genera un Bitmap QR a partir de un texto usando ZXing.
         * Requiere la dependencia: com.google.zxing:core:3.5.1
         */
        private Bitmap generarBitmapQr(String text, int width, int height) {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                    }
                }
                return bmp;
            } catch (Exception e) {
                Log.e(TAG, "Error generando bitmap QR: " + e.getMessage(), e);
                return null;
            }
        }
    }
