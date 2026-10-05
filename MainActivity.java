package com.abualjees.player;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int REQ_PICK = 101;
    private WebView web;
    private FrameLayout root;
    private ValueCallback<Uri[]> pending;
    private View customView;
    private WebChromeClient.CustomViewCallback customCb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            // The key part: open Android's document picker (ACTION_OPEN_DOCUMENT) instead of the photo picker,
            // so the web page receives the REAL file name (e.g. الهوبيت_رحلة_غير_متوقعة 4K.mp4), not a number.
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pending != null) pending.onReceiveValue(null);
                pending = cb;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                ArrayList<String> mimes = new ArrayList<>();
                if (p.getAcceptTypes() != null) {
                    for (String t : p.getAcceptTypes()) {
                        if (t != null && t.contains("/")) mimes.add(t);
                    }
                }
                if (!mimes.isEmpty()) i.putExtra(Intent.EXTRA_MIME_TYPES, mimes.toArray(new String[0]));
                if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(i, REQ_PICK);
                } catch (Exception e) {
                    pending = null;
                    cb.onReceiveValue(null);
                    return false;
                }
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) { cb.onCustomViewHidden(); return; }
                customView = view; customCb = cb;
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                web.setVisibility(View.GONE);
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                root.removeView(customView);
                customView = null;
                web.setVisibility(View.VISIBLE);
                if (customCb != null) customCb.onCustomViewHidden();
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK || pending == null) return;
        Uri[] out = null;
        if (res == RESULT_OK && data != null) {
            ArrayList<Uri> list = new ArrayList<>();
            ClipData cd = data.getClipData();
            if (cd != null) {
                for (int k = 0; k < cd.getItemCount(); k++) list.add(cd.getItemAt(k).getUri());
            } else if (data.getData() != null) {
                list.add(data.getData());
            }
            out = list.toArray(new Uri[0]);
        }
        pending.onReceiveValue(out);
        pending = null;
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            web.getWebChromeClient().onHideCustomView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() { super.onPause(); }
}
