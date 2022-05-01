package Model.Prodotto_Ordine;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

public class Prodotto_Ordine implements Serializable  {

    @Serial
    private static final long serialVersionUID = -4914161285010290458L;

    private String email_utente;
    private String codice_ordine;
    private String codice_gioco;
    private Timestamp data_ora_creazione;
    private int quantita;

    public Prodotto_Ordine() {

    }

    @Override
    public String toString() {
        return "Prodotto_Ordine{" +
                "email_utente='" + email_utente + '\'' +
                ", codice_ordine='" + codice_ordine + '\'' +
                ", codice_gioco='" + codice_gioco + '\'' +
                ", data_ora_creazione=" + data_ora_creazione +
                ", quantita=" + quantita +
                '}';
    }

    public String getEmail_utente() {
        return email_utente;
    }

    public void setEmail_utente(String email_utente) {
        this.email_utente = email_utente;
    }

    public String getCodice_ordine() {
        return codice_ordine;
    }

    public void setCodice_ordine(String codice_ordine) {
        this.codice_ordine = codice_ordine;
    }

    public String getCodice_gioco() {
        return codice_gioco;
    }

    public void setCodice_gioco(String codice_gioco) {
        this.codice_gioco = codice_gioco;
    }

    public Timestamp getData_ora_creazione() {
        return data_ora_creazione;
    }

    public void setData_ora_creazione(Timestamp data_ora_creazione) {
        this.data_ora_creazione = data_ora_creazione;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }
}