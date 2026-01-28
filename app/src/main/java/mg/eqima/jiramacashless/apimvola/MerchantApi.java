package mg.eqima.jiramacashless.apimvola;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

import mg.eqima.jiramacashless.Payement;

/**
 *
 * @author Tino Ran
 */
public class MerchantApi {
    private String urlSandBox = "https://devapi.mvola.mg";
    private String urlProduction = "https://api.mvola.mg";

    private String urlForInitiateTransaction = "/mvola/mm/transactions/type/merchantpay/1.0.0/";
    private String urlForTransactionDetails = "/mvola/mm/transactions/type/merchantpay/1.0.0/"; //{{transID}}
    private String urlForTransactionStatus = "/mvola/mm/transactions/type/merchantpay/1.0.0/status/"; //{{serverCorrelationId}}

    private String merchantNumber = "0343500004";

    private String tokenTemp = "";

    public MerchantApi()
    {

    }

    public MerchantApi(String token)
    {
        this.tokenTemp = token;
    }

    public String getToken()
    {
        return this.tokenTemp;
    }


    public String initiateTransaction(String customerNumber, String amountS) throws IOException, JSONException {
        //POST:
        JSONObject valinyJson = new JSONObject();

        //The amount of the transaction into double format:
        int amount = Integer.valueOf(amountS);
        String dateNow = this.getDateNow();
        String descriptionText = "Paiement";

        //GET BEARER TOKEN AND UUID to identify the transaction:
        //Authentication authForToken = new Authentication();
        String bearerToken = "Bearer "+this.tokenTemp;
        System.out.println("TTTTTTTTTTTTT=>"+ bearerToken);
        String uuid = this.uuidGenerator();

        //CONSTRUCTION DATA-RAW:
        String data = "{\r\n\"amount\" : \""+amount+"\","
                + "\r\n\"currency\" : \"Ar\","
                + "\r\n\"descriptionText\" : \""+descriptionText+"\","
                + "\r\n\"requestingOrganisationTransactionReference\" : \""+uuid+"\","
                + "\r\n\"requestDate\" : \""+dateNow+"\","
                + "\r\n\"originalTransactionReference\" : \""+uuid+"\","
                + "\r\n\"debitParty\" : [{\"key\": \"msisdn\", \"value\": \""+customerNumber+"\"}],"
                + "\r\n\"creditParty\" : [{\"key\": \"msisdn\", \"value\": \""+merchantNumber+"\"}],"
                + "\r\n\"metadata\" : [{\"key\": \"partnerName\", \"value\": \"EqimaSolution\"}]"
                + "\r\n}";

        JSONObject jsonBody = new JSONObject(data);

        //CONSTRUCT THE URL OF THE CONNECTION:
        URL url = new URL(urlSandBox+urlForInitiateTransaction);
        HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("Version", "1.0");
        httpConn.setRequestProperty("X-CorrelationID", uuid);
        httpConn.setRequestProperty("UserLanguage", "MG");
        httpConn.setRequestProperty("UserAccountIdentifier", "msisdn;"+merchantNumber);
        httpConn.setRequestProperty("partnerName","EqimaSolution");
        httpConn.setRequestProperty("Content-Type", "application/json");
        httpConn.setRequestProperty("Accept", "application/json");
        httpConn.setRequestProperty("Authorization", bearerToken);
        httpConn.setRequestProperty("Cache-Control", "no-cache");

        httpConn.setDoOutput(true);
        //httpConn.setChunkedStreamingMode(0);
        httpConn.setDoInput(true);

        System.out.println("DATA RAW: "+data);

        byte[] postDataBytes = data.getBytes("UTF-8");

        try(OutputStream os =  httpConn.getOutputStream())
        {
            os.write(postDataBytes);
            os.flush();
            os.close();
        }
        catch (Exception e)
        {
            Log.e("ERROR OUTPUTSTREAM: ", e.getMessage());
        }

        /*DataOutputStream dos = new DataOutputStream(httpConn.getOutputStream());
        dos.writeUTF(data);
        dos.flush();*/

       /* OutputStreamWriter writer = new OutputStreamWriter(os);
        writer.write(data);
        writer.flush();
        writer.close();*/
        /*OutputStream out = new BufferedOutputStream(httpConn.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                out, "UTF-8"));
        writer.write(jsonBody.toString());
        writer.flush();*/


        //System.out.println("OOOOOUTPUT STREAM: "+httpConn.getOutputStream().toString());
        // Log.e("TAG", httpConn.getErrorStream().toString());

        System.out.println(httpConn.getResponseMessage());

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        s.close();
        System.out.println(response);
        valinyJson = new JSONObject(response);

        String serverCorrelationId = "";
        if(valinyJson.get("status").equals("pending"))
        {
            serverCorrelationId = valinyJson.getString("serverCorrelationId");
        }
        responseStream.close();
        httpConn.disconnect();

        return serverCorrelationId;
    }
    /************************ GET TRANSACTION STATUS: *********************************/
    public Map<String, String> getTransactionStatus(String serverCorrelationId) throws JSONException, IOException {
        //GET BEARER TOKEN AND UUID to identify the transaction:
        Authentication authForToken = new Authentication();
        //Authentication authForToken = new Authentication();
        String bearerToken = "Bearer "+this.tokenTemp;
        String uuid = uuidGenerator();

        //serverCorrelationId = "dc3d282d-5ac4-4859-afc4-1a9fb1c310da";

        //CREATION DE LA CONNEXION:
        URL url = new URL(urlSandBox+urlForTransactionStatus+serverCorrelationId);
        HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        httpConn.setRequestProperty("Version", "1.0");
        httpConn.setRequestProperty("X-CorrelationID", uuid);
        httpConn.setRequestProperty("UserLanguage", "MG");
        httpConn.setRequestProperty("UserAccountIdentifier", "msisdn;"+merchantNumber);
        httpConn.setRequestProperty("partnerName","EqimaSolution");
        httpConn.setRequestProperty("Content-Type", "application/json");
        httpConn.setRequestProperty("Accept", "application/json");
        httpConn.setRequestProperty("Authorization", bearerToken);
        httpConn.setRequestProperty("Cache-Control", "no-cache");

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        s.close();
        System.out.println(response);
        JSONObject valinyJson = new JSONObject(response);
        responseStream.close();
        httpConn.disconnect();
        String status = valinyJson.getString("status");
        String objectReference = valinyJson.getString("objectReference");
        Map<String, String> statusOfTransaction = new HashMap<>();
        statusOfTransaction.put("status", status);
        statusOfTransaction.put("objectReference", objectReference);
        return statusOfTransaction;
    }

    /******************************** GET TRANSACTION DETAILS: ***************************/
    public JSONObject getTransactionDetail(String transId) throws JSONException, IOException {
        //GET BEARER TOKEN AND UUID to identify the transaction:
        //Authentication authForToken = new Authentication();
        String bearerToken = "Bearer "+this.tokenTemp;
        String uuid = uuidGenerator();

        //CREATION DE LA CONNEXION:
        URL url = new URL(urlSandBox+urlForTransactionDetails+transId);
        HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        httpConn.setRequestProperty("Version", "1.0");
        httpConn.setRequestProperty("X-CorrelationID", uuid);
        httpConn.setRequestProperty("UserLanguage", "MG");
        httpConn.setRequestProperty("UserAccountIdentifier", "msisdn;"+merchantNumber);
        httpConn.setRequestProperty("partnerName","EqimaSolution");
        httpConn.setRequestProperty("Content-Type", "application/json");
        httpConn.setRequestProperty("Accept", "application/json");
        httpConn.setRequestProperty("Authorization", bearerToken);
        httpConn.setRequestProperty("Cache-Control", "no-cache");

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        responseStream.close();
        httpConn.disconnect();
        System.out.println(response);
        JSONObject valinyJson = new JSONObject(response);

        return valinyJson;
    }

    //METHODE UTILISANT VOLLEY:
    public void makePayementVolley(String customerNumber, int amountS, Context context) throws JSONException, IOException, AuthFailureError {
        //Information that we need:
        String uuid = uuidGenerator();
        String dateNow = this.getDateNow();
        String descriptionText = "Paiement cashless";


        //GET BEARER TOKEN AND UUID to identify the transaction:
        Authentication authForToken = new Authentication();
        String tokenRecup = new Authentication2().getTokenVolley(context);
        System.out.println("Token recuperé:"+ tokenRecup);
        String bearerToken = "Bearer "+"eyJ4NXQiOiJPRE5tWkRFMll6UTRNVEkxTVRZME1tSmhaR00yTUdWa1lUZGhOall5TWpnM01XTmpNalJqWWpnMll6bGpNRGRsWWpZd05ERmhZVGd6WkRoa1lUVm1OZyIsImtpZCI6Ik9ETm1aREUyWXpRNE1USTFNVFkwTW1KaFpHTTJNR1ZrWVRkaE5qWXlNamczTVdOak1qUmpZamcyWXpsak1EZGxZall3TkRGaFlUZ3paRGhrWVRWbU5nX1JTMjU2IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJyYW5kcmlhbWFyb3ZlbG90aW5vQGdtYWlsLmNvbUBjYXJib24uc3VwZXIiLCJhdXQiOiJBUFBMSUNBVElPTiIsImF1ZCI6Imk2bTlCbEJmY294dnMxbEpEbHhEdDdoa0ZaZ2EiLCJuYmYiOjE2NzEwMzM2MTEsImF6cCI6Imk2bTlCbEJmY294dnMxbEpEbHhEdDdoa0ZaZ2EiLCJzY29wZSI6IkVYVF9JTlRfTVZPTEFfU0NPUEUiLCJpc3MiOiJodHRwczpcL1wvYXBpbS5wcmVwLnRlbG1hLm1nOjk0NDNcL29hdXRoMlwvdG9rZW4iLCJleHAiOjE2NzEwMzcyMTEsImlhdCI6MTY3MTAzMzYxMSwianRpIjoiNjk4OTgxMjYtMmVjNC00NmY4LTk2YzUtYTUzNDBhMTc2MGEyIn0.ro8c8mJoJ0OxBDzAOu7OtmZIuTDoRAvQiAkh21v4z4z-fWOJE0gPkkqov78wk6ZQMdNLaQvlKuT6DQfs3SZimWkVOeIdQS-m_olOMv9VFSgefOWEH2btpDEajDv68sTCE0ROMvko3le0LjlvDW_MJaeZndl_jr_nI4bQPHZtZX1fZnEWve0s6rQvGkKDE1yfLlOl6rD22m0VKhtJCJSUMVamSo-SFc2R3HgcLRfyB6w_SONGpMpX4p5uLI_dRgb2MIVT_rC3ul77nw0WQTC65dogeqQYucNhmZfZr6zPJw-jyX8dEL5U0Ke3bnv0thNuvneHdmYko1i6JdNjPZrZ1Q";

        //CONSTRUCTION OF THE HEADER MAP:
        Map<String , String> headerMap = new HashMap();

        //PARTI DEBIT:
        JSONObject jsonObjectDebit = new JSONObject();
        jsonObjectDebit.put("key","msisdn");
        jsonObjectDebit.put("value",customerNumber);

        JSONArray jsonArrayDebit = new JSONArray();
        jsonArrayDebit.put(0,jsonObjectDebit);
        //PARTI CREDIT:
        JSONObject jsonObjectCredit = new JSONObject();
        jsonObjectCredit.put("key","msisdn");
        jsonObjectCredit.put("value",merchantNumber);

        JSONArray jsonArrayCredit = new JSONArray();
        jsonArrayCredit.put(0,jsonObjectCredit);

        //PARTNER NAME:
        JSONObject jsonObjectMetadata = new JSONObject();
        jsonObjectMetadata.put("key","partnerName");
        jsonObjectMetadata.put("value","Eqimasolution");

        JSONArray jsonArrayPartnerName = new JSONArray();
        jsonArrayPartnerName.put(0,jsonObjectMetadata);

        //CONSTRUCTION DU VRAI BODY:
        JSONObject jsonRequestBody = new JSONObject();
        jsonRequestBody.put("amount", ""+Integer.parseInt(String.valueOf(amountS))+"");
        jsonRequestBody.put("currency", "Ar");
        jsonRequestBody.put("descriptionText", "Paiementcashless");
        jsonRequestBody.put("requestingOrganisationTransactionReference", uuid);
        jsonRequestBody.put("requestDate", dateNow);
        jsonRequestBody.put("originalTransactionReference", uuid);
        jsonRequestBody.put("debitParty", jsonArrayDebit);
        jsonRequestBody.put("creditParty", jsonArrayCredit);
        jsonRequestBody.put("metadata", jsonArrayPartnerName);

        //CONSTRUCTION OF THE URL AND ETC:
        String urlPaiement = urlSandBox+urlForInitiateTransaction;
        RequestQueue requestQueue = Volley.newRequestQueue(context.getApplicationContext());

        System.out.println("BOOOOODY: "+ jsonRequestBody);

        StringRequest theRequest = new StringRequest(Request.Method.POST, urlPaiement, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response=> ", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.e("Failed: ", error.getMessage());
            }
        }){
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {


                if (response.data == null || response.data.length == 0) {
                    return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));
                } else {
                    return super.parseNetworkResponse(response);
                }

            }
            @Override
            public byte[] getBody() {
                try {
                    return jsonRequestBody == null ? null : jsonRequestBody.toString().getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", jsonRequestBody, "utf-8");
                    return null;
                }
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>(super.getHeaders());

                headers.put("Version","1.0");
                headers.put("X-CorrelationID",uuid);
                headers.put("Content-Type","application/json");
                headers.put("UserLanguage","MG");
                headers.put("UserAccountIdentifier","msisdn;"+merchantNumber);
                headers.put("partnerName","Eqimasolution");
                headers.put("Authorization",bearerToken);
                headers.put("Cache-Control","no-cache");

                return headers;
            }
        };
        requestQueue.add(theRequest);

        String s1 = new String(theRequest.getBody(), StandardCharsets.UTF_8);
        String s2 = new String(String.valueOf(theRequest.getHeaders()));
        Log.i("reponse", "sendRequest: "+s1);
        Log.i("Headers", "header: "+s2);
    }



    private byte[] getParamsByte(String rawData) {
        byte[] result = null;
        try {
            result = rawData.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String uuidGenerator()
    {
        UUID uuid = UUID.randomUUID();
        String uuidAsString = uuid.toString();
        return uuidAsString;
    }

    public String getDateNow()
    {
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = new Date(System.currentTimeMillis());
        String valiny = formatter.format(date);
        return valiny;
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

        System.out.println(generatedString);
        return generatedString;
    }





}

