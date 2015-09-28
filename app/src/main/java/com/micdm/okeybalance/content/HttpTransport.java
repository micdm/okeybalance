package com.micdm.okeybalance.content;

import com.micdm.okeybalance.exceptions.ServerUnavailableException;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URL;
import java.util.Map;

public class HttpTransport {

    protected static final OkHttpClient client = getClient();

    protected static OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient();
        client.setCookieHandler(new CookieManager());
        return client;
    }

    public static String send(URL url, Map<String, String> params) {
        RequestBody body = getRequestBody(params);
        return sendRequest(url, body);
    }

    protected static RequestBody getRequestBody(Map<String, String> params) {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (Map.Entry<String, String> item: params.entrySet()) {
            builder.add(item.getKey(), item.getValue());
        }
        return builder.build();
    }

    protected static String sendRequest(URL url, RequestBody body) {
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new ServerUnavailableException();
            }
            return response.body().string();
        } catch (IOException e) {
            throw new ServerUnavailableException(e);
        }
    }
}
