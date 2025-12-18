package cycredit.io;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class HomeActivity extends AppCompatActivity {
    
    private WebView webView;
    private boolean audioUnlocked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e("HomeActivity", "========================================");
        android.util.Log.e("HomeActivity", "=== HomeActivity.onCreate() CALLED ===");
        android.util.Log.e("HomeActivity", "========================================");
        
        // We want the system to inset content BELOW the bars.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        
        setContentView(R.layout.activity_home);

        // White icons (bars are dark)
        WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(
            getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);
        c.setAppearanceLightNavigationBars(false);

        webView = findViewById(R.id.homeWebView);
        
        // Null check for WebView
        if (webView == null) {
            android.util.Log.e("HomeActivity", "WebView not found in layout");
            finish();
            return;
        }
        
        // WebView settings for proper viewport rendering
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false); // Allow autoplay
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        // Add JavaScript interface for audio control
        webView.addJavascriptInterface(new AudioBridge(), "AndroidAudio");
        
        // Remove any previous insets listeners/padding scrims for this screen.
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // If info page loaded with autoplay flag, trigger autoplay
                if (url.contains("cycredit_info.html") && url.contains("autoplay=1")) {
                    // Small delay to ensure audio element is ready
                    view.postDelayed(() -> {
                        view.evaluateJavascript("if(window.__startIntroAudio){window.__startIntroAudio();}", null);
                    }, 150);
                }
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri != null && "app".equalsIgnoreCase(uri.getScheme())) {
                    String host = uri.getHost();
                    if ("start".equalsIgnoreCase(host)) {
                        // Navigate to Login/next screen
                        android.util.Log.e("HomeActivity", "=== app://start clicked - navigating to LoginActivity ===");
                        Intent loginIntent = new Intent(HomeActivity.this, LoginActivity.class);
                        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        android.util.Log.d("HomeActivity", "Starting LoginActivity...");
                        startActivity(loginIntent);
                        android.util.Log.d("HomeActivity", "LoginActivity started");
                        return true;
                    } else if ("info".equalsIgnoreCase(host)) {
                        // Unlock audio immediately on Info button press (user gesture)
                        unlockAudioForWebView();
                        // Load info page with autoplay flag
                        view.loadUrl("file:///android_asset/cycredit_info.html?autoplay=1");
                        return true;
                    } else if ("back".equalsIgnoreCase(host)) {
                        // Stop audio when going back
                        view.evaluateJavascript("if(window.__stopIntroAudio){window.__stopIntroAudio();}", null);
                        view.loadUrl("file:///android_asset/cycredit_homepage_fixed.html");
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // For older APIs
                if (url != null && url.startsWith("app://")) {
                    Uri uri = Uri.parse(url);
                    String host = uri.getHost();
                    if ("start".equalsIgnoreCase(host)) {
                        android.util.Log.e("HomeActivity", "=== app://start clicked (old API) - navigating to LoginActivity ===");
                        Intent loginIntent = new Intent(HomeActivity.this, LoginActivity.class);
                        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        android.util.Log.d("HomeActivity", "Starting LoginActivity...");
                        startActivity(loginIntent);
                        android.util.Log.d("HomeActivity", "LoginActivity started");
                        return true;
                    } else if ("info".equalsIgnoreCase(host)) {
                        // Unlock audio immediately on Info button press (user gesture)
                        unlockAudioForWebView();
                        // Load info page with autoplay flag
                        view.loadUrl("file:///android_asset/cycredit_info.html?autoplay=1");
                        return true;
                    } else if ("back".equalsIgnoreCase(host)) {
                        // Stop audio when going back
                        view.evaluateJavascript("if(window.__stopIntroAudio){window.__stopIntroAudio();}", null);
                        view.loadUrl("file:///android_asset/cycredit_homepage_fixed.html");
                        return true;
                    }
                }
                return false;
            }
        });
        webView.loadUrl("file:///android_asset/cycredit_homepage_fixed.html");
    }
    
    /**
     * Unlock audio for WebView by requesting audio focus.
     * This must be called from a user gesture (Info button press).
     */
    private void unlockAudioForWebView() {
        if (audioUnlocked) return;
        
        try {
            // Request audio focus to unlock WebView audio
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                int result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                );
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioUnlocked = true;
                    // Release focus immediately - we just needed to unlock
                    audioManager.abandonAudioFocus(null);
                }
            }
        } catch (Exception e) {
            // Ignore errors - audio unlock is best-effort
        }
    }
    
    /**
     * JavaScript bridge for audio control from WebView
     */
    private class AudioBridge {
        @JavascriptInterface
        public void unlockAudio() {
            runOnUiThread(() -> unlockAudioForWebView());
        }
        
        @JavascriptInterface
        public boolean isAudioUnlocked() {
            return audioUnlocked;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Pause WebView to prevent issues when activity goes to background
        if (webView != null) {
            webView.onPause();
            webView.evaluateJavascript("if(window.__stopIntroAudio){window.__stopIntroAudio();}", null);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Resume WebView when activity comes to foreground
        if (webView != null) {
            webView.onResume();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up WebView to prevent memory leaks
        if (webView != null) {
            webView.evaluateJavascript("if(window.__stopIntroAudio){window.__stopIntroAudio();}", null);
            webView.destroy();
            webView = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        // Allow normal back button behavior for HomeActivity
        super.onBackPressed();
    }
}
