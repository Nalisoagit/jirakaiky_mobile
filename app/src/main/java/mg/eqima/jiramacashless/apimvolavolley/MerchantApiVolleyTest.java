package mg.eqima.jiramacashless.apimvolavolley;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mg.eqima.jiramacashless.R;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class MerchantApiVolleyTest {

    private String urlDeTest                 = "https://devapi.mvola.mg";
    private String urlDeProduction           = "https://api.mvola.mg";

    private String url_mvola                 = urlDeTest;

    private String urlToken                  = "/token";
    private String urlForInitiateTransaction = "/mvola/mm/transactions/type/merchantpay/1.0.0/";
    private String urlForTransactionDetails  = "/mvola/mm/transactions/type/merchantpay/1.0.0/"; //{{transID}}
    private String urlForTransactionStatus   = "/mvola/mm/transactions/type/merchantpay/1.0.0/status/"; //{{serverCorrelationId}}
    private String domain_name               = "https://preprod.api.cashless.eqima.org";
    private String url_set_recu              = "/api/paiement_facture/setCodeRecu?coderecu=";
    private String url_success_state         = "/api/paiement_facture/setSuccessState?id=";
    private String url_verif_transaction     = "/rombo/findByServerCorrelationId?serverCorrelationId=";
    private int callbackCallCount         = 0;

    private String urlCallback               = domain_name + "/rombo/callback";

    private String consumer_key_0            = "lpWFNoBtBZ4elJECnXrsDKfICLUa";
    private String consumer_secret_0         = "WXsIPhnVgyYYZuaa6CTMQDVt0uYa";
    private String merchantNumber_0          = "0343500004";

    private String consumer_key_1            = "p1rUPnXjDgPcREnOkPkDJ24Jsc0a";
    private String consumer_secret_1         = "60pD_yDrykkt9J6NYAM_SBLsAbIa";
    private String merchantNumber_1          = "0343500004";

    //private String consumer_key_2            = "34wcX2Jh4wvClmhPiuQAx0TRJoYa";
    //private String consumer_secret_2         = "8z_RkyRumYaUxU5fRiQMSSXsbmIa";
    //private String merchantNumber_2          = "0342135891";

    private String consumer_key_2_p            = "IOoJUzeZ5QGisGmBqBs0KQ1ofjAa";
    private String consumer_secret_2_p         = "vy6j13Tv3kExXmDj5rYJugZkVyEa";
    private String merchantNumber_2            = "0342135891";

    private Context myContext;
    private RequestQueue myRequestQueue;
    private String myToken = "";
    private String myFrais;
    private String myMontant;

    private ProgressDialog progressDialog;

    private PaiementFacture myPaiementFactureBase;
    private String myLatitude;
    private String myLongitude;
    private String myDateForFailed;
    private Activity myActivity;
    private View myView;
    private DatabaseManager myDatabaseManager;
    private Utilitaire myUtilitaire;
    private String myCashpointNumber;

    private static final int PERMISSION_BLUETOOTH = 1;
    private static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    private static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    private static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public MerchantApiVolleyTest(Context context, Activity act, String frais)
    {
        myContext             = context;
        myActivity            = act;
        myRequestQueue        = Volley.newRequestQueue(myContext);
        myFrais               = frais;

    }

    public void showProgressDialog(String title, String message)
    {
        dismissProgressDialog();
        this.progressDialog = new ProgressDialog(this.myContext);
        this.progressDialog.setTitle(title);
        this.progressDialog.setMessage(message);
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.show();
    }

    public void dismissProgressDialog()
    {
        if (this.progressDialog!=null && this.progressDialog.isShowing())
        {
            this.progressDialog.dismiss();
        }
    }

    public void getToken(int idTrans, String customNumber)
    {
        showProgressDialog(myContext.getString(R.string.transaction_title), myContext.getString(R.string.patientez_message));
        // Creation authorization:
        String cKcS = this.conversionBase64(idTrans);
        String autho = "Basic "+cKcS;
        System.out.println("SECRET ET KEY: "+autho);

        // Initialisation requete:
        String urlTokenMethod = url_mvola + urlToken;
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                urlTokenMethod, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("TokenResponse:", response);
                try{
                    JSONObject jsonObjectResponse = new JSONObject(response);
                    if(jsonObjectResponse.has("access_token"))
                    {
                        String tokenS = jsonObjectResponse.getString("access_token");
                        String bearerToken = "Bearer " + tokenS;
                        //getTransactionDetails(bearerToken, transId, 1, "0340712387");
                        initiateTransaction(tokenS, customNumber, getMontantAPayer(idTrans), idTrans);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.e("Token: ", error.getMessage());
                dismissProgressDialog();
                Toast.makeText(myContext, "Get token failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("grant_type", "client_credentials");
                params.put("scope", "EXT_INT_MVOLA_SCOPE");
                return params;
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", autho);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        this.myRequestQueue.add(stringRequest);
    }

    public void initiateTransaction(String token, String customerNumber, String amountS, int idTrans) {
        //The amount of the transaction into double format:
        int amount = Integer.valueOf(amountS);
        String dateNow = this.getDateNow();
        String descriptionText = "Paiement Facture JIRAMA EF";

        // Creatio bearer token:
        String bearerToken = "Bearer " + token;
        System.out.println("Bearer token: " + bearerToken);
        String uuid = this.uuidGenerator();

        // DATA ROW:
        String data = "{\r\n\"amount\" : \"" + amount + "\","
                + "\r\n\"currency\" : \"Ar\","
                + "\r\n\"descriptionText\" : \"" + descriptionText + "\","
                + "\r\n\"requestingOrganisationTransactionReference\" : \"" + uuid + "\","
                + "\r\n\"requestDate\" : \"" + dateNow + "\","
                + "\r\n\"originalTransactionReference\" : \"" + uuid + "\","
                + "\r\n\"debitParty\" : [{\"key\": \"msisdn\", \"value\": \"" + customerNumber + "\"}],"
                + "\r\n\"creditParty\" : [{\"key\": \"msisdn\", \"value\": \"" + getMerchantNumber(idTrans) + "\"}],"
                + "\r\n\"metadata\" : [{\"key\": \"partnerName\", \"value\": \"JIRAMA\"}]"
                + "\r\n}";

        JsonObjectRequest jsonObjectRequest = null;
        try {
            JSONObject jsonRequest = new JSONObject(data);
            //CREATION REQUEST:
            String urlInitiateMethod = url_mvola + urlForInitiateTransaction;
            jsonObjectRequest = new JsonObjectRequest(Request.Method.POST
                    , urlInitiateMethod, jsonRequest, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.e("InitiateResponse:", response.toString());
                    if (response.has("status")) {
                        try {
                            if (response.getString("status").equals("pending") && response.has("serverCorrelationId")) {
                                String serverCorrelationId = response.getString("serverCorrelationId");
                                Log.e("INITIATETRANSACTION", "Server correlation id: " + serverCorrelationId);
                                //MAKA STATUS:
                                //delayEo();
                                Long delay = Long.valueOf(20000);
                                delayEoStatuts(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);
                                //delayEoStatutsVerif(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if(error.getMessage()!=null)
                    {
                        Log.e("InitiateTransaction: ", error.getMessage());
                        Toast.makeText(myContext, "Initiate transaction failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        if(error.networkResponse!=null)
                        {
                            Log.e("InitiateTransaction: ", error.getMessage());
                            Toast.makeText(myContext, "Initiate transaction code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Version", "1.0");
                    headers.put("X-CorrelationID", uuid);
                    headers.put("UserLanguage", "MG");
                    headers.put("UserAccountIdentifier", "msisdn;" + getMerchantNumber(idTrans));
                    headers.put("partnerName", "JIRAMA");
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    headers.put("Authorization", bearerToken);
                    headers.put("X-Callback-URL", urlCallback);
                    headers.put("Cache-Control", "no-cache");
                    return headers;
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.myRequestQueue.add(jsonObjectRequest);
    }

    public String getMerchantNumber(int idTrans)
    {
        String valiny = "";
        if(idTrans==0)
        {
            valiny = merchantNumber_0;
        }
        else if(idTrans==1)
        {
            valiny = merchantNumber_0;
        }

        return valiny;
    }

    public String getMontantAPayer(int idTrans)
    {
        String valiny = "";
        if(idTrans==0)
        {
            valiny = this.myFrais;
        }
        else if(idTrans==1)
        {
            valiny = this.myFrais;
        }

        return valiny;
    }

    public void getStatusOfTransaction(String token,String serverCorrelationId, int idTrans, String customerNumber)
    {
        //Authentication authForToken = new Authentication();
        String bearerToken = token;
        String uuid = uuidGenerator();

        // CREATION REQUETES:
        String urlStatus = url_mvola+urlForTransactionStatus+serverCorrelationId;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlStatus, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("STATUSRESPONSE", response.toString());
                if(response.has("status") && response.has("objectReference"))
                {
                    try {
                        String status = response.getString("status");
                        String objectReference = response.getString("objectReference");
                        Log.e("STATUS", "Statut: "+status+"\n Object reference: "+ objectReference);
                        if(status.equals("pending"))
                        {
                            Long delay = Long.valueOf(5000);
                            delayEoStatuts(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);
                        }
                        else if(!status.equals("pending")){
                            Log.e("STOPSTATUS", "Status: "+status+"\n Object reference: "+ objectReference);
                            if(status.equals("completed"))
                            {
                                getTransactionDetails(bearerToken, objectReference, idTrans, customerNumber);
                            }
                            else
                            {
                                /*Object[] basePaiement = new Object[1];
                                myPaiementFactureBase.setEtat("Failed");
                                myPaiementFactureBase.setNumeroPayeur(customerNumber);
                                myPaiementFactureBase.setLatitude(String.valueOf(myLatitude));
                                myPaiementFactureBase.setLongitude(String.valueOf(myLongitude));
                                myPaiementFactureBase.setDatePaiement(myDateForFailed);
                                basePaiement[0] = myPaiementFactureBase;
                                AsyncInsertCashlessWebApi asyncInsertCashlessWebApi = new AsyncInsertCashlessWebApi();
                                String insertionFailed = null;
                                try {
                                    insertionFailed = asyncInsertCashlessWebApi.execute(basePaiement).get();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                JSONObject failedJson = null;
                                try {
                                    failedJson = new JSONObject(insertionFailed);
                                    Log.e("TRANSACTION", "Failed pour:" + failedJson.get("id"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }*/
                                Long delay = Long.valueOf(10000);
                                delayEoStatutsVerif(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);
                                //dismissProgressDialog();
                                //Toast.makeText(myContext, myContext.getString(R.string.transaction_echouer_message), Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("StatusTransaction: ", error.getMessage());
                Toast.makeText(myContext, "Status transaction failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Version", "1.0");
                headers.put("X-CorrelationID", uuid);
                headers.put("UserLanguage", "MG");
                headers.put("UserAccountIdentifier", "msisdn;"+getMerchantNumber(idTrans));
                headers.put("partnerName","JIRAMA");
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                headers.put("Authorization", bearerToken);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        this.myRequestQueue.add(jsonObjectRequest);
    }

    public void getTransactionDetails(String token, String transId, int idTrans, String customerNumber)
    {
        //GET BEARER TOKEN AND UUID to identify the transaction:
        String bearerToken = token;
        String uuid = uuidGenerator();

        // CONSTRUCTION REQUETE:
        String urlDetails = url_mvola+urlForTransactionDetails+transId;
        JsonObjectRequest jsonObjectRequestDetails = new JsonObjectRequest(Request.Method.GET,
                urlDetails, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("DETAILRESPONSE", response.toString());
                dismissProgressDialog();
                if(idTrans==0)
                {
                    // Frais:
                    Toast.makeText(myContext, "Frais payé", Toast.LENGTH_SHORT).show();

                    //getToken(1, customerNumber);

                }
                else if(idTrans==1)
                {
                    // Montant facture:
                    Toast.makeText(myContext, myContext.getString(R.string.transaction_reussi_message), Toast.LENGTH_SHORT).show();
                    // Mise des detail:

                    if(response.has("amount") && response.has("transactionReference") && response.has("requestDate")) {

                    }

                    // Start


                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("DetailTransaction: ", error.getMessage());
                Toast.makeText(myContext, "Detail transaction failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Version", "1.0");
                headers.put("X-CorrelationID", uuid);
                headers.put("UserLanguage", "MG");
                headers.put("UserAccountIdentifier", "msisdn;"+getMerchantNumber(idTrans));
                headers.put("partnerName","JIRAMA");
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                headers.put("Authorization", bearerToken);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        this.myRequestQueue.add(jsonObjectRequestDetails);

    }

    // Get encode to use:
    public String getAEncoder(int idTrans)
    {
        String valiny = "";
        if(idTrans==0)
        {
            valiny = this.consumer_key_0+":"+this.consumer_secret_0;
        }
        else if(idTrans==1)
        {
            //valiny = this.consumer_key_2_p+":"+this.consumer_secret_2_p;
            valiny = this.consumer_key_0+":"+this.consumer_secret_0;
        }

        return valiny;
    }

    //CONVERTIR EN BASE64:
    public String conversionBase64(int idTrans)
    {
        String valiny = "";
        String aEncoder = getAEncoder(idTrans);
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

    /******** BLUETOOTH VERSION FARANY MODIFICATION: Dantsu:ESCPOS-ThermalPrinter ******************/
    public void doPrint(View view, Recu recuS) {
        String recu = recuS.contruireMonRecu();
        String combinaison = recuS.getRefFacture() + "_" + recuS.getNumeroRecuJirama();
        Utilitaire utilitaire = new Utilitaire();
        Bitmap qrCode = utilitaire.generateBitmap(combinaison);
        //BLUETOOTH:
        BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
        try {
            if (ContextCompat.checkSelfPermission(myContext, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(myActivity, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_BLUETOOTH);
            } else if (ContextCompat.checkSelfPermission(myContext, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(myActivity, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_BLUETOOTH_ADMIN);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(myContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(myActivity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_BLUETOOTH_CONNECT);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(myContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(myActivity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_BLUETOOTH_SCAN);
            } else {

                if (connection != null) {
                    EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
                    String logo = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, myContext.getResources().getDrawableForDensity(R.drawable.logojirama2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n"
                            + "[L]\n" + recu + "\n" +
                            "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, qrCode) + "</img>\n"
                            + "[L]\n";

                    printer.printFormattedText(logo);
                    connection.disconnect();

                } else {
                    Toast.makeText(myContext, myContext.getString(R.string.no_connected_imprimante), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("APP", "Can't print", e);
        }
    }

    /*********** FIN BLUETOOTH **************/

    public void setCodeRecu(String coderecu, String id) {
        String url = domain_name + url_set_recu + coderecu + "&id=" + id;
        //RequestQueue requestQueue = Volley.newRequestQueue(myContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RECU", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(myContext, myContext.getString(R.string.error) + error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: " + error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(stringRequest);

    }

    public void setSuccessState(String id) {
        String url = domain_name + url_success_state + id;
        //RequestQueue requestQueue = Volley.newRequestQueue(myContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("STATE", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(myContext, myContext.getString(R.string.error) + "Etat success\n" + error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: " + error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(stringRequest);

    }

    public String[] getDateId(String daty) {
        Log.e("DATE", daty.substring(0, 4));
        Log.e("DATE", daty.substring(5, 7));
        String annee = daty.substring(0, 4);
        String mois = daty.substring(5, 7);
        if (mois.startsWith("0")) {
            mois = mois.substring(1, 2);
        }

        String[] valiny = new String[2];
        valiny[0] = annee;
        valiny[1] = mois;

        Log.e("DATEEE", valiny[0]);
        Log.e("DATEEE", valiny[1]);

        return valiny;
    }

    public String getDateNowSimple() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());

        String valiny = formatter.format(date);
        getDateId(valiny);
        return valiny;
    }

    public void delayEo(String beast)
    {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                afficheo(beast);
            }
        }, 6000);
    }

    public void delayEoStatuts(String bearerToken,String serverCorrelationId, int idTrans, String customerNumber,Long delay)
    {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getStatusOfTransaction(bearerToken, serverCorrelationId, idTrans, customerNumber);
            }
        }, delay);
    }

    public void afficheo(String beast)
    {
        Log.e("DELAYEO", beast);
    }

    public void getTransactionStatusWithServerCorrelationId(String token,String serverCorrelationId, int idTrans, String customerNumber)
    {
        String urlStatus = domain_name + url_verif_transaction + serverCorrelationId;
        StringRequest stringRequest = new StringRequest(Request.Method.GET,urlStatus, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RESPONSE", response);
                if(response.equals("null"))
                {
                    if(callbackCallCount<12)
                    {
                        Long delay = Long.valueOf(6000);
                        callbackCallCount += 1;
                        delayEoStatutsVerif(token, serverCorrelationId, idTrans, customerNumber, delay);
                    }
                    else if(callbackCallCount==12)
                    {
                        dismissProgressDialog();
                        Toast.makeText(myContext, myContext.getString(R.string.null_callback), Toast.LENGTH_LONG).show();
                    }

                }
                else
                {
                    try {
                        JSONObject responseJson = new JSONObject(response);
                        Log.e("VERDICT", responseJson.toString());
                        if(responseJson.has("id"))
                        {
                            String transactionStatus    = responseJson.getString("transactionStatus");
                            String transId = responseJson.getString("transactionReference");

                            if(transactionStatus.equals("completed")){
                                getTransactionDetails(token, transId, idTrans, customerNumber);
                            }
                            else
                            {
                                dismissProgressDialog();
                                Toast.makeText(myContext, myContext.getString(R.string.transaction_echouer_message), Toast.LENGTH_LONG).show();
                            }

                        }
                    } catch (JSONException e) {
                        dismissProgressDialog();
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Toast.makeText(myContext, "Status transaction error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: " + error.getMessage());
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, "Status transaction error code: " + String.valueOf(error.networkResponse.statusCode), Toast.LENGTH_SHORT).show();
                        Log.e("VOLLEY", "ERREUR: " + error.networkResponse.statusCode);
                    }
                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(stringRequest);
    }

    public void delayEoStatutsVerif(String bearerToken,String serverCorrelationId, int idTrans, String customerNumber, Long delay)
    {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getTransactionStatusWithServerCorrelationId(bearerToken, serverCorrelationId, idTrans, customerNumber);
            }
        }, delay);
    }

}
