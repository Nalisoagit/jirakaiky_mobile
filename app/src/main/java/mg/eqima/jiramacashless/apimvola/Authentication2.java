package mg.eqima.jiramacashless.apimvola;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Authentication2 {
    private String urlDeTest = "https://devapi.mvola.mg/token";
    private String urlDeProduction = "https://api.mvola.mg/token";
    private String consumer_key = "n22LvN0mtzooYex4iyo_5MMFwpka";
    private String consumer_secret = "n3eopK325SEOnql12xeeF2lu26ka";

    public String getTokenVolley(Context context)
    {
        final String[] valiny = new String[1];
        String cKcS = this.conversionBase64();
        String autho = "Basic "+cKcS;
        System.out.println("SECRET ET KEY: "+autho);

        //CONSTRUCTION DE L'URL:
        RequestQueue requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        StringRequest theRequest = new StringRequest(Request.Method.POST, urlDeTest, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response: ", response.toString());
                try {
                    JSONObject responseOjbject = new JSONObject(response);
                    valiny[0] = responseOjbject.getString("access_token");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                System.out.println("Token:"+ valiny[0]);

            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("RequestFailed: ", error.getMessage());
            }
        }){
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("grant_type", "client_credentials");
                params.put("scope", "EXT_INT_MVOLA_SCOPE");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>(super.getHeaders());
                headers.put("Authorization", autho);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        requestQueue.add(theRequest);
        String valinyVrai = valiny[0];
        return valinyVrai;
    }

    //CONVERTIR EN BASE64:
    public String conversionBase64()
    {
        String valiny = "";
        String aEncoder = this.consumer_key+":"+this.consumer_secret;
        Base64.Encoder encoderBase64 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            encoderBase64 = Base64.getEncoder();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            valiny = encoderBase64.encodeToString(aEncoder.getBytes());
        }
        System.out.println("Encodage cK et cS: "+valiny);

        return valiny;
    }

}
