package org.xresloader.core.data.vfy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.ExcelEngine.CustomDataRowIndex;
import org.xresloader.core.engine.ExcelEngine.CustomDataTableIndex;
import org.xresloader.core.engine.IdentifyEngine;

public class DataVerifyInMacroTable extends DataVerifyImpl {
    private boolean valid = false;
    private HashSet<String> dataValues = new HashSet<String>();
    private HashMap<String, String> dataKeyMapping = new HashMap<String, String>();
    private File file = null;
    private ArrayList<String> parameters = null;

    public DataVerifyInMacroTable(ValidatorTokens tokens) {
        super(tokens);

        this.valid = false;
        if (tokens.parameters.size() < 6) {
            ProgramOptions.getLoger().error("Invalid in macro table validator %s", tokens.name);
            return;
        }
        this.parameters = tokens.parameters;

        this.file = DataSrcImpl.getDataFile(this.parameters.get(1));
        if (file == null) {
            ProgramOptions.getLoger().error("Can not find file %s for validator %s", this.parameters.get(1),
                    tokens.name);
            return;
        }

        this.valid = true;
    }

    public boolean isValid() {
        return this.valid;
    }

    private boolean loadFile() {
        if (this.file == null) {
            return true;
        }

        if (this.parameters.size() < 6) {
            ProgramOptions.getLoger().error("Invalid in macro table validator %s", this.name);
            return false;
        }

        try {
            String sheetName = this.parameters.get(2);
            int startRow = Integer.parseInt(this.parameters.get(3)) - 1;

            CustomDataTableIndex tableIndex = ExcelEngine.openStreamTableIndex(file, sheetName);

            int aliasKeyColumn = -1;
            int aliasValueColumn = -1;
            if (this.parameters.size() == 6) {
                aliasKeyColumn = Integer.parseInt(this.parameters.get(4));
                aliasValueColumn = Integer.parseInt(this.parameters.get(5));
            } else {
                int keyRow = Integer.parseInt(this.parameters.get(4)) - 1;
                String aliasKeyName = this.parameters.get(5).trim();
                String aliasValueName = this.parameters.get(6).trim();
                String normalizedKeyName = IdentifyEngine.normalize(aliasKeyName);
                String normalizedValueName = IdentifyEngine.normalize(aliasValueName);
                CustomDataRowIndex row = tableIndex.getRow(keyRow);
                if (null == row) {
                    ProgramOptions.getLoger().error("Invalid key row in macro table validator %s", this.name);
                    return false;
                }
                for (int i = 0; i < row.getColumnSize(); ++i) {
                    String value = row.getCellValue(i);
                    if (value == null) {
                        continue;
                    }

                    String trimedValue = value.trim();
                    if (trimedValue.equals(aliasKeyName) || trimedValue.equals(normalizedKeyName)) {
                        aliasKeyColumn = i;
                    } else if (trimedValue.equals(aliasValueName) || trimedValue.equals(normalizedValueName)) {
                        aliasValueColumn = i;
                    }
                    if (aliasKeyColumn >= 0 && aliasValueColumn >= 0) {
                        break;
                    }
                }
            }

            if (startRow < 0) {
                ProgramOptions.getLoger().error("Invalid start row in macro table validator %s", this.name);
                return false;
            }
            if (aliasKeyColumn < 0 || aliasValueColumn < 0) {
                if (aliasKeyColumn < 0) {
                    ProgramOptions.getLoger().error(
                            "Invalid alias key name or alias key name not found in macro table validator %s",
                            this.name);
                }
                if (aliasValueColumn < 0) {
                    ProgramOptions.getLoger().error(
                            "Invalid alias value name or alias value name not found in macro table validator %s",
                            this.name);
                }

                return false;
            }

            for (int i = startRow; i <= tableIndex.getLastRowNum(); ++i) {
                CustomDataRowIndex row = tableIndex.getRow(i);
                if (null == row) {
                    continue;
                }

                String aliasKey = row.getCellValue(aliasKeyColumn);
                String aliasValue = row.getCellValue(aliasValueColumn);
                if (aliasKey != null) {
                    aliasKey = aliasKey.trim();
                }
                if (aliasValue != null) {
                    aliasValue = aliasValue.trim();
                }

                if ((aliasKey == null || aliasKey.isEmpty()) && (aliasValue == null || aliasValue.isEmpty())) {
                    continue;
                }

                if (aliasKey == null || aliasKey.isEmpty()) {
                    this.dataValues.add(aliasValue);
                }

                if (aliasValue == null || aliasValue.isEmpty()) {
                    this.dataKeyMapping.put(aliasKey, "");
                } else {
                    this.dataKeyMapping.put(aliasKey, aliasValue);
                    this.dataValues.add(aliasValue);
                }
            }
        } catch (NumberFormatException e) {
            ProgramOptions.getLoger().error("Can not parse number of validator %s: %s",
                    this.name,
                    e.getMessage());
            return false;
        } catch (Exception e) {
            ProgramOptions.getLoger().error("Can not open file %s for %s validator: %s", this.parameters.get(1),
                    this.name,
                    e.getMessage());
            return false;
        }

        this.file = null;
        return true;
    }

    @Override
    public boolean get(double number, DataVerifyResult res) {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return true;
        }

        if (!loadFile()) {
            res.success = false;
            return false;
        }

        String value;
        if (number == (long) number) {
            value = String.format("%d", (long) number);
        } else {
            value = String.format("%g", number);
        }
        if (this.dataValues.contains(value)) {
            res.success = true;
            res.value = number;
            return true;
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(long number, DataVerifyResult res) {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return true;
        }

        if (!loadFile()) {
            res.success = false;
            return false;
        }

        String value;
        value = String.format("%d", number);
        if (this.dataValues.contains(value)) {
            res.success = true;
            res.value = number;
            return true;
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(String input, DataVerifyResult res) throws NumberFormatException {
        // 空值永久有效,因为空数据项会被填充默认值
        if (input.isEmpty()) {
            res.success = true;
            res.value = "";
            return true;
        }

        if (!loadFile()) {
            res.success = false;
            res.value = "";
            return false;
        }

        String value = this.dataKeyMapping.getOrDefault(input, null);
        if (value != null) {
            res.success = true;
            res.value = value;
            return true;
        }

        if (this.dataValues.contains(input)) {
            res.success = true;
            res.value = input;
            return true;
        }

        res.success = false;
        return false;
    }
}
