# Android middleware

### Requirements

1. Rooted android phone with Play Services
2. Magisk with Zygisk enabled and [PlayIntegrityFix module](https://github.com/chiteroman/PlayIntegrityFix) installed
3. [Frida server installed](https://frida.re/docs/android/)
4. Whatsapp and/or Whatsapp business installed on the phone **from the Play Store** (APKs don't work)

### How to run

1. Run `npm install` in the android directory
2. Open Whatsapp/Whatsapp Business and try to register a number (needed to load gpia components, won't work if you don't do it)
3. Run:
    - `frida -U "WhatsApp" -l server_with_dependencies.js` (WhatsApp)
    - `frida -U "WhatsApp Business" -l server_with_dependencies.js` (WhatsApp Business)