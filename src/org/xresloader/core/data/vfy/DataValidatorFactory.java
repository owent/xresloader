package org.xresloader.core.data.vfy;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl.ValidatorParameter;
import org.xresloader.core.data.vfy.DataVerifyImpl.ValidatorTokens;

public class DataValidatorFactory {
  static private HashMap<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> initFuncValidatorCreator() {
    HashMap<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> funcMap = new HashMap<>();

    funcMap.put("intext", (ruleObject) -> new DataVerifyInText(ruleObject));
    funcMap.put("intablecolumn", (ruleObject) -> new DataVerifyInTableColumn(ruleObject));
    funcMap.put("inmacrotable", (ruleObject) -> new DataVerifyInMacroTable(ruleObject));
    funcMap.put("regex", (ruleObject) -> new DataVerifyRegex(ruleObject));
    funcMap.put("and", (ruleObject) -> new DataVerifyCustomAndRule(ruleObject.getName(),
        new ArrayList<>(ruleObject.getParameters().subList(1, ruleObject.getParameters().size())), null, 0));
    funcMap.put("or", (ruleObject) -> new DataVerifyCustomOrRule(ruleObject.getName(),
        new ArrayList<>(ruleObject.getParameters().subList(1, ruleObject.getParameters().size())), null, 0));
    funcMap.put("not", (ruleObject) -> new DataVerifyCustomNotRule(ruleObject.getName(),
        new ArrayList<>(ruleObject.getParameters().subList(1, ruleObject.getParameters().size())), null, 0));
    funcMap.put("invalues", (ruleObject) -> new DataVerifyInValues(ruleObject));

    return funcMap;
  }

  private static final HashMap<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> funcValidatorCreator = initFuncValidatorCreator();
  private static boolean isInited = false;

  static public void Initialize() {
    if (isInited) {
      return;
    }
    isInited = true;
  }

  static private List<String> splitTokens(String expression) {
    List<String> ret = new ArrayList<>();
    ((ArrayList<String>) ret).ensureCapacity(16);
    if (null == expression) {
      return ret;
    }

    String stripedExpression = expression.trim();
    if (stripedExpression.isEmpty()) {
      return ret;
    }

    int start = -1;
    int end = 0;
    char stringMark = 0;
    for (; end < stripedExpression.length(); ++end) {
      char c = stripedExpression.charAt(end);

      // Close string
      if (stringMark != 0) {
        if (c == stringMark) {
          stringMark = 0;
          ret.add(stripedExpression.substring(start, end));
          start = -1;
        }
        continue;
      }

      // 未开始下一个token，可以跳过space
      if (start < 0) {
        if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
          continue;
        }
      }

      // Start string
      if (c == '"' || c == '\'') {
        if (start >= 0 && end > start) {
          ret.add(stripedExpression.substring(start, end).trim());
        }
        stringMark = c;
        start = end + 1;
        continue;
      }

      if (c == ',' || c == '|' || c == '(' || c == ')') {
        if (start >= 0 && end > start) {
          ret.add(stripedExpression.substring(start, end).trim());
        }
        ret.add(String.valueOf(c));
        start = -1;
        continue;
      }

      if (start < 0) {
        start = end;
      }
    }

    // 仅仅只有一边引号视为普通变量，不用判定stringMark
    if (start >= 0 && start < stripedExpression.length()) {
      ret.add(stripedExpression.substring(start).trim());
    }

    return ret;
  }

  static public class BuildValidatorTokensCursor {
    public int nextIndex = 0;
    public ValidatorTokens result = null;
  }

  static private BuildValidatorTokensCursor buildOneValidatorTokens(String name,
      List<String> tokens,
      int paramStartIndex) {
    BuildValidatorTokensCursor ret = new BuildValidatorTokensCursor();

    if (paramStartIndex >= tokens.size()) {
      ret.result = new ValidatorTokens(name, false);
      ret.nextIndex = paramStartIndex;
      return ret;
    }

    boolean funcMode = tokens.get(paramStartIndex).equals("(");
    if (funcMode) {
      ++paramStartIndex;
    }

    ret.result = new ValidatorTokens(name, funcMode);
    String lastToken = null;
    while (paramStartIndex < tokens.size()) {
      String currentToken = tokens.get(paramStartIndex);
      // End of parameters
      if (currentToken.equals(")") || (!funcMode && currentToken.equals("|"))) {
        if (lastToken != null) {
          ret.result.appendParameter(ValidatorParameter.ofString(lastToken));
        }
        ret.nextIndex = paramStartIndex + 1;
        return ret;
      }

      // Sub-parameter separator
      if (currentToken.equals(",")) {
        if (lastToken != null) {
          ret.result.appendParameter(ValidatorParameter.ofString(lastToken));
          lastToken = null;
        }

        ++paramStartIndex;
        continue;
      }

      // Sub-parameter token
      if (lastToken != null && !lastToken.isEmpty() && currentToken.equals("(")) {
        BuildValidatorTokensCursor subRet = buildOneValidatorTokens(lastToken, tokens, paramStartIndex);
        if (subRet.result != null) {
          ret.result.appendParameter(ValidatorParameter.ofToken(subRet.result));
        }

        lastToken = null;
        paramStartIndex = subRet.nextIndex;
        continue;
      }

      if (lastToken != null) {
        ret.result.appendParameter(ValidatorParameter.ofString(lastToken));
      }
      lastToken = currentToken;
      ++paramStartIndex;
    }

    if (lastToken != null) {
      ret.result.appendParameter(ValidatorParameter.ofString(lastToken));
    }

    ret.nextIndex = paramStartIndex;
    return ret;
  }

  static public LinkedList<ValidatorTokens> buildValidatorTokens(String expression) {
    if (null == expression) {
      return null;
    }

    List<String> tokens = splitTokens(expression);
    LinkedList<ValidatorTokens> ret = new LinkedList<>();
    int start = 0;
    while (start < tokens.size()) {
      String name = tokens.get(start);
      if (name.isEmpty() || name.equals("|")) {
        ++start;
        continue;
      }
      BuildValidatorTokensCursor cur = buildOneValidatorTokens(name, tokens, start + 1);
      start = cur.nextIndex;
      if (cur.result != null) {
        ret.add(cur.result);
      }
    }

    return ret;
  }

  public static boolean setupAll(DataValidatorCache cache) {
    if (cache == null) {
      return false;
    }

    boolean allSuccess = true;

    DataVerifyImpl validator = cache.popUnsetupValidator();
    while (validator != null) {
      if (!validator.setup(cache)) {
        allSuccess = false;
      }
      validator = cache.popUnsetupValidator();
    }

    return allSuccess;
  }

  static private DataVerifyImpl createValidator(DataVerifyImpl.ValidatorTokens ruleObject,
      Map<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> externalCreators) {
    if (ruleObject == null) {
      return null;
    }

    // 第一优先级，函数验证器
    if (ruleObject.isFunctionMode()) {
      Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl> creator;
      synchronized (funcValidatorCreator) {
        String funcName = ruleObject.getParameters().get(0).toString().toLowerCase();
        creator = funcValidatorCreator.get(funcName);
      }

      if (creator != null) {
        DataVerifyImpl fnVfy = creator.apply(ruleObject);
        if (fnVfy != null && fnVfy.isValid()) {
          return fnVfy;
        }
      }
    }

    // 第二优先级，范围验证器
    String name = ruleObject.getName();
    if (name.charAt(0) == '-' || (name.charAt(0) >= '0' && name.charAt(0) <= '9')
        || (name.charAt(0) == '>' || name.charAt(0) == '<')) {
      DataVerifyNumberRange vfyRange = new DataVerifyNumberRange(name);
      if (vfyRange.isValid()) {
        return vfyRange;
      } else {
        ProgramOptions.getLoger().error("Validator %s(DataVerifyNumberRange) is invalid",
            name);
      }
      return null;
    }

    // 第三优先级，外部注入的验证器
    if (externalCreators != null) {
      for (Map.Entry<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> entry : externalCreators
          .entrySet()) {
        // 任意自定义验证器可创建都视为成功
        if (entry.getValue() != null) {
          DataVerifyImpl extVfy = entry.getValue().apply(ruleObject);
          if (extVfy != null && extVfy.isValid()) {
            return extVfy;
          }
        }
      }
    }

    if (ruleObject.isFunctionMode()) {
      ProgramOptions.getLoger().error("Unknown function validator %s", name);
    } else {
      ProgramOptions.getLoger().error("Unknown identify validator %s", name);
    }
    return null;
  }

  static public DataVerifyImpl createValidator(DataValidatorCache cache, DataVerifyImpl.ValidatorTokens ruleObject) {
    if (!isInited) {
      ProgramOptions.getLoger()
          .error(
              "DataValidatorFactory.createValidator() should not be called before DataValidatorFactory.Initialize()");
      return null;
    }
    DataVerifyImpl ret;
    boolean valid = true;
    if (null == cache) {
      ret = createValidator(ruleObject, null);
    } else {
      ret = cache.getValidatorByToken(ruleObject);
      if (null == ret) {
        ret = createValidator(ruleObject, cache.getCreatorMap());
        if (null != ret) {
          cache.putValidator(ruleObject, ret);
        }
      }
    }

    if (ret != null) {
      if (!ret.setup(cache)) {
        valid = false;
      }
    }

    if (ret == null) {
      valid = false;
    } else if (valid) {
      valid = ret.isValid();
    }

    if (!valid) {
      return null;
    }

    return ret;
  }

  static public DataVerifyImpl createSymbolValidator(DataValidatorCache cache,
      String symbol) {
    if (cache == null || symbol == null || symbol.isEmpty()) {
      return null;
    }

    return createValidator(cache, cache.mutableSymbolToTokens(symbol));
  }

  public static HashMap<String, DataVerifyImpl> internalLoadFromFile(DataValidatorCache cache, String filePath) {
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
          Object modeObj = ((Map<?, ?>) ruleObject).getOrDefault("mode", null);
          Object rulesObj = ((Map<?, ?>) ruleObject).get("rules");
          if (nameObj == null || !(nameObj instanceof String)) {
            continue;
          }

          if (rulesObj == null || !(rulesObj instanceof List<?>)) {
            continue;
          }

          String name = (String) nameObj;
          String description = null;
          String mode = "";
          if (descriptionObj != null && descriptionObj instanceof String) {
            description = (String) descriptionObj;
          }
          if (modeObj != null && modeObj instanceof String) {
            mode = (String) modeObj;
          }

          List<ValidatorParameter> rules = new ArrayList<>();
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

          ((ArrayList<ValidatorParameter>) rules).ensureCapacity(((List<?>) rulesObj).size());
          for (Object ruleData : ((List<?>) rulesObj)) {
            String ruleStr = ruleData.toString().trim();
            if (!ruleStr.isEmpty()) {
              // 可能有表达式，需要展开
              LinkedList<ValidatorTokens> subTokens = buildValidatorTokens(ruleStr);
              if (subTokens.size() > 1) {
                ValidatorTokens subToken = new ValidatorTokens("Or", true);
                for (ValidatorTokens token : subTokens) {
                  if (token.isFunctionMode()) {
                    subToken.appendParameter(ValidatorParameter.ofToken(token));
                  } else {
                    subToken.appendParameter(ValidatorParameter.ofString(token.toString()));
                  }
                }
                rules.add(ValidatorParameter.ofToken(subToken));
              } else if (!subTokens.isEmpty()) {
                ValidatorTokens token = subTokens.get(0);
                if (token.isFunctionMode()) {
                  rules.add(ValidatorParameter.ofToken(token));
                } else {
                  rules.add(ValidatorParameter.ofString(token.toString()));
                }
              }
            }
          }

          if (mode.equalsIgnoreCase("and")) {
            DataVerifyImpl validator = new DataVerifyCustomAndRule(name, rules, description, version);
            if (null != ret.put(name, validator)) {
              ProgramOptions.getLoger().warn(
                  "Load custom validator file \"%s\" with more than one rule with name \"%s\", we will use the last one.",
                  filePath,
                  name);
            }
            if (null != cache) {
              cache.putValidator(name, validator);
            }
          } else if (mode.equalsIgnoreCase("not")) {
            DataVerifyImpl validator = new DataVerifyCustomNotRule(name, rules, description, version);
            if (null != ret.put(name, validator)) {
              ProgramOptions.getLoger().warn(
                  "Load custom validator file \"%s\" with more than one rule with name \"%s\", we will use the last one.",
                  filePath,
                  name);
            }
            if (null != cache) {
              cache.putValidator(name, validator);
            }
          } else {
            // Default is or
            DataVerifyImpl validator = new DataVerifyCustomOrRule(name, rules, description, version);
            if (null != ret.put(name, validator)) {
              ProgramOptions.getLoger().warn(
                  "Load custom validator file \"%s\" with more than one rule with name \"%s\", we will use the last one.",
                  filePath,
                  name);
            }
            if (null != cache) {
              cache.putValidator(name, validator);
            }
          }
        }
      }

      return ret;
    } catch (java.io.IOException | NumberFormatException e) {
      ProgramOptions.getLoger().error("Load custom validator file \"%s\" failed, %s", filePath, e.getMessage());
      return null;
    }
  }

  public static HashMap<String, DataVerifyImpl> loadFromFile(DataValidatorCache cache, String filePath) {
    if (null == cache) {
      return internalLoadFromFile(cache, filePath);
    }

    HashMap<String, DataVerifyImpl> existed = cache.getFileCache(filePath);
    if (null != existed) {
      return existed;
    }

    HashMap<String, DataVerifyImpl> newFileCache = internalLoadFromFile(cache, filePath);
    if (null != newFileCache) {
      cache.putFileCache(filePath, newFileCache);
    }

    return newFileCache;
  }
}
