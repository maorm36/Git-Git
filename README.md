# Git-Git

**Git-Git** is an interactive Android game that helps users learn Git through hands-on practice, terminal-style challenges, visual branch diagrams, guided objectives, and an in-app Git assistant.

The app is designed for students, beginner developers, and anyone who wants to understand Git by doing, not only by reading theory.

---

## Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/285241d4-848b-4d3e-a81b-e4c1e53b56a1" width="240" alt="Git-Git world map screen" />
  <img src="https://github.com/user-attachments/assets/e273387f-573b-48e6-b22c-eef43a7e89de" width="240" alt="Git-Git gameplay screen" />
  <img src="https://github.com/user-attachments/assets/c66e6094-c3b2-48f1-b6f4-62802af60d59" width="240" alt="Git-Git assistant screen" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/c6ef1b69-7f49-460b-8e96-cc5778bb1d57" width="240" alt="Git-Git branch visualizer screen" />
</p>

---

## Overview

Git is one of the most important tools in software development, but many beginners struggle to understand how commits, branches, staging, merging, checkout, reset, and history actually work.

Git-Git turns Git learning into a structured game experience.

Instead of reading long explanations, the player progresses through levels, types Git-like commands, sees the repository state change visually, and receives contextual guidance from the in-app assistant.

---

## Main Features

### Interactive Git Levels

Players complete Git-based objectives through progressive levels.

Example objectives:

- Initialize a Git repository
- Stage files before committing
- Create commits with specific messages
- Inspect repository status
- View commit history
- Switch branches
- Understand branch structure
- Work with Git history and log-based tasks

Each level focuses on a specific Git concept and asks the player to solve it by entering commands.

---

### Terminal-Style Gameplay

The app includes a terminal-inspired command input where users type Git commands directly.

Examples:

```bash
git init
git add README.md
git commit -m "Initial commit"
git status
git log
git checkout main
```

The terminal output gives immediate feedback, including success messages, errors, hints, and level completion messages.

---

### Visual Branch and Commit Graph

Git-Git includes a visual Git graph that shows commits and branches in a beginner-friendly way.

This helps players understand:

- where commits are located
- which branch is currently active
- how branch history grows
- how Git history changes after commands
- what HEAD/current branch means in practice

The goal is to make Git less abstract and more visual.

---

### World-Based Progression

The game is organized into worlds and levels.

Each world introduces a group of Git concepts:

- **World 1: First Steps**
  - `git init`
  - `git add`
  - `git commit`
  - `git status`
  - `git log`

Future worlds can expand into:

- branching
- merging
- collaboration
- rewriting history
- recovery commands
- advanced Git workflows

---

### Star Scoring System

Players earn stars based on their performance.

The scoring considers factors such as:

- number of commands used
- whether hints were used
- whether the solution was close to the optimal path

This gives the game replay value and encourages cleaner Git usage.

---

### Hints and Guided Learning

Each level can provide hints to help the player understand what to do next.

Hints are designed to guide the player without immediately giving away the full solution.

Example:

```text
Hint: Start by creating a repository with 'git init'
Hint: Use 'git add' to stage your file before committing
```

---

### In-App Git Assistant

Git-Git includes an AI-style Git assistant screen that helps the player understand the current level.

The assistant can help with:

- explaining the current objective
- checking the player's current status
- explaining Git commands
- helping when the player is stuck
- giving short, practical guidance
- keeping answers focused on the current Git level

The assistant uses the current game context, such as:

- level title
- level objective
- current branch
- commit count
- available commands
- staged files
- working files
- command history
- goal completion status

---

### AI Response Reporting

The app includes a report flow for problematic assistant responses.

Users can report responses for reasons such as:

- incorrect answer
- offensive or inappropriate content
- unsafe content
- off-topic response
- other

Reports are sent to Firebase Firestore and are used to improve response quality and safety.

---

## Tech Stack

### Android

- Kotlin
- Jetpack Compose
- Material 3
- MVVM architecture
- Hilt dependency injection
- Kotlin Coroutines
- StateFlow
- Android Navigation
- Gradle Kotlin DSL

### Backend / Cloud

- Firebase Firestore
- Firebase configuration through `google-services.json`
- Firestore security rules for AI response reports

### AI / Local Model Integration

The project includes integration work for optional on-device AI support.

The app is designed so that:

- core gameplay works without the AI model
- smart deterministic responses can still guide the user
- AI mode can be enabled when the model is available
- the app handles model loading and failure states gracefully

---

## Project Structure

```text
app/
├── src/main/
│   ├── java/com/app/gitquest/
│   │   ├── data/
│   │   │   └── levels/
│   │   ├── engine/
│   │   ├── ui/
│   │   │   ├── aichat/
│   │   │   ├── game/
│   │   │   ├── onboarding/
│   │   │   ├── theme/
│   │   │   ├── visualizer/
│   │   │   └── worldmap/
│   │   ├── GitQuestApp.kt
│   │   └── MainActivity.kt
│   ├── res/
│   └── AndroidManifest.xml
```

Main areas:

### `engine/`

Contains the core Git simulation logic.

Responsible for:

- parsing commands
- executing Git operations
- updating repository state
- returning command results
- validating state transitions

### `data/levels/`

Contains level definitions.

Each level can define:

- title
- story/briefing
- objective
- initial Git state
- available commands
- hints
- goal conditions
- optimal command count

### `ui/game/`

Contains the main gameplay screen.

Responsible for:

- terminal UI
- command input
- suggestions
- hints
- level completion
- branch visualizer integration
- opening the AI assistant

### `ui/aichat/`

Contains the Git assistant screen.

Responsible for:

- chat messages
- assistant responses
- quick actions
- AI model status
- response reporting

### `ui/visualizer/`

Contains Git branch and commit visualization components.

### `ui/worldmap/`

Contains world and level selection UI.

---

## Example Gameplay Flow

A player starts the first level:

```text
Objective: Initialize a Git repository and make your first commit.
```

The player enters:

```bash
git init
git add README.md
git commit -m "Initial commit"
```

The app updates the simulated Git state, checks the objective, and completes the level when the required conditions are met.

---

## Installation

### Requirements

- Android Studio
- Kotlin
- Android Gradle Plugin
- Firebase project configuration
- Android device or emulator

### Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY_NAME.git
cd YOUR_REPOSITORY_NAME
```

### Open in Android Studio

Open the project folder in Android Studio.

Wait for Gradle sync to complete.

### Firebase Setup

The app uses Firebase for AI response reports.

Add your Firebase configuration file:

```text
app/google-services.json
```

Make sure your Firebase project has Firestore enabled.

---

## Firestore Security Rules

The app uses Firestore to submit AI response reports.

Recommended Firestore rules should:

- allow report creation only
- block public reads
- block updates
- block deletes
- validate allowed fields
- validate field types
- limit text sizes

Example collection:

```text
ai_response_reports/{reportId}
```

Example report fields:

```text
messageText
reason
reasonLabel
userFeedback
levelTitle
levelObjective
currentBranch
commitCount
isLevelCompleted
appVersionName
appVersionCode
createdAt
```

---

## Build and Run

### Debug Build

Run the app directly from Android Studio:

```text
Run → app
```

### Release Build

To create a release App Bundle:

```text
Build → Generate Signed Bundle / APK → Android App Bundle
```

Before uploading to Google Play, update:

```kotlin
versionCode = 1
versionName = "1.0"
```

For every new upload, increase `versionCode`.

Example:

```kotlin
versionCode = 2
versionName = "1.0.1"
```

---

## Google Play Testing

The app can be tested through Google Play Console using:

- Internal testing
- Closed testing
- Production release, when eligible

For new personal developer accounts, Google Play may require closed testing with at least 12 testers for 14 days before production access is available.

---

## Privacy

Git-Git is designed to keep gameplay mostly local.

The app may store gameplay progress locally on the device.

If the user reports an AI response, the app may send the reported message, report reason, optional feedback, level context, and app version information to Firebase Firestore.

The app does not require user account creation.

A public privacy policy URL should be provided in Google Play Console before publishing.

---

## Security Notes

Before publishing, verify that the project does not include:

- private API keys
- service account JSON files
- keystore files
- keystore passwords
- SMTP credentials
- backend admin tokens
- private local paths in user-facing messages
- debug-only UI
- stack traces shown to users

Files such as `local.properties` should not be committed to Git.

The Android `google-services.json` file is normally expected in Firebase Android apps, but Firestore rules and backend configuration must be properly secured.

---

## Current Status

Git-Git currently includes:

- interactive Git gameplay
- level system
- terminal-style command input
- command suggestions
- hints
- branch visualization
- AI assistant screen
- AI response reporting
- Firebase integration
- Google Play testing preparation

---

## Planned Improvements

- more worlds and levels
- more Git commands
- improved AI assistant behavior
- better onboarding
- achievements
- more advanced branch and merge scenarios
- improved animations and polish

---

## Developer Notes

This app demonstrates:

- Android development with Kotlin and Jetpack Compose
- MVVM architecture
- state-driven UI
- custom game logic
- command parsing
- simulated Git engine
- visual graph representation
- Firebase integration
- Google Play release preparation
- AI assistant integration in a mobile learning app

---

## License

Add your license here.

Example:

```text
MIT License
```

or:

```text
All rights reserved.
```

---

## Author

Developed by **Maor Mordo**.

GitHub: [Add your GitHub profile link]  
LinkedIn: [Add your LinkedIn profile link]  
Google Play: [Add Play Store link when available]
