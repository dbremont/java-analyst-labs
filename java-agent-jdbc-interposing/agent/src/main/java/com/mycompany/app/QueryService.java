package com.mycompany.app;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class QueryService {

    public static String handleQuery(String path) {
        try {
            // Parse query params: conn=1&q=SELECT+1
            String query = path.contains("?") ? path.split("\\?")[1] : "";
            String[] parts = query.split("&");
            int connId = -1;
            String sql = null;

            for (String p : parts) {
                if (p.startsWith("conn=")) {
                    connId = Integer.parseInt(p.substring(5));
                } else if (p.startsWith("q=")) {
                    sql = java.net.URLDecoder.decode(p.substring(2), "UTF-8");
                }
            }

            if (connId == -1 || sql == null) {
                return "{\"error\":\"missing params\"}";
            }

            // Get connection object
            Object conn = ConnectionRegistry.get(connId);
            if (conn == null) {
                return "{\"error\":\"connection not found\"}";
            }

            Object resultJson = null;
            Object stmt = null;
            Object rs = null;

            try {
                // 1. Create Statement
                Method createStatementMethod = conn.getClass().getMethod("createStatement");
                createStatementMethod.setAccessible(true);
                stmt = createStatementMethod.invoke(conn);

                // 2. Execute Query
                Method executeMethod = stmt.getClass().getMethod("execute", String.class);
                executeMethod.setAccessible(true);
                boolean isResultSet = (Boolean) executeMethod.invoke(stmt, sql);

                if (isResultSet) {
                    // 3. Get ResultSet
                    Method getResultSetMethod = stmt.getClass().getMethod("getResultSet");
                    getResultSetMethod.setAccessible(true);
                    rs = getResultSetMethod.invoke(stmt);
                    resultJson = resultSetToJson(rs);
                } else {
                    // 4. Get Update Count
                    Method getUpdateCountMethod = stmt.getClass().getMethod("getUpdateCount");
                    getUpdateCountMethod.setAccessible(true);
                    int updateCount = (Integer) getUpdateCountMethod.invoke(stmt);
                    
                    resultJson = "{\"status\":\"success\", \"updateCount\": " + updateCount + "}";
                }

            } catch (Exception e) {
                throw e;
            } finally {
                closeQuietly(rs);
                closeQuietly(stmt);
            }

            return resultJson != null ? resultJson.toString() : "{\"error\":\"no result\"}";

        } catch (Exception e) {
            // UNWRAP the InvocationTargetException to see the real error
            Throwable cause = e;
            if (e instanceof InvocationTargetException && e.getCause() != null) {
                cause = e.getCause();
            }
            
            String msg = cause.getMessage();
            if (msg == null) msg = cause.toString();
            return "{\"error\":\"" + msg.replace("\"", "\\\"") + "\"}";
        }
    }

    private static void closeQuietly(Object obj) {
        if (obj != null) {
            try {
                Method closeMethod = obj.getClass().getMethod("close");
                closeMethod.setAccessible(true);
                closeMethod.invoke(obj);
            } catch (Exception ignored) {}
        }
    }

    private static String resultSetToJson(Object rs) throws Exception {
        Class<?> rsClass = rs.getClass();
        
        Method metaMethod = rsClass.getMethod("getMetaData");
        metaMethod.setAccessible(true);
        Object meta = metaMethod.invoke(rs);
        
        Class<?> metaClass = meta.getClass();
        Method getColumnCountMethod = metaClass.getMethod("getColumnCount");
        getColumnCountMethod.setAccessible(true);
        Method getColumnLabelMethod = metaClass.getMethod("getColumnLabel", int.class);
        getColumnLabelMethod.setAccessible(true);

        int colCount = (Integer) getColumnCountMethod.invoke(meta);
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        Method nextMethod = rsClass.getMethod("next");
        nextMethod.setAccessible(true);
        Method getObjectMethod = rsClass.getMethod("getObject", int.class);
        getObjectMethod.setAccessible(true);

        boolean first = true;
        while ((Boolean) nextMethod.invoke(rs)) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) sb.append(",");
                String colName = (String) getColumnLabelMethod.invoke(meta, i);
                Object val = getObjectMethod.invoke(rs, i);
                sb.append("\"").append(colName).append("\":");
                if (val == null) {
                    sb.append("null");
                } else if (val instanceof Number || val instanceof Boolean) {
                    sb.append(val.toString());
                } else {
                    sb.append("\"").append(val.toString().replace("\"", "\\\"")).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}