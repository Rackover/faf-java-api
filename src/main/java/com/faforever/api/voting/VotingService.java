package com.faforever.api.voting;

import com.faforever.api.data.domain.Player;
import com.faforever.api.data.domain.Vote;
import com.faforever.api.data.domain.VotingChoice;
import com.faforever.api.data.domain.VotingSubject;
import com.faforever.api.error.ApiException;
import com.faforever.api.error.Error;
import com.faforever.api.error.ErrorCode;
import com.faforever.api.game.GamePlayerStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Optional;

@Service
public class VotingService {
  private final VoteRepository voteRepository;
  private final VotingAnswerRepository votingAnswerRepository;
  private final VotingSubjectRepository votingSubjectRepository;
  private final GamePlayerStatsRepository gamePlayerStatsRepository;
  private final VotingChoiceRepository votingChoiceRepository;

  public VotingService(VoteRepository voteRepository, VotingAnswerRepository votingAnswerRepository, VotingSubjectRepository votingSubjectRepository, GamePlayerStatsRepository gamePlayerStatsRepository, VotingChoiceRepository votingChoiceRepository) {
    this.voteRepository = voteRepository;
    this.votingAnswerRepository = votingAnswerRepository;
    this.votingSubjectRepository = votingSubjectRepository;
    this.gamePlayerStatsRepository = gamePlayerStatsRepository;
    this.votingChoiceRepository = votingChoiceRepository;
  }

  void saveVote(Vote vote, Player player) {
    vote.setPlayer(player);
    Assert.notNull(vote.getVotingSubject(), "You must specify a subject");
    Optional<Vote> byPlayerAndVotingSubject = voteRepository.findByPlayerAndVotingSubject(player, vote.getVotingSubject());
    if (byPlayerAndVotingSubject.isPresent()) {
      throw new ApiException(new Error(ErrorCode.VOTED_TWICE));
    }
    VotingSubject subject = votingSubjectRepository.findOne(vote.getVotingSubject().getId());
    int gamesPlayed = gamePlayerStatsRepository.countByPlayer(player);
    //TODO: count only valid games
    if (gamesPlayed < subject.getMinGamesToVote()) {
      throw new ApiException(new Error(ErrorCode.NOT_ENOUGH_GAMES, gamesPlayed, subject.getMinGamesToVote()));
    }

    if (vote.getVotingAnswers() == null) {
      vote.setVotingAnswers(Collections.emptyList());
    }

    vote.getVotingAnswers().forEach(votingAnswer -> {
      VotingChoice choice = votingChoiceRepository.findOne(votingAnswer.getVotingChoice().getId());
      long count = vote.getVotingAnswers().stream()
        .filter(votingAnswer1 -> votingAnswer1.getVotingQuestion().equals(votingAnswer.getVotingQuestion()))
        .count();
      int maxAnswers = choice.getVotingQuestion().getMaxAnswers();
      if (maxAnswers > count) {
        throw new ApiException(new Error(ErrorCode.TOO_MANY_ANSWERS, count, maxAnswers));
      }
    });

    votingAnswerRepository.save(vote.getVotingAnswers());
    voteRepository.save(vote);
  }
}
