# Manga viewer

Supports two cmd args:
 - `-l` `nimbus` to use dark theme instead of system
 - `-f` `/path/to/folder` to open folder from terminal

## Feature list TODO:

Features that must be supported:
- [ ] file sorting !!!
- [ ] image zoom 
- [ ] image fit by width
- [x] image fit by height
- [x] image fill in case size is too small
- [ ] two image view side-by-side with possibility to define left-to-right or right-to-left view
- [ ] zip archives support

features that would be nice to have:
- [ ] image rotation
- [x] Nested directories support
  - [ ] zip archive contains 10 directories with volumes, 
  - [x] each volume contains directories with chapters,
  - [x] images are in chapter directories. 
  - [x] Viewer should understand this structure and read files in natural order from vol1 chapter1 to vol10 chapter 10)

