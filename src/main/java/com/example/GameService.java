package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MoveRepository moveRepository;

    /**
     * Tworzy nową grę w bazie danych i zwraca jej ID.
     */
    @Transactional
    public Long createNewGame(int size) {
        GameEntity game = new GameEntity(size);
        game = gameRepository.save(game);
        System.out.println("DB: Utworzono nową grę o ID: " + game.getId());
        return game.getId();
    }

    /**
     * Zapisuje ruch w bazie danych.
     */
    @Transactional
    public void saveMove(Long gameId, int moveNum, int x, int y, StoneColor color, String type) {
        GameEntity game = gameRepository.findById(gameId).orElse(null);
        if (game != null) {
            MoveEntity move = new MoveEntity(game, moveNum, x, y, color, type);
            moveRepository.save(move);
            System.out.println("DB: Zapisano ruch " + moveNum + " (" + type + ")");
        }
    }

    /**
     * Aktualizuje wynik gry po jej zakończeniu.
     */
    @Transactional
    public void finishGame(Long gameId, String result) {
        GameEntity game = gameRepository.findById(gameId).orElse(null);
        if (game != null) {
            game.setResult(result);
            gameRepository.save(game);
            System.out.println("DB: Zakończono grę " + gameId + " z wynikiem: " + result);
        }
    }

    /**
     * Specjalna metoda do odtwarzania powtórki dla konkretnego klienta.
     * Pobiera ruchy i wysyła je z opóźnieniem do klienta.
     */
    public void playReplayForClient(Long gameId, ClientHandler client) {
        List<MoveEntity> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
        
        if (moves.isEmpty()) {
            client.sendMessage("MESSAGE Błąd: Gra o ID " + gameId + " nie istnieje lub nie ma ruchów.");
            return;
        }

        GameEntity gameEntity = gameRepository.findById(gameId).orElseThrow();
        
        Game simulationGame = new Game(gameEntity.getBoardSize());
        
        simulationGame.setPersistenceEnabled(false); 

        client.sendMessage("MESSAGE --- Rozpoczynam powtórkę gry ID: " + gameId + " ---");
        
        client.sendMessage("BOARD " + simulationGame.getBoard().getBoardStateString());

        new Thread(() -> {
            try {
                for (MoveEntity move : moves) {
                    Thread.sleep(800); 

                    if ("MOVE".equals(move.getType())) {
                        simulationGame.processMove(move.getX(), move.getY(), move.getColor());
                    } else if ("PASS".equals(move.getType())) {
                        simulationGame.processPass(move.getColor());
                    }
                    else if ("SURRENDER".equals(move.getType())) {
                         client.sendMessage("MESSAGE Gracz " + move.getColor() + " poddał się (w historii).");
                    }
                    
                    client.sendMessage("BOARD " + simulationGame.getBoard().getBoardStateString());
                }
                client.sendMessage("MESSAGE --- Koniec powtórki ---");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}