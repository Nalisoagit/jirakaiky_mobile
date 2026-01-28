package mg.eqima.jiramacashless.apiairtel;

import android.os.StrictMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Authorization {

//    private String client_id = "21eb207f-534d-4e80-939b-43d2515a9f7f";
//    private String client_secret = "20f4c576-7d11-4474-b30f-c63b371bfb85";
    private String client_id = "9eee99a7-4ea6-4ad4-bb9c-5b57b81b296d";
    private String client_secret = "53bffba2-2850-4d30-a356-9f81eb6e1d83";
    private String grant_type = "client_credentials";
    private String urlDeTest = "https://openapiuat.airtel.africa/auth/oauth2/token";
    //private String urlDeProduction = "https://openapi.airtel.africa/auth/oauth2/token";
    private String apiInfo = "{\n" +
            "    \"client_id\" : \"9eee99a7-4ea6-4ad4-bb9c-5b57b81b296d\",\n" +
            "    \"client_secret\" : \"53bffba2-2850-4d30-a356-9f81eb6e1d83\",\n" +
            "    \"grant_type\" : \"client_credentials\"\n" +
            "}";


    public String getTokenAuthorization() throws Exception
    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        URL obj = new URL(urlDeTest);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        //NAMPIANA:
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = apiInfo.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        //FIN NAMPIANA.
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.getInputStream().close();
        con.disconnect();

        String token = this.getToken(response.toString());
        System.out.println(token);

        return token;
    }

    public String getToken(String reponse) throws JSONException {
        String valiny = "";
        JSONObject objJson = new JSONObject(reponse);
        valiny = objJson.get("access_token").toString();
        return valiny;
    }

}

