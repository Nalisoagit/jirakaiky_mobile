package mg.eqima.jiramacashless.cashlesswebapi;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import mg.eqima.jiramacashless.MainActivity;
import mg.eqima.jiramacashless.R;

public class JiramaRestApi {

    private Environnement environnement;
    private String domain_name;
    private String url_existence_facture = "/jiramacontroller/verifExistenceFacture?referencefacture=";
    private String url_statut_facture = "/jiramacontroller/statutFacture?referencefacture=";
    private String url_montant = "/jiramacontroller/montantFacture?referencefacture=";
    private String url_reglement_facture = "/jiramacontroller/reglementFacture?referencefacture=";
    private String url_info_client = "/jiramacontroller/infoClient?referencefacture=";

    public JiramaRestApi(){
        environnement = new Environnement();
        domain_name = environnement.getDomainName();
    }
    public Map<String, String> verifExistenceFacture(String reference) throws IOException, JSONException {
        URL url = new URL(domain_name+url_existence_facture+reference);
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");

        // LECTURE RESPONSE BODY:
        JSONObject objectEtape = null;
        Map<String, String> valinyFarany = null;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "utf-8")))
        {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while((responseLine = br.readLine()) != null)
            {
                response.append(responseLine.trim());
            }
            httpURLConnection.getInputStream().close();
            br.close();
            System.out.println(response.toString());
            System.out.println("VALINY VOALOHANY: "+response.toString());
            objectEtape = new JSONObject(response.toString());
        }
        finally {
            httpURLConnection.disconnect();
        }
        String valiny = objectEtape.get("fs_DATABROWSE_F55INV").toString();
        valinyFarany = traitementExistenceFacture(valiny);

        return valinyFarany;
    }

    // VERSION WITH VOLLEY:
    public void verifExistenceFactureVolley(ProgressDialog progressDialog, Context context, RequestQueue requestQueue, StringRequest stringRequest, String referenceFacture)
    {
        //String url = domain_name+url_existence_facture+referenceFacture;
        String url = "https://cashless.eqima.org/api/paiement_facture/getAll2";
        //requestQueue = Volley.newRequestQueue(context);
        stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //progressDialog.dismiss();
                //try {
                    //JSONObject reponsePureJSON = new JSONObject(response);
                    //String valiny = reponsePureJSON.get("fs_DATABROWSE_F55INV").toString();
                    //Map<String, String> valinyFarany = traitementExistenceFacture(valiny);
                    //Log.e("EXISTENCE", "VALINY MAP: "+ valinyFarany);
                    //Toast.makeText(context, "Facture existe: \n"+ valinyFarany.get("exist")+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();

                    /*if(valinyFarany.get("exist").equals("true"))
                    {
                        Toast.makeText(context, "Facture existante", Toast.LENGTH_SHORT).show();
                        //SI FACTURE EXISTE:
                        //StringRequest stringRequest1 = null;
                        //statutFactureVolley(context, requestQueue, stringRequest1, referenceFacture);
                    }
                    else
                    {
                        // SI FACTURE N\'EXISTE PAS:
                        Toast.makeText(context, "Facture n\'existe pas!", Toast.LENGTH_SHORT).show();
                    }*/

                //} catch (JSONException e) {
                    //e.printStackTrace();
                //}
                //progressDialog.dismiss();
                Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, context.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        requestQueue.add(stringRequest);

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(context.getString(R.string.analyse_scan_title));
        progressDialog.setMessage(context.getString(R.string.analyse_scan_message)+" "+referenceFacture);
        progressDialog.setCancelable(false);
        progressDialog.setIcon(R.drawable.search_ico);
        progressDialog.show();

    }

    public Map<String, String> traitementExistenceFacture(String resultatRequeteS) throws JSONException
    {
        Map<String, String> valinyFarany = new HashMap();
        JSONObject resultatRequete = new JSONObject(resultatRequeteS);
        String errors = resultatRequete.get("errors").toString();
        valinyFarany.put("errors", "");
        valinyFarany.put("exist", "false");
        valinyFarany.put("reference", "");
        boolean verificationError = errorVerificationVide(errors);
        if(!verificationError)
        {
            JSONObject dataJson = new JSONObject(resultatRequete.get("data").toString());
            System.out.println("DATA JSON: "+dataJson.toString());
            JSONObject gridData = new JSONObject(dataJson.get("gridData").toString());
            System.out.println("GRIDDATA JSON: "+gridData.toString());
            JSONArray rowset = new JSONArray(gridData.get("rowset").toString());
            System.out.println("Taille rowset: "+rowset.length());
            if(rowset.length()>0)
            {
                String factureReference = rowset.getJSONObject(0).get("F55INV_55FACREF").toString(); //CONFIRMATION EXISTENCE
                System.out.println("Facture reference: "+factureReference);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("exist", "true");
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("reference", factureReference);
                }
            }

        }
        return valinyFarany;
    }

    public String statutFacture(String reference) throws IOException, JSONException {
        URL url = new URL(domain_name+url_statut_facture+reference);
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");

        // LECTURE RESPONSE BODY:
        String valiny = null;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "utf-8")))
        {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while((responseLine = br.readLine()) != null)
            {
                response.append(responseLine.trim());
            }
            httpURLConnection.getInputStream().close();
            br.close();
            System.out.println(response.toString());
            valiny = response.toString();
        }
        finally {
            httpURLConnection.disconnect();
        }
        String valinyFarany = traitementStatutFacture(valiny);
        return valinyFarany;
    }

    public void statutFactureVolley(Context context, RequestQueue requestQueue, StringRequest stringRequest, String referenceFacture)
    {
        String url = domain_name+url_statut_facture+referenceFacture;
        //requestQueue = Volley.newRequestQueue(context);
        stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    String valinyFarany = traitementStatutFacture(response);
                    Log.e("STATUT", "REPONSE: "+ valinyFarany);
                    //Toast.makeText(context, "Facture payée: \n"+ valinyFarany+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();
                    if(valinyFarany.equals("Y"))
                    {
                        Toast.makeText(context, "Facture payée", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(context, "Facture pas encore payée", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, context.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        requestQueue.add(stringRequest);
    }

    public String traitementStatutFacture(String reponseRequeteString) throws JSONException
    {
        JSONObject reponseRequete = new JSONObject(reponseRequeteString);
        Map<String, String> valinyFarany = new HashMap();
        JSONObject ds_F55INV = new JSONObject(reponseRequete.get("ds_F55INV").toString());
        System.out.println("ds_F55INV: "+ds_F55INV);
        JSONArray output = new JSONArray(ds_F55INV.get("output").toString());
        System.out.println("output: "+output);
        String payer = "";
        if(output.length()>0)
        {
            JSONObject conteneurStatut = output.getJSONObject(0);
            System.out.println("Conteneur statut: "+conteneurStatut);
            JSONObject groupBy = new JSONObject(conteneurStatut.get("groupBy").toString());
            payer = groupBy.get("f55inv.55trinvp").toString();
            System.out.println("Payer: "+payer);
        }

        return payer;
    }

    public Map<String, String> traitementInfoClient(String resultatRequeteS, String factureReference) throws JSONException
    {
        Map<String, String> valinyFarany = new HashMap();
        JSONObject resultatRequeteSJSon = new JSONObject(resultatRequeteS);
        JSONObject resultatRequete = new JSONObject(resultatRequeteSJSon.get("fs_DATABROWSE_V55WS1A").toString());
        String errors = resultatRequete.get("errors").toString();
        valinyFarany.put("errors", "");
        valinyFarany.put("exist", "false");
        valinyFarany.put("reference", "");
        valinyFarany.put("numero_client", "");
        valinyFarany.put("nom_client", "");
        valinyFarany.put("adresse_client", "");
        boolean verificationError = errorVerificationVide(errors);
        if(!verificationError)
        {
            JSONObject dataJson = new JSONObject(resultatRequete.get("data").toString());
            System.out.println("DATA JSON: "+dataJson.toString());
            JSONObject gridData = new JSONObject(dataJson.get("gridData").toString());
            System.out.println("GRIDDATA JSON: "+gridData.toString());
            JSONArray rowset = new JSONArray(gridData.get("rowset").toString());
            System.out.println("Taille rowset: "+rowset.length());
            if(rowset.length()>0)
            {
                String numero_client = rowset.getJSONObject(0).get("F55INV_AN81").toString();
                String nom_client = rowset.getJSONObject(0).get("F0101_ALPH").toString(); //CONFIRMATION EXISTENCE
                String adresse_client = rowset.getJSONObject(0).get("F0116_ADD1").toString();
                System.out.println("Numero client: "+numero_client);
                System.out.println("Nom client: "+nom_client);
                System.out.println("Adresse client: "+adresse_client);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("exist", "true");
                    valinyFarany.replace("reference", factureReference);
                    valinyFarany.replace("numero_client", numero_client);
                    valinyFarany.replace("nom_client", nom_client);
                    valinyFarany.replace("adresse_client", adresse_client);
                }
            }

        }

        return valinyFarany;
    }

    public Map<String, String> traitementInfoClient2(String resultatRequeteS, String factureReference) throws JSONException
    {
        Map<String, String> valinyFarany = new HashMap();
        JSONObject resultatRequeteSJSon = new JSONObject(resultatRequeteS);
        JSONObject resultatRequete = new JSONObject(resultatRequeteSJSon.get("fs_DATABROWSE_V55WS1A").toString());
        String errors = resultatRequete.get("errors").toString();
        valinyFarany.put("errors", "");
        valinyFarany.put("exist", "false");
        valinyFarany.put("reference", "");
        valinyFarany.put("numero_client", "");
        valinyFarany.put("nom_client", "");
        valinyFarany.put("adresse_client", "");
        valinyFarany.put("reference_client", "");
        boolean verificationError = errorVerificationVide(errors);
        if(!verificationError)
        {
            JSONObject dataJson = new JSONObject(resultatRequete.get("data").toString());
            System.out.println("DATA JSON: "+dataJson.toString());
            JSONObject gridData = new JSONObject(dataJson.get("gridData").toString());
            System.out.println("GRIDDATA JSON: "+gridData.toString());
            JSONArray rowset = new JSONArray(gridData.get("rowset").toString());
            System.out.println("Taille rowset: "+rowset.length());
            if(rowset.length()>0)
            {
                String numero_client = rowset.getJSONObject(0).get("F55INV_AN81").toString();
                String nom_client = rowset.getJSONObject(0).get("F0101_ALPH").toString(); //CONFIRMATION EXISTENCE
                String adresse_client = rowset.getJSONObject(0).get("F0116_ADD1").toString();
                String reference_client = rowset.getJSONObject(0).get("F55INV_PROFCU17").toString();
                System.out.println("Numero client: "+numero_client);
                System.out.println("Nom client: "+nom_client);
                System.out.println("Adresse client: "+adresse_client);
                System.out.println("Référence client: "+ reference_client);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("exist", "true");
                    valinyFarany.replace("reference", factureReference);
                    valinyFarany.replace("numero_client", numero_client);
                    valinyFarany.replace("nom_client", nom_client);
                    valinyFarany.replace("adresse_client", adresse_client);
                    valinyFarany.replace("reference_client", reference_client);
                }
            }

        }

        return valinyFarany;
    }


    public Map<String, String> montantFacture(String reference) throws IOException, JSONException {
        URL url = new URL(domain_name+url_montant+reference);
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        System.out.println("MONTAAAAAANT: ");
        // LECTURE RESPONSE BODY:
        String valiny = null;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "utf-8")))
        {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while((responseLine = br.readLine()) != null)
            {
                response.append(responseLine.trim());
            }
            httpURLConnection.getInputStream().close();
            br.close();
            System.out.println(response.toString());
            valiny = response.toString();
        }
        finally {
            httpURLConnection.disconnect();
        }

        Map<String, String> valinyFarany = traitementPaiementNonFinal(valiny);
        return valinyFarany;
    }

    public void montantFactureVolley(Context context, RequestQueue requestQueue, StringRequest stringRequest, String referenceFacture)
    {
        String url = domain_name+url_montant+referenceFacture;
        requestQueue = Volley.newRequestQueue(context);
        stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Map<String, String> valinyFarany = traitementPaiementNonFinal(response);
                    Log.e("MONTANT", "REPONSE: "+ valinyFarany);
                    Toast.makeText(context, "Montant facture: \n"+ valinyFarany.get("montant")+"\n\n\n\n\n", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Log.e("VOLLEY", "REPONSE: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, context.getString(R.string.error)+error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("VOLLEY", "ERREUR: "+ error.toString());
            }
        });

        requestQueue.add(stringRequest);
    }

    public Map<String, String> traitementPaiementNonFinal(String reponseRequete) throws JSONException
    {
        Map<String, String> valinyFarany = new HashMap();
        valinyFarany.put("errors","");
        valinyFarany.put("montant","");
        JSONObject reponseRequeteJson = new JSONObject(reponseRequete);
        JSONObject fs_P55PAYFA_W55PAYFAA = new JSONObject(reponseRequeteJson.get("fs_P55PAYFA_W55PAYFAA").toString());
        JSONArray errors = new JSONArray(fs_P55PAYFA_W55PAYFAA.getString("errors"));
        boolean verificationError = errorVerificationVide(errors.toString());
        if(!verificationError)
        {
            JSONObject data = new JSONObject(fs_P55PAYFA_W55PAYFAA.getString("data"));
            System.out.println("DATA: "+ data);
            JSONObject txtMontant_28 = new JSONObject(data.getString("txtMontant_28"));
            System.out.println("txtMontant_28: "+ txtMontant_28);
            String montant = txtMontant_28.getString("internalValue");
            System.out.println("Montant: "+ montant);
            int arrondi = arrondisseurMontant(montant);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                valinyFarany.replace("montant", String.valueOf(arrondi));
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                valinyFarany.replace("errors",errors.toString());
            }
        }
        return valinyFarany;
    }

    public Map<String, String> reglerFacture(String reference, String controlid, String transid, String operationid, String operateur) throws IOException, JSONException {
        Map<String, String> valinyFarany = new HashMap<>();
        URL url = new URL(domain_name+url_reglement_facture+reference+"&controlid="+controlid+"&transid="+transid+"&operationid="+operationid+"&operateur="+operateur);
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");

        // LECTURE RESPONSE BODY:
        String valiny = null;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "utf-8")))
        {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while((responseLine = br.readLine()) != null)
            {
                response.append(responseLine.trim());
            }
            //httpURLConnection.getInputStream().close();
            br.close();
            System.out.println(response.toString());
            valiny = response.toString();
        }
        httpURLConnection.disconnect();
        valinyFarany = traitementPaiementFinal(valiny);
        return valinyFarany;
    }

    public HashMap<String, String> traitementGetMontant_WS14(String valinyReq) throws JSONException
    {
        HashMap<String, String> valiny = new HashMap();
        Log.e("MONTANT", valinyReq);
        JSONObject responseJSON = new JSONObject(valinyReq);
        JSONObject ds_F55INV_JSON = new JSONObject(responseJSON.getString("ds_F55INV"));
        JSONArray output_JSON = new JSONArray(ds_F55INV_JSON.getString("output"));
        JSONObject indZero = new JSONObject(output_JSON.get(0).toString());
        String montantInit = indZero.getString("F55INV.55TRINVA_SUM");
        String alreadyPaid = indZero.getString("F55INV.DECTAMNT_SUM");
        String mois = indZero.getString("F55INV.PN_AVG");
        String annee = indZero.getString("F55INV.CFY_AVG");
        String categorie = indZero.getString("F55INV.PROFCO8_AVG");

        Log.e("CATEGORIE", categorie);

        double montantInitD = Double.valueOf(montantInit);
        double alreadyPaidD = Double.valueOf(alreadyPaid);

        double aPayer = montantInitD - alreadyPaidD;

        int aPayerInt = arrondisseurMontant(String.valueOf(aPayer));

        valiny.put("Mois",getMonthLetter(mois));
        valiny.put("Annee",annee);
        valiny.put("Montant",String.valueOf(aPayerInt));
        valiny.put("Categorie",categorie);

        return valiny;
    }

    public Map<String, String> traitementGetCodeRecu(String reponseRequete) throws JSONException {
        Map<String, String> valinyFarany = new HashMap();
        valinyFarany.put("recucode","");
        JSONObject reponseRequeteJson = new JSONObject(reponseRequete);
        if(reponseRequeteJson.has("ds_V55INVCA"))
        {
            valinyFarany.put("errors", "N");
            JSONObject ds_V55INVCA = new JSONObject(reponseRequeteJson.getString("ds_V55INVCA"));
            JSONArray output       = new JSONArray(ds_V55INVCA.getString("output"));
            if(output.length()>0)
            {
                String dataString   = output.get(0).toString();
                JSONObject dataJson = new JSONObject(dataString);
                JSONObject groupBy  = new JSONObject(dataJson.getString("groupBy"));
                String code_recu = groupBy.getString("F55CSSRC.PYID");
                Log.e("CODE_RECU_GET", code_recu);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("recucode", code_recu);
                }
            }
        }
        else if(!reponseRequeteJson.has("ds_V55INVCA"))
        {
            valinyFarany.put("errors", "Y");
        }

        return valinyFarany;
    }

    public Map<String, String> traitementPaiementFinal(String reponseRequete) throws JSONException
    {
        Map<String, String> valinyFarany = new HashMap();
        valinyFarany.put("errors","");
        valinyFarany.put("recucode","");
        JSONObject reponseRequeteJson = new JSONObject(reponseRequete);
        JSONObject fs_P55PAYFA_W55PAYFAA = new JSONObject(reponseRequeteJson.get("fs_P55PAYFA_W55PAYFAA").toString());
        JSONArray errors = new JSONArray(fs_P55PAYFA_W55PAYFAA.getString("errors"));
        boolean verificationError = errorVerificationVide(errors.toString());
        if(!verificationError)
        {
            JSONObject data = new JSONObject(fs_P55PAYFA_W55PAYFAA.getString("data"));
            System.out.println("DATA: "+ data);
            JSONObject txtMontant_28 = new JSONObject(data.getString("txtMontant_28"));
            System.out.println("txtMontant_28: "+ txtMontant_28);
            String montant = txtMontant_28.getString("internalValue");
            System.out.println("Montant: "+ montant);

            JSONObject txtRecuCode_31 = new JSONObject(data.getString("txtRecuCode_31"));
            System.out.println("txtRecuCode_31: "+ txtRecuCode_31.toString());
            String recuCode = txtRecuCode_31.getString("value");
            System.out.println("recu: "+ recuCode);
            if(!recuCode.equals("0"))
            {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    valinyFarany.replace("recucode",recuCode);
                }
            }

        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                valinyFarany.replace("errors",errors.toString());
            }
        }
        return valinyFarany;
    }

    public void reglerFactureVolley(String reference, String controlid, String transid, String operationid)
    {

    }

    public boolean errorVerificationVide(String errors)
    {
        boolean valiny = false;
        if(!errors.equals("[]"))
        {
            valiny = true;
        }
        return valiny;
    }

    public int arrondisseurMontant(String montantS)
    {
        double montant = Double.valueOf(montantS);
        int intForm1 = (int)(montant);
        double doubleForm1 = Double.parseDouble(String.valueOf(intForm1));
        double difference = montant-doubleForm1;
        double valinyEtape1=Double.parseDouble(String.valueOf(intForm1));
        if(difference>0.0){
            valinyEtape1=valinyEtape1+1;
        }
        System.out.println(valinyEtape1);

        int valinyFarany = (int) valinyEtape1;
        return valinyFarany;
    }

    public String getMonthLetter(String moisInt)
    {
        Map<String, String> moisDeLannee = new HashMap();
        moisDeLannee.put("1", "Janvier");
        moisDeLannee.put("2", "Fevrier");
        moisDeLannee.put("3", "Mars");
        moisDeLannee.put("4", "Avril");
        moisDeLannee.put("5", "Mai");
        moisDeLannee.put("6", "Juin");
        moisDeLannee.put("7", "Juillet");
        moisDeLannee.put("8", "Aout");
        moisDeLannee.put("9", "Septembre");
        moisDeLannee.put("10", "Octobre");
        moisDeLannee.put("11", "Novembre");
        moisDeLannee.put("12", "Decembre");

        String valiny = moisDeLannee.get(moisInt);
        return valiny;
    }



}
