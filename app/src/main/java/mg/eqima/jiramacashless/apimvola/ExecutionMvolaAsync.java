package mg.eqima.jiramacashless.apimvola;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONException;

import java.io.IOException;
import java.text.CollationElementIterator;
import java.util.HashMap;
import java.util.Map;

import mg.eqima.jiramacashless.ActivityTest;
import mg.eqima.jiramacashless.Payement;
import mg.eqima.jiramacashless.R;


public class ExecutionMvolaAsync extends AsyncTask<Object, Integer, Map<String, String>> {


    Context myContext;
    Payement actPayement;

    //ILAINA ATO:
    String statusVerification = "";
    String serverCorrelationId = "";
    String objectReferenceVerification = "";
    Map<String , String> valinyFarany = new HashMap<>();

    public ExecutionMvolaAsync()
    {

    }


    @Override
    protected Map<String, String> doInBackground(Object... objects) {

        //TEMPS DE SOMMEIL DE THREAD
        //RECUPERATION NUMERO ET MONTANT
        int montant = (int) objects[0];
        String montantS = String.valueOf(montant);
        String numero = (String) objects[1];
        Log.d("TAG", "Numero: "+numero);
        Log.d("TAG", "Montant: "+montant);

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

        //DEMARRAGE TRANSACTION
        MerchantApi merchantApi = new MerchantApi(token);
        Log.e("Token_Api", merchantApi.getToken());

        try {
            serverCorrelationId = merchantApi.initiateTransaction(numero, montantS);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }



        Log.e("GETSTATUS: ","COMMENCEMENT...");
        Map<String, String> transactionStatus = null;
        try {
            transactionStatus = merchantApi.getTransactionStatus(serverCorrelationId);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        statusVerification = transactionStatus.get("status");
        objectReferenceVerification = transactionStatus.get("objectReference");

        int compteur = 0;
        int limite = 2;
        while(compteur<limite)
        {
            publishProgress(compteur);
            if(statusVerification.equals("pending"))
            {
                Map<String, String> statusTransactionTemp = null;
                try {
                    statusTransactionTemp = merchantApi.getTransactionStatus(serverCorrelationId);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                statusVerification = statusTransactionTemp.get("status");
                objectReferenceVerification = statusTransactionTemp.get("objectReference");

                System.out.println("Status: "+statusTransactionTemp.get("status"));
                System.out.println(compteur);
                compteur++;
                limite++;

            }
            if(!statusVerification.equals("pending")){
                limite = 10;
                compteur = 10;
            }

        }

        //MISE DES DERNIERE REPONSE:
        valinyFarany.put("status",statusVerification);
        valinyFarany.put("objectReference",objectReferenceVerification);
        //progressDialog.dismiss();

        return valinyFarany;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();


    }
    @Override
    protected void onPostExecute(Map<String, String> result) {
        // execution of result of Long time consuming operation
        super.onPostExecute(result);
        //valinyFarany=result;
        // progressDialog.dismiss();
    }
    @Override
    protected void onProgressUpdate(Integer... values) {
        //payementTextView.setText("Transaction en Cours..."+ values[0]);
        //ProgressBar progressBarTransaction = actPayement.findViewById(R.id.progressPaiement);
        //progressBarTransaction.setVisibility(View.VISIBLE);
    }


}