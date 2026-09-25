package hu.elte.ik.thesis.cinegrade.infra.services.database;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.CatalogDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CatalogDbService {

    private final CatalogDao dao;
    private final Connection connection;

    public CatalogDbService() {
        connection = AppDatabase.getInstance().getConnection();
        dao = new CatalogDao(connection);
    }

    public List<Catalog> getRecentCatalogs() {

        List<Catalog> catalogList = new ArrayList<>();

        try {
            catalogList = dao.findAll();
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "Catalogs");
        }

        catalogList.sort(Comparator.comparing(Catalog::getLastOpenedAt));
        return catalogList;
    }
}
