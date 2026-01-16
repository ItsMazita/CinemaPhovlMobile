package com.phovl.cinemaphovlmobile.ui.main;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.adapter.PurchasesAdapter;
import com.phovl.cinemaphovlmobile.model.PurchaseItem;
import com.phovl.cinemaphovlmobile.session.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private RecyclerView rvPurchases;
    private ProgressBar progress;
    private PurchasesAdapter adapter;
    private List<PurchaseItem> items = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadPurchases();
                } else {
                    Toast.makeText(this, "Permiso denegado: no se pueden listar PDFs", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge padding (igual que antes)
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Session
        sessionManager = new SessionManager(this);

        // UI
        TextView userName = findViewById(R.id.user_name);
        userName.setText(sessionManager.getUserName());

        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(ProfileActivity.this, com.phovl.cinemaphovlmobile.ui.auth.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // RecyclerView y progress
        rvPurchases = findViewById(R.id.rvPurchases);
        progress = findViewById(R.id.progressPurchases);

        int span = 2;
        rvPurchases.setLayoutManager(new GridLayoutManager(this, span));
        adapter = new PurchasesAdapter(items, item -> openPdf(item), this);
        rvPurchases.setAdapter(adapter);

        // Pedir permiso si es necesario (API < Q)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadPurchases();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            // Scoped storage: no se necesita permiso para MediaStore
            loadPurchases();
        }
    }

    private void loadPurchases() {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<PurchaseItem> found = findPdfPurchases(ProfileActivity.this);
            runOnUiThread(() -> {
                items.clear();
                items.addAll(found);
                adapter.notifyDataSetChanged();
                progress.setVisibility(View.GONE);
            });
        }).start();
    }

    private void openPdf(PurchaseItem item) {
        if (item == null || item.uri == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri uri = item.uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && "file".equals(uri.getScheme())) {
            File f = new File(uri.getPath());
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            intent.setDataAndType(contentUri, "application/pdf");
        } else {
            intent.setDataAndType(uri, "application/pdf");
        }

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No hay aplicación para abrir PDF", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Busca PDFs en Downloads/CinemaPHOVL.
     * Para API >= 29 usa MediaStore; para <29 usa File API.
     */
    private List<PurchaseItem> findPdfPurchases(Context ctx) {
        List<PurchaseItem> out = new ArrayList<>();
        final String targetFolder = "CinemaPHOVL";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.MediaColumns.MIME_TYPE + "=? AND " +
                    MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
            String[] selArgs = new String[] { "application/pdf", "%" + File.separator + targetFolder + "%" };

            String[] projection = new String[] {
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
            };

            try (Cursor c = ctx.getContentResolver().query(collection, projection, selection, selArgs, MediaStore.MediaColumns.DATE_MODIFIED + " DESC")) {
                if (c != null) {
                    int idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                    int nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                    int sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                    int dateIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);

                    while (c.moveToNext()) {
                        long id = c.getLong(idIdx);
                        String name = c.getString(nameIdx);
                        long size = c.getLong(sizeIdx);
                        long dateSec = c.getLong(dateIdx);
                        Uri contentUri = ContentUris.withAppendedId(collection, id);
                        String funcionId = extractFuncionFromName(name);
                        out.add(new PurchaseItem(name, contentUri, size, dateSec * 1000L, funcionId));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            File folder = new File(downloads, targetFolder);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName();
                        long size = f.length();
                        long date = f.lastModified();
                        Uri uri = Uri.fromFile(f);
                        String funcionId = extractFuncionFromName(name);
                        out.add(new PurchaseItem(name, uri, size, date, funcionId));
                    }
                }
            }
        }
        return out;
    }

    private String extractFuncionFromName(String filename) {
        try {
            if (filename == null) return null;
            String base = filename.replace(".pdf", "");
            String[] parts = base.split("_");
            for (int i = 0; i < parts.length; i++) {
                if ("funcion".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Manejo de permiso si el usuario responde manualmente desde ajustes
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // No usado porque usamos ActivityResultLauncher, pero lo dejo por compatibilidad
    }
}
