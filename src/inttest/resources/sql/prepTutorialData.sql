DELETE FROM tutorial;

SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO tutorial (id, description, title, category, image, ordinal, launchable) VALUES
  (1, 'description', 'title', 'category', 'image.png', 1, 1);
