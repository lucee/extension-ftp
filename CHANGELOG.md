# Changelog

## 1.0.0.6-RC

- Fix FTPS test to use `secure="FTPS"` instead of SFTP
- Fix TLD metadata (was incorrectly copied from Mail extension)
- Align Maven license metadata with LGPL 2.1
- Rename `FTPResoucreException` to `FTPResourceException`

## 1.0.0.5-RC

- Release candidate
- Add lite extension artifact (without bundled Maven dependencies)

## 1.0.0.4

- [LDEV-6093](https://luceeserver.atlassian.net/browse/LDEV-6093) — auto-bundle parent POMs to make extension fully self-contained

## 1.0.0.3

- Add Maven GAV metadata

## 1.0.0.2

- Add logo to Maven publishing
- [LDEV-6216](https://luceeserver.atlassian.net/browse/LDEV-6216) — disable SFTP/FTPS virtual filesystem test cases (not yet supported)

## 1.0.0.1

- Beta release
- Add test cases for virtual file system

## 1.0.0.0

- Initial version — FTP/SFTP functionality extracted from Lucee core (since Lucee 7.1)
- Uses Apache Commons Net and mwiede/jsch libraries
