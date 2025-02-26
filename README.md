# StroopLocker

**StroopLocker** aims to be a productivity tool to specifically counter your urges for doomscrolling your phone. The idea is to encourage you to challenge your consciousness before being granted to apps. 
It's Android app that briefly displays a double Stroop-style color challenge before allowing the user to access a chosen locked app. It presents a word in one color and requires the user to identify the ink color (not the text label). The user must correctly tap the matching color label from a grid of mismatched label-text pairs to pass.

### Key Features

1. **Double Stroop Challenge**  
   - The displayed word is a “decoy” (e.g., "Red"), but its *ink color* is the correct answer (e.g., "Cyan").  
   - The user must spot the correct color label among nine randomized buttons.

2. **App Locking**  
   - Select any installed app.  
   - The chosen app is locked behind this Stroop puzzle, requiring successful challenge completion.

### How it Works

1. **Launch StroopLocker**  
2. **Choose an app** to lock through the “Select App” button.  
3. **See a word** in an ink color that is different from the word meaning.  
4. **Tap** the correct label in the 3×3 grid that matches the ink color.  
5. If correct, the locked app launches; if not, you get a new puzzle.

### Getting Started

- Clone the repository.  
- Open in Android Studio, build, and run on a device or emulator.  
- Tap “Enable Service” to grant Accessibility permissions if desired.  
- Tap “Select App” to lock an app.  
- Solve the color challenge to unlock the chosen app.

### Regrets, for now
Unfortunately,large parts of this repo remain authored by LLMs. I would be thrilled to see any and all changes the open-source community would suggest.

I just wanted to get this over the line in a vaguely functioning state, this is awful code and probably should not be run on your phone yet.


### License
Licensed under GPL 2.0.
[This project is provided as-is, without warranty. Modify and distribute at your discretion.](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

### Wishlist/to-do
- Accessibility (alternative stroop tests for colourblind users, at minimum compatible palette with the three common types of colourblindess).
- Settings menu to customize aspects of the app (button size, font, colour palette)
- Thorough testing on an array of devices (pretty much only Pixel 9 API 35 has been tested)
- iOS port


---

Thanks for checking out **StroopLocker**!

“Enjoy every moment you have. Because in life, there are no rewinds, only flashbacks. Make sure it’s all worth it.” – Anonymous
