package com.zwsoft.connector.beans;

import com.zwsoft.connector.enums.EnvPropKey;
import com.zwsoft.connector.utils.PersistUtils;
import com.zwsoft.connector.vo.EnvPropVo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class WkEnv {
    private SQLiteCommand command;
    @Resource
    private SQLiteSource source;

    public List<String> envNames() {
        return PersistUtils.listAll("ENV");
    }

    public String getStrProp(EnvPropKey sa) {
        EnvPropVo vo = getProp(sa);
        return vo.getValue();
    }

    private EnvPropVo getProp(EnvPropKey sa) {
        List<Object> binder = new ArrayList<>();
        binder.add(1);
        binder.add(sa.name());
        return command.findOne(EnvPropVo.class,"where `env`=? and `key`=?",binder);
    }

    public void writeEnv(String sa1) {
        EnvPropVo envPropVo =new EnvPropVo();
        envPropVo.setEnv(1+"");
        envPropVo.setValue(sa1);
        command.insertOrUpdate(envPropVo);

    }
}
