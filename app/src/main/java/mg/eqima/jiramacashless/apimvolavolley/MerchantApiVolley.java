package mg.eqima.jiramacashless.apimvolavolley;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
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
import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import mg.eqima.jiramacashless.DetailPaiementFacture;
import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mg.eqima.jiramacashless.MainActivity;
import mg.eqima.jiramacashless.R;
import mg.eqima.jiramacashless.cashlesswebapi.AsynReglerFacture;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.session.SessionManagement;
import mg.eqima.jiramacashless.utilitaire.AnotherHelper;
import mg.eqima.jiramacashless.utilitaire.BluetoothPermissionHelper;
import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class MerchantApiVolley {

    private Environnement environnement;

    private String domain_name;
    private String url_mvola;

    private String urlToken                  = "/token";
    private String urlForInitiateTransaction = "/mvola/mm/transactions/type/merchantpay/1.0.0/";
    private String urlForTransactionDetails  = "/mvola/mm/transactions/type/merchantpay/1.0.0/"; //{{transID}}
    private String urlForTransactionStatus   = "/mvola/mm/transactions/type/merchantpay/1.0.0/status/"; //{{serverCorrelationId}}

    private String url_set_recu              = "/api/paiement_facture/setCodeRecu?coderecu=";
    private String url_success_state         = "/api/paiement_facture/setSuccessState?id=";
    private String url_state                 = "/api/paiement_facture/setState?etat=";
    private String urlCallback               = domain_name + "/rombo/callback";
    private String url_deduction_credit      = "/transaction/credit/credit_decrease";
    private String url_deduction_credit_half = "/transaction/credit/credit_decrease_half";
    private String url_verif_transaction     = "/rombo/findByServerCorrelationId?serverCorrelationId=";
    private int callbackCallCount            = 0;

    private double seuille                   = 10000;

    private String urt_set_ref_transaction   = "/api/paiement_facture/setRefTransaction?reftransaction=";
    // URL SPECIAL REGLEMENT FACTURE:
    private String url_reglement_facture = "/jiramacontroller/reglementFacture?referencefacture=";

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
    private SessionManagement sessionManagement;

    private static final int PERMISSION_BLUETOOTH = 1;
    private static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    private static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    private static final int PERMISSION_BLUETOOTH_SCAN = 4;

    private static final int PERMISSION_BLUETOOTH_ADVERTISE = 5;

    public MerchantApiVolley(Context context,View view,DatabaseManager db, Activity act, String frais, String montant, String cashpointNumber, PaiementFacture myPaiementFactureB)
    {
        myContext             = context;
        myActivity            = act;
        myDatabaseManager     = db;
        myView                = view;
        myRequestQueue        = Volley.newRequestQueue(myContext);
        myFrais               = frais;
        myMontant             = montant;
        myUtilitaire          = new Utilitaire();
        myCashpointNumber     = cashpointNumber;
        myPaiementFactureBase = myPaiementFactureB;
        sessionManagement     = new SessionManagement(myContext);
        environnement = new Environnement();
        domain_name = environnement.getDomainName();
        url_mvola = environnement.getDomainNameMvola();

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

    public void startOperation(int idTrans, String customNumber)
    {

    }

    public void getToken(int idTrans, String customNumber)
    {
        showProgressDialog(myContext.getString(R.string.transaction_title), myContext.getString(R.string.patientez_message));
        // Creation authorization:
        String cKcS = this.conversionBase64(idTrans);
        String autho = "Basic "+cKcS;
        System.out.println("SECRET ET KEY: "+autho);

        // Initialisation requete:
        String urlTokenMethod = url_mvola+urlToken;
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
                            // GET DE TOKEN POUR INITIER LA TRANSACTION (ENVOIE PUSH USSD):
                            String tokenS = jsonObjectResponse.getString("access_token");
                            initiateTransaction(tokenS, customNumber, getMontantAPayer(), idTrans);
                        }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    if(e.getMessage()!=null)
                    {
                        Toast.makeText(myContext, "Json exception: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("Token: ", error.getMessage());
                    Toast.makeText(myContext, "Get token failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("Token_code_error: ", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "Get token failed code: "+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
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

        Log.e("DATYMVOLA", dateNow);
        String descriptionText = "Paiement Facture JIRAMA EF";

        // Enregistrement merchant number dans classe:
        String merchantNumber = environnement.getMerchantNumber();
        myDatabaseManager.setMerchantNumber(merchantNumber, myPaiementFactureBase.getId());
        myDatabaseManager.close();

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
                + "\r\n\"creditParty\" : [{\"key\": \"msisdn\", \"value\": \"" + merchantNumber + "\"}],"
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
                                //INSERTION DE LA SERVER CORRELATION ID DANS LA BASE DE DONNEES SQLITE:
                                myDatabaseManager.setServerCorrelationId(serverCorrelationId, myPaiementFactureBase.getId());
                                myDatabaseManager.close();
                                Log.e("INITIATETRANSACTION", "Server correlation id: " + serverCorrelationId);

                                // GET STATUS DE LA TRANSACTION:
                                Long delay = Long.valueOf(12000);
                                //delayEoStatuts(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);
                                delayEoStatutsVerif(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);
                                //getStatusOfTransaction(bearerToken, serverCorrelationId, idTrans, customerNumber);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            if(e.getMessage()!=null)
                            {
                                Toast.makeText(myContext, "Json exception: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        Log.e("InitiateTransaction: ", error.getMessage());
                        Toast.makeText(myContext, "Initiate transaction failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        if(error.networkResponse!=null)
                        {
                            Log.e("Transaction_code: ", String.valueOf(error.networkResponse.statusCode));
                            Toast.makeText(myContext, "Initiate transaction failed code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();

                        }
                    }
                    else
                    {
                        Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Version", "1.0");
                    headers.put("X-CorrelationID", uuid);
                    headers.put("UserLanguage", "MG");
                    headers.put("UserAccountIdentifier", "msisdn;" + environnement.getMerchantNumber());
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


    public String getMontantAPayer()
    {
        String valiny = this.myMontant;

        return valiny;
    }

    public void getStatusOfTransaction(String token,String serverCorrelationId, int idTrans, String customerNumber)
    {
        // TOKEN ET GENERATION uuid POUR L'OPERATION DE GET STATUS D'UNE TRANSACTION:
        String bearerToken = token;
        String uuid = uuidGenerator();

        // CREATION REQUETES ET DEMARRAGE GET STATUS:
        String urlStatus = url_mvola+urlForTransactionStatus+serverCorrelationId;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlStatus, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                        if(response.has("status") && response.has("objectReference"))
                        {
                            try {
                                    String status = response.getString("status");
                                    String objectReference = response.getString("objectReference");
                                    Log.e("STATUS", "Statut: "+status+"\n Object reference: "+ objectReference);
                                    if(status.equals("pending"))
                                    {
                                        // SI LE STATUS EST ENCORE EN MODE PENDING
                                        Long delay = Long.valueOf(5000);
                                        delayEoStatuts(bearerToken, serverCorrelationId, idTrans, customerNumber, delay);
                                        //getStatusOfTransaction(bearerToken, serverCorrelationId, idTrans, customerNumber);
                                    }
                                    else if(!status.equals("pending")){
                                        Log.e("STOPSTATUS", "Status: "+status+"\n Object reference: "+ objectReference);
                                            if(status.equals("completed"))
                                            {

                                                Toast.makeText(myContext, myContext.getString(R.string.transaction_reussi_message), Toast.LENGTH_SHORT).show();
                                                // MISE DE REFERENCE TRANSACTION DANS SQLITE ET MISE ETAT EN PAID DANS SQLITE:
                                                myPaiementFactureBase.setRefTransaction(objectReference);
                                                myDatabaseManager.setRefTransaction(objectReference, myPaiementFactureBase.getId());
                                                myDatabaseManager.close();
                                                myDatabaseManager.setEtat("PAID", myPaiementFactureBase.getId());
                                                myDatabaseManager.close();

                                                sessionManagement.decreaseNbCredit();
                                                decreaseMyCredit(token, objectReference, idTrans, customerNumber);

                                            }
                                            else
                                            {
                                                // SI LE STATUS EST FAILED ALORS MISE EN ETAT CANCELED DANS SQLITE:
                                                dismissProgressDialog();
                                                myDatabaseManager.setEtat("CANCELED", myPaiementFactureBase.getId());
                                                myDatabaseManager.close();
                                                setState("CANCELED", myPaiementFactureBase.getId());
                                                Toast.makeText(myContext, myContext.getString(R.string.transaction_echouer_message), Toast.LENGTH_LONG).show();

                                            }
                                    }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            if (e.getMessage()!=null)
                            {
                                Log.e("JSON_ERROR", e.getMessage());
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
                    Log.e("StatusTransaction: ", error.getMessage());
                    Toast.makeText(myContext, "Status transaction failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("StatusTransactionCode: ", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "Status transaction failed code: "+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();

                    }
                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Version", "1.0");
                headers.put("X-CorrelationID", uuid);
                headers.put("UserLanguage", "MG");
                headers.put("UserAccountIdentifier", "msisdn;"+environnement.getMerchantNumber());
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
                    getToken(1, customerNumber);

                }
                else if(idTrans==1)
                {
                    // SI LE REGLEMENT EST EQIVAUT AU MONTANT FACTURE:
                    // Mise des detail:
                        if(response.has("amount") && response.has("transactionReference") && response.has("requestDate")) {

                            // MISE EN VARIABLE DES DETAILS TRANSACTION MVOLA:
                            String transactionReferenceForPr = "";
                            String amountForPrTotale = "";
                            String requestDateForPr = "";
                            String numberForPr = customerNumber;
                            String fraisForPr = myFrais;
                            String operateurForPr = "Mvola";

                            try {
                                amountForPrTotale = String.valueOf(Double.valueOf(response.getString("amount"))+Double.valueOf(myFrais));
                                transactionReferenceForPr = response.getString("transactionReference");
                                requestDateForPr = response.getString("creationDate");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                if(e.getMessage()!=null)
                                {
                                    Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            // ENVOIE REFERENCE DE TRANSACTION DANS LA BASE MYSQL:
                            setRefTransaction(transId, myPaiementFactureBase.getId(), requestDateForPr, numberForPr, fraisForPr, amountForPrTotale, operateurForPr);

                        }
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("DetailTransaction: ", error.getMessage());
                    Toast.makeText(myContext, "Detail transaction failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("DetailTransactionCode: ", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "Detail transaction failed code: "+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }

                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Version", "1.0");
                headers.put("X-CorrelationID", uuid);
                headers.put("UserLanguage", "MG");
                headers.put("UserAccountIdentifier", "msisdn;"+environnement.getMerchantNumber());
                headers.put("partnerName","JIRAMA");
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                headers.put("Authorization", bearerToken);
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        jsonObjectRequestDetails.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        this.myRequestQueue.add(jsonObjectRequestDetails);

    }

    // Get encode to use:

    //CONVERTIR EN BASE64:
    public String conversionBase64(int idTrans)
    {
        String valiny = "";
        String aEncoder = environnement.getAEncoder();
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
    public void doPrint(View view, Recu recuS, String id) {

                String recu = recuS.contruireMonRecu();
                String combinaison = recuS.getRefFacture() + "_" + recuS.getNumeroRecuJirama();
                Utilitaire utilitaire = new Utilitaire();
                Bitmap qrCode = utilitaire.generateBitmap(combinaison);
                //BLUETOOTH:
                BluetoothDevice printerDevice= AnotherHelper.findBluetoothprinter(new BluetoothPermissionHelper(myActivity),myContext,myActivity);
                BluetoothConnection connection =new BluetoothConnection(printerDevice);
                try {


                        if (connection != null) {
                            EscPosCharsetEncoding charsetEncoding = new EscPosCharsetEncoding("UTF-8", 6);

                            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32,charsetEncoding);
                            String logo = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, myContext.getResources().getDrawableForDensity(R.drawable.logojirama2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n"
                                    + "[L]\n" + recu + "\n" +
                                    "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, qrCode) + "</img>\n"
                                    + "[L]\n";

                            printer.printFormattedText(logo);
                            myDatabaseManager.miseEnDuplicata(id);
                            myDatabaseManager.close();
                            connection.disconnect();

                        } else {
                            Toast.makeText(myContext, myContext.getString(R.string.no_connected_imprimante), Toast.LENGTH_SHORT).show();
                        }

                } catch (Exception e) {
                    if(e.getMessage()!=null)
                    {
                        Log.e("APP", "Can't print",e);
                        Toast.makeText(myContext, "Print error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

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
                        dismissProgressDialog();
                        if(error.getMessage()!=null)
                        {
                            Toast.makeText(myContext, myContext.getString(R.string.error) + " code reçu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("VOLLEY", "Erreur code recu code: " + error.getMessage());
                            if(error.networkResponse!=null)
                            {
                                Toast.makeText(myContext, myContext.getString(R.string.error) + " code recu code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                                Log.e("VOLLEY", "Erreur code recu code: " + String.valueOf(error.networkResponse.statusCode));

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
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error) + "Etat success\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: " + error.getMessage());
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, myContext.getString(R.string.error) + "Etat success code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                        Log.e("VOLLEY", "Error code: " + String.valueOf(error.networkResponse.statusCode));
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

    public void setState(String state, String id) {
        String url = domain_name + url_state + state+"&id="+id;
        //RequestQueue requestQueue = Volley.newRequestQueue(myContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("STATE", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error) + "Etat success\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: " + error.getMessage());
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, myContext.getString(R.string.error) + "Etat success code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                        Log.e("VOLLEY", "Error code: " + String.valueOf(error.networkResponse.statusCode));
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

    public void delayEoStatuts(String bearerToken,String serverCorrelationId, int idTrans, String customerNumber, Long delay)
    {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getStatusOfTransaction(bearerToken, serverCorrelationId, idTrans, customerNumber);
            }
        }, delay);
    }

    public void setRefTransaction(String transactionId, String idPaiement, String requestDateForPr, String numberForPr, String fraisForPr, String amountForPrTotale, String operateurForPr)
    {
        String url = domain_name + urt_set_ref_transaction + transactionId+"&idPaiement="+idPaiement;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dismissProgressDialog();
                Log.e("STATE", response);
                JSONObject reponseSetRefTransaction = null;
                try {
                    reponseSetRefTransaction = new JSONObject(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                    if(e.getMessage()!=null)
                    {
                        Toast.makeText(myContext, "Json error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                Log.e("STATE", response);
                if(reponseSetRefTransaction.has("response"))
                {
                    try {
                        // SI LA REPONSE SET REF TRANSACTION EST ok ALORS PROCEDER AU REGLEMENT DE LA FACTURE:
                        if (reponseSetRefTransaction.getString("response").equals("ok")) {
                            Toast.makeText(myContext, "Ref transaction enregistrement: " + reponseSetRefTransaction.getString("response"), Toast.LENGTH_SHORT).show();

                            //Toast.makeText(myContext, myContext.getString(R.string.reglement_title), Toast.LENGTH_SHORT).show();
                            showProgressDialog(myContext.getString(R.string.reglement_title), myContext.getString(R.string.reglement_facture));

                            // FONCTION DE REGLEMENT FACTURE JIRAMA:
                            // CONSTRUCTION RECU POUR IMPRESSION
                            Recu recu = null;
                            recu = new Recu(myPaiementFactureBase.getDatePaiement(), myPaiementFactureBase.getRefFacture(), String.valueOf(myPaiementFactureBase.getMontantFacture()), numberForPr, myCashpointNumber, fraisForPr, amountForPrTotale, operateurForPr, transactionId, myPaiementFactureBase.getRefClient(), myPaiementFactureBase.getNomClient(), myPaiementFactureBase.getAdresseClient(), myPaiementFactureBase.getMois(), myPaiementFactureBase.getAnnee(), myPaiementFactureBase.getCaissier());

                            // CONTRUCTION ID INTERNE POUR ENVOIE JIRAMA:
                            String[] dateId = getDateId(myPaiementFactureBase.getDatePaiement());
                            String idInterneMaking = dateId[0] + dateId[1] + myPaiementFactureBase.getId();
                            recu.setIdInterne(idInterneMaking);

                            AsynReglerFacture asyncReglerFacture = new AsynReglerFacture();
                            Object[] obj = new Object[4];
                            obj[0] = recu.getRefFacture();
                            obj[1] = recu.getNumero() + "_Eqima";
                            obj[2] = recu.getRefTransaction();
                            obj[3] = idInterneMaking + "_Mvola";
                            Log.e("EQ", String.valueOf(obj[3]));

                            //dismissProgressDialog();
                            reglerFacture(obj, recu);


                        }
                    }catch (JSONException e) {
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            Toast.makeText(myContext, "Json error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(myContext, "Set RefTransaction error" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: " + error.getMessage());
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, "Set RefTransaction error code: " + String.valueOf(error.networkResponse.statusCode), Toast.LENGTH_SHORT).show();
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

    public void getTransactionStatusWithServerCorrelationId(String token,String serverCorrelationId, int idTrans, String customerNumber)
    {
        String urlStatus = domain_name+url_verif_transaction+serverCorrelationId;
        StringRequest stringRequest = new StringRequest(Request.Method.GET,urlStatus, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RESPONSE", response);
                if(response.equals("null"))
                {
                    if(callbackCallCount<12)
                    {
                        callbackCallCount += 1;
                        Long delay = Long.valueOf(5000);
                        delayEoStatutsVerif(token, serverCorrelationId, idTrans, customerNumber, delay);
                    }
                    else if(callbackCallCount==12)
                    {
                        Toast.makeText(myContext, myContext.getString(R.string.null_callback), Toast.LENGTH_LONG).show();
                        Long delay = Long.valueOf(5000);
                        delayEoStatuts(token, serverCorrelationId, idTrans, customerNumber, delay);
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
                                myPaiementFactureBase.setRefTransaction(transId);
                                myDatabaseManager.setRefTransaction(transId, myPaiementFactureBase.getId());
                                myDatabaseManager.close();
                                myDatabaseManager.setEtat("PAID", myPaiementFactureBase.getId());
                                myDatabaseManager.close();

                                sessionManagement.decreaseNbCredit();
                                decreaseMyCredit(token, transId, idTrans, customerNumber);

                            }
                            else
                            {
                                dismissProgressDialog();
                                myDatabaseManager.setEtat("CANCELED", myPaiementFactureBase.getId());
                                myDatabaseManager.close();
                                setState("CANCELED", myPaiementFactureBase.getId());
                                Toast.makeText(myContext, myContext.getString(R.string.transaction_echouer_message), Toast.LENGTH_LONG).show();
                                //dismissProgressDialog();
                                //Toast.makeText(myContext, myContext.getString(R.string.transaction_echouer_message), Toast.LENGTH_LONG).show();
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

    // FONCTION REGLEMENT FACTURE VERSION VOLLEY:
    public void reglerFacture(Object[] objects, Recu recu)
    {
        String referenceFacture = (String) objects[0];
        String controlid = (String) objects[1];
        String transid = (String) objects[2];
        String operationid = (String) objects[3];
        String url = domain_name + url_reglement_facture+ referenceFacture +"&controlid="+controlid+"&transid="+transid+"&operationid="+operationid+"&operateur="+myPaiementFactureBase.getOperateur();
        StringRequest stringRequestReglementFacture = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RESPONSE", response);
                try {
                        Map<String, String> valinyReglement = traitementPaiementFinal(response);
                        if (valinyReglement.get("recucode") != null && !valinyReglement.get("recucode").equals("") && valinyReglement.get("errors").equals("")) {
                            recu.setNumeroRecuJirama(valinyReglement.get("recucode"));


                            // ENREGISTREMENT CODE RECU DANS SQLITE:
                            myDatabaseManager.setCodeRecu(recu.getNumeroRecuJirama(), myPaiementFactureBase.getId());
                            myDatabaseManager.close();

                            // MISE EN SUCCESS STATES ET CODE RECU DANS SQLITE:
                            myDatabaseManager.setSuccessState(myPaiementFactureBase.getId());
                            myDatabaseManager.close();
                            setCodeRecu(recu.getNumeroRecuJirama(), myPaiementFactureBase.getId());
                            setSuccessState(myPaiementFactureBase.getId());

                            Toast.makeText(myContext, myContext.getString(R.string.reglement_ok), Toast.LENGTH_SHORT).show();
                            Toast.makeText(myContext, myContext.getString(R.string.impression_message), Toast.LENGTH_SHORT).show();

                            // IMPRESSION FACTURE:
                            doPrint(myView, recu, myPaiementFactureBase.getId());

                            Toast.makeText(myContext, myContext.getString(R.string.merci_message), Toast.LENGTH_SHORT).show();

                            //RETOUR VERS L'AFFICHAGE PRINCIPALE:
                            dismissProgressDialog();
                            Intent intent1 = new Intent(myContext, MainActivity.class);
                            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            myActivity.startActivity(intent1);
                        }
                        else{
                                // EN CAS D'ECHEC REGLEMENT DE FACTURE MISE EN ETAT FAILED DANS SQLITE ET MYSQL:
                                myDatabaseManager.setEtat("FAILED", myPaiementFactureBase.getId());
                                myDatabaseManager.close();
                                setState("FAILED", myPaiementFactureBase.getId());
                                Toast.makeText(myContext, myContext.getString(R.string.reglement_pas_ok)+"\n Erreur: "+valinyReglement.get("errors"), Toast.LENGTH_SHORT).show();

                        }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if(e.getMessage()!=null)
                    {
                        Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("REGLEMENT", error.getMessage());
                    Toast.makeText(myContext, "Error: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, "Error code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        stringRequestReglementFacture.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(stringRequestReglementFacture);
    }

    // TRAITEMENT FINALE:
    public Map<String, String> traitementPaiementFinal(String reponseRequete) throws JSONException
    {
        Map<String, String> valinyFarany = new HashMap();
        valinyFarany.put("errors","");
        valinyFarany.put("recucode","");
        JSONObject reponseRequeteJson = new JSONObject(reponseRequete);
        JSONObject fs_P55PAYFA_W55PAYFAA = new JSONObject(reponseRequeteJson.get("fs_P55PAYFA_W55PAYFAA").toString());
        JSONArray errors = new JSONArray(fs_P55PAYFA_W55PAYFAA.getString("errors"));
        boolean verificationError = errorVerificationVide(errors.toString());
        if(!verificationError)
        {
            JSONObject data = new JSONObject(fs_P55PAYFA_W55PAYFAA.getString("data"));
            System.out.println("DATA: "+ data);
            JSONObject txtMontant_28 = new JSONObject(data.getString("txtMontant_28"));
            System.out.println("txtMontant_28: "+ txtMontant_28);
            String montant = txtMontant_28.getString("internalValue");
            System.out.println("Montant: "+ montant);

            JSONObject txtRecuCode_31 = new JSONObject(data.getString("txtRecuCode_31"));
            System.out.println("txtRecuCode_31: "+ txtRecuCode_31.toString());
            String recuCode = txtRecuCode_31.getString("value");
            System.out.println("recu: "+ recuCode);
            if(!recuCode.equals("0"))
            {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("recucode",recuCode);
                }
            }

        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                valinyFarany.replace("errors",errors.toString());
            }
        }
        return valinyFarany;
    }

    // VERIFICATION ERROR:
    public boolean errorVerificationVide(String errors)
    {
        boolean valiny = false;
        if(!errors.equals("[]"))
        {
            valiny = true;
        }
        return valiny;
    }

    // DECREASE CREDIT:
    public void decreaseMyCredit(String token, String transId, int idTrans, String customerNumber) {
        String caissier = myPaiementFactureBase.getCaissier();
        String id       = caissier.replace("EQ-", "");
        String url      = domain_name + url_deduction_credit;
        String info     = "{\n" +
                            "    \"id_cashpoint\" : \""+id+"\",\n" +
                            "    \"date_request\" : \""+myPaiementFactureBase.getDatePaiement()+"\"\n" +
                           "}";
        JSONObject json = null;
        try {
               json = new JSONObject(info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequestDecrease = new JsonObjectRequest(url, json, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("id"))
                {
                    getTransactionDetails(token, transId, idTrans, customerNumber);
                }
                else
                {
                    dismissProgressDialog();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("CREDIT", error.getMessage());
                    Toast.makeText(myContext, "Error: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, "Error code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        jsonObjectRequestDecrease.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(jsonObjectRequestDecrease);
    }

    public void decreaseMyCreditHalf(String token, String transId, int idTrans, String customerNumber)
    {
        String caissier = myPaiementFactureBase.getCaissier();
        String id       = caissier.replace("EQ-", "");
        String url      = domain_name + url_deduction_credit_half;
        String info     = "{\n" +
                "    \"id_cashpoint\" : \""+id+"\",\n" +
                "    \"date_request\" : \""+myPaiementFactureBase.getDatePaiement()+"\"\n" +
                "}";
        JSONObject json = null;
        try {
            json = new JSONObject(info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequestDecrease = new JsonObjectRequest(url, json, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("id"))
                {
                    getTransactionDetails(token, transId, idTrans, customerNumber);
                }
                else
                {
                    dismissProgressDialog();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("CREDIT", error.getMessage());
                    Toast.makeText(myContext, "Error: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(myContext, "Error code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        jsonObjectRequestDecrease.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(jsonObjectRequestDecrease);
    }
}
