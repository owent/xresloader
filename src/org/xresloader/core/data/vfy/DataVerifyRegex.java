package org.xresloader.core.data.vfy;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.xresloader.core.ProgramOptions;

public class DataVerifyRegex extends DataVerifyImpl {
    private boolean valid = false;
    private ArrayList<Pattern> rules = new ArrayList<>();

    public DataVerifyRegex(ValidatorTokens tokens) {
        super(tokens);

        this.valid = false;
        if (tokens.parameters.size() < 2) {
            ProgramOptions.getLoger().error("Invalid in regex validator %s", tokens.name);
            return;
        }

        this.valid = true;
        for (int i = 1; i < tokens.parameters.size(); ++i) {
            try {
                this.rules.add(Pattern.compile(tokens.parameters.get(i)));
            } catch (PatternSyntaxException e) {
                ProgramOptions.getLoger().error("Can not parse regex %s for validator %s : %s",
                        tokens.parameters.get(i),
                        tokens.name,
                        e.getMessage());
                this.valid = false;
            }
        }
    }

    @Override
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

        for (Pattern rule : this.rules) {
            if (rule.matcher(value).matches()) {
                res.success = true;
                res.value = number;
                return true;
            }
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

        String value;
        value = String.format("%d", number);

        for (Pattern rule : this.rules) {
            if (rule.matcher(value).matches()) {
                res.success = true;
                res.value = number;
                return true;
            }
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

        for (Pattern rule : this.rules) {
            if (rule.matcher(input.trim()).matches()) {
                res.success = true;
                res.value = input;
                return true;
            }
        }

        res.success = false;
        return false;
    }
}
