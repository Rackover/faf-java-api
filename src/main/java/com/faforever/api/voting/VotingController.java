package com.faforever.api.voting;

import com.faforever.api.data.domain.Vote;
import com.faforever.api.player.PlayerService;
import com.faforever.api.security.OAuthScope;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping(path = VotingController.PATH)
public class VotingController {
  static final String PATH = "/vote";
  private static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";
  private final VotingService votingService;
  private final PlayerService playerService;

  @Inject
  public VotingController(VotingService votingService, PlayerService playerService) {
    this.votingService = votingService;
    this.playerService = playerService;
  }

  @ApiOperation(value = "Post a vote")
  @PreAuthorize("#oauth2.hasScope('" + OAuthScope._Vote + "')")
  @RequestMapping(method = RequestMethod.POST, produces = JSON_API_MEDIA_TYPE)
  public void post(@RequestBody Vote vote, Authentication authentication) {
    votingService.saveVote(vote, playerService.getPlayer(authentication));
  }
}
