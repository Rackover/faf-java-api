package com.faforever.api.data.listeners;

import com.faforever.api.data.domain.VotingSubject;
import org.springframework.stereotype.Component;

import javax.persistence.PostLoad;

@Component
public class VotingSubjectEnricher {

  @PostLoad
  public void enhance(VotingSubject votingSubject) {
    votingSubject.setNumberOfVotes(votingSubject.getVotes().size());
  }
}
