package com.codelab.core.jpa;

import java.net.URI;
import java.net.URISyntaxException;

final class JdbcUrls {

    private JdbcUrls() {}

    /** Replaces the database name (path segment) of a jdbc:<subprotocol>://host:port/db(?query) URL. */
    static String withDatabase(String jdbcUrl, String databaseName) {
        if (!jdbcUrl.startsWith("jdbc:")) {
            throw new IllegalArgumentException("Not a JDBC URL: " + jdbcUrl);
        }
        String withoutJdbcPrefix = jdbcUrl.substring("jdbc:".length()); // postgresql://host:5432/codelab?params

        URI uri;
        try {
            uri = new URI(withoutJdbcPrefix);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed JDBC URL: " + jdbcUrl, e);
        }

        URI rebuilt;
        try {
            rebuilt = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    "/" + databaseName, uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not rebuild JDBC URL for database " + databaseName, e);
        }

        return "jdbc:" + rebuilt;
    }
}
