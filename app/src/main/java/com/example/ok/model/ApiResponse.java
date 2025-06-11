// filepath: com.example.ok.model.ApiResponse.java
package com.example.ok.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    
    // Convert data to a specific class
    public <T> T getDataAs(Class<T> classOfT) {
        if (data == null) return null;
        Gson gson = new Gson();
        String json = gson.toJson(data);
        return gson.fromJson(json, classOfT);
    }
    
    // Convert data to a list of a specific class
    public <T> List<T> getDataListAs(Class<T> classOfT) {
        if (data == null) return null;
        Gson gson = new Gson();
        String json = gson.toJson(data);
        Type listType = TypeToken.getParameterized(List.class, classOfT).getType();
        return gson.fromJson(json, listType);
    }
}