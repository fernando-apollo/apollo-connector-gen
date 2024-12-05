package com.apollographql.oas.select.prompt;

import com.apollographql.oas.select.nodes.Type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Prompt {
  private static Prompt instance;
  private Input input;
  private boolean muted;
  private Type origin;

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
    if (isMuted()) {
      System.out.println(">>>>>>>> ignored prompt bc I'm muted");
      return true;
    }
    return getRecorded().next(msg);
  }

  public void mute(final boolean mute, final Type origin) {
    if (this.origin == null) {
      System.out.println(">>>>>>>> mute instruction " + mute + " from " + origin);
      this.muted = mute;
      this.origin = origin;
    }
    else if (mute != this.muted && this.origin == origin) {
      System.out.println(">>>>>>>> mute change to " + mute + " accepted from " + origin);
      this.muted = mute;

      if (!this.muted) // we can clear the origin
      {
        System.out.println(">>>>>>>> mute change cleared origin");
        this.origin = null;
      }
    }
    else {
      System.out.println(">>>>>>>> ignored mute instruction bc origin is different");
    }
  }

  public boolean isMuted() {
    return muted;
  }

  public static class ConsoleInput implements Input {
    final protected Scanner scanner = new Scanner(System.in);

    public boolean next(final String prompt) {
      System.out.println(prompt + " (Y/n)");
      final String next = scanner.nextLine();
      return next.equals("") || next.equalsIgnoreCase("y");
    }
  }

  public static class Player implements Input {
    private final String[] record;
    int track = 0;

    public Player(final String[] record) {
      this.record = record;
    }

    @Override
    public boolean next(final String prompt) {
      final String next = record[track++];
      return next.equals("") || next.equalsIgnoreCase("y");
    }
  }

  public static class Recorder extends Prompt.ConsoleInput {
    final Map<String, String> records = new LinkedHashMap<>();

    @Override
    public boolean next(final String prompt) {
      System.out.println(prompt + " (Y/n)");
      final String answer = scanner.nextLine();
      records.put(prompt, answer);
      return answer.equals("") || answer.equalsIgnoreCase("y");
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
      return prompt -> true;
    }

    public static Input player(String[] record) {
      return new Player(record);
    }

    public static Input recorder() {
      return new Recorder();
    }
  }
}
