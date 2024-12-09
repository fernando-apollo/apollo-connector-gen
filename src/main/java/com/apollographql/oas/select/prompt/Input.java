package com.apollographql.oas.select.prompt;

public interface Input {
  boolean yesNo(final String prompt);
  char yesNoSelect(final String prompt);
}
