package mg.eqima.jiramacashless.paositravolley;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import mg.eqima.jiramacashless.MainActivity;
import mg.eqima.jiramacashless.Payement;
import mg.eqima.jiramacashless.R;
import mg.eqima.jiramacashless.cashlesswebapi.AsynReglerFacture;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.session.SessionManagement;
import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class PaositraVolley {

    // URL
    // API:
        private String domain_name_test          = "https://preprod.api.cashless.eqima.org";
        private String domain_name_prod          = "https://cashless.eqima.org";

        private String domain_name               = domain_name_test;

        private String url_deduction_credit      = "/transaction/credit/credit_decrease";
        private String url_deduction_credit_half = "/transaction/credit/credit_decrease_half";
        private String urt_set_ref_transaction   = "/api/paiement_facture/setRefTransaction?reftransaction=";
        // URL SPECIAL REGLEMENT FACTURE:
        private String url_reglement_facture = "/jiramacontroller/reglementFacture?referencefacture=";
        private String url_set_recu              = "/api/paiement_facture/setCodeRecu?coderecu=";
        private String url_success_state         = "/api/paiement_facture/setSuccessState?id=";
        private String url_state                 = "/api/paiement_facture/setState?etat=";
        private String url_reste_credit          = "/transaction/credit/credit_left?idCashpoint=";

    // PAOSITRA:
        private String url_paositra_test        = "https://ws-paoma.numherit-dev.com";
        private String url_paositra_prod        = "";



        private String url_paositra             = url_paositra_test;

        private String url_token                = "/auth/loginUserAgence";
        private String url_initiate_cashout     = "/monetique/operation/initialisationRetraitEspeceWithCodeRetrait";
        private String url_validation_cashout   = "/monetique/operation/validationRetraitEspeceByCodeRetrait";
        private String url_logout               = "/auth/logout";
        private String login                    = "equima";
        private String password                 = "8ND@14T7f";

        private static final int PERMISSION_BLUETOOTH = 1;
        private static final int PERMISSION_BLUETOOTH_ADMIN = 2;
        private static final int PERMISSION_BLUETOOTH_CONNECT = 3;
        private static final int PERMISSION_BLUETOOTH_SCAN = 4;

    // ELEMENT PROPRE:
        Context myContext;
        RequestQueue myRequestQueue;
        String myMontant;
        String myDebitNumber;
        private ProgressDialog progressDialog;
        LayoutInflater myLayoutInflater;
        DatabaseManager myDatabaseManager;
        PaiementFacture myPaiementFactureBase;
        private Activity myActivity;
        private View myView;
        AlertDialog myAlertDialog;
        SessionManagement mySessionManagement;
        double seuille;


    public PaositraVolley(Context context, String montant, String debitNumber, LayoutInflater layoutInflater, PaiementFacture paiementFacture, Activity activity, View view)
    {
        myContext         = context;
        myRequestQueue    = Volley.newRequestQueue(myContext);
        myMontant         = montant;
        myDebitNumber     = debitNumber;
        myLayoutInflater  = layoutInflater;
        myDatabaseManager = new DatabaseManager(context);
        myPaiementFactureBase = paiementFacture;
        myActivity        = activity;
        myView            = view;
        mySessionManagement = new SessionManagement(context);
        seuille = mySessionManagement.getSeuille();
    }

    public void getToken(int operation, String codeDevalidation, String cin){
        if(operation==1)
        {
            showProgressDialog(myContext.getString(R.string.transaction_title), myContext.getString(R.string.patientez_message));
        }

        String url          = url_paositra + url_token;
        String bodyString   = "{\n" +
                            " \"login\":\""+login+"\",\n" +
                            " \"password\":\""+password+"\"\n" +
                            "}";

        JSONObject bodyJson = null;
        try {
                bodyJson    = new JSONObject(bodyString);
        } catch (JSONException e) {
            e.printStackTrace();
            if(e.getMessage()!=null)
            {
                Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(myContext, "Json error", Toast.LENGTH_LONG).show();
            }
        }

        JsonObjectRequest tokenRequest = new JsonObjectRequest(Request.Method.POST, url, bodyJson, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("TokenResponse", response.toString());
                if(response.has("code"))
                {
                    try {
                            String codeReponse = response.getString("code");
                            String error       = response.getString("error");
                            if(codeReponse.equals("200") && error.equals("false"))
                            {
                                String dataString   = response.getString("data");
                                JSONObject data     = new JSONObject(dataString);
                                String access_token = data.getString("access_token");

                                Log.e("TOKENPAOMA", access_token);

                                // REQUETE INITIALISATION TRANSACTION:
                                String bearerToken  = "Bearer " + access_token;
                                if(operation==1)
                                {
                                    initiateCashout(bearerToken);
                                }
                                else if(operation==2)
                                {
                                    validateCashout(bearerToken, codeDevalidation, cin);
                                }

                            }
                            else{
                                    Toast.makeText(myContext, "Paoma error token", Toast.LENGTH_SHORT).show();
                                    myDatabaseManager.setEtat("CANCELED", myPaiementFactureBase.getId());
                                    myDatabaseManager.close();
                                    setState("CANCELED", myPaiementFactureBase.getId());
                            }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Json error", Toast.LENGTH_LONG).show();
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
                    Log.e("TokenError", error.getMessage());
                    Toast.makeText(myContext, "TokenError: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("TokenError", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "TokenError code: "+ error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Log.e("TokenError", error.toString());
                    Toast.makeText(myContext, "TokenError: "+ error.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        tokenRequest.addMarker("Token_Request");

        tokenRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(tokenRequest);

    };

    public void initiateCashout(String bearerToken){
        String url = url_paositra + url_initiate_cashout;
        String bodyString = "{\n" +
                            " \"telephone\" : \""+this.myDebitNumber+"\",\n" +
                            " \"montant\" : \""+this.myMontant+"\"\n" +
                            "}";

        JSONObject bodyJson = null;
        try {
            bodyJson    = new JSONObject(bodyString);
        } catch (JSONException e) {
            e.printStackTrace();
            if(e.getMessage()!=null)
            {
                Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(myContext, "Json error", Toast.LENGTH_LONG).show();
            }
        }

        JsonObjectRequest initiateCashoutRequest = new JsonObjectRequest(Request.Method.POST, url, bodyJson, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("INITIATECASHOUTRESPONSE", response.toString());
                if(response.has("code"))
                {
                    try {
                            String codeReponse = response.getString("code");
                            String error       = response.getString("error");
                            String message     = response.getString("msg");
                            if(codeReponse.equals("200") && message.equals("Données disponibles"))
                            {
                                dismissProgressDialog();
                                showValidationCashout();
                                // SI TRANSACTION LANCE AVEC SUCCESS:
                                Toast.makeText(myContext, myContext.getString(R.string.voir_telephone), Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                myDatabaseManager.setEtat("CANCELED", myPaiementFactureBase.getId());
                                myDatabaseManager.close();
                                setState("CANCELED", myPaiementFactureBase.getId());
                                if(response.has("data"))
                                {
                                    String dataString   = response.getString("data");
                                    JSONObject dataJson = new JSONObject(dataString);
                                    if(dataJson.has("errorMessage"))
                                    {
                                        Toast.makeText(myContext, "Paoma error: "+ dataJson.getString("errorMessage"), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else
                                {
                                    Toast.makeText(myContext, "Paoma error transaction", Toast.LENGTH_SHORT).show();
                                }
                            }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Json error", Toast.LENGTH_LONG).show();
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
                    Log.e("InitiateCashoutError", error.getMessage());
                    Toast.makeText(myContext, "InitiateCashoutError: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("InitiateCashoutError", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "InitiateCashoutError code: "+ error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Log.e("InitiateCashoutError", error.toString());
                    Toast.makeText(myContext, "InitiateCashoutError: "+ error.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", bearerToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        initiateCashoutRequest.addMarker("Initiate_cashout_request");

        initiateCashoutRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(initiateCashoutRequest);
    };

    public void validateCashout(String bearerToken, String codeValidation, String cin)
    {
        String url         = url_paositra + url_validation_cashout;
        String commentaire = "Paiement jirama EF";
        String bodyString = "{\n" +
                            " \"telephone\" : \""+this.myDebitNumber+"\",\n" +
                            " \"montant\" : \""+this.myMontant+"\",\n" +
                            " \"code\" : \""+codeValidation+"\",\n" +
                            " \"cni\" : \""+cin+"\",\n" +
                            " \"commentaire\":\""+commentaire+"\"\n" +
                            "}";

        JSONObject bodyJson = null;
        try {
            bodyJson    = new JSONObject(bodyString);
        } catch (JSONException e) {
            e.printStackTrace();
            if(e.getMessage()!=null)
            {
                dismissProgressDialog();
                Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_LONG).show();
            }
            else
            {
                dismissProgressDialog();
                Toast.makeText(myContext, "Json error", Toast.LENGTH_LONG).show();
            }
        }

        JsonObjectRequest validationCashoutRequest = new JsonObjectRequest(Request.Method.POST, url, bodyJson, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("VALIDATIONCASHOUTRESP", response.toString());
                if(response.has("code"))
                {
                    try {
                        String codeReponse = response.getString("code");
                        String error       = response.getString("error");
                        String msg         = response.getString("msg");
                        if(codeReponse.equals("200") && msg.equals("Données disponibles"))
                        {
                            // SI VALIDATION CASHOUT LANCEE AVEC SUCCESS:
                            String dataString      = response.getString("data");
                            JSONObject dataJson    = new JSONObject(dataString);
                            String num_transaction = dataJson.getString("num_transaction");
                            myPaiementFactureBase.setRefTransaction(num_transaction);
                            myDatabaseManager.setRefTransaction(num_transaction, myPaiementFactureBase.getId());
                            myDatabaseManager.setEtat("PAID", myPaiementFactureBase.getId());
                            myDatabaseManager.close();
                            myAlertDialog.dismiss();
                            Toast.makeText(myContext, myContext.getString(R.string.validation_cashout_ok)+": "+num_transaction, Toast.LENGTH_SHORT).show();


                            // CONSTRUCTION RECU POUR IMPRESSION
                            Recu recu = null;
                            recu = new Recu(myPaiementFactureBase.getDatePaiement(), myPaiementFactureBase.getRefFacture(), String.valueOf(myPaiementFactureBase.getMontantFacture()), myPaiementFactureBase.getNumeroPayeur(), myPaiementFactureBase.getNumeroCashpoint(), String.valueOf(myPaiementFactureBase.getFrais()), String.valueOf(myPaiementFactureBase.getTotal()), myPaiementFactureBase.getOperateur(), myPaiementFactureBase.getRefTransaction(), myPaiementFactureBase.getRefClient(), myPaiementFactureBase.getNomClient(), myPaiementFactureBase.getAdresseClient(), myPaiementFactureBase.getMois(), myPaiementFactureBase.getAnnee(), myPaiementFactureBase.getCaissier());

                            // CONTRUCTION ID INTERNE POUR ENVOIE JIRAMA:
                            String[] dateId = getDateId(myPaiementFactureBase.getDatePaiement());
                            String idInterneMaking = dateId[0] + dateId[1] + myPaiementFactureBase.getId();
                            recu.setIdInterne(idInterneMaking);

                            Object[] obj = new Object[4];
                            obj[0] = myPaiementFactureBase.getRefFacture();
                            obj[1] = myPaiementFactureBase.getNumeroPayeur() + "_Eqima";
                            obj[2] = myPaiementFactureBase.getRefTransaction();
                            obj[3] = idInterneMaking + "_Paoma";
                            Log.e("EQ", String.valueOf(obj[3]));

                            Log.e("NUMTRANSACTION", num_transaction);
                            Log.e("NUMTRANSACTIONBASE", myPaiementFactureBase.getRefTransaction());

                            if(myPaiementFactureBase.getMontantFacture()>seuille)
                            {
                                decreaseMyCredit(num_transaction, obj, recu);
                            }
                            else if(myPaiementFactureBase.getMontantFacture()<=seuille)
                            {
                                decreaseMyCreditHalf(num_transaction, obj, recu);
                            }

                        }
                        else
                        {
                            myAlertDialog.dismiss();
                            myDatabaseManager.setEtat("CANCELED", myPaiementFactureBase.getId());
                            myDatabaseManager.close();
                            setState("CANCELED", myPaiementFactureBase.getId());
                            if(response.has("data"))
                            {
                                String dataString   = response.getString("data");
                                JSONObject dataJson = new JSONObject(dataString);
                                if(dataJson.has("errorMessage"))
                                {
                                    Toast.makeText(myContext, "Paoma error: "+ dataJson.getString("errorMessage"), Toast.LENGTH_SHORT).show();
                                }
                            }
                            else
                            {
                                Toast.makeText(myContext, "Paoma error validation", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Json error", Toast.LENGTH_LONG).show();
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
                    Log.e("ValidationCashoutError", error.getMessage());
                    Toast.makeText(myContext, "ValidationCashoutError: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("ValidationCashoutError", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "ValidationCashoutError code: "+ error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Log.e("ValidationCashoutError", error.toString());
                    Toast.makeText(myContext, "ValidationCashoutError: "+ error.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", bearerToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        validationCashoutRequest.addMarker("Validation_cashout_request");

        validationCashoutRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(validationCashoutRequest);
    };

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

    public void showValidationCashout()
    {
        View validation_cashout_layout = myLayoutInflater.inflate(R.layout.cashout_validation_layout, null);
        EditText codeEditText          = validation_cashout_layout.findViewById(R.id.codeEditText);
        EditText cinEditText           = validation_cashout_layout.findViewById(R.id.cinEditText);
        Button validerBoutton          = validation_cashout_layout.findViewById(R.id.validerBoutton);

        AlertDialog.Builder builder    = new AlertDialog.Builder(this.myContext);
        builder.setView(validation_cashout_layout);
        builder.setCancelable(false);


        builder.setPositiveButton(this.myContext.getString(R.string.fermer_label), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDemandeFermeture(myAlertDialog);
            }
        });
        myAlertDialog        = builder.create();

        // VALIDATION CASHOUT:
        validerBoutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(verifEditText(codeEditText) && verifEditText(cinEditText))
                {
                    String codeString = codeEditText.getText().toString().trim();
                    String cinString  = cinEditText.getText().toString().trim();
                    Log.e("CINANDCODE", "CIN: "+ cinString +"\n"+ "CODE: "+codeString);
                    getCreditLeft(codeString, cinString);
                }

            }
        });

        myAlertDialog.show();


    }

    public void showDemandeFermeture(AlertDialog alertDialog)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.myContext);
        builder.setTitle(this.myContext.getString(R.string.close_cashout_validation_title));
        builder.setMessage(this.myContext.getString(R.string.close_cashout_validation_message));
        builder.setCancelable(false);

        builder.setPositiveButton(this.myContext.getString(R.string.yes_label), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //alertDialog.dismiss();
            }
        });

        builder.setNegativeButton(this.myContext.getString(R.string.no_label), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showValidationCashout();
            }
        });

        builder.show();
    }

    public boolean verifEditText(EditText editText)
    {
        boolean valiny = false;
        String texte   = editText.getText().toString().trim();
        if(!texte.equals(""))
        {
            valiny = true;
        }
        else
        {
            editText.setError(this.myContext.getString(R.string.error_champ_vide));
            return false;
        }

        return valiny;
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
                        Toast.makeText(myContext, myContext.getString(R.string.reglement_pas_ok), Toast.LENGTH_SHORT).show();

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
    public void decreaseMyCredit(String num_trans,Object[] obj, Recu recu) {
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
                    setRefTransaction(num_trans, myPaiementFactureBase.getId(), obj, recu);
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

    public void decreaseMyCreditHalf(String num_trans,Object[] obj, Recu recu)
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
                    setRefTransaction(num_trans, myPaiementFactureBase.getId(), obj, recu);
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

    /******** BLUETOOTH VERSION FARANY MODIFICATION: Dantsu:ESCPOS-ThermalPrinter ******************/
    public void doPrint(View view, Recu recuS, String id) {
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
                    myDatabaseManager.miseEnDuplicata(id);
                    myDatabaseManager.close();
                    connection.disconnect();

                } else {
                    Toast.makeText(myContext, myContext.getString(R.string.no_connected_imprimante), Toast.LENGTH_SHORT).show();
                }
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
                dismissProgressDialog();
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

    // GET MY CREDIT LEFT:
    public void getCreditLeft(String codeValidation, String cin)
    {
        showProgressDialog(myContext.getString(R.string.validation_cashout_title), myContext.getString(R.string.patientez_message));

        String[] mySession = mySessionManagement.getSession();
        String url = domain_name + url_reste_credit + mySession[2].replace("EQ-", "");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("resteCredit"))
                {
                    try {
                            Log.e("NBCREDIT", response.getString("resteCredit"));
                            double resteCreditDouble = response.getDouble("resteCredit");
                            Log.e("CREDIT:", seuille+" Montant:"+ myPaiementFactureBase.getMontantFacture());
                            if(myPaiementFactureBase.getMontantFacture()>seuille)
                            {
                                if(resteCreditDouble>=1)
                                {
                                    getToken(2, codeValidation, cin);
                                }
                                else
                                {
                                    dismissProgressDialog();
                                    Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                }
                            }
                            else if(myPaiementFactureBase.getMontantFacture()<=seuille)
                            {
                                if(resteCreditDouble>=0.5)
                                {
                                    getToken(2, codeValidation, cin);
                                }
                                else
                                {
                                    dismissProgressDialog();
                                    Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                }
                            }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            Log.e("ERRORJSON", "Json error: "+ e.getMessage());
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.getMessage()!=null)
                {
                    Log.e("ERROR", error.getMessage());
                }
            }
        });

        this.myRequestQueue.add(jsonObjectRequest);
    }


    public void setRefTransaction(String transactionId, String idPaiement, Object[] obj, Recu recu)
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

}
