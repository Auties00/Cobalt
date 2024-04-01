import base64
from concurrent.futures import Future

from flask import Flask, request
import frida
from waitress import serve

global script

app = Flask(__name__)


def concat_base64(base64_str1: str | None, base64_str2: str | None) -> str:
    bytes1 = base64.urlsafe_b64decode(base64_str1)
    bytes2 = base64.urlsafe_b64decode(base64_str2)
    return base64.urlsafe_b64encode(bytes1 + bytes2).decode('utf-8')


@app.route('/gpia')
def gpia_route():
    auth_key = request.args.get('authKey')
    if auth_key is None:
        return "Missing authKey parameter", 400
    future = Future()
    script.post({"type": "gpia", "authKey": auth_key})
    script.on('message', lambda message, data: future.set_result({
        "token": message.get("payload", {}).get("token"),
        "error": message.get("description")
    }))
    return future.result(), 200


@app.route('/cert')
def cert_route():
    auth_key = request.args.get('authKey')
    if auth_key is None:
        return "Missing authKey parameter", 400
    enc = request.args.get('enc')
    if enc is None:
        return "Missing enc parameter", 400
    data = concat_base64(auth_key, enc)
    future = Future()
    script.post({"type": "cert", "data": data})
    script.on('message', lambda message, data: future.set_result({
        "signature": message.get("payload", {}).get("signature"),
        "certificate": message.get("payload", {}).get("certificate"),
        "error": message.get("description")
    }))
    return future.result(), 200


if __name__ == '__main__':
    device = frida.get_usb_device()
    session = device.attach("WhatsApp Business")

    with open('android.js', 'r', encoding='utf-8') as script_file:
        js_code = script_file.read()

    script = session.create_script(js_code)
    script.load()

    print('[*] Running server on port 1119')

    serve(app, host="0.0.0.0", port=1119)
