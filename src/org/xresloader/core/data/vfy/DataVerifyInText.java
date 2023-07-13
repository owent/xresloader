package org.xresloader.core.data.vfy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;

public class DataVerifyInText extends DataVerifyImpl {
    private boolean valid = false;
    private HashSet<String> dataSet = new HashSet<String>();
    private File file = null;
    private ArrayList<String> parameters = null;

    static Pattern SPACE_SPLITOR = Pattern.compile("\\s+");

    public DataVerifyInText(ValidatorTokens tokens) {
        super(tokens);

        this.valid = false;
        if (tokens.parameters.size() < 2) {
            ProgramOptions.getLoger().error("Invalid in text validator %s", tokens.name);
            return;
        }
        this.parameters = tokens.parameters;

        this.file = DataSrcImpl.getDataFile(tokens.parameters.get(1));
        if (this.file == null) {
            ProgramOptions.getLoger().error("Can not find file %s for validator %s.",
                    tokens.parameters.get(1),
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

        if (this.parameters.size() < 2) {
            ProgramOptions.getLoger().error("Invalid in text validator %s", this.name);
            return false;
        }

        try {
            int fieldIndex = -1;
            if (this.parameters.size() > 2) {
                fieldIndex = Integer.parseInt(this.parameters.get(2)) - 1;
            }
            Pattern separator = SPACE_SPLITOR;
            if (this.parameters.size() > 3) {
                separator = Pattern.compile(this.parameters.get(3));
            }

            FileInputStream fileInputStream = new FileInputStream(this.file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            int lineNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                ++lineNumber;
                if (fieldIndex < 0) {
                    dataSet.add(line.trim());
                } else {
                    String[] fields = separator.split(line.trim());
                    if (fieldIndex < fields.length) {
                        dataSet.add(fields[fieldIndex]);
                    } else {
                        ProgramOptions.getLoger().warn("%s will skip line %d(%s) because it has not enough fields",
                                this.name,
                                lineNumber,
                                line.trim());
                    }
                }
            }

            bufferedReader.close();
            inputStreamReader.close();
            fileInputStream.close();
        } catch (NumberFormatException e) {
            ProgramOptions.getLoger().error("Can not parse field number %s for validator %s : %s",
                    this.parameters.get(2),
                    this.name,
                    e.getMessage());
            return false;
        } catch (PatternSyntaxException e) {
            ProgramOptions.getLoger().error("Can not parse field separator %s for validator %s : %s",
                    this.parameters.get(3),
                    this.name,
                    e.getMessage());
            return false;
        } catch (Exception e) {
            ProgramOptions.getLoger().error("Can not open file %s for validator %s : %s", this.parameters.get(1),
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

        if (!loadFile()) {
            res.success = false;
            res.value = "";
            return false;
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
