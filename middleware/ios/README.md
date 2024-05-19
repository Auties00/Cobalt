# iOS middleware

### Requirements

1. iPhone with Jailbreak
2. [Frida server installed](https://frida.re/docs/ios/)
3. Whatsapp and/or Whatsapp Business installed from the App Store

### How to run

1. Run `npm install` in the ios directory
2. Disable sleep on your iPhone
3. Run:
    - `frida -U -l server_with_dependencies.js -f "net.whatsapp.WhatsApp"` (WhatsApp)
    - `frida -U -l server_with_dependencies.js -f "net.whatsapp.WhatsAppSMB"` (WhatsApp Business)
4. Make sure the iOS device and PC running the protocol are on the same network, copy the local ip of the iOS device and use it like this:
   `CompanionDevice.ios(business, ipAddress)`