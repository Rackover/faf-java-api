package com.faforever.api.data.listeners;

import com.faforever.api.data.domain.VotingChoice;
import org.springframework.stereotype.Component;

import javax.persistence.PostLoad;

@Component
public class VotingChoiceEnricher {

  @PostLoad
  public void enhance(VotingChoice votingChoice) {
    votingChoice.setNumberOfAnswers(votingChoice.getVotingAnswers().size());
  }
}
