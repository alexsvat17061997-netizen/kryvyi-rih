package com.kryvbasgames.kryvyirih;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
  // Гра завантажується з GitHub Pages: оновлення приходять самі.
  // Без інтернету при першому запуску відкривається копія, вшита в застосунок.
  static final String GAME = "https://alexsvat17061997-netizen.github.io/kryvyi-rih/";
  static final String LOCAL = "https://appassets.androidplatform.net/assets/index.html";
  WebView web;
  ValueCallback<Uri[]> fileCb;
  static final int PICK = 77;
  boolean fellBack = false;
  boolean loaded = false;
  final Handler h = new Handler(Looper.getMainLooper());
  void fallBack() { if (fellBack) return; fellBack = true; web.stopLoading(); web.loadUrl(LOCAL); }

  @Override protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
        if (fileCb != null) fileCb.onReceiveValue(null);
        fileCb = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try { startActivityForResult(Intent.createChooser(i, "Обери пісні"), PICK); } catch (Exception e) { fileCb = null; return false; }
        return true;
      }
    });
    web.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return loader.shouldInterceptRequest(r.getUrl());
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        String h = r.getUrl().getHost();
        if (h != null && (h.endsWith("github.io") || h.equals("appassets.androidplatform.net"))) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
        return true;
      }
      @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
        if (r.isForMainFrame()) fallBack();
      }
      @Override public void onReceivedHttpError(WebView v, WebResourceRequest r, WebResourceResponse resp) {
        if (r.isForMainFrame() && resp.getStatusCode() >= 400) fallBack();
      }
      @Override public void onPageFinished(WebView v, String url) {
        if (url != null && (url.startsWith(GAME) || url.startsWith(LOCAL))) loaded = true;
      }
    });
    if (saved != null) web.restoreState(saved); else web.loadUrl(GAME);
    h.postDelayed(new Runnable() { public void run() { if (!loaded) fallBack(); } }, 10000);
  }
  @Override protected void onActivityResult(int req, int res, Intent data) {
    super.onActivityResult(req, res, data);
    if (req != PICK || fileCb == null) return;
    Uri[] out = null;
    if (res == RESULT_OK && data != null) {
      if (data.getClipData() != null) {
        int n = data.getClipData().getItemCount(); out = new Uri[n];
        for (int k = 0; k < n; k++) out[k] = data.getClipData().getItemAt(k).getUri();
      } else if (data.getData() != null) out = new Uri[]{ data.getData() };
    }
    fileCb.onReceiveValue(out); fileCb = null;
  }
  @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
  @Override protected void onPause() { super.onPause(); web.onPause(); }
  @Override protected void onResume() { super.onResume(); web.onResume(); }
  @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
