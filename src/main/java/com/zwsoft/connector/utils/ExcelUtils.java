package com.zwsoft.connector.utils;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.zwsoft.connector.anno.ExcelColumn;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {
    private ExcelUtils() {
        //sonar 隐藏工具类构造函数
    }

    /**
     * @param dataList bean实例数据列表
     * @param cls      bean class
     * @Description: 将bean以及实例数据写到excel
     * @return: org.apache.poi.ss.usermodel.Workbook
     */
    public static <T> Workbook writeExcel(List<T> dataList, Class<T> cls) {
        Workbook workbook = new XSSFWorkbook();
        List<Field> fields = Arrays.asList(cls.getDeclaredFields());
        List<Field> fieldList = fields.stream().filter(field -> {
            //cls中带ExcelColumn注解的field会导出
            ExcelColumn excelColumnAnnotation = field.getAnnotation(ExcelColumn.class);
            if (!ObjectUtils.isEmpty(excelColumnAnnotation)) {
                ReflectionUtils.makeAccessible(field);
                return true;
            }
            return false;
        }).sorted(Comparator.comparing(field -> {
            //根据注解里的index排序
            int index = 0;
            ExcelColumn excelColumnAnnotation = field.getAnnotation(ExcelColumn.class);
            if (!ObjectUtils.isEmpty(excelColumnAnnotation)) {
                index = excelColumnAnnotation.index();
            }
            return index;
        })).collect(Collectors.toList());
        Sheet sheet = workbook.createSheet("sheet1");
        int rowIndex = 0;
        int colIndex = 0;
        //写标题栏
        Row row = sheet.createRow(rowIndex);
        writeTitle(workbook, fieldList, sheet, colIndex, row);
        rowIndex++;
        //写数据
        writeData(dataList, fieldList, sheet, rowIndex);
        workbook.getSheet("Sheet1").createFreezePane(0, 1, 0, 1);
        log.info("write workbook complete!");
        return workbook;
    }

    private static <T> void writeData(List<T> dataList, List<Field> fieldList, Sheet sheet, int rowIndex) {
        if (CollectionUtil.isNotEmpty(dataList)) {
            for (T t : dataList) {
                Row dataRow = sheet.createRow(rowIndex);
                int dataColIndex = 0;
                for (Field field : fieldList
                ) {
                    Object value = null;
                    try {
                        value = field.get(t);
                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
                        log.error(e.getMessage());
                    }
                    Cell cell = dataRow.createCell(dataColIndex);
                    assert value != null;
                    if (ObjectUtil.isNotEmpty(value)) {
                        setValueToCell(field, value, cell);
                    }
                    //cell赋值完，设置自适应列宽
                    //sheet.autoSizeColumn(dataColIndex);
                    dataColIndex++;
                }
                rowIndex++;
                log.info("write data line " + rowIndex + " complete");
            }
        }
    }

    private static void setValueToCell(Field field, Object value, Cell cell) {
        if (field.getType() == List.class) {
            List<String> list = (List<String>) value;
            StringBuilder sb = new StringBuilder();
            for (String str : list
            ) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(str);
            }
            cell.setCellValue(sb.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private static void writeTitle(Workbook workbook, List<Field> fieldList, Sheet sheet, int colIndex, Row row) {
        for (Field field : fieldList) {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            String columnValue = "";
            if (ObjectUtil.isNotEmpty(annotation)) {
                columnValue = annotation.value();
            }
            Cell cell = row.createCell(colIndex);
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
            cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(columnValue);
            sheet.setColumnWidth(colIndex, 24 * 256);
            colIndex++;
        }
    }

    /**
     * @param firstRow 起始行号
     * @param firstCol 起始列
     * @param lastRow  结束行
     * @param lastCol  结束列
     * @param sheet    工作表
     * @param datas    下拉列表可选值，大小不能超过255
     * @Description: 设置下拉列表
     * @return: void
     */
    public static void setValidation(int firstRow, int firstCol, int lastRow, int lastCol, Sheet sheet,
                                     List<String> datas) {
        //设置下拉列表
        DataValidationHelper helper = sheet.getDataValidationHelper();
        CellRangeAddressList addressList = new CellRangeAddressList();
        addressList.addCellRangeAddress(firstRow, firstCol, lastRow, lastCol);
        DataValidationConstraint constraint =
                helper.createExplicitListConstraint(datas.toArray(datas.toArray(new String[datas.size()])));
        DataValidation dataValidation = helper.createValidation(constraint, addressList);
        dataValidation.setSuppressDropDownArrow(true);
        dataValidation.setShowErrorBox(true);
        sheet.addValidationData(dataValidation);
    }

    public static <T> List<T> readExcel(Class<T> cls, InputStream is) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        List<T> dataList = new ArrayList<>();
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook(is);
            if (ObjectUtil.isEmpty(workbook))
                return dataList;
            Map<String, List<Field>> classMap = getClassMap(cls);
            Map<Integer, List<Field>> indexMap = new HashMap<>();
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (i == sheet.getFirstRowNum()) {
                    readTitle(classMap, indexMap, row);
                } else {
                    if (ObjectUtil.isEmpty(row)) {
                        continue;
                    }
                    readDataToList(cls, dataList, indexMap, row);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return dataList;
    }

    private static <T> void readDataToList(Class<T> cls, List<T> dataList, Map<Integer, List<Field>> indexMap, Row row) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        T t = cls.getDeclaredConstructor().newInstance();
        boolean allEmpty = true;
        for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
            if (indexMap.containsKey(j)) {
                Cell cell = row.getCell(j);
                String value = getCellValue(cell);
                if (StrUtil.isNotEmpty(value)) {
                    allEmpty = false;
                }
                List<Field> fields = indexMap.get(j);
                for (Field field : fields
                ) {
                    transferToField(t, field, value);
                }
            }
        }
        if (!allEmpty) {
            dataList.add(t);
        }
    }

    private static void readTitle(Map<String, List<Field>> classMap, Map<Integer, List<Field>> indexMap, Row row) {
        for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
            Cell cell = row.getCell(j);
            String value = getCellValue(cell);
            if (classMap.containsKey(value)) {
                indexMap.put(j, classMap.get(value));
            }
        }
    }

    private static <T> Map<String, List<Field>> getClassMap(Class<T> cls) {
        Map<String, List<Field>> classMap = new HashMap<>();
        List<Field> fieldList = Arrays.asList(cls.getDeclaredFields());
        for (Field field : fieldList
        ) {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (ObjectUtil.isNotEmpty(annotation)) {
                String value = annotation.value();
                if (StrUtil.isEmpty(value)) {
                    continue;
                }
                classMap.computeIfAbsent(value, k -> new ArrayList<>());
                ReflectionUtils.makeAccessible(field);
                classMap.get(value).add(field);
            }
        }
        return classMap;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        } else if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            return "";
        } else if (cell.getCellType() == cell.CELL_TYPE_NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == cell.CELL_TYPE_STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == cell.CELL_TYPE_FORMULA) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == cell.CELL_TYPE_BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else {
            log.error("----cell.getRowIndex()----" + cell.getRowIndex() + "----cell.getColumnIndex()----" + cell.getColumnIndex());
            return "";
        }
    }

    private static <T> void transferToField(T t, Field field, String value) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            Class<?> type = field.getType();
            if (type == null || type == void.class) {
                return;
            }
            if (type == Object.class) {
                ReflectionUtils.setField(field, t, value);
            } else if (type.getSuperclass() == null || type.getSuperclass() == Number.class) {
                if (type == int.class || type == Integer.class) {
                    ReflectionUtils.setField(field, t, NumberUtil.parseInt(value));
                } else if (type == long.class || type == Long.class) {
                    ReflectionUtils.setField(field, t, NumberUtil.parseLong(value));
                } else if (type == byte.class || type == Byte.class) {
                    ReflectionUtils.setField(field, t, value);
                } else if (type == short.class || type == Short.class) {
                    ReflectionUtils.setField(field, t, Short.parseShort(value));
                } else if (type == double.class || type == Double.class) {
                    ReflectionUtils.setField(field, t, NumberUtil.parseDouble(value));
                } else if (type == float.class || type == Float.class) {
                    ReflectionUtils.setField(field, t, NumberUtil.parseFloat(value));
                } else if (type == char.class || type == Character.class) {
                    ReflectionUtils.setField(field, t, value);
                } else if (type == boolean.class) {
                    ReflectionUtils.setField(field, t, BooleanUtil.toBoolean(value));
                } else if (type == BigDecimal.class) {
                    ReflectionUtils.setField(field, t, new BigDecimal(value));
                } else if (type == List.class) {
                    List<String> list = handlerList(value);
                    ReflectionUtils.setField(field, t, list);
                }
            } else if (type == Boolean.class) {
                ReflectionUtils.setField(field, t, BooleanUtil.toBoolean(value));
            } else if (type == Date.class) {
                ReflectionUtils.setField(field, t, value);
            } else if (type == String.class) {
                ReflectionUtils.setField(field, t, value);
            } else {
                Constructor<?> constructor = type.getConstructor(String.class);
                ReflectionUtils.setField(field, t, constructor.newInstance(value));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private static List<String> handlerList(String value) {
        List<String> list = new ArrayList<>();
        String data = value.replaceAll("[\\[\\]]", "");
        if (StrUtil.isNotEmpty(data)) {
            list = Arrays.asList(data.split(","));
        }
        return list;
    }
}

