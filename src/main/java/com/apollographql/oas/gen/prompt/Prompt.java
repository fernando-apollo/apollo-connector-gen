package com.apollographql.oas.gen.prompt;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Prompt {
  private Input input;

  private Prompt(Input input) {
    this.input = input;
  }

  static public Prompt create(Input input) {
    return new Prompt(input);
  }

  private Input getInput() {
    return input;
  }

  private void setInput(final Input input) {
    this.input = input;
  }

  public boolean yesNo(final String id, final String msg) {
    return getInput().yesNo(id, msg);
  }

  public char yesNoSelect(final String id, final String msg) {
    return getInput().yesNoSelect(id, msg);
  }

  public static class ConsoleInput implements Input {
    final protected Scanner scanner = new Scanner(System.in);

    public boolean yesNo(final String id, final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': No");
      final String next = scanner.nextLine();
      return next.equals("") || next.equalsIgnoreCase("y");
    }

    public char yesNoSelect(final String id, final String prompt) {
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
    public boolean yesNo(final String id, final String prompt) {
      final String next = record[track++].substring(1, 2);
      final boolean response = next.equalsIgnoreCase("y");
      System.out.println(prompt + " 'y': Yes, 'n': No" + " -> " + next + ", response? " + response);
      return response;
    }

    @Override
    public char yesNoSelect(final String id, final String prompt) {
      final String next = record[track++].substring(1, 2);
      System.out.println(prompt + " 'y': Yes, 'n': Skip, 's': Select" + " -> " + next);
      if (next.equalsIgnoreCase("y")) return 'y';
      else if (next.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }
  }

  /* contains records of the form "key" -> "answer" */
  public static class MapPlayer implements Input {
    private Map<String, String> records;
    int track = 0;

    public MapPlayer(final Map<String, String> records) {
      this.records = records;
    }

    @Override
    public boolean yesNo(final String id, final String prompt) {
      final String next = records.get(id);
      if (next == null) {
        throw new IllegalArgumentException("Could not find response for '" + id + "'");
      }

      final boolean response = next.equalsIgnoreCase("y");
      System.out.println(prompt + " 'y': Yes, 'n': No" + " -> " + next + ", response? " + response);
      return response;
    }

    @Override
    public char yesNoSelect(final String id, final String prompt) {
      final String next = records.get(id);
      if (next == null) {
        throw new IllegalArgumentException("Could not find response for '" + id + "'");
      }

      System.out.println(prompt + " 'y': Yes, 'n': Skip, 's': Select" + " -> " + next);
      if (next.equalsIgnoreCase("y")) return 'y';
      else if (next.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }
  }

  public static class Recorder extends Prompt.ConsoleInput {
    final List<Pair<String, String>> records = new LinkedList<>();

    @Override
    public boolean yesNo(final String id, final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': No");
      final String answer = scanner.nextLine();
      records.add(new ImmutablePair<>(answer, prompt.replaceAll("\\n", "")));
      return answer.equals("") || answer.equalsIgnoreCase("y");
    }

    @Override
    public char yesNoSelect(final String id, final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': Skip, 's': Select");
      final String answer = scanner.nextLine();
      records.add(new ImmutablePair<>(answer, prompt.replaceAll("\\n", "")));

      if (answer.equals("") || answer.equalsIgnoreCase("y")) return 'y';
      else if (answer.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }

    public List<Pair<String, String>> getRecords() {
      return records;
    }
  }

  public static class MapRecorder extends Prompt.ConsoleInput {
    final List<Pair<String, String>> records = new LinkedList<>();

    @Override
    public boolean yesNo(final String id, final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': No");
      final String answer = scanner.nextLine();
      records.add(new ImmutablePair<>(id, answer));
      return answer.equals("") || answer.equalsIgnoreCase("y");
    }

    @Override
    public char yesNoSelect(final String id, final String prompt) {
      System.out.println(prompt + " 'y': Yes, 'n': Skip, 's': Select");
      final String answer = scanner.nextLine();
      records.add(new ImmutablePair<>(id, answer));

      if (answer.equals("") || answer.equalsIgnoreCase("y")) return 'y';
      else if (answer.equalsIgnoreCase("s")) return 's';
      else return 'n';
    }

    public List<Pair<String, String>> getRecords() {
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
        public boolean yesNo(final String id, final String prompt) {
          return true;
        }

        @Override
        public char yesNoSelect(final String id, final String prompt) {
          return 'y';
        }
      };
    }

    public static Input player(String[] record) {
      return new Player(record);
    }

    public static Input mapPlayer(Map<String, String> records) {
      return new MapPlayer(records);
    }

    public static Input recorder() {
      return new Recorder();
    }

    public static Input mapRecorder() {
      return new MapRecorder();
    }
  }
}
