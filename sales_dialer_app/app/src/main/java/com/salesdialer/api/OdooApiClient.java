package com.salesdialer.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.salesdialer.model.Customer;
import com.salesdialer.model.FollowUp;
import com.salesdialer.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Odoo API客户端
 * 使用JSON-RPC协议与Odoo后台通信
 */
public class OdooApiClient {
    
    private static final String TAG = "OdooApiClient";
    private static OdooApiClient instance;
    
    private Context context;
    private OkHttpClient client;
    private Gson gson;
    
    private String baseUrl;
    private String database;
    private int userId;
    private String sessionId;
    
    private OdooApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        
        // 配置OkHttp客户端
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
        
        // 从本地存储加载配置
        loadConfig();
    }
    
    public static synchronized OdooApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new OdooApiClient(context);
        }
        return instance;
    }
    
    private void loadConfig() {
        baseUrl = PreferenceUtils.getString(context, "odoo_url", "");
        database = PreferenceUtils.getString(context, "odoo_database", "");
        userId = PreferenceUtils.getInt(context, "user_id", 0);
        sessionId = PreferenceUtils.getString(context, "session_id", "");
    }
    
    private void saveConfig() {
        PreferenceUtils.putString(context, "odoo_url", baseUrl);
        PreferenceUtils.putString(context, "odoo_database", database);
        PreferenceUtils.putInt(context, "user_id", userId);
        PreferenceUtils.putString(context, "session_id", sessionId);
    }
    
    /**
     * 登录认证
     */
    public boolean authenticate(String url, String db, String username, String password) throws Exception {
        this.baseUrl = url;
        this.database = db;
        
        // 构建认证请求
        JsonObject params = new JsonObject();
        params.addProperty("db", database);
        params.addProperty("login", username);
        params.addProperty("password", password);
        
        JsonObject request = buildJsonRpcRequest("call", params);
        
        // 发送请求
        String response = post("/web/session/authenticate", request.toString());
        JsonObject result = gson.fromJson(response, JsonObject.class);
        
        if (result.has("result")) {
            JsonObject resultObj = result.getAsJsonObject("result");
            if (resultObj.has("uid") && !resultObj.get("uid").isJsonNull()) {
                userId = resultObj.get("uid").getAsInt();
                if (resultObj.has("session_id")) {
                    sessionId = resultObj.get("session_id").getAsString();
                }
                saveConfig();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取客户列表
     */
    public List<Customer> getCustomers(int limit, int offset) throws Exception {
        // 构建搜索域
        JsonArray domain = new JsonArray();
        
        // 构建请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("model", "crm.customer");
        params.put("method", "search_read");
        params.put("args", new Object[]{domain});
        params.put("kwargs", buildKwargs(
                new String[]{"name", "phone", "wechat_name", "customer_type", "level", 
                        "status", "requirement", "remark", "intended_property", 
                        "owner_id", "follow_count", "write_date", "create_date"},
                limit,
                offset,
                "write_date asc"
        ));
        
        // 发送请求
        String response = callKw(params);
        JsonObject result = gson.fromJson(response, JsonObject.class);
        
        List<Customer> customers = new ArrayList<>();
        if (result.has("result")) {
            JsonArray records = result.getAsJsonArray("result");
            for (int i = 0; i < records.size(); i++) {
                JsonObject record = records.get(i).getAsJsonObject();
                Customer customer = gson.fromJson(record, Customer.class);
                customers.add(customer);
            }
        }
        
        return customers;
    }
    
    /**
     * 创建跟进记录
     */
    public int createFollowUp(FollowUp followUp) throws Exception {
        // 构建参数
        JsonObject vals = new JsonObject();
        vals.addProperty("customer_id", followUp.getCustomerId());
        vals.addProperty("follow_content", followUp.getFollowContent());
        vals.addProperty("result", followUp.getResult());
        if (followUp.getNextFollowTime() != null) {
            vals.addProperty("next_follow_time", followUp.getNextFollowTime());
        }
        
        JsonArray valsArray = new JsonArray();
        valsArray.add(vals);
        
        Map<String, Object> params = new HashMap<>();
        params.put("model", "crm.customer.follow");
        params.put("method", "create");
        params.put("args", new Object[]{valsArray});
        params.put("kwargs", new HashMap<>());
        
        // 发送请求
        String response = callKw(params);
        JsonObject result = gson.fromJson(response, JsonObject.class);
        
        if (result.has("result")) {
            return result.get("result").getAsInt();
        }
        
        return 0;
    }
    
    /**
     * 更新客户信息
     */
    public boolean updateCustomer(int customerId, Map<String, Object> values) throws Exception {
        JsonObject vals = gson.toJsonTree(values).getAsJsonObject();
        
        JsonArray idsArray = new JsonArray();
        idsArray.add(customerId);
        
        Map<String, Object> params = new HashMap<>();
        params.put("model", "crm.customer");
        params.put("method", "write");
        params.put("args", new Object[]{idsArray, vals});
        params.put("kwargs", new HashMap<>());
        
        // 发送请求
        String response = callKw(params);
        JsonObject result = gson.fromJson(response, JsonObject.class);
        
        return result.has("result") && result.get("result").getAsBoolean();
    }
    
    /**
     * 获取客户的跟进记录
     */
    public List<FollowUp> getFollowUps(int customerId) throws Exception {
        // 构建搜索域
        JsonArray domain = new JsonArray();
        JsonArray customerDomain = new JsonArray();
        customerDomain.add("customer_id");
        customerDomain.add("=");
        customerDomain.add(customerId);
        domain.add(customerDomain);
        
        Map<String, Object> params = new HashMap<>();
        params.put("model", "crm.customer.follow");
        params.put("method", "search_read");
        params.put("args", new Object[]{domain});
        params.put("kwargs", buildKwargs(
                new String[]{"customer_id", "follower_id", "follow_content", 
                        "follow_time", "result", "next_follow_time", "create_date"},
                100,
                0,
                "follow_time desc"
        ));
        
        // 发送请求
        String response = callKw(params);
        JsonObject result = gson.fromJson(response, JsonObject.class);
        
        List<FollowUp> followUps = new ArrayList<>();
        if (result.has("result")) {
            JsonArray records = result.getAsJsonArray("result");
            for (int i = 0; i < records.size(); i++) {
                JsonObject record = records.get(i).getAsJsonObject();
                FollowUp followUp = gson.fromJson(record, FollowUp.class);
                followUps.add(followUp);
            }
        }
        
        return followUps;
    }
    
    /**
     * 构建kwargs参数
     */
    private Map<String, Object> buildKwargs(String[] fields, int limit, int offset, String order) {
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("fields", fields);
        kwargs.put("limit", limit);
        kwargs.put("offset", offset);
        if (order != null) {
            kwargs.put("order", order);
        }
        return kwargs;
    }
    
    /**
     * 调用Odoo的call_kw接口
     */
    private String callKw(Map<String, Object> params) throws Exception {
        JsonObject request = buildJsonRpcRequest("call", params);
        return post("/web/dataset/call_kw", request.toString());
    }
    
    /**
     * 构建JSON-RPC请求
     */
    private JsonObject buildJsonRpcRequest(String method, Object params) {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("method", method);
        request.add("params", gson.toJsonTree(params));
        request.addProperty("id", System.currentTimeMillis());
        return request;
    }
    
    /**
     * 发送POST请求
     */
    private String post(String endpoint, String json) throws Exception {
        String url = baseUrl + endpoint;
        
        Log.d(TAG, "Request URL: " + url);
        Log.d(TAG, "Request Body: " + json);
        
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json
        );
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        
        // 添加session cookie
        if (sessionId != null && !sessionId.isEmpty()) {
            requestBuilder.addHeader("Cookie", "session_id=" + sessionId);
        }
        
        Request request = requestBuilder.build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code() + ": " + response.message());
            }
            
            String responseBody = response.body().string();
            Log.d(TAG, "Response: " + responseBody);
            
            return responseBody;
        }
    }
    
    // Getters
    public boolean isAuthenticated() {
        return userId > 0 && sessionId != null && !sessionId.isEmpty();
    }
    
    public int getUserId() {
        return userId;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void logout() {
        userId = 0;
        sessionId = "";
        saveConfig();
    }
}
