package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<MoveEntity, Long> {
    List<MoveEntity> findByGameIdOrderByMoveNumberAsc(Long gameId);
}