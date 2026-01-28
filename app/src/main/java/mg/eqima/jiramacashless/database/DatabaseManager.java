package mg.eqima.jiramacashless.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.eqima.jiramacashless.dataobject.PaiementFactureData;
import mg.eqima.jiramacashless.model.PaiementFacture;
import mg.eqima.jiramacashless.model.operateur.TarificationOperateur;
import mg.eqima.jiramacashless.recu.Recu;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cashless.db";
    private static final int DATABASE_VERSION = 2;
    private FirebaseFirestore firestoreDB;
    private Context myContext;


    public DatabaseManager(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        firestoreDB = FirebaseFirestore.getInstance();
        myContext   = context;
    }

    public void setInstanceFirestore(FirebaseFirestore firestore)
    {
        this.firestoreDB = firestore;
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createSqlPaiementFacture = "CREATE TABLE paiement_facture("
                        + "     id INTEGER primary key AUTOINCREMENT,"
                        + "     daty TEXT,"
                        + "     frais TEXT,"
                        + "     latitude TEXT,"
                        + "     longitude TEXT,"
                        + "     montantFacture TEXT,"
                        + "     numeroPayeur TEXT,"
                        + "     numeroCashpoint TEXT,"
                        + "     operateur TEXT,"
                        + "     refTransaction TEXT,"
                        + "     refFacture TEXT,"
                        + "     totale TEXT,"
                        + "     synchronisation TEXT,"
                        + "     codeRecu TEXT,"
                        + "     refClient TEXT,"
                        + "     nomClient TEXT,"
                        + "     adresseClient TEXT,"
                        + "     idInterne TEXT,"
                        + "     mois TEXT,"
                        + "     annee TEXT,"
                        + "     etat TEXT,"
                        + "     anterieur TEXT,"
                        + "     caissier TEXT,"
                        + "     idInterneReel TEXT,"
                        + "     serverCorrelationId TEXT,"
                        + "     merchantNumber TEXT,"
                        + "     idDistributeur TEXT,"
                        + "     duplicata INTEGER DEFAULT 0"
                        + ")";

        sqLiteDatabase.execSQL(createSqlPaiementFacture);
        createTableTarification(sqLiteDatabase);
        Log.i("SQLITEDATABASE", "onCreate a été invoquer");


    }

    public void updateTableWithMerchantNumber()
    {
        String updateTableScript = "ALTER TABLE paiement_facture ADD COLUMN merchantNumber TEXT";
        this.getWritableDatabase().execSQL(updateTableScript);
        Log.i("SQLITEDATABASE", "MerchantNumber invoqué");

    }

    public void insertBase(PaiementFacture paiementFactureBase, String idI)
    {
        String insertSql = "INSERT INTO paiement_facture(daty, frais, latitude, longitude, montantFacture, numeroPayeur, numeroCashpoint, operateur, refFacture, totale, synchronisation, refClient,nomClient, adresseClient, mois, annee, caissier, etat, idInterne, idInterneReel, idDistributeur) VALUES('"
                + paiementFactureBase.getDatePaiement() + "','"
                + paiementFactureBase.getFrais() + "','"
                + paiementFactureBase.getLatitude() + "','"
                + paiementFactureBase.getLongitude() + "','"
                + paiementFactureBase.getMontantFacture() + "','"
                + paiementFactureBase.getNumeroPayeur() + "','"
                + paiementFactureBase.getNumeroCashpoint() + "','"
                + paiementFactureBase.getOperateur() + "','"
                + paiementFactureBase.getRefFacture() + "','"
                + paiementFactureBase.getTotal() +"','"
                + "true','"
                + paiementFactureBase.getRefClient() + "','"
                + paiementFactureBase.getNomClient().replace("'", "''") + "','"
                + paiementFactureBase.getAdresseClient().replace("'", "''") + "','"
                + paiementFactureBase.getMois() + "','"
                + paiementFactureBase.getAnnee() + "','"
                + paiementFactureBase.getCaissier() + "','"
                + paiementFactureBase.getEtat() + "','"
                +  idI + "','"
                + paiementFactureBase.getId() + "','"
                + paiementFactureBase.getIdDistributeur() +"')";

        this.getWritableDatabase().execSQL(insertSql);

        Toast.makeText(myContext, "SQLite ok", Toast.LENGTH_SHORT).show();
        Log.e("SQLITEDATABASE", "insertBase invoqué");
    }

    public void insertIntoDatabase(Recu recu, String daty, String latitude, String longitude) {
        String insertSql = "INSERT INTO paiement_facture(daty, frais, latitude, longitude, montantFacture, numeroPayeur, numeroCashpoint, operateur, refTransaction, refFacture, totale, synchronisation, refClient,nomClient, adresseClient, mois, annee, caissier) VALUES('"
                + daty + "','"
                + recu.getFrais() + "','"
                + latitude + "','"
                + longitude + "','"
                + recu.getMontantFacture() + "','"
                + recu.getNumero() + "','"
                + recu.getNumeroCashpoint() + "','"
                + recu.getOperateur() + "','"
                + recu.getRefTransaction() +"','"
                + recu.getRefFacture() + "','"
                + recu.getMontantTotale() +"','"
                + "true','"
                + recu.getReferenceClient() + "','"
                + recu.getNomClient() + "','"
                + recu.getAdresseClient() + "','"
                + recu.getMois() + "','"
                + recu.getAnnee() + "','"
                + recu.getCaissier() +"')";

        this.getWritableDatabase().execSQL(insertSql);

        Log.i("SQLITEDATABASE", "insertIntoDatabase invoqué");
        // ← on enlève complètement le Toast ici
    }

    public void setRefTransaction(String refTransaction, String id)
    {
        String sqlUpdate = "UPDATE paiement_facture set refTransaction='"+refTransaction+"' WHERE idInterneReel='"+id+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Toast.makeText(myContext, "Ref transaction: "+refTransaction, Toast.LENGTH_SHORT).show();
        Log.e("SQLITEDATABASE", "Enregistrement ref transaction: "+refTransaction+" pour id interne N°: "+id);
        Toast.makeText(myContext, "Enregistrement ref transaction: "+refTransaction+" pour facture N°: "+id, Toast.LENGTH_SHORT).show();

    }

    public void miseEnDuplicata(String id)
    {
        String sqlUpdate = "UPDATE paiement_facture set duplicata=1 WHERE idInterneReel='"+id+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Mise en duplicata pour id interne N°: "+id);
    }

    public void setCodeRecu(String codeRecu, String id)
    {
        String sqlUpdate = "UPDATE paiement_facture set codeRecu='"+codeRecu+"' WHERE idInterneReel='"+id+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        //Toast.makeText(myContext, "Code reçu: "+codeRecu, Toast.LENGTH_SHORT).show();
        Log.e("SQLITEDATABASE", "Enregistrement code reçu: "+codeRecu+" pour id interne: "+id);
        //Toast.makeText(myContext, "Enregistrement code reçu: "+codeRecu+" pour id interne: "+id, Toast.LENGTH_SHORT).show();

    }

    public void setServerCorrelationId(String serverCorrID, String id)
    {
        String sqlUpdate = "UPDATE paiement_facture set serverCorrelationId='"+serverCorrID+"' WHERE idInterneReel='"+id+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Toast.makeText(myContext, "Server correlation id: "+serverCorrID, Toast.LENGTH_SHORT).show();
        Log.e("SQLITEDATABASE", "Enregistrement server correlation id: "+serverCorrID+" pour id interne: "+id);
        //Toast.makeText(myContext, "Enregistrement code reçu: "+codeRecu+" pour id interne: "+id, Toast.LENGTH_SHORT).show();

    }

    public void setAnterieur(String refFacture, String anter)
    {
        String sqlUpdate = "UPDATE paiement_facture set anterieur='"+anter+"' WHERE refFacture='"+refFacture+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Enregistrement anterieur: "+anter+" pour facture N°: "+refFacture);
        Toast.makeText(myContext, "Enregistrement anterieur: "+anter+" pour facture N°: "+refFacture, Toast.LENGTH_SHORT).show();

    }

    public void setSuccessState(String idInterneReel)
    {
        String sqlUpdate = "UPDATE paiement_facture set etat='SUCCESS' WHERE idInterneReel='"+idInterneReel+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Mise en etat Success pour id interne: "+idInterneReel);
        //Toast.makeText(myContext, "Mise en etat Success pour id interne: "+idInterneReel, Toast.LENGTH_SHORT).show();

    }

//    public void setIdInterne(String refFacture, String idInterne, String datePaiement)
//    {
//        String sqlUpdate = "UPDATE paiement_facture set idInterne='"+idInterne+"' WHERE refFacture='"+refFacture+"'"+" AND daty='"+datePaiement+"'";
//        this.getWritableDatabase().execSQL(sqlUpdate);
//        Log.e("SQLITEDATABASE", "Enregistrement id interne: "+idInterne+" pour facture N°: "+refFacture);
//        Toast.makeText(myContext, "Enregistrement id interne: "+idInterne+" pour facture N°: "+refFacture, Toast.LENGTH_SHORT).show();
//    }

    public void setIdInterne(String refFacture, String idInterne, String datePaiement) {
        String sqlUpdate = "UPDATE paiement_facture set idInterne='" + idInterne + "' WHERE refFacture='" + refFacture + "'" + " AND daty='" + datePaiement + "'";
        this.getWritableDatabase().execSQL(sqlUpdate);

        Log.e("SQLITEDATABASE", "Enregistrement id interne: " + idInterne + " pour facture N°: " + refFacture);

        // ← Toast sur main thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(myContext,
                        "Enregistrement id interne: " + idInterne + " pour facture N°: " + refFacture,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void setIdInterneReel(String refFacture, String idInterne, String datePaiement)
    {
        String sqlUpdate = "UPDATE paiement_facture set idInterneReel='"+idInterne+"' WHERE refFacture='"+refFacture+"'"+" AND daty='"+datePaiement+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Enregistrement id interne réel: "+idInterne+" pour facture N°: "+refFacture);
        Toast.makeText(myContext, "Enregistrement id interne: "+idInterne+" pour facture N°: "+refFacture, Toast.LENGTH_SHORT).show();
    }

    public void setEtat(String etat, String idInterneReel)
    {
        String sqlUpdate = "UPDATE paiement_facture set etat='"+etat+"' WHERE idInterneReel='"+idInterneReel+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Enregistrement etat : "+etat+" pour id interne: "+idInterneReel);
        //Toast.makeText(myContext, "Enregistrement etat : "+etat+" pour id interne: "+idInterneReel, Toast.LENGTH_SHORT).show();
    }

    public void setMerchantNumber(String merchantNumber, String idInterneReel)
    {
        String sqlUpdate = "UPDATE paiement_facture set merchantNumber='"+merchantNumber+"' WHERE idInterneReel='"+idInterneReel+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Enregistrement merchant number : "+merchantNumber+" pour id interne: "+idInterneReel);
        Toast.makeText(myContext, "Enregistrement merchant number : "+merchantNumber+" pour id interne: "+idInterneReel, Toast.LENGTH_SHORT).show();
    }

    public void setEtatByRefId(String id, String etat)
    {
        String sqlUpdate = "UPDATE paiement_facture set etat='"+etat+"' WHERE id='"+id+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Enregistrement etat: "+etat+" pour id: "+id);

    }

    public int getIdPaiementFacture(String refTransaction)
    {
        String sql = "SELECT id FROM paiement_facture WHERE refTransaction='"+refTransaction+"'";
        Cursor cursor = this.getReadableDatabase().rawQuery(sql, null);
        cursor.moveToFirst();
        int valiny = 0;
        while(!cursor.isAfterLast())
        {
            valiny = Integer.valueOf(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();

        return valiny;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        updateWithNewColumn(sqLiteDatabase);
        Log.i("SQLITEDATA" +
                "BASE", "onUpgrade a été invoquer");
    }

    public void updateWithNewColumn(SQLiteDatabase sqLiteDatabase)
    {
        String sqlScriptUdate = "ALTER TABLE paiement_facture ADD COLUMN duplicata INTEGER DEFAULT 0";
        sqLiteDatabase.execSQL(sqlScriptUdate);
        Log.i("SQLITEDATABASE", "UpdateWithNewColumn a été invoquer");
    }

    public void createTableTarification(SQLiteDatabase sqLiteDatabase)
    {
        String createSql = "CREATE TABLE tarification_operateur("
                + "     id INTEGER primary key,"
                + "     operateur TEXT,"
                + "     tarif TEXT,"
                + "     datemaj TEXT"
                + ")";

        sqLiteDatabase.execSQL(createSql);
        Log.i("SQLITEDATABASE", "Table tarification_operateur créeé");
    }


    public List<PaiementFactureData> getAllListFacture()
    {
        List<PaiementFactureData> listePaiement = new ArrayList<>();
        //Methode 1:
            String selectSql = "SELECT * FROM paiement_facture";
            Cursor cursor = this.getReadableDatabase().rawQuery(selectSql, null);

        //Methode 2: style objet.
            /*Cursor cursor = this.getReadableDatabase().query("paiement_facture",new String[]{"id", "daty",
                            "frais", "latitude", "longitude", "montantFacture", "numeroPayeur", "operateur",
                            "refFacture", "totale"},null, null, null, null, null); */
            cursor.moveToFirst();
            while( !cursor.isAfterLast())
            {
                PaiementFactureData paiementFactureData = new PaiementFactureData(cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getString(7),
                        cursor.getString(8),
                        cursor.getString(9),
                        cursor.getString(10),
                        cursor.getString(11),
                        cursor.getString(13),
                        cursor.getString(14),
                        cursor.getString(15),
                        cursor.getString(16),
                        cursor.getString(17),
                        cursor.getString(18),
                        cursor.getString(19),
                        cursor.getString(20),
                        cursor.getString(21),
                        cursor.getString(22),
                        cursor.getString(23),
                        cursor.getString(24),
                        cursor.getString(25),
                        cursor.getString(26),
                        cursor.getString(27)
                        );
                listePaiement.add(paiementFactureData);
                cursor.moveToNext();
            }
            cursor.close();

        return listePaiement;
    }

    public List<PaiementFactureData> getListeForHistorique()
    {
        List<PaiementFactureData> listePaiement = new ArrayList<>();
        //Methode 1:
        String selectSql = "SELECT * FROM paiement_facture WHERE etat='SUCCESS' ORDER BY id DESC";
        Cursor cursor = this.getReadableDatabase().rawQuery(selectSql, null);


        cursor.moveToFirst();
        while( !cursor.isAfterLast())
        {
            PaiementFactureData paiementFactureData = new PaiementFactureData(cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8),
                    cursor.getString(9),
                    cursor.getString(10),
                    cursor.getString(11),
                    cursor.getString(13),
                    cursor.getString(14),
                    cursor.getString(15),
                    cursor.getString(16),
                    cursor.getString(17),
                    cursor.getString(18),
                    cursor.getString(19),
                    cursor.getString(20),
                    cursor.getString(21),
                    cursor.getString(22),
                    cursor.getString(23),
                    cursor.getString(24),
                    cursor.getString(25),
                    cursor.getString(26),
                    cursor.getString(27)
            );
            listePaiement.add(paiementFactureData);
            cursor.moveToNext();
        }
        cursor.close();

        return listePaiement;
    }

    public List<PaiementFactureData> getListeForSynchronization()
    {
        List<PaiementFactureData> listePaiement = new ArrayList<>();
        //Methode 1:
       // String selectSql = "SELECT * FROM paiement_facture WHERE etat!='Success'";
        String selectSql = "SELECT * FROM paiement_facture WHERE etat!='SUCCESS' ORDER BY id DESC";
        //String selectSql = "SELECT * FROM paiement_facture";
        Cursor cursor = this.getReadableDatabase().rawQuery(selectSql, null);
        cursor.moveToFirst();
        while( !cursor.isAfterLast())
        {
            PaiementFactureData paiementFactureData = new PaiementFactureData(cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8),
                    cursor.getString(9),
                    cursor.getString(10),
                    cursor.getString(11),
                    cursor.getString(13),
                    cursor.getString(14),
                    cursor.getString(15),
                    cursor.getString(16),
                    cursor.getString(17),
                    cursor.getString(18),
                    cursor.getString(19),
                    cursor.getString(20),
                    cursor.getString(21),
                    cursor.getString(22),
                    cursor.getString(23),
                    cursor.getString(24),
                    cursor.getString(25),
                    cursor.getString(26),
                    cursor.getString(27)
            );
            listePaiement.add(paiementFactureData);
            cursor.moveToNext();
        }
        cursor.close();

        return listePaiement;
    }


    public void updateTarificationOperateurBase(TarificationOperateur tarificationOperateur)
    {
        String sqlUpdateTarification = "UPDATE tarification_operateur SET tarif='"
                +tarificationOperateur.getTarif()+"', datemaj='"
                +tarificationOperateur.getDatemaj()
                +"' WHERE id="+tarificationOperateur.getId();

        this.getWritableDatabase().execSQL(sqlUpdateTarification);

        Log.i("SQLITEDATABASE", "Update tarification base faite =>"
                + tarificationOperateur.getOperateur() +": "
                +tarificationOperateur.getTarif()+" MGA");
    }

    public void insertIntoTarificationOperateurFirestoreFirst()
    {
        //FONCTION DE BASE DE PREMIERE INSERTION DANS SQLITE DES TARIFICATION DEPUIS FIRESTORE:
        firestoreDB.collection("tarification_operateur")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            //MAKA NY LISTE TARIFICATION VIA FIRESTORE:
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("TAG", document.getId() + " => " + document.getData());

                                Map<String, Object> temporaireTarification = new HashMap<>();
                                temporaireTarification = document.getData();
                                TarificationOperateur tarificationOperateur = new TarificationOperateur(Integer.valueOf(String.valueOf(temporaireTarification.get("id"))), String.valueOf(temporaireTarification.get("operateur")), String.valueOf(temporaireTarification.get("tarif")), String.valueOf(temporaireTarification.get("datemaj")));
                                Log.e("OBJT", tarificationOperateur.getOperateur());
                                insertIntoTarificationBase(tarificationOperateur);
                                //listeTarifViaFirebase.add(tarificationOperateur);
                            }
                             //MAKA NY AO ANATY BASE SQLITE:

                        } else {
                            Log.w("TAG", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public void insertIntoTarificationBase(TarificationOperateur operateur)
    {
        //FONCTION DE BASE INSERTION TARIFICATION OPERATEUR DANS SQLITE:
        String insertSql = "INSERT INTO tarification_operateur VALUES("
                + operateur.getId() +",'"
                + operateur.getOperateur() + "','"
                + operateur.getTarif() + "','"
                + operateur.getDatemaj() + "')";

        this.getWritableDatabase().execSQL(insertSql);
        this.getWritableDatabase().close();
        Log.i("SQLITEDATABASE", "Insertion tarification base invoqué");
    }

    public int verificationEtInsertionGeneraleTarificationOperateur(List<TarificationOperateur> listeTarificationFirebase)
    {
        //FONCTION DE PREMIERE INSERTION TARIF OPERATEUR DEPUIS FIREBASE:
        List<TarificationOperateur> tarificationDansSQLITE = this.getTarificationOperateurAll();
        int valiny = 0;
        if(tarificationDansSQLITE.size()==0)
        {
            for(int i=0; i<listeTarificationFirebase.size(); i++)
            {
                this.insertIntoTarificationBase(listeTarificationFirebase.get(i));
            }

        }
        else
        {
            Log.d("TARIFICATION", "TARIFICATION CONTIENT DES DONNEES");
            valiny = 1;
        }
        return valiny;
    }

    public List<TarificationOperateur> getTarificationOperateurAll()
    {
        //FONCTION DE GET TOUT LES TARIFICATION OPERATEUR DANS BASE SQLITE:
        List<TarificationOperateur> listeTarification = new ArrayList<>();
        //Methode 1:
        String selectSql = "SELECT * FROM tarification_operateur";
        Cursor cursor = this.getReadableDatabase().rawQuery(selectSql, null);

        cursor.moveToFirst();
        while( !cursor.isAfterLast())
        {
            TarificationOperateur tarificationOperateur = new TarificationOperateur(cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
            );
            listeTarification.add(tarificationOperateur);
            cursor.moveToNext();
        }
        cursor.close();
        return listeTarification;
    }

    public void deMarkForSynchronization(String refFacture, String datePaiement)
    {
        String sqlUpdate = "UPDATE paiement_facture set synchronisation='false' WHERE refFacture='"+refFacture+"'"+" AND daty='"+datePaiement+"'";
        this.getWritableDatabase().execSQL(sqlUpdate);
        Log.e("SQLITEDATABASE", "Demarquage synchronisation effectué");
    }

    private String singleQuoteManager(String str)
    {
        str.replace("'", " ");
        return str;
    }
}
