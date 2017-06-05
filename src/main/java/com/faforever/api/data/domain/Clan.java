package com.faforever.api.data.domain;

import com.faforever.api.clan.ClanRepository;
import com.faforever.api.data.checks.IsClanLeader;
import com.faforever.api.data.listeners.ClanEnricherListener;
import com.faforever.api.data.validation.IsLeaderInClan;
import com.faforever.api.error.ApiException;
import com.faforever.api.error.Error;
import com.faforever.api.error.ErrorCode;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.RequestScope;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

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
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "clan")
@Include(rootLevel = true, type = "clan")
@SharePermission(expression = IsClanLeader.EXPRESSION)
@DeletePermission(expression = IsClanLeader.EXPRESSION)
@CreatePermission(expression = "Prefab.Role.All")
@Setter
@IsLeaderInClan
@EntityListeners(ClanEnricherListener.class)
public class Clan extends AbstractEntity {

  private String name;
  private String tag;
  private Player founder;
  private Player leader;
  private String description;
  private String tagColor;
  private String websiteUrl;
  private List<ClanMembership> memberships;

  @Column(name = "name")
  @NotNull
  @UpdatePermission(expression = IsClanLeader.EXPRESSION)
  public String getName() {
    return name;
  }

  @Column(name = "tag")
  @Size(max = 3)
  @NotNull
  @UpdatePermission(expression = IsClanLeader.EXPRESSION)
  public String getTag() {
    return tag;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "founder_id")
  public Player getFounder() {
    return founder;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leader_id")
  @UpdatePermission(expression = IsClanLeader.EXPRESSION)
  public Player getLeader() {
    return leader;
  }

  @Column(name = "description")
  @UpdatePermission(expression = IsClanLeader.EXPRESSION)
  public String getDescription() {
    return description;
  }

  @Column(name = "tag_color")
  public String getTagColor() {
    return tagColor;
  }

  // Cascading is needed for Create & Delete
  @OneToMany(mappedBy = "clan", cascade = CascadeType.ALL, orphanRemoval = true)
  // Permission is managed by ClanMembership class
  @UpdatePermission(expression = "Prefab.Role.All")
  @NotEmpty(message = "At least the leader should be in the clan")
  public List<ClanMembership> getMemberships() {
    return this.memberships;
  }

  @Transient
  @ComputedAttribute
  public String getWebsiteUrl() {
    return websiteUrl;
  }

  @OnCreatePreSecurity
  public void onCreatePreSecurity(RequestScope requestScope) {
    // TODO: how to get creator out of request Scope
    Player creator = null;
    // TODO: how to get clanRepository
    ClanRepository clanRepository = null;
    if (!creator.getClanMemberships().isEmpty()) {
      throw new ApiException(new Error(ErrorCode.CLAN_CREATE_CREATOR_IS_IN_A_CLAN));
    }
    if (clanRepository.findOneByName(getName()).isPresent()) {
      throw new ApiException(new Error(ErrorCode.CLAN_NAME_EXISTS, getName()));
    }
    if (clanRepository.findOneByTag(getTag()).isPresent()) {
      throw new ApiException(new Error(ErrorCode.CLAN_TAG_EXISTS, getTag()));
    }

    setFounder(creator);
    setLeader(creator);

    ClanMembership membership = new ClanMembership();
    membership.setClan(this);
    membership.setPlayer(creator);

    // clan membership is saved over cascading, otherwise validation will fail
    setMemberships(Arrays.asList(membership));
  }
}
