package com.zwsoft.connector.beans;

import cn.hutool.core.util.PageUtil;
import cn.hutool.db.PageResult;
import cn.hutool.core.codec.Base64;
import com.google.common.collect.ImmutableList;
import com.zwsoft.connector.utils.ReflectUtils;
import com.zwsoft.connector.vo.SQLiteBaseVO;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import com.alibaba.fastjson.JSON;



public class SQLiteCommand {
    private static final List<String> RESERVED_KEYS = ImmutableList.<String>builder()
            .add("id").add("v").add("ctime").add("utime").build();

    private Connection connection;

    private SQLiteCommand() {}

    public SQLiteCommand(Connection connection) {
        this.connection = connection;
    }

    public <T extends SQLiteBaseVO> void ensureTable(Class<T> clazz) {
        ensureTable(clazz, new ArrayList<>());
    }

    public <T extends SQLiteBaseVO> void ensureTable(Class<T> clazz, List<String> indexes) {
        String tableName = tableName(clazz);
        Map<String, String> colSqlMap = colSqlMap(clazz);
        // query if table exists
        try {
            if (existTable(clazz)) {
                return;
            }
        } catch (SQLException e) {
            throw new RuntimeException("cannot check sqlite table " + tableName);
        }
        StringBuilder createSql = new StringBuilder();
        // pre fields id,v
        createSql.append("create table ").append(tableName).append(" (\n");
        createSql.append("  ").append("id ").append(colSqlMap.get("id")).append(",\n");
        createSql.append("  ").append("v ").append(colSqlMap.get("v")).append(",\n");
        for (Map.Entry<String, String> entry : colSqlMap.entrySet()) {
            String ck = entry.getKey();
            String desc = entry.getValue();
            if (!RESERVED_KEYS.contains(ck)) {
                createSql.append("  ").append(ck).append(" ").append(desc).append(",\n");
            }
        }
        // suf fields ctime,utime
        createSql.append("  ").append("ctime ").append(colSqlMap.get("ctime")).append(",\n");
        createSql.append("  ").append("utime ").append(colSqlMap.get("utime")).append("\n");
        createSql.append(");");
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(createSql.toString());
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("cannot create table " + tableName + " (" + e.getMessage() + ")");
        } finally {
            closeStmt(stmt);
        }

        List<String> idxSqls = buildIndexSql(tableName, indexes);
        if (!idxSqls.isEmpty()) {
            PreparedStatement stmt1 = null;
            try {
                for (String idxSql : idxSqls) {
                    stmt1 = connection.prepareStatement(idxSql);
                    stmt1.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException("cannot create index for table " + tableName + " (" + e.getMessage() + ")");
            } finally {
                closeStmt(stmt1);
            }
        }
    }

    private static <T extends SQLiteBaseVO> String tableName(Class<T> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    public <T extends SQLiteBaseVO> boolean existTable(Class<T> clazz) throws SQLException {
        String tableName = tableName(clazz);
        String sql = "select * from sqlite_master where `type`='table' and `name`=?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, tableName);
        ResultSet rs = stmt.executeQuery();
        boolean exist = rs.next();
        closeStmt(stmt);
        return exist;
    }

    private static <T extends SQLiteBaseVO> Map<String, String> colSqlMap(Class<T> clazz) {
        Map<String, String> colSqlMap = new HashMap<>();
        colSqlMap.put("id", "integer primary key autoincrement not null");
        colSqlMap.put("v", "integer not null default 0");
        colSqlMap.put("ctime", "integer not null default 0");
        colSqlMap.put("utime", "integer not null default 0");
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String colName = field.getName().toLowerCase();
            if (!colSqlMap.containsKey(colName)) {
                Class<?> type = field.getType();
                if (type.equals(String.class) || type.isEnum()) {
                    colSqlMap.put(colName, "text not null");
                } else if (type.equals(Integer.class)) {
                    colSqlMap.put(colName, "integer not null default 0");
                } else if (type.isArray()) {
                    Class<?> compType = type.getComponentType();
                    if (compType.isPrimitive() && compType.equals(byte.class)) {
                        colSqlMap.put(colName, "text not null");
                    }
                }
            }
        }
        return colSqlMap;
    }

    private static List<String> buildIndexSql(String tableName, List<String> indexes) {
        List<String> idxSqls = new ArrayList<>();
        for (String idx : indexes) {
            StringBuilder idxSql = new StringBuilder();
            String[] ss = idx.split("_");
            int beginPos = 0;
            if (Objects.equals(ss[0], "u")) {
                idxSql.append("create unique index `uidx_");
                beginPos = 1;
            } else {
                idxSql.append("create index `idx_");
            }
            StringBuilder cols = new StringBuilder();
            for (int i=beginPos; i<ss.length; i++) {
                String col = ss[i].toLowerCase();
                idxSql.append(col);
                cols.append('`').append(col).append('`');
                if (i != ss.length - 1) {
                    idxSql.append('_');
                    cols.append(',');
                }
            }
            idxSql.append("` on ").append(tableName).append(" (").append(cols).append(");");
            idxSqls.add(idxSql.toString());
        }
        return idxSqls;
    }

    public <T extends SQLiteBaseVO> boolean insertOrUpdate(T obj) {
        Long id = obj.getId();
        if (null != id && id > 0L) {
            obj.setV(obj.getV() + 1L);
            obj.setUtime(new Date());
            try {
                executeUpdate(obj);
            } catch (SQLException e) {
                throw new RuntimeException("update data failed! " + JSON.toJSONString(obj));
            }
        } else {
            obj.setId(null);
            try {
                executeInsert(obj);
            } catch (SQLException e) {
                throw new RuntimeException("insert data failed! " + e.getMessage() + JSON.toJSONString(obj));
            }
        }
        return true;
    }

    private <T extends SQLiteBaseVO> void executeUpdate(T obj) throws SQLException {
        connection.setAutoCommit(true);
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String tableName = tableName(obj.getClass());
        sb.append("update ").append(tableName).append(" set ");
        sb.append("`v`=?,");
        Object vv = ReflectUtils.getFieldValue(obj, "v");
        params.add(vv);
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if (!RESERVED_KEYS.contains(name)) {
                sb.append('`').append(name).append("`=?,");
                params.add(ReflectUtils.getFieldValue(obj, field.getName()));
            }
        }
        sb.append("`utime`=?").append(" where `id`=? and `v`=?");
        params.add(ReflectUtils.getFieldValue(obj, "utime"));
        Object id = ReflectUtils.getFieldValue(obj, "id");
        params.add(id);
        params.add((Long) vv - 1L);
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sb.toString());
            int pl = params.size();
            for (int i=0; i<pl; i++) {
                Object v = params.get(i);
                conditionalBind(stmt, i+1, v);
            }
            stmt.execute();
        } finally {
            closeStmt(stmt);
        }
    }

    private <T extends SQLiteBaseVO> void executeInsert(T obj) throws SQLException {
        connection.setAutoCommit(true);
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder();
        StringBuilder vsb = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sb.append("insert into ").append(tableName(obj.getClass())).append(" (`v`,");
        vsb.append(" values (?,");
        params.add(1L);
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if (!RESERVED_KEYS.contains(name)) {
                sb.append('`').append(name).append("`,");
                vsb.append("?,");
                params.add(ReflectUtils.getFieldValue(obj, field.getName()));
            }
        }
        sb.append("`ctime`,`utime`)");
        vsb.append("?,?)");
        Object preDefinedCtime = ReflectUtils.getFieldValue(obj, "ctime");
        if (null == preDefinedCtime) {
            preDefinedCtime = new Date();
        }
        params.add(preDefinedCtime);
        Object preDefinedUtime = ReflectUtils.getFieldValue(obj, "utime");
        if (null == preDefinedUtime) {
            preDefinedUtime = preDefinedCtime;
        }
        params.add(preDefinedUtime);
        sb.append(vsb);
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sb.toString());
            int pl = params.size();
            for (int i=0; i<pl; i++) {
                Object v = params.get(i);
                conditionalBind(stmt, i+1, v);
            }
            stmt.execute();
            ResultSet rs = stmt.getGeneratedKeys();
            if (!rs.next()) {
                throw new RuntimeException("cannot get id for insert " + JSON.toJSONString(obj));
            }
            obj.setId(rs.getLong(1));
        } finally {
            closeStmt(stmt);
        }
    }

    private void conditionalBind(PreparedStatement stmt, int idx, Object val) throws SQLException {
        if (val instanceof Long) {
            stmt.setLong(idx, (Long) val);
        } else if (val instanceof Integer) {
            stmt.setInt(idx, (Integer) val);
        } else if (val instanceof String) {
            stmt.setString(idx, (String) val);
        } else if (val instanceof byte[]) {
            String b64 = Base64.encode((byte[]) val);
            stmt.setString(idx, b64);
        } else if (val instanceof Date) {
            long ts =  ((Date) val).getTime();
            stmt.setLong(idx, ts);
        } else {
            Class<?> clazz = val.getClass();
            if (clazz.isEnum()) {
                stmt.setString(idx, val.toString());
            } else {
                throw new RuntimeException("illegal value for sqlite " + val + " @" + idx);
            }
        }
    }

    private Object conditionalGet(ResultSet rs, Field field) throws SQLException {
        Object res = null;
        String fieldName = field.getName();
        Class<?> clazz = field.getType();
        if (clazz.equals(Long.class)) {
            res = rs.getLong(fieldName);
        } else if (clazz.equals(Integer.class)) {
            res = rs.getInt(fieldName);
        } else if (clazz.equals(String.class)) {
            res = rs.getString(fieldName);
        } else if (clazz.isArray() && clazz.getComponentType().equals(byte.class)) {
            res = Base64.decode(rs.getString(fieldName));
        } else if (clazz.isEnum()) {
            try {
                Method valMethod = clazz.getMethod("valueOf", String.class);
                res = valMethod.invoke(clazz, rs.getString(fieldName));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private <T extends SQLiteBaseVO> List<T> extractResultSet(Class<T> clazz, ResultSet rs) throws SQLException {
        List<T> res = new ArrayList<>();
        try {
            while (rs.next()) {
                T item = clazz.newInstance();
                item.setId(rs.getLong("id"));
                item.setV(rs.getLong("v"));
                item.setCtime(new Date(rs.getLong("ctime")));
                item.setUtime(new Date(rs.getLong("utime")));
                Field[] fields = item.getClass().getDeclaredFields();
                for (Field field : fields) {
                    String fieldName = field.getName();
                    Object v = conditionalGet(rs, field);
                    ReflectUtils.setFieldValue(item, fieldName, v);
                }
                res.add(item);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return res;
    }

    public <T extends SQLiteBaseVO> int batchInsertOrUpdate(List<T> objs) {
        int suc = 0;
        for (T obj : objs) {
            if (insertOrUpdate(obj)) {
                suc++;
            }
        }
        return suc;
    }

    public <T extends SQLiteBaseVO> T find(Class<T> clazz, Long id) {
        String ms = "where id=?";
        List<Object> binder = new ArrayList<>();
        binder.add(id);
        return findOne(clazz, ms, binder);
    }

    public <T extends SQLiteBaseVO> List<T> find(Class<T> clazz, List<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        StringBuilder ms = new StringBuilder();
        ms.append("where id in (");
        List<Object> binder = new ArrayList<>();
        int len = ids.size();
        for (int i=0; i<len; i++) {
            ms.append('?');
            binder.add(ids.get(i));
            if (i < len - 1) {
                ms.append(',');
            }
        }
        ms.append(')');
        return find(clazz, ms.toString(), binder);
    }

    public <T extends SQLiteBaseVO> T findOne(Class<T> clazz, String ms, List<Object> binder) {
        PageResult<T> pr = findPage(clazz, ms, binder, 1, 1, false);
        if (pr.isEmpty()) {
            return null;
        }
        return pr.get(0);
    }

    public <T extends SQLiteBaseVO> List<T> find(Class<T> clazz, String ms, List<Object> binder) {
        PageResult<T> pr = findPage(clazz, ms, binder, 1, 10000, false);
        return new ArrayList<>(pr);
    }

    public <T extends SQLiteBaseVO> PageResult<T> findPage(
            Class<T> clazz, String ms, List<Object> binder,
            int page, int size, boolean countTotal) {
        PageResult<T> pr = new PageResult<>();
        StringBuilder commonStr = new StringBuilder();
        String tbn = tableName(clazz);
        if (page < 1) {
            page = 1;
        }
        if (null == binder) {
            binder = new ArrayList<>();
        }
        int offset = (page - 1) * size;
        commonStr.append("from `").append(tbn).append("` ").append(ms);
        StringBuilder sql = new StringBuilder();
        sql.append("select * ").append(commonStr);
        if (offset == 0 && size == 1) {
            sql.append(" limit 1");
        } else {
            sql.append(" limit ").append(offset).append(',').append(size);
        }
        PreparedStatement stmt = null;
        PreparedStatement cntStmt = null;
        try {
            stmt = connection.prepareStatement(sql.toString());
            int bl = binder.size();
            for (int i=0; i<bl; i++) {
                conditionalBind(stmt, i+1, binder.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            List<T> data = extractResultSet(clazz, rs);
            pr.addAll(data);
            pr.setPage(page);
            pr.setPageSize(size);
            pr.setTotal(0);
            pr.setTotalPage(0);
            if (countTotal) {
                String cntSql = "select count(*) " + commonStr;
                cntStmt = connection.prepareStatement(cntSql);
                for (int i=0; i<bl; i++) {
                    conditionalBind(cntStmt, i+1, binder.get(i));
                }
                ResultSet cntRs = stmt.executeQuery();
                if (!cntRs.next()) {
                    throw new RuntimeException("sqlite findPage error [1]: " + sql);
                }
                pr.setTotal(cntRs.getInt(1));
                pr.setTotalPage(PageUtil.totalPage(pr.getTotal(), pr.getPageSize()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("sqlite findPage error [2]: " + e.getMessage() + " " + sql);
        } finally {
            closeStmt(stmt);
            closeStmt(cntStmt);
        }
        return pr;
    }

    public <T extends SQLiteBaseVO> void delete(T obj) {
        String tb = tableName(obj.getClass());
        StringBuilder sb = new StringBuilder();
        sb.append("delete from `").append(tb).append("` where id=?");
        PreparedStatement stmt = null;
        try {
            connection.setAutoCommit(true);
            stmt = connection.prepareStatement(sb.toString());
            stmt.setLong(1, obj.getId());
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("sqlite delete failed! " + JSON.toJSONString(obj));
        } finally {
            closeStmt(stmt);
        }
    }

    public <T extends SQLiteBaseVO> void delete(List<T> objs) {
        if (null == objs || objs.isEmpty()) {
            return ;
        }
        String tb = tableName(objs.get(0).getClass());
        StringBuilder sb = new StringBuilder();
        sb.append("delete from `").append(tb).append("` where id in (");
        List<Object> binder = new ArrayList<>();
        int len = objs.size();
        for (int i=0; i<len; i++) {
            sb.append('?');
            binder.add(objs.get(i).getId());
            if (i < len - 1) {
                sb.append(',');
            }
        }
        sb.append(')');
        PreparedStatement stmt = null;
        try {
            connection.setAutoCommit(true);
            stmt = connection.prepareStatement(sb.toString());
            for (int i=0; i<len; i++) {
                stmt.setLong(i+1, (Long) binder.get(i));
            }
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("sqlite batch delete failed! " + JSON.toJSONString(objs));
        } finally {
            closeStmt(stmt);
        }
    }

    private void closeStmt(PreparedStatement stmt) {
        if (null != stmt) {
            try {
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
