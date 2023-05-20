package com.zwsoft.connector.enums;

public enum FieldNameEnum {
    PartName(2000, "零件名称"),
    ProductNumber(2001, "产品编号"),
    SupplierName(2002, "供应商名称"),
    PartClass(2003, "零件类型");

    public int getIndex() {
        return index;
    }

    public String getCnName() {
        return cnName;
    }

    private int index;
    private String cnName;

    FieldNameEnum(int index, String cnName) {
        this.index = index;
        this.cnName = cnName;
    }
}
