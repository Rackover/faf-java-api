package com.faforever.api.data.domain;

import com.faforever.api.data.checks.permission.IsModerator;
import com.faforever.api.data.listeners.VotingChoiceEnricher;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(name = "voting_choice")
@ReadPermission(expression = "Prefab.Role.All")
@SharePermission(expression = IsModerator.EXPRESSION)
@DeletePermission(expression = IsModerator.EXPRESSION)
@UpdatePermission(expression = IsModerator.EXPRESSION)
@CreatePermission(expression = IsModerator.EXPRESSION)
@Include(rootLevel = true, type = VotingChoice.TYPE_NAME)
@Setter
@EntityListeners(VotingChoiceEnricher.class)
public class VotingChoice extends AbstractEntity {
  public static final String TYPE_NAME = "votingChoice";

  private String name;
  private int numberOfAnswers;
  private String description;
  private VotingQuestion votingQuestion;
  private List<VotingAnswer> votingAnswers;

  @NotNull
  @Column(name = "name", nullable = false)
  public String getName() {
    return name;
  }

  @Transient
  @ComputedAttribute
  public int getNumberOfAnswers() {
    return numberOfAnswers;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  @JoinColumn(name = "voting_question_id")
  @ManyToOne(fetch = FetchType.LAZY)
  public VotingQuestion getVotingQuestion() {
    return votingQuestion;
  }

  @Exclude
  @OneToMany(mappedBy = "votingChoice", cascade = CascadeType.ALL, orphanRemoval = true)
  public List<VotingAnswer> getVotingAnswers() {
    return votingAnswers;
  }
}
