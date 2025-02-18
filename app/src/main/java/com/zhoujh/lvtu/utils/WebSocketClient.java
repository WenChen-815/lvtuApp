package com.zhoujh.lvtu.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {
    public static final String TAG = "WebSocketClient";
    private final OkHttpClient client = new OkHttpClient();
    private WebSocket webSocket;

    /**
     * 连接到WebSocket服务器
     * @param url WebSocket服务器的URL
     */
    public void connect(@NonNull String url, WebSocketListener listener) {
        Request request = new Request.Builder().url(url).build();
        if (listener != null) {
            webSocket = client.newWebSocket(request, listener);
        } else{
            webSocket = client.newWebSocket(request, new mWebSocketListener());
        }
        // 关闭调度器的执行器服务，以便进程可以干净地退出
        client.dispatcher().executorService().shutdown();
    }

    /**
     * 发送文本消息到WebSocket服务器
     * @param message 要发送的文本消息
     */
    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    /**
     * 发送二进制消息到WebSocket服务器
     * @param bytes 要发送的二进制消息
     */
    public void sendMessage(ByteString bytes) {
        if (webSocket != null) {
            webSocket.send(bytes);
        }
    }

    /**
     * 关闭WebSocket连接
     * @param code 关闭状态码
     * @param reason 关闭原因
     */
    public void close(int code, String reason) {
        if (webSocket != null) {
            webSocket.close(code, reason);
        }
    }

    public static class mWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            // 连接成功后的回调
            Log.i("WebSocketListener", "WebSocket连接已打开");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            // 接收到文本消息的回调
            Log.i("WebSocketListener", "接收到消息: " + text);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, ByteString bytes) {
            // 接收到二进制消息的回调
            Log.i("WebSocketListener", "接收到消息: " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            // 连接关闭前的回调
            webSocket.close(1000, null);
            Log.i("WebSocketListener", "WebSocket连接正在关闭: " + code + " " + reason);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
            // 连接失败的回调
            t.printStackTrace();
        }
    }
}
