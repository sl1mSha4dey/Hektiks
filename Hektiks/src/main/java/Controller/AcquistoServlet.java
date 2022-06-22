package Controller;

import Model.Carrello.CarrelloDAO;
import Model.Gioco.Gioco;
import Model.Gioco.GiocoDAO;
import Model.Ordine.Ordine;
import Model.Ordine.OrdineDAO;
import Model.Prodotto.ProdottoDAO;
import Model.Prodotto_Ordine.Prodotto_Ordine;
import Model.Prodotto_Ordine.Prodotto_OrdineDAO;
import Model.Utente.Utente;
import Model.Utente.UtenteDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class AcquistoServlet extends HttpServlet {

    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        controllaSeLoggato(request, response);

        String from = request.getParameter("from");

        if (!from.equals("carrello") && !from.equals("gioco")) {
            request.getSession().setAttribute("msg-error", "Qualcosa è andato storto!");
            response.sendRedirect(request.getRequestURI());
        }

        List<Gioco> giochiDaAcquistare = new ArrayList<>();
        DataSource source = (DataSource) getServletContext().getAttribute("DataSource");
        GiocoDAO giocoDAO = new GiocoDAO(source);
        double prezzoTotale = 0, prezzoGioco = 0;
        Gioco gioco = null;
        HttpSession session = request.getSession();
        HashMap<String, Integer> carrello = (HashMap<String, Integer>) session.getAttribute("carrello");

        // from -> "carrello", la richiesta proviene da carrello.jsp quindi l'utente vuole acquistare tutto quello che sta nel carrello
        // from -> "gioco", la richiesta proviene da carrello.jsp quindi l'utente vuole acquistare solo quello specifico gioco

        if (from.equals("carrello")) {
            for (String key : carrello.keySet()) {
                try {
                    gioco = giocoDAO.doRetrieveByKey(key);

                    if (gioco.getPercentuale_sconto() > 0)
                        prezzoGioco = gioco.getPrezzo() - ((gioco.getPrezzo() * gioco.getPercentuale_sconto()) / 100);
                    else
                        prezzoGioco = gioco.getPrezzo();

                    prezzoTotale += prezzoGioco * carrello.get(key);
                    giochiDaAcquistare.add(gioco);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            String codice_gioco = request.getParameter("codice_gioco");

            try {
                gioco = giocoDAO.doRetrieveByKey(codice_gioco);

                if (gioco.getPercentuale_sconto() > 0)
                    prezzoTotale = gioco.getPrezzo() - ((gioco.getPrezzo() * gioco.getPercentuale_sconto()) / 100);
                else
                    prezzoTotale = gioco.getPrezzo();

                giochiDaAcquistare.add(gioco);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        Utente utente = (Utente) session.getAttribute("user");

        if (utente.getSaldo() < prezzoTotale) {
            session.setAttribute("msg-error", "Il tuo saldo non è sufficiente ad effettuare l'acquisto");
            response.sendRedirect(request.getRequestURI());
            return;
        }

        OrdineDAO ordineDAO = new OrdineDAO(source);
        Prodotto_OrdineDAO prodotto_ordineDAO = new Prodotto_OrdineDAO(source);

        Ordine ordine = new Ordine();
        String codice_ordine = null;

        // Mi assicuro che il codice ordine sia unico
        try {
            do {
                codice_ordine = generaCodiceOrdine();
            } while (ordineDAO.doRetrieveByKey(utente.getEmail(), codice_ordine) != null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ordine.setCodice_ordine(codice_ordine);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        ordine.setEmail_utente(utente.getEmail());
        ordine.setData_ora_ordinazione(date);
        ordine.setPrezzo_totale(prezzoTotale);

        try {
            // Se l'ordine è andato a buon fine, inserisco i prodotti in db
            // Il procedimento è uguale per entrambe i valori di "from"
            if (ordineDAO.doSave(ordine)) {
                Prodotto_Ordine prodotto_ordine = new Prodotto_Ordine();
                prodotto_ordine.setEmail_utente(utente.getEmail());
                prodotto_ordine.setData_ora_creazione(date);
                prodotto_ordine.setCodice_ordine(codice_ordine);

                for (Gioco giocoDaAcquistare : giochiDaAcquistare) {
                    prodotto_ordine.setCodice_gioco(giocoDaAcquistare.getCodice_gioco());

                    if (from.equals("carrello"))
                        prodotto_ordine.setQuantita(carrello.get(gioco.getCodice_gioco()));
                    else
                        prodotto_ordine.setQuantita(1);

                    prodotto_ordineDAO.doSave(prodotto_ordine);
                }

                // Aggiorno il saldo dell'utente sia in sessione che nel db
                utente.setSaldo(utente.getSaldo() - prezzoTotale);
                session.setAttribute("user", utente);
                UtenteDAO utenteDAO = new UtenteDAO(source);
                HashMap<String, Double> map = new HashMap<>();
                map.put("saldo", utente.getSaldo());
                utenteDAO.doUpdate(map, "email = '" + utente.getEmail() + "'");

                // Svuoto il carrello in sessione e quello nel db <=> from == "carrello"
                if (from.equals("carrello")) {
                    session.setAttribute("carrello", new HashMap<String, Integer>());
                    session.setAttribute("quantita_carrello", 0);

                    CarrelloDAO carrelloDAO = new CarrelloDAO(source);
                    ProdottoDAO prodottoDAO = new ProdottoDAO(source);

                    carrelloDAO.doDelete("email_utente = '" + utente.getEmail() + "'");
                    prodottoDAO.doDelete("email_utente = '" + utente.getEmail() + "'");
                }

                session.setAttribute("msg-success", "Ordine <b>#" + ordine.getCodice_ordine() + "</b> effettutato con successo!");

            } else {
                session.setAttribute("msg-error", "Qualcosa è andato storto");
                response.sendRedirect(request.getRequestURI());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String generaCodiceOrdine() {
        String alfabeto = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = 6;

        for (int i = 0; i < length; i++)
            sb.append(alfabeto.charAt(random.nextInt(alfabeto.length())));

        return sb.toString();
    }

    private void controllaSeLoggato(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            if (session == null)
                session = request.getSession(true);

            session.setAttribute("msg-error", "Per poter effettuare un acquisto devi accedere al tuo account!");
        }

        response.sendRedirect(request.getContextPath() + "/");

    }
}
