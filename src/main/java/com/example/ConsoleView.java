package com.example;

import java.util.Scanner;

/**
 * Provides a text-based interface for displaying game status and debugging.
 * Useful for testing logic without the full GUI.
 */
public class ConsoleView {
    private Scanner scanner;

    /**
     * Initializes the console view with a scanner for input.
     * @param scanner The Scanner instance to read user input.
     */
    public ConsoleView(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Prints a generic message to the console.
     * @param msg The message to display.
     */
    public void displayMessage(String msg){
        System.out.println(msg);
    }

    /**
     * Formats and prints the board state to the console.
     * Replaces internal delimiters with newlines for readability.
     * @param boardData The serialized board string.
     */
    public void displayBoard(String boardData){
        clearScreen();
        
        System.out.println(boardData.replaceAll(";", "\n"));
    }

    /**
     * Reads a line of input from the user.
     * @return The input string entered by the user.
     */
    public String getUserInput(){
        return scanner.nextLine();
    }

    /**
     * Clears the console screen using ANSI escape codes.
     */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}