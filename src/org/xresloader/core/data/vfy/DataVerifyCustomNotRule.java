package org.xresloader.core.data.vfy;

import java.util.ArrayList;

public class DataVerifyCustomNotRule extends DataVerifyCustomRule {

  public DataVerifyCustomNotRule(String name, ArrayList<String> rules, String description, int version) {
    super(name, rules, description, version);
  }

  @Override
  public boolean batchGetSubValidators(double number, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      return true;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(number, res)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean batchGetSubValidators(long number, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      return true;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(number, res)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean batchGetSubValidators(String input, DataVerifyResult res) {
    if (this.validators == null || this.validators.isEmpty()) {
      return true;
    }

    for (DataVerifyImpl vfy : this.validators) {
      if (vfy.get(input, res)) {
        return false;
      }
    }

    return true;
  }

}
