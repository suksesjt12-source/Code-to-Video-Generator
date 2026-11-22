window.startCVSRecording = function (fps, durationSeconds, outputWidth, outputHeight) {
    try {
        const canvas = document.querySelector("canvas");
        if (!canvas) {
            Android.onRecordingStatus("Ekspor gagal: halaman ini tidak memiliki elemen Canvas.");
            return;
        }
        if (!canvas.captureStream || typeof MediaRecorder === "undefined") {
            Android.onRecordingStatus("Ekspor tidak didukung oleh WebView ini. Perbarui Android System WebView.");
            return;
        }

        canvas.width = outputWidth;
        canvas.height = outputHeight;

        const candidates = [
            "video/webm;codecs=vp9",
            "video/webm;codecs=vp8",
            "video/webm"
        ];
        const mimeType = candidates.find((type) => MediaRecorder.isTypeSupported(type));
        if (!mimeType) {
            Android.onRecordingStatus("Codec WebM tidak tersedia pada perangkat ini.");
            return;
        }

        const stream = canvas.captureStream(fps);
        const recorder = new MediaRecorder(stream, {
            mimeType: mimeType,
            videoBitsPerSecond: outputWidth >= 1920 ? 8000000 : (outputWidth >= 1280 ? 5000000 : 2500000)
        });
        const chunks = [];

        recorder.ondataavailable = function (event) {
            if (event.data && event.data.size > 0) chunks.push(event.data);
        };
        recorder.onerror = function () {
            Android.onRecordingStatus("Terjadi kesalahan saat merekam. Coba turunkan resolusi atau FPS.");
            stream.getTracks().forEach((track) => track.stop());
        };
        recorder.onstop = function () {
            stream.getTracks().forEach((track) => track.stop());
            const blob = new Blob(chunks, { type: mimeType });
            const reader = new FileReader();
            reader.onloadend = function () {
                Android.saveVideo(String(reader.result), mimeType);
            };
            reader.onerror = function () {
                Android.onRecordingStatus("Gagal menyiapkan berkas video.");
            };
            reader.readAsDataURL(blob);
        };

        recorder.start(300);
        Android.onRecordingStatus("Merekam " + durationSeconds + " detik pada " + fps + " FPS...");
        window.setTimeout(function () {
            if (recorder.state !== "inactive") recorder.stop();
        }, durationSeconds * 1000);
    } catch (error) {
        Android.onRecordingStatus("Ekspor gagal: " + String(error));
    }
};
