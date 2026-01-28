package mg.eqima.jiramacashless.cashlesswebapi;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

public class AsynReglerFacture extends AsyncTask<Object, Void, Map<String, String>> {
    @Override
    protected Map<String, String> doInBackground(Object... objects) {
        String referenceFacture = (String) objects[0];
        String controlid = (String) objects[1];
        String transid = (String) objects[2];
        String operationid = (String) objects[3];
        String operateur = (String) objects[4];
        Map<String, String> mapFarany = null;
        JiramaRestApi jiramaRestApi = new JiramaRestApi();
        try {
                mapFarany = jiramaRestApi.reglerFacture(referenceFacture,controlid, transid, operationid, operateur);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mapFarany;

    }
}
