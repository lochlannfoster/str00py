# str00py Behavior Design Spec

> **For Claude:** Use this document as the source of truth for intended app behavior.

**Date:** 2026-02-05
**Status:** Draft

---

## Overview

str00py locks selected apps behind Stroop color-word challenges. User must identify the INK COLOR (not the word) to unlock an app.

---

## States

```
┌─────────────┐
│    IDLE     │  No active session
└──────┬──────┘
       │ User opens locked app
       ▼
┌─────────────┐
│  CHALLENGE  │  Wrong answer → new challenge + feedback, stay here
│             │  Back button → IDLE (returns to home screen)
└──────┬──────┘
       │ Correct answer (or X correct in a row if configured)
       ▼
┌─────────────────────────────────────────────────┐
│   SESSION_ACTIVE (single app only)              │
│                                                 │
│   Ends immediately on ANY of:                   │
│   • Home button                                 │
│   • Open different app (locked or not)          │
│   • Lock screen                                 │
│   • App crash                                   │
│   • Phone restart                               │
│   • (Optional) configured time limit            │
└──────┬──────────────────────────────────────────┘
       │ Any of the above
       ▼
┌─────────────┐
│    IDLE     │  Must challenge again to re-enter any locked app
└─────────────┘
```

**Key behavior:**
- Only one app can be unlocked at a time
- Any context switch ends the current session
- Sessions are per-unlock, not per-app (leaving and returning requires new challenge)

---

## Screens

### Main Screen (opening str00py directly)

| Element | Description |
|---------|-------------|
| App selection button | Opens app selection screen |
| Gear icon (top right) | Opens settings menu |

### Challenge Screen (opening a locked app)

| Element | Description |
|---------|-------------|
| Challenge word | Color name displayed in different ink color |
| 3x3 button grid | 9 square buttons to select answer |
| Progress indicator | Shows "2 of 3" when multiple challenges required |
| Gear icon (top right) | Opens settings menu |
| Back button | Cancels challenge, returns to home screen |

**Note:** Challenge screen does NOT have app selection button.

---

## Challenge UI

### Colors (Standard Mode)

9 colors with adaptive text outline for readability:

| Color | RGB | Outline |
|-------|-----|---------|
| Red | (255, 0, 0) | White |
| Green | (0, 255, 0) | Black |
| Blue | (0, 0, 255) | White |
| Yellow | (255, 255, 0) | Black |
| Purple | (128, 0, 128) | White |
| Orange | (255, 165, 0) | Black |
| Pink | (255, 192, 203) | Black |
| Brown | (165, 42, 42) | White |
| Cyan | (0, 255, 255) | Black |

### Shapes (Colorblind Mode)

9 shapes replace colors when colorblind mode enabled:

Circle, Square, Triangle, Star, Heart, Diamond, Cross, Arrow, Moon

Challenge becomes: identify the SHAPE (not the word).

### Button Design

- Shape: Square
- Layout: Equidistant 3x3 grid
- Background: Light gray (#E0E0E0) in light theme, dark gray in dark theme
- Text: Colored with adaptive outline (black for light colors, white for dark)

### Challenge Word

- Large, centered above button grid
- Adaptive outline (same treatment as buttons)
- Font: Bold/extrabold for readability

### Wrong Answer Feedback

| Feedback | When |
|----------|------|
| Screen shake | Always (visual feedback) |
| Sound | Only if sound setting enabled |
| Vibration | Only if vibration setting enabled |

Screen shake: Quick horizontal shake (~300ms) on challenge view.

### Multiple Challenges

When challenges required > 1:
- Each challenge uses a different color/shape
- Progress indicator shows current progress (e.g., "2 of 3")

---

## Settings

Accessed via gear icon (top right) on both main screen and challenge screen.

| Setting | Default | Options |
|---------|---------|---------|
| Challenge timeout | Off (infinite) | Seconds until challenge auto-closes |
| Session duration | Until leave | Seconds until session expires |
| Sound | Off | On / Off |
| Vibration | Off | On / Off |
| Challenges required | 1 | Number to solve in a row |
| Colorblind mode | Off | Off / Shapes |
| Theme | System default | System / Light / Dark |
| Master disable | Off | On / Off (protected by Stroop challenge) |

### Master Disable

Temporarily disables all app locking. Protected by a Stroop challenge to prevent easy bypass.

---

## App Selection

Accessed via button on main screen.

### Display

| Aspect | Behavior |
|--------|----------|
| Apps shown | User-installed apps (option to show all) |
| Organization | Alphabetical, locked apps at top |
| Search | Yes, searchable |
| Interaction | Tap to toggle lock status |
| Visual | Locked apps show "(locked)" in green |
| Confirmation | None needed |

### Restrictions

Cannot lock:
- str00py itself
- System settings
- Phone dialer

---

## Permissions

### Accessibility Service

Required for app to function.

| Scenario | Behavior |
|----------|----------|
| First launch | Prompt to enable accessibility service |
| Permission denied | Keep prompting on each app open |
| Permission disabled later | Prompt returns on next app open |

Prompt appears on both main screen and challenge screen until enabled.

### Overlay Permission

Required to show challenge over other apps. Requested during onboarding flow.

---

## Notifications

Persistent notification when str00py is active (accessibility service running).

---

## Future Work

### Error States (TODO)

Need to define behavior for:
- Accessibility service disabled mid-session
- App force-stopped by system
- Database corruption
- Other unexpected states

---

## Summary

| Component | Key Decision |
|-----------|--------------|
| Session model | One app at a time, any leave ends session |
| Challenge | 9 colors/shapes, 3x3 grid, adaptive outlines |
| Settings | 8 configurable options via gear menu |
| App selection | User apps, alphabetical, tap to toggle |
| Accessibility | Keep prompting until enabled |
| Notification | Persistent when active |
