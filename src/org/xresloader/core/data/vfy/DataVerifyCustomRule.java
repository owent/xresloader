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

public class DataVerifyCustomRule extends DataVerifyImpl {
    private ArrayList<DataVerifyImpl> validators = null;
    private ArrayList<String> rules = null;
    private Boolean checkResult = null;
    private boolean checking = false;

    public class RuleConfigure {
        public String name;
        public List<String> rules;
    }

    public DataVerifyCustomRule(String name, ArrayList<String> rules) {
        super(name);

        this.rules = rules;
    }

    public void setup(ArrayList<DataVerifyImpl> validators) {
        this.validators = validators;
        this.checkResult = null;
    }

    public boolean hasChecked() {
        return this.checkResult != null;
    }

    public boolean check() {
        if (this.checkResult != null) {
            return this.checkResult;
        }
        if (this.checking) {
            return false;
        }

        this.checking = true;
        this.checkResult = false;
        if (this.rules == null || this.rules.size() == 0) {
            this.checking = false;
            this.checkResult = true;
            return this.checkResult;
        }

        if (this.validators == null || this.validators.size() == 0) {
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
    public boolean get(double number, DataVerifyResult res) {
        if (this.validators == null || this.validators.size() == 0) {
            res.success = true;
            res.value = number;
            return true;
        }

        for (DataVerifyImpl vfy : this.validators) {
            if (vfy.get(number, res)) {
                return true;
            }
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(String input, DataVerifyResult res) {
        if (this.validators == null || this.validators.size() == 0) {
            res.success = true;
            res.value = input;
            return true;
        }

        for (DataVerifyImpl vfy : this.validators) {
            if (vfy.get(input, res)) {
                return true;
            }
        }

        res.success = false;
        return false;
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
            HashMap<String, DataVerifyImpl> ret = new HashMap<String, DataVerifyImpl>();
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
                    Object rulesObj = ((Map<?, ?>) ruleObject).get("rules");
                    if (nameObj == null || !(nameObj instanceof String)) {
                        continue;
                    }

                    if (rulesObj == null || !(rulesObj instanceof List<?>)) {
                        continue;
                    }

                    String name = (String) nameObj;
                    ArrayList<String> rules = new ArrayList<String>();
                    rules.ensureCapacity(((List<?>) rulesObj).size());
                    for (Object ruleData : ((List<?>) rulesObj)) {
                        String ruleStr = ruleData.toString().trim();
                        if (!ruleStr.isEmpty()) {
                            rules.add(ruleStr);
                        }
                    }

                    if (null != ret.put(name, new DataVerifyCustomRule(name, rules))) {
                        ProgramOptions.getLoger().warn(
                                "Load custom validator file \"%s\" with more than one rule with name \"%s\", we will use the last one.",
                                filePath,
                                name);
                    }
                }
            }

            return ret;
        } catch (Exception e) {
            ProgramOptions.getLoger().error("Load custom validator file \"%s\" failed, %s", filePath, e.getMessage());
            return null;
        }
    }
}
