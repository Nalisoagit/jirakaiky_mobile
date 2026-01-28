package mg.eqima.jiramacashless;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mg.eqima.jiramacashless.apimvolavolley.MerchantApiVolley;
import mg.eqima.jiramacashless.cashlesswebapi.AsynReglerFacture;
import mg.eqima.jiramacashless.cashlesswebapi.AsyncInsertCashlessWebApi;
import mg.eqima.jiramacashless.cashlesswebapi.JiramaRestApi;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.dataobject.PaiementFactureData;
import mg.eqima.jiramacashless.internet.InternetConnection;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.recyclerview.RecyclerViewCardAdapter;
import mg.eqima.jiramacashless.recyclerview.RecyclerViewInterface;
import mg.eqima.jiramacashless.session.SessionManagement;
import mg.eqima.jiramacashless.synchrofunction.SynchFonction;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SynchronizationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SynchronizationFragment extends Fragment implements RecyclerViewInterface {

    DatabaseManager databaseManager;
    RecyclerView recyclerView;
    Context myContext;
    RequestQueue myRequestQueue;
    InternetConnection internetConnection;
    public static BackHome backHome;

    Button synchronisationButton;
    EditText refTransactionAVerifierEditText;
    EditText codePaomaEditText;
    EditText cinPaomaEditText;


    List<PaiementFactureData> listeNonSynchro;

    ProgressDialog progressDialog;
    RecyclerViewCardAdapter recyclerViewCardAdapter;
    SessionManagement sessionManagement;

    // URL

    private Environnement environnement;

    private String domain_name;
    private String url_mvola  ;

    private String url_set_recu              = "/api/paiement_facture/setCodeRecu?coderecu=";
    private String url_success_state          = "/api/paiement_facture/setSuccessState?id=";
    private String urlCallback               = domain_name + "/rombo/callback";
    private String url_verif_transaction     = "/rombo/findByServerCorrelationId?serverCorrelationId=";
    private String url_reste_credit          = "/transaction/credit/credit_left?idCashpoint=";

    private String urlToken                  = "/token";
    private String urlForInitiateTransaction = "/mvola/mm/transactions/type/merchantpay/1.0.0/";
    private String urlForTransactionDetails  = "/mvola/mm/transactions/type/merchantpay/1.0.0/"; //{{transID}}
    private String urlForTransactionStatus   = "/mvola/mm/transactions/type/merchantpay/1.0.0/status/"; //{{serverCorrelationId}}


    // PAOSITRA:
    private String url_paositra_test        = "https://ws-paoma.numherit-dev.com";
    private String url_paositra_prod        = "";
    private String url_paositra             = url_paositra_test;

    private String url_token                = "/auth/loginUserAgence";
    private String url_validation_cashout   = "/monetique/operation/validationRetraitEspeceByCodeRetrait";

    private String login                    = "equima";
    private String password                 = "8ND@14T7f";

    private String url_state                 = "/api/paiement_facture/setState?etat=";

    private String urt_set_ref_transaction   = "/api/paiement_facture/setRefTransaction?reftransaction=";
    private String url_deduction_credit      = "/transaction/credit/credit_decrease";
    private String url_deduction_credit_half = "/transaction/credit/credit_decrease_half";
    private String url_get_code_recu         = "/jiramacontroller/recuFacture?referencefacture=";
    private String insertPaiementFacture_url = "/api/paiement_facture/insertPost";



    private double seuille = 10000;

    public SynchronizationFragment() {
        // Required empty public constructor
        environnement = new Environnement();
        domain_name = environnement.getDomainName();
        url_mvola = environnement.getDomainNameMvola();
    }

    public SynchronizationFragment(DatabaseManager dbManager, Context context, BackHome homeActivity) {
        // Required empty public constructor
        this.setDatabaseManager(dbManager);
        myContext = context;
        myRequestQueue = Volley.newRequestQueue(myContext);
        internetConnection = new InternetConnection();
        backHome = homeActivity;
        environnement = new Environnement();
        domain_name = environnement.getDomainName();
        url_mvola = environnement.getDomainNameMvola();
    }

    private void setDatabaseManager(DatabaseManager dbManager) {
        this.databaseManager = dbManager;
    }


    public static SynchronizationFragment newInstance(String param1, String param2) {
        SynchronizationFragment fragment = new SynchronizationFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inf = inflater.inflate(R.layout.fragment_synchronization, container, false);
        recyclerView = inf.findViewById(R.id.recyclerViewSynchronisation);
        Log.e("STRING:",databaseManager.toString());
        listeNonSynchro = databaseManager.getListeForSynchronization();
        sessionManagement = new SessionManagement(myContext);

        Log.e("TAAAAA:",String.valueOf(listeNonSynchro.size()));
        recyclerViewCardAdapter = new RecyclerViewCardAdapter(container.getContext(), listeNonSynchro, this);
        recyclerView.setAdapter(recyclerViewCardAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        return inf;
    }

    @Override
    public void onClickItemListener(int position) {
        final View synchroDialogView    = getLayoutInflater().inflate(R.layout.synchro_dialog2, null);
        synchronisationButton           = synchroDialogView.findViewById(R.id.synchro_button);
        refTransactionAVerifierEditText = synchroDialogView.findViewById(R.id.refTrasactionAVerifier);
        codePaomaEditText               = synchroDialogView.findViewById(R.id.codePaoma);
        cinPaomaEditText               = synchroDialogView.findViewById(R.id.cinPaoma);

        // OBTENTION DE L'ITEM SELECTIONNE:
        PaiementFactureData paiementFactureData = listeNonSynchro.get(position);
        String etatPaiementFacture = paiementFactureData.getEtat();

        // VERIFICATION SI REF TRANSACTION:
        if(paiementFactureData.getRefTransaction()!=null)
        {
            // SI IL Y A REF TRANSACTION:
            refTransactionAVerifierEditText.setText(paiementFactureData.getRefTransaction());
            refTransactionAVerifierEditText.setEnabled(false);
            codePaomaEditText.setEnabled(false);
            cinPaomaEditText.setEnabled(false);
            codePaomaEditText.setVisibility(View.GONE);
            cinPaomaEditText.setVisibility(View.GONE);
        }
        else if(paiementFactureData.getRefTransaction()==null){
                if(paiementFactureData.getOperateur().equals("Mvola"))
                {
                    codePaomaEditText.setEnabled(false);
                    cinPaomaEditText.setEnabled(false);
                    codePaomaEditText.setVisibility(View.GONE);
                    cinPaomaEditText.setVisibility(View.GONE);
                }
                else if(paiementFactureData.getOperateur().equals("Paoma"))
                {
                    refTransactionAVerifierEditText.setEnabled(false);
                    refTransactionAVerifierEditText.setVisibility(View.GONE);
                }
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(R.string.synchronisation);
        alertDialogBuilder.setIcon(R.drawable.synchro_alert_ico);
        alertDialogBuilder.setView(synchroDialogView);
        alertDialogBuilder.setPositiveButton(myContext.getString(R.string.retour_label), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        // CLICK LISTENER DU BOUTON
        synchronisationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(internetConnection.checkConnection(myContext))
                {

                    if(etatPaiementFacture.equals("INITIE") || etatPaiementFacture.equals("CANCELED"))
                    {
                        Log.e("SYNC", "Paiement non validé");
                        if(paiementFactureData.getRefTransaction()==null)
                        {
                            if(paiementFactureData.getOperateur().equals("Mvola"))
                            {
                                String refTransactionAVerifierString = refTransactionAVerifierEditText.getText().toString().trim();
                                if(!refTransactionAVerifierString.equals(""))
                                {
                                    alertDialog.dismiss();
                                    Log.e("VERIFICATION", refTransactionAVerifierString);
                                    String[] sessionUser = sessionManagement.getSession();
                                    double myCredit = Double.valueOf(sessionUser[3]);
                                    if(Double.valueOf(paiementFactureData.getMontantFacture())>seuille)
                                    {
                                        if(myCredit>=1)
                                        {
                                            getTokenForSynchro(1,refTransactionAVerifierString, paiementFactureData.getNumeroPayeur(), paiementFactureData, position);
                                        }
                                        else
                                        {
                                            Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else if(Double.valueOf(paiementFactureData.getMontantFacture())<=seuille)
                                    {
                                        if(myCredit>=0.5)
                                        {
                                            getTokenForSynchro(1,refTransactionAVerifierString, paiementFactureData.getNumeroPayeur(), paiementFactureData, position);
                                        }
                                        else
                                        {
                                            Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                                else
                                {
                                    refTransactionAVerifierEditText.setError(myContext.getString(R.string.error_champ_vide));
                                    return;
                                }
                            }
                            else if(paiementFactureData.getOperateur().equals("Paoma"))
                            {
                                Log.e("ATOAH", "PAOMA");
                                String codePaomaString               = codePaomaEditText.getText().toString().trim();
                                String cinPaomaString                = cinPaomaEditText.getText().toString().trim();
                                if(!codePaomaString.equals(""))
                                {
                                    if(!cinPaomaString.equals(""))
                                    {
                                        alertDialog.dismiss();
                                        Log.e("VERIFICATION", codePaomaString);
                                        String[] sessionUser = sessionManagement.getSession();
                                        double myCredit = Double.valueOf(sessionUser[3]);
                                        if(Double.valueOf(paiementFactureData.getMontantFacture())>seuille)
                                        {
                                            if(myCredit>=1)
                                            {
                                                getCreditLeft(codePaomaString, cinPaomaString, paiementFactureData, position);
                                                //getTokenForSynchro(1,refTransactionAVerifierString, paiementFactureData.getNumeroPayeur(), paiementFactureData, position);
                                            }
                                            else
                                            {
                                                Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        else if(Double.valueOf(paiementFactureData.getMontantFacture())<=seuille)
                                        {
                                            if(myCredit>=0.5)
                                            {
                                                getCreditLeft(codePaomaString, cinPaomaString, paiementFactureData, position);
                                            }
                                            else
                                            {
                                                Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                    else
                                    {
                                        cinPaomaEditText.setError(myContext.getString(R.string.error_champ_vide));
                                        return;
                                    }

                                }
                                else
                                {
                                    codePaomaEditText.setError(myContext.getString(R.string.error_champ_vide));
                                    return;
                                }
                            }
                        }

                    }
                    else if(etatPaiementFacture.equals("PAID") || etatPaiementFacture.equals("FAILED"))
                    {
                        alertDialog.dismiss();
                        Log.e("SYNC", "Facture non reglé");
                        Toast.makeText(getContext(), "Facture non reglé", Toast.LENGTH_SHORT).show();
                        verifCodeRecu(listeNonSynchro.get(position).getRefFacture(), listeNonSynchro.get(position), position);
                        //synchroForJiramaOnly(listeNonSynchro.get(position), position);
                    }
                }
                else{
                    Toast.makeText(myContext, myContext.getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void removeFromList(int position) {
        listeNonSynchro.remove(position);
        recyclerViewCardAdapter.notifyItemRemoved(position);
    }


    public void synchroForJiramaOnly(PaiementFactureData contenantData, int positionS)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*BackHome.backHome.runOnUiThread(new Runnable() {
                    public void run() {
                        //Do your UI operations like dialog opening or Toast here
                        progressDialog = new ProgressDialog(getContext());
                        progressDialog.setTitle(getContext().getString(R.string.reglement_title));
                        progressDialog.setMessage(getContext().getString(R.string.reglement_facture));
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.show();
                    }
                });*/

                String[] dateId = getDateId(contenantData.getDaty());
                String idSimple = "";

                //databaseManager.deMarkForSynchronization(paiementFactureForMysql.getRefTransaction());
                //databaseManager.setIdInterne(contenantData.getRefTransaction(), idInterneString);
                //databaseManager.close();

                // REGLEMENT DE FACTURE:

                AsynReglerFacture asyncReglerFacture = new AsynReglerFacture();
                Object[] obj = new Object[5];
                obj[0] = contenantData.getRefFacture();
                obj[1] = contenantData.getNumeroPayeur() + "_Eqima";
                obj[2] = contenantData.getRefTransaction();
                obj[3] = contenantData.getIdInterne() + "_" + contenantData.getOperateur();
                obj[4] = contenantData.getOperateur();
                Log.e("IDINTERNE", String.valueOf(obj[3]));
                Map<String, String> valinyRecuJirama = null;
                try {
                    valinyRecuJirama = asyncReglerFacture.execute(obj).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(valinyRecuJirama!=null){
                if (!valinyRecuJirama.get("recucode").equals("") && valinyRecuJirama.get("recucode") != null  && valinyRecuJirama.get("errors").equals("")) {
                    // INSERTION RECU CODE DANS SQLITE:
                    databaseManager.setCodeRecu(valinyRecuJirama.get("recucode"), contenantData.getIdInterneReel());
                    databaseManager.close();
                    databaseManager.setSuccessState(contenantData.getIdInterneReel());
                    databaseManager.close();

                    BackHome.backHome.runOnUiThread(new Runnable() {
                        public void run() {
                            //Do your UI operations like dialog opening or Toast here
                            dismissProgressDialog();
                            Toast.makeText(getContext(), getContext().getString(R.string.reglement_ok), Toast.LENGTH_SHORT).show();
                            progressDialog = new ProgressDialog(getContext());
                            progressDialog.setTitle(getContext().getString(R.string.enregistrement_title));
                            progressDialog.setMessage(getContext().getString(R.string.enregistrement_base));
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.show();
                        }
                    });

                    //setCodeRecu(valinyRecuJirama.get("recucode"),idSimple);
                    PaiementFacture paiementFactureForMysql = new PaiementFacture(Double.valueOf(contenantData.getFrais()),
                            contenantData.getLatitude(), contenantData.getLongitude(),
                            Double.valueOf(contenantData.getMontantFacture()), contenantData.getNumeroPayeur(),
                            contenantData.getNumeroCashpoint(), contenantData.getOperateur(),
                            contenantData.getRefFacture(), contenantData.getRefTransaction(),
                            Double.valueOf(contenantData.getTotale()), contenantData.getDaty(), contenantData.getRefClient(), contenantData.getNomClient(), contenantData.getAdresseClient(), contenantData.getMois(), contenantData.getAnnee());

                    paiementFactureForMysql.setCodeRecu(valinyRecuJirama.get("recucode"));
                    paiementFactureForMysql.setAnterieur(contenantData.getIdInterne());
                    paiementFactureForMysql.setEtat("SUCCESS");
                    paiementFactureForMysql.setRefTransaction(contenantData.getRefTransaction());
                    paiementFactureForMysql.setCaissier(contenantData.getCaissier());
                    paiementFactureForMysql.setIdDistributeur(contenantData.getIdDistributeur());

                    //ENREGISTREMENT DANS MYSQL:
                    Object[] objectsPaiementFacture = new Object[1];
                    objectsPaiementFacture[0] = paiementFactureForMysql;
                    AsyncInsertCashlessWebApi asyncInsertCashlessWebApi = new AsyncInsertCashlessWebApi();
                    String insertionPaiementString = null;
                    JSONObject insertionPaiementJson = null;
                    try {
                        insertionPaiementString = asyncInsertCashlessWebApi.execute(objectsPaiementFacture).get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        if (e.getMessage() != null) {
                            BackHome.backHome.runOnUiThread(new Runnable() {
                                public void run() {
                                    //Do your UI operations like dialog opening or Toast here
                                    Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (e.getMessage() != null) {
                            BackHome.backHome.runOnUiThread(new Runnable() {
                                public void run() {
                                    //Do your UI operations like dialog opening or Toast here
                                    Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    // MISE DANS JSON DE LA REPONSE:
                    try {
                        insertionPaiementJson = new JSONObject(insertionPaiementString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (e.getMessage() != null) {
                            BackHome.backHome.runOnUiThread(new Runnable() {
                                public void run() {
                                    //Do your UI operations like dialog opening or Toast here
                                    Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    //setSuccessState(idSimple);
                    try {
                        if (!insertionPaiementJson.get("id").equals(null)) {
                            //databaseManager.setAnterieur(contenantData.getRefTransaction(), contenantData.getIdInterne());
                            //databaseManager.setSuccessState(contenantData.getRefFacture(), contenantData.getDaty());
                            //databaseManager.close();
                            BackHome.backHome.runOnUiThread(new Runnable() {
                                public void run() {
                                    //Do your UI operations like dialog opening or Toast here
                                    removeFromList(positionS);
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), getContext().getString(R.string.enregistrement_ok), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (e.getMessage() != null) {
                            BackHome.backHome.runOnUiThread(new Runnable() {
                                public void run() {
                                    //Do your UI operations like dialog opening or Toast here
                                    Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                }
            }
                else{
                    BackHome.backHome.runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            //Do your UI operations like dialog opening or Toast here
                            Toast.makeText(myContext, "Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public String[] getDateId(String daty)
    {
        Log.e("DATE",daty.substring(0,4));
        Log.e("DATE",daty.substring(5,7));
        String annee = daty.substring(0,4);
        String mois = daty.substring(5,7);
        if (mois.startsWith("0"))
        {
            mois = mois.substring(1,2);
        }

        String[] valiny = new String[2];
        valiny[0] = annee;
        valiny[1] = mois;

        Log.e("DATEEE",valiny[0]);
        Log.e("DATEEE",valiny[1]);

        return valiny;
    }

    public void setCodeRecu(String coderecu, String id, int position)
    {
        String url = domain_name+url_set_recu+coderecu+"&id="+id;
        //RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RECU", response);
                setSuccessState(id, position);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.getMessage()!=null)
                {
                    Toast.makeText(getContext(), getContext().getString(R.string.error)+error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: "+ error.getMessage());
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

    public void setSuccessState(String id, int positionS)
    {
        String url = domain_name+url_success_state+id;
        //RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("STATE", response);
                BackHome.backHome.runOnUiThread(new Runnable() {
                    public void run(){
                        //Do your UI operations like dialog opening or Toast here
                        removeFromList(positionS);
                        progressDialog.dismiss();
                        Toast.makeText(myContext, myContext.getString(R.string.reglement_ok), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.getMessage()!=null)
                {
                    Toast.makeText(getContext(), getContext().getString(R.string.error)+"Etat success\n"+error.toString(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: "+ error.toString());
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

    public void dismissProgressDialog()
    {
        if (this.progressDialog!=null && this.progressDialog.isShowing())
        {
            this.progressDialog.dismiss();
        }
    }

    /*** SYNCHRO CONCERNANT MVOLA: ******/
    public void getTransactionStatusWithServerCorrelationIdForSync(String token, String serverCorrelationId, int idTrans, String customerNumber, PaiementFactureData paiementFactureData, int position)
    {
        showProgressDialog(myContext.getString(R.string.commencement), myContext.getString(R.string.patientez_message));
        // TOKEN ET GENERATION uuid POUR L'OPERATION DE GET STATUS D'UNE TRANSACTION:
        String bearerToken = token;
        String uuid = uuidGenerator();

        // CREATION REQUETES ET DEMARRAGE GET STATUS:
        String urlStatus = url_mvola + urlForTransactionStatus + serverCorrelationId;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlStatus, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("status") && response.has("objectReference"))
                {
                    try {
                        String status = response.getString("status");
                        String objectReference = response.getString("objectReference");
                        Log.e("STATUS", "Statut: "+status+"\n Object reference: "+ objectReference);
                        if(!status.equals("pending")){
                            Log.e("STOPSTATUS", "Status: "+status+"\n Object reference: "+ objectReference);
                            if(status.equals("completed"))
                            {
                                // SI LE STATUS EST COMPLETED => GET DETAILS TRANSACTION:
                                getTransactionDetails(bearerToken, objectReference, idTrans, customerNumber, paiementFactureData, position);
                            }
                            else
                            {
                                // SI LE STATUS EST FAILED ALORS MISE EN ETAT CANCELED DANS SQLITE:
                                dismissProgressDialog();
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

    public void getTokenForSynchro(int idTrans,String refTransaction, String customNumber, PaiementFactureData paiementFactureData, int position)
    {
        showProgressDialog(myContext.getString(R.string.commencement), myContext.getString(R.string.patientez_message));
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
                        String tokenS = jsonObjectResponse.getString("access_token");

                        String bearerToken = "Bearer " + tokenS;
                        getTransactionDetails(bearerToken, refTransaction, idTrans, customNumber, paiementFactureData, position);
                        //getTransactionStatusWithServerCorrelationIdForSync(bearerToken, paiementFactureData.getServerCorrelationId(),
                                                                       // 1, paiementFactureData.getNumeroPayeur(), paiementFactureData, position);
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
                    if(error.networkResponse!=null) {
                        Log.e("Token_code_error: ", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "Get token failed code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
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
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(stringRequest);
    }

    public void getTransactionDetails(String token, String transId, int idTrans, String customerNumber, PaiementFactureData paiementFactureData, int position)
    {
        //GET BEARER TOKEN AND UUID to identify the transaction:
        String bearerToken = token;
        String uuid = uuidGenerator();

        // CONSTRUCTION REQUETE:
        String urlDetails = url_mvola + urlForTransactionDetails + transId;
        JsonObjectRequest jsonObjectRequestDetails = new JsonObjectRequest(Request.Method.GET,
                urlDetails, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("DETAILRESPONSE", response.toString());
                //dismissProgressDialog();
                if(idTrans==0)
                {
                    // Frais:
                    Toast.makeText(myContext, "Frais payé", Toast.LENGTH_SHORT).show();
                    //getToken(1, customerNumber);

                }
                else if(idTrans==1)
                {
                    // Montant facture:
                    //Toast.makeText(myContext, myContext.getString(R.string.details_done), Toast.LENGTH_SHORT).show();                    // Mise des detail:
                    //dismissProgressDialog();
                    if(response.has("transactionStatus") && response.has("amount") && response.has("transactionReference") && response.has("requestDate"))
                    {
                        try {
                                // OBTENTION DES ELEMENT A VERIFIER:
                                    String amount  = response.getString("amount");
                                    String transactionStatus = response.getString("transactionStatus");
                                    String creationDate      = response.getString("creationDate");
                                    String transactionReference = response.getString("transactionReference");
                                    JSONArray customerNumberJSONArray = new JSONArray(response.getString("debitParty"));
                                    JSONArray merchantNumberJSONArray = new JSONArray(response.getString("creditParty"));

                                    String customerNumber = new JSONObject(customerNumberJSONArray.getString(0)).getString("value");
                                    String merchantNumber = new JSONObject(merchantNumberJSONArray.getString(0)).getString("value");

                                    Log.e("DETAILS", "Montant: " + amount);
                                    Log.e("DETAILS", "Status: " + transactionStatus);
                                    Log.e("DETAILS", "Creation date: " + creationDate);
                                    Log.e("DETAILS", "Transaction reference: " + transactionReference);
                                    Log.e("DETAILS", "Numero payeur: " + customerNumber);
                                    Log.e("DETAILS", "Numero marchand: " + merchantNumber);
                                    Log.e("MONTANT", String.valueOf(Math.round(Double.valueOf(amount))));
                                    Log.e("MONTANT2", paiementFactureData.getMontantFacture());

                                    if(transactionStatus.equals("completed"))
                                    {
                                        if(String.valueOf(Math.round(Double.valueOf(paiementFactureData.getMontantFacture()))).equals(String.valueOf(Math.round(Double.valueOf(amount)))))
                                        {
                                            Log.e("VERIF", "Montant ok");
                                            if(paiementFactureData.getNumeroPayeur().equals(customerNumber))
                                            {
                                                Log.e("VERIF", "Numero payeur ok");
                                                if(merchantNumber.equals(paiementFactureData.getMerchantNumber()))
                                                {
                                                    Log.e("VERIF", "Numero marchand ok");
                                                    SynchFonction synchFonction = new SynchFonction();
                                                    Map<String, Long> differenceBetweenDateMap = synchFonction.getDifferenceBetweenToDate(creationDate, paiementFactureData.getDaty());
                                                    boolean verdictDate = synchFonction.verdictDate(differenceBetweenDateMap);

                                                    Log.e("VERDICT", String.valueOf(verdictDate));
                                                    if(verdictDate==true)
                                                    {
                                                        // Mise ref transaction dans sqlite:
                                                        paiementFactureData.setRefTransaction(transId);
                                                        databaseManager.setRefTransaction(transId, paiementFactureData.getIdInterneReel());
                                                        databaseManager.close();
                                                        databaseManager.setEtat("PAID", paiementFactureData.getIdInterneReel());
                                                        databaseManager.close();
                                                        // Mise des données mvola:
                                                        String transactionReferenceForPr = "";
                                                        String amountForPrTotale = "";
                                                        String requestDateForPr = "";
                                                        String numberForPr = customerNumber;
                                                        String fraisForPr = paiementFactureData.getFrais();
                                                        String operateurForPr = "Mvola";

                                                        try {
                                                            amountForPrTotale = String.valueOf(Double.valueOf(response.getString("amount"))+Double.valueOf(paiementFactureData.getFrais()));
                                                            transactionReferenceForPr = response.getString("transactionReference");
                                                            requestDateForPr = response.getString("creationDate");
                                                        } catch (JSONException e) {
                                                            dismissProgressDialog();
                                                            e.printStackTrace();
                                                            if(e.getMessage()!=null)
                                                            {
                                                                Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                        //dismissProgressDialog();
                                                        // Mise refTransaction dans base de donnee Mysql:
                                                        decreaseMyCredit(transId, paiementFactureData, position);


                                                    }
                                                    else
                                                    {
                                                        dismissProgressDialog();
                                                        Toast.makeText(myContext, myContext.getString(R.string.transId_error), Toast.LENGTH_SHORT).show();
                                                        Log.e("VERIF", "Date pas ok");
                                                    }
                                                }
                                                else
                                                {
                                                    dismissProgressDialog();
                                                    Toast.makeText(myContext, myContext.getString(R.string.transId_error), Toast.LENGTH_SHORT).show();
                                                    Log.e("VERIF", "Numero marchand pas ok");
                                                }
                                            }
                                            else
                                            {
                                                dismissProgressDialog();
                                                Toast.makeText(myContext, myContext.getString(R.string.transId_error), Toast.LENGTH_SHORT).show();
                                                Log.e("VERIF", "Numero payeur pas ok");
                                            }

                                        }
                                        else
                                        {
                                            dismissProgressDialog();
                                            Toast.makeText(myContext, myContext.getString(R.string.transId_error), Toast.LENGTH_SHORT).show();
                                            Log.e("VERIF", "Montant pas ok");
                                        }
                                    }
                                    else
                                    {
                                        dismissProgressDialog();
                                        Toast.makeText(myContext, myContext.getString(R.string.transId_error), Toast.LENGTH_SHORT).show();
                                        Log.e("VERIF", "Status pas ok");
                                    }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            dismissProgressDialog();
                            if(e!=null)
                            {
                                if(e.getMessage()!=null)
                                {
                                    Toast.makeText(myContext, "Json error", Toast.LENGTH_SHORT).show();
                                }
                            }
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
                    Log.e("DetailTransaction: ", error.getMessage());
                    Toast.makeText(myContext, "Detail transaction failed: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    if(error.networkResponse!=null) {
                        Log.e("DetailTransactionCode: ", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(myContext, "Detail transaction failed code: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
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

    public void setRefTransaction(String transactionId, String idPaiement, String requestDateForPr, String numberForPr, String fraisForPr, String amountForPrTotale, String operateurForPr, PaiementFactureData paiementFactureData, int position)
    {
        String url = domain_name + urt_set_ref_transaction + transactionId+"&idPaiement="+idPaiement;
        //RequestQueue requestQueue = Volley.newRequestQueue(myContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
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
                        if(reponseSetRefTransaction.getString("response").equals("ok"))
                        {
                            Toast.makeText(myContext, "Ref transaction enregistrement: "+ reponseSetRefTransaction.getString("response"), Toast.LENGTH_SHORT).show();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    BackHome.backHome.runOnUiThread(new Runnable() {
                                        public void run() {
                                            //Do your UI operations like dialog opening or Toast here
                                            dismissProgressDialog();
                                            Toast.makeText(BackHome.backHome, BackHome.backHome.getString(R.string.reglement_title), Toast.LENGTH_SHORT).show();
                                            progressDialog = new ProgressDialog(BackHome.backHome);
                                            progressDialog.setTitle(BackHome.backHome.getString(R.string.reglement_title));
                                            progressDialog.setMessage(BackHome.backHome.getString(R.string.reglement_facture));
                                            progressDialog.setCanceledOnTouchOutside(false);
                                            progressDialog.show();
                                        }
                                    });
                                    /************* REGLEMENT FACTURE: ******************/
                                    // CREATION DE L'OBJET HASHMAP POUR FIREBASE:
                                    String[] dateId = getDateId(paiementFactureData.getDaty());
                                    String idInterneMaking = dateId[0] + dateId[1] + paiementFactureData.getIdInterneReel();

                                    AsynReglerFacture asyncReglerFacture = new AsynReglerFacture();
                                    Object[] obj = new Object[5];
                                    obj[0] = paiementFactureData.getRefFacture();
                                    obj[1] = paiementFactureData.getNumeroPayeur() + "_Eqima";
                                    obj[2] = transactionId;
                                    obj[3] = idInterneMaking + "_"+paiementFactureData.getOperateur();
                                    obj[4] = paiementFactureData.getOperateur();
                                    Log.e("EQ", String.valueOf(obj[3]));
                                    Map<String, String> valinyRecuJirama = null;
                                    try {
                                          valinyRecuJirama = asyncReglerFacture.execute(obj).get();
                                    } catch (ExecutionException e) {
                                        dismissProgressDialog();
                                        e.printStackTrace();
                                        if(e.getMessage()!=null)
                                        {
                                            Toast.makeText(myContext, "Json error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (InterruptedException e) {
                                        dismissProgressDialog();
                                        e.printStackTrace();
                                        if(e.getMessage()!=null)
                                        {
                                            Toast.makeText(myContext, "Json error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    if(valinyRecuJirama!=null)
                                    {
                                        if (valinyRecuJirama.get("recucode") != null && !valinyRecuJirama.get("recucode").equals("") && valinyRecuJirama.get("errors").equals("")) {
                                            Log.e("RECUCOOOODE", valinyRecuJirama.get("recucode"));
                                            String code_recu = valinyRecuJirama.get("recucode");
                                            databaseManager.setCodeRecu(code_recu, paiementFactureData.getIdInterneReel());
                                            databaseManager.close();

                                            // ETO IZANY TOKONY INSERT VAOVAO:
                                            BackHome.backHome.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    //Do your UI operations like dialog opening or Toast here
                                                    dismissProgressDialog();
                                                    Toast.makeText(myContext, myContext.getString(R.string.reglement_ok), Toast.LENGTH_SHORT).show();
                                                    progressDialog = new ProgressDialog(myContext);
                                                    progressDialog.setTitle(myContext.getString(R.string.enregistrement_title));
                                                    progressDialog.setMessage(myContext.getString(R.string.enregistrement_base));
                                                    progressDialog.setCanceledOnTouchOutside(false);
                                                    progressDialog.show();
                                                }
                                            });

                                            PaiementFacture paiementFactureForMysql = new PaiementFacture(Double.valueOf(paiementFactureData.getFrais()),
                                                    paiementFactureData.getLatitude(), paiementFactureData.getLongitude(),
                                                    Double.valueOf(paiementFactureData.getMontantFacture()), paiementFactureData.getNumeroPayeur(),
                                                    paiementFactureData.getNumeroCashpoint(), paiementFactureData.getOperateur(),
                                                    paiementFactureData.getRefFacture(), paiementFactureData.getRefTransaction(),
                                                    Double.valueOf(paiementFactureData.getTotale()), paiementFactureData.getDaty(), paiementFactureData.getRefClient(), paiementFactureData.getNomClient(), paiementFactureData.getAdresseClient(), paiementFactureData.getMois(), paiementFactureData.getAnnee());

                                            paiementFactureForMysql.setCodeRecu(code_recu);
                                            paiementFactureForMysql.setAnterieur(paiementFactureData.getIdInterne());
                                            paiementFactureForMysql.setEtat("SUCCESS");
                                            paiementFactureForMysql.setRefTransaction(paiementFactureData.getRefTransaction());
                                            paiementFactureForMysql.setCaissier(paiementFactureData.getCaissier());
                                            paiementFactureForMysql.setIdDistributeur(paiementFactureData.getIdDistributeur());

                                            //ENREGISTREMENT DANS MYSQL:
                                            Object[] objectsPaiementFacture = new Object[1];
                                            objectsPaiementFacture[0] = paiementFactureForMysql;
                                            AsyncInsertCashlessWebApi asyncInsertCashlessWebApi = new AsyncInsertCashlessWebApi();
                                            String insertionPaiementString = null;
                                            JSONObject insertionPaiementJson = null;
                                            try {
                                                insertionPaiementString = asyncInsertCashlessWebApi.execute(objectsPaiementFacture).get();
                                            } catch (ExecutionException e) {
                                                e.printStackTrace();
                                                if (e.getMessage() != null) {
                                                    BackHome.backHome.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Do your UI operations like dialog opening or Toast here
                                                            Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                                if (e.getMessage() != null) {
                                                    BackHome.backHome.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Do your UI operations like dialog opening or Toast here
                                                            Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }
                                            // MISE DANS JSON DE LA REPONSE:
                                            try {
                                                insertionPaiementJson = new JSONObject(insertionPaiementString);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                if (e.getMessage() != null) {
                                                    BackHome.backHome.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Do your UI operations like dialog opening or Toast here
                                                            dismissProgressDialog();
                                                            Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }

                                            //setSuccessState(idSimple);
                                            try {
                                                if (!insertionPaiementJson.get("id").equals(null)) {
                                                    //databaseManager.setAnterieur(contenantData.getRefTransaction(), contenantData.getIdInterne());
                                                    //databaseManager.setSuccessState(contenantData.getRefFacture(), contenantData.getDaty());
                                                    //databaseManager.close();
                                                    databaseManager.setSuccessState(paiementFactureData.getIdInterneReel());
                                                    databaseManager.close();

                                                    BackHome.backHome.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Do your UI operations like dialog opening or Toast here
                                                            removeFromList(position);
                                                            dismissProgressDialog();
                                                            Toast.makeText(myContext, myContext.getString(R.string.enregistrement_ok), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                if (e.getMessage() != null) {
                                                    BackHome.backHome.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Do your UI operations like dialog opening or Toast here
                                                            dismissProgressDialog();
                                                            Toast.makeText(myContext, "Json error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }

                                            //setCodeRecu(code_recu, paiementFactureData.getIdInterneReel(), position);

                                        }
                                        else{
                                                databaseManager.setEtat("FAILED", paiementFactureData.getIdInterneReel());
                                                databaseManager.close();
                                                setState("FAILED", paiementFactureData.getIdInterneReel());
                                                BackHome.backHome.runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(myContext, myContext.getString(R.string.reglement_pas_ok), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                        }
                                    }

                                }
                            }).start();

                        }
                    } catch (JSONException e) {
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
                    if(error.networkResponse!=null) {
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
                if(error.getMessage()!=null) {
                    Toast.makeText(myContext, myContext.getString(R.string.error) + "Etat success\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: " + error.getMessage());
                    if (error.networkResponse != null) {
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

    public void showProgressDialog(String title, String message)
    {
        dismissProgressDialog();
        this.progressDialog = new ProgressDialog(this.myContext);
        this.progressDialog.setTitle(title);
        this.progressDialog.setMessage(message);
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.show();
    }

    // CREDIT DECREASE:
    // DECREASE CREDIT:
    public void decreaseMyCredit(String transId, PaiementFactureData paiementFactureData, int position) {
        String caissier = paiementFactureData.getCaissier();
        String id       = caissier.replace("EQ-", "");
        String url      = domain_name + url_deduction_credit;
        String info     = "{\n" +
                            "    \"id_cashpoint\" : \""+id+"\",\n" +
                            "    \"date_request\" : \""+paiementFactureData.getDaty()+"\"\n" +
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
                    setRefTransaction(transId, paiementFactureData.getIdInterneReel(), paiementFactureData.getDaty(), paiementFactureData.getNumeroPayeur(), paiementFactureData.getFrais(), paiementFactureData.getTotale(), paiementFactureData.getOperateur(), paiementFactureData, position);;
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

    public void decreaseMyCreditHalf(String transId, PaiementFactureData paiementFactureData, int position)
    {
        String caissier = paiementFactureData.getCaissier();
        String id       = caissier.replace("EQ-", "");
        String url      = domain_name + url_deduction_credit_half;
        String info     = "{\n" +
                            "    \"id_cashpoint\" : \""+id+"\",\n" +
                            "    \"date_request\" : \""+paiementFactureData.getDaty()+"\"\n" +
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
                    setRefTransaction(transId, paiementFactureData.getIdInterneReel(), paiementFactureData.getDaty(), paiementFactureData.getNumeroPayeur(), paiementFactureData.getFrais(), paiementFactureData.getTotale(), paiementFactureData.getOperateur(), paiementFactureData, position);;
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

    public void verifCodeRecu(String refFacture, PaiementFactureData paiementFactureData, int positionS)
    {
        showProgressDialog(myContext.getString(R.string.reglement_title), myContext.getString(R.string.reglement_facture));
        String url = domain_name + url_get_code_recu + refFacture;
        StringRequest stringRequestRecu = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("CODERECUVERIF", response);
                JiramaRestApi jiramaRestApi = new JiramaRestApi();
                try {
                        Map<String, String> valiny_get_recu = jiramaRestApi.traitementGetCodeRecu(response);
                        Log.e("VALINYMAP", valiny_get_recu.toString());
                        if(valiny_get_recu.get("errors").equals("N"))
                        {
                            if(!valiny_get_recu.get("recucode").equals(""))
                            {
                                Log.e("VERIFRECU", "AVEC");
                                Toast.makeText(myContext, "Code reçu ok", Toast.LENGTH_SHORT).show();
                                databaseManager.setCodeRecu(valiny_get_recu.get("recucode"), paiementFactureData.getIdInterneReel());
                                databaseManager.close();
                                databaseManager.setSuccessState(paiementFactureData.getIdInterneReel());
                                databaseManager.close();

                                Toast.makeText(getContext(), getContext().getString(R.string.reglement_ok), Toast.LENGTH_SHORT).show();
                                showProgressDialog(myContext.getString(R.string.enregistrement_title), myContext.getString(R.string.enregistrement_base));

                                //setCodeRecu(valinyRecuJirama.get("recucode"),idSimple);
                                PaiementFacture paiementFactureForMysql = new PaiementFacture(Double.valueOf(paiementFactureData.getFrais()),
                                        paiementFactureData.getLatitude(), paiementFactureData.getLongitude(),
                                        Double.valueOf(paiementFactureData.getMontantFacture()), paiementFactureData.getNumeroPayeur(),
                                        paiementFactureData.getNumeroCashpoint(), paiementFactureData.getOperateur(),
                                        paiementFactureData.getRefFacture(), paiementFactureData.getRefTransaction(),
                                        Double.valueOf(paiementFactureData.getTotale()), paiementFactureData.getDaty(),
                                        paiementFactureData.getRefClient(), paiementFactureData.getNomClient(),
                                        paiementFactureData.getAdresseClient(), paiementFactureData.getMois(),
                                        paiementFactureData.getAnnee());

                                paiementFactureForMysql.setCodeRecu(valiny_get_recu.get("recucode"));
                                paiementFactureForMysql.setAnterieur(paiementFactureData.getIdInterne());
                                paiementFactureForMysql.setEtat("SUCCESS");
                                paiementFactureForMysql.setRefTransaction(paiementFactureData.getRefTransaction());
                                paiementFactureForMysql.setCaissier(paiementFactureData.getCaissier());
                                paiementFactureForMysql.setIdDistributeur(paiementFactureData.getIdDistributeur());

                                insertPaiementFactureBase(paiementFactureForMysql, positionS);
                            }
                            else if(valiny_get_recu.get("recucode").equals(""))
                            {
                                Log.e("VERIFRECU", "SANS");
                                Toast.makeText(myContext, "Code reçu vide", Toast.LENGTH_SHORT).show();
                                synchroForJiramaOnly(paiementFactureData, positionS);
                            }
                        }
                        else
                        {
                            dismissProgressDialog();
                            Toast.makeText(myContext, myContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
                        }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("CODE_RECU", error.getMessage());
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

        stringRequestRecu.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.myRequestQueue.add(stringRequestRecu);
    }

    public void insertPaiementFactureBase(PaiementFacture paiementFacture, int positionS)
    {
        showProgressDialog(myContext.getString(R.string.enregistrement_title), myContext.getString(R.string.enregistrement_message));

        JSONObject bodyJson = null;
        try {
            bodyJson = new JSONObject(paiementFacture.jsonMe());
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
                        if(response.get("id")!=null)
                        {
                            removeFromList(positionS);
                            dismissProgressDialog();
                            Toast.makeText(getContext(), getContext().getString(R.string.enregistrement_ok), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        dismissProgressDialog();
                        e.printStackTrace();
                        if(e.getMessage()!=null)
                        {
                            Toast.makeText(myContext, "Json error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(myContext, "Json error: ", Toast.LENGTH_SHORT).show();
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

    /****** SYNCHRONISATION PAOSITRA ******/
    public void getToken(int operation, String codeDevalidation, String cin,PaiementFactureData paiementFactureData, int position){
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
                                //initiateCashout(bearerToken);
                            }
                            else if(operation==2)
                            {
                                validateCashout(bearerToken, codeDevalidation, cin, paiementFactureData,position);
                            }

                        }
                        else{
                                dismissProgressDialog();
                                Toast.makeText(myContext, "Paoma error token", Toast.LENGTH_SHORT).show();
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

    public void validateCashout(String bearerToken, String codeValidation, String cin, PaiementFactureData paiementFactureData, int position)
    {
        String url         = url_paositra + url_validation_cashout;
        String commentaire = "Paiement jirama EF";
        String bodyString = "{\n" +
                " \"telephone\" : \""+paiementFactureData.getNumeroPayeur()+"\",\n" +
                " \"montant\" : \""+paiementFactureData.getMontantFacture()+"\",\n" +
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
                            paiementFactureData.setRefTransaction(num_transaction);
                            databaseManager.setRefTransaction(num_transaction, paiementFactureData.getIdInterneReel());
                            databaseManager.setEtat("PAID", paiementFactureData.getIdInterneReel());
                            databaseManager.close();
                            Toast.makeText(myContext, myContext.getString(R.string.validation_cashout_ok)+": "+num_transaction, Toast.LENGTH_SHORT).show();


                            // CONSTRUCTION RECU POUR IMPRESSION

                            // CONTRUCTION ID INTERNE POUR ENVOIE JIRAMA:
                            /*String[] dateId = getDateId(paiementFactureData.getDaty());
                            String idInterneMaking = dateId[0] + dateId[1] + paiementFactureData.getIdInterneReel();

                            Object[] obj = new Object[4];
                            obj[0] = paiementFactureData.getRefFacture();
                            obj[1] = paiementFactureData.getNumeroPayeur() + "_Eqima";
                            obj[2] = paiementFactureData.getRefTransaction();
                            obj[3] = idInterneMaking + "_Paoma";
                            Log.e("EQ", String.valueOf(obj[3]));*/

                            Log.e("NUMTRANSACTION", num_transaction);
                            Log.e("NUMTRANSACTIONBASE", paiementFactureData.getRefTransaction());

                            decreaseMyCredit(num_transaction, paiementFactureData, position);


                        }
                        else
                        {
                            dismissProgressDialog();
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

    // GET MY CREDIT LEFT:
    public void getCreditLeft(String codeValidation, String cin, PaiementFactureData paiementFactureData, int position)
    {
        showProgressDialog(myContext.getString(R.string.validation_cashout_title), myContext.getString(R.string.patientez_message));

        String[] mySession = sessionManagement.getSession();
        String url = domain_name + url_reste_credit + mySession[2].replace("EQ-", "");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("resteCredit"))
                {
                    try {
                        Log.e("NBCREDIT", response.getString("resteCredit"));
                        double resteCreditDouble = response.getDouble("resteCredit");
                        Log.e("CREDIT:", seuille+" Montant:"+ paiementFactureData.getMontantFacture());
                        if(Double.valueOf(paiementFactureData.getMontantFacture())>seuille)
                        {
                            if(resteCreditDouble>=1)
                            {
                                getToken(2, codeValidation, cin, paiementFactureData, position);
                            }
                            else
                            {
                                dismissProgressDialog();
                                Toast.makeText(myContext, myContext.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                            }
                        }
                        else if(Double.valueOf(paiementFactureData.getMontantFacture())<=seuille)
                        {
                            if(resteCreditDouble>=0.5)
                            {
                                getToken(2, codeValidation, cin, paiementFactureData, position);
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
                dismissProgressDialog();
                if(error.getMessage()!=null)
                {
                    Log.e("ERROR", error.getMessage());
                    Toast.makeText(myContext, "ERROR: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Log.e("ERRORCREDIT", "ERROR");
                    Toast.makeText(myContext, "ERROR CREDIT", Toast.LENGTH_SHORT).show();
                }
            }
        });

        this.myRequestQueue.add(jsonObjectRequest);
    }

}