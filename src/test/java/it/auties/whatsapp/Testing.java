package it.auties.whatsapp;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class Testing {
  public static void main(String[] args) throws NumberParseException {
    System.out.println(PhoneNumberUtil.getInstance().parse("393495089819", null));
  }
}
