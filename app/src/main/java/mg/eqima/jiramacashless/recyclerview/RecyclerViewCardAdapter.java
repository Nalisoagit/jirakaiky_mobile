package mg.eqima.jiramacashless.recyclerview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mg.eqima.jiramacashless.R;
import mg.eqima.jiramacashless.dataobject.PaiementFactureData;

public class RecyclerViewCardAdapter extends RecyclerView.Adapter<RecyclerViewCardAdapter.MyViewHolder> {
    private final RecyclerViewInterface recyclerViewInterface;
    Context context;
    List<PaiementFactureData> listePaiementFacture;

    public RecyclerViewCardAdapter(Context context, List<PaiementFactureData> listePaiementFacture, RecyclerViewInterface recyclerViewInterface)
    {
        this.context = context;
        this.listePaiementFacture = listePaiementFacture;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public RecyclerViewCardAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.template_cardview, parent, false);

        return new RecyclerViewCardAdapter.MyViewHolder(view, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewCardAdapter.MyViewHolder holder, int position) {
        holder.cardRef.setText("Référence : "+listePaiementFacture.get(position).getRefFacture());
        holder.cardNumero.setText("Numéro    : "+listePaiementFacture.get(position).getNumeroPayeur());
        holder.cardMontant.setText("Montant   : "+listePaiementFacture.get(position).getMontantFacture()+" MGA");
        holder.cardDate.setText("Date      : "+listePaiementFacture.get(position).getDaty());
    }

    @Override
    public int getItemCount() {
        return listePaiementFacture.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView cardRef;
        TextView cardMontant;
        TextView cardNumero;
        TextView cardDate;

        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            cardRef = itemView.findViewById(R.id.cardRef);
            cardNumero = itemView.findViewById(R.id.cardNumero);
            cardMontant = itemView.findViewById(R.id.cardMontant);
            cardDate = itemView.findViewById(R.id.cardDate);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(recyclerViewInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION)
                        {
                            recyclerViewInterface.onClickItemListener(pos);
                        }
                    }
                }
            });

        }
    }

}
