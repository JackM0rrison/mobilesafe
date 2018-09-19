package com.example.mobilesafe.utils;

import android.util.Log;
import android.widget.Toast;

import com.example.mobilesafe.Activity.SplashActivity;
import com.example.mobilesafe.JavaBean.UpdateInfo;
import com.example.mobilesafe.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

public class GSonUtils {

    public static UpdateInfo handlerUpdateInfoResponse(String response){

       try{
           //JSONObject jsonObject = new JSONObject(response);
           //JSONArray jsonArray = jsonObject.getJSONArray("update");
           //JSONArray jsonArray = jsonObject.getJSONArray("update");
           //String downloadUrl = jsonObject.getString("downloadUrl");
           //Log.d("jsonObject",downloadUrl);
           JSONArray jsonArray = new JSONArray(response);
           String updateInfoContent =jsonArray.getJSONObject(0).toString();
           return new Gson().fromJson(updateInfoContent, UpdateInfo.class);
        }catch(JSONException e){
            e.printStackTrace();
        }

        return null;
    }
}
