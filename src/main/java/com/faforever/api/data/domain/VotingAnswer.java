package com.faforever.api.data.domain;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "voting_answer")
@Include(type = VotingAnswer.TYPE_NAME)
@ReadPermission(expression = "Prefab.Role.None")
@Setter
public class VotingAnswer extends AbstractEntity {
  public static final String TYPE_NAME = "votingAnswer";

  private VotingQuestion votingQuestion;
  private Vote vote;
  private VotingChoice votingChoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voting_question_id")
  public VotingQuestion getVotingQuestion() {
    return votingQuestion;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vote_id")
  public Vote getVote() {
    return vote;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voting_choice_id")
  public VotingChoice getVotingChoice() {
    return votingChoice;
  }
}
