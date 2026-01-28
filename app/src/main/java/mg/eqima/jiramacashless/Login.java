package mg.eqima.jiramacashless;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import mg.eqima.jiramacashless.encrypt.Encrypt;
import mg.eqima.jiramacashless.internet.InternetConnection;
import mg.eqima.jiramacashless.session.SessionManagement;
import mg.eqima.jiramacashless.user.User;
import mg.eqima.jiramacashless.utilitaire.AppUtils;

public class Login extends AppCompatActivity {

    private FirebaseFirestore firebaseFirestoreInstance;

    //LES CHAMPS DE LOGIN:
    private EditText telephone;
    private EditText password;
    private Button loginButton;
    private TextView registerLink;
    private TextView registerLabel ;
    private ProgressBar progressBarLogin;

    // NFC:
    private final String errorDetected = "Erreur";
    private final String write_success = "Tag ok";
    private final String write_error = "Erreur dans le tag";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;

    private String domain_name;
    private String login_url        = "/cashpoint/existenceVerifying?numero=";

    private Environnement environnement;


    private void checkSession() {
        //VERIFIER SI L'USER EST CONNECTER:
        SessionManagement sessionManagement = new SessionManagement(Login.this);
        String[] sessionUser = sessionManagement.getSession();
        if(!sessionUser[0].equals("-1"))
        {
            //SI UNE SESSION EXISTE:
            moveToMainActivity();
        }
        else
        {
            //RESTE SUR LA PAGE DE LOGIN
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        checkSession();
        String version = AppUtils.getAppVersion(this);
        Log.d("APP_VERSION", version) ;

        telephone = findViewById(R.id.telephoneEdit);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.registerButton);
        progressBarLogin = findViewById(R.id.progressBarLogin);
        registerLabel = findViewById(R.id.registerLabel);
        String versionApp = AppUtils.getAppVersion(this);
        registerLabel.setText("Jirakaiky v" + versionApp);

        //INSTANCIATION FIREBASE FIRESTORE:
        //firebaseFirestoreInstance = FirebaseFirestore.getInstance();

        environnement = new Environnement();
        domain_name = environnement.getDomainName();

        // NFC:
        getLoginNFC();

        //LOGIN BUTTON:
        this.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    String stelephone = telephone.getText().toString().trim();
                    String sPassword = password.getText().toString().trim();

                    if (TextUtils.isEmpty(stelephone)) {
                        telephone.setError(Login.this.getString(R.string.error_Identifiant));
                        return;

                    }
                    if (TextUtils.isEmpty(sPassword)) {
                        password.setError(Login.this.getString(R.string.error_Password));
                        return;
                    }
                    if (!TextUtils.isEmpty(sPassword) && !TextUtils.isEmpty(stelephone)) {
                        if(InternetConnection.checkConnection(Login.this)) {
                            progressBarLogin.setVisibility(View.VISIBLE);
                            Log.e("USER", "Numéro: " + stelephone + " Password:" + sPassword);
                            authenticateUser(stelephone, sPassword);
                        }
                        else
                        {
                            Toast.makeText(Login.this, Login.this.getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                        }
                    }


            }
        });
    }

    /*public void authenticateUser(String phoneNumber, String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Encrypt enc = new Encrypt();
        //String sha = enc.SHA256(password);
        //Log.e("SHA", sha);

        firebaseFirestoreInstance.collection("cashlessUser")
                .whereEqualTo("numero",phoneNumber)
                .whereEqualTo("password",enc.SHA256(password))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if(!task.getResult().isEmpty())
                            {
                                Map<String, Object> userObject = new HashMap<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.e("USER", document.getId() + " => " + document.getData());
                                    userObject = document.getData();
                                }
                                progressBarLogin.setVisibility(View.GONE);
                                System.out.println(userObject.toString());
                                if(String.valueOf(userObject.get("validation")).equals("true"))
                                {
                                    User userForSession = new User();
                                    userForSession.setId(String.valueOf(userObject.get("id")));
                                    userForSession.setNumero(String.valueOf(userObject.get("numero")));
                                    userForSession.setIdCard(String.valueOf(userObject.get("id_carte")));
                                    Toast.makeText(Login.this, "User validé", Toast.LENGTH_SHORT).show();
                                    createSession(userForSession);
                                    //Intent intent = new Intent(Login.this, MainActivity.class);
                                    //startActivity(intent);
                                }
                                else
                                {
                                    Toast.makeText(Login.this, Login.this.getString(R.string.account_in_waiting), Toast.LENGTH_SHORT).show();
                                }

                            }
                            else
                            {
                                progressBarLogin.setVisibility(View.GONE);
                                Toast.makeText(Login.this, Login.this.getString(R.string.invalidUser), Toast.LENGTH_SHORT).show();
                                Log.e("USER", "Erreur identification");
                            }

                        } else {
                            progressBarLogin.setVisibility(View.GONE);
                            Log.e("USER:", "Error documents: ", task.getException());
                        }
                    }
        });
    }*/

    public void authenticateUser(String telephone, String password) {
        Encrypt enc = new Encrypt();
        String passEnc = null;
        try {
            passEnc = enc.SHA256(password);
            Log.d("Passowrd",passEnc) ;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(Login.this);
        String url = domain_name+login_url+telephone+"&password="+passEnc;
        Log.d("LOGIN",url) ;
        //String url="https://cashless.eqima.org/cashpoint/existenceVerifying?numero=0343659444&password=fd791f91c5dfd6e3f96a33cd6e07e6c35c9614293f6466610bff5c2f3c19ec09";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("LOGIN", response);
                        if(response.equals("null")) {
                            progressBarLogin.setVisibility(View.GONE);
                            Toast.makeText(Login.this, Login.this.getString(R.string.invalidUser), Toast.LENGTH_SHORT).show();
                            Log.e("USER", "Erreur identification");
                        }
                        else if(!response.equals("null")) {
                            JSONObject userObject = null;
                            try {
                                userObject = new JSONObject(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if(userObject.has("validation")) {
                                try {
                                    if(userObject.getString("validation").equals("true")) {
                                        User userForSession = new User();
                                        userForSession.setId(userObject.getString("id"));
                                        userForSession.setNumero(userObject.getString("numero"));
                                        userForSession.setSignature(userObject.getString("signatureCashpoint"));
                                        userForSession.setNom(userObject.getString("nom"));
                                        userForSession.setCredit(userObject.getString("resteCredit"));
                                        userForSession.setIdDistributeur((userObject.getString("idDistributeur")));
                                        Toast.makeText(Login.this, "User validé", Toast.LENGTH_SHORT).show();
                                        createSession(userForSession);
                                    } else {
                                        progressBarLogin.setVisibility(View.GONE);
                                        Toast.makeText(Login.this, Login.this.getString(R.string.account_in_waiting), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    progressBarLogin.setVisibility(View.GONE);
                                    if(e.getMessage() != null) {
                                        Toast.makeText(Login.this, "Json login error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBarLogin.setVisibility(View.GONE);
                if(error.getMessage() != null) {
                    Toast.makeText(Login.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ERROR", error.getMessage());
                }
                if(error.networkResponse != null) {
                    Toast.makeText(Login.this, "Login code error: "+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    Log.e("ERROR", String.valueOf(error.networkResponse.statusCode));
                }
            }
        });

        // Configuration de la politique de retry avec timeout de 20 secondes
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000, // timeout en millisecondes (20 secondes)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // nombre maximum de tentatives
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT // multiplicateur de backoff
        ));

        requestQueue.add(stringRequest);
    }
    /*
    public void authenticateUser(String telephone, String password) {
        Encrypt enc = new Encrypt();
        String passEnc = null;
        try {
            passEnc = enc.SHA256(password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(Login.this);
        String url = domain_name+ login_url + telephone+"&password="+passEnc;

        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("LOGIN", response);
                if(response.equals("null"))
                {
                    progressBarLogin.setVisibility(View.GONE);
                    Toast.makeText(Login.this, Login.this.getString(R.string.invalidUser), Toast.LENGTH_SHORT).show();
                    Log.e("USER", "Erreur identification");
                }
                else if(!response.equals("null"))
                {
                    JSONObject userObject = null;
                    try {
                        userObject = new JSONObject(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(userObject.has("validation"))
                    {
                        try {
                            if(userObject.getString("validation").equals("true"))
                            {
                                User userForSession = new User();
                                userForSession.setId(userObject.getString("id"));
                                userForSession.setNumero(userObject.getString("numero"));
                                userForSession.setSignature(userObject.getString("signatureCashpoint"));
                                userForSession.setNom(userObject.getString("nom"));
                                userForSession.setCredit(userObject.getString("resteCredit"));
                                userForSession.setIdDistributeur((userObject.getString("idDistributeur")));
                                //Toast.makeText(Login.this, "User validé: "+ userForSession.getSignature(), Toast.LENGTH_SHORT).show();
                                Toast.makeText(Login.this, "User validé", Toast.LENGTH_SHORT).show();
                                createSession(userForSession);

                            }
                            else
                            {
                                progressBarLogin.setVisibility(View.GONE);
                                Toast.makeText(Login.this, Login.this.getString(R.string.account_in_waiting), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            progressBarLogin.setVisibility(View.GONE);
                            if(e.getMessage()!=null)
                            {
                                Toast.makeText(Login.this, "Json login error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBarLogin.setVisibility(View.GONE);
                if(error.getMessage()!=null)
                {
                    Toast.makeText(Login.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ERROR", error.getMessage());
                    if(error.networkResponse!=null)
                    {
                        Toast.makeText(Login.this, "Login code error: "+error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                        Log.e("ERROR", String.valueOf(error.networkResponse.statusCode));
                    }

                }

            }
        });

        requestQueue.add(stringRequest);
    }*/

    public void createSession(User user)
    {
        SessionManagement sessionManagement = new SessionManagement(Login.this);
        sessionManagement.saveSession(user);
        moveToMainActivity();
    }

    public void moveToMainActivity()
    {
        Intent intent = new Intent(Login.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void getLoginNFC()
    {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter==null)
        {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_LONG).show();
            finish();
        }

        readFromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[] {tagDetected};
    }

    private void readFromIntent(Intent intent)
    {
        String action = intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if(rawMsgs != null)
            {
                msgs = new NdefMessage[rawMsgs.length];
                for(int i = 0; i< rawMsgs.length; i++)
                {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }

    }

    private void buildTagViews(NdefMessage[] msgs) {
        if(msgs == null || msgs.length ==0) return;
        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // get the text encoding
        int languageCodeLength = payload[0] & 0063; // GET the language code, e.g. "en"
        // String languageCode = new String(playload, 1, languageCodeLength, "US-ASCII");

        try{
            //Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);

        }catch (UnsupportedEncodingException e)
        {
            Log.e("UnsupportedEncoding", e.toString());
        }
        Log.e("NFC","NFC content:"+ text);

        try {
            JSONObject nfcContent = new JSONObject(text);
            if(nfcContent.has("login"))
            {
                telephone.setText(nfcContent.getString("login"));
                Toast.makeText(Login.this, Login.this.getString(R.string.nfc_autorise), Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(Login.this, Login.this.getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(Login.this, Login.this.getString(R.string.error), Toast.LENGTH_SHORT).show();
        }
    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection:
        ndef.close();
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textBytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
        {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeModeOff();
    }

    @Override
    protected void onResume() {
        super.onResume();
        writeModeOn();
    }

    private void writeModeOn()
    {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, null);
    }

    private void writeModeOff()
    {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }


}