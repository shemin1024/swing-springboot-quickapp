package com.zwsoft.connector.beans;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HttpClient {
    private okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient();
    public JSONObject getWithQuery(String uri, String token ){
        Request request = new Request.Builder()
                .url(uri)
                .header("User-Agent", "test")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .get()
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (null != body) {
                return JSON.parseObject(body.string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public JSONObject postWithBody(String uri, Object obj) {
        return postWithBody(uri, obj, "");
    }

    public JSONObject postWithBody(String uri, Object obj, String token) {
        if (null == token) {
            return null;
        }
        RequestBody requestBody = RequestBody.create(JSON.toJSONString(obj),
                MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(uri)
                .header("User-Agent", "test")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .post(requestBody)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (null != body) {
                return JSON.parseObject(body.string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
