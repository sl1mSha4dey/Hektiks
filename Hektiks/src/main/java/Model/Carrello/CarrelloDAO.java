package Model.Carrello;

import Model.Storage.DAO;
import Model.Storage.SQLDAO;
import Utils.InvalidPrimaryKeyException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static Model.Storage.Entities.CARRELLI;

public class CarrelloDAO extends SQLDAO implements DAO<Carrello> {

    public CarrelloDAO(DataSource source) {
        super(source);
    }

    @Override
    public List<Carrello> doRetrieveByCondition(String condition) throws SQLException {

        return genericDoRetrieveByCondition(CARRELLI, condition, new CarrelloExtractor(), this.source);
    }

    @Override
    public Carrello doRetrieveByKey(String... key) throws SQLException, InvalidPrimaryKeyException {

        if(key == null || key.length != 2)
            throw new InvalidPrimaryKeyException();

        List<Carrello> carrello = doRetrieveByCondition(
                String.format("%s.email_utente = '%s' AND %s.data_creazione = '%s'", CARRELLI, key[0], CARRELLI, key[1]));
        return carrello.isEmpty() ? null : carrello.get(0);
    }

    @Override
    public List<Carrello> doRetrieveAll() throws SQLException {

        return doRetrieveByCondition("TRUE");
    }


    @Override
    public boolean doSave(Carrello obj) throws SQLException {

        return genericDoSave(CARRELLI, obj.toHashMap(), this.source);
    }

    @Override
    public boolean doUpdate(Map<String, ?> values, String condition) throws SQLException {

        return genericDoUpdate(CARRELLI, condition, values, this.source);
    }

    @Override
    public boolean doSaveOrUpdate(Carrello obj) throws SQLException {

        if (doRetrieveByKey(obj.getEmail_utente(), obj.getData_creazione().toString()) == null)
            return doSave(obj);

        return doUpdate(obj.toHashMap(),
                String.format("%s.email_utente = '%s' AND %s.data_creazione = '%s'",
                        CARRELLI, obj.getEmail_utente(), CARRELLI, obj.getData_creazione().toString()));
    }

    @Override
    public boolean doDelete(String condition) throws SQLException {

        return genericDoDelete(CARRELLI, condition, this.source);
    }
}
