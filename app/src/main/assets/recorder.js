window.startCVSRecording = function (fps, durationSeconds, outputWidth, outputHeight) {
    let stream = null;
    let recorder = null;
    try {
        const canvas = document.querySelector("canvas");
        if (!canvas) {
            Android.onRecordingStatus("Ekspor gagal: halaman ini tidak memiliki elemen Canvas.");
            return;
        }
        if (!canvas.captureStream || typeof MediaRecorder === "undefined") {
            Android.onRecordingStatus("Ekspor tidak didukung WebView ini. Perbarui Android System WebView.");
            return;
        }
        if (window.__cvsRecording) {
            Android.onRecordingStatus("Rekaman sedang berlangsung.");
            return;
        }

        const candidates = ["video/webm;codecs=vp9", "video/webm;codecs=vp8", "video/webm"];
        const mimeType = candidates.find((type) => MediaRecorder.isTypeSupported(type));
        if (!mimeType) {
            Android.onRecordingStatus("Codec WebM tidak tersedia pada perangkat ini.");
            return;
        }

        const oldWidth = canvas.width;
        const oldHeight = canvas.height;
        canvas.width = outputWidth;
        canvas.height = outputHeight;
        stream = canvas.captureStream(fps);
        recorder = new MediaRecorder(stream, {
            mimeType: mimeType,
            videoBitsPerSecond: outputWidth >= 1920 ? 6000000 : (outputWidth >= 1280 ? 3500000 : 1800000)
        });

        let writeQueue = Promise.resolve();
        let failed = false;
        window.__cvsRecording = true;
        Android.beginVideo(mimeType);
        recorder.ondataavailable = function (event) {
            if (!event.data || event.data.size === 0 || failed) return;
            writeQueue = writeQueue.then(() => new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onloadend = function () {
                    try {
                        const dataUrl = String(reader.result || "");
                        const comma = dataUrl.indexOf(",");
                        if (comma < 0) throw new Error("Potongan video tidak valid");
                        Android.appendVideoChunk(dataUrl.substring(comma + 1));
                        resolve();
                    } catch (error) {
                        reject(error);
                    }
                };
                reader.onerror = () => reject(new Error("Gagal membaca potongan video"));
                reader.readAsDataURL(event.data);
            })).catch((error) => {
                failed = true;
                Android.onRecordingStatus("Gagal memproses video: " + String(error));
            });
        };

        recorder.onerror = function () {
            failed = true;
            Android.onRecordingStatus("Terjadi kesalahan saat merekam. Coba turunkan resolusi atau FPS.");
            if (stream) stream.getTracks().forEach((track) => track.stop());
            window.__cvsRecording = false;
        };

        recorder.onstop = function () {
            if (stream) stream.getTracks().forEach((track) => track.stop());
            writeQueue.then(() => {
                if (!failed) {
                    Android.finishVideo();
                    Android.onRecordingStatus("Perekaman selesai. Menyimpan video...");
                } else {
                    Android.onRecordingStatus("Ekspor dibatalkan karena ada potongan video yang gagal diproses.");
                }
            }).catch((error) => {
                Android.onRecordingStatus("Ekspor gagal: " + String(error));
            }).finally(() => {
                canvas.width = oldWidth;
                canvas.height = oldHeight;
                window.__cvsRecording = false;
            });
        };

        recorder.start(500);
        Android.onRecordingStatus("Merekam " + durationSeconds + " detik · " + fps + " FPS · " + outputWidth + "x" + outputHeight);
        window.setTimeout(function () {
            if (recorder && recorder.state !== "inactive") recorder.stop();
        }, durationSeconds * 1000);
    } catch (error) {
        if (stream) stream.getTracks().forEach((track) => track.stop());
        window.__cvsRecording = false;
        Android.onRecordingStatus("Ekspor gagal: " + String(error));
    }
};