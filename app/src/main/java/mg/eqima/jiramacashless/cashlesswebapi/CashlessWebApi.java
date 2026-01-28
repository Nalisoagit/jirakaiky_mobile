package mg.eqima.jiramacashless.cashlesswebapi;

import android.util.Log;

import mg.eqima.jiramacashless.environnement.Environnement;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import mg.eqima.jiramacashless.model.PaiementFacture;

public class CashlessWebApi {
    private String domain_name;
    private String listePaiementFacture_url = "/api/paiement_facture/getAll";
    private String insertPaiementFacture_url = "/api/paiement_facture/insertPost";

    private Environnement environnement;

    public CashlessWebApi()
    {
        environnement = new Environnement();
        domain_name = environnement.getDomainName();
    }

    public JSONArray getAllPaiementFacture() throws IOException, JSONException {
        String valiny = "";
        String urlCombinaison = domain_name+listePaiementFacture_url;

        URL url = new URL(urlCombinaison);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        /*httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("grant_type=client_credentials&scope=EXT_INT_MVOLA_SCOPE");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();*/



        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        responseStream.close();
        s.close();
        httpConn.disconnect();

        System.out.println("CASHLESS_API: "+response);
        JSONArray listePaiementFactureJSONArray = new JSONArray(response);

        return listePaiementFactureJSONArray;
    }

    public String insertPaiementFacture(PaiementFacture paiementFacture)  throws IOException, JSONException
    {
        String valiny = "";
        String urlCombinaison = domain_name+insertPaiementFacture_url;


        URL url = new URL(urlCombinaison);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("content-type", "application/json");

        httpConn.setDoOutput(true);
        //httpConn.setChunkedStreamingMode(0);
        httpConn.setDoInput(true);

        String paiementFactureJson = paiementFacture.jsonMe();
        System.out.println("PAIEMENT RAW : "+paiementFactureJson);

        byte[] postDataBytes = paiementFactureJson.getBytes("UTF-8");

        try(OutputStream os =  httpConn.getOutputStream())
        {
            os.write(postDataBytes);
            os.flush();
        }
        catch (Exception e)
        {
            Log.e("ERROR OUTPUTSTREAM: ", e.getMessage());
        }

        /*httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("grant_type=client_credentials&scope=EXT_INT_MVOLA_SCOPE");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();*/
        String response = null;

        try{
            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
        }
        catch(Exception e)
        {
            Log.e("MYSQL", e.getMessage());
        }



        System.out.println("REPONSE INSERT: "+response);
        //JSONArray listePaiementFactureJSONArray = new JSONArray(response);

        return response;
    }
}
