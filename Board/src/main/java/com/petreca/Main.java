package com.petreca;

import com.petreca.persistence.config.ConnectionConfig;
import com.petreca.persistence.migration.MigrationStrategy;
import com.petreca.ui.MainMenu;

import java.sql.SQLException;

import static com.petreca.persistence.config.ConnectionConfig.getConnection;

public class Main {
    public static void main(String[] args) throws SQLException {
        try(var connection = getConnection()){
            new MigrationStrategy(connection).executeMigration();
        }
        try(var connection = getConnection()){
            new MainMenu().execute();
        }
    }
}
