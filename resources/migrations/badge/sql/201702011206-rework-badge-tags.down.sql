ALTER TABLE `badge_content_tag` DROP PRIMARY KEY;

--;;

ALTER TABLE `badge_content_tag` ADD COLUMN `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
