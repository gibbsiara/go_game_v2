package com.example;

import jakarta.persistence.*;

@Entity
@Table(name = "moves")
public class MoveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private GameEntity game;

    private int moveNumber;
    private int x;
    private int y;
    
    @Enumerated(EnumType.STRING)
    private StoneColor color;

    private String type;

    public MoveEntity() {}

    public MoveEntity(GameEntity game, int moveNumber, int x, int y, StoneColor color, String type) {
        this.game = game;
        this.moveNumber = moveNumber;
        this.x = x;
        this.y = y;
        this.color = color;
        this.type = type;
    }

    public Long getId() { return id; }
    public GameEntity getGame() { return game; }
    public int getMoveNumber() { return moveNumber; }
    public int getX() { return x; }
    public int getY() { return y; }
    public StoneColor getColor() { return color; }
    public String getType() { return type; }
}