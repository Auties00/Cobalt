# Android middleware

### Requirements

1. Rooted android phone
2. Magisk with Zygisk enabled and [PlayIntegrityFix module](https://github.com/chiteroman/PlayIntegrityFix) installed
3. [Frida server installed](https://frida.re/docs/android/)
4. Both Whatsapp and Whatsapp business installed on the phone **from the Play Store**

### How to run

1. Run npm install in the android directory 
2. Open Whatsapp and try to register a number, do the same with Whatsapp business (needed to load gpia components)
3. Run:
    - `frida -U "WhatsApp" -l _frida.js`
    - `frida -U "WhatsApp" -l _frida.js` 