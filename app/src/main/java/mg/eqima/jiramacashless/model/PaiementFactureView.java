package mg.eqima.jiramacashless.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.UnsupportedEncodingException;

import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class PaiementFactureView {

    private String id;
    private String frais;
    private String latitude;
    private String longitude;
    private String montantFacture;
    private String numeroPayeur;
    private String numeroCashpoint;
    private String operateur;
    private String refFacture;
    private String refTransaction;
    private String total;
    private String datePaiement;
    private String codeRecu = "";
    private String refClient = "";
    private String nomClient = "";
    private String adresseClient = "";
    private String mois;
    private String annee;
    private String etat;
    private String anterieur;
    private String caissier;
    private String idDistributeur;
    private String paiementDistributeur;
    private String codeTemporaire ;

    private String signature;

    private String dateId;
    private String androidId ;



    public PaiementFactureView() {
    }

    public PaiementFactureView(String id, String frais, String latitude, String longitude, String montantFacture, String numeroPayeur,
                               String numeroCashpoint, String operateur, String refFacture, String refTransaction, String total, String datePaiement,
                               String codeRecu, String refClient, String nomClient, String adresseClient, String mois, String annee, String etat, String ant,String codeTemporaire,String androidId) {
        this.id = id;
        this.frais = frais;
        this.latitude = latitude;
        this.longitude = longitude;
        this.montantFacture = montantFacture;
        this.numeroPayeur = numeroPayeur;
        this.numeroCashpoint = numeroCashpoint;
        this.operateur = operateur;
        this.refFacture = refFacture;
        this.refTransaction = refTransaction;
        this.total = total;
        this.datePaiement = datePaiement;
        this.codeRecu = codeRecu;
        this.refClient = refClient;
        this.nomClient = nomClient;
        this.adresseClient = adresseClient;
        this.mois = mois;
        this.annee = annee;
        this.etat = etat;
        this.anterieur = ant;
        this.codeTemporaire=codeTemporaire ;
        this.androidId=androidId ;
    }
    public String getCodeTemporaire() {
        return codeTemporaire;
    }

    public void setCodeTemporaire(String codeTemporaire) {
        this.codeTemporaire = codeTemporaire;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRefTransaction() {
        return refTransaction;
    }

    public void setRefTransaction(String refTransaction) {
        this.refTransaction = refTransaction;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getNumeroCashpoint() {
        return numeroCashpoint;
    }

    public void setNumeroCashpoint(String numeroCashpoint) {
        this.numeroCashpoint = numeroCashpoint;
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

    public String getCodeRecu() {
        return codeRecu;
    }

    public void setCodeRecu(String codeRecu) {
        this.codeRecu = codeRecu;
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

    public String getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(String datePaiement) {
        this.datePaiement = datePaiement;
    }

    public String getRefClient() {
        return refClient;
    }

    public void setRefClient(String refClient) {
        this.refClient = refClient;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getAnterieur() {
        return anterieur;
    }

    public void setAnterieur(String anterieur) {
        this.anterieur = anterieur;
    }

    public String getCaissier() {
        return caissier;
    }

    public void setCaissier(String caissier) {
        this.caissier = caissier;
    }

    public String getIdDistributeur() {
        return idDistributeur;
    }

    public void setIdDistributeur(String idDistributeur) {
        this.idDistributeur = idDistributeur;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDateId() {
        return dateId;
    }

    public void setDateId(String dateId) {
        this.dateId = dateId;
    }

    public String getPaiementDistributeur() {
        return paiementDistributeur;
    }

    public void setPaiementDistributeur(String paiementDistributeur) {
        this.paiementDistributeur = paiementDistributeur;
    }
    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    //LES METHODES AJOUTEES:
    public String jsonMe() throws JsonProcessingException {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return objectWriter.writeValueAsString(this);
    }

    public String encodeMeAsAURLBody() throws JsonProcessingException, UnsupportedEncodingException {
        Utilitaire utilitaire = new Utilitaire();
        String jsonMe = this.jsonMe();
        return utilitaire.encodeUrlBody(jsonMe);
    }



}
