import contextlib
import io
import html

def run_code(source):
    output = io.StringIO()
    scope = {"__name__": "__main__"}
    try:
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            exec(compile(source, "<CodeVideoStudio>", "exec"), scope, scope)
        page = scope.get("HTML")
        if isinstance(page, str) and page.strip():
            return page
        result = output.getvalue().strip() or "Selesai. Untuk menampilkan animasi, tetapkan HTML = r\"\"\"...HTML/CSS/JS...\"\"\"."
        return "<!doctype html><meta charset='utf-8'><body style='background:#08101f;color:#eaf2ff;font:15px monospace;padding:20px'><h2>Python Output</h2><pre>" + html.escape(result) + "</pre></body>"
    except Exception as exc:
        return "<!doctype html><meta charset='utf-8'><body style='background:#180b12;color:#ffdce5;font:14px monospace;padding:20px'><h2>Python Error</h2><pre>" + html.escape(repr(exc)) + "</pre></body>"
