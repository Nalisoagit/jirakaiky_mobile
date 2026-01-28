package mg.eqima.jiramacashless;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import mg.eqima.jiramacashless.encrypt.Encrypt;
import mg.eqima.jiramacashless.internet.InternetConnection;
import mg.eqima.jiramacashless.user.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText telephone;
    private EditText email;
    private EditText nom;
    private EditText password;
    private Button registerButton;
    private ProgressBar progressBarRegister;
    private FirebaseFirestore firestoreInstance;
    private TextView loginLink;

    private String USER_TAG = "USER";

    //VARIABLES UTILISEE DANS VERIFICATION EXISTANCE USER:
        int numeroDejaUtiliseBool = 0; //0: mbola tsisy verif; 1:tsy miexiste; 2: miexiste
    //VARIABLE UTILISE SI UTILISATEUR EN ATTENTE:
        int userEnAttente = 0; //0: mbola tsisy verif; 1:tsy miexiste; 2: miexiste


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_register);
        telephone = findViewById(R.id.telephoneEdit);
        email = findViewById(R.id.emailEdit);
        nom = findViewById(R.id.nomEdit);
        password = findViewById(R.id.password);
        progressBarRegister = findViewById(R.id.progressBarRegister);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);

        firestoreInstance = FirebaseFirestore.getInstance();

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean verification = verificationChampsGenerale();
                Log.e("VERIFICATION", String.valueOf(verification));
                if(verification==true)
                {
                    progressBarRegister.setVisibility(View.VISIBLE);
                    if(InternetConnection.checkConnection(RegisterActivity.this)) {
                        registerButton.setClickable(false);
                        Log.e("BUTTON", "DESACTIVE");
                        String telephoneS = telephone.getText().toString().trim();
                        String emailS = email.getText().toString().trim();
                        String nomS = nom.getText().toString().trim();
                        String passwordS = password.getText().toString().trim();

                        User user = new User(emailS, nomS, telephoneS, passwordS);

                        //DEBUT ENREGISTREMENT UTILISATEUR:
                            //VERIFICATION SI NUMERO TELEPHONE DEJA UTILISE:
                                verifIfPhoneNumberIsUsed(user);
                    }
                    else
                    {
                        progressBarRegister.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, RegisterActivity.this.getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                   }
                }
            }
        });


    }

    public boolean verificationChampsGenerale()
    {
        String telephoneS = telephone.getText().toString().trim();
        String emailS = email.getText().toString().trim();
        String nomS = nom.getText().toString().trim();
        String passwordS = password.getText().toString().trim();

        boolean valiny = false;

        if(TextUtils.isEmpty(telephoneS))
        {
            telephone.setError(RegisterActivity.this.getString(R.string.requierd_numero));
        }
        if(TextUtils.isEmpty(emailS))
        {
            email.setError(RegisterActivity.this.getString(R.string.requierd_mail));
        }
        if(TextUtils.isEmpty(nomS))
        {
            nom.setError(RegisterActivity.this.getString(R.string.requierd_nom));
        }
        if(TextUtils.isEmpty(passwordS)) {
            password.setError(RegisterActivity.this.getString(R.string.requierd_password));
        }
        if(!TextUtils.isEmpty(telephoneS) && !TextUtils.isEmpty(emailS) && !TextUtils.isEmpty(nomS) && !TextUtils.isEmpty(passwordS))
        {
            boolean verifLongueurPassword = verificationLongueurPassword();
            if(verifLongueurPassword==true)
            {
                boolean verifComposantPassword = verificationComposantPassword();
                if(verifComposantPassword==true)
                {
                    boolean verifNumero = verificationNumero();
                    if(verifNumero==true)
                    {
                        valiny = true;
                    }
                }
            }

        }
        return valiny;
    }

    public boolean verificationComposantPassword()
    {
        final String PASSWORD_PATTERN = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$*%^&+=])(?=\\S+$).{4,}";
        String passwordS = password.getText().toString().trim();
        boolean valiny = false;
        if(!passwordS.matches(PASSWORD_PATTERN))
        {
            Log.e("Password", "Composant password pas ok");
            password.setError(RegisterActivity.this.getString(R.string.low_strength_password));
        }
        else
        {
            Log.e("Password", "Composant password ok");
            valiny = true;
        }
        return valiny;
    }

    public boolean verificationLongueurPassword()
    {
        String passwordS = password.getText().toString().trim();
        boolean valiny = false;
        if(passwordS.length()<8)
        {
            Log.e("Password", "Longueur password pas ok");
            password.setError(RegisterActivity.this.getString(R.string.error_taille_password));
        }
        else
        {
            Log.e("Password", "Longueur password ok");
            valiny = true;
        }
        return valiny;
    }

    public boolean verificationNumero()
    {
        String telephoneS = telephone.getText().toString().trim();
        boolean valiny = false;
        if(telephoneS.length()!=10)
        {
            telephone.setError(RegisterActivity.this.getString(R.string.error_numero_taille_dix));
        }
        if(telephoneS.length()==10)
        {
            if(telephoneS.startsWith("034") || telephoneS.startsWith("033") || telephoneS.startsWith("032") || telephoneS.startsWith("038"))
            {
                Log.e("Numero", "Numero ok");
                valiny = true;
            }
            else
            {
                Log.e("Numero", "Numero pas ok début");
                telephone.setError(RegisterActivity.this.getString(R.string.error_debut_numero));
            }
        }
        return valiny;
    }

    public String passwordEncryption()
    {
        String valiny = "";

        return valiny;
    }

    public void verifSiUserEnAttente(User user)
    {
        firestoreInstance.collection("cashlessUser")
                .whereEqualTo("email", user.getEmail())
                .whereEqualTo("nom",user.getNom())
                .whereEqualTo("numero", user.getNumero())
                .whereEqualTo("password", user.getPassword())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful())
                        {
                            if(!task.getResult().isEmpty())
                            {
                                Map<String, Object> userObject = new HashMap<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.e("USER", document.getId() + " => " + document.getData());
                                    userObject = document.getData();
                                }
                                System.out.println("VERIFICATION USER EN ATTENTE: "+userObject.toString());

                                if(String.valueOf(userObject.get("validation")).equals("false"))
                                {
                                    progressBarRegister.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, RegisterActivity.this.getString(R.string.compte_en_attente), Toast.LENGTH_SHORT).show();
                                    registerButton.setClickable(true);
                                    Log.e("BUTTON", "ACTIVE");
                                }

                            }
                            if(task.getResult().isEmpty())
                            {
                                registerButton.setClickable(true);
                                Log.e("BUTTON", "ACTIVE");
                                progressBarRegister.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, RegisterActivity.this.getString(R.string.phone_number_used), Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            Log.e("USER:", "Error documents: ", task.getException());
                        }
                    }
                });
    }

    public void verifIfPhoneNumberIsUsed(User user)
    {
         firestoreInstance.collection("cashlessUser")
                .whereEqualTo("numero", user.getNumero())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful())
                        {
                            if(!task.getResult().isEmpty())
                            {
                               verifSiUserEnAttente(user);
                            }
                            if(task.getResult().isEmpty())
                            {
                                Toast.makeText(RegisterActivity.this, "NUM MBOLA TSY MIEXISTE",Toast.LENGTH_SHORT).show();
                                try {
                                    registerUser(user);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                progressBarRegister.setVisibility(View.GONE);
                            }
                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            Log.e("USER:", "Error documents: ", task.getException());
                        }
                    }
                });
    }

    public void setValueIntoVariable(int variable, int value)
    {
        variable = value;
    }



    public void registerUser(User user) throws UnsupportedEncodingException, NoSuchAlgorithmException
    {
        // Add a new document with a generated ID
        Encrypt enc = new Encrypt();
        DocumentReference documentWithGeneratedId = firestoreInstance.collection("cashlessUser").document();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", documentWithGeneratedId.getId());
        userMap.put("email", user.getEmail());
        userMap.put("nom", user.getNom());
        userMap.put("numero", user.getNumero());
        userMap.put("password", enc.SHA256(user.getPassword()));
        userMap.put("validation", false);

        documentWithGeneratedId.set(userMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(USER_TAG, "Succes add user");
                Toast.makeText(RegisterActivity.this, RegisterActivity.this.getString(R.string.add_user_success), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, Login.class);
                startActivity(intent);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(USER_TAG, "Failed add user");
                Toast.makeText(RegisterActivity.this, RegisterActivity.this.getString(R.string.add_user_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }
}