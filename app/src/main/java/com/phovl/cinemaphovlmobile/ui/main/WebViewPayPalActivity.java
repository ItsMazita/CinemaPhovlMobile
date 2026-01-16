package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.phovl.cinemaphovlmobile.R;

public class WebViewPayPalActivity extends AppCompatActivity {
    private static final String TAG = "WebViewPayPal";
    public static final String EXTRA_AMOUNT = "extra_amount";
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_paypal);

        webView = findViewById(R.id.webviewPayPal);
        Button btnCancel = findViewById(R.id.btnCancel);

        // Configuración WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebChromeClient(new WebChromeClient());

        // Interfaz JS
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onPaymentCompleted(String orderId) {
                Log.d(TAG, "JS reported payment completed: " + orderId);
                // Devolver resultado al caller (SeleccionAsientosActivity)
                notifyResultAndFinish("COMPLETED", orderId);
            }

            @JavascriptInterface
            public void onPaymentCancelled() {
                Log.d(TAG, "JS reported payment cancelled");
                notifyResultAndFinish("CANCELLED", null);
            }
        }, "AndroidPay");

        // Interceptar esquemas paypal:// (compatibilidad con versiones nuevas y antiguas)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                return handlePaypalScheme(u);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri u = Uri.parse(url);
                return handlePaypalScheme(u);
            }

            private boolean handlePaypalScheme(Uri u) {
                if (u == null) return false;
                if ("paypal".equals(u.getScheme())) {
                    if ("success".equals(u.getHost())) {
                        String orderId = u.getQueryParameter("orderId");
                        Log.d(TAG, "Intercepted paypal://success orderId=" + orderId);
                        notifyResultAndFinish("COMPLETED", orderId);
                    } else {
                        Log.d(TAG, "Intercepted paypal scheme, host=" + u.getHost());
                        notifyResultAndFinish("CANCELLED", null);
                    }
                    return true;
                }
                return false;
            }
        });

        // Cargar HTML local
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        if (amount == null) amount = "0.00";
        String localUrl = "file:///android_asset/paypal_checkout.html?amount=" + Uri.encode(amount);
        webView.loadUrl(localUrl);

        btnCancel.setOnClickListener(v -> notifyResultAndFinish("CANCELLED", null));
    }

    /**
     * Devuelve el resultado al activity que llamó con startActivityForResult.
     * Usa setResult(...) en lugar de lanzar otra Activity.
     */
    private void notifyResultAndFinish(String status, @Nullable String orderId) {
        Log.d(TAG, "notifyResultAndFinish: status=" + status + ", orderId=" + orderId);

        Intent result = new Intent();
        result.putExtra("paypal_status", status);
        if (orderId != null && !orderId.isEmpty()) {
            result.putExtra("paypal_order_id", orderId);
        }

        // Elegir resultCode según el estado
        if ("COMPLETED".equals(status)) {
            setResult(RESULT_OK, result);
        } else if ("CANCELLED".equals(status)) {
            setResult(RESULT_CANCELED, result);
        } else {
            setResult(RESULT_FIRST_USER, result);
        }

        finish();
    }
}
