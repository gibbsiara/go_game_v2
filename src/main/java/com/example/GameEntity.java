package com.example;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private int boardSize;
    private String result;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<MoveEntity> moves;

    public GameEntity() {}

    public GameEntity(int boardSize) {
        this.boardSize = boardSize;
        this.startTime = LocalDateTime.now();
        this.result = "ONGOING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public int getBoardSize() { return boardSize; }
    public void setBoardSize(int boardSize) { this.boardSize = boardSize; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public List<MoveEntity> getMoves() { return moves; }
    public void setMoves(List<MoveEntity> moves) { this.moves = moves; }
}