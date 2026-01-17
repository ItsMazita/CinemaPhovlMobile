package com.phovl.cinemaphovlmobile.ui.main;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


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
import com.phovl.cinemaphovlmobile.ui.main.ProfileActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

    // Executor para generar PDFs en background
    private final Executor pdfExecutor = Executors.newSingleThreadExecutor();

    // NUEVO: nombre de la película (se recibe por Intent)
    private String nombrePelicula;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion_asientos);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "compras_channel",
                    "Compras",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        sessionManager = new SessionManager(this);

        idFuncion = getIntent().getIntExtra("idFuncion", 0);
        totalBoletos = getIntent().getIntExtra("totalBoletos", 0);
        totalPrecio = getIntent().getIntExtra("totalPrecio", 0);
        nombrePelicula = getIntent().getStringExtra("nombrePelicula"); // <-- nuevo
        Log.d(TAG, "onCreate nombrePelicula='" + nombrePelicula + "' idFuncion=" + idFuncion);

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

    private void mostrarNotificacionCompraExitosa() {
        // Intent que abre la pantalla de perfil
        Intent intent = new Intent(this, com.phovl.cinemaphovlmobile.ui.main.ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "compras_channel")
                .setSmallIcon(R.drawable.ic_profile) // usa un icono de tu app
                .setContentTitle("Compra exitosa")
                .setContentText("Tus boletos se generaron correctamente. Toca para ver tu perfil.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1001, builder.build());
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
                                    // Generar un PDF por ticket/asiento en background
                                    final String nombreParaPdf = nombrePelicula; // captura el valor actual
                                    Log.d(TAG, "Encolando generación PDF. nombreParaPdf='" + nombreParaPdf + "'");
                                    pdfExecutor.execute(() -> generarPdfPorTicket(seleccionados, qrCodes, orderId, nombreParaPdf));
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

    private String safeFileName(String raw) {
        if (raw == null) return null;
        // Reemplaza caracteres inválidos por guión bajo
        String s = raw.replaceAll("[^a-zA-Z0-9\\- _]", "_");
        // Reemplaza espacios múltiples por uno solo y recorta
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > 60) s = s.substring(0, 60).trim();
        // Cambia espacios por guiones bajos para mayor compatibilidad
        s = s.replace(' ', '_');
        return s;
    }

    /**
     * Genera un PDF por cada asiento/ticket y lo guarda en Descargas/CinemaPHOVL.
     * Ahora recibe nombrePeliculaLocal para garantizar que el título viaja correctamente.
     */
    private void generarPdfPorTicket(
            List<Asiento> seleccionados,
            List<String> qrCodes,
            String orderId,
            String nombrePeliculaLocal
    ) {
        Log.d(TAG, "generarPdfPorTicket inicio. nombrePeliculaLocal='" + nombrePeliculaLocal + "'");
        if (seleccionados == null || seleccionados.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this,
                    "No hay asientos seleccionados para generar PDF",
                    Toast.LENGTH_SHORT).show());
            return;
        }

        int generated = 0;

        for (int i = 0; i < seleccionados.size(); i++) {
            Asiento a = seleccionados.get(i);
            String qrContent = (i < qrCodes.size()) ? qrCodes.get(i) : UUID.randomUUID().toString();

            // Normaliza el nombre de la película para usarlo como nombre de archivo
            String baseName = (nombrePeliculaLocal != null && !nombrePeliculaLocal.isEmpty())
                    ? safeFileName(nombrePeliculaLocal)
                    : "ticket_funcion_" + idFuncion;

            // Sufijo para evitar colisiones y para identificar asiento
            String seatSuffix = "_asiento_" + a.getId();
            // Timestamp corto opcional para evitar duplicados exactos
            String ts = "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

            // Construye el nombre final. Si quieres SOLO el nombre de la película,
            // usa: String displayName = baseName + ".pdf";
            int userId = sessionManager.getUserId(); // usuario actual
            String displayName = baseName + seatSuffix + "_usuario" + userId + ts + ".pdf";


            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            try {
                // Dibuja plantilla mejorada (ahora incluye nombre de la película pasado como parámetro)
                drawTicketPage(canvas, pageInfo, idFuncion, a, qrContent, orderId, nombrePeliculaLocal);
            } catch (Exception e) {
                Log.w(TAG, "Error dibujando ticket para " + a.getId(), e);
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
                    if (uri == null) throw new IOException("No se pudo crear Uri en MediaStore");
                    out = getContentResolver().openOutputStream(uri);
                } else {
                    File downloadsDir =
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                            );
                    File folder = new File(downloadsDir, "CinemaPHOVL");
                    if (!folder.exists() && !folder.mkdirs()) {
                        Log.w(TAG, "No se pudo crear carpeta Descargas/CinemaPHOVL");
                    }
                    File file = new File(folder, displayName);
                    out = new FileOutputStream(file);
                    uri = Uri.fromFile(file);
                }

                if (out == null) throw new IOException("OutputStream nulo al escribir PDF");

                document.writeTo(out);
                out.flush();
                generated++;
                Log.d(TAG, "PDF guardado: " + displayName + " uri=" + uri);

            } catch (Exception e) {
                Log.e(TAG, "Error al guardar PDF " + displayName, e);
                final String nameErr = displayName;
                runOnUiThread(() -> Toast.makeText(this, "Error al guardar PDF: " + nameErr, Toast.LENGTH_LONG).show());
            } finally {
                try {
                    if (out != null) out.close();
                } catch (Exception ignored) {}
                document.close();
            }
        }

        final int finalGenerated = generated;
        runOnUiThread(() -> {
            Toast.makeText(this, "Se generaron " + finalGenerated + " PDFs en Descargas/CinemaPHOVL", Toast.LENGTH_LONG).show();
            mostrarNotificacionCompraExitosa();

            // Regresar a MainActivity
            Intent intent = new Intent(SeleccionAsientosActivity.this, com.phovl.cinemaphovlmobile.ui.main.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }


    /**
     * Dibuja la plantilla mejorada del ticket en el canvas.
     * Ahora recibe nombrePeliculaLocal para evitar depender del estado de instancia en el hilo.
     */
    private void drawTicketPage(Canvas canvas, PdfDocument.PageInfo pageInfo, int idFuncion, Asiento a, String qrContent, String orderId, String nombrePeliculaLocal) {
        Paint paint = new Paint();
        int margin = 36;
        int x = margin;
        int y = 48;

        // Fondo blanco limpio
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(0, 0, pageInfo.getPageWidth(), pageInfo.getPageHeight(), paint);

        // Header background (brand color)
        paint.setColor(0xFF6A21BD);
        canvas.drawRect(0, 0, pageInfo.getPageWidth(), 88, paint);

        // Logo (si existe)
        try {
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            if (logo != null) {
                int logoH = 56;
                int logoW = (int) (logo.getWidth() * (logoH / (float) logo.getHeight()));
                Bitmap scaled = Bitmap.createScaledBitmap(logo, logoW, logoH, true);
                canvas.drawBitmap(scaled, margin, 16, null);
            }
        } catch (Exception ignored) {}

        // Title
        paint.setColor(0xFFFFEB3B);
        paint.setTextSize(20f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Cinema PHOVL", margin + 120, 52, paint);

        // Reset paint for body
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(0xFF000000);
        paint.setTextSize(12f);

        y = 120;
        // Mostrar nombre de la película usando el parámetro local
        canvas.drawText("Película: " + (nombrePeliculaLocal != null ? nombrePeliculaLocal : "N/A"), x, y, paint);
        y += 18;
        canvas.drawText("Función ID: " + idFuncion, x, y, paint);
        y += 18;
        canvas.drawText("Asiento: " + a.getId(), x, y, paint);
        y += 18;
        canvas.drawText("Usuario ID: " + sessionManager.getUserId(), x, y, paint);
        y += 18;
        canvas.drawText("Fecha/Hora: " + getFormattedDateTime(), x, y, paint);
        y += 24;

        // Price box
        paint.setColor(0xFF6A21BD); // Amarillo dorado
        canvas.drawRect(x, y, x + 160, y + 40, paint);
        paint.setColor(0xFFFFEB3B);
        paint.setTextSize(14f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Precio: " + precioStrFor(a), x + 8, y + 26, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(12f);
        y += 64;

        // QR grande a la derecha
        int qrSize = 220;
        Bitmap qr = generarBitmapQr(qrContent, qrSize, qrSize);
        if (qr != null) {
            int qrX = pageInfo.getPageWidth() - margin - qrSize;
            int qrY = 120;
            canvas.drawBitmap(qr, qrX, qrY, null);

            // Código legible debajo del QR
            paint.setColor(0xFF000000);
            paint.setTextSize(10f);
            canvas.drawText("Código: " + qrContent, qrX, qrY + qrSize + 18, paint);
        } else {
            paint.setTextSize(10f);
            canvas.drawText("Código: " + qrContent, pageInfo.getPageWidth() - margin - 220, 120 + 220 + 18, paint);
        }

        // Perforation line (dashed)
        Paint dashPaint = new Paint();
        dashPaint.setColor(0xFF9E9E9E);
        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setStrokeWidth(2f);
        dashPaint.setPathEffect(new DashPathEffect(new float[]{10,6}, 0));
        float yPerforation = pageInfo.getPageHeight() - 160;
        canvas.drawLine(margin, yPerforation, pageInfo.getPageWidth() - margin, yPerforation, dashPaint);

        // Talón (below perforation)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(margin, yPerforation + 8, pageInfo.getPageWidth() - margin, pageInfo.getPageHeight() - 24, paint);
        paint.setColor(0xFF000000);
        paint.setTextSize(12f);
        canvas.drawText("TALÓN - Presentar en acceso", margin + 8, yPerforation + 36, paint);
        canvas.drawText("Order: " + (orderId != null ? orderId : "N/A"), margin + 8, yPerforation + 56, paint);

        // Small legal text
        paint.setTextSize(8f);
        paint.setColor(0xFF616161);
        canvas.drawText("No reembolsable. Presenta este ticket en taquilla.", margin + 8, pageInfo.getPageHeight() - 40, paint);
    }

    private String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String precioStrFor(Asiento a) {
        try {
            if (totalPrecio > 0 && totalBoletos > 0) {
                double precioUnit = (double) totalPrecio / (double) totalBoletos;
                return String.format(Locale.getDefault(), "%.2f MXN", precioUnit);
            }
        } catch (Exception ignored) {}
        return "5.00 MXN";
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
