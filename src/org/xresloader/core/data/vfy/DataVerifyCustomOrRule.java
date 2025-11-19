package org.xresloader.core.data.vfy;

import java.util.List;

public class DataVerifyCustomOrRule extends DataVerifyCustomRule {

  public DataVerifyCustomOrRule(String name, List<ValidatorParameter> rules, String description, int version) {
    super(name, rules, description, version);
  }

  @Override
  public boolean batchGetSubValidators(double number, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      return false;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(number, res)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean batchGetSubValidators(long number, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      return false;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(number, res)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean batchGetSubValidators(String input, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      return false;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(input, res)) {
        return true;
      }
    }

    return false;
  }

}
