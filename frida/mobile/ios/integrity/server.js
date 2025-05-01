import http from 'http';
import url from "url";
import {nextTick} from 'node:process';

setTimeout(() => {
    console.log("[*] Script loaded")

    function semaphore(capacity) {
        var semaphore = {
            capacity: capacity || 1,
            current: 0,
            queue: [],
            firstHere: false,

            take: function () {
                if (semaphore.firstHere === false) {
                    semaphore.current++;
                    semaphore.firstHere = true;
                    var isFirst = 1;
                } else {
                    var isFirst = 0;
                }
                var item = {n: 1};

                if (typeof arguments[0] == 'function') {
                    item.task = arguments[0];
                } else {
                    item.n = arguments[0];
                }

                if (arguments.length >= 2) {
                    if (typeof arguments[1] == 'function') item.task = arguments[1];
                    else item.n = arguments[1];
                }

                var task = item.task;
                item.task = function () {
                    task(semaphore.leave);
                };

                if (semaphore.current + item.n - isFirst > semaphore.capacity) {
                    if (isFirst === 1) {
                        semaphore.current--;
                        semaphore.firstHere = false;
                    }
                    return semaphore.queue.push(item);
                }

                semaphore.current += item.n - isFirst;
                item.task(semaphore.leave);
                if (isFirst === 1) semaphore.firstHere = false;
            },

            leave: function (n) {
                n = n || 1;

                semaphore.current -= n;

                if (!semaphore.queue.length) {
                    if (semaphore.current < 0) {
                        throw new Error('leave called too many times.');
                    }

                    return;
                }

                var item = semaphore.queue[0];

                if (item.n + semaphore.current > semaphore.capacity) {
                    return;
                }

                semaphore.queue.shift();
                semaphore.current += item.n;

                nextTick(item.task);
            },

            available: function (n) {
                n = n || 1;
                return (semaphore.current + n <= semaphore.capacity);
            }
        };

        return semaphore;
    }

    let integritySemaphore = semaphore()

    const personalBundleName = "net.whatsapp.WhatsApp"
    const personalServerPort = 1119
    const businessServerPort = 1120
    const bundleName = ObjC.classes.NSBundle.mainBundle().infoDictionary().objectForKey_("CFBundleIdentifier").toString()
    const NSData = ObjC.classes.NSData
    const NSString = ObjC.classes.NSString
    const DCAppAttestService = ObjC.classes.DCAppAttestService["+ sharedService"]()

    function onIntegrity(req, res) {
        integritySemaphore.take(() => {
            try {
                const authKey = NSData.alloc().initWithBase64EncodedString_options_(NSString.stringWithUTF8String_(Memory.allocUtf8String(req.authKey)), 0).SHA256Hash()
                let keyHandler = new ObjC.Block({
                    retType: 'void',
                    argTypes: ['object', 'object'],
                    implementation(keyId, error) {
                        let keyIdData = keyId.toString()
                        if (error !== null) {
                            res.end(JSON.stringify({
                                "error": error.toString()
                            }))
                            setTimeout(() => integritySemaphore.leave(1), 1000)
                        } else {
                            let attestationHandler = new ObjC.Block({
                                retType: 'void',
                                argTypes: ['object', 'object'],
                                implementation(attestation, error) {
                                    if (error !== null) {
                                        res.end(JSON.stringify({
                                            "error": error.toString()
                                        }))
                                        setTimeout(() => integritySemaphore.leave(1), 1000)
                                    } else {
                                        attestation = attestation.base64EncodedStringWithOptions_(0).toString()
                                        keyId = NSString.stringWithUTF8String_(Memory.allocUtf8String(keyIdData))
                                        let assertionHandler = new ObjC.Block({
                                            retType: 'void',
                                            argTypes: ['object', 'object'],
                                            implementation(assertion, error) {
                                                if (error !== null) {
                                                    res.end(JSON.stringify({
                                                        "error": error.toString()
                                                    }))
                                                } else {
                                                    assertion = assertion.base64EncodedStringWithOptions_(0).toString()
                                                    res.end(JSON.stringify({
                                                        "attestation": attestation,
                                                        "assertion": assertion
                                                    }))
                                                }
                                                setTimeout(() => integritySemaphore.leave(1), 1000)
                                            }
                                        });
                                        DCAppAttestService["- generateAssertion:clientDataHash:completionHandler:"](keyId, authKey, assertionHandler)
                                    }
                                }
                            });
                            DCAppAttestService["- attestKey:clientDataHash:completionHandler:"](keyId, authKey, attestationHandler)
                        }
                    }
                });
                DCAppAttestService.generateKeyWithCompletionHandler_(keyHandler)
            } catch (error) {
                res.end(JSON.stringify({
                    "error": error.toString() + "\n" + error.stack.toString()
                }))
                setTimeout(() => integritySemaphore.leave(1), 1000)
            }
        })
    }

    console.log("[*] All server components are ready")
    const server = http.createServer((req, res) => {
        let parsedRequest = url.parse(req.url, true)
                    switch (parsedRequest.pathname) {
                        case "/integrity":
                            res.writeHead(200, {"Content-Type": "application/json"});
                            onIntegrity(parsedRequest.query, res)
                            break;
                        default:
                            res.writeHead(404, {"Content-Type": "application/json"});
                            res.end(JSON.stringify({"error": "Unknown method"}))
                            break;
                    }
    })

    const serverPort = bundleName === personalBundleName ? personalServerPort : businessServerPort
    server.listen(serverPort, () => {
        console.log("[*] Server ready on port", serverPort)
    })
}, 1000)