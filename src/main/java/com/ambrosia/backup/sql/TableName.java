package com.ambrosia.backup.sql;

public record TableName(String schema, String table) {

    @Override
    public String toString() {
        return sql();
    }

    public String sql() {
        return "%s.%s".formatted(schema, table);
    }
}
