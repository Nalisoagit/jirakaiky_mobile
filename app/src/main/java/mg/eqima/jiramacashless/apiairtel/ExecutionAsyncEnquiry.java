package mg.eqima.jiramacashless.apiairtel;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import mg.eqima.jiramacashless.recu.Recu;

public class ExecutionAsyncEnquiry extends AsyncTask<Object, Void, Map<String, String>> {

    @Override
    protected Map<String, String> doInBackground(Object... objects) {
        double montant = (double) objects[0];
        String numero = (String) objects[1];
        Map<String, String> valiny = new HashMap<>();
        CollectionApi collectionApi = new CollectionApi();
        try {
            valiny = collectionApi.makeReclamation(montant, numero);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return valiny;
    }
}
