package mg.eqima.jiramacashless;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mg.eqima.jiramacashless.apimvolavolley.MerchantApiVolley;
import mg.eqima.jiramacashless.cashlesswebapi.AsyncApiJirama;
import mg.eqima.jiramacashless.cashlesswebapi.JiramaRestApi;
import mg.eqima.jiramacashless.cashlesswebapivolley.CashlessWebApiVolley;
import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.internet.InternetConnection;
import mg.eqima.jiramacashless.localisation.GpsTracker;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.model.PaiementFactureView;
import mg.eqima.jiramacashless.model.operateur.TarificationOperateur;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.scanner.CaptureAct;
import mg.eqima.jiramacashless.session.SessionManagement;
import mg.eqima.jiramacashless.utilitaire.AppUtils;
import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class MainActivity extends AppCompatActivity {

    private static final String factureTag = "FACTURE";
    private static final boolean TODO = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private EditText refFacture;
    private EditText montantFacture;
    private Button scanButton;
    Button saisieButton;

    //private ProgressBar progressBar;
    //TEST CASHLESS WEB API:
    Button buttonPurchase;
    Button buttonUser;

    //LOCALISATION:
    private GpsTracker gpsTracker;

    //LANGUAGE:
    private Spinner spinnerLangue;
    private static final String[] codeLanguages = {"", "MG", "FR", "Déconnexion"};

    //COMPTEUR:
    private int compteur = 0;

    //BOUTON CARDVIEW:
    private CardView cardViewTelma;
    private CardView cardViewAirtel;
    private CardView cardViewPaositra;

    //PROGRESS DIALOG:
    ProgressDialog progressDialogSynchronizationTarif;
    ProgressDialog progressDialogForAnalyseFacture;

    //DATABASE:
    private DatabaseManager databaseManagerSqlite;
    private FirebaseFirestore firestoreDB;

    //TAG:
    String TARIFICATIONTAG = "TARIFICATION";

    // UTILITAIRE VOLLEY:
    RequestQueue myRequestQueue;
    StringRequest stringRequest;

    // ACTIVITY:
    Activity me;
    SessionManagement mySessionManagement;
    String[] mySession;

    // LOCATION:
    double latitude;
    double longitude;


    /*** BLUETOOTH ILAINA com.github.DantSu:ESCPOS-ThermalPrinter ****/
    public static final int PERMISSION_BLUETOOTH = 1;
    private static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    private static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    private static final int PERMISSION_BLUETOOTH_SCAN = 4;

    private final Locale locale = new Locale("id", "ID");
    private final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", locale);
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(locale);

    BluetoothConnection btConnection;

    /*** BLUETOOTH ILAINA com.github.DantSu:ESCPOS-ThermalPrinter FIN ****/

    // URL API:
    private Environnement environnement;
    private String domain_name;
    private String url_existence_facture = "/jiramacontroller/verifExistenceFacture?referencefacture=";
    private String url_statut_facture    = "/jiramacontroller/statutFacture?referencefacture=";
    private String url_montant           = "/jiramacontroller/montantFacture2?referencefacture=";
    private String url_info_client       = "/jiramacontroller/infoClient?referencefacture=";
    private String url_info_client2      = "/jiramacontroller/infoClient2?referencefacture=";
    private String url_info_paiement     = "/api/paiement_facture/getPaiementFacture?referencefacture=";

    // PaiementFacture pour facture déjà payer:
    Recu recuFactureDejaPayer;


    // Bundle for information:
    Bundle bundleForInformation;
    Bundle bundleForScan;

    // CASHLESSEPAPI POUR RUNNABLE:
    CashlessWebApiVolley cashlessWebApiVolley;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        me = this;
        mySessionManagement = new SessionManagement(MainActivity.this);
        mySession           = mySessionManagement.getSession();

        myRequestQueue = Volley.newRequestQueue(MainActivity.this);

        environnement = new Environnement();
        domain_name = environnement.getDomainName();

        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.bienvenueToast), Toast.LENGTH_SHORT).show();
        //INITIALISATION DATABASE:
        //firestoreDB = FirebaseFirestore.getInstance();
        databaseManagerSqlite = new DatabaseManager(this);

        //INITIALISATION BUTTON TEST CASHLESS WEB API:
        buttonPurchase = findViewById(R.id.buttonPurchase);
        buttonUser = findViewById(R.id.buttonUser);
        saisieButton = findViewById(R.id.saisieButton);

        //INITIALISATION DES TEXTVIEW FACTURE:
        this.refFacture = findViewById(R.id.refFacture);
        this.montantFacture = findViewById(R.id.montantFacture);

        //INITIALISATION DES CARDVIEW BUTTON OPERATEUR:
        this.cardViewTelma = findViewById(R.id.cardViewTelma);
        this.cardViewAirtel = findViewById(R.id.cardViewAirtel);
        this.cardViewPaositra = findViewById(R.id.cardViewPaositra);

        //this.progressBar = findViewById(R.id.progressBar);
        this.scanButton = findViewById(R.id.scanButton);
        this.spinnerLangue = findViewById(R.id.spinnerLanguage);

        // INITIALISATION BUNDLE:
        bundleForInformation = new Bundle();
        bundleForScan = new Bundle();

        // MISE DES DONNEES TEMPLATE POUR BUNDLESCAN:
        bundleForScan.putString("refFacture", "");
        bundleForScan.putString("refClient", "");


        // LANCEMENT SYNCHRONISATION CREDIT LEFT:
        cashlessWebApiVolley = new CashlessWebApiVolley(MainActivity.this, me);
        cashlessWebApiVolley.startSync();


        ConstraintLayout mainLayout = findViewById(R.id.mainActivityLayout);
        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (compteur < 20) {
                    compteur += 1;
                    Log.e("SECRET", String.valueOf(compteur));
                }
                if (compteur == 20) {
                    compteur = 0;
                    /*Toast.makeText(MainActivity.this, "Mode admin", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, BackHome.class);
                    startActivity(intent);*/
                }


            }
        });


        //CODE POUR CHANGER LANGUAGE:
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, codeLanguages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLangue.setAdapter(adapter);
        spinnerLangue.setSelection(0);
        spinnerLangue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String languageSelectionne = adapterView.getItemAtPosition(i).toString();
                if (languageSelectionne.equals("MG")) {
                    setLocal(MainActivity.this, "mg");
                    finish();
                    Intent rechargerPageIntent = getIntent();
                    startActivity(rechargerPageIntent);
                    Toast.makeText(MainActivity.this, "Malagasy", Toast.LENGTH_SHORT).show();

                } else if (languageSelectionne.equals("FR")) {
                    setLocal(MainActivity.this, "en");
                    finish();
                    Intent rechargerPageIntent = getIntent();
                    startActivity(rechargerPageIntent);
                    Toast.makeText(MainActivity.this, "Français", Toast.LENGTH_SHORT).show();
                }
                else if(languageSelectionne.equals("Déconnexion"))
                {
                    cashlessWebApiVolley.stopSynchroCredit();
                    SessionManagement sessionManagement = new SessionManagement(MainActivity.this);
                    sessionManagement.removeSession();
                    Intent intent = new Intent(MainActivity.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //FIN CODE POUR CHANGER LANGUAGE.
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SYNCHRONISATION TARIFICATION:
        //runSynchronisationTarification();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bundleForScan.getString("refFacture").equals("")) {
                    startScan(MainActivity.this.getString(R.string.ref_facture_scan_label));
                }
                if (!bundleForScan.getString("refFacture").equals("")) {
                    refFacture.getText().clear();
                    montantFacture.getText().clear();
                    bundleForScan.putString("refFacture", "");
                    //bundleForScan.putString("refClient", "");
                    Log.e("SCAN", "REFERENCE FACTURE b: " + bundleForScan.getString("refFacture"));
                    //Log.e("SCAN", "REFERENCE CLIENT b: " + bundleForScan.getString("refClient"));
                    startScan(MainActivity.this.getString(R.string.ref_facture_scan_label));
                }
            }
        });

        buttonPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String referenceFacture = "23517091086440";

                Toast.makeText(MainActivity.this, "Historique", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, BackHome.class);
                startActivity(intent);

            }
        });

        buttonUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMyInformation();

            }
        });

        cardViewTelma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Mvola choisi", Toast.LENGTH_SHORT).show();
                //makeSynchroTarificationBase();
                if (InternetConnection.checkConnection(MainActivity.this)) {
                    getLocation(view);
                    //progressBar.setVisibility(View.VISIBLE);
                    boolean verifMontant = verifierChampsMontant();
                    boolean verifRef = verifierChampsRef();
                    //progressBar.setVisibility(View.GONE);
                    //EMPORT DES DONNEES:
                    if (verifMontant == true && verifRef == true && verifyCategorie()) {
                        cashlessWebApiVolley.stopSynchroCredit();
                        Intent intent = new Intent(MainActivity.this, Payement.class);
                        //Bundle bundle = new Bundle();
                        bundleForInformation.putString("refFacture", refFacture.getText().toString());
                        bundleForInformation.putString("montantFacture", montantFacture.getText().toString());
                        bundleForInformation.putString("operateur", "Mvola");
                        //bundleForInformation.putString("refClient", bundleForScan.getString("refClient"));
                        bundleForInformation.putString("mois", bundleForScan.getString("mois"));
                        bundleForInformation.putString("annee", bundleForScan.getString("annee"));
                        intent.putExtras(bundleForInformation);
                        //intent.putExtra("SYNCH_CREDIT", cashlessWebApiVolley);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.scan_null), Toast.LENGTH_SHORT).show();
                    }
                    //FIN EMPORT DES DONNEES:
                } else {
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                    Log.e("CONNECTION", "Hors ligne");
                }
            }
        });

        cardViewAirtel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Airtel money choisi", Toast.LENGTH_SHORT).show();
                //makeSynchroTarificationBase();
                if (InternetConnection.checkConnection(MainActivity.this)) {
                    getLocation(view);
                    //progressBar.setVisibility(View.VISIBLE);
                    boolean verifMontant = verifierChampsMontant();
                    boolean verifRef = verifierChampsRef();
                    //progressBar.setVisibility(View.GONE);
                    //EMPORT DES DONNEES:
                    if (verifMontant == true && verifRef == true && verifyCategorie()) {
                        cashlessWebApiVolley.stopSynchroCredit();
                        Intent intent = new Intent(MainActivity.this, Payement.class);
                        //Bundle bundle = new Bundle();
                        bundleForInformation.putString("refFacture", refFacture.getText().toString());
                        bundleForInformation.putString("montantFacture", montantFacture.getText().toString());
                        bundleForInformation.putString("operateur", "Airtel");
                        //bundleForInformation.putString("refClient", bundleForScan.getString("refClient"));
                        bundleForInformation.putString("mois", bundleForScan.getString("mois"));
                        bundleForInformation.putString("annee", bundleForScan.getString("annee"));
                        intent.putExtras(bundleForInformation);
                        //intent.putExtra("SYNCH_CREDIT", cashlessWebApiVolley);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.scan_null), Toast.LENGTH_SHORT).show();
                    }
                    //FIN EMPORT DES DONNEES:
                } else {
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                    Log.e("CONNECTION", "Hors ligne");
                }
            }
        });

        cardViewPaositra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Paoma choisi", Toast.LENGTH_SHORT).show();

            }
        });

        saisieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildAlertForSaisie();
            }
        });

    }

    private void buildAlertForSaisie() {
        AlertDialog.Builder dialogBox = new AlertDialog.Builder(MainActivity.this);
        dialogBox.setTitle("Saisie");
        dialogBox.setIcon(MainActivity.this.getDrawable(R.drawable.saisie_ico));

        LinearLayout linearLayout = new LinearLayout(MainActivity.this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(15,0,15,0);

        final TextView refFactureLabel = new TextView(MainActivity.this);
        refFactureLabel.setText(R.string.refFactureLabel);
        refFactureLabel.setTextSize(20);
        refFactureLabel.setTypeface(null, Typeface.BOLD);
        linearLayout.addView(refFactureLabel);

        final EditText refFactureBox = new EditText(MainActivity.this);
        refFactureBox.setInputType(InputType.TYPE_CLASS_PHONE);
        refFactureBox.setBackground(MainActivity.this.getDrawable(R.drawable.rounded_edittext));
        refFactureBox.setGravity(Gravity.CENTER);
        linearLayout.addView(refFactureBox);


        dialogBox.setView(linearLayout);
        dialogBox.setNegativeButton(MainActivity.this.getString(R.string.annulation), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialogBox.setPositiveButton("Scan", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //bundleForInformation.putString("refClient", "");
                String refFactureForScan = refFactureBox.getText().toString().trim();
                //String refClientForScan = refClientBox.getText().toString().trim();
                if(!refFactureForScan.equals(""))
                {
                    refFacture.setText("");
                    montantFacture.setText("");
                    cashlessWebApiVolley.pauseSynchroCredit();
                    //Toast.makeText(MainActivity.this, "Ref facture: "+refFactureForScan+"=> Ref client: "+refClientForScan, Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Ref facture: "+refFactureForScan, Toast.LENGTH_SHORT).show();
                    Log.e("SCAN", "REFERENCE FACTURE: "+refFactureForScan);
                    //Log.e("SCAN", "REFERENCE CLIENT: "+refClientForScan);
                    //bundleForScan.putString("refClient", refClientForScan);
                    infoClientVolley2(refFactureForScan);
                }
                else
                {
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.donnees_insuffisante), Toast.LENGTH_SHORT).show();
                }

            }
        });

        dialogBox.show();

    }

    //LOCALISATION FONCTION:
    public void getLocation(View view) {
        gpsTracker = new GpsTracker(MainActivity.this);
        if (gpsTracker.canGetLocation()) {
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
            Toast.makeText(MainActivity.this, "Latitude: " + String.valueOf(latitude) + "-Longitude: " + String.valueOf(longitude), Toast.LENGTH_SHORT).show();
            Log.e("GPS", "Latitude: " + latitude + "-Longitude: " + longitude);
        } else {
            gpsTracker.showSettingsAlert();

        }
    }

    // FONCTION ACTIVATION BLUETOOTH:
    public boolean isBluetoothAvailable() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean valiny = false;
        if(bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON)
        {
            valiny = true;
        }
        else
        {
            AlertDialog.Builder alertDialogBT = new AlertDialog.Builder(MainActivity.this);
            alertDialogBT.setTitle("Bluetooth");
            alertDialogBT.setIcon(R.drawable.bluetooth);
            alertDialogBT.setMessage(MainActivity.this.getString(R.string.bt_ask_enabling));
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

    public boolean isLocationIsActivated()
    {
        boolean valiny = false;
        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        if (gpsTracker.canGetLocation())
        {
            valiny = true;
        }
        else
        {
            AlertDialog.Builder alertDialogBT = new AlertDialog.Builder(MainActivity.this);
            alertDialogBT.setTitle("Localisation");
            alertDialogBT.setIcon(R.drawable.location);
            alertDialogBT.setMessage(MainActivity.this.getString(R.string.loc_ask_enabling));
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

    // START SCAN FUNCTION:
    public void startScan(String scanDeQuoi)
    {
        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
        //set prompt text
        intentIntegrator.setPrompt(scanDeQuoi+"\n"+MainActivity.this.getString(R.string.flash_label));
        // set beep
        intentIntegrator.setBeepEnabled(true);

        //locked orientation
        intentIntegrator.setOrientationLocked(true);

        //set capture activity:
        intentIntegrator.setCaptureActivity(CaptureAct.class);

        //initiate scan:
        intentIntegrator.initiateScan();
    }

    //LANGUAGE FONCTION:
    public void setLocal(Activity activity, String languageCode)
    {
        Locale locale = new Locale(languageCode);
        locale.setDefault(locale);
        Resources resources = activity.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Initialize intent result
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if(intentResult.getContents() != null)
            {
                String resultatScan = intentResult.getContents();
                bundleForScan.putString("refFacture", resultatScan);
                refFacture.setText(bundleForScan.getString("refFacture"));
                infoClientVolley2(bundleForScan.getString("refFacture"));

            }else{
                //When result content is null
                //Display toast
                if(!bundleForScan.getString("refFacture").equals(""))
                {
                    refFacture.setText(bundleForScan.getString("refFacture"));
                }
                Toast.makeText(getApplicationContext(), MainActivity.this.getString(R.string.scan_null), Toast.LENGTH_SHORT).show();
            }


    }

    public void showProgressDialogForAnalysFacture(String referenceFacture)
    {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialogForAnalyseFacture = new ProgressDialog(MainActivity.this);
                progressDialogForAnalyseFacture.setTitle(MainActivity.this.getString(R.string.analyse_scan_title));
                progressDialogForAnalyseFacture.setMessage(MainActivity.this.getString(R.string.analyse_scan_message)+" "+referenceFacture);
                progressDialogForAnalyseFacture.setCancelable(false);
                progressDialogForAnalyseFacture.setIcon(R.drawable.search_ico);
                progressDialogForAnalyseFacture.show();
            }
        });
    }

    public void constructAlertDialogReferenceFacture(String message, int okOrNot, String referenceFacture, String montant, String mois, String annee, String recuS, String combinaison)
    {
        //Initialize alert dialog:
        progressDialogForAnalyseFacture.dismiss();

        //set message
        if(okOrNot==1)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            //set title:
            builder.setTitle(MainActivity.this.getString(R.string.title_alert_dialog_scan_ref));
            builder.setIcon(R.drawable.ok_icon);
            builder.setMessage(message);
            //set positive button:
            builder.setPositiveButton(MainActivity.this.getString(R.string.imprimer), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(!recuS.equals(""))
                    {
                        Bundle recuDetailBundle = new Bundle();
                        recuDetailBundle.putString("recu_detail", recuS);
                        recuDetailBundle.putString("depart", "1");
                        recuDetailBundle.putString("combinaison", combinaison);
                        recuDetailBundle.putString("duplicata", "1");
                        recuDetailBundle.putString("idInterneReel", "0");
                        Intent intentRecuDetail = new Intent(MainActivity.this, DetailPaiementFacture.class);
                        intentRecuDetail.putExtras(recuDetailBundle);
                        dialogInterface.dismiss();
                        startActivity(intentRecuDetail);
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.facture_paye_not_in_database), Toast.LENGTH_SHORT).show();
                        dialogInterface.dismiss();
                        cashlessWebApiVolley.resumeSynchroCredit();
                    }
                    
                }
            });

            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    cashlessWebApiVolley.resumeSynchroCredit();
                }
            });

            builder.show();
        }
        else if(okOrNot==2)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            //set title:
            builder.setTitle(MainActivity.this.getString(R.string.title_alert_dialog_scan_ref));
            builder.setIcon(R.drawable.not_ok_icon);
            builder.setMessage(message);
            //set positive button:
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    dialogInterface.dismiss();
                }
            });

            builder.show();
        }
        else if(okOrNot==3)
        {
            showResultatScan(montant);
        }

    }

    public Map<String, String> traitementExistenceFacture(String referenceFacture)
    {
        // TRAITEMENT EXISTENCE FACTURE:
        AsyncApiJirama jiramaRestApi = new AsyncApiJirama();
        Map<String, String> verificationExistenceFacture = null;
        try{
            verificationExistenceFacture = jiramaRestApi.execute(referenceFacture).get();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
            Log.e(factureTag, e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(factureTag, e.getMessage());
        }

        return verificationExistenceFacture;

    }

    public boolean verifierChampsRef()
    {
        String refFactureS = this.refFacture.getText().toString();
        boolean valiny = false;

        if(TextUtils.isEmpty(refFactureS))
        {
            refFacture.setError(MainActivity.this.getString(R.string.error_champ_vide));
        }
        else
        {
            valiny = true;
        }

        return valiny;
    }

    public boolean verifierChampsMontant()
    {
        boolean valiny = false;
        String montantFactureS = this.montantFacture.getText().toString();

        if(TextUtils.isEmpty(montantFactureS))
        {
            montantFacture.setError(MainActivity.this.getString(R.string.error_champ_vide));
        }
        if(!TextUtils.isEmpty(montantFactureS))
        {
            double montantDouble = Double.valueOf(montantFactureS);
            if(montantDouble<=0)
            {
                montantFacture.setError(MainActivity.this.getString(R.string.error_superieur_zero));
            }
            else
            {
                valiny = true;
            }
        }

        return valiny;

    }

    public String[] splitCodeBarre(String code)
    {
        String[] refEtMontant = code.split("-");
        return  refEtMontant;
    }

    //SYNCHRONISATION TARIFICATION OPERATEUR:
    public void runSynchronisationTarification()
    {
        int millisInFuture = 90000;
        int countDownInterval = 1000;
        new CountDownTimer(millisInFuture, countDownInterval)
        {
            @Override
            public void onTick(long l) {

                Log.e(TARIFICATIONTAG, "MillisInFuture: "+ millisInFuture);
                //makeSynchroTarificationBase();
            }

            @Override
            public void onFinish() {
                Log.e(TARIFICATIONTAG, "VERIFICATION MISE À JOUR TERMINE");
            }
        }.start();
    }
    public void makeSynchroTarificationBase()
    {
        if(InternetConnection.checkConnection(this))
        {
            firestoreDB.collection("tarification_operateur")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                //ALAINA NY VOALOHANY POUR INSERTION:

                                List<TarificationOperateur> listeTarifViaFirebase = new ArrayList<>();
                                //MAKA NY LISTE TARIFICATION VIA FIRESTORE:
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d(TARIFICATIONTAG, document.getId() + " => " + document.getData());
                                        Map<String, Object> temporaireTarificationFirebase = new HashMap<>();
                                        temporaireTarificationFirebase = document.getData();
                                        TarificationOperateur tarificationOperateurFirebase = new TarificationOperateur(Integer.valueOf(String.valueOf(temporaireTarificationFirebase.get("id"))), String.valueOf(temporaireTarificationFirebase.get("operateur")), String.valueOf(temporaireTarificationFirebase.get("tarif")), String.valueOf(temporaireTarificationFirebase.get("datemaj")));
                                        Log.e("OBJT", tarificationOperateurFirebase.getOperateur());
                                        listeTarifViaFirebase.add(tarificationOperateurFirebase);
                                    }

                                    System.out.println("LISTE FIREBASE taille: "+listeTarifViaFirebase.size());
                                //VERIFICATION SI SQLITE TABLE TARIFICATION ENCORE VIDE:
                                int verificationSQLiteSiVide = databaseManagerSqlite.verificationEtInsertionGeneraleTarificationOperateur(listeTarifViaFirebase);
                                //MAKA NY AO ANATY BASE SQLITE:
                                if(verificationSQLiteSiVide==1)
                                {
                                    List<TarificationOperateur> listeTarifViaSQLite = databaseManagerSqlite.getTarificationOperateurAll();
                                    Utilitaire utilitaire = new Utilitaire();
                                    int verificationCoherence = utilitaire.compareTwoArrayList(listeTarifViaFirebase, listeTarifViaSQLite);
                                    if(verificationCoherence==1)
                                    {
                                        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.mise_a_jour_en_cours),Toast.LENGTH_SHORT).show();
                                        //S'IL Y A INCOHERENCE DE DONNEES:
                                        for(int i=0; i<listeTarifViaFirebase.size(); i++)
                                        {
                                            databaseManagerSqlite.updateTarificationOperateurBase(listeTarifViaFirebase.get(i));
                                        }
                                    }
                                    else
                                    {
                                        //S'IL N'Y A PAS D'INCOHERENCE DE DONNEES:
                                        Log.e(TARIFICATIONTAG, "AUCUNE MISE À JOUR");
                                    }

                                }
                                else
                                {
                                    Log.e(TARIFICATIONTAG, "PREMIERE INSERION TARIFICATION");
                                }

                            } else {
                                Log.w(TARIFICATIONTAG, "Error getting documents.", task.getException());
                            }
                        }
                    });
        }
        else
        {
            Log.e(TARIFICATIONTAG, "ERREUR CONNEXION DANS LA SYNCHRONISATION");
        }

    }
    public void infoClientVolley2(String referenceFacture)
    {
        //String url  = domain_name+url_info_client2+referenceFacture+"&versionApp=1.8.2";
        String version = AppUtils.getAppVersion(this);
        String url  = domain_name+url_info_client2+referenceFacture+"&versionApp="+version;

        Log.d("TAG", "url infoClientVolley2: "+url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response != null && response.trim().equalsIgnoreCase("Mise à jour nécessaire")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Information")
                            .setMessage("Une nouvelle version de cette application est disponible.\n\nPour continuer à utiliser toutes les fonctionnalités, merci de bien vouloir mettre à jour l’application dès maintenant.")
                            .setCancelable(false)
                            //.setIcon(R.drawable.warning_ico) // facultatif
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    // Tu peux aussi rediriger vers Play Store ici
                                }
                            });

                    builder.show();

                    // Ferme le ProgressDialog si ouvert
                    if (progressDialogForAnalyseFacture != null && progressDialogForAnalyseFacture.isShowing()) {
                        progressDialogForAnalyseFacture.dismiss();
                    }

                    return; // quitte onResponse ici, inutile de continuer
                }
                try {

                        JiramaRestApi jiramaRestApi = new JiramaRestApi();
                        Map<String, String> valinyFarany = jiramaRestApi.traitementInfoClient2(response, referenceFacture);
                        Log.e("EXISTENCE", "VALINY MAP: "+ valinyFarany);
                        //Toast.makeText(MainActivity.this, "Facture existe: \n"+ valinyFarany.get("exist")+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();

                        if(valinyFarany.get("exist").equals("true"))
                        {
                            Toast.makeText(MainActivity.this, "Facture existante", Toast.LENGTH_SHORT).show();
                            bundleForInformation.putString("numero_client", valinyFarany.get("numero_client"));
                            bundleForInformation.putString("nom_client", valinyFarany.get("nom_client"));
                            bundleForInformation.putString("adresse_client", valinyFarany.get("adresse_client"));
                            bundleForInformation.putString("refClient", valinyFarany.get("reference_client"));

                            statutFactureVolley(referenceFacture);
                        }
                        else
                        {
                            // SI FACTURE N\'EXISTE PAS:
                            cashlessWebApiVolley.resumeSynchroCredit();
                            int okOrNot = 2;
                            String reference = "";
                            String montant = "";
                            String mois = "";
                            String annee = "";
                            String recu = "";
                            String combinaison = "";
                            constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_nexiste), okOrNot, reference, montant, mois, annee, recu, combinaison);
                            Toast.makeText(MainActivity.this, "Facture n\'existe pas!", Toast.LENGTH_SHORT).show();
                        }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialogForAnalyseFacture.dismiss();
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        myRequestQueue.add(stringRequest);

        progressDialogForAnalyseFacture = new ProgressDialog(MainActivity.this);
        progressDialogForAnalyseFacture.setTitle(MainActivity.this.getString(R.string.analyse_scan_title));
        progressDialogForAnalyseFacture.setMessage(referenceFacture);
        progressDialogForAnalyseFacture.setCancelable(false);
        progressDialogForAnalyseFacture.setIcon(R.drawable.search_ico);
        progressDialogForAnalyseFacture.show();
    }

    public void infoClientVolley(String referenceFacture, String referenceClient)
    {
        String url  = domain_name+url_info_client+referenceFacture+"&referenceclient="+referenceClient;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JiramaRestApi jiramaRestApi = new JiramaRestApi();
                    Map<String, String> valinyFarany = jiramaRestApi.traitementInfoClient(response, referenceFacture);
                    Log.e("EXISTENCE", "VALINY MAP: "+ valinyFarany);
                    //Toast.makeText(MainActivity.this, "Facture existe: \n"+ valinyFarany.get("exist")+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();

                    if(valinyFarany.get("exist").equals("true"))
                    {
                        Toast.makeText(MainActivity.this, "Facture existante", Toast.LENGTH_SHORT).show();
                        bundleForInformation.putString("numero_client", valinyFarany.get("numero_client"));
                        bundleForInformation.putString("nom_client", valinyFarany.get("nom_client"));
                        bundleForInformation.putString("adresse_client", valinyFarany.get("adresse_client"));
                        //int okOrNot = 1;
                        //String reference = "";
                        //String montant = "";
                        //constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_existe), okOrNot, reference, montant);
                        //SI FACTURE EXISTE:
                        statutFactureVolley(referenceFacture);
                    }
                    else
                    {
                        // SI FACTURE N\'EXISTE PAS:
                        cashlessWebApiVolley.resumeSynchroCredit();
                        int okOrNot = 2;
                        String reference = "";
                        String montant = "";
                        String mois = "";
                        String annee = "";
                        String recu = "";
                        String combinaison = "";
                        constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_nexiste), okOrNot, reference, montant, mois, annee, recu, combinaison);
                        Toast.makeText(MainActivity.this, "Facture n\'existe pas!", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialogForAnalyseFacture.dismiss();
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        myRequestQueue.add(stringRequest);

        progressDialogForAnalyseFacture = new ProgressDialog(MainActivity.this);
        progressDialogForAnalyseFacture.setTitle(MainActivity.this.getString(R.string.analyse_scan_title));
        progressDialogForAnalyseFacture.setMessage(referenceFacture);
        progressDialogForAnalyseFacture.setCancelable(false);
        progressDialogForAnalyseFacture.setIcon(R.drawable.search_ico);
        progressDialogForAnalyseFacture.show();
    }

    public void verifExistenceFactureVolley(RequestQueue requestQueue, String referenceFacture)
    {
        String url = domain_name+url_existence_facture+referenceFacture;
        //String url = "https://cashless.eqima.org/api/paiement_facture/getAll2";
        //requestQueue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //progressDialogForAnalyseFacture.dismiss();
                try {
                JSONObject reponsePureJSON = new JSONObject(response);
                JiramaRestApi jiramaRestApi = new JiramaRestApi();
                String valiny = reponsePureJSON.get("fs_DATABROWSE_F55INV").toString();
                Map<String, String> valinyFarany = jiramaRestApi.traitementExistenceFacture(valiny);
                Log.e("EXISTENCE", "VALINY MAP: "+ valinyFarany);
                //Toast.makeText(MainActivity.this, "Facture existe: \n"+ valinyFarany.get("exist")+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();

                    if(valinyFarany.get("exist").equals("true"))
                    {
                        Toast.makeText(MainActivity.this, "Facture existante", Toast.LENGTH_SHORT).show();
                        //int okOrNot = 1;
                        //String reference = "";
                        //String montant = "";
                        //constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_existe), okOrNot, reference, montant);
                        //SI FACTURE EXISTE:
                        statutFactureVolley(referenceFacture);
                    }
                    else
                    {
                        // SI FACTURE N\'EXISTE PAS:
                        int okOrNot = 2;
                        String reference = "";
                        String montant = "";
                        String mois = "";
                        String annee = "";
                        String recu = "";
                        String combinaison = "";
                        constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_nexiste), okOrNot, reference, montant, mois, annee, recu, combinaison);
                        Toast.makeText(MainActivity.this, "Facture n\'existe pas!", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialogForAnalyseFacture.dismiss();
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);

        progressDialogForAnalyseFacture = new ProgressDialog(MainActivity.this);
        progressDialogForAnalyseFacture.setTitle(MainActivity.this.getString(R.string.analyse_scan_title));
        progressDialogForAnalyseFacture.setMessage(MainActivity.this.getString(R.string.analyse_scan_message)+" "+referenceFacture);
        progressDialogForAnalyseFacture.setCancelable(false);
        progressDialogForAnalyseFacture.setIcon(R.drawable.search_ico);
        progressDialogForAnalyseFacture.show();

    }

    public void statutFactureVolley(String referenceFacture)
    {
        String url = domain_name+url_statut_facture+referenceFacture;
        JiramaRestApi jiramaRestApi = new JiramaRestApi();
        //RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    String valinyFarany = jiramaRestApi.traitementStatutFacture(response);
                    Log.e("STATUT", "REPONSE: "+ valinyFarany);
                    //Toast.makeText(context, "Facture payée: \n"+ valinyFarany+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();
                    if(valinyFarany.equals("Y"))
                    {
                        getInfoPaiement(referenceFacture);
                        int okOrNot = 1;
                        String reference = "";
                        String montant = "";
                        String mois = "";
                        String annee = "";
                        Toast.makeText(MainActivity.this, "Facture payée", Toast.LENGTH_SHORT).show();
                    }
                    if(valinyFarany.equals("N"))
                    {
                        montantFactureVolley(referenceFacture);
                        //int okOrNot = 3;
                        //String montant = existence.get("montant");
                        //String montant = "";
                        //refFacture.setText(referenceFacture);
                        //montantFacture.setText(montant);
                        //constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_non_payee), okOrNot, referenceFacture, montant);
                        Toast.makeText(MainActivity.this, "Facture pas encore payée", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialogForAnalyseFacture.dismiss();
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        myRequestQueue.add(stringRequest);

    }

    public void montantFactureVolley(String referenceFacture)
    {
        String url = domain_name+url_montant+referenceFacture;
        //RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JiramaRestApi jiramaRestApi = new JiramaRestApi();
                    Map<String, String> valinyFarany = jiramaRestApi.traitementGetMontant_WS14(response);
                    Log.e("MONTANT", "REPONSE: "+ valinyFarany);
                    String montantAfficher = valinyFarany.get("Montant");
                    Toast.makeText(MainActivity.this, "Montant facture: \n"+ valinyFarany.get("Montant"), Toast.LENGTH_SHORT).show();
                    int okOrNot = 3;

                    String mois = valinyFarany.get("Mois");
                    String annee = valinyFarany.get("Annee");

                    //String montant = "";
                    refFacture.setText(referenceFacture);
                    montantFacture.setText(montantAfficher);

                    bundleForScan.putString("mois", mois);
                    bundleForScan.putString("annee", annee);
                    bundleForScan.putString("categorie", valinyFarany.get("Categorie"));
                    String recu = "";
                    String combinaison = "";

                    progressDialogForAnalyseFacture.dismiss();
                    showResultatScan(montantAfficher);
                    //constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_non_payee), okOrNot, referenceFacture, montantAfficher, mois, annee, recu, combinaison);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialogForAnalyseFacture.dismiss();
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        myRequestQueue.add(stringRequest);

    }

    public void getInfoPaiement(String referenceFacture)
    {
        String url = domain_name+url_info_paiement+referenceFacture;
        //RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("VOLLEY", "REPONSE: "+ response);
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    if(!response.equals("null") && !response.equals(""))
                    {
                        PaiementFactureView paiementFacture = objectMapper.readValue(response, PaiementFactureView.class);
                        recuFactureDejaPayer = new Recu();

                        recuFactureDejaPayer.setNumeroRecuJirama(paiementFacture.getCodeRecu());
                        recuFactureDejaPayer.setNumeroCashpoint(paiementFacture.getNumeroCashpoint());
                        recuFactureDejaPayer.setRefFacture(paiementFacture.getRefFacture());
                        recuFactureDejaPayer.setMois(paiementFacture.getMois());
                        recuFactureDejaPayer.setAnnee(paiementFacture.getAnnee());
                        recuFactureDejaPayer.setReferenceClient(paiementFacture.getRefClient());
                        recuFactureDejaPayer.setNomClient(paiementFacture.getNomClient());
                        recuFactureDejaPayer.setMontantFacture(String.valueOf(paiementFacture.getMontantFacture()));
                        recuFactureDejaPayer.setFrais(String.valueOf(paiementFacture.getFrais()));
                        recuFactureDejaPayer.setMontantTotale(String.valueOf(paiementFacture.getTotal()));
                        recuFactureDejaPayer.setRefTransaction(paiementFacture.getRefTransaction());
                        recuFactureDejaPayer.setNumero(paiementFacture.getNumeroPayeur());
                        recuFactureDejaPayer.setOperateur(paiementFacture.getOperateur());
                        recuFactureDejaPayer.setDaty(paiementFacture.getDatePaiement());
                        recuFactureDejaPayer.setIdInterne(paiementFacture.getId());
                        recuFactureDejaPayer.setCaissier(paiementFacture.getCaissier());

                        int okOrNot = 1;
                        String reference = "";
                        String montant = "";
                        String mois = "";
                        String annee = "";
                        String combinaison = recuFactureDejaPayer.getRefFacture()+"_"+recuFactureDejaPayer.getNumeroRecuJirama();

                        constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_deja_payee), okOrNot, reference, montant, mois, annee, recuFactureDejaPayer.contruireMonRecu(), combinaison);

                    }
                    else
                    {
                        int okOrNot        = 1;
                        String reference   = "";
                        String montant     = "";
                        String mois        = "";
                        String annee       = "";
                        String combinaison = "";
                        String monRecu        = "";

                        constructAlertDialogReferenceFacture(MainActivity.this.getString(R.string.facture_deja_payee), okOrNot, reference, montant, mois, annee, monRecu, combinaison);

                    }


                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    if(e.getMessage()!=null)
                    {
                        Toast.makeText(MainActivity.this, "Json error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialogForAnalyseFacture.dismiss();
                if(error.getMessage()!=null)
                {
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                    Log.e("VOLLEY", "ERREUR: "+ error.toString());
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error)+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                        Log.e("VOLLEY", "ERREUR: "+ error.toString());
                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error), Toast.LENGTH_SHORT).show();
                }

            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        myRequestQueue.add(stringRequest);

    }

    /******** BLUETOOTH VERSION FARANY MODIFICATION: Dantsu:ESCPOS-ThermalPrinter ******************/
    public void doPrint(View view, String recu) {
        //BLUETOOTH:
        BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
            } else {

                if (connection != null) {
                    EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
                    String logo = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logojirama2, DisplayMetrics.DENSITY_140)) + "</img>\n";

                    printer.printFormattedText(logo);
                    connection.disconnect();

                } else {
                    Toast.makeText(this, MainActivity.this.getString(R.string.no_connected_imprimante), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("APP", "Can't print", e);
        }
    }

    public boolean verifyCategorie()
    {
        boolean valiny = false;
        if(!bundleForScan.get("categorie").equals("80"))
        {
            valiny = true;
            Toast.makeText(MainActivity.this, "Catégorie ok", Toast.LENGTH_SHORT).show();

        }
        else
        {
            Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.categorie_non_payable), Toast.LENGTH_SHORT).show();
        }
        return valiny;
    }

    public void showMyInformation()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View user_layout            = getLayoutInflater().inflate(R.layout.user_layout, null);
        TextView close              = user_layout.findViewById(R.id.close);
        TextView nom_user           = user_layout.findViewById(R.id.nom_user);
        TextView id_user            = user_layout.findViewById(R.id.id_user);
        TextView kredit_user        = user_layout.findViewById(R.id.kredit_user);
        TextView phone_user         = user_layout.findViewById(R.id.phone_user);
        TextView location_user      = user_layout.findViewById(R.id.location_user);

        nom_user.setText(mySession[4]);
        id_user.setText(mySession[0]);
        kredit_user.setText(mySessionManagement.getNbCredit());
        phone_user.setText(mySession[1]);
        // GET LOCATION:
        getLocation(user_layout);
        location_user.setText(latitude+", "+longitude);

        builder.setView(user_layout);
        AlertDialog alertDialog = builder.create();
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
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
        cashlessWebApiVolley.startSync();
    }

    public void showResultatScan(String montant)
    {
        Utilitaire utilitaire = new Utilitaire();
        View resultatScanLayout   = getLayoutInflater().inflate(R.layout.resultat_scan_layout, null);
        TextView nom_client       = resultatScanLayout.findViewById(R.id.nom_client);
        TextView adresse_client   = resultatScanLayout.findViewById(R.id.adresse_client);
        TextView reference_client = resultatScanLayout.findViewById(R.id.reference_client);
        TextView date_facture     = resultatScanLayout.findViewById(R.id.date_facture);
        TextView montant_facture  = resultatScanLayout.findViewById(R.id.montant_facture);
        TextView categorie_facture= resultatScanLayout.findViewById(R.id.categorie_facture);
        EditText montant_donnee   = resultatScanLayout.findViewById(R.id.montant_donnee);
        Button calculer_button    = resultatScanLayout.findViewById(R.id.calculer_button);

        TextView monnaie          = resultatScanLayout.findViewById(R.id.monnaie);

        nom_client.setText(bundleForInformation.getString("nom_client"));
        adresse_client.setText(bundleForInformation.getString("adresse_client"));
        reference_client.setText(bundleForInformation.getString("refClient"));
        categorie_facture.setText(bundleForScan.getString("categorie"));
        date_facture.setText(bundleForScan.getString("mois")+" "+bundleForScan.getString("annee"));
        montant_facture.setText(montant);

        montant_donnee.addTextChangedListener(new TextWatcher(){
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                String strPattern = "[a-zA-Z~!@#$%^&*()_+{}\\[\\]:;,.<>/?-]";

                Pattern p = Pattern.compile(strPattern);
                Matcher m = p.matcher(montant_donnee.getText().toString().trim());

                if (m.find())
                {
                    // Not allowed
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.no_special_caractere), Toast.LENGTH_SHORT).show();
                    montant_donnee.setText("");
                }
            }
            @Override
            public void afterTextChanged(Editable arg0) { }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        });


        calculer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String montantDonneeString = montant_donnee.getText().toString().trim();
                if(!montantDonneeString.equals(""))
                {
                    if(Integer.valueOf(montantDonneeString)>=Integer.valueOf(montant))
                    {
                        int monnaieARendre = utilitaire.rendreMonnaie(montantDonneeString, montant);
                        Log.e("MONNAIE", String.valueOf(monnaieARendre));
                        monnaie.setText("");
                        monnaie.setText(String.valueOf(monnaieARendre));
                    }
                    else{
                          montant_donnee.setError(MainActivity.this.getString(R.string.verify_montant_donnee));
                          Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.verify_montant_donnee), Toast.LENGTH_SHORT).show();
                    }

                }
                else{
                        montant_donnee.setError(MainActivity.this.getString(R.string.error_champ_vide));
                }
            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setIcon(R.drawable.warning_icon);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setView(resultatScanLayout);

        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialogBuilder.show();
    }

}