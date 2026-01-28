package mg.eqima.jiramacashless.AsyncPrintRecu;


import android.os.AsyncTask;

import mg.eqima.jiramacashless.recu.Recu;

public class AsyncPrintRecu extends AsyncTask<Object, Void, String> {

    @Override
    protected String doInBackground(Object... objects) {
        String requestDateForPr = (String) objects[0];
        String refFactureBundle = (String) objects[1];
        String amountForPr = (String) objects[2];
        String numberForPr = (String) objects[3];
        String fraisForPr = (String) objects[4];
        String operateurForPr = (String) objects[5];
        String transactionReferenceForPr = (String) objects[6];

        //Recu recu = new Recu(requestDateForPr, refFactureBundle, amountForPr, numberForPr,fraisForPr, operateurForPr, transactionReferenceForPr);
        return null;
    }


}
