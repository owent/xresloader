package org.xresloader.core.data.vfy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataValidatorCache {
  private final HashMap<String, DataVerifyImpl> validator = new HashMap<>();
  private final HashMap<String, HashMap<String, DataVerifyImpl>> fileCache = new HashMap<>();
  private final HashMap<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> creator = new HashMap<>();
  private final HashMap<String, DataVerifyImpl.ValidatorTokens> symbolToTokens = new HashMap<>();
  private final List<DataVerifyImpl> pendingToSetup = new LinkedList<>();

  public DataValidatorCache() {
  }

  public void registerCreator(String name,
      Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl> func) {
    if (func == null) {
      return;
    }

    synchronized (this.creator) {
      this.creator.put(name.toLowerCase(), func);
    }
  }

  public Map<String, Function<DataVerifyImpl.ValidatorTokens, DataVerifyImpl>> getCreatorMap() {
    synchronized (this.creator) {
      return this.creator;
    }
  }

  public DataVerifyImpl getValidatorByKey(String key) {
    if (key == null || key.isEmpty()) {
      return null;
    }

    synchronized (this.validator) {
      return this.validator.get(key);
    }
  }

  public DataVerifyImpl getValidatorByToken(DataVerifyImpl.ValidatorTokens tokens) {
    if (tokens == null) {
      return null;
    }

    return this.getValidatorByKey(tokens.getKey());
  }

  public void putValidator(DataVerifyImpl.ValidatorTokens tokens, DataVerifyImpl validator) {
    if (validator == null || tokens == null) {
      return;
    }

    String key = tokens.getKey();
    if (key == null || key.isEmpty()) {
      return;
    }

    synchronized (this.validator) {
      DataVerifyImpl oldValue = this.validator.getOrDefault(key, null);
      if (oldValue == validator) {
        return;
      }

      this.validator.put(key, validator);
    }

    synchronized (this.pendingToSetup) {
      this.pendingToSetup.add(validator);
    }
  }

  public void putValidator(String name, DataVerifyImpl validator) {
    if (validator == null || name == null || name.isEmpty()) {
      return;
    }

    synchronized (this.validator) {
      DataVerifyImpl oldValue = this.validator.getOrDefault(name, null);
      if (oldValue == validator) {
        return;
      }

      this.validator.put(name, validator);
    }

    synchronized (this.pendingToSetup) {
      this.pendingToSetup.add(validator);
    }
  }

  public DataVerifyImpl popUnsetupValidator() {
    synchronized (this.pendingToSetup) {
      if (this.pendingToSetup.isEmpty()) {
        return null;
      }

      LinkedList<DataVerifyImpl> list = (LinkedList<DataVerifyImpl>) this.pendingToSetup;
      return list.pop();
    }
  }

  public HashMap<String, DataVerifyImpl> getFileCache(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return null;
    }

    synchronized (this.fileCache) {
      return this.fileCache.getOrDefault(filePath, null);
    }
  }

  public void putFileCache(String filePath, HashMap<String, DataVerifyImpl> validators) {
    if (filePath == null || filePath.isEmpty() || validators == null) {
      return;
    }

    synchronized (this.fileCache) {
      this.fileCache.put(filePath, validators);
    }
  }

  public DataVerifyImpl.ValidatorTokens mutableSymbolToTokens(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }

    synchronized (this.symbolToTokens) {
      DataVerifyImpl.ValidatorTokens ret = this.symbolToTokens.getOrDefault(name, null);
      if (ret == null) {
        ret = new DataVerifyImpl.ValidatorTokens(name, false);
        this.symbolToTokens.put(name, ret);
      }
      return ret;
    }
  }
}
