package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.phovl.cinemaphovlmobile.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PdfRendererActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "extra_pdf_uri";

    private ImageView pdfImage;
    private Button btnBack;
    private TextView tvPage;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private int pageIndex = 0;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_renderer);

        pdfImage = findViewById(R.id.pdfImage);
        btnBack = findViewById(R.id.btnBack);
        tvPage = findViewById(R.id.tvPage);

        // Botón volver
        btnBack.setOnClickListener(v -> finish());

        // Gesture detector para swipes (izquierda/derecha)
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 120;
            private static final int SWIPE_THRESHOLD_VELOCITY = 200;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float adx = Math.abs(dx);
                if (adx > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (dx < 0) { // swipe left -> siguiente página
                        runOnUiThread(() -> {
                            if (pdfRenderer != null && pageIndex < pdfRenderer.getPageCount() - 1) {
                                pageIndex++;
                                showPage(pageIndex);
                            }
                        });
                    } else { // swipe right -> página anterior
                        runOnUiThread(() -> {
                            if (pdfRenderer != null && pageIndex > 0) {
                                pageIndex--;
                                showPage(pageIndex);
                            }
                        });
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        // Interceptar toques sobre la imagen para detectar swipes
        pdfImage.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // Obtener URI (acepta String o Parcelable)
        Intent intent = getIntent();
        String uriStr = intent != null ? intent.getStringExtra(EXTRA_URI) : null;
        Uri uri = null;
        if (uriStr != null) {
            uri = Uri.parse(uriStr);
        } else if (intent != null) {
            uri = intent.getParcelableExtra(EXTRA_URI);
        }

        if (uri == null) {
            Toast.makeText(this, "No se recibió URI del PDF", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            if ("content".equals(uri.getScheme())) {
                parcelFileDescriptor = openFileDescriptorFromContent(uri);
            } else if ("file".equals(uri.getScheme())) {
                File f = new File(uri.getPath());
                parcelFileDescriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
            } else {
                parcelFileDescriptor = openFileDescriptorFromContent(uri);
            }

            if (parcelFileDescriptor == null) throw new Exception("No se pudo abrir descriptor");

            pdfRenderer = new PdfRenderer(parcelFileDescriptor);

            if (pdfRenderer.getPageCount() == 0) throw new Exception("PDF sin páginas");

            pageIndex = 0;
            showPage(pageIndex);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error abriendo PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private ParcelFileDescriptor openFileDescriptorFromContent(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) return null;
        File temp = new File(getCacheDir(), "temp_pdf_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.flush();
        } finally {
            try { is.close(); } catch (Exception ignored) {}
        }
        return ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private void showPage(int index) {
        if (pdfRenderer == null) return;
        if (index < 0 || index >= pdfRenderer.getPageCount()) return;

        if (currentPage != null) currentPage.close();
        currentPage = pdfRenderer.openPage(index);

        // Calcular tamaño en dp/dpi para renderizar con buena resolución
        int width = getResources().getDisplayMetrics().densityDpi / 72 * currentPage.getWidth();
        int height = getResources().getDisplayMetrics().densityDpi / 72 * currentPage.getHeight();

        int maxDim = Math.max(width, height);
        float scale = 1f;
        if (maxDim > 2048) scale = 2048f / maxDim;

        Bitmap bitmap = Bitmap.createBitmap((int)(width * scale), (int)(height * scale), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        pdfImage.setImageBitmap(bitmap);

        tvPage.setText((index + 1) + " / " + pdfRenderer.getPageCount());
    }

    @Override
    protected void onDestroy() {
        try {
            if (currentPage != null) currentPage.close();
            if (pdfRenderer != null) pdfRenderer.close();
            if (parcelFileDescriptor != null) parcelFileDescriptor.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
