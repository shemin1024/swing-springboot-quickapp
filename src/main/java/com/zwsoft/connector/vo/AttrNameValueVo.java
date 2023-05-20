package com.zwsoft.connector.vo;

import com.zwsoft.connector.anno.ExcelColumn;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttrNameValueVo {
    @ExcelColumn(value = "名称")
    private String name;

    public AttrNameValueVo() {
    }

    public AttrNameValueVo(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @ExcelColumn(value = "值")
    private String value;
}
