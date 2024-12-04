package com.apollographql.oas.select.prompt;

import java.util.Scanner;

public class Prompt {
  static int index = 0;
  static String[] recorder = new String[] {
  // 0    1    2    3    4    5    6    7    8    9   10   11   12   13   14   15
    "n", "n", "n", "n", "n", "y", "y", "y", "n", "y", "y", "y", "y", "n", "n", "n"
  };

  public static boolean prompt(final String msg) {
//    final String response = recorder[index++];
//    System.out.println(msg + " (Y/n) -> " + response + " (" + (index - 1) + ")");
//    return response.equalsIgnoreCase("y");

    System.out.println(msg + " (Y/n)");
    final String next = new Scanner(System.in).nextLine();
    return next.equals("") || next.equalsIgnoreCase("y");

//    return true;
  }
}
