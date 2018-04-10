package com.faforever.api.game;

import com.faforever.api.data.domain.GamePlayerStats;
import com.faforever.api.data.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlayerStatsRepository extends JpaRepository<GamePlayerStats, Integer> {
  int countByPlayer(Player player);
}
