# Smeargle's Mystery Pixel Art

Cobblemon event specification for **Fabric 1.21.1** and **Cobblemon 1.7.0** using **MiniMessage** for all player-facing text.

## 1. Event Overview

Smeargle's Mystery Pixel Art is a competitive guessing event where an AI-controlled Smeargle progressively builds a pixel-art Pokémon from a predefined schematic.

Players watch the image slowly appear and try to identify the Pokémon as quickly as possible. The first player to correctly guess the Pokémon wins the round.

### Core gameplay loop

**Smeargle starts building → blocks are revealed progressively → players recognise the Pokémon → players submit `/guess <pokemon>`**

## 2. Platform Target

- **Minecraft:** Fabric 1.21.1
- **Cobblemon:** 1.7.0
- **Text formatting:** MiniMessage

## 3. Round Structure

Each round uses one predefined Pokémon pixel-art schematic.

1. A round starts and announces that Smeargle is beginning a new drawing.
2. The event selects a hidden target Pokémon tied to the schematic.
3. Smeargle places blocks gradually rather than revealing the image instantly.
4. Players attempt to identify the Pokémon while the build is still incomplete.
5. Players submit guesses with `/guess <pokemon>`.
6. The first correct guess immediately ends the round and declares the winner.
7. If nobody guesses correctly before the build finishes, the round ends with the answer revealed.

## 4. Gameplay Requirements

### 4.1 Smeargle builder behaviour

- Smeargle should feel like the active artist for the event.
- The build must be revealed progressively, one step at a time, from the selected schematic.
- The reveal pace should be slow enough for deduction and fast enough to keep rounds engaging.
- Only blocks defined by the schematic should be placed during a round.

### 4.2 Guessing rules

- Guesses are submitted with `/guess <pokemon>`.
- Guess matching should accept the intended Pokémon name in a player-friendly way.
- The first correct guess wins the round.
- Incorrect guesses must not end the round.
- Only one Pokémon answer is valid for each round.

### 4.3 Win condition

- The winner is the first player whose guess matches the hidden Pokémon for the active round.
- When a player wins:
  - the round stops immediately,
  - the correct Pokémon is revealed,
  - the winner is broadcast to all players.

## 5. Messaging

All player-facing messages should use **MiniMessage** formatting.

Example message intents:

- Round start announcement
- Hints or progress updates
- Incorrect guess feedback
- Winner announcement
- Round end / reveal message

## 6. Content Requirements

- Pixel art must come from predefined schematics.
- Each schematic must map cleanly to a single Pokémon answer.
- The event should be able to run multiple rounds by selecting different valid schematics.

## 7. Functional Summary

The finished project should support:

- running a Smeargle-driven pixel-art guessing round,
- progressively placing schematic blocks,
- accepting player guesses through `/guess <pokemon>`,
- determining the first correct guess,
- announcing the result with MiniMessage text.

## 8. Current Implementation

This repository now contains an initial server-side Fabric sidemod implementation that targets Cobblemon 1.7.0 on Minecraft 1.21.1.

### Admin commands

- `/smearglespixelart start random <pos>` starts a round with a random built-in template.
- `/smearglespixelart start template <template> <pos>` starts a round with a specific built-in template.
- `/smearglespixelart list` shows available built-in templates.
- `/smearglespixelart status` shows current round progress.
- `/smearglespixelart stop` stops the active round.

### Player command

- `/guess <pokemon>` submits a guess for the active round.

### Built-in templates

- `pikachu`
- `voltorb`

The block position supplied to the start command is the bottom-left origin of the vertical pixel-art canvas.
