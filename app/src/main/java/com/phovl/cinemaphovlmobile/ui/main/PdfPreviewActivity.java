package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.phovl.cinemaphovlmobile.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "extra_pdf_uri";
    private static final String TAG = "PdfPreviewActivity";

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;

    private ImageView imageViewPage;
    private ProgressBar progress;
    private TextView tvPage;
    private ImageButton btnPrev, btnNext;

    private int pageIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_preview);

        imageViewPage = findViewById(R.id.pdfImage);
        progress = findViewById(R.id.pdfProgress);
        tvPage = findViewById(R.id.pdfPageText);
        btnPrev = findViewById(R.id.btnPrevPage);
        btnNext = findViewById(R.id.btnNextPage);

        Intent i = getIntent();
        Uri uri = i != null ? i.getParcelableExtra(EXTRA_URI) : null;
        if (uri == null) {
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        openRenderer(uri);
        progress.setVisibility(View.GONE);

        btnPrev.setOnClickListener(v -> showPage(pageIndex - 1));
        btnNext.setOnClickListener(v -> showPage(pageIndex + 1));
    }

    private void openRenderer(Uri uri) {
        try {
            if ("content".equals(uri.getScheme()) || "file".equals(uri.getScheme())) {
                parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            } else {
                File f = new File(uri.getPath());
                parcelFileDescriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
            }

            if (parcelFileDescriptor == null) {
                Log.e(TAG, "ParcelFileDescriptor es null");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                if (pdfRenderer.getPageCount() > 0) {
                    showPage(0);
                }
            } else {
                Log.e(TAG, "PdfRenderer requiere API 21+");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Archivo no encontrado: " + e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "Error abriendo PDF: " + e.getMessage(), e);
        }
    }

    private void showPage(int index) {
        if (pdfRenderer == null) return;
        if (index < 0 || index >= pdfRenderer.getPageCount()) return;

        if (currentPage != null) currentPage.close();

        currentPage = pdfRenderer.openPage(index);

        // Render a bitmap at page's size
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageViewPage.setImageBitmap(bitmap);

        pageIndex = index;
        tvPage.setText((pageIndex + 1) + " / " + pdfRenderer.getPageCount());

        btnPrev.setEnabled(pageIndex > 0);
        btnNext.setEnabled(pageIndex < pdfRenderer.getPageCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (currentPage != null) currentPage.close();
            if (pdfRenderer != null) pdfRenderer.close();
            if (parcelFileDescriptor != null) parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.w(TAG, "Error cerrando renderer: " + e.getMessage(), e);
        }
    }
}
