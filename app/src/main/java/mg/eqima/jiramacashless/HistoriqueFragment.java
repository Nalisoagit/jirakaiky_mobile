package mg.eqima.jiramacashless;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.dataobject.PaiementFactureData;
import mg.eqima.jiramacashless.recu.Recu;
import mg.eqima.jiramacashless.recyclerview.RecyclerViewCardAdapter;
import mg.eqima.jiramacashless.recyclerview.RecyclerViewInterface;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HistoriqueFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoriqueFragment extends Fragment implements RecyclerViewInterface {

    RecyclerView recyclerView;
    DatabaseManager databaseManager;
    Context context;

    // LIST:
    List<PaiementFactureData> historiquePaiementFactures;
    Recu recuFactureDejaPayer;



    public HistoriqueFragment(DatabaseManager dbManager, Context context) {
        // Required empty public constructor
        this.setDatabaseManager(dbManager);
    }

    public HistoriqueFragment() {
        // Required empty public constructor

    }


    private void setDatabaseManager(DatabaseManager dbManager) {
        this.databaseManager = dbManager;
    }
    private void setContext(Context context){ this.context = context; }



    // TODO: Rename and change types and number of parameters
    public static HistoriqueFragment newInstance() {
        HistoriqueFragment fragment = new HistoriqueFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inf = inflater.inflate(R.layout.fragment_historique, container, false);
        recyclerView = inf.findViewById(R.id.recyclerViewHistorique);
        historiquePaiementFactures = databaseManager.getListeForHistorique();

        RecyclerViewCardAdapter recyclerViewCardAdapter = new RecyclerViewCardAdapter(container.getContext(),historiquePaiementFactures, this);
        recyclerView.setAdapter(recyclerViewCardAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        return inf;
    }

    public String hidePhoneNumber(String numero)
    {
        if(!numero.startsWith("0"))
        {
            numero = "0"+numero;
        }
        String head = numero.substring(0, 5);
        String tail = numero.substring(8);
        String valiny = head+"***"+tail;
        return valiny;
    }

    @Override
    public void onClickItemListener(int position) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        alertDialog.setTitle(R.string.historique);
        alertDialog.setIcon(R.drawable.historique);
        alertDialog.setMessage(getContext().getString(R.string.refFactureLabel)+" "+historiquePaiementFactures.get(position).getRefFacture()
                                +"\n"+ getContext().getString(R.string.refClientLabel)+" "+historiquePaiementFactures.get(position).getRefClient()
                                +"\n"+ getContext().getString(R.string.nom_label)+" "+ historiquePaiementFactures.get(position).getNomClient());
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog.setNeutralButton(getContext().getString(R.string.imprimer), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                recuFactureDejaPayer = new Recu();

                recuFactureDejaPayer.setNumeroRecuJirama(historiquePaiementFactures.get(position).getCodeRecu());
                recuFactureDejaPayer.setNumeroCashpoint(historiquePaiementFactures.get(position).getNumeroCashpoint());
                recuFactureDejaPayer.setRefFacture(historiquePaiementFactures.get(position).getRefFacture());
                recuFactureDejaPayer.setMois(historiquePaiementFactures.get(position).getMois());
                recuFactureDejaPayer.setAnnee(historiquePaiementFactures.get(position).getAnnee());
                recuFactureDejaPayer.setReferenceClient(historiquePaiementFactures.get(position).getRefClient());
                recuFactureDejaPayer.setNomClient(historiquePaiementFactures.get(position).getNomClient());
                recuFactureDejaPayer.setMontantFacture(String.valueOf(historiquePaiementFactures.get(position).getMontantFacture()));
                recuFactureDejaPayer.setFrais(String.valueOf(historiquePaiementFactures.get(position).getFrais()));
                recuFactureDejaPayer.setMontantTotale(String.valueOf(historiquePaiementFactures.get(position).getTotale()));
                recuFactureDejaPayer.setRefTransaction(historiquePaiementFactures.get(position).getRefTransaction());
                recuFactureDejaPayer.setNumero(historiquePaiementFactures.get(position).getNumeroPayeur());
                recuFactureDejaPayer.setOperateur(historiquePaiementFactures.get(position).getOperateur());
                recuFactureDejaPayer.setDaty(historiquePaiementFactures.get(position).getDaty());
                recuFactureDejaPayer.setIdInterne(historiquePaiementFactures.get(position).getIdInterne());
                recuFactureDejaPayer.setCaissier(historiquePaiementFactures.get(position).getCaissier());

                String combinaison = recuFactureDejaPayer.getRefFacture()+"_"+recuFactureDejaPayer.getNumeroRecuJirama();

                Intent intent = new Intent(getContext(), DetailPaiementFacture.class);
                Bundle recuDetailBundle = new Bundle();
                recuDetailBundle.putString("recu_detail", recuFactureDejaPayer.contruireMonRecu());
                recuDetailBundle.putString("depart", "2");
                recuDetailBundle.putString("combinaison", combinaison);
                recuDetailBundle.putString("duplicata", historiquePaiementFactures.get(position).getDuplicata());
                recuDetailBundle.putString("idInterneReel", historiquePaiementFactures.get(position).getIdInterneReel());
                intent.putExtras(recuDetailBundle);
                dialogInterface.dismiss();
                startActivity(intent);
            }
        });
        alertDialog.show();

    }

    @Override
    public void removeFromList(int position) {
    }
}