package Model.Gioco_Genere;

import Model.Storage.DAO;
import Model.Storage.SQLDAO;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Model.Storage.Entities.GIOCHI_GENERE;

public class Gioco_GenereDAO extends SQLDAO implements DAO<Gioco_Genere> {

    public Gioco_GenereDAO(DataSource source) {
        super(source);
    }

    @Override
    public List<Gioco_Genere> doRetrieveByCondition(String condition) throws SQLException {

        return genericDoRetrieveByCondition(GIOCHI_GENERE, condition, new Gioco_GenereExtractor(), this.source);
    }

    @Override
    public Gioco_Genere doRetrieveByKey(Object... key) throws SQLException {

        List<Gioco_Genere> gioco_Genere = doRetrieveByCondition(GIOCHI_GENERE + ".codice_gioco = " + "'" + key.toString() + "'");
        return gioco_Genere.isEmpty() ? null : gioco_Genere.get(0);
    }

    @Override
    public List<Gioco_Genere> doRetrieveAll() throws SQLException {

        return doRetrieveByCondition("TRUE");
    }

    @Override
    public boolean doSave(Gioco_Genere obj) throws SQLException {

        return genericDoSave(GIOCHI_GENERE, new HashMap<>() {{
                    put("codice_gioco", obj.getCodice_gioco());
                    put("nome_genere", obj.getNome_genere());

                }},
                this.source);
    }

    @Override
    public boolean doUpdate(Map<String, ?> values, String condition) throws SQLException {

        return genericDoUpdate(GIOCHI_GENERE, condition, values, this.source);
    }

    @Override
    public boolean doSaveOrUpdate(Gioco_Genere obj) throws SQLException {

        if (doRetrieveByKey(obj.getCodice_gioco()) == null)
            return doSave(obj);

        return doUpdate(new HashMap<>() {{
            put("codice_gioco", obj.getCodice_gioco());
            put("nome_genere", obj.getNome_genere());

        }}, GIOCHI_GENERE + ".codice_gioco = " + "'" + obj.getCodice_gioco() + "'");
    }

    @Override
    public boolean doDelete(String condition) throws SQLException {

        return genericDoDelete(GIOCHI_GENERE, condition, this.source);
    }
}
