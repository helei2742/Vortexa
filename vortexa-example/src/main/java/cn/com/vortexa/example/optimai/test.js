function stableStringify(obj, options) {
    const opts = {};
    const space = typeof opts.space === "number" ? " ".repeat(opts.space) : opts.space || "";
    const allowCycles = opts.cycles ?? false;
    const replacer = opts.replacer || ((key, value) => value);
    const compareFn = opts.cmp ? (node) => (a, b) => opts.cmp({ key: a, value: node[a] }, { key: b, value: node[b] }) : void 0;
    const seenObjects = [];
    function stringify(parent, key, node, level) {
        const indent = space ? "\n" + space.repeat(level) : "";
        const colonSeparator = space ? ": " : ":";
        if (node && typeof node.toJSON === "function") {
            node = node.toJSON();
        }
        node = replacer.call(parent, key, node);
        if (node === void 0) return;
        if (typeof node !== "object" || node === null) return JSON.stringify(node);
        if (Array.isArray(node)) {
            const items = node.map(
                (item, index) => stringify(node, index, item, level + 1) || JSON.stringify(null)
            );
            return `[${items.map((item) => indent + space + item).join(",")}${indent}]`;
        } else {
            if (seenObjects.includes(node)) {
                if (allowCycles) return JSON.stringify("__cycle__");
                throw new TypeError("Converting circular structure to JSON");
            }
            seenObjects.push(node);
            const keys = Object.keys(node).sort(compareFn && compareFn(node));
            const keyValuePairs = keys.map((propKey) => {
                const value = stringify(node, propKey, node[propKey], level + 1);
                return value ? `${indent + space + JSON.stringify(propKey) + colonSeparator + value}` : "";
            }).filter(Boolean);
            seenObjects.splice(seenObjects.indexOf(node), 1);
            return `{${keyValuePairs.join(",")}${indent}}`;
        }
    }
    return stringify({ "": obj }, "", obj, 0) || "";
}

function generateClientToken(deviceInfo) {
    try {
        const payload = {
            client_app_id: CLIENT_APP_ID || "",
            timestamp: Date.now(),
            device_info: deviceInfo
        };
        if (!CLIENT_SECRET) ;
        const payloadString = stableStringify(payload);
        const keyData = stringToUint8Array(CLIENT_SECRET);
        const key = await crypto.subtle.importKey(
            "raw",
            keyData,
            { name: "HMAC", hash: "SHA-256" },
            false,
            ["sign"]
        );
        const signatureBuffer = await crypto.subtle.sign("HMAC", key, stringToUint8Array(payloadString));
        const signature = uint8ArrayToHexString(new Uint8Array(signatureBuffer));
        const tokenPayload = {
            ...payload,
            signature
        };
        return btoa(stableStringify(tokenPayload));
    } catch (error) {
        console.log("Generate client token error", error);
        return "";
    }
}
