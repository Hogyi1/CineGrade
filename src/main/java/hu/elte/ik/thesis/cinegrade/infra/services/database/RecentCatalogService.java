package hu.elte.ik.thesis.cinegrade.infra.services.database;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.RecentCatalogDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.SettingsDao;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class RecentCatalogService {

    private final RecentCatalogDao dao;
    private final SettingsDao settingsDao;
    private final Connection connection;
    private final String SETTING_KEY = "app.project_path";

    public RecentCatalogService() {
        connection = AppDatabase.getInstance().getConnection();
        dao = new RecentCatalogDao(connection);
        settingsDao = new SettingsDao(connection);
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

    public boolean removeCatalog(Catalog catalog) {
        if (catalog == null) {
            return false;
        }

        try {
            return dao.delete(catalog.getId());
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "removeCatalog: " + catalog.getCatalogName());
        }
    }

    public Optional<String> getSavedPath() {
        try {
            String value = settingsDao.get(SETTING_KEY);
            return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "Project path");
        }
    }

    public Catalog addCatalog(String name, Path path) {
        try {
            Optional<Catalog> catalog = dao.insert(name, path.toFile().getAbsolutePath());
            if (catalog.isPresent()) {
                return catalog.get();
            }
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "New Catalog");
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "");
        }
    }

    public void updateLastOpenedAt(int id, Instant now) {
        try {
            dao.updateLastOpenedAt(id, now);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "updateLastOpenedAt");
        }
    }
}
