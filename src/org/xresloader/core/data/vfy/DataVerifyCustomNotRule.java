package org.xresloader.core.data.vfy;

import java.util.ArrayList;

public class DataVerifyCustomNotRule extends DataVerifyCustomRule {

  public DataVerifyCustomNotRule(String name, ArrayList<String> rules, String description, int version) {
    super(name, rules, description, version);
  }

  @Override
  public boolean batchGetSubValidators(double number, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      if (res != null) {
        res.success = true;
        res.value = number;
      }
      return true;
    }

    Object oldValue = null;
    if (res != null) {
      oldValue = res.value;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(number, res)) {
        if (res != null) {
          res.success = false;
        }
        return false;
      }
    }

    if (res != null) {
      res.success = true;
      // 优先使用原始值，不改变已经通过的验证器转化的数据
      if (oldValue != null) {
        res.value = oldValue;
      } else {
        res.value = number;
      }
    }
    return true;
  }

  @Override
  public boolean batchGetSubValidators(long number, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      if (res != null) {
        res.success = true;
        res.value = number;
      }
      return true;
    }

    Object oldValue = null;
    if (res != null) {
      oldValue = res.value;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(number, res)) {
        if (res != null) {
          res.success = false;
        }
        return false;
      }
    }

    if (res != null) {
      res.success = true;
      // 优先使用原始值，不改变已经通过的验证器转化的数据
      if (oldValue != null) {
        res.value = oldValue;
      } else {
        res.value = number;
      }
    }
    return true;
  }

  @Override
  public boolean batchGetSubValidators(String input, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      if (res != null) {
        res.success = true;
        res.value = input;
      }
      return true;
    }

    Object oldValue = null;
    if (res != null) {
      oldValue = res.value;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(input, res)) {
        if (res != null) {
          res.success = false;
        }
        return false;
      }
    }

    if (res != null) {
      res.success = true;
      // 优先使用原始值，不改变已经通过的验证器转化的数据
      if (oldValue != null) {
        res.value = oldValue;
      } else {
        res.value = input;
      }
    }
    return true;
  }

}
