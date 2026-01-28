package mg.eqima.jiramacashless.apimvola;

import android.content.Context;
import android.os.Build;
import android.util.Log;
/*
import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Request;
*/
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Tino Ran
 */
public class Authentication {

    private String urlDeTest = "https://devapi.mvola.mg/token";
    private String urlDeProduction = "https://api.mvola.mg/token";
    private String consumer_key = "lpWFNoBtBZ4elJECnXrsDKfICLUa";
    private String consumer_secret = "WXsIPhnVgyYYZuaa6CTMQDVt0uYa";

    //RECUPERATION DE TOKEN MVOLA:
    public String getToken2() throws MalformedURLException, IOException, JSONException {

        String valiny = "";
        String cKcS = this.conversionBase64();
        String autho = "Basic "+cKcS;
        System.out.println("SECRET ET KEY: "+autho);

        URL url = new URL(urlDeTest);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("Authorization", autho);
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpConn.setRequestProperty("Cache-Control", "no-cache");

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("grant_type=client_credentials&scope=EXT_INT_MVOLA_SCOPE");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();


        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        s.close();
        responseStream.close();
        httpConn.disconnect();

        System.out.println(response);
        JSONObject jsonObject = new JSONObject(response);
        String token = jsonObject.getString("access_token");
        System.out.println("Token: "+token);
        return token;

    }


    /*public String getTokenVolley(Context context)
    {
        final String[] valiny = {""};
        String cKcS = this.conversionBase64();
        String autho = "Basic "+cKcS;
        System.out.println("SECRET ET KEY: "+autho);

        //CONSTRUCTION DE L'URL:
        RequestQueue requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        StringRequest theRequest = new StringRequest(Request.Method.POST, urlDeTest, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response: ", response.toString());

                    //valiny[0] = response.getString("access_token");
                    Log.d("Token: ", response);

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
                Map<String, String> headers = super.getHeaders();
                headers.put("Authorization", autho);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        requestQueue.add(theRequest);
        String valinyVrai = valiny[0];
        return valinyVrai;
    }*/

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
