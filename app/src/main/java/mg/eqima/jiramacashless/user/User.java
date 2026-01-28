package mg.eqima.jiramacashless.user;

public class User {
    private String id;
    private String signature;
    private String email;
    private String nom;
    private String numero;
    private String password;
    private boolean validation;
    private String credit;
    private String idDistributeur;

    public User() {
    }

    public User(String email, String nom, String numero, String password) {
        this.email = email;
        this.nom = nom;
        this.numero = numero;
        this.password = password;
    }

    public User(String email, String nom, String numero, String password, boolean validation) {
        this.email = email;
        this.nom = nom;
        this.numero = numero;
        this.password = password;
        this.validation = validation;
    }

    public User(String id, String email, String nom, String numero, String password, boolean validation) {
        this.id = id;
        this.email = email;
        this.nom = nom;
        this.numero = numero;
        this.password = password;
        this.validation = validation;
    }

    public User(String id, String idCard, String email, String nom, String numero, String password, boolean validation) {
        this.id = id;
        this.signature = idCard;
        this.email = email;
        this.nom = nom;
        this.numero = numero;
        this.password = password;
        this.validation = validation;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String idCard) {
        this.signature = idCard;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isValidation() {
        return validation;
    }

    public void setValidation(boolean validation) {
        this.validation = validation;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getIdDistributeur() {
        return idDistributeur;
    }

    public void setIdDistributeur(String idDistributeur) {
        this.idDistributeur = idDistributeur;
    }
}
