package mg.eqima.jiramacashless.cashlesswebapi;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class AsyncCashlessWebApi extends AsyncTask<Void, Void , JSONArray> {

    @Override
    protected JSONArray doInBackground(Void... voids) {
        CashlessWebApi cashlessWebApi = new CashlessWebApi();
        JSONArray listePaiementFacture = null;
        try {
            listePaiementFacture = cashlessWebApi.getAllPaiementFacture();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listePaiementFacture;

    }
}
