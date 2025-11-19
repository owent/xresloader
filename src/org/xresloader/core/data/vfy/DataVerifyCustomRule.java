package org.xresloader.core.data.vfy;

import java.util.ArrayList;
import java.util.List;

import org.xresloader.core.ProgramOptions;

public abstract class DataVerifyCustomRule extends DataVerifyImpl {
    protected List<DataVerifyImpl> validators = null;
    private List<ValidatorParameter> rules = null;
    private Boolean checkResult = null;
    private boolean checking = false;
    private String description = null;
    private int version = 0;
    private Integer versionResult = null;

    public DataVerifyCustomRule(String name, List<ValidatorParameter> rules, String description, int version) {
        super(name);

        this.rules = rules;
        this.version = version;

        if (description != null && !description.isEmpty()) {
            this.description = description;
        }
    }

    @Override
    public String getDescription() {
        if (this.description != null) {
            return this.description;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        if (this.validators != null) {
            sb.append("[");
            boolean first = true;
            boolean checked = check();
            for (DataVerifyImpl vfy : this.validators) {
                if (!first) {
                    sb.append(", ");
                }
                if (checked) {
                    sb.append(vfy.getDescription());
                } else {
                    sb.append(vfy.getName());
                }
                first = false;
            }
            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public boolean setup(DataValidatorCache cache) {
        // 第一次setup失败就会退出，不需要反复失败重试
        if (this.validators != null) {
            return true;
        }

        this.validators = new ArrayList<>();
        ((ArrayList<DataVerifyImpl>) this.validators).ensureCapacity(this.rules.size());
        this.checkResult = null;
        this.versionResult = null;

        for (ValidatorParameter rule : this.rules) {
            if (rule.isToken()) {
                // setup阶段，尝试创建token对应的验证器
                DataVerifyImpl vfy = DataValidatorFactory.createValidator(cache, rule.getTokens());
                if (vfy == null) {
                    ProgramOptions.getLoger().error(
                            "Setup custom validator rule \"%s\" failed because sub-rule \"%s\" not found",
                            getName(), rule.toString());
                    this.validators.clear();
                    return false;
                }
                this.validators.add(vfy);
            } else {
                // setup阶段，尝试创建token对应的验证器
                DataVerifyImpl vfy = DataValidatorFactory.createValidator(cache,
                        cache.mutableSymbolToTokens(rule.toString()));
                if (vfy == null) {
                    ProgramOptions.getLoger().error(
                            "Setup custom validator rule \"%s\" failed because sub-rule \"%s\" not found",
                            getName(), rule.toString());
                    this.validators.clear();
                    return false;
                }
                this.validators.add(vfy);
            }
        }

        return true;
    }

    @Override
    public boolean isValid() {
        if (this.validators != null && this.validators.isEmpty()) {
            return false;
        }
        return this.rules != null && !this.rules.isEmpty();
    }

    public boolean hasChecked() {
        return this.checkResult != null;
    }

    public boolean check() {
        // Check circle dependency
        if (this.checkResult != null) {
            return this.checkResult;
        }
        if (this.checking) {
            return false;
        }

        this.checking = true;
        this.checkResult = false;
        if (this.rules == null || this.rules.isEmpty()) {
            this.checking = false;
            this.checkResult = true;
            return this.checkResult;
        }

        if (this.validators == null || this.validators.isEmpty()) {
            this.checking = false;
            ProgramOptions.getLoger().error("Check custom validator rule %s without setup it", getName());
            return this.checkResult;
        }

        for (DataVerifyImpl vfy : this.validators) {
            if (!(vfy instanceof DataVerifyCustomRule)) {
                continue;
            }

            DataVerifyCustomRule depVfy = (DataVerifyCustomRule) vfy;
            if (!depVfy.check()) {
                if (depVfy.checking) {
                    ProgramOptions.getLoger().error(
                            "Check custom validator rule \"%s\" with circular dependency from \"%s\"",
                            getName(), vfy.getName());
                } else {
                    ProgramOptions.getLoger().error(
                            "Check custom validator rule \"%s\" failed because checking dependency \"%s\" failed",
                            getName(), vfy.getName());
                }

                this.checking = false;
                return this.checkResult;
            }
        }

        this.checking = false;
        this.checkResult = true;
        return this.checkResult;
    }

    public List<ValidatorParameter> getRules() {
        return this.rules;
    }

    @Override
    public int getVersion() {
        if (this.versionResult != null) {
            return this.versionResult;
        }

        DataVerifyImpl useChild = null;
        int maxVersion = this.version;
        for (DataVerifyImpl vfy : this.validators) {
            int v = vfy.getVersion();
            if (v > maxVersion) {
                maxVersion = v;
                useChild = vfy;
            }
        }
        this.versionResult = maxVersion;
        if (null != useChild && this.version > 0) {
            ProgramOptions.getLoger().warn(
                    "Custom validator rule \"%s\" use child rule \"%s\" with higher version %d, which will implicit increase the version from %d to %d.",
                    getName(),
                    useChild.getName(),
                    useChild.getVersion(),
                    this.version,
                    maxVersion);
        }
        return maxVersion;
    }

    public abstract boolean batchGetSubValidators(double number, DataVerifyResult res);

    public abstract boolean batchGetSubValidators(long number, DataVerifyResult res);

    public abstract boolean batchGetSubValidators(String input, DataVerifyResult res);

    @Override
    public boolean get(double number, DataVerifyResult res) {
        if (this.validators == null || this.validators.isEmpty()) {
            res.success = true;
            res.value = number;
            return true;
        }

        if (!check()) {
            res.success = false;
            return false;
        }

        res.success = this.batchGetSubValidators(number, res);
        return res.success;
    }

    @Override
    public boolean get(long number, DataVerifyResult res) {
        if (this.validators == null || this.validators.isEmpty()) {
            res.success = true;
            res.value = number;
            return true;
        }

        if (!check()) {
            res.success = false;
            return false;
        }

        res.success = this.batchGetSubValidators(number, res);
        return res.success;
    }

    @Override
    public boolean get(String input, DataVerifyResult res) throws NumberFormatException {
        if (this.validators == null || this.validators.isEmpty()) {
            res.success = true;
            res.value = input;
            return true;
        }

        if (!check()) {
            res.success = false;
            return false;
        }

        res.success = this.batchGetSubValidators(input, res);
        return res.success;
    }
}
