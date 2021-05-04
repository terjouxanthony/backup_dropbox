# Script to publish backup files to Dropbox with rotation

- Gradle required version: 7.0
- Java version: 16

Imagine you store timestamped backups of something (folder, website, ...) and you have a folder structure like this:
```
~/website/
├── backups/
│   ├── wordpress-backup-2021-05-02_04:24:01.tar.gz
│   ├── wordpress-backup-2021-04-25_20:06:02.tar.gz
│   ├── database-backup-2021-05-02_04:24:21.sql
│   └── database-backup-2021-04-25_20:06:20.sql
```

This application is aimed at pushing your latest backups to Dropbox, and removing the oldest ones in Dropbox.
This history kept in Dropbox is configurable, as well as the backups prefixes and their date format.

<ins>Example of a config file</ins>:
```yaml
backup_dir: ~/website/backups/
file_prefix_to_backup:
  - wordpress-backup-
  - database-backup-
date_format: "yyyy-MM-dd_HH_mm_ss"
dropbox_client_identifier: "Name of your Dropbox app"
dropbox_token: "Your generated Dropbox Token"
number_of_days_to_keep: 3
```

### Usage:
Install locally the application:
```
gradle installDist
```

Move it somewhere, ex:
```
mv build/install/backup_dropbox/ ~/java-projects/
```

Launch it:

<b>Linux/Mac</b>:
```
~/java-projects/backup_dropbox/bin/backup_dropbox config.yaml
```

<b>Windows</b>:
```
~/java-projects/backup_dropbox/bin/backup_dropbox.bat config.yaml
```

### Initial Goal
A cron job periodically creates a backup of my website and I wanted to publish it to Dropbox, keeping at most the n latest backups.

### Get a Dropbox token
https://www.dropbox.com/developers/apps

-> Create an App

-> Scoped access

-> App folder

-> Name your app
