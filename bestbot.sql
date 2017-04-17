SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

CREATE DATABASE IF NOT EXISTS `bestbot` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `bestbot`;

CREATE TABLE `banlist` (
  `ip` varchar(18) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `expire` bigint(20) NOT NULL DEFAULT '0',
  `date` bigint(20) NOT NULL DEFAULT '0',
  `banner` varchar(45) NOT NULL,
  `lastdate` longtext,
  `lastbanner` longtext
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `blacklist` (
  `name` varchar(45) NOT NULL,
  `md5` varchar(32) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `date` bigint(20) NOT NULL,
  `banner` varchar(45) NOT NULL,
  `lastdate` longtext,
  `lastbanner` longtext
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `cfg` (
  `cfgname` varchar(45) CHARACTER SET latin1 NOT NULL,
  `username` varchar(45) CHARACTER SET latin1 NOT NULL,
  `date` datetime NOT NULL,
  `ip` varchar(128) CHARACTER SET latin1 NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `checked_ips` (
  `ip` varchar(15) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `hostmasks` (
  `hostmask` varchar(100) NOT NULL,
  `username` varchar(45) NOT NULL,
  `date` bigint(20) NOT NULL,
  `adder` varchar(45) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `login` (
  `id` int(11) NOT NULL,
  `username` varchar(45) CHARACTER SET latin1 NOT NULL,
  `password` varchar(80) CHARACTER SET latin1 NOT NULL,
  `level` int(11) NOT NULL,
  `activated` tinyint(4) NOT NULL,
  `server_limit` int(11) DEFAULT '1',
  `remember_token` varchar(255) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `save` (
  `id` int(11) NOT NULL,
  `serverstring` text CHARACTER SET latin1 NOT NULL,
  `slot` smallint(6) NOT NULL,
  `username` varchar(45) CHARACTER SET latin1 NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `serverlog` (
  `id` int(11) NOT NULL,
  `unique_id` varchar(60) CHARACTER SET latin1 NOT NULL DEFAULT '',
  `servername` varchar(255) CHARACTER SET latin1 NOT NULL,
  `username` varchar(45) CHARACTER SET latin1 NOT NULL,
  `date` datetime NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `server_recovery` (
  `nid` int(11) NOT NULL,
  `uid` varchar(256) NOT NULL,
  `hostcmd` text NOT NULL,
  `port` int(11) NOT NULL,
  `owner` text NOT NULL,
  `owner_nick` text NOT NULL,
  `owner_hostname` text NOT NULL,
  `node` varchar(45) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `sitelog` (
  `id` bigint(20) NOT NULL,
  `type` tinytext NOT NULL,
  `action` varchar(24) NOT NULL,
  `details` text NOT NULL,
  `user` varchar(45) NOT NULL,
  `date` bigint(20) NOT NULL,
  `ip` varchar(64) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `wads` (
  `wadname` varchar(45) CHARACTER SET latin1 NOT NULL,
  `size` int(11) NOT NULL,
  `md5` varchar(32) NOT NULL,
  `date` datetime NOT NULL,
  `username` varchar(45) CHARACTER SET latin1 NOT NULL,
  `ip` varchar(128) CHARACTER SET latin1 NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `wad_downloads` (
  `name` varchar(45) NOT NULL DEFAULT '',
  `date` datetime NOT NULL,
  `ip` varchar(128) NOT NULL DEFAULT ''
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `wad_pages` (
  `id` int(11) UNSIGNED NOT NULL,
  `key` varchar(32) DEFAULT NULL,
  `wad_string` varchar(500) NOT NULL DEFAULT ''
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `whiteboard` (
  `id` int(11) NOT NULL,
  `content` blob NOT NULL,
  `date` bigint(20) NOT NULL,
  `author` varchar(45) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `whitelist` (
  `ip` varchar(15) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `adder` varchar(45) NOT NULL,
  `date` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=MyISAM DEFAULT CHARSET=latin1;


ALTER TABLE `banlist`
  ADD PRIMARY KEY (`ip`);

ALTER TABLE `checked_ips`
  ADD UNIQUE KEY `ip` (`ip`);

ALTER TABLE `hostmasks`
  ADD UNIQUE KEY `hostmask` (`hostmask`);

ALTER TABLE `login`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

ALTER TABLE `save`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `serverlog`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `server_recovery`
  ADD UNIQUE KEY `nid` (`nid`),
  ADD UNIQUE KEY `uid` (`uid`);

ALTER TABLE `sitelog`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `wad_pages`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `whiteboard`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `whitelist`
  ADD PRIMARY KEY (`ip`),
  ADD KEY `ip` (`ip`);


ALTER TABLE `login`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2016;
ALTER TABLE `save`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1176;
ALTER TABLE `serverlog`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=56786;
ALTER TABLE `server_recovery`
  MODIFY `nid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1475;
ALTER TABLE `sitelog`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8918;
ALTER TABLE `wad_pages`
  MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17146;
ALTER TABLE `whiteboard`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=65;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
