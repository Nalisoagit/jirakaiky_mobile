package mg.eqima.jiramacashless.cashlesswebapi;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

public class AsyncApiJirama extends AsyncTask<String, Void, Map<String, String>> {
    @Override
    protected Map<String, String> doInBackground(String... strings) {
        String referenceFacture = strings[0];
        JiramaRestApi jiramaRestApi = new JiramaRestApi();
        Map<String, String> valiny = null;
        Map<String, String> verificationExistenceFacture = null;
        try{
            verificationExistenceFacture = jiramaRestApi.verifExistenceFacture(referenceFacture);
            if(verificationExistenceFacture.get("exist").equals("true"))
            {
                String statut = "";
                try {
                    statut = jiramaRestApi.statutFacture(referenceFacture);
                    verificationExistenceFacture.put("statut", statut);
                    if(statut.equals("Y"))
                    {
                        valiny = verificationExistenceFacture;
                    }
                    if(statut.equals("N"))
                    {
                        Map<String, String> montantMap = jiramaRestApi.montantFacture(referenceFacture);
                        verificationExistenceFacture.put("montant", montantMap.get("montant"));
                        valiny = verificationExistenceFacture;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                valiny = verificationExistenceFacture;
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.e("FACTURE", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FACTURE", e.getMessage());
        }

        return valiny;
    }
}
