<<<<<<< HEAD
# StroopLocker aka str00py

**StroopLocker** aims to be a productivity tool to specifically counter your urges for doomscrolling your phone. The idea is to encourage you to challenge your consciousness before being granted to apps. 
It's Android app that briefly displays a double Stroop-style color challenge before allowing the user to access a chosen locked app. It presents a word in one color and requires the user to identify the ink color (not the text label). The user must correctly tap the matching color label from a grid of mismatched label-text pairs to pass.

### Key Features
=======
<<<<<<< HEAD
**StroopLocker** aka **str00py** aims to be a productivity tool to specifically counter your urges for doomscrolling your phone. The idea is to encourage you to forcefully prompt your consciousness for even a split second before being granted to apps. 
It's currently an Android app that briefly displays a double Stroop-style color challenge before allowing the user to access their specified locked apps. It presents a word in one color and requires the user to identify the ink color (not the text label); the user must then correctly tap the matching color label from a grid of mismatched label-text pairs to pass, hopefully doubling the required level of effort.
=======
# StroopLocker aka str00py
>>>>>>> 67c6344 (Update README.md)

---
>>>>>>> 21bc4e8 (fail)

1. **Double Stroop Challenge**  
   - The displayed word is a “decoy” (e.g., "Red"), but its *ink color* is the correct answer (e.g., "Cyan").  
   - The user must spot the correct color label among nine randomized buttons.

2. **App Locking**  
   - Select any installed app.  
   - The chosen app is locked behind this Stroop puzzle, requiring successful challenge completion.

<<<<<<< HEAD
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
=======

---

### 🚀 How it Works

1. **Launch StroopLocker**  
2. **Choose an app** to lock through the “Select App” button.  
3. **See a word (a label)** in an ink color that is different from the label meaning.  
4. **Tap** the correct label in the 3×3 grid that matches the ink color.  
5. If correct, the locked app launches; if not, you get a new puzzle!
>>>>>>> 21bc4e8 (fail)


---

<<<<<<< HEAD
Thanks for checking out **StroopLocker**!

“Enjoy every moment you have. Because in life, there are no rewinds, only flashbacks. Make sure it’s all worth it.” – Anonymous
=======
### Regrets, for now
Unfortunately,large parts of this repo remain authored by various LLMs. It is a clusterfuck and will probably always remain a clusterfuck. **I would be thrilled to see any and all changes the open-source community would suggest!**

I just wanted to get this 'over the line' and try to use some VCS, this is awful code and probably should not be run on your phone yet.

---

### 🐛 Known issues 

- Currently, it barely works, lol. 

---

### 🙏 To do 
- It needs to be a lot 'stickier' and more comprehensively function like an application locker; it's too easy to bypass with stock Android features such as tiled window gestures and application dismissal. 
- Basic accessibility (alternative palette for colourblind users, at minimum compatible palette with the three common types of colourblindess). Perhaps alternative stroop tests (e.g. shape and word challenges) for those without colour vision. Right now, I think extremely vision-impaired users will forever be unable to utilise the application.
- Settings menu to customize aspects of the app (button size, font, colour palette)
- Thorough testing on an array of devices (pretty much only Pixel 9 API 35 has been tested sorry I'm really bad at this)
- iOS port, I'm developing for Android because I have an Android phone. I don't know how easily similar functionality can be implemented with iOS security features.

---

### 🇺🇸 🦅 License
Licensed under GPL 2.0, for your freedoms.
[This project is provided as-is, without warranty. Modify and distribute at your discretion.](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

---

Thanks for checking out the repo!

---

**Any and all PRs, constructive criticism, or other contributions greatly encouraged and welcomed!**
>>>>>>> 21bc4e8 (fail)
