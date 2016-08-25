CREATE TABLE IF NOT EXISTS `processed_tx_updates` (
  `hash` varchar(255) NOT NULL,
  PRIMARY KEY (`hash`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `public_keys` (
  `key_value` varchar(255) NOT NULL,
  PRIMARY KEY (`key_value`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `recipients` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` double NOT NULL,
  `public_key` varchar(255) DEFAULT NULL,
  `transaction_hash` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_cgisw91npm2cnv4ncy8nml6j9` (`transaction_hash`),
  CONSTRAINT `FK_cgisw91npm2cnv4ncy8nml6j9` FOREIGN KEY (`transaction_hash`) REFERENCES `transactions` (`hash`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `senders` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` double NOT NULL,
  `public_key` varchar(255) DEFAULT NULL,
  `transaction_hash` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_mphhcivqlqfhdcdpa67vx35j2` (`transaction_hash`),
  CONSTRAINT `FK_mphhcivqlqfhdcdpa67vx35j2` FOREIGN KEY (`transaction_hash`) REFERENCES `transactions` (`hash`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `transactions` (
  `hash` varchar(255) NOT NULL,
  `date` bigint(20) NOT NULL,
  `processed` bit(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`hash`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;