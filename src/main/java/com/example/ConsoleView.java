package com.example;

import java.util.Scanner;

public class ConsoleView {
    private Scanner scanner;

    public ConsoleView(Scanner scanner) {
        this.scanner = scanner;
    }

    public void displayMessage(String msg){
        System.out.println(msg);
    }

    public void displayBoard(String boardData){
        clearScreen();
        
        System.out.println(boardData.replaceAll(";", "\n"));
    }

    public String getUserInput(){
        return scanner.nextLine();
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}