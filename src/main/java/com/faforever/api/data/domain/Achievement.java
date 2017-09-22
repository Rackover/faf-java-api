package com.faforever.api.data.domain;

import com.faforever.api.data.listeners.AchievementLocalizationListener;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.OffsetDateTime;

@Entity
@Table(name = "achievement_definitions")
@Include(rootLevel = true, type = "achievement")
@EntityListeners(AchievementLocalizationListener.class)
@Setter
public class Achievement {

  private String id;
  private int order;
  private String nameKey;
  private String descriptionKey;
  private AchievementType type;
  private Integer totalSteps;
  private String revealedIconUrl;
  private String unlockedIconUrl;
  private AchievementState initialState;
  private int experiencePoints;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private AchievementStatistics statistics;

  // Set by AchievementLocalizationListener
  private String name;
  private String description;

  @Id
  @Column(name = "id")
  public String getId() {
    return id;
  }

  @Column(name = "\"order\"")
  public int getOrder() {
    return order;
  }

  @Column(name = "name_key")
  @Exclude
  public String getNameKey() {
    return nameKey;
  }

  @Transient
  @ComputedAttribute
  public String getName() {
    return name;
  }

  @Column(name = "description_key")
  @Exclude
  public String getDescriptionKey() {
    return descriptionKey;
  }

  @Transient
  @ComputedAttribute
  public String getDescription() {
    return description;
  }

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  public AchievementType getType() {
    return type;
  }

  @Column(name = "total_steps")
  public Integer getTotalSteps() {
    return totalSteps;
  }

  @Column(name = "revealed_icon_url")
  public String getRevealedIconUrl() {
    return revealedIconUrl;
  }

  @Column(name = "unlocked_icon_url")
  public String getUnlockedIconUrl() {
    return unlockedIconUrl;
  }

  @Column(name = "initial_state")
  @Enumerated(value = EnumType.STRING)
  public AchievementState getInitialState() {
    return initialState;
  }

  @Column(name = "experience_points")
  public int getExperiencePoints() {
    return experiencePoints;
  }

  @Column(name = "create_time")
  public OffsetDateTime getCreateTime() {
    return createTime;
  }

  @Column(name = "update_time")
  public OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @OneToOne(mappedBy = "achievement", fetch = FetchType.LAZY)
  public AchievementStatistics getStatistics() {
    return statistics;
  }
}
