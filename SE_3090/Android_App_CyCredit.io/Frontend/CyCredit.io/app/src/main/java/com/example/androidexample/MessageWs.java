package cycredit.io.chat;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.concurrent.TimeUnit;

public class MessageWs extends WebSocketListener {

    public interface Listener {
        void onOpen();
        void onMessage(String text);
        void onClosed(int code, String reason);
        void onFailure(Throwable t);
    }

    private final String url;
    private final Listener listener;
    private WebSocket socket;
    private final OkHttpClient client;

    public MessageWs(String url, Listener listener){
        this.url = url;
        this.listener = listener;
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public void connect(){
        Request req = new Request.Builder().url(url).build();
        client.newWebSocket(req, this);
    }

    public void send(String text){
        if (socket != null) socket.send(text);
    }

    public void close(){
        if (socket != null) socket.close(1000, "bye");
        client.dispatcher().executorService().shutdown();
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        this.socket = webSocket;
        if (listener != null) listener.onOpen();
    }

    @Override public void onMessage(WebSocket webSocket, String text) {
        if (listener != null) listener.onMessage(text);
    }

    @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        if (listener != null) listener.onFailure(t);
    }

    @Override public void onClosed(WebSocket webSocket, int code, String reason) {
        if (listener != null) listener.onClosed(code, reason);
    }
}
