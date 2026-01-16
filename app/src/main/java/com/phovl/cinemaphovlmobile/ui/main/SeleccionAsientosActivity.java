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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.AsientoAdapter;
import com.phovl.cinemaphovlmobile.model.Asiento;
import com.phovl.cinemaphovlmobile.network.ApiService;
import com.phovl.cinemaphovlmobile.network.RetrofitClient;
import com.phovl.cinemaphovlmobile.session.SessionManager;
import com.phovl.cinemaphovlmobile.util.GridSpacingItemDecoration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    private List<Asiento> asientos;

    private static final char[] FILAS = { 'A','B','C','D','E' };

    private static final int REQ_PAYPAL = 1234;

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

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int minItemDp = 56;
        int screenDp = (int) (dm.widthPixels / dm.density);
        int calculatedColumns = Math.max(6, screenDp / minItemDp);
        int spanCount = Math.min(calculatedColumns, 12);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        recyclerAsientos.setLayoutManager(gridLayoutManager);
        recyclerAsientos.setHasFixedSize(true);

        int spacingDp = 10;
        int spacingPx = (int) (spacingDp * dm.density + 0.5f);
        recyclerAsientos.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacingPx, true));
        recyclerAsientos.setPadding(spacingPx, spacingPx, spacingPx, spacingPx);
        recyclerAsientos.setClipToPadding(false);

        asientos = generarAsientosMock(spanCount);

        Log.d(TAG, "asientos.size() = " + asientos.size() + " (spanCount=" + spanCount + ")");

        asientoAdapter = new AsientoAdapter(
                asientos,
                seleccionados -> runOnUiThread(() -> {
                    txtSelectedCount.setText("Boletos seleccionados: " + seleccionados.size());
                    btnConfirmar.setEnabled(seleccionados.size() == totalBoletos && totalBoletos > 0);
                }),
                totalBoletos,
                spanCount,
                spacingPx
        );

        recyclerAsientos.setAdapter(asientoAdapter);
        asientoAdapter.notifyDataSetChanged();

        fetchOcupadosAndMark();

        btnConfirmar.setOnClickListener(v -> {
            List<Asiento> seleccionados = asientoAdapter.getSeleccionados();
            if (seleccionados.size() != totalBoletos) {
                Toast.makeText(this, "Selecciona exactamente " + totalBoletos + " asientos", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);
            btnConfirmar.setEnabled(false);

            checkAsientosDisponiblesBeforePayment(seleccionados, available -> runOnUiThread(() -> {
                showLoading(false);
                btnConfirmar.setEnabled(true);

                if (!available) {
                    Toast.makeText(this, "Algunos asientos ya fueron ocupados. Se actualizaron los asientos.", Toast.LENGTH_LONG).show();
                    fetchOcupadosAndMark();
                    return;
                }

                double amountDouble;
                if (totalPrecio > 0) {
                    amountDouble = totalPrecio;
                } else {
                    amountDouble = 5.00 * seleccionados.size();
                }

                String amountStr = String.format("%.2f", amountDouble);

                Intent payIntent = new Intent(SeleccionAsientosActivity.this, WebViewPayPalActivity.class);
                payIntent.putExtra(WebViewPayPalActivity.EXTRA_AMOUNT, amountStr);
                startActivityForResult(payIntent, REQ_PAYPAL);
            }));
        });

        txtSelectedCount.setText("Boletos seleccionados: 0");
        btnConfirmar.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        showLoading(false);
        btnConfirmar.setEnabled(true);

        if (requestCode == REQ_PAYPAL) {
            if (resultCode == RESULT_OK && data != null) {
                String status = data.getStringExtra("paypal_status");
                String orderId = data.getStringExtra("paypal_order_id");

                Log.d(TAG, "onActivityResult RESULT_OK paypal_status=" + status + " orderId=" + orderId);

                if ("COMPLETED".equals(status)) {
                    List<Asiento> seleccionados = asientoAdapter.getSeleccionados();

                    if (seleccionados == null || seleccionados.isEmpty()) {
                        Toast.makeText(this, "No hay asientos seleccionados", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Integer> idAsientosInt = new ArrayList<>();
                    for (Asiento a : seleccionados) {
                        idAsientosInt.add(a.getIdAsientoDb());
                    }

                    if (idAsientosInt.isEmpty()) {
                        Toast.makeText(this, "No se pudieron obtener los IDs numéricos de los asientos.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    int idUsuario = sessionManager.getUserId();

                    com.phovl.cinemaphovlmobile.network.model.CompraRequest req =
                            new com.phovl.cinemaphovlmobile.network.model.CompraRequest(
                                    idFuncion,
                                    idAsientosInt,
                                    idUsuario
                            );

                    ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

                    showLoading(true);

                    api.comprarTickets(req).enqueue(new Callback<com.phovl.cinemaphovlmobile.network.model.CompraResponse>() {
                        @Override
                        public void onResponse(Call<com.phovl.cinemaphovlmobile.network.model.CompraResponse> call,
                                               Response<com.phovl.cinemaphovlmobile.network.model.CompraResponse> response) {

                            showLoading(false);
                            btnConfirmar.setEnabled(true);

                            if (response.isSuccessful() && response.body() != null) {
                                List<com.phovl.cinemaphovlmobile.network.model.TicketDto> tickets =
                                        response.body().tickets;

                                if (tickets != null && !tickets.isEmpty()) {
                                    List<String> qrCodes = new ArrayList<>();
                                    for (com.phovl.cinemaphovlmobile.network.model.TicketDto t : tickets) {
                                        qrCodes.add(t.codigo_qr);
                                    }
                                    generarPdfCompraEnDescargas(seleccionados, qrCodes);
                                } else {
                                    Toast.makeText(SeleccionAsientosActivity.this,
                                            "Compra registrada pero no se devolvieron tickets",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                if (response.code() == 409) {
                                    String body = "Asiento ocupado";
                                    try {
                                        if (response.errorBody() != null)
                                            body = response.errorBody().string();
                                    } catch (Exception ignored) {}
                                    Toast.makeText(SeleccionAsientosActivity.this,
                                            "No se pudo completar la compra: " + body,
                                            Toast.LENGTH_LONG).show();
                                    fetchOcupadosAndMark();
                                    return;
                                }

                                String err = "Error registrando compra: " + response.code();
                                try {
                                    if (response.errorBody() != null)
                                        err += " " + response.errorBody().string();
                                } catch (Exception ignored) {}

                                Log.e(TAG, err);
                                Toast.makeText(SeleccionAsientosActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<com.phovl.cinemaphovlmobile.network.model.CompraResponse> call,
                                              Throwable t) {
                            showLoading(false);
                            btnConfirmar.setEnabled(true);
                            Log.e(TAG, "Fallo al llamar comprarTickets: " + t.getMessage(), t);
                            Toast.makeText(SeleccionAsientosActivity.this,
                                    "Fallo conexión: " + t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this,
                            "Resultado de pago desconocido: " + status,
                            Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No se recibió respuesta del pago", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLoading(boolean show) {
        runOnUiThread(() ->
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE)
        );
    }

    private List<Asiento> generarAsientosMock(int spanCount) {
        List<Asiento> list = new ArrayList<>();
        Random rnd = new Random(12345);
        int dbCounter = 1;

        for (char fila : FILAS) {
            for (int c = 1; c <= spanCount; c++) {
                String label = fila + String.valueOf(c);
                boolean ocupado = rnd.nextInt(100) < 15;
                list.add(new Asiento(dbCounter++, label, ocupado));
            }
        }
        return list;
    }

    private void checkAsientosDisponiblesBeforePayment(
            List<Asiento> seleccionados,
            Consumer<Boolean> callback
    ) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Call<ResponseBody> call = api.getAsientosOcupados(idFuncion);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.accept(true);
                        return;
                    }

                    String json = response.body().string();
                    JSONArray arr = new JSONArray(json);
                    List<Integer> ocupados = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        Object o = arr.get(i);
                        if (o instanceof JSONObject) {
                            JSONObject obj = (JSONObject) o;
                            if (obj.has("id_asiento"))
                                ocupados.add(obj.getInt("id_asiento"));
                            else if (obj.has("id"))
                                ocupados.add(obj.getInt("id"));
                        } else {
                            try {
                                ocupados.add(Integer.parseInt(o.toString()));
                            } catch (Exception ignored) {}
                        }
                    }

                    for (Asiento a : seleccionados) {
                        if (ocupados.contains(a.getIdAsientoDb())) {
                            callback.accept(false);
                            return;
                        }
                    }

                    callback.accept(true);
                } catch (Exception e) {
                    Log.w(TAG, "Error parseando ocupados: " + e.getMessage(), e);
                    callback.accept(true);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.w(TAG, "Fallo al obtener asientos ocupados: " + t.getMessage(), t);
                callback.accept(true);
            }
        });
    }

    private void fetchOcupadosAndMark() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Call<ResponseBody> call = api.getAsientosOcupados(idFuncion);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.w(TAG, "No se pudo obtener ocupados");
                        return;
                    }

                    String json = response.body().string();
                    JSONArray arr = new JSONArray(json);
                    List<Integer> ocupados = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        Object o = arr.get(i);
                        if (o instanceof JSONObject) {
                            JSONObject obj = (JSONObject) o;
                            if (obj.has("id_asiento"))
                                ocupados.add(obj.getInt("id_asiento"));
                            else if (obj.has("id"))
                                ocupados.add(obj.getInt("id"));
                        } else {
                            try {
                                ocupados.add(Integer.parseInt(o.toString()));
                            } catch (Exception ignored) {}
                        }
                    }

                    runOnUiThread(() -> {
                        if (asientoAdapter != null) {
                            asientoAdapter.markOcupados(ocupados);
                            txtSelectedCount.setText(
                                    "Boletos seleccionados: " +
                                            asientoAdapter.getSeleccionados().size()
                            );
                            btnConfirmar.setEnabled(
                                    asientoAdapter.getSeleccionados().size() == totalBoletos
                                            && totalBoletos > 0
                            );
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Error parseando ocupados: " + e.getMessage(), e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.w(TAG, "Fallo al obtener asientos ocupados: " + t.getMessage(), t);
            }
        });
    }

    private void generarPdfCompraEnDescargas(
            List<Asiento> seleccionados,
            List<String> qrCodes
    ) {
        if (seleccionados == null || seleccionados.isEmpty()) {
            Toast.makeText(this,
                    "No hay asientos seleccionados para generar PDF",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName =
                "ticket_compra_funcion_" + idFuncion + "_" + System.currentTimeMillis() + ".pdf";

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
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

        int qrSizePx = 80;

        for (int i = 0; i < seleccionados.size(); i++) {
            Asiento a = seleccionados.get(i);
            String qrContent =
                    (i < qrCodes.size()) ? qrCodes.get(i) : UUID.randomUUID().toString();

            canvas.drawText("- Asiento: " + a.getId(), x, y + 12, paint);

            try {
                Bitmap qrBitmap = generarBitmapQr(qrContent, qrSizePx, qrSizePx);
                if (qrBitmap != null) {
                    int qrX = pageInfo.getPageWidth() - marginLeft - qrSizePx;
                    int qrY = y - 6;
                    canvas.drawBitmap(qrBitmap, qrX, qrY, null);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error generando QR para " + a.getId(), e);
            }

            paint.setTextSize(10f);
            canvas.drawText("QR: " + qrContent, x + 120, y + 12, paint);
            paint.setTextSize(14f);

            y += Math.max(qrSizePx, 18) + 8;

            if (y > pageInfo.getPageHeight() - 80) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(
                        595, 842, document.getPages().size() + 1
                ).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                paint.setTextSize(14f);
                y = 60;
            }
        }

        document.finishPage(page);

        OutputStream out = null;
        Uri uri = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/CinemaPHOVL");

                uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                );
                out = getContentResolver().openOutputStream(uri);
            } else {
                File downloadsDir =
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                        );
                File folder = new File(downloadsDir, "CinemaPHOVL");
                if (!folder.exists()) folder.mkdirs();
                File file = new File(folder, displayName);
                out = new FileOutputStream(file);
                uri = Uri.fromFile(file);
            }

            document.writeTo(out);
            out.flush();

            Toast.makeText(this,
                    "PDF guardado en Descargas",
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(
                    SeleccionAsientosActivity.this,
                    com.phovl.cinemaphovlmobile.ui.main.MainActivity.class
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error al guardar PDF", e);
            Toast.makeText(this,
                    "Error al guardar PDF",
                    Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {}
            document.close();
        }
    }

    private Bitmap generarBitmapQr(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix =
                    writer.encode(text, BarcodeFormat.QR_CODE, width, height);

            Bitmap bmp = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888
            );

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? 0xFF000000
                                    : 0xFFFFFFFF
                    );
                }
            }
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "Error generando bitmap QR", e);
            return null;
        }
    }
}
