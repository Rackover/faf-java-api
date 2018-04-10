package com.faforever.api.data.domain;

import com.faforever.api.data.checks.permission.IsModerator;
import com.faforever.api.data.listeners.VotingQuestionEnricher;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
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
@Table(name = "voting_question")
@ReadPermission(expression = "Prefab.Role.All")
@SharePermission(expression = IsModerator.EXPRESSION)
@DeletePermission(expression = IsModerator.EXPRESSION)
@UpdatePermission(expression = IsModerator.EXPRESSION)
@CreatePermission(expression = IsModerator.EXPRESSION)
@Include(rootLevel = true, type = VotingQuestion.TYPE_NAME)
@Setter
@EntityListeners(VotingQuestionEnricher.class)
public class VotingQuestion extends AbstractEntity {
  public static final String TYPE_NAME = "votingQuestion";

  private int numberOfAnswers;
  private String question;
  private String description;
  private int maxAnswers;
  private VotingSubject votingSubject;
  private List<VotingChoice> votingChoices;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voting_subject_id")
  public VotingSubject getVotingSubject() {
    return votingSubject;
  }

  @NotNull
  @Column(name = "question", nullable = false)
  public String getQuestion() {
    return question;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  @Column(name = "max_answers")
  public int getMaxAnswers() {
    return maxAnswers;
  }

  @Transient
  @ComputedAttribute
  public int getNumberOfAnswers() {
    return numberOfAnswers;
  }

  @OneToMany(mappedBy = "votingQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
  public List<VotingChoice> getVotingChoices() {
    return votingChoices;
  }
}
