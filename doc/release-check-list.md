# Create archives

- Check source tree is clean

    git checkout master
    git pull
    git status

- Bump version number

    vi version.properties

- Update changelog
    vi CHANGELOG.md
    vi fastlane/metadata/android/en-US/changelogs/$version.txt

- Build archives

    Check signing key is in android/signing.gradle

    make clean-dist

- Test on computer
- Test on device

- Update screenshots in fastlane/metadata/android/en-US/images/

- git commit

- Upload apk on Google Play

    Check api file is in fastlane/google-play-api.json

    make fastlane-beta

- Tag and push

    make tagpush

# Upload archives

- Upload archives

# Game page

- Update game page
    - Screenshots
    - Archive links
- Write blog post
- Publish

# Google Play

- Update Google Play page
- Publish

# F-Droid
- Get the F-Droid version updated

# Spread

- Post on:
    - Mastodon
    - Twitter
    - FB
