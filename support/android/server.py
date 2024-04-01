import base64
from concurrent.futures import Future
from threading import Lock
from typing import Any

from flask import Flask, request
import frida
from waitress import serve

global script

app = Flask(__name__)

gpia_requests: dict[str, Future] = {}
gpia_requests_lock: Lock = Lock()
cert_requests: dict[str, Future] = {}
cert_requests_lock: Lock = Lock()


def concat_base64(base64_str1: str | None, base64_str2: str | None) -> str:
    bytes1 = base64.urlsafe_b64decode(base64_str1)
    bytes2 = base64.urlsafe_b64decode(base64_str2)
    return base64.urlsafe_b64encode(bytes1 + bytes2).decode('utf-8')


@app.route('/gpia')
def gpia_route() -> (str, int):
    auth_key: str | None = request.args.get('authKey')
    if auth_key is None:
        return "Missing authKey parameter", 400
    future = Future()
    script.post({"type": "gpia", "authKey": auth_key})
    gpia_requests_lock.acquire()
    gpia_requests[auth_key] = future
    gpia_requests_lock.release()
    return future.result(), 200


@app.route('/cert')
def cert_route() -> (str, int):
    auth_key: str | None = request.args.get('authKey')
    if auth_key is None:
        return "Missing authKey parameter", 400
    enc: str | None = request.args.get('enc')
    if enc is None:
        return "Missing enc parameter", 400
    data = concat_base64(auth_key, enc)
    future = Future()
    script.post({"type": "cert", "data": data})
    cert_requests_lock.acquire()
    cert_requests[auth_key] = future
    cert_requests_lock.release()
    return future.result(), 200


def on_message(message: dict[str, Any], _):
    print(f"[*] Handling incoming device message {message}")
    message_payload = message.get("payload", {})
    message_type: str | None = message.get("type")
    if message_type is None:
        return

    auth_key: str | None = message_payload.get("authKey")
    if auth_key is None:
        return

    message_caller: str | None = message_payload.get("caller")
    if message_caller == "gpia":
        gpia_requests_lock.acquire()
        gpia_future = gpia_requests.get(auth_key)
        gpia_requests_lock.release()
        if gpia_future is None:
            return

        if message_type == "error":
            gpia_future.set_result({
                "error": message.get("description", "Unknown error")
            })
        else:
            gpia_future.set_result({
                "token": message_payload.get("token")
            })

    elif message_caller == "cert":
        cert_requests_lock.acquire()
        cert_future = cert_requests.get(auth_key)
        cert_requests_lock.release()
        if cert_future is None:
            return

        if message_type == "error":
            cert_future.set_result({
                "error": message.get("description", "Unknown error")
            })
        else:
            cert_future.set_result({
                "signature": message_payload.get("signature"),
                "certificate": message_payload.get("certificate"),
            })


if __name__ == '__main__':
    device = frida.get_usb_device()
    session = device.attach("WhatsApp Business")

    with open('android.js', 'r', encoding='utf-8') as script_file:
        js_code = script_file.read()

    script = session.create_script(js_code)
    script.load()
    script.on("message", on_message)

    print('[*] Running server on port 1119')

    serve(app, host="0.0.0.0", port=1119)
