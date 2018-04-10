package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "vote")
@Include(type = Vote.TYPE_NAME)
@ReadPermission(expression = "Prefab.Role.None")
@Setter
public class Vote extends AbstractEntity {
  public static final String TYPE_NAME = "vote";

  private Player player;
  private VotingSubject votingSubject;
  private List<VotingAnswer> votingAnswers;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id")
  public Player getPlayer() {
    return player;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voting_subject_id")
  public VotingSubject getVotingSubject() {
    return votingSubject;
  }

  @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
  public List<VotingAnswer> getVotingAnswers() {
    return votingAnswers;
  }
}
