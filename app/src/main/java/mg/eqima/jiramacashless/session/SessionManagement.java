package mg.eqima.jiramacashless.session;

import android.content.Context;
import android.content.SharedPreferences;

import mg.eqima.jiramacashless.user.User;

public class SessionManagement {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String SHARED_PREF_NAME      = "session";
    String SESSION_KEY_ID        = "session_user_id";
    String SESSION_KEY_NUMERO    = "session_user_numero";
    String SESSION_KEY_SIGNATURE = "session_user_signature";
    String SESSION_KEY_CREDIT    = "session_user_nb_credit";
    String SESSION_KEY_NOM       = "session_user_nom";
    String SESSION_KEY_ID_DIST   = "session_id_dist";
    String SESSION_KEY_SEUILLE   = "session_seuille";

    public SessionManagement(Context context)
    {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveSession(User user)
    {
        //Enregistrement de la session de l'utilisateur:
        String userId = user.getId();
        editor.putString(SESSION_KEY_ID, userId).commit();
        editor.putString(SESSION_KEY_NUMERO, user.getNumero()).commit();
        editor.putString(SESSION_KEY_SIGNATURE, user.getSignature()).commit();
        editor.putString(SESSION_KEY_CREDIT, user.getCredit()).commit();
        editor.putString(SESSION_KEY_NOM, user.getNom()).commit();
        editor.putString(SESSION_KEY_ID_DIST, user.getIdDistributeur()).commit();
        editor.putFloat(SESSION_KEY_SEUILLE, 10000);
    }

    public String[] getSession()
    {
        //retourne l'id de l'user connecter:
        String[] valiny = new String[7];
        valiny[0] = sharedPreferences.getString(SESSION_KEY_ID, "-1");
        valiny[1] = sharedPreferences.getString(SESSION_KEY_NUMERO, "");
        valiny[2] = sharedPreferences.getString(SESSION_KEY_SIGNATURE, "");
        valiny[3] = sharedPreferences.getString(SESSION_KEY_CREDIT, "");
        valiny[4] = sharedPreferences.getString(SESSION_KEY_NOM, "");
        valiny[5] = sharedPreferences.getString(SESSION_KEY_ID_DIST, "");
        valiny[6] = sharedPreferences.getString(SESSION_KEY_SEUILLE, "10000");
        return valiny;
    }

    public String getNumeroUserConnecter()
    {
        String valiny = sharedPreferences.getString(SESSION_KEY_NUMERO, "");
        return valiny;
    }

    public void removeSession()
    {
        editor.putString(SESSION_KEY_ID, "-1").commit();
        editor.putString(SESSION_KEY_NUMERO, "").commit();
        editor.putString(SESSION_KEY_SIGNATURE, "").commit();
        editor.putString(SESSION_KEY_CREDIT, "").commit();
        editor.putString(SESSION_KEY_NOM, "").commit();
        editor.putString(SESSION_KEY_ID_DIST, "").commit();
    }

    // UPDATE DIRECT NB CREDIT:
    public void updateNbCreditUser(double nbCreditBase)
    {
        editor.putString(SESSION_KEY_CREDIT, String.valueOf(nbCreditBase)).commit();
    }

    // GET NB CREDIT:
    public String getNbCredit()
    {
        String valiny = sharedPreferences.getString(SESSION_KEY_CREDIT, "0");
        return valiny;
    }

    // GET SEUILLE:
    public double getSeuille()
    {
        double valiny = sharedPreferences.getFloat(SESSION_KEY_SEUILLE, 10000);
        return valiny;
    }


    // DIMINUTION NB CREDIT:
    public void decreaseNbCredit()
    {
        String nbCreditInitiale = sharedPreferences.getString(SESSION_KEY_CREDIT, "");
        double nbCreditFinal    = Double.valueOf(nbCreditInitiale) - 1;
        editor.putString(SESSION_KEY_CREDIT, String.valueOf(nbCreditFinal)).commit();
    }

    public void decreaseNbCreditHalf()
    {
        String nbCreditInitiale = sharedPreferences.getString(SESSION_KEY_CREDIT, "");
        double nbCreditFinal    = Double.valueOf(nbCreditInitiale) - 0.5;
        editor.putString(SESSION_KEY_CREDIT, String.valueOf(nbCreditFinal)).commit();
    }


}
