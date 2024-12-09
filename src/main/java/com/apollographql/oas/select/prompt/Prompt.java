package com.apollographql.oas.select.prompt;

import java.util.LinkedHashMap;
import java.util.Map;
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

  public boolean yesNo(final String msg) {
    return getRecorded().yesNo(msg);
  }

  public char yesNoSelect(final String msg) {
    return getRecorded().yesNoSelect(msg);
  }

  public static class ConsoleInput implements Input {
    final protected Scanner scanner = new Scanner(System.in);

    public boolean yesNo(final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': No");
      final String next = scanner.nextLine();
      return next.equals("") || next.equalsIgnoreCase("y");
    }

    public char yesNoSelect(final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': Skip, 's': Select");
      final String next = scanner.nextLine();

      if (next.equals("") || next.equalsIgnoreCase("y")) return 'y';
      else if (next.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }
  }

  public static class Player implements Input {
    private final String[] record;
    int track = 0;

    public Player(final String[] record) {
      this.record = record;
    }

    @Override
    public boolean yesNo(final String prompt) {
      final String next = record[track++];
      return next.equals("") || next.equalsIgnoreCase("y");
    }

    @Override
    public char yesNoSelect(final String prompt) {
      final String next = record[track++];
      if (next.equals("") || next.equalsIgnoreCase("y")) return 'y';
      else if (next.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }
  }

  public static class Recorder extends Prompt.ConsoleInput {
    final Map<String, String> records = new LinkedHashMap<>();

    @Override
    public boolean yesNo(final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': No");
      final String answer = scanner.nextLine();
      records.put(prompt, answer);
      return answer.equals("") || answer.equalsIgnoreCase("y");
    }

    @Override
    public char yesNoSelect(final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': Skip, 's': Select");
      final String next = scanner.nextLine();
      records.put(prompt, next);

      if (next.equals("") || next.equalsIgnoreCase("y")) return 'y';
      else if (next.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }

    public Map<String, String> getRecords() {
      return records;
    }
  }

  public static class Factory {
    public static Input console() {
      return new ConsoleInput();
    }

    public static Input yes() {
      return new Input() {
        @Override
        public boolean yesNo(final String prompt) {
          return true;
        }

        @Override
        public char yesNoSelect(final String prompt) {
          return 'y';
        }
      };
    }

    public static Input player(String[] record) {
      return new Player(record);
    }

    public static Input recorder() {
      return new Recorder();
    }
  }
}
