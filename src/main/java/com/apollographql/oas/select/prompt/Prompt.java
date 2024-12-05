package com.apollographql.oas.select.prompt;

import java.util.Scanner;

public class Prompt {
  private static Prompt instance;
  private Input input;

  static public Prompt get() {
    if (instance == null)
      instance = new Prompt();

    return instance;
  }

  static public Prompt get(Input input) {
    final Prompt prompt = Prompt.get();
    prompt.setRecorded(input);
    return prompt;
  }

  private Prompt() {
    this.input = new ConsoleInput();
  }

  private Input getRecorded() {
    return input;
  }

  private void setRecorded(final Input input) {
    this.input = input;
  }

  public boolean prompt(final String msg) {
    return getRecorded().next(msg);
  }

  public static class ConsoleInput implements Input {
//    int index = 0;
//    static String[] recorder = new String[]{
//      // 0    1    2    3    4    5    6    7    8    9   10   11   12   13   14   15
//      "n", "n", "n", "n", "n", "y", "y", "y", "n", "y", "y", "y", "y", "n", "n", "n"
//    };

    final Scanner scanner = new Scanner(System.in);

    public boolean next(final String prompt) {
      System.out.println(prompt + " (Y/n)");
      final String next = scanner.nextLine();
      return next.equals("") || next.equalsIgnoreCase("y");
    }
  }
}
