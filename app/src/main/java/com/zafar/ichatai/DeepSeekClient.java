package com.zafar.ichatai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekClient {

    private static final String BASE_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient();

    public void getResponse(String query, ResponseCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "deepseek/deepseek-r1:free");

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", query);
            messages.put(msg);

            body.put("messages", messages);
            body.put("temperature", 0.9);
            body.put("top_p", 0.1);

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer " + ApiKeys.OPENROUTER_API_KEY)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "com.zafar.ichataibeta")
                    .addHeader("X-Title", "iChatAI Beta")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) { callback.onError(e); }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError(new IOException("HTTP " + response.code() + ": " + response.message()));
                        return;
                    }
                    String resp = response.body() != null ? response.body().string() : "";
                    try {
                        JSONObject json = new JSONObject(resp);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject choice0 = choices.getJSONObject(0);
                            JSONObject messageObj = choice0.optJSONObject("message");
                            String content = messageObj != null ? messageObj.optString("content", "") : "";
                            if (content == null) content = "";
                            callback.onResponse(content.trim());
                        } else {
                            callback.onError(new IOException("Empty response from model."));
                        }
                    } catch (JSONException je) {
                        callback.onError(je);
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError(e);
        }
    }
}
