package com.zwsoft.connector.utils;

import cn.hutool.db.PageResult;
import com.zwsoft.connector.beans.SQLiteCommand;
import com.zwsoft.connector.beans.SQLiteSource;
import com.zwsoft.connector.vo.PersistVO;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class PersistUtils {
    private static SQLiteCommand command;
    private static ReentrantLock locker = new ReentrantLock();

    private PersistUtils() {
    }

    public static void persist() {
        Path parentDir = dirname();
        File dir = parentDir.toFile();
        if (!dir.exists()) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SQLiteCommand command = getCommand();
        List<String> indexes = new ArrayList<>();
        indexes.add("key");
        command.ensureTable(PersistVO.class, indexes);
    }

    public static SQLiteCommand getCommand() {
        if (null == command) {
            SQLiteSource source = ContextUtils.getBean(SQLiteSource.class);
            if (null == source) {
                throw new RuntimeException("cannot get command for persist [1]");
            }
            Connection connection = source.getPersistConnection();
            if (null == connection) {
                throw new RuntimeException("cannot get command for persist [2]");
            }
            command = new SQLiteCommand(connection);
        }
        return command;
    }

    public static Path dirname() {
        return Paths.get(System.getProperty("user.dir"), ".zwconnector");
    }

    public static void set(String key, String val) {
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            PersistVO vo = command.findOne(PersistVO.class, "where `key`=?", binder);
            if (null == vo) {
                vo = new PersistVO();
                vo.setKey(key);
                vo.setIdx(0);
            }
            vo.setVal(val);
            command.insertOrUpdate(vo);
        } finally {
            locker.unlock();
        }
    }

    public static void set(String key, List<String> vals) {
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            List<PersistVO> vos = command.find(PersistVO.class, "where `key`=?", binder);
            if (!vos.isEmpty()) {
                command.delete(vos);
            }
            vos.clear();
            int len = vals.size();
            for (int i = 0; i < len; i++) {
                PersistVO vo = new PersistVO();
                vo.setKey(key);
                vo.setIdx(i + 1);
                vo.setVal(vals.get(i));
                vos.add(vo);
            }
            command.batchInsertOrUpdate(vos);
        } finally {
            locker.unlock();
        }
    }

    public static void set(String key, int index, String val) {
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            binder.add(index);
            PersistVO vo = command.findOne(PersistVO.class, "where `key`=? and `idx`=?", binder);
            if (null == vo) {
                // can not set not exist vo by index
                return;
            }
            vo.setVal(val);
            command.insertOrUpdate(vo);
        } finally {
            locker.unlock();
        }
    }

    public static void insert(String key, int index, String val) {
        locker.lock();
        try {
            PersistVO insertVo;
            List<PersistVO> needUpdateVos;
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            binder.add(index);
            needUpdateVos = command.find(PersistVO.class, "where `key`=? and `idx`>=?", binder);
            if (needUpdateVos.isEmpty()) {
                // 说明不存在对应index的对象，不能进行操作
                return;
            }
            // 先将老数据后移
            for (PersistVO ovo : needUpdateVos) {
                ovo.setIdx(ovo.getIdx() + 1);
            }
            command.batchInsertOrUpdate(needUpdateVos);
            insertVo = new PersistVO();
            insertVo.setKey(key);
            insertVo.setIdx(index);
            insertVo.setVal(val);
            command.insertOrUpdate(insertVo);
        } finally {
            locker.unlock();
        }
    }

    public static void after(String key, int index, String val) {
        locker.lock();
        try {
            PersistVO afterVo;
            List<PersistVO> queryVos;
            List<PersistVO> needUpdateVos = new ArrayList<>();
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            binder.add(index);
            queryVos = command.find(PersistVO.class, "where `key`=? and `idx`>=?", binder);
            if (queryVos.isEmpty()) {
                // 说明没有当前index对象，不能进行写入
                return;
            }
            for (PersistVO qvo : queryVos) {
                if (!Objects.equals(index, qvo.getIdx())) {
                    qvo.setIdx(qvo.getIdx() + 1);
                    needUpdateVos.add(qvo);
                }
            }
            afterVo = new PersistVO();
            afterVo.setKey(key);
            afterVo.setIdx(index + 1);
            afterVo.setVal(val);
            command.insertOrUpdate(afterVo);
            if (!needUpdateVos.isEmpty()) {
                command.batchInsertOrUpdate(needUpdateVos);
            }
        } finally {
            locker.unlock();
        }
    }

    public static void append(String key, String val) {
        locker.lock();
        try {
            PersistVO maxIdxVo;
            PersistVO appendVo = new PersistVO();
            appendVo.setKey(key);
            appendVo.setVal(val);
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            maxIdxVo = command.findOne(PersistVO.class, "where `key`=? order by idx desc", binder);
            if (null == maxIdxVo) {
                // 默认新增一个idx=1的
                appendVo.setIdx(1);
            } else {
                appendVo.setIdx(maxIdxVo.getIdx() + 1);
            }
            command.insertOrUpdate(appendVo);
        } finally {
            locker.unlock();
        }
    }

    public static void prepend(String key, String val) {
        locker.lock();
        try {
            // 当前所有的对象都要更新idx
            List<PersistVO> needUpdateVos;
            PersistVO prependVo = new PersistVO();
            prependVo.setKey(key);
            prependVo.setIdx(1);
            prependVo.setVal(val);
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            needUpdateVos = command.find(PersistVO.class, "where `key`=?", binder);
            if (!needUpdateVos.isEmpty()) {
                for (PersistVO uvo : needUpdateVos) {
                    uvo.setIdx(uvo.getIdx() + 1);
                }
                command.batchInsertOrUpdate(needUpdateVos);
            }
            command.insertOrUpdate(prependVo);
        } finally {
            locker.unlock();
        }
    }

    public static void delete(String key) {
        locker.lock();
        try {
            // 只能删除单值key
            List<PersistVO> queryVos;
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            queryVos = command.find(PersistVO.class, "where `key`=?", binder);
            if (queryVos.size() == 1) {
                command.delete(queryVos.get(0));
            }
        } finally {
            locker.unlock();
        }
    }

    public static void delete(String key, int index) {
        locker.lock();
        try {
            List<PersistVO> queryVos;
            List<PersistVO> needUpdateVos = new ArrayList<>();
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            binder.add(index);
            queryVos = command.find(PersistVO.class, "where `key`=? and `idx`>=?", binder);
            if (queryVos.isEmpty()) {
                return;
            }
            for (PersistVO qvo : queryVos) {
                if (Objects.equals(index, qvo.getIdx())) {
                    command.delete(qvo);
                } else {
                    qvo.setIdx(qvo.getIdx() - 1);
                    needUpdateVos.add(qvo);
                }
            }
            if (!needUpdateVos.isEmpty()) {
                command.batchInsertOrUpdate(needUpdateVos);
            }
        } finally {
            locker.unlock();
        }
    }

    public static void clear(String key) {
        locker.lock();
        try {
            List<PersistVO> toDelVos;
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            toDelVos = command.find(PersistVO.class, "where `key`=?", binder);
            if (!toDelVos.isEmpty()) {
                command.delete(toDelVos);
            }
        } finally {
            locker.unlock();
        }
    }

    public static String get(String key) {
        String val = "";
        locker.lock();
        try {
            PageResult<PersistVO> psVos;
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            psVos = command.findPage(PersistVO.class, "where `key`=?", binder, 1, 2, false);
            if (psVos.size() == 1) {
                val = psVos.get(0).getVal();
            }
        } finally {
            locker.unlock();
        }
        return val;
    }

    public static List<String> listAll(String key) {
        List<String> res = new ArrayList<>();
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            List<PersistVO> queryVos;
            queryVos = command.find(PersistVO.class, "where `key`=? order by `idx` asc", binder);
            for (PersistVO qvo : queryVos) {
                res.add(qvo.getVal());
            }
        } finally {
            locker.unlock();
        }
        return res;
    }

    public static List<String> listAllReversed(String key) {
        List<String> res = new ArrayList<>();
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            List<PersistVO> queryVos;
            queryVos = command.find(PersistVO.class, "where `key`=? order by `idx` desc", binder);
            for (PersistVO qvo : queryVos) {
                res.add(qvo.getVal());
            }
        } finally {
            locker.unlock();
        }
        return res;
    }

    public static List<String> range(String key, int from, int to) {
        List<String> res = new ArrayList<>();
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            binder.add(from);
            binder.add(to);
            List<PersistVO> rangeResult;
            rangeResult = command.find(PersistVO.class, "where `key`=? and `idx`>=? and `idx` <=?", binder);
            for (PersistVO pvo : rangeResult) {
                res.add(pvo.getVal());
            }
        } finally {
            locker.unlock();
        }
        return res;
    }

    public static String get(String key, int index) {
        String val = "";
        locker.lock();
        try {
            List<Object> binder = new ArrayList<>();
            binder.add(key);
            binder.add(index);
            PersistVO res;
            res = command.findOne(PersistVO.class, "where `key`=? and `idx`=?", binder);
            if (null != res) {
                val = res.getVal();
            }
        } finally {
            locker.unlock();
        }
        return val;
    }
}
