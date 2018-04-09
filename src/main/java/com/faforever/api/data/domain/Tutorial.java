package com.faforever.api.data.domain;

import com.faforever.api.data.checks.permission.IsModerator;
import com.faforever.api.data.listeners.TutorialEnricherListener;
import com.github.jasminb.jsonapi.annotations.Type;
import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Audit.Action;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "tutorial")
@Setter
@Include(rootLevel = true, type = Tutorial.TYPE_NAME)
@SharePermission(expression = IsModerator.EXPRESSION)
@DeletePermission(expression = IsModerator.EXPRESSION)
@UpdatePermission(expression = IsModerator.EXPRESSION)
@CreatePermission(expression = IsModerator.EXPRESSION)
@Audit(action = Action.DELETE, logStatement = "Tutorial with name '{0}' and id' {1}' deleted", logExpressions = "${tutorial.tile},${tutorial.id}")
@ReadPermission(expression = "Prefab.Role.All")
@EntityListeners(TutorialEnricherListener.class)
@Type(Tutorial.TYPE_NAME)
public class Tutorial extends AbstractEntity {
  public static final String TYPE_NAME = "tutorial";
  private String description;
  private String title;
  private String category;
  private String thumbnailUrl;
  private int ordinal;
  private boolean launchable;
  private MapVersion mapVersion;

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  @Column(name = "title")
  @NotNull
  public String getTitle() {
    return title;
  }

  @Column(name = "category")
  @NotNull
  public String getCategory() {
    return category;
  }

  @Column(name = "thumbnail_url")
  @Nullable
  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  @NotNull
  @Column(name = "ordinal")
  public int getOrdinal() {
    return ordinal;
  }

  @NotNull
  @Column(name = "launchable")
  public boolean getLaunchable() {
    return launchable;
  }

  @ManyToOne
  @Nullable
  @JoinColumn(name = "map_version_id")
  public MapVersion getMapVersion() {
    return mapVersion;
  }

}
