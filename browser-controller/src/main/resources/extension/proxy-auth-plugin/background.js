chrome.cookies.get({ url: "http://localhost", name: "proxyAuth" }, function(cookie) {
    if (cookie) {
        let credentials = JSON.parse(cookie.value);
        var config = {
            mode: "fixed_servers",
            rules: {
                singleProxy: {
                    scheme: "http",
                    host: credentials.host,
                    port: parseInt(credentials.port)
                },
                bypassList: ["localhost"]
            }
        };

        chrome.proxy.settings.set({ value: config, scope: "regular" }, function() {});

        chrome.webRequest.onAuthRequired.addListener(
            function(details) {
                return {
                    authCredentials: {
                        username: credentials.username,
                        password: credentials.password
                    }
                };
            },
            { urls: ["<all_urls>"] },
            ["blocking"]
        );
    }
});
