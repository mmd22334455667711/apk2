package com.example.tlstest;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Collections;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class MainActivity extends Activity {

    private static final String TEST_URL = "https://www.google.com/";

    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultText = findViewById(R.id.resultText);
        Button testButton = findViewById(R.id.testButton);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultText.setText("در حال تست اتصال...");
                new TestTask().execute();
            }
        });
    }

    private OkHttpClient buildClient() throws Exception {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1)
                .build();

        return new OkHttpClient.Builder()
                .sslSocketFactory(new TLSSocketFactory(), trustManager)
                .connectionSpecs(Collections.singletonList(spec))
                .build();
    }

    private class TestTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                OkHttpClient client = buildClient();
                Request request = new Request.Builder().url(TEST_URL).build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";
                boolean ok = response.isSuccessful();
                int code = response.code();
                response.close();
                if (ok) {
                    return "SUCCESS! کد پاسخ: " + code
                            + "\n\nTLS با موفقیت کار کرد.\n\nبخشی از پاسخ:\n"
                            + body.substring(0, Math.min(200, body.length()));
                } else {
                    return "اتصال برقرار شد ولی سرور خطا داد. کد: " + code;
                }
            } catch (IOException e) {
                return "FAILED (خطا در اتصال): " + e.getClass().getSimpleName()
                        + "\n" + e.getMessage();
            } catch (Throwable t) {
                return "FAILED (خطای غیرمنتظره): " + t.getClass().getSimpleName() + " - " + t.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            resultText.setText(result);
        }
    }
}
