package com.faforever.api.data.listeners;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.Tutorial;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TutorialEnricherListener {

  private static FafApiProperties fafApiProperties;
  private static Pattern thumbnailUrlPattern;

  @Inject
  public void init(FafApiProperties fafApiProperties) {
    TutorialEnricherListener.fafApiProperties = fafApiProperties;
    TutorialEnricherListener.thumbnailUrlPattern = Pattern.compile(fafApiProperties.getTutorial().getThumbnailUrlFormat().replace("%s", "(.*)"));
  }

  @PostLoad
  public void enrich(Tutorial tutorial) {
    if (tutorial.getThumbnailUrl() == null) {
      return;
    }
    Matcher matcher = thumbnailUrlPattern.matcher(tutorial.getThumbnailUrl());
    if (!matcher.matches()) {
      tutorial.setThumbnailUrl(String.format(fafApiProperties.getTutorial().getThumbnailUrlFormat(), tutorial.getThumbnailUrl()));
    }
  }

  @PreUpdate
  public void preUpdate(Tutorial tutorial) {
    if (tutorial.getThumbnailUrl() == null) {
      return;
    }
    Matcher matcher = thumbnailUrlPattern.matcher(tutorial.getThumbnailUrl());
    if (matcher.matches()) {
      tutorial.setThumbnailUrl(matcher.group(1));
    }
  }
}
