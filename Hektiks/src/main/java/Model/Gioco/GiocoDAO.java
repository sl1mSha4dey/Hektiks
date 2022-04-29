package Model.Gioco;

import Model.Storage.DAO;
import Model.Storage.SQLDAO;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GiocoDAO extends SQLDAO implements DAO<Gioco> {

    public GiocoDAO(DataSource source) {
        super(source);
    }

    @Override
    public List<Gioco> doRetrieveByCondition(String condition) throws SQLException {
        return null;
    }

    @Override
    public List<Gioco> doRetrieveAll() throws SQLException {
        return null;
    }

    @Override
    public Optional<Gioco> doRetrieveByKey(Gioco obj) throws SQLException {
        return Optional.empty();
    }

    @Override
    public boolean doSave(Gioco obj) throws SQLException {
        return false;
    }

    @Override
    public boolean doUpdate(Map<String, ?> values, String condition) throws SQLException {
        return false;
    }

    @Override
    public boolean doDelete(String condition) throws SQLException {
        return false;
    }
}