package mg.eqima.jiramacashless.dataobject;

public class PaiementFactureData {
    String id;
    String daty;
    String frais;
    String latitude;
    String longitude;
    String montantFacture;
    String numeroPayeur;
    String numeroCashpoint;
    String operateur;
    String refTransaction;
    String refFacture;
    String totale;
    String codeRecu;
    String refClient;
    String nomClient;
    String adresseClient;
    String idInterne;
    String mois;
    String annee;
    String etat;
    String anterieur;
    String caissier;
    String idInterneReel;
    String serverCorrelationId;
    String merchantNumber;
    String idDistributeur;
    String duplicata;


    public PaiementFactureData(String id, String daty, String frais, String latitude, String longitude, String montantFacture,
                               String numeroPayeur, String numeroCashpoint,
                               String operateur, String refTransaction, String refFacture, String totale,
                               String codeRecu, String refClient, String nomClient, String adresseClient,
                               String idInterne, String mois, String annee, String etat, String ant,
                               String caissier, String idInterneR, String serverCorrelationId,
                               String merchantNumbers, String idDistributeur, String dupli) {
        this.id = id;
        this.daty = daty;
        this.frais = frais;
        this.latitude = latitude;
        this.longitude = longitude;
        this.montantFacture = montantFacture;
        this.numeroPayeur = numeroPayeur;
        this.numeroCashpoint = numeroCashpoint;
        this.operateur = operateur;
        this.refTransaction = refTransaction;
        this.refFacture = refFacture;
        this.totale = totale;
        this.codeRecu = codeRecu;
        this.refClient = refClient;
        this.nomClient = nomClient;
        this.adresseClient = adresseClient;
        this.idInterne = idInterne;
        this.mois = mois;
        this.annee = annee;
        this.etat = etat;
        this.anterieur = ant;
        this.caissier = caissier;
        this.idInterneReel = idInterneR;
        this.serverCorrelationId = serverCorrelationId;
        this.merchantNumber = merchantNumbers;
        this.idDistributeur = idDistributeur;
        this.duplicata      = dupli;
    }


    public String getCaissier() {
        return caissier;
    }

    public void setCaissier(String caissier) {
        this.caissier = caissier;
    }

    public String getAnterieur() {
        return anterieur;
    }

    public void setAnterieur(String anterieur) {
        this.anterieur = anterieur;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeroCashpoint() {
        return numeroCashpoint;
    }

    public void setNumeroCashpoint(String numeroCashpoint) {
        this.numeroCashpoint = numeroCashpoint;
    }

    public String getCodeRecu() {
        return codeRecu;
    }

    public void setCodeRecu(String codeRecu) {
        this.codeRecu = codeRecu;
    }

    public String getRefClient() {
        return refClient;
    }

    public void setRefClient(String refClient) {
        this.refClient = refClient;
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

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getRefTransaction() {
        return refTransaction;
    }

    public void setRefTransaction(String refTransaction) {
        this.refTransaction = refTransaction;
    }

    public String getDaty() {
        return daty;
    }

    public void setDaty(String daty) {
        this.daty = daty;
    }

    public String getFrais() {
        return frais;
    }

    public void setFrais(String frais) {
        this.frais = frais;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getMontantFacture() {
        return montantFacture;
    }

    public void setMontantFacture(String montantFacture) {
        this.montantFacture = montantFacture;
    }

    public String getNumeroPayeur() {
        return numeroPayeur;
    }

    public void setNumeroPayeur(String numeroPayeur) {
        this.numeroPayeur = numeroPayeur;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getRefFacture() {
        return refFacture;
    }

    public void setRefFacture(String refFacture) {
        this.refFacture = refFacture;
    }

    public String getTotale() {
        return totale;
    }

    public void setTotale(String totale) {
        this.totale = totale;
    }

    public String getIdInterneReel() {
        return idInterneReel;
    }

    public void setIdInterneReel(String idInterneReel) {
        this.idInterneReel = idInterneReel;
    }

    public String getServerCorrelationId() {
        return serverCorrelationId;
    }

    public void setServerCorrelationId(String serverCorrelationId) {
        this.serverCorrelationId = serverCorrelationId;
    }

    public String getMerchantNumber() {
        return merchantNumber;
    }

    public void setMerchantNumber(String merchantNumber) {
        this.merchantNumber = merchantNumber;
    }

    public String getDuplicata() {
        return duplicata;
    }

    public void setDuplicata(String duplicata) {
        this.duplicata = duplicata;
    }

    @Override
    public String toString() {
        return "PaiementFactureData{" +
                "refTransaction='" + refTransaction + '\'' +
                ", daty='" + daty + '\'' +
                ", frais='" + frais + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", montantFacture='" + montantFacture + '\'' +
                ", numeroPayeur='" + numeroPayeur + '\'' +
                ", operateur='" + operateur + '\'' +
                ", refFacture='" + refFacture + '\'' +
                ", totale='" + totale + '\'' +
                '}';
    }

    public String getIdDistributeur() {
        return idDistributeur;
    }

    public void setIdDistributeur(String idDistributeur) {
        this.idDistributeur = idDistributeur;
    }


}
