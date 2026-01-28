package mg.eqima.jiramacashless.cashlesswebapi;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.IOException;

import mg.eqima.jiramacashless.model.PaiementFacture;

public class AsyncInsertCashlessWebApi extends AsyncTask<Object, Void, String> {
    @Override
    protected String doInBackground(Object... objects) {
        PaiementFacture paiementFacture = (PaiementFacture) objects[0];
        CashlessWebApi cashlessWebApi = new CashlessWebApi();
        String valiny = null;
        try {
            valiny = cashlessWebApi.insertPaiementFacture(paiementFacture);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return valiny;
    }
}
