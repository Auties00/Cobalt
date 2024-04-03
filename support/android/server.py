import base64
import random
import string
from concurrent.futures import Future
from threading import Lock
from typing import Any

from flask import Flask, request
import frida
from waitress import serve

global personal_script
global business_script

app = Flask(__name__)

gpia_requests: dict[str, Future] = {}
gpia_requests_lock: Lock = Lock()
cert_requests: dict[str, Future] = {}
cert_requests_lock: Lock = Lock()
info_requests: dict[str, Future] = {}
info_requests_lock: Lock = Lock()


def concat_base64(base64_str1: str | None, base64_str2: str | None) -> str:
    bytes1 = base64.b64decode(base64_str1)
    bytes2 = base64.b64decode(base64_str2)
    return base64.b64encode(bytes1 + bytes2).decode('utf-8')


@app.route('/gpia')
def gpia_route() -> (str, int):
    auth_key: str | None = request.args.get('authKey')
    if auth_key is None:
        return "Missing authKey parameter", 400

    business: str | None = request.args.get("business")
    if business is None:
        return "Missing business parameter", 400

    future = Future()
    if business.lower() == "true":
        business_script.post({"type": "gpia", "authKey": auth_key})
    else:
        personal_script.post({"type": "gpia", "authKey": auth_key})
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

    business: str | None = request.args.get("business")
    if business is None:
        return "Missing business parameter", 400

    data = concat_base64(auth_key, enc)
    future = Future()
    if business.lower() == "true":
        business_script.post({"type": "cert", "data": data})
    else:
        personal_script.post({"type": "cert", "data": data})
    cert_requests_lock.acquire()
    cert_requests[auth_key] = future
    cert_requests_lock.release()
    return future.result(), 200


@app.route('/info')
def info_route() -> (str, int):
    business: str | None = request.args.get("business")
    if business is None:
        return "Missing business parameter", 400

    future = Future()
    message_id = ''.join(random.choices(string.ascii_uppercase + string.digits, k=5))
    if business.lower() == "true":
        business_script.post({"type": "info", "id": message_id})
    else:
        personal_script.post({"type": "info", "id": message_id})
    info_requests_lock.acquire()
    info_requests[message_id] = future
    info_requests_lock.release()
    return future.result(), 200


def on_message(message: dict[str, Any], _):
    message_payload = message.get("payload", {})
    message_caller: str | None = message_payload.get("caller")
    if message_caller == "gpia":
        auth_key: str | None = message_payload.get("authKey")
        if auth_key is None:
            print("[*] No gpia auth key")
            return

        message_type: str | None = message.get("type")
        if message_type is None:
            print("[*] Missing message type")
            return

        gpia_requests_lock.acquire()
        gpia_future = gpia_requests.get(auth_key)
        gpia_requests_lock.release()
        if gpia_future is None:
            print("[*] No gpia request found")
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
        auth_key: str | None = message_payload.get("authKey")
        if auth_key is None:
            print("[*] No cert auth key")
            return

        message_type: str | None = message.get("type")
        if message_type is None:
            print("[*] Missing message type")
            return

        cert_requests_lock.acquire()
        cert_future = cert_requests.get(auth_key)
        cert_requests_lock.release()
        if cert_future is None:
            print("[*] No cert request found")
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
    elif message_caller == "info":
        message_id: str | None = message_payload.get("id")
        if message_id is None:
            print("[*] No message id")
            return

        message_type: str | None = message_payload.get("type")
        if message_type is None:
            print("[*] Missing message type")
            return

        info_requests_lock.acquire()
        info_future = info_requests.get(message_id)
        info_requests_lock.release()
        if info_future is None:
            print("[*] No info request found")
            return

        if message_type == "error":
            info_future.set_result({
                "error": message_payload.get("description", "Unknown error")
            })
        else:
            info_future.set_result({
                    "packageName": message_payload.get("packageName"),
                    "version": message_payload.get("version"),
                    "apkSha256": message_payload.get("apkSha256"),
                    "apkShatr": message_payload.get("apkShatr"),
                    "apkSize": message_payload.get("apkSize"),
                    "classesMd5": message_payload.get("classesMd5"),
                    "secretKey": message_payload.get("secretKey"),
                    "signature": message_payload.get("signature"),
                    "signatureSha1": message_payload.get("signatureSha1")
            })


if __name__ == '__main__':
    device = frida.get_usb_device()

    with open('android.js', 'r', encoding='utf-8') as script_file:
        js_code = script_file.read()

    business_session = device.attach("WhatsApp Business")
    business_script = business_session.create_script(js_code)
    business_script.load()
    business_script.on("message", on_message)

    personal_session = device.attach("WhatsApp")
    personal_script = personal_session.create_script(js_code)
    personal_script.load()
    personal_script.on("message", on_message)

    print('[*] Running server on port 1119')

    serve(app, host="0.0.0.0", port=1119)
