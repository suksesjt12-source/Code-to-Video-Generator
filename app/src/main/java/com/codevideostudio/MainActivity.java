package com.codevideostudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 12, 23);
    private static final int PANEL = Color.rgb(17, 25, 42);
    private static final int TEXT = Color.rgb(235, 242, 255);
    private static final int MUTED = Color.rgb(151, 166, 190);
    private static final int ACCENT = Color.rgb(91, 108, 255);

    private EditText editor;
    private WebView preview;
    private TextView status;
    private Spinner resolutionSpinner;
    private Spinner fpsSpinner;
    private Spinner durationSpinner;
    private boolean pageReady = false;

    private static final String SAMPLE_HTML =
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "  <title>Neon Orbit</title>\n" +
            "  <style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#070b18}" +
            "canvas{display:block;width:100%;height:100%}</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<canvas id=\"scene\"></canvas>\n" +
            "<script>\n" +
            "const canvas = document.getElementById('scene');\n" +
            "const ctx = canvas.getContext('2d');\n" +
            "canvas.width = 1280; canvas.height = 720;\n" +
            "let tick = 0;\n" +
            "function draw() {\n" +
            "  tick += 0.012;\n" +
            "  const w = canvas.width, h = canvas.height;\n" +
            "  ctx.fillStyle = '#070b18'; ctx.fillRect(0, 0, w, h);\n" +
            "  const cx = w / 2, cy = h / 2;\n" +
            "  for (let i = 0; i < 9; i++) {\n" +
            "    const a = tick * (i % 2 ? -1 : 1) + i * 0.7;\n" +
            "    const r = 65 + i * 25 + Math.sin(tick * 2 + i) * 10;\n" +
            "    ctx.beginPath(); ctx.arc(cx, cy, r, a, a + Math.PI * 1.55);\n" +
            "    ctx.strokeStyle = i % 2 ? '#38d9ff' : '#a78bfa';\n" +
            "    ctx.lineWidth = 5 + (i % 3); ctx.shadowBlur = 18;\n" +
            "    ctx.shadowColor = ctx.strokeStyle; ctx.stroke();\n" +
            "  }\n" +
            "  ctx.shadowBlur = 0; ctx.fillStyle = '#ffffff';\n" +
            "  ctx.font = 'bold 42px sans-serif'; ctx.textAlign = 'center';\n" +
            "  ctx.fillText('CODE  /  MOTION', cx, h - 85);\n" +
            "  requestAnimationFrame(draw);\n" +
            "}\n" +
            "draw();\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildInterface();
        loadSample();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private Button makeButton(String title, int color) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        return button;
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vertical();
        root.setPadding(dp(16), dp(14), dp(16), dp(22));
        scroll.addView(root);

        root.addView(label("CODEVIDEO STUDIO", 22, TEXT, true));
        TextView subtitle = label("Create animations with code. Preview, record, and save offline.", 12, MUTED, false);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        root.addView(subtitle);

        LinearLayout previewHeader = new LinearLayout(this);
        previewHeader.setGravity(Gravity.CENTER_VERTICAL);
        previewHeader.addView(label("LIVE PREVIEW", 13, TEXT, true), new LinearLayout.LayoutParams(0, dp(36), 1));
        previewHeader.addView(label("WebView · Canvas", 11, MUTED, false));
        root.addView(previewHeader);

        preview = new WebView(this);
        preview.setBackgroundColor(Color.rgb(5, 8, 17));
        preview.getSettings().setJavaScriptEnabled(true);
        preview.getSettings().setDomStorageEnabled(false);
        preview.getSettings().setAllowFileAccess(false);
        preview.getSettings().setAllowContentAccess(false);
        preview.setWebChromeClient(new WebChromeClient());
        preview.addJavascriptInterface(new StudioBridge(), "Android");
        preview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                injectRecorder();
                setStatus("Preview siap. Jalankan kode untuk memperbarui tampilan.");
            }
        });
        root.addView(preview, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(250)));

        TextView editorTitle = label("HTML / CSS / JAVASCRIPT", 13, TEXT, true);
        editorTitle.setPadding(0, dp(16), 0, dp(8));
        root.addView(editorTitle);

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(12);
        editor.setTextColor(TEXT);
        editor.setHintTextColor(MUTED);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setHint("Paste your HTML, CSS and JavaScript here...");
        editor.setPadding(dp(12), dp(12), dp(12), dp(12));
        editor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(62, 78, 111)));
        editor.setMinLines(11);
        editor.setHorizontallyScrolling(true);
        editor.setText(SAMPLE_HTML);
        root.addView(editor, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(260)));

        LinearLayout actions1 = new LinearLayout(this);
        actions1.setGravity(Gravity.CENTER);
        actions1.setPadding(0, dp(8), 0, 0);
        Button run = makeButton("Run Preview", ACCENT);
        Button sample = makeButton("Contoh", Color.rgb(45, 61, 88));
        actions1.addView(run, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions1.addView(sample, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(actions1);
        run.setOnClickListener(v -> renderPreview());
        sample.setOnClickListener(v -> loadSample());

        TextView exportTitle = label("VIDEO EXPORT", 13, TEXT, true);
        exportTitle.setPadding(0, dp(18), 0, dp(8));
        root.addView(exportTitle);

        LinearLayout settings = new LinearLayout(this);
        settings.setGravity(Gravity.CENTER_VERTICAL);
        resolutionSpinner = addSetting(settings, "Resolusi", new String[]{"480p · 854x480", "720p · 1280x720", "1080p · 1920x1080"});
        fpsSpinner = addSetting(settings, "Frame rate", new String[]{"24 FPS", "30 FPS", "60 FPS"});
        durationSpinner = addSetting(settings, "Durasi", new String[]{"5 detik", "10 detik", "15 detik", "30 detik"});
        root.addView(settings);

        LinearLayout actions2 = new LinearLayout(this);
        actions2.setPadding(0, dp(8), 0, 0);
        Button record = makeButton("REKAM VIDEO", Color.rgb(0, 145, 120));
        actions2.addView(record, new LinearLayout.LayoutParams(0, dp(50), 1));
        root.addView(actions2);
        record.setOnClickListener(v -> startRecording());

        LinearLayout actions3 = new LinearLayout(this);
        actions3.setPadding(0, dp(6), 0, 0);
        Button save = makeButton("Simpan Proyek", Color.rgb(45, 61, 88));
        Button open = makeButton("Buka Proyek", Color.rgb(45, 61, 88));
        actions3.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1));
        actions3.addView(open, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(actions3);
        save.setOnClickListener(v -> saveProjectDialog());
        open.setOnClickListener(v -> openProjectDialog());

        status = label("Siap. Semua proses berjalan secara lokal di perangkat.", 11, MUTED, false);
        status.setPadding(dp(2), dp(14), dp(2), dp(4));
        root.addView(status);

        setContentView(scroll);
    }

    private Spinner addSetting(LinearLayout parent, String title, String[] options) {
        LinearLayout column = vertical();
        column.setPadding(dp(2), 0, dp(2), 0);
        column.addView(label(title, 10, MUTED, true));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        column.addView(spinner, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        parent.addView(column, new LinearLayout.LayoutParams(0, dp(72), 1));
        return spinner;
    }

    private void setStatus(String message) {
        if (status != null) status.setText(message);
    }

    private void loadSample() {
        editor.setText(SAMPLE_HTML);
        renderPreview();
    }

    private void renderPreview() {
        if (preview == null || editor == null) return;
        pageReady = false;
        String html = editor.getText().toString();
        if (html.trim().isEmpty()) {
            setStatus("Editor masih kosong.");
            return;
        }
        preview.loadDataWithBaseURL("https://codevideo.local/", html, "text/html", "UTF-8", null);
        setStatus("Memuat preview...");
    }

    private void injectRecorder() {
        try {
            InputStream input = getAssets().open("recorder.js");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            input.close();
            String script = output.toString("UTF-8");
            preview.evaluateJavascript(script, null);
        } catch (Exception error) {
            setStatus("Gagal menyiapkan renderer: " + error.getMessage());
        }
    }

    private void startRecording() {
        if (!pageReady) {
            setStatus("Tunggu sampai preview selesai dimuat.");
            return;
        }
        int width = 854;
        int height = 480;
        int res = resolutionSpinner.getSelectedItemPosition();
        if (res == 1) { width = 1280; height = 720; }
        if (res == 2) { width = 1920; height = 1080; }

        int[] fpsValues = {24, 30, 60};
        int fps = fpsValues[fpsSpinner.getSelectedItemPosition()];
        int[] durations = {5, 10, 15, 30};
        int duration = durations[durationSpinner.getSelectedItemPosition()];

        String command = "window.startCVSRecording(" + fps + "," + duration + "," + width + "," + height + ")";
        preview.evaluateJavascript(command, null);
    }

    private File projectDirectory() {
        File directory = new File(getFilesDir(), "projects");
        if (!directory.exists()) directory.mkdirs();
        return directory;
    }

    private String safeName(String name) {
        String clean = name == null ? "Project" : name.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (clean.isEmpty()) clean = "Project";
        return clean.length() > 40 ? clean.substring(0, 40) : clean;
    }

    private void saveProjectDialog() {
        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("Nama proyek");
        nameInput.setText("MyAnimation");
        new AlertDialog.Builder(this)
                .setTitle("Simpan proyek HTML")
                .setView(nameInput)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String name = safeName(nameInput.getText().toString());
                    File target = new File(projectDirectory(), name + ".html");
                    try (FileOutputStream stream = new FileOutputStream(target)) {
                        stream.write(editor.getText().toString().getBytes("UTF-8"));
                        setStatus("Proyek disimpan: " + target.getName());
                        Toast.makeText(this, "Proyek tersimpan", Toast.LENGTH_SHORT).show();
                    } catch (Exception error) {
                        setStatus("Gagal menyimpan proyek: " + error.getMessage());
                    }
                }).show();
    }

    private void openProjectDialog() {
        File[] files = projectDirectory().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".html"));
        if (files == null || files.length == 0) {
            Toast.makeText(this, "Belum ada proyek tersimpan.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
        new AlertDialog.Builder(this)
                .setTitle("Buka proyek")
                .setItems(names, (dialog, which) -> {
                    try (FileInputStream input = new FileInputStream(files[which]);
                         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                        editor.setText(output.toString("UTF-8"));
                        renderPreview();
                    } catch (Exception error) {
                        setStatus("Gagal membuka proyek: " + error.getMessage());
                    }
                }).show();
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    private class StudioBridge {
        @JavascriptInterface
        public void onRecordingStatus(String message) {
            runOnUiThread(() -> setStatus(message));
        }

        @JavascriptInterface
        public void saveVideo(String dataUrl, String mimeType) {
            runOnUiThread(() -> setStatus("Menyiapkan file video..."));
            try {
                int comma = dataUrl.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException("Data video tidak valid");
                byte[] video = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                String filename = "CodeVideo_" + timestamp() + ".webm";
                Uri savedUri;

                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    values.put(MediaStore.Downloads.MIME_TYPE, "video/webm");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CodeVideoStudio");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);
                    savedUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (savedUri == null) throw new IllegalStateException("Tidak bisa membuat file di Downloads");
                    try (OutputStream output = getContentResolver().openOutputStream(savedUri)) {
                        if (output == null) throw new IllegalStateException("Stream output tidak tersedia");
                        output.write(video);
                        output.flush();
                    }
                    ContentValues complete = new ContentValues();
                    complete.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(savedUri, complete, null, null);
                } else {
                    File directory = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                    if (directory == null) throw new IllegalStateException("Folder penyimpanan tidak tersedia");
                    if (!directory.exists()) directory.mkdirs();
                    File target = new File(directory, filename);
                    try (FileOutputStream output = new FileOutputStream(target)) {
                        output.write(video);
                    }
                    savedUri = Uri.fromFile(target);
                }

                runOnUiThread(() -> {
                    setStatus("Video tersimpan: Download/CodeVideoStudio/" + filename);
                    Toast.makeText(MainActivity.this, "Video berhasil disimpan", Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("Gagal menyimpan video: " + error.getMessage()));
            }
        }
    }

    @Override
    public void onDestroy() {
        if (preview != null) {
            preview.removeJavascriptInterface("Android");
            preview.destroy();
            preview = null;
        }
        super.onDestroy();
    }
}
