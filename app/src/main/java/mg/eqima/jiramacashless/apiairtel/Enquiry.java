package mg.eqima.jiramacashless.apiairtel;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class


Enquiry {

    Map<String, String> errorCode;
    String authorization;
    private String urlDeTest = "https://openapiuat.airtel.africa/standard/v1/payments/";
    //private String urlDeProduction = "https://openapi.airtel.africa/standard/v1/payments/";


    public Enquiry()
    {

    }

    public String[] makeEnquiryInProcess(String transactionUniqueId, String token) throws IOException, Exception
    {
        authorization = token;
        String tokenUnique = authorization;
        String autho = "Bearer "+tokenUnique;


        String urlAvecId = urlDeTest+transactionUniqueId;

        OkHttpClient client = new OkHttpClient().newBuilder().build();

        MediaType mediaType = MediaType.parse("text/plain");
        //RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url(urlAvecId)
                .method("GET", null)
                .addHeader("X-Country", "MG")
                .addHeader("X-Currency", "MGA")
                .addHeader("Authorization", autho)
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        JSONObject objectJson = new JSONObject(response.body().string());
        // CLOSING:
        //response.close();

        System.out.println("Reponse AAAAAAAAAAAAAA: "+objectJson);

        String[] valiny = new String[3];

        if(objectJson.has("status") && objectJson.has("data"))
        {
            String status = objectJson.get("status").toString();
            String data = objectJson.get("data").toString();

            JSONObject statusJson = new JSONObject(status);
            JSONObject dataJson = new JSONObject(data);

            String transactionString = dataJson.get("transaction").toString();

            JSONObject transactionJson = new JSONObject(transactionString);


            String laResponseCode = statusJson.get("response_code").toString();

            System.out.println("La response code: ");
            System.out.println(laResponseCode);

            String message = getMessageForResponseCode(laResponseCode);
            System.out.println(message);


            valiny[0] = laResponseCode;
            valiny[1] = message;
            valiny[2] = "";

            if(laResponseCode.equals("DP00800001001"))
            {
                valiny[2] = transactionJson.get("airtel_money_id").toString();
            }
        }
        else
        {
            valiny[0] = "DP00000000000";
            valiny[1] = "Canceled";
            valiny[2] = "";
        }

        return valiny;

    }

    public String getMessageForResponseCode(String responseCode)
    {
        errorCode = new HashMap<>();
        errorCode.put("DP00800001001","Success");
        errorCode.put("DP00800001002","Incorrect Pin");
        errorCode.put("DP00800001003","Exceeds withdrawal amount limit(s) / Withdrawal amount limit exceeded");
        errorCode.put("DP00800001004","Invalid Amount");
        errorCode.put("DP00800001005","Transaction ID is invalid");
        errorCode.put("DP00800001006","In process");
        errorCode.put("DP00800001007","Not enough balance");
        errorCode.put("DP00800001008","Refused");
        errorCode.put("DP00800001009","Do not honor");
        errorCode.put("DP00800001010","Transaction not permitted to Payee");
        errorCode.put("DP00800001024","Transaction Timed Out");

        String messageCode = errorCode.get(responseCode);

        return messageCode;
    }

}

