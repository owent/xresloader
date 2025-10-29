package org.xresloader.core.data.vfy;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;

public abstract class DataVerifyCustomRule extends DataVerifyImpl {
    protected ArrayList<DataVerifyImpl> validators = null;
    private ArrayList<String> rules = null;
    private Boolean checkResult = null;
    private boolean checking = false;
    private String description = null;
    private int version = 0;
    private Integer versionResult = null;

    public class RuleConfigure {
        public String name;
        public List<String> rules;
    }

    public DataVerifyCustomRule(String name, ArrayList<String> rules, String description, int version) {
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

        return sb.toString();
    }

    public void setup(ArrayList<DataVerifyImpl> validators) {
        this.validators = validators;
        this.checkResult = null;
        this.versionResult = null;
    }

    @Override
    public boolean isValid() {
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

    public ArrayList<String> getRules() {
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

    public static HashMap<String, DataVerifyImpl> loadFromFile(String filePath) {
        File yamlFile = DataSrcImpl.getDataFile(filePath);
        if (null == yamlFile) {
            ProgramOptions.getLoger().error("Can not find custom validator file \"%s\"", filePath);
            return null;
        }

        try {
            LoadSettings settings = LoadSettings.builder().setLabel("xresloader.DataVerifyCustomRule").build();
            Load load = new Load(settings);

            var allRootObjects = load.loadAllFromInputStream(new FileInputStream(yamlFile));
            HashMap<String, DataVerifyImpl> ret = new HashMap<>();
            for (Object object : allRootObjects) {
                if (!(object instanceof Map<?, ?>)) {
                    continue;
                }

                Object ruleSet = ((Map<?, ?>) object).get("validator");
                if (null == ruleSet) {
                    continue;
                }

                if (!(ruleSet instanceof List<?>)) {
                    continue;
                }

                for (Object ruleObject : (List<?>) ruleSet) {
                    if (!(ruleObject instanceof Map<?, ?>)) {
                        continue;
                    }

                    Object nameObj = ((Map<?, ?>) ruleObject).get("name");
                    Object descriptionObj = ((Map<?, ?>) ruleObject).getOrDefault("description", null);
                    Object rulesObj = ((Map<?, ?>) ruleObject).get("rules");
                    if (nameObj == null || !(nameObj instanceof String)) {
                        continue;
                    }

                    if (rulesObj == null || !(rulesObj instanceof List<?>)) {
                        continue;
                    }

                    String name = (String) nameObj;
                    String description = null;
                    if (descriptionObj != null && descriptionObj instanceof String) {
                        description = (String) descriptionObj;
                    }

                    ArrayList<String> rules = new ArrayList<>();
                    int version = 0;
                    Object versionObj = ((Map<?, ?>) ruleObject).get("version");
                    if (versionObj != null) {
                        if (versionObj instanceof Integer) {
                            version = (Integer) versionObj;
                        } else if (versionObj instanceof String) {
                            try {
                                version = Integer.parseInt((String) versionObj);
                            } catch (NumberFormatException e) {
                                ProgramOptions.getLoger().warn(
                                        "Load custom validator file \"%s\" with invalid version \"%s\", we will use 0.",
                                        filePath,
                                        versionObj);
                            }
                        } else {
                            ProgramOptions.getLoger().warn(
                                    "Load custom validator file \"%s\" with invalid version \"%s\", we will use 0.",
                                    filePath,
                                    versionObj.toString());
                        }
                    }

                    rules.ensureCapacity(((List<?>) rulesObj).size());
                    for (Object ruleData : ((List<?>) rulesObj)) {
                        String ruleStr = ruleData.toString().trim();
                        if (!ruleStr.isEmpty()) {
                            rules.add(ruleStr);
                        }
                    }

                    if (null != ret.put(name, new DataVerifyCustomOrRule(name, rules, description, version))) {
                        ProgramOptions.getLoger().warn(
                                "Load custom validator file \"%s\" with more than one rule with name \"%s\", we will use the last one.",
                                filePath,
                                name);
                    }
                }
            }

            return ret;
        } catch (java.io.IOException | NumberFormatException e) {
            ProgramOptions.getLoger().error("Load custom validator file \"%s\" failed, %s", filePath, e.getMessage());
            return null;
        }
    }
}
