package com.example.tlstest;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.conscrypt.Conscrypt;

import java.io.IOException;
import java.security.Security;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {

    // TODO: این آدرس رو با آدرس واقعی تانل خودت (ngrok یا cloudflared) عوض کن
    // مثال: https://random-words-1234.trycloudflare.com/
    private static final String TEST_URL = "https://example.com/";

    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // نصب Conscrypt به عنوان بالاترین اولویت security provider
        // این کار باعث می‌شه TLS مدرن مستقل از سیستم‌عامل قدیمی کار کنه
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
        } catch (Throwable t) {
            // اگه به هر دلیلی conscrypt لود نشد، بازم با موتور پیش‌فرض تلاش می‌کنیم
        }

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

    private class TestTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                OkHttpClient client = new OkHttpClient.Builder().build();
                Request request = new Request.Builder().url(TEST_URL).build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";
                boolean ok = response.isSuccessful();
                response.close();
                if (ok) {
                    return "SUCCESS! کد پاسخ: " + response.code()
                            + "\n\nTLS با موفقیت کار کرد.\n\nبخشی از پاسخ:\n"
                            + body.substring(0, Math.min(200, body.length()));
                } else {
                    return "اتصال برقرار شد ولی سرور خطا داد. کد: " + response.code();
                }
            } catch (IOException e) {
                return "FAILED (خطا در اتصال): " + e.getClass().getSimpleName()
                        + "\n" + e.getMessage()
                        + "\n\nاین یعنی TLS روی این گوشی حتی با Conscrypt هم گیر داره؛ باید بریم سراغ راه‌حل HTTP خام.";
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
