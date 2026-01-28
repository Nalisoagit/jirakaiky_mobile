package mg.eqima.jiramacashless;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.google.firebase.firestore.FirebaseFirestore;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mg.eqima.jiramacashless.apiairtel.ExecutionAsyncEnquiry;
import mg.eqima.jiramacashless.apimvola.ExecutionMvolaAsync;
import mg.eqima.jiramacashless.cashlesswebapi.AsynReglerFacture;
import mg.eqima.jiramacashless.cashlesswebapivolley.CashlessWebApiVolley;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.cashlesswebapi.AsyncInsertCashlessWebApi;
import mg.eqima.jiramacashless.internet.InternetConnection;
import mg.eqima.jiramacashless.localisation.GpsTracker;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.session.SessionManagement;
import mg.eqima.jiramacashless.utilitaire.PrintUtility;
import mg.eqima.jiramacashless.utilitaire.SharedData;
import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class Payement extends AppCompatActivity {

    // URL
    private Environnement environnement;
    private String domain_name;
    private String url_set_recu              = "/api/paiement_facture/setCodeRecu?coderecu=";
    private String url_success_state         = "/api/paiement_facture/setSuccessState?id=";

    EditText refFactGrisee;
    EditText montantFactGrisee;
    EditText numero;
    TextView payementTextView;
    TextView exempleNumeroTextView;
    Button payerButton;
    String operateur;
    String refFactureBundle;
    String montantFactureBundle;
    Context context ;

    // ACTIVITY:
    Activity me;

    //FIREBASE UTIL:
    FirebaseFirestore dbFirestore;

    //LES PROGRESS DIALOG:
    ProgressDialog progressDialog;
    ProgressBar progressPaiement;

    // Base PaiementFacture instance:
    PaiementFacture paiementFactureBase;
    String dateForFailed;

    // CASHLESSEPAPI POUR RUNNABLE:
    CashlessWebApiVolley cashlessWebApiVolley;

    // FRAIS:
   // String frais = "1000";
    String frais ;
    String etatInitie = "INITIE";
    private double seuille = 10000;


    /*** BLUETOOTH ILAINA com.github.DantSu:ESCPOS-ThermalPrinter ****/
    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    private final Locale locale = new Locale("id", "ID");
    private final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", locale);
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(locale);

    BluetoothConnection btConnection;

    /*** BLUETOOTH ILAINA com.github.DantSu:ESCPOS-ThermalPrinter FIN ****/

    //LOCALISATION:
    private GpsTracker gpsTracker;
    double latitude, longitude;
    //FIN LOCALISATION

    //BASE DE DONNEES SQLITE
    DatabaseManager sqliteDatabase;

    //RESAKA SESSION:
    String[] sessionUser;

    //RESAKA UTILITAIRE:
    Utilitaire utilitaire;

    // BUNDLE:
    Bundle bundle;

    // SYNC CREDIT:
    CashlessWebApiVolley myCashlessWebApiVolley;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_payement);
        String refFacture = "";
        String montantFacture = "";
        operateur = "";
        context=this ;

        frais= String.valueOf(SharedData.TRANSACTION_FEE_TOTAL) ;

        refFactGrisee = findViewById(R.id.refFactGrisee);
        montantFactGrisee = findViewById(R.id.montantFactGrisee);
        numero = findViewById(R.id.numero);
        payementTextView = findViewById(R.id.payementTextView);
        exempleNumeroTextView = findViewById(R.id.exempleNumeroTextView);
        payerButton = findViewById(R.id.payerButton);
        progressPaiement = findViewById(R.id.progressPaiement);

        //INSTANCIATION FIRESTORE:
        //dbFirestore = FirebaseFirestore.getInstance();

        environnement = new Environnement();
        domain_name = environnement.getDomainName();

        //INSTANCIATION SQLITE DATABASE:
        sqliteDatabase = new DatabaseManager(this);

        //OBTENTION DE LA SESSION UTILISATEUR:
        sessionUser = new SessionManagement(Payement.this).getSession();

        //INSTANCIATION UTILITAIRE:
        utilitaire = new Utilitaire();

        me = this;

        // LANCEMENT SYNCHRONISATION CREDIT LEFT:
        cashlessWebApiVolley = new CashlessWebApiVolley(Payement.this, me);
        //cashlessWebApiVolley.synchroCredit();
        cashlessWebApiVolley.startSync();

        getDateNowSimple();

        //OBTENTION DES VALEURS PASSEES:
        Intent intent = getIntent();
        if (intent != null) {
            bundle = intent.getExtras();
            refFactureBundle = bundle.getString("refFacture");
            montantFactureBundle = bundle.getString("montantFacture");
            operateur = bundle.getString("operateur");
            //myCashlessWebApiVolley = (CashlessWebApiVolley) intent.getSerializableExtra("SYNCH_CREDIT");

            String numero_client = bundle.getString("numero_client");
            String nom_client = bundle.getString("nom_client");
            String adresse_client = bundle.getString("adresse_client");
            String ref_client = bundle.getString("refClient");
            String mois_facture = bundle.getString("mois");
            String annee_facture = bundle.getString("annee");

            Log.e("Numero_client:", numero_client);
            Log.e("Nom_client:", nom_client);
            Log.e("Adresse_client:", adresse_client);
            Log.e("Réf_client:", ref_client);
            Log.e("Mois_de:", mois_facture);
            Log.e("Année: ", annee_facture);

            makeSpecificationOperateur();

            int montantTotal = Integer.valueOf(frais) + Integer.valueOf(montantFactureBundle);
            //MISE DES TEXTES GRISEE:
            refFactGrisee.setText(refFactureBundle);
            montantFactGrisee.setText(montantFactureBundle);
            paiementFactureBase = new PaiementFacture();
            // SET DES ELEMENTS DE BASE:
            paiementFactureBase.setMontantFacture(Double.valueOf(montantFactureBundle));
            paiementFactureBase.setRefFacture(refFactureBundle);
            paiementFactureBase.setNomClient(nom_client);
            paiementFactureBase.setOperateur(operateur);
            paiementFactureBase.setNumeroCashpoint(sessionUser[1]);
            paiementFactureBase.setAdresseClient(adresse_client);
            paiementFactureBase.setRefClient(ref_client);
            paiementFactureBase.setMois(mois_facture);
            paiementFactureBase.setAnnee(annee_facture);
            paiementFactureBase.setFrais(Double.valueOf(frais));
            paiementFactureBase.setTotal(montantTotal);
            paiementFactureBase.setCaissier(sessionUser[2]);
            paiementFactureBase.setEtat(etatInitie);
            paiementFactureBase.setIdDistributeur(sessionUser[5]);
        }

        // BLUETOOTH CONNEXION:
        //btConnection = BluetoothPrintersConnections.selectFirstPaired();

        /******************************** COMMENCEMENT DE PAIEMENT ******************************/
        payerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            /*if(btConnection!=null)
            {
                btConnection = BluetoothPrintersConnections.selectFirstPaired();
            }*/
                boolean btIsActived = isBluetoothAvailable();
                boolean locIsActived = isLocationIsActivated();
                //boolean isThereAPrinterConnected = isPrinterConnected();


                if (InternetConnection.checkConnection(Payement.this)) {
                    if (btIsActived && locIsActived) {
                        getLocation(view);
                        boolean verifNum = verificationNumero();
                        if (verifNum == true) {

                            payerButton.setEnabled(false);

                            if (operateur.equals("Airtel")) {
                                String numeroS = numero.getText().toString().trim();

                                double montantInitiale = Double.valueOf(montantFactureBundle);
                                double montantADeduire = montantInitiale + Double.valueOf(frais);
                                //double montantADeduire = montantInitiale;

                                SimpleDateFormat s = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                                Date date = new Date();
                                String daty = s.format(date);

                                // DATE FAILED:
                                dateForFailed = getDateNowSimple();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        /****** DEBUT RUN ******/
                                        Payement.this.runOnUiThread(new Runnable() {
                                            public void run() {
                                                //Do your UI operations like dialog opening or Toast here
                                                progressDialog = new ProgressDialog(Payement.this);
                                                progressDialog.setTitle(Payement.this.getString(R.string.transaction_title));
                                                progressDialog.setMessage(Payement.this.getString(R.string.patientez_message));
                                                progressDialog.setCanceledOnTouchOutside(false);
                                                progressDialog.show();
                                            }
                                        });

                                        String airtelMoneyId = "";
                                        Object[] informationRequestForPayement = new Object[2];
                                        informationRequestForPayement[0] = montantADeduire;
                                        informationRequestForPayement[1] = numeroS;
                                        ExecutionAsyncEnquiry executionAsyncEnquiry = new ExecutionAsyncEnquiry();

                                        //INITIALISATION PAIEMENT:
                                        //CollectionApi collectionApi = new CollectionApi();

//                                        try {
//                                            //Map<String, String> payement = collectionApi.makeReclamation(montantADeduire, numeroS);
//                                            Map<String, String> payement = (Map<String, String>) executionAsyncEnquiry.execute(informationRequestForPayement).get();
//                                            if (payement.get("okEcritureFirebase").equals("true")) {
//                                                if (payement.containsKey("airtelMoneyId")) {
//                                                    Payement.this.runOnUiThread(new Runnable() {
//                                                        public void run() {
//                                                            //Do your UI operations like dialog opening or Toast here
//                                                            Toast.makeText(Payement.this, Payement.this.getString(R.string.transaction_reussi_message), Toast.LENGTH_SHORT).show();
//                                                        }
//                                                    });
//
//                                                    Payement.this.runOnUiThread(new Runnable() {
//                                                        public void run() {
//                                                            //Do your UI operations like dialog opening or Toast here
//                                                            Toast.makeText(Payement.this, Payement.this.getString(R.string.enregistrement_message), Toast.LENGTH_SHORT).show();
//                                                            progressDialog = new ProgressDialog(Payement.this);
//                                                            progressDialog.setTitle(Payement.this.getString(R.string.enregistrement_title));
//                                                            progressDialog.setMessage(Payement.this.getString(R.string.enregistrement_message));
//                                                            progressDialog.setCanceledOnTouchOutside(false);
//                                                            progressDialog.show();
//                                                        }
//                                                    });
//
//                                                    airtelMoneyId = payement.get("airtelMoneyId");
//                                                    //CONSTRUCTION RECU:
//                                                    Recu recu = new Recu(utilitaire.changeDateFormat(daty), refFactureBundle, montantFactureBundle, numeroS, sessionUser[1], frais, String.valueOf(montantADeduire), operateur, airtelMoneyId, bundle.getString("refClient"), bundle.getString("nom_client"), bundle.getString("adresse_client"), bundle.getString("mois"), bundle.getString("annee"), sessionUser[2]);
//                                                    //NOTE:
//                                                    // sessionUser[1] => numeroCashpoint
//                                                    //ENVOI VERS FIREBASE:
//                                                    // Create a new user with a first and last name
//                                                    String datySimple = getDateNowSimple();
//                                                    Map<String, Object> recuMap = new HashMap<>();
//                                                    recuMap.put("daty", recu.getDaty());
//                                                    recuMap.put("refFacture", recu.getRefFacture());
//                                                    recuMap.put("montantFacture", recu.getMontantFacture());
//                                                    recuMap.put("numeroPayeur", recu.getNumero());
//                                                    recuMap.put("frais", recu.getFrais());
//                                                    recuMap.put("operateur", recu.getOperateur());
//                                                    recuMap.put("refTransaction", recu.getRefTransaction());
//                                                    recuMap.put("latitude", latitude);
//                                                    recuMap.put("longitude", longitude);
//                                                    Log.d("RECU", "recu: "+recu.contruireMonRecu());
//                                                    PaiementFacture paiementFactureForMysql = new PaiementFacture(Double.valueOf(recu.getFrais()),
//                                                            String.valueOf(latitude), String.valueOf(longitude),
//                                                            Double.valueOf(recu.getMontantFacture()), recu.getNumero(),
//                                                            recu.getNumeroCashpoint(), recu.getOperateur(),
//                                                            recu.getRefFacture(), recu.getRefTransaction(),
//                                                            Double.valueOf(recu.getMontantTotale()), utilitaire.makeDateToMatchDateTimeFormat(recu.getDaty()), bundle.getString("refClient"), bundle.getString("nom_client"), bundle.getString("adresse_client"), bundle.getString("mois"), bundle.getString("annee"));
//                                                    paiementFactureForMysql.setCaissier(paiementFactureBase.getCaissier());
//
//                                                    System.out.println("JJJJJJ: " + paiementFactureForMysql.getRefTransaction());
//
//
//                                                    //ENREGISTREMENT DANS BASE SQLITE:
//                                                    sqliteDatabase.insertIntoDatabase(recu, datySimple, String.valueOf(latitude), String.valueOf(longitude));
//                                                    sqliteDatabase.close();
//                                                    Log.d("sqlLiteAirtel", "run: ");
//
//                                                    //ENREGISTREMENT DANS MYSQL:
//                                                    Object[] objectsPaiementFacture = new Object[1];
//                                                    objectsPaiementFacture[0] = paiementFactureForMysql;
//                                                    AsyncInsertCashlessWebApi asyncInsertCashlessWebApi = new AsyncInsertCashlessWebApi();
//                                                    String insertionPaiementString = asyncInsertCashlessWebApi.execute(objectsPaiementFacture).get();
//                                                    JSONObject insertionPaiementJson = new JSONObject(insertionPaiementString);
//
//
//                                                    if (!insertionPaiementJson.get("id").equals(null)) {
//
//                                                        sqliteDatabase.deMarkForSynchronization(recu.getRefFacture(), recu.getDaty());
//                                                        sqliteDatabase.close();
//
//                                                        String[] dateId = getDateId(dateForFailed);
//                                                        String idInterneMaking = dateId[0] + dateId[1] + insertionPaiementJson.get("id");
//                                                        recu.setIdInterne(idInterneMaking);
//                                                        sqliteDatabase.setIdInterne(recu.getRefFacture(), recu.getIdInterne(), recu.getDaty());
//                                                        sqliteDatabase.close();
//
//                                                        AsynReglerFacture asyncReglerFacture = new AsynReglerFacture();
//                                                        Object[] obj = new Object[4];
//                                                        obj[0] = recu.getRefFacture();
//                                                        obj[1] = recu.getNumeroCashpoint() + "_Eqima";
//                                                        obj[2] = recu.getRefTransaction();
//                                                        obj[3] = idInterneMaking + "_Airtel";
//                                                        Log.e("EQ", String.valueOf(obj[3]));
//                                                        Map<String, String> valinyRecuJirama = asyncReglerFacture.execute(obj).get();
//
//                                                        recu.setNumeroRecuJirama(valinyRecuJirama.get("recucode"));
//                                                        /* AMBOARINA CODE RECU SET SQLITE */
//
//                                                        //sqliteDatabase.setCodeRecu(recu.getNumeroRecuJirama(), recu.getIdInterne());
//                                                        //sqliteDatabase.close();
//
//
//                                                        if (!valinyRecuJirama.get("recucode").equals("") && valinyRecuJirama.get("recucode") != null) {
//                                                            setCodeRecu(recu.getNumeroRecuJirama(), insertionPaiementJson.getString("id"));
//                                                            setSuccessState(insertionPaiementJson.getString("id"));
//
//                                                            /* AMBOARINA SET SUCCESS STATE */
//                                                            //sqliteDatabase.setSuccessState(recu.getRefFacture(), recu.getDaty());
//                                                            //sqliteDatabase.close();
//                                                            //recu.setEtat("Success");
//                                                        }
//
//                                                        Payement.this.runOnUiThread(new Runnable() {
//                                                            public void run() {
//                                                                //Do your UI operations like dialog opening or Toast here
//                                                                //ARRET PROGRESS DIALOGUE ENREGISTREMENT
//                                                                progressDialog.dismiss();
//
//                                                                Toast.makeText(Payement.this, Payement.this.getString(R.string.impression_message), Toast.LENGTH_SHORT).show();
//
//                                                            }
//                                                        });
//
//                                                        //DEBUT IMPRESSION
//                                                       PrintUtility.doPrintPayement(view, recu,Payement.this,context);
//
//                                                        Payement.this.runOnUiThread(new Runnable() {
//                                                            public void run() {
//                                                                //Do your UI operations like dialog opening or Toast here
//                                                                //ARRET PROGRESS DIALOGUE IMPRESSION
//
//                                                                Toast.makeText(Payement.this, Payement.this.getString(R.string.merci_message), Toast.LENGTH_SHORT).show();
//                                                            }
//                                                        });
//
//                                                        Intent intent1 = new Intent(Payement.this, MainActivity.class);
//                                                        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                                        startActivity(intent1);
//
//                                                    } else {
//                                                        Payement.this.runOnUiThread(new Runnable() {
//                                                            public void run() {
//                                                                //Do your UI operations like dialog opening or Toast here
//                                                                progressDialog.dismiss();
//                                                                Toast.makeText(Payement.this, Payement.this.getString(R.string.error_firebase_message), Toast.LENGTH_SHORT).show();
//                                                            }
//                                                        });
//                                                    }
//
//                                                }
//                                            } else {
//
//                                                Object[] basePaiement = new Object[1];
//                                                paiementFactureBase.setEtat("Canceled");
//                                                paiementFactureBase.setNumeroPayeur(numero.getText().toString().trim());
//                                                paiementFactureBase.setLatitude(String.valueOf(latitude));
//                                                paiementFactureBase.setLongitude(String.valueOf(longitude));
//                                                paiementFactureBase.setDatePaiement(dateForFailed);
//                                                basePaiement[0] = paiementFactureBase;
//                                                AsyncInsertCashlessWebApi asyncInsertCashlessWebApi = new AsyncInsertCashlessWebApi();
//                                                String insertionFailed = asyncInsertCashlessWebApi.execute(basePaiement).get();
//                                                JSONObject failedJson = new JSONObject(insertionFailed);
//
//                                                Log.e("TRANSACTION", "Failed pour:" + failedJson.get("id"));
//
//                                                Payement.this.runOnUiThread(new Runnable() {
//                                                    public void run() {
//                                                        //Do your UI operations like dialog opening or Toast here
//                                                        progressDialog.dismiss();
//                                                        Toast.makeText(Payement.this, Payement.this.getString(R.string.transaction_echouer_message), Toast.LENGTH_LONG).show();
//                                                    }
//                                                });
//
//                                            }
//
//                                        } catch (Exception e) {
//                                            Log.d("Tag", e.getMessage());
//                                        }
                                        try {
                                            Map<String, String> payement = executionAsyncEnquiry.execute(informationRequestForPayement).get();
                                            Log.d("PAYEMENT_DEBUG", "=== RÉPONSE PAYEMENT REÇUE ===");
                                            Log.d("PAYEMENT_DEBUG", "Payement: " + payement.toString());

                                            if (payement.get("okEcritureFirebase").equals("true")) {
                                                Log.d("PAYEMENT_DEBUG", "Firebase OK");

                                                if (payement.containsKey("airtelMoneyId")) {
                                                    Log.d("PAYEMENT_DEBUG", "airtelMoneyId présent: " + payement.get("airtelMoneyId"));

                                                    // Toast transaction réussie
                                                    Payement.this.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(Payement.this, Payement.this.getString(R.string.transaction_reussi_message), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                    // ProgressDialog Enregistrement
                                                    Payement.this.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            if (progressDialog != null && progressDialog.isShowing()) {
                                                                progressDialog.dismiss();
                                                            }
                                                            progressDialog = new ProgressDialog(Payement.this);
                                                            progressDialog.setTitle(Payement.this.getString(R.string.enregistrement_title));
                                                            progressDialog.setMessage(Payement.this.getString(R.string.enregistrement_message));
                                                            progressDialog.setCanceledOnTouchOutside(false);
                                                            progressDialog.show();
                                                        }
                                                    });

                                                    airtelMoneyId = payement.get("airtelMoneyId");

                                                    // Construction du reçu
                                                    Recu recu = new Recu(utilitaire.changeDateFormat(daty), refFactureBundle, montantFactureBundle, numeroS, sessionUser[1], frais, String.valueOf(montantADeduire), operateur, airtelMoneyId, bundle.getString("refClient"), bundle.getString("nom_client"), bundle.getString("adresse_client"), bundle.getString("mois"), bundle.getString("annee"), sessionUser[2]);
                                                    Log.d("PAYEMENT_DEBUG", "Reçu construit");

                                                    String datySimple = getDateNowSimple();

                                                    // Préparer les données
                                                    PaiementFacture paiementFactureForMysql = new PaiementFacture(
                                                            Double.valueOf(recu.getFrais()),
                                                            String.valueOf(latitude),
                                                            String.valueOf(longitude),
                                                            Double.valueOf(recu.getMontantFacture()),
                                                            recu.getNumero(),
                                                            recu.getNumeroCashpoint(),
                                                            recu.getOperateur(),
                                                            recu.getRefFacture(),
                                                            recu.getRefTransaction(),
                                                            Double.valueOf(recu.getMontantTotale()),
                                                            utilitaire.makeDateToMatchDateTimeFormat(recu.getDaty()),
                                                            bundle.getString("refClient"),
                                                            bundle.getString("nom_client"),
                                                            bundle.getString("adresse_client"),
                                                            bundle.getString("mois"),
                                                            bundle.getString("annee")
                                                    );
                                                    paiementFactureForMysql.setCaissier(paiementFactureBase.getCaissier());

                                                    // Enregistrement SQLite
                                                    Log.d("PAYEMENT_DEBUG", "=== DÉBUT ENREGISTREMENT SQLITE ===");
                                                    sqliteDatabase.insertIntoDatabase(recu, datySimple, String.valueOf(latitude), String.valueOf(longitude));
                                                    sqliteDatabase.close();
                                                    Log.d("PAYEMENT_DEBUG", "SQLite OK");

                                                    // Enregistrement MySQL
                                                    Log.d("PAYEMENT_DEBUG", "=== DÉBUT ENREGISTREMENT MYSQL ===");
                                                    Object[] objectsPaiementFacture = new Object[1];
                                                    objectsPaiementFacture[0] = paiementFactureForMysql;
                                                    AsyncInsertCashlessWebApi asyncInsertCashlessWebApi = new AsyncInsertCashlessWebApi();

                                                    try {
                                                        String insertionPaiementString = asyncInsertCashlessWebApi.execute(objectsPaiementFacture).get(30, TimeUnit.SECONDS); // ⚠️ TIMEOUT DE 30 secondes
                                                        Log.d("PAYEMENT_DEBUG", "Réponse MySQL: " + insertionPaiementString);

                                                        JSONObject insertionPaiementJson = new JSONObject(insertionPaiementString);
                                                        Log.d("PAYEMENT_DEBUG", "ID MySQL: " + insertionPaiementJson.get("id"));

                                                        if (!insertionPaiementJson.get("id").equals(null)) {
                                                            Log.d("PAYEMENT_DEBUG", "ID valide, suite du traitement...");

                                                            sqliteDatabase.deMarkForSynchronization(recu.getRefFacture(), recu.getDaty());
                                                            sqliteDatabase.close();

                                                            String[] dateId = getDateId(dateForFailed);
                                                            String idInterneMaking = dateId[0] + dateId[1] + insertionPaiementJson.get("id");
                                                            recu.setIdInterne(idInterneMaking);
                                                            sqliteDatabase.setIdInterne(recu.getRefFacture(), recu.getIdInterne(), recu.getDaty());
                                                            sqliteDatabase.close();

                                                            Log.d("PAYEMENT_DEBUG", "=== DÉBUT RÈGLEMENT FACTURE ===");
                                                            AsynReglerFacture asyncReglerFacture = new AsynReglerFacture();
                                                            Object[] obj = new Object[4];
                                                            obj[0] = recu.getRefFacture();
                                                            obj[1] = recu.getNumeroCashpoint() + "_Eqima";
                                                            obj[2] = recu.getRefTransaction();
                                                            obj[3] = idInterneMaking + "_Airtel";

                                                            Map<String, String> valinyRecuJirama = asyncReglerFacture.execute(obj).get(30, TimeUnit.SECONDS); // ⚠️ TIMEOUT
                                                            Log.d("PAYEMENT_DEBUG", "Réponse ReglerFacture: " + valinyRecuJirama.toString());

                                                            recu.setNumeroRecuJirama(valinyRecuJirama.get("recucode"));

                                                            if (!valinyRecuJirama.get("recucode").equals("") && valinyRecuJirama.get("recucode") != null) {
                                                                Log.d("PAYEMENT_DEBUG", "RecuCode valide: " + valinyRecuJirama.get("recucode"));
                                                                setCodeRecu(recu.getNumeroRecuJirama(), insertionPaiementJson.getString("id"));
                                                                setSuccessState(insertionPaiementJson.getString("id"));
                                                            } else {
                                                                Log.e("PAYEMENT_DEBUG", "⚠️ RecuCode vide ou null !");
                                                            }

                                                            Log.d("PAYEMENT_DEBUG", "=== FIN TRAITEMENTS - DISMISS DIALOG ===");

                                                            // Fermer le progressDialog
                                                            Payement.this.runOnUiThread(new Runnable() {
                                                                public void run() {
                                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                                        progressDialog.dismiss();
                                                                    }
                                                                    Toast.makeText(Payement.this, Payement.this.getString(R.string.impression_message), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });

                                                            // Impression
                                                            PrintUtility.doPrintPayement(view, recu, Payement.this, context);

                                                            Payement.this.runOnUiThread(new Runnable() {
                                                                public void run() {
                                                                    Toast.makeText(Payement.this, Payement.this.getString(R.string.merci_message), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });

                                                            Intent intent1 = new Intent(Payement.this, MainActivity.class);
                                                            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(intent1);

                                                        } else {
                                                            Log.e("PAYEMENT_DEBUG", "⚠️ ID MySQL est null !");
                                                            throw new Exception("ID MySQL null");
                                                        }

                                                    } catch (TimeoutException e) {
                                                        Log.e("PAYEMENT_DEBUG", "⚠️⚠️⚠️ TIMEOUT API - " + e.getMessage());
                                                        throw new Exception("Timeout lors de l'enregistrement");
                                                    }
                                                }
                                            } else {
                                                Log.e("PAYEMENT_DEBUG", "❌ okEcritureFirebase = false");
                                                // Gestion de l'échec
                                            }

                                        } catch (Exception e) {
                                            Log.e("PAYEMENT_DEBUG", "❌❌❌ EXCEPTION GLOBALE ❌❌❌");
                                            Log.e("PAYEMENT_DEBUG", "Type: " + e.getClass().getName());
                                            Log.e("PAYEMENT_DEBUG", "Message: " + (e.getMessage() != null ? e.getMessage() : "null"));
                                            e.printStackTrace();

                                            // ⚠️ TOUJOURS FERMER LE DIALOG EN CAS D'ERREUR
                                            Payement.this.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    if (progressDialog != null && progressDialog.isShowing()) {
                                                        progressDialog.dismiss();
                                                    }
                                                    Toast.makeText(Payement.this, "Erreur: " + (e.getMessage() != null ? e.getMessage() : "Inconnue"), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                }).start();

                            }
                            else if (operateur.equals("Mvola")) {
                                Toast.makeText(Payement.this, Payement.this.getString(R.string.commencement), Toast.LENGTH_SHORT).show();
                                //myCashlessWebApiVolley.pauseSynchroCredit();
                                Log.e("Tag", "Mvola choisi");
                                String numeroS = numero.getText().toString().trim();

                                cashlessWebApiVolley.stopSynchroCredit();

                                int montantInitiale = Integer.valueOf(montantFactureBundle);
                                int montantADeduire = montantInitiale;
                                int fraisInt = Integer.valueOf(frais);
                                double totalBase = Double.valueOf(montantADeduire) + Double.valueOf(fraisInt);

                                // DATE FAILED:
                                dateForFailed = getDateNowSimple();
                                paiementFactureBase.setDatePaiement(dateForFailed);
                                paiementFactureBase.setLatitude(String.valueOf(latitude));
                                paiementFactureBase.setLongitude(String.valueOf(longitude));
                                paiementFactureBase.setNumeroPayeur(numeroS);
                                paiementFactureBase.setTotal(totalBase);

                                double myCredit = Double.valueOf(sessionUser[3]);
                                if(paiementFactureBase.getMontantFacture()>seuille)
                                {
                                    if(myCredit>=1)
                                    {
                                        CashlessWebApiVolley cashlessWebApiVolley = new CashlessWebApiVolley(Payement.this, paiementFactureBase,
                                                sqliteDatabase, view, Payement.this);

                                        cashlessWebApiVolley.startOperation();
                                    }
                                    else
                                    {
                                        Toast.makeText(Payement.this, Payement.this.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else if(paiementFactureBase.getMontantFacture()<=seuille)
                                {
                                    if(myCredit>=0.5)
                                    {
                                        CashlessWebApiVolley cashlessWebApiVolley = new CashlessWebApiVolley(Payement.this, paiementFactureBase,
                                                sqliteDatabase, view, Payement.this);

                                        cashlessWebApiVolley.startOperation();
                                    }
                                    else
                                    {
                                        Toast.makeText(Payement.this, Payement.this.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                    }
                                }


                            }
                            else if (operateur.equals("Orange")) {
                                Log.e("Tag", "Orange pas encore pris en charge");
                                progressPaiement.setVisibility(View.GONE);
                            }
                            else if (operateur.equals("Paoma")) {
                                Toast.makeText(Payement.this, Payement.this.getString(R.string.commencement), Toast.LENGTH_SHORT).show();
                                //myCashlessWebApiVolley.pauseSynchroCredit();
                                Log.e("Tag", "Paoma choisi");
                                String numeroS = numero.getText().toString().trim();

                                cashlessWebApiVolley.stopSynchroCredit();

                                int montantInitiale = Integer.valueOf(montantFactureBundle);
                                int montantADeduire = montantInitiale;
                                int fraisInt = Integer.valueOf(frais);
                                double totalBase = Double.valueOf(montantADeduire) + Double.valueOf(fraisInt);

                                // DATE FAILED:
                                dateForFailed = getDateNowSimple();
                                paiementFactureBase.setDatePaiement(dateForFailed);
                                paiementFactureBase.setLatitude(String.valueOf(latitude));
                                paiementFactureBase.setLongitude(String.valueOf(longitude));
                                paiementFactureBase.setNumeroPayeur(numeroS);
                                paiementFactureBase.setTotal(totalBase);

                                double myCredit = Double.valueOf(sessionUser[3]);
                                //showValidationCashout();
                                if(paiementFactureBase.getMontantFacture()>seuille)
                                {
                                    if(myCredit>=1)
                                    {
                                        LayoutInflater layoutInflater = getLayoutInflater();
                                        CashlessWebApiVolley cashlessWebApiVolley = new CashlessWebApiVolley(Payement.this, paiementFactureBase,
                                                sqliteDatabase, view, Payement.this, layoutInflater);

                                        cashlessWebApiVolley.startOperation();
                                    }
                                    else
                                    {
                                        Toast.makeText(Payement.this, Payement.this.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else if(paiementFactureBase.getMontantFacture()<=seuille)
                                {
                                    if(myCredit>=0.5)
                                    {
                                        LayoutInflater layoutInflater = getLayoutInflater();
                                        CashlessWebApiVolley cashlessWebApiVolley = new CashlessWebApiVolley(Payement.this, paiementFactureBase,
                                                sqliteDatabase, view, Payement.this, layoutInflater);

                                        cashlessWebApiVolley.startOperation();
                                    }
                                    else
                                    {
                                        Toast.makeText(Payement.this, Payement.this.getString(R.string.credit_insuffisant), Toast.LENGTH_SHORT).show();
                                    }
                                }

                            }
                        } else {
                            Log.e("Tag", "Numéro ou opérateur incorrect !!!");
                            progressPaiement.setVisibility(View.GONE);
                        }
                    }
                } else {
                        Toast.makeText(Payement.this, Payement.this.getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                        activateInternetConnectionDialog();
                        Log.e("CONNECTION", "Hors ligne");
                }
            }
        });
    }

    public String getDateNowSimple() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());

        String valiny = formatter.format(date);
        Log.e("DATE", valiny);
        getDateId(valiny);
        return valiny;
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


    @SuppressLint("ResourceAsColor")
    public void makeSpecificationOperateur() {
        if (operateur.equals("Mvola")) {
            //payementTextView.setTextColor(this.getResources().getColor(R.color.telmaColor));
            exempleNumeroTextView.setText(Payement.this.getString(R.string.exemple_label) + " => 0340000000 " + Payement.this.getString(R.string.ou_label) + " 0380000000");
            payerButton.setBackgroundColor(this.getResources().getColor(R.color.telmaColor));
        }
        if (operateur.equals("Airtel")) {
            // payementTextView.setTextColor(this.getResources().getColor(R.color.airtelColor));
            exempleNumeroTextView.setText(Payement.this.getString(R.string.exemple_label) + " => 0330000000");
            payerButton.setBackgroundColor(this.getResources().getColor(R.color.airtelColor));
        }
        if (operateur.equals("Paoma")) {
            // payementTextView.setTextColor(this.getResources().getColor(R.color.orangeColor));
            exempleNumeroTextView.setText(Payement.this.getString(R.string.exemple_label) + " => 002613*0000000");
            payerButton.setBackgroundColor(this.getResources().getColor(R.color.paomaColor));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("PAUSE", "PAUSE");
        cashlessWebApiVolley.pauseSynchroCredit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("RESUME", "RESUME");
        cashlessWebApiVolley.resumeSynchroCredit();
    }

    public boolean verificationNumero() {
        boolean valiny = false;
        String numeroS = numero.getText().toString().trim();
        int longueurNumero = numeroS.length();

        if(!operateur.equals("Paoma"))
        {
            if (longueurNumero == 10) {
                if (operateur.equals("Mvola")) {
                    if (numeroS.startsWith("034") || numeroS.startsWith("038")) {
                        valiny = true;
                        Toast.makeText(this, "Ok num telma", Toast.LENGTH_SHORT);
                    } else {
                        numero.setError(Payement.this.getString(R.string.error_champ_numero) + " 034 ou 038");
                    }
                }
                if (operateur.equals("Airtel")) {
                    if (numeroS.startsWith("033")) {
                        valiny = true;
                        Toast.makeText(this, "Ok num airtel", Toast.LENGTH_SHORT);
                    } else {
                        numero.setError(Payement.this.getString(R.string.error_champ_numero) + " 033");
                    }
                }
                if (operateur.equals("Orange")) {
                    if (numeroS.startsWith("032")) {
                        valiny = true;
                        Toast.makeText(this, "Ok num orange", Toast.LENGTH_SHORT);
                    } else {
                        numero.setError(Payement.this.getString(R.string.error_champ_numero) + " 032");
                    }
                }
            }
            else if (longueurNumero != 10) {
                numero.setError(Payement.this.getString(R.string.error_champ_numero_longueur));
            }
        }
        else if(operateur.equals("Paoma"))
        {
            if (longueurNumero == 14) {
                if (numeroS.startsWith("0026134") || numeroS.startsWith("0026132") || numeroS.startsWith("0026133")) {
                    valiny = true;
                    Toast.makeText(this, "Ok num paoma", Toast.LENGTH_SHORT);
                } else {
                    numero.setError(Payement.this.getString(R.string.error_champ_numero) + " 002613*");
                }
            }
            else if (longueurNumero != 14) {
                numero.setError(Payement.this.getString(R.string.error_champ_numero_longueur));
            }
        }

        return valiny;

    }


    /******* LOCATION *******/
    public void getLocation(View view) {
        gpsTracker = new GpsTracker(Payement.this);
        if (gpsTracker.canGetLocation()) {
            this.latitude = gpsTracker.getLatitude();
            this.longitude = gpsTracker.getLongitude();
            Log.e("GPS", "Latitude: " + latitude + "-Longitude: " + longitude);
        } else {
            gpsTracker.showSettingsAlert();
        }
    }

    /******** BLUETOOTH VERSION FARANY MODIFICATION: Dantsu:ESCPOS-ThermalPrinter ******************/


    /*********** FIN BLUETOOTH **************/

    /**** BASE DE DONNEES SQLITE **************/
    public void insertIntoDbFunction(DatabaseManager dbSQLite, Recu recu, String daty, String latitude, String longitude) {
        dbSQLite.insertIntoDatabase(recu, daty, latitude, longitude);
    }

    /**** FIN BASE DE DONNEES SQLITE **************/

    public boolean isPrinterConnected() {
        boolean valiny = false;
        if (btConnection != null) {
            valiny = true;
        }
        if (btConnection == null) {
            AlertDialog.Builder alertDialogBT = new AlertDialog.Builder(Payement.this);
            alertDialogBT.setTitle("Imprimante BT");
            alertDialogBT.setIcon(R.drawable.printer);
            alertDialogBT.setMessage(Payement.this.getString(R.string.no_connected_imprimante));
            alertDialogBT.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            alertDialogBT.show();
        }
        return valiny;
    }

    // FONCTION ACTIVATION BLUETOOTH:
    public boolean isBluetoothAvailable() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean valiny = false;
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            valiny = true;
        } else {
            AlertDialog.Builder alertDialogBT = new AlertDialog.Builder(Payement.this);
            alertDialogBT.setTitle("Bluetooth");
            alertDialogBT.setIcon(R.drawable.bluetooth);
            alertDialogBT.setMessage(Payement.this.getString(R.string.bt_ask_enabling));
            alertDialogBT.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            alertDialogBT.show();
        }

        return valiny;
    }

    public boolean isLocationIsActivated() {
        boolean valiny = false;
        GpsTracker gpsTracker = new GpsTracker(Payement.this);
        if (gpsTracker.canGetLocation()) {
            valiny = true;
        } else {
            AlertDialog.Builder alertDialogBT = new AlertDialog.Builder(Payement.this);
            alertDialogBT.setTitle("Localisation");
            alertDialogBT.setIcon(R.drawable.location);
            alertDialogBT.setMessage(Payement.this.getString(R.string.loc_ask_enabling));
            alertDialogBT.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            alertDialogBT.show();
        }

        return valiny;
    }

    public void activateInternetConnectionDialog() {
        AlertDialog.Builder alertDialogBT = new AlertDialog.Builder(Payement.this);
        alertDialogBT.setTitle("Internet");
        alertDialogBT.setIcon(R.drawable.no_internet);
        alertDialogBT.setMessage(Payement.this.getString(R.string.internet_ask_enabling));
        alertDialogBT.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialogBT.show();
    }

    public void setCodeRecu(String coderecu, String id) {
        String url = domain_name + url_set_recu + coderecu + "&id=" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(Payement.this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RECU", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(Payement.this, Payement.this.getString(R.string.error) + error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: " + error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);

    }

    public void setSuccessState(String id) {
        String url = domain_name + url_success_state + id;
        RequestQueue requestQueue = Volley.newRequestQueue(Payement.this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("STATE", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(Payement.this, Payement.this.getString(R.string.error) + "Etat success\n" + error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: " + error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);

    }

    public Map<String, String> paiementFrais(Object[] o) {
        ExecutionMvolaAsync mvolaAsync = new ExecutionMvolaAsync();
        Map<String, String> statusOfTheTransaction = new HashMap<>();
        try {
            statusOfTheTransaction = mvolaAsync.execute(o).get();

            System.out.println("HHHHHHHHHHH" + statusOfTheTransaction.toString());
            Log.e("STATUS_FINAL: ", statusOfTheTransaction.get("status"));

            //Log.e("OBJECT_REFERENCE: ", statusOfTheTransaction.get("objectReference"));
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /**** OPERATION DE MISE DANS OBJET ET ENVOIE FIREBASE *****/
        return statusOfTheTransaction;
    }





}
