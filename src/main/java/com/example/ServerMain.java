package com.example;

import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        int port;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.out.println("Niepoprawny port, używam domyślnego 12345");
                port = 12345;
            }
        } else {
            System.out.print("Podaj port serwera [domyślnie 12345]: ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                port = 12345;
            } else {
                try {
                    port = Integer.parseInt(input);
                } catch (Exception e) {
                    System.out.println("Niepoprawny port, używam 12345");
                    port = 12345;
                }
            }
        }

        System.out.println("Uruchamianie serwera na porcie: " + port);
        GoServer.getInstance().start(port);
    }
}