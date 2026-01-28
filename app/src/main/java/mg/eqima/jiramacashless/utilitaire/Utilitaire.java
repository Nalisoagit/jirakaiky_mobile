package mg.eqima.jiramacashless.utilitaire;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import mg.eqima.jiramacashless.model.operateur.TarificationOperateur;

public class Utilitaire {
    public int compareTwoArrayList(List<TarificationOperateur> listeFirestore, List<TarificationOperateur> listeSqlite)
    {
        int valiny = 0;
        for(int i=0; i<listeSqlite.size(); i++)
        {
            TarificationOperateur tarifSQLiteCourant = listeSqlite.get(i);
            Log.e("TARIFICATION", "SQLITE COURANT =>"+ tarifSQLiteCourant.getOperateur() +": "+ tarifSQLiteCourant.getTarif());
            for(int j=0; j<listeFirestore.size(); j++)
            {
                TarificationOperateur tarifFirestoreCourant = listeFirestore.get(j);
                Log.e("TARIFICATION", "FIRESTORE COURANT =>"+ tarifFirestoreCourant.getOperateur() +": "+ tarifFirestoreCourant.getTarif());
                if(tarifSQLiteCourant.getId()==tarifFirestoreCourant.getId())
                {
                    if(!tarifSQLiteCourant.getTarif().equals(tarifFirestoreCourant.getTarif()))
                    {
                        valiny = 1;
                        Log.e("TARICATION", "INCOHERENCE DE DONNEES");
                    }
                    else
                    {
                        Log.e("TARICATION", "COHERENCE DE DONNEES");
                    }
                }
            }
        }

        return valiny;
    }

    public String deleteCrochet(String json)
    {
        String valiny = "";
        if(json.startsWith("[") && json.endsWith("]"))
        {
            json = json.replace("[", "");
            valiny = json.replace("]", "");
        }
        return valiny;

    }

    public String encodeUrlBody(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    public String makeDateToMatchDateTimeFormat(String daty)
    {
        String valiny = "";
        if(daty.contains("T"))
        {
            daty = daty.replace("T", " ");
            valiny = daty;
        }
        if(daty.contains("Z"))
        {
            daty = daty.replace("Z", "");
            valiny = daty;
        }
        else
        {
            valiny = daty;
        }
        return valiny;
    }

    public String changeDateFormat(String daty) throws ParseException
    {
        String valiny = "";
        if(daty.contains("/"))
        {
            String formatDateAirtel = "dd/MM/yyyy HH:mm:ss";
            String dateFormatMysql = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(formatDateAirtel);
            Date date = (Date) sdf.parse(daty);

            System.out.println("DATE AIRTEL: "+date.toString());

            sdf.applyPattern(dateFormatMysql);
            valiny = sdf.format(date);

            System.out.println("DATE VALINY: "+valiny);
        }
        else
        {
            valiny = daty;
            System.out.println("DATE VALINY: "+valiny);
        }

        return valiny;
    }

    public Bitmap generateBitmap(String text)
    {
        utilitaire.Utilitaire utilitaire = new utilitaire.Utilitaire();
        String luhn = utilitaire.constructLuhn(text);
        Log.e("LUHN", luhn);
        Bitmap mBitmap = null;
        //initializing MultiFormatWriter for QR code
        MultiFormatWriter mWriter = new MultiFormatWriter();
        try {
            //BitMatrix class to encode entered text and set Width & Height
            BitMatrix mMatrix = mWriter.encode(luhn, BarcodeFormat.QR_CODE, 120,120);
            BarcodeEncoder mEncoder = new BarcodeEncoder();
            mBitmap = mEncoder.createBitmap(mMatrix);//creating bitmap of code
            //imageCode.setImageBitmap(mBitmap);//Setting generated QR code to imageView
            // to hide the keyboard
            //InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            //manager.hideSoftInputFromWindow(etText.getApplicationWindowToken(), 0);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return mBitmap;
    }

    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    // FONCTION RENDRE MONNAIE:
    public int rendreMonnaie(String montantDonnee,String montantADeduire)
    {
        int monnaie          = Integer.valueOf(montantDonnee) - Integer.valueOf(montantADeduire);
        int monnaieFinale    = 0;

        System.out.println("Montant données: "+ montantDonnee);
        System.out.println("Montant à déduire: "+ montantADeduire);
        System.out.println("Monnaie: "+ monnaie);

        String monnaieString = String.valueOf(monnaie);
        int tailleMonnaie    = monnaieString.length();
        System.out.println("Taille string: "+ tailleMonnaie);
        String deuxDernierDigitStr = monnaieString.substring(tailleMonnaie-2);
        System.out.println("Deux dernier digit: "+ deuxDernierDigitStr);
        int deuxDernierDigit = Integer.valueOf(deuxDernierDigitStr);
        if(deuxDernierDigit<50)
        {
            monnaieFinale = monnaie - deuxDernierDigit;
        }
        else if(deuxDernierDigit>=50)
        {
            int rajout = 100 - deuxDernierDigit;
            monnaieFinale = monnaie + rajout;
        }

        return monnaieFinale;
    }

}
