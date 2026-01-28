package mg.eqima.jiramacashless.cashlesswebapivolley;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import mg.eqima.jiramacashless.R;
import mg.eqima.jiramacashless.apimvolavolley.MerchantApiVolley;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.internet.InternetConnection;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.paositravolley.PaositraVolley;
import mg.eqima.jiramacashless.session.SessionManagement;

public class CashlessWebApiVolley implements Serializable {

    //URL:
    private Environnement environnement;
    private String domain_name;
    private String listePaiementFacture_url  = "/api/paiement_facture/getAll";
    private String insertPaiementFacture_url = "/api/paiement_facture/insertPost";
    private String url_reste_credit          = "/transaction/credit/credit_left?idCashpoint=";


    Context myContext;
    RequestQueue myRequestQueue;
    PaiementFacture myPaiementFactureBase;
    DatabaseManager myDatabaseManager;
    View myView;
    Activity myAtivity;
    ProgressDialog progressDialog;
    SessionManagement mySessionManagement;
    private InternetConnection internetConnection;
    Runnable myRunnable;
    Handler myHandler;
    RequestQueue requestQueueForCredit;
    Activity myActivity;
    LayoutInflater myLayoutInflater;

    // TAG:
    private final String CREDIT_MAJ_TAG = "CREDIT_MAJ";

    // CONSTRUCTOR POUR REQUETE ENREGISTREMENT:
    public CashlessWebApiVolley(Context myContext, PaiementFacture paiementFactureBase, DatabaseManager databaseManager, View view, Activity activity) {
        this.myContext             = myContext;
        this.myPaiementFactureBase = paiementFactureBase;
        this.myDatabaseManager     = databaseManager;
        this.myRequestQueue        = Volley.newRequestQueue(this.myContext);
        this.myView                = view;
        this.myAtivity             = activity;
        environnement = new Environnement();
        domain_name = environnement.getDomainName();

    }

    // CONSTRUCTOR POUR PAOSITRA AVEC LAYOUT INFLATER:
    public CashlessWebApiVolley(Context myContext, PaiementFacture paiementFactureBase, DatabaseManager databaseManager, View view, Activity activity, LayoutInflater layoutInflater) {
        this.myContext             = myContext;
        this.myPaiementFactureBase = paiementFactureBase;
        this.myDatabaseManager     = databaseManager;
        this.myRequestQueue        = Volley.newRequestQueue(this.myContext);
        this.myView                = view;
        this.myAtivity             = activity;
        this.myLayoutInflater      = layoutInflater;
        environnement = new Environnement();
        domain_name = environnement.getDomainName();

    }

    // CONSTRUCTOR POUR SYNCHRONISATION RESTE CREDIT:
    public CashlessWebApiVolley(Context context, Activity act)
    {
        mySessionManagement   = new SessionManagement(context);
        myContext             = context;
        requestQueueForCredit = Volley.newRequestQueue(myContext);
        internetConnection    = new InternetConnection();
        myActivity            = act;
        environnement = new Environnement();
        domain_name = environnement.getDomainName();
    }

    /* INITIALISATION DE L'OPERATION */
    public void startOperation()
    {
        insertPaiementFactureBase();
    }

    /*  FONCTION D'INSERTION DANS LA BASE DE DONNEES */
    public void insertPaiementFactureBase()
    {
        showProgressDialog(myContext.getString(R.string.enregistrement_title), myContext.getString(R.string.enregistrement_message));

        JSONObject bodyJson = null;
        try {
                bodyJson = new JSONObject(myPaiementFactureBase.jsonMe());
                Log.e("PAIEMENT_BASE", bodyJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            if(e.getMessage()!=null)
            {
                Toast.makeText(myContext, "Model error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            if(e.getMessage()!=null)
            {
                Toast.makeText(myContext, "Model error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        String url = domain_name + insertPaiementFacture_url;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, bodyJson, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("id"))
                {
                    try {
                        // SI L'OPERATION OBTIENT UNE id DE LA PART DE LA BASE DE DONNEES MYSQL:
                        if(!response.get("id").equals(null))
                        {
                            String idInterneReel = response.getString("id");

                            myPaiementFactureBase.setId(idInterneReel);

                            String[] dateId = getDateId(myPaiementFactureBase.getDatePaiement());
                            String idInterneMaking = dateId[0] + dateId[1] + idInterneReel;

                            // INSERTION DE LA BASE DE LA TRANSACTION DANS LA BASE SQLITE:
                            myDatabaseManager.insertBase(myPaiementFactureBase, idInterneMaking);
                            myDatabaseManager.close();


                            // INITIALISATION PAIEMENT:
                            if(myPaiementFactureBase.getOperateur().equals("Mvola"))
                            {
                                dismissProgressDialog();
                                MerchantApiVolley merchantApiVolley = new MerchantApiVolley(myContext,
                                                                        myView, myDatabaseManager,
                                                                        myAtivity, String.valueOf((int)myPaiementFactureBase.getFrais()),
                                                                        String.valueOf((int)myPaiementFactureBase.getMontantFacture()),
                                                                        myPaiementFactureBase.getNumeroCashpoint(), myPaiementFactureBase);

                                merchantApiVolley.getToken(1, myPaiementFactureBase.getNumeroPayeur());
                            }
                            else if (myPaiementFactureBase.getOperateur().equals("Paoma"))
                            {
                                dismissProgressDialog();
                                PaositraVolley paositraVolley = new PaositraVolley(myContext, String.valueOf(myPaiementFactureBase.getMontantFacture()),
                                                                myPaiementFactureBase.getNumeroPayeur(), myLayoutInflater, myPaiementFactureBase, myAtivity, myView);
                                paositraVolley.getToken(1, "", "");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, "Model error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Log.e("INSERTERROR", "Insert error message:"+ error.getMessage());
                    Toast.makeText(myContext, "Insert error message:"+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null)
                    {
                        Log.e("INSERTERROR", "Response code error: "+error.networkResponse.statusCode);
                        Toast.makeText(myContext, "Response code error: "+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        myRequestQueue.add(jsonObjectRequest);
    }

    public void dismissProgressDialog()
    {
        if (this.progressDialog!=null && this.progressDialog.isShowing())
        {
            this.progressDialog.dismiss();
        }
    }

    public void showProgressDialog(String title, String message)
    {
        dismissProgressDialog();
        Toast.makeText(myContext, message, Toast.LENGTH_SHORT).show();
        this.progressDialog = new ProgressDialog(this.myContext);
        this.progressDialog.setTitle(title);
        this.progressDialog.setMessage(message);
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.show();
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
    public void getCreditLeft(Response.Listener<JSONObject> successListener,
                              Response.ErrorListener errorListener) {
        String[] mySession = mySessionManagement.getSession();
        String url = domain_name + url_reste_credit + mySession[2].replace("EQ-", "");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Vérification et log du crédit
                            if (response.has("resteCredit")) {
                                String creditRemaining = response.getString("resteCredit");
                                Log.e("NBCREDIT", "Crédit restant : " + creditRemaining);

                                // Récupération du crédit actuel en session
                                String creditSession = mySessionManagement.getNbCredit();

                                // Mise à jour si le crédit a changé
                                if (!creditRemaining.equals(creditSession)) {
                                    double newCredit = response.getDouble("resteCredit");
                                    mySessionManagement.updateNbCreditUser(newCredit);

                                    // Actions supplémentaires en cas de changement de crédit
                                    handleCreditChange(creditSession, newCredit);
                                }

                                // Log de suivi
                                Log.d("CREDIT_SYNC", "Crédit synchronisé avec succès");
                            }

                            // Appel du listener de succès original
                            if (successListener != null) {
                                successListener.onResponse(response);
                            }

                        } catch (JSONException e) {
                            // Gestion des erreurs JSON
                            e.printStackTrace();
                            if (e.getMessage() != null) {
                                Log.e("ERRORJSON", "Erreur de parsing JSON : " + e.getMessage());
                            }

                            // Appel du listener d'erreur si nécessaire
                            if (errorListener != null) {
                                errorListener.onErrorResponse(new VolleyError("Erreur de parsing JSON"));
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Log de l'erreur
                        if (error.getMessage() != null) {
                            Log.e("ERROR", "Erreur de requête : " + error.getMessage());
                        }

                        // Gestion personnalisée des erreurs réseau
                        handleNetworkError(error);

                        // Appel du listener d'erreur original
                        if (errorListener != null) {
                            errorListener.onErrorResponse(error);
                        }
                    }
                }
        );
        // 🔧 Définir le timeout à 20 secondes
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000, // timeout en millisecondes
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        this.requestQueueForCredit.add(jsonObjectRequest);
    }

    // Méthode pour gérer les changements de crédit
    private void handleCreditChange(String oldCredit, double newCredit) {
        // Logique personnalisée en cas de changement de crédit
        // Par exemple :
        if (Double.parseDouble(oldCredit) > newCredit) {
            // Le crédit a diminué
            Log.i("CREDIT_CHANGE", "Crédit consommé. Ancien : " + oldCredit + ", Nouveau : " + newCredit);
            // Vous pouvez ajouter des notifications, des logs, etc.
        }
    }

    // Méthode pour gérer les erreurs réseau
    private void handleNetworkError(VolleyError error) {
        // Logique personnalisée de gestion des erreurs réseau
        if (error instanceof NoConnectionError) {
            Log.e("NETWORK_ERROR", "Pas de connexion internet");
            // Possibilité de déclencher une action spécifique en cas de perte de connexion
        } else if (error instanceof TimeoutError) {
            Log.e("NETWORK_ERROR", "Délai de requête dépassé");
            // Gestion du timeout
        }
        // Ajoutez d'autres types d'erreurs spécifiques si nécessaire
    }


    public interface CreditSyncCallback {
        void onSyncComplete(boolean success);
    }
    public void synchroCredit(final CreditSyncCallback callback) {
        myHandler = new Handler();
        final AtomicBoolean isSyncing = new AtomicBoolean(true);

        myRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSyncing.get()) {
                    return;
                }

                Log.e(CREDIT_MAJ_TAG, "Synchro appelé");
                if (internetConnection.checkConnection(myContext)) {
                    getCreditLeft(new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.has("resteCredit")) {
                                    String creditSession = mySessionManagement.getNbCredit();
                                    if (!response.getString("resteCredit").equals(creditSession)) {
                                        mySessionManagement.updateNbCreditUser(response.getDouble("resteCredit"));
                                    }

                                    // Arrêter la synchronisation
                                    isSyncing.set(false);
                                    myHandler.removeCallbacks(myRunnable);

                                    // Appeler le callback avec succès
                                    if (callback != null) {
                                        callback.onSyncComplete(true);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();

                                // Arrêter la synchronisation en cas d'erreur
                                isSyncing.set(false);
                                myHandler.removeCallbacks(myRunnable);

                                // Appeler le callback avec échec
                                if (callback != null) {
                                    callback.onSyncComplete(false);
                                }
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Gestion des erreurs de requête
                            Log.e("ERROR", error.getMessage() != null ? error.getMessage() : "Erreur inconnue");

                            // Arrêter la synchronisation
                            isSyncing.set(false);
                            myHandler.removeCallbacks(myRunnable);

                            // Appeler le callback avec échec
                            if (callback != null) {
                                callback.onSyncComplete(false);
                            }
                        }
                    });
                } else {
                    // Pas de connexion internet
                    isSyncing.set(false);
                    myHandler.removeCallbacks(myRunnable);

                    if (callback != null) {
                        callback.onSyncComplete(false);
                    }
                }
            }
        };

        myHandler.post(myRunnable);
    }
    public void startSync() {
        synchroCredit(new CreditSyncCallback() {
            @Override
            public void onSyncComplete(boolean success) {
                if (success) {
                    Log.d("SYNC", "Synchronisation réussie");
                    // Actions supplémentaires en cas de succès
                } else {
                    Log.d("SYNC", "Échec de synchronisation");
                    // Gestion de l'échec
                }
            }
        });
    }

    // HANDLER APPEL GET CREDIT LEFT:
//    public void synchroCredit() {
//        // Create the Handler
//        myHandler = new Handler();
//
//        // Define the code block to be executed
//        myRunnable = new Runnable() {
//            @Override
//            public void run() {
//                // Insert custom code here
//                Log.e(CREDIT_MAJ_TAG, "Synchro appelé");
//                if (internetConnection.checkConnection(myContext)) {
//                        getCreditLeft();
//                } else
//                {
//                    myHandler.removeCallbacks(myRunnable);
//                   //Toast.makeText(myContext, "sans connection internet", Toast.LENGTH_SHORT).show();
//                }
//                // Repeat every 2 seconds
//                myHandler.postDelayed(myRunnable, 3000);
//            }
//        };
//
//        // Start the Runnable immediately
//        myHandler.post(myRunnable);
//
//    }

    public void stopSynchroCredit()
    {
        myHandler.removeCallbacks(myRunnable);
    }

    public void pauseSynchroCredit()
    {
        myHandler.removeCallbacks(myRunnable);
    }

    public void resumeSynchroCredit()
    {
        myHandler.post(myRunnable);
    }
}
