package mg.eqima.jiramacashless.apiairtel;

import static android.content.ContentValues.TAG;

import android.os.Build;
import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class CollectionApi {

    private Authorization authorization;
    private String token = "";
    private String x_country = "MG";
    private String x_currency = "MGA";
//    private String msisdn = "330368795";
    private String msisdn = "339745621" ;
    private String urlDeTest = "https://openapiuat.airtel.africa/merchant/v1/payments/";
    //private String urlDeProduction = "https://openapi.airtel.africa/merchant/v1/payments/";

    public Map<String, String> makeReclamation(double montant, String numero) throws IOException, Exception
    {
        numero = deleteZeroInTheStart(numero);
        authorization = new Authorization();
        token = authorization.getTokenAuthorization();
        String tokenUnique = token;
        String autho = "Bearer "+tokenUnique;
       // String montants = String.valueOf(montant);
        String montants = String.valueOf(300);
        String reference = "Demande test";
        String idUnique= generatorIdUnique();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");

        String bodyRequest = "{\r\n\"reference\": \""+idUnique+"\",\r\n\"subscriber\": {\r\n\"country\": \"MG\",\r\n\"currency\": \"MGA\",\r\n\"msisdn\":"+ numero+"\r\n},\r\n\"transaction\": {\r\n\"amount\":"+ montants+",\r\n\"country\": \"MG\",\r\n\"currency\": \"MGA\",\r\n\"id\": \""+idUnique+"\"\r\n}\r\n}";
        RequestBody body = RequestBody.create(mediaType,bodyRequest);
        Request request = new Request.Builder()
                .url(urlDeTest)
                .method("POST", body)
                .addHeader("X-Country", "MG")
                .addHeader("X-Currency", "MGA")
                .addHeader("Authorization", autho)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        //Log.e("STRING",response.body().string());
        String reponse = response.body().string();
        JSONObject responseJson = new JSONObject(reponse);

        //CLOSING:
        ///response.body().close();

        String dataString = responseJson.get("data").toString();
        JSONObject dataJson = new JSONObject(dataString);
        String transactionString = dataJson.get("transaction").toString();
        JSONObject transactionJson = new JSONObject(transactionString);


        //DECLARATION DU VALINY:
        boolean boolValiny = false;
        Map<String, String> valiny = new HashMap<>();
        valiny.put("okEcritureFirebase", String.valueOf(boolValiny));

        String status = transactionJson.get("status").toString();
        System.out.println("******"+status);
        //CONDITION POUR LANCER ENQUIRY:
        if("Success.".equals(status))
        {
            String id = transactionJson.get("id").toString();
            Map<String, String> verificationEnquiry = verifEnquiry(id);
            System.out.println(verificationEnquiry);
            if(verificationEnquiry.get("codeReponse").equals("DP00800001001"))
            {
                boolValiny = true;
                String airtelMoneyId = verificationEnquiry.get("airtel_money_id");
                valiny.put("airtelMoneyId", airtelMoneyId);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valiny.replace("okEcritureFirebase", "true");
                }

            }
        }
        if(!"Success.".equals(status))
        {
            System.out.println("Ussd échouer");
        }

        return valiny;
    }

    public Map<String, String> verifEnquiry(String uniqueId) throws Exception
    {
        Enquiry enq = new Enquiry();
        System.out.println("ATO ah zao");
        String[] enquiryValiny = enq.makeEnquiryInProcess(uniqueId, token);
        String codeReponse = enquiryValiny[0];
        String message = enquiryValiny[1];
        String airtel_money_id = enquiryValiny[2];
        int compteur = 0;
        int nombre = 50;
        while(compteur<nombre)
        {
            if("DP00800001006".equals(codeReponse))
            {
                String[] valinyBoucle = enq.makeEnquiryInProcess(uniqueId, token);
                codeReponse = valinyBoucle[0];
                message = valinyBoucle[1];
                airtel_money_id = valinyBoucle[2];
                System.out.println("Code: "+valinyBoucle[0]+" || Message: "+valinyBoucle[1]);
                System.out.println(compteur);
                compteur++;
                nombre++;
            }
            if(!"DP00800001006".equals(codeReponse)){
                nombre = 10;
                compteur = 10;
            }

        }

        System.out.println("Final response:");
        System.out.println("Code:"+ codeReponse+" ==> "+message);

        Map<String, String> transactionValiny = new HashMap<>();
        transactionValiny.put("codeReponse",codeReponse);
        transactionValiny.put("airtel_money_id",airtel_money_id);

        return transactionValiny;
    }



    private static void setHeaders(HttpURLConnection httpUrlConnection, Map<String, String> headers) {
        for (String headerKey : headers.keySet()) {
            httpUrlConnection.setRequestProperty(headerKey, headers.get(headerKey));
            System.out.println(headers.get(headerKey));
            System.out.println(httpUrlConnection.getRequestProperty(headerKey));
        }
    }

    public String generatorIdUnique() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 16;
        Random random = new Random();

        String generatedString = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            generatedString = random.ints(leftLimit, rightLimit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(targetStringLength)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }

        /* System.out.println(generatedString); */
        return generatedString;
    }

    public String deleteZeroInTheStart(String numero)
    {
        String valiny = "";
        if(numero.startsWith("0"))
        {
            valiny = numero.substring(1,10);
        }
        if(!numero.startsWith("0"))
        {
            valiny = numero;
        }
        return valiny;
    }



}

