package com.apollographql.oas.gen.prompt;

public interface Input {
  boolean yesNo(final String id, final String prompt);
  char yesNoSelect(final String id, final String prompt);
}
