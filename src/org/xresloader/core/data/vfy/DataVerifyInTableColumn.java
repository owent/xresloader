package org.xresloader.core.data.vfy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.ExcelEngine.CustomDataRowIndex;
import org.xresloader.core.engine.ExcelEngine.CustomDataTableIndex;

public class DataVerifyInTableColumn extends DataVerifyImpl {
    private boolean valid = false;
    private HashSet<String> dataSet = new HashSet<String>();

    public DataVerifyInTableColumn(ValidatorTokens tokens) {
        super(tokens);

        if (tokens.parameters.size() < 5) {
            ProgramOptions.getLoger().error("Invalid in text validator %s", tokens.name);
            return;
        }

        try {
            String filePath = tokens.parameters.get(1);
            String sheetName = tokens.parameters.get(2);
            int startRow = Integer.parseInt(tokens.parameters.get(3)) - 1;

            File file = DataSrcImpl.getDataFile(filePath);
            if (file == null) {
                ProgramOptions.getLoger().error("Can not find file % for validator %s", filePath, tokens.name);
                return;
            }

            CustomDataTableIndex tableIndex = ExcelEngine.openStreamTableIndex(file, sheetName);

            int startColumn = -1;
            if (tokens.parameters.size() == 5) {
                startColumn = Integer.parseInt(tokens.parameters.get(4));
            } else {
                int keyRow = Integer.parseInt(tokens.parameters.get(4)) - 1;
                String keyValue = tokens.parameters.get(5).trim();
                CustomDataRowIndex row = tableIndex.getRow(keyRow);
                if (null == row) {
                    ProgramOptions.getLoger().error("Invalid key row in table column validator %s", tokens.name);
                    return;
                }
                for (int i = 0; i < row.getColumnSize(); ++i) {
                    String value = row.getCellValue(i);
                    if (value == null) {
                        continue;
                    }
                    if (value.trim().equals(keyValue)) {
                        startColumn = i;
                        break;
                    }
                }
            }

            if (startRow < 0) {
                ProgramOptions.getLoger().error("Invalid start row in table column validator %s", tokens.name);
                return;
            }
            if (startColumn < 0) {
                ProgramOptions.getLoger().error("Invalid column or column not found in table column validator %s",
                        tokens.name);
                return;
            }

            for (int i = startRow; i <= tableIndex.getLastRowNum(); ++i) {
                CustomDataRowIndex row = tableIndex.getRow(i);
                if (null == row) {
                    continue;
                }

                String value = row.getCellValue(startColumn);
                if (value == null) {
                    continue;
                }

                value = value.trim();
                if (value.isEmpty()) {
                    continue;
                }

                dataSet.add(value);
            }

            this.valid = true;
        } catch (NumberFormatException e) {
            ProgramOptions.getLoger().error("Can not parse number of validator %s: %s",
                    tokens.name,
                    e.getMessage());
        } catch (Exception e) {
            ProgramOptions.getLoger().error("Can not open file %s for %s validator: %s", tokens.parameters.get(1),
                    tokens.name,
                    e.getMessage());
        }
    }

    public boolean isValid() {
        return this.valid;
    }

    @Override
    public boolean get(double number, DataVerifyResult res) {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return true;
        }

        String value;
        if (number == (long) number) {
            value = String.format("%d", (long) number);
        } else {
            value = String.format("%g", number);
        }
        if (this.dataSet.contains(value)) {
            res.success = true;
            res.value = number;
            return true;
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(String input, DataVerifyResult res) {
        // 空值永久有效,因为空数据项会被填充默认值
        if (input.isEmpty()) {
            res.success = true;
            res.value = "";
            return true;
        }

        if (dataSet.contains(input)) {
            res.success = true;
            res.value = input;
            return true;
        }

        res.success = false;
        return false;
    }
}
