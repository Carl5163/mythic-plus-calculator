package com.cwahler.application.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cwahler.application.entities.Dungeon;

import java.util.List;

public interface DungeonRepository extends JpaRepository<Dungeon, Long> {
    List<Dungeon> findByNameStartsWithIgnoreCase(String name);
}
