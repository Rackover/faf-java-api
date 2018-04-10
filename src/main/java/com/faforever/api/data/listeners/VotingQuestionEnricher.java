package com.faforever.api.data.listeners;

import com.faforever.api.data.domain.VotingQuestion;
import org.springframework.stereotype.Component;

import javax.persistence.PostLoad;

@Component
public class VotingQuestionEnricher {

  @PostLoad
  public void enhance(VotingQuestion votingQuestion) {
    long numberOfAnswers = votingQuestion.getVotingChoices().stream()
      .map(votingChoice -> votingChoice.getVotingAnswers().stream())
      .count();
    votingQuestion.setNumberOfAnswers((int) numberOfAnswers);
  }
}
