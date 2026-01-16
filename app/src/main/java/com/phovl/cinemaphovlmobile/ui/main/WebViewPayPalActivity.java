package com.phovl.cinemaphovlmobile.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.phovl.cinemaphovlmobile.R;

public class WebViewPayPalActivity extends AppCompatActivity {
    private static final String TAG = "WebViewPayPal";
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_RETURN_TO = "extra_return_to"; // opcional: url de retorno
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_paypal);

        webView = findViewById(R.id.webviewPayPal);
        Button btnCancel = findViewById(R.id.btnCancel);

        // Habilitar JS
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());

        // Interfaz para que el HTML/JS pueda notificar al app
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onPaymentCompleted(String orderId) {
                Log.d(TAG, "JS reported payment completed: " + orderId);
                notifyResultAndFinish("COMPLETED", orderId);
            }
            @JavascriptInterface
            public void onPaymentCancelled() {
                Log.d(TAG, "JS reported payment cancelled");
                notifyResultAndFinish("CANCELLED", null);
            }
        }, "AndroidPay");

        // Interceptar redirecciones (por si tu checkout redirige a URLs especiales)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                // Detecta esquemas que uses para comunicar resultado, por ejemplo:
                // paypal://success?orderId=XXX  o paypal://cancel
                if ("paypal".equals(u.getScheme())) {
                    if ("success".equals(u.getHost())) {
                        String orderId = u.getQueryParameter("orderId");
                        notifyResultAndFinish("COMPLETED", orderId);
                    } else {
                        notifyResultAndFinish("CANCELLED", null);
                    }
                    return true;
                }
                return false;
            }
        });

        // Cargar el checkout: puede ser un HTML local en assets o una URL remota.
        // Si usas HTML local: coloca file en app/src/main/assets/paypal_checkout.html
        // y carga: webView.loadUrl("file:///android_asset/paypal_checkout.html?amount=" + amount);
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        if (amount == null) amount = "0.00";

        // Ejemplo: cargar HTML local (recomendado para pruebas)
        String localUrl = "file:///android_asset/paypal_checkout.html?amount=" + Uri.encode(amount);
        webView.loadUrl(localUrl);

        btnCancel.setOnClickListener(v -> {
            notifyResultAndFinish("CANCELLED", null);
        });
    }

    private void notifyResultAndFinish(String status, @Nullable String orderId) {
        Intent result = new Intent();
        result.putExtra("paypal_status", status);
        if (orderId != null) result.putExtra("paypal_order_id", orderId);

        // Enviamos el resultado a la actividad que inició la selección (puede ser SeleccionAsientosActivity)
        // Usamos FLAG_ACTIVITY_CLEAR_TOP para que onNewIntent sea llamado si la activity ya está en stack.
        result.setAction(Intent.ACTION_VIEW);
        result.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Cambia el target si quieres: aquí lanzamos la activity principal de selección
        result.setClass(this, SeleccionAsientosActivity.class);
        startActivity(result);
        finish();
    }
}
