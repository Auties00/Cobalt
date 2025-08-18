// Print registration public key

Interceptor.attach(ObjC.classes.WAECAgreement["+ calculateAgreementFromPublicKey:privateKey:"].implementation, {
   onEnter(args) {
      console.log("[*] Registration Public key:", arrayBufferToHex(ObjC.Object(args[2])["- key"]().bytes().readByteArray(32)))
   }
})

function arrayBufferToHex(arrayBuffer) {
  const byteArray = new Uint8Array(arrayBuffer);
  let hexString = '';
  for (let i = 0; i < byteArray.length; i++) {
    const hex = byteArray[i].toString(16);
    hexString += (hex.length === 1 ? '0' : '') + hex;
  }
  return hexString;
}
