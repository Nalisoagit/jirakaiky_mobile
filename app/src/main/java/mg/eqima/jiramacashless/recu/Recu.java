package mg.eqima.jiramacashless.recu;


import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

import mg.eqima.jiramacashless.utilitaire.SharedData;

public class Recu {


    String daty;
    String refFacture;
    String montantFacture;
    String numero;
    String numeroCashpoint;
    String frais;
    String montantTotale;
    String operateur;
    String refTransaction;
    String numeroRecuJirama;
    String referenceClient;
    String nomClient;
    String adresseClient;
    String idInterne;
    String mois;
    String annee;
    String etat;
    String agence="Jirakaiky-Ankorondrano";
    String caisse="999EQ";
    String caisse_paoma = "999EQP";
    String caissier;

    double seuille = 10000;
    int fraisJirakaiky ;

    public Recu(String daty, String refFacture, String montantFacture, String numero, String numeroCashpoint, String frais, String montantTotale, String operateur, String refTransaction,String referenceClient, String nom, String adresse, String mois, String annee, String caissier) {
        this.daty = daty;
        this.refFacture = refFacture;
        this.montantFacture = montantFacture;
        this.numero = numero;
        this.numeroCashpoint = numeroCashpoint;
        this.frais = frais;
        this.montantTotale = montantTotale;
        this.operateur = operateur;
        this.refTransaction = refTransaction;
        this.referenceClient = referenceClient;
        this.nomClient = nom;
        this.adresseClient = adresse;
        this.mois = mois;
        this.annee = annee;
        this.caissier = caissier;

    }

    public String getNumeroCashpoint() {
        return numeroCashpoint;
    }

    public void setNumeroCashpoint(String numeroCashpoint) {
        this.numeroCashpoint = numeroCashpoint;
    }

    public String getNumeroRecuJirama() {
        return numeroRecuJirama;
    }

    public void setNumeroRecuJirama(String numeroRecuJirama) {
        this.numeroRecuJirama = numeroRecuJirama;
    }

    public String getMontantTotale() {

        String montantTotaleV = "";
        if(this.montantTotale.contains(".0"))
        {
            montantTotaleV = this.montantTotale;
        }
        if(!this.montantTotale.contains(".0"))
        {
            montantTotaleV = this.montantTotale+".0";
        }
        return montantTotaleV;
    }

    public void setMontantTotale(String montantTotale) {
        this.montantTotale = montantTotale;
    }

    public String getDaty() {
        return daty;
    }

    public String getRefTransaction() {
        return refTransaction;
    }

    public void setRefTransaction(String refTransaction) {
        this.refTransaction = refTransaction;
    }

    public void setDaty(String daty) {
        this.daty = daty;
    }

    public String getRefFacture() {
        return refFacture;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public void setRefFacture(String refFacture) {
        this.refFacture = refFacture;
    }

    public String getMontantFacture() {

        String montantV = "";
        if(this.montantFacture.contains(".0"))
        {
            montantV = this.montantFacture;
        }
        if(!this.montantFacture.contains(".0"))
        {
            montantV = this.montantFacture+".0";
        }
        return montantV;
    }

    public void setMontantFacture(String montantFacture) {
        this.montantFacture = montantFacture;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getFrais() {
        String fraisV = "";
        if(this.frais.contains(".0"))
        {
            fraisV = this.frais;
        }
        if(!this.frais.contains(".0"))
        {
            fraisV = this.frais+".0";
        }
        return fraisV;
    }

    public void setFrais(String frais) {
        this.frais = frais;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getAdresseClient() {
        return adresseClient;
    }

    public void setAdresseClient(String adresseClient) {
        this.adresseClient = adresseClient;
    }

    public String getReferenceClient() {
        return referenceClient;
    }

    public void setReferenceClient(String referenceClient) {
        this.referenceClient = referenceClient;
    }

    public String getIdInterne() {
        return idInterne;
    }

    public void setIdInterne(String idInterne) {
        this.idInterne = idInterne;
    }

    public String getMois() {
        return mois;
    }

    public void setMois(String mois) {
        this.mois = mois;
    }

    public String getAnnee() {
        return annee;
    }

    public void setAnnee(String annee) {
        this.annee = annee;
    }

    public String getCaissier() {
        return caissier;
    }

    public void setCaissier(String caissier) {
        this.caissier = caissier;
    }

    public Recu() {
    }


    public String contruireMonRecu()

    {
        fraisJirakaiky = SharedData.TRANSACTION_FEE_JIRAKAIKY;


        String recu= "      JIRO sy RANO MALAGASY\n";
        recu += "         TICKET D\'ACQUIT \n\n";
        recu += "Recu jirama:      "+this.numeroRecuJirama+"\n";
        recu += "AGENCE:  "+this.agence+"\n";
        recu += "Caisse:           "+this.getCaisse()+"\n";
        recu += "Du:     "+this.daty+"\n";
        recu += "Caissier:         "+this.caissier+"\n";
        recu += "--------------------------------\n";
        recu += "Ref client:       "+this.referenceClient+"\n";
        recu += "Nom client:       "+this.nomClient+"\n";
        recu += "--------------------------------\n";
        recu += "Ref facture:   "+this.refFacture+"\n";
        recu += "Mois facture:     "+this.mois+" "+this.annee+"\n";
        recu += "Montant facture:  "+this.getMontantFacture()+" MGA\n\n";
        recu += "  Veuillez conserver ce ticket\n";
        recu += "  Tehirizo tsara ity rosia ity\n\n";
        recu += "===============================\n";
        recu += "Ref transaction:  "+this.refTransaction+"\n";
        recu += "Numero payeur:    "+this.numero+"\n";
        recu += "Operateur:        "+this.operateur+"\n";
        recu += "Id interne:       "+this.idInterne+"\n";
        recu += "Frais jirakaiky: "+fraisJirakaiky+" MGA\n";
        recu += "Frais operateur:  500 MGA\n";
        recu+=  "[L]\n" ;
        recu += "[C]Misaotra anao nanjifa. Amparitao amin’ny namanao ny JIRAKAIKY !\n" ;

        SpannableString spannableString = new SpannableString(recu);
        spannableString.setSpan(new RelativeSizeSpan(0.7f), 0, recu.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

        return spannableString.toString();
    }

    public SpannableString contruireMonRecu2()
    {
        String recu= "      JIRO sy RANO MALAGASY\n";
        recu += "         TICKET D\'ACQUIT \n\n";
        recu += "Recu jirama:      "+this.numeroRecuJirama+"\n";
        recu += "AGENCE:  "+this.agence+"\n";
        recu += "Caisse:           "+this.getCaisse()+"\n";
        recu += "Du:     "+this.daty+"\n";
        recu += "Caissier:         "+this.caissier+"\n";
        recu += "--------------------------------\n";
        recu += "Ref client:       "+this.referenceClient+"\n";
        recu += "Nom client:       "+this.nomClient+"\n";
        recu += "--------------------------------\n";
        recu += "Ref facture:   "+this.refFacture+"\n";
        recu += "Mois facture:     "+this.mois+" "+this.annee+"\n";
        recu += "Montant facture:  "+this.getMontantFacture()+" MGA\n\n";
        recu += "  Veuillez conserver ce ticket\n";
        recu += "  Tehirizo tsara ity rosia ity\n\n";
        recu += "===============================\n";
        recu += "Ref transaction:  "+this.refTransaction+"\n";
        recu += "Numero payeur:    "+this.numero+"\n";
        recu += "Operateur:        "+this.operateur+"\n";
        recu += "Id interne:       "+this.idInterne+"\n";

        recu += "Frais jirakaiky: "+getFraisSeuille()+" MGA\n";

        SpannableString spannableString = new SpannableString(recu);
        spannableString.setSpan(new RelativeSizeSpan(0.7f), 0, recu.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

        return spannableString;
    }

    public String deletePointDoubleZero(String montant)
    {
        String valiny = "";
        if(montant.contains(".00") || montant.contains(".0"))
        {
            if(montant.contains(".00"))
            {
                valiny = montant.replace(".00","");
            }
            if(montant.contains(".0") && !montant.contains(".00"))
            {
                valiny = montant.replace(".0","");
            }
        }
        if(!montant.contains(".00") && !montant.contains(".0"))
        {
            valiny = montant;
        }
        return valiny;
    }

    public String getFraisSeuille()
    {
        String frais = "1000.0";
        if(Double.valueOf(this.getMontantFacture())<=seuille)
        {
            frais = "500.0";
        }

        return frais;
    }

    public String getCaisse()
    {
        String valiny = this.caisse;
        if(this.operateur.equals("Paoma"))
        {
            valiny = this.caisse_paoma;
        }
        return valiny;
    }

}
