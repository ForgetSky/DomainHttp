package com.forgetsky.domainhttp.net;


import com.blankj.utilcode.util.GsonUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by wh on 2019/02/18.
 */
public class HttpUtil {

    public static MediaType TYPE_JSON = MediaType.parse("application/json;charset=utf-8");

    public static RequestBody createRequestBodyByParams(Map<String, Object> params) {
        String bodyString = GsonUtils.toJson(params);
        return createRequestBodyByParams(bodyString);
    }

    public static RequestBody createRequestBodyByParams(String bodyString) {
        return RequestBody.create(MediaType.parse("application/json;charset=utf-8"), bodyString);
    }

    public static RequestBody createRequestBodyByParams(byte[] params) {
        return RequestBody.create(MediaType.parse("application/octet-stream"), params);
    }

    public static boolean isJsonType(MediaType type) {
        return type != null && TYPE_JSON.toString().equalsIgnoreCase(type.toString());
    }

    public static byte[] inputStream2Bytes(final InputStream is) {
        if (is == null) return null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            byte[] b = new byte[10240];
            int len;
            while ((len = is.read(b)) != -1) {
                os.write(b, 0, len);
            }
            return os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
