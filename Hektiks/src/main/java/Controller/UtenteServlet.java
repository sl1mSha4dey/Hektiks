package Controller;

import Model.Gioco.GiocoDAO;
import Model.Ordine.Ordine;
import Model.Ordine.OrdineDAO;
import Model.Prodotto_Ordine.Prodotto_OrdineDAO;
import Model.Recensione.Recensione;
import Model.Recensione.RecensioneDAO;
import Model.Utente.Utente;
import Model.Utente.UtenteDAO;
import Utils.Logger.Logger;
import Utils.LoginChecker;
import Utils.PasswordEncrypt;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;


@MultipartConfig(
        fileSizeThreshold = 1024 * 1024, // 1 MB
        maxFileSize = 1024 * 1024,      // 1 MB
        maxRequestSize = 1024 * 1024 * 5   // 5 MB
)

public class UtenteServlet extends HttpServlet implements LoginChecker {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        Logger.consoleLog(Logger.INFO, "UTENTE SERVLET DO GET");

        if (!controllaSeLoggato(request, response, "", false)) return;

        String part = request.getParameter("part");
        DataSource source = (DataSource) getServletContext().getAttribute("DataSource");
        Utente user = (Utente) request.getSession().getAttribute("user");

        // se 'part' è nullo, mostro la dashboard
        if (part == null) {

            try {
                List<Recensione> recensioni = new RecensioneDAO(source).doRetrieveByCondition("email_utente = '" + user.getEmail() + "'");
                List<Ordine> ordini = new OrdineDAO(source).doRetrieveByCondition("email_utente = '" + user.getEmail() + "'");

                double totaleSpeso = ordini.stream().map(Ordine::getPrezzo_totale).reduce(0.0D, Double::sum);

                request.setAttribute("recensioni", recensioni.size());
                request.setAttribute("ordini", ordini.size());
                request.setAttribute("totaleSpeso", totaleSpeso);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (part.equals("orders")) {
            try {
                request.setAttribute("ordini", new OrdineDAO(source).doRetrieveByCondition("email_utente = '" + user.getEmail() + "'"));
                request.setAttribute("prodottoOrdineDAO", new Prodotto_OrdineDAO(source));
                request.setAttribute("giocoDAO", new GiocoDAO(source));
                request.setAttribute("part", "parts/orders.jsp");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (part.equals("settings")) {
            request.setAttribute("part", "parts/settings.jsp");
        }

        request.setAttribute("title", "Hektiks | " + user.getUsername());
        request.setAttribute("page", "utente/dashboard.jsp");
        request.setAttribute("scripts", new String[]{"user.js"});

        request.getRequestDispatcher("WEB-INF/index.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        Logger.consoleLog(Logger.INFO, "UTENTE SERVLET DO POST");

        if (!controllaSeLoggato(request, response, "", false)) return;

        HttpSession session = request.getSession(false);
        Utente utente = (Utente) session.getAttribute("user");
        UtenteDAO utenteDAO = new UtenteDAO((DataSource) getServletContext().getAttribute("DataSource"));

        //mappa per i filed del db da aggiornare
        HashMap<String, String> map = new HashMap<>();
        boolean update = false;

        /* IMMAGINE PROFILO START */
        Part filePart = request.getPart("profile_pic");

        // L'utente ha effettivamente caricato un'file
        if (filePart != null && filePart.getSize() > 0) {
            String fileName = filePart.getSubmittedFileName();

            // Controllo che il file sia un'immagine
            if (!fileName.endsWith(".jpeg") && !fileName.endsWith(".jpg") && !fileName.endsWith(".png")) {
                session.setAttribute("msg-error", "Il file caricato non è un'immagine");
                response.sendRedirect(request.getContextPath() + "/utente?part=settings");
                return;
            }

            if (filePart.getSize() > 1048576) {
                session.setAttribute("msg-error", "La dimensione dell'immagine è troppo grande");
                response.sendRedirect(request.getContextPath() + "/utente?part=settings");
                return;
            }

            String appPath = request.getServletContext().getRealPath("/");
            String newPicPath;

            File userProfilePicFolder = new File(appPath + "/assets/uploads/users/" + utente.getUsername());

            //se non ha un folder per l'utente, lo creo
            if (!userProfilePicFolder.exists()) {

                if (!userProfilePicFolder.mkdir()) {

                    session.setAttribute("msg-error", "Errore durante l'aggiornamento del profilo!");
                    response.sendRedirect(request.getContextPath() + "/utente?part=settings");

                    return;
                }
                //se l'utente non aveva una propic la salvo per la prima volta
                else {
                    salvaImmagineRidimensionata(fileName, userProfilePicFolder, filePart);
                    map.put("profile_pic", utente.getProfile_pic());
                    update = true;
                }

            }
            // se l'utente aveva già un'immagine
            else {

                //salvo file di backup
                File[] files = userProfilePicFolder.listFiles();
                File oldFile = null;

                // La directory conterrà sempre e solo un file
                if (files != null && files.length > 0) {
                    oldFile = files[0];
                }

                newPicPath = salvaImmagineRidimensionata(fileName, userProfilePicFolder, filePart);
                map.put("profile_pic", utente.getProfile_pic());
                update = true;

                //cancello il file di backup
                if (new File(newPicPath).exists()) {
                    if (oldFile != null)
                        oldFile.delete();
                } else {

                    session.setAttribute("msg-error", "Errore durante l'aggiornamento del profilo!");
                    response.sendRedirect(request.getContextPath() + "/utente?part=settings");
                    return;
                }
            }

            utente.setProfile_pic("/" + utente.getUsername() + "/" + fileName);
        }
        /*IMMAGINE PROFILO END */

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        // Serve per aggiornare l'utente nel db
        String currentEmail = utente.getEmail();

        // Controllo che lo username non sia già in uso da un altro utente
        if (username != null && !username.equals("")) {

            if (!controllaValiditaCampi("username", username, currentEmail, utenteDAO)) {
                session.setAttribute("msg-error", "Lo username è già in uso");
                response.sendRedirect(request.getContextPath() + "/utente?part=settings");
                return;
            } else {

                if (!username.equals(utente.getUsername())) {
                    utente.setUsername(username);
                    map.put("username", utente.getUsername());
                    update = true;
                }

            }
        }

        if (email != null && !email.equals("")) {

            if (!controllaValiditaCampi("email", email, currentEmail, utenteDAO)) {
                session.setAttribute("msg-error", "L'email è già in uso");
                response.sendRedirect(request.getContextPath() + "/utente?part=settings");
                return;
            } else {

                if (!email.equals(utente.getEmail())) {
                    utente.setEmail(email);
                    map.put("email", utente.getEmail());
                    update = true;
                }
            }
        }

        if (password != null && !password.equals("")) {

            if (!password.matches("(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}")) {
                session.setAttribute("msg-error", "La password non rispetta i requisti");
                response.sendRedirect(request.getContextPath() + "/utente?part=settings");
                return;
            }

            try {

                if (!PasswordEncrypt.sha1(password).equals(utente.getPassword_utente())) {

                    utente.setPassword_utente(password);
                    map.put("password_utente", utente.getPassword_utente());
                    update = true;
                }

            } catch (NoSuchAlgorithmException e) {

                e.printStackTrace();
            }
        }

        try {

            if (update)
                utenteDAO.doUpdate(map, "email = '" + currentEmail + "'");

            session.setAttribute("user", utente);
            session.setAttribute("msg-success", "Profilo aggiornato con successo!");

        } catch (SQLException e) {
            session.setAttribute("msg-error", "Errore durante l'aggiornamento del profilo!");
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/utente?part=settings");
    }

    private boolean controllaValiditaCampi(String campo, String parametro, String email, UtenteDAO utenteDAO) {

        // Controllo che lo username non sia già in uso da un altro utente
        try {
            List<Utente> utenti = utenteDAO.doRetrieveByCondition(campo + " = '" + parametro + "' AND email <> '" + email + "'");

            return utenti == null || utenti.size() == 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String salvaImmagineRidimensionata(String fileName, File userProfilePicFolder, Part filePart) throws IOException {

        String[] splitted = fileName.split("\\.");
        String ext = splitted[splitted.length - 1];
        String newPicPath = userProfilePicFolder.getPath() + File.separator + fileName;

        // Ridimensiono l'immagine a 360x360
        BufferedImage bufferedImage = ImageIO.read(filePart.getInputStream());
        ImageIO.write(creaCopiaRidimensionata(bufferedImage), ext, new File(newPicPath));

        return newPicPath;
    }

    private BufferedImage creaCopiaRidimensionata(BufferedImage image) {

        BufferedImage scaledBI = new BufferedImage(360, 360, image.getType());
        Graphics2D g = scaledBI.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(image, 0, 0, 360, 360, null);
        g.dispose();

        return scaledBI;
    }

}