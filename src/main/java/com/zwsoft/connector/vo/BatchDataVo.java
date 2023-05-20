package com.zwsoft.connector.vo;

import com.zwsoft.connector.anno.ExcelColumn;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchDataVo {
    @ExcelColumn(value = "PartName")
    private String partName;
    @ExcelColumn(value = "ProductNumber")
    private String productNumber;
    @ExcelColumn(value = "SupplierName")
    private String supplierName;
    @ExcelColumn(value = "PartClass")
    private String partClass;
}
