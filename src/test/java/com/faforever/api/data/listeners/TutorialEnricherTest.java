package com.faforever.api.data.listeners;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.Tutorial;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TutorialEnricherTest {
  private TutorialEnricherListener instance;


  @Before
  public void setUp() {

    FafApiProperties fafApiProperties = new FafApiProperties();
    fafApiProperties.getTutorial().setThumbnailUrlFormat("http://example.com/%s");

    instance = new TutorialEnricherListener();
    instance.init(fafApiProperties);
  }

  @Test
  public void enrich() {
    Tutorial tutorial = new Tutorial();
    tutorial.setThumbnailUrl("abc.png");

    instance.enrich(tutorial);

    assertThat(tutorial.getThumbnailUrl(), is("http://example.com/abc.png"));
  }

  @Test
  public void preUpdate() {
    Tutorial tutorial = new Tutorial();
    tutorial.setThumbnailUrl("http://example.com/abc.png");

    instance.preUpdate(tutorial);

    assertThat(tutorial.getThumbnailUrl(), is("abc.png"));
  }
}
