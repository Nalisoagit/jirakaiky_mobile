package mg.eqima.jiramacashless.model.operateur;

public class TarificationOperateur {
    private int id;
    private String operateur;
    private String tarif;
    private String datemaj;

    public TarificationOperateur() {
    }

    public TarificationOperateur(int id, String operateur, String tarif, String datemaj) {
        this.id = id;
        this.operateur = operateur;
        this.tarif = tarif;
        this.datemaj = datemaj;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getTarif() {
        return tarif;
    }

    public void setTarif(String tarif) {
        this.tarif = tarif;
    }

    public String getDatemaj() {
        return datemaj;
    }

    public void setDatemaj(String datemaj) {
        this.datemaj = datemaj;
    }
}
