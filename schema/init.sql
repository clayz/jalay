CREATE TABLE `user` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `mail` VARCHAR(255) NOT NULL,
  `password` varchar(45) NOT NULL,
  `status` tinyint(2) NOT NULL,
  `create_date` datetime NOT NULL,
  `update_date` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `del` tinyint(1) NOT NULL DEFAULT '0',
  `note` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_UNIQUE` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

  
CREATE TABLE `jalay`.`user_profile` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `nickname` VARCHAR(50) NOT NULL,
  `birthday` DATE NOT NULL,
  `gender` TINYINT(2) NOT NULL,
  `create_date` DATETIME NOT NULL,
  `update_date` DATETIME NOT NULL DEFAULT '0000-00-00 00:00:00',
  `del` TINYINT(1) NOT NULL DEFAULT 0,
  `note` VARCHAR(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uuid_UNIQUE` (`user_id` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;