package mg.eqima.jiramacashless.apimvola;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class AsyncTransactionDetail extends AsyncTask<Object, Void, JSONObject> {


    @Override
    protected JSONObject doInBackground(Object... objects) {
        //MAKA Authentication:
        Authentication authentication= new Authentication();
        String token = "";
        try {
            token = authentication.getToken2();
            Log.e("Token:", token);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String transId = (String) objects[0];
        JSONObject valinyJson = null;
        MerchantApi merchantApi = new MerchantApi(token);
        try {
            valinyJson = merchantApi.getTransactionDetail(transId);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return valinyJson;
    }
}
