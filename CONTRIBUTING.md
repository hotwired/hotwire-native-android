# Contributing to Hotwire Native Android

Note that we have a [code of conduct](/CODE_OF_CONDUCT.md). Please follow it in your interactions with this project.

## Developing locally

Hotwire Native for Android is built using Kotlin and Android SDK 28+ as its minimum version. To set up your development environment:

1. Clone the repo
1. Open the directory in the latest version of Android Studio

To run the test suite:

1. Open the directory in Terminal
1. Run the `./gradlew testRelease` command

## Sending a Pull Request

The core team is monitoring for pull requests. We will review your pull request and either merge it, request changes to it, or close it with an explanation.

Before submitting a pull request, please:

1. Fork the repository and create your branch.
2. Follow the setup instructions in this file.
3. If you’re fixing a bug or adding code that should be tested, add tests!
4. Ensure the test suite passes.

## Feature parity with Android

New features will not be merged until also added to [Hotwire Native iOS](https://github.com/hotwired/hotwire-native-ios).

This does not apply to bugs that only appear on Android.