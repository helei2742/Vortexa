/* eslint-disable no-restricted-globals */
/* eslint-disable no-undef */
importScripts('./scripts/firebase-app.js');
importScripts('./scripts/firebase-messaging.js');

firebase.initializeApp({
    apiKey: 'AIzaSyDxeJTktPI-R-VGXMNvMUb1OCzTUliJNmU',
    projectId: 'haha-notifications',
    messagingSenderId: '192475249754',
    appId: '1:192475249754:web:24c63b7333b52ddca96c63',
});

const sendClickedMessage = (client, data) => {
    client.postMessage({
        type: 'PUSH_NOTIFICATION_CLICKED',
        data,
    });
};

self.addEventListener('notificationclick', event => {
    event.notification.close();

    let focusedClient = null;

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
            for (const client of clientList) {
                focusedClient = client;
                return client.focus();
            }
            return clients.openWindow('/home.html#');
        }),
    );

    let counter = 0;

    const tryToSendClickMessage = () => {
        counter++;

        if (counter > 10) {
            return;
        }

        self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
            if (clients.length === 0) {
                setTimeout(tryToSendClickMessage, 1000);
            } else {
                clients.forEach(client => {
                    if (focusedClient === null) {
                        sendClickedMessage(client, event.notification.data);
                    } else if (client.id === focusedClient?.id) {
                        sendClickedMessage(client, event.notification.data);
                    }
                });
            }
        });
    };

    tryToSendClickMessage();
});

const messaging = firebase.messaging();
messaging.onBackgroundMessage(payload => {
    // Send the payload data to the UI thread (open pages)
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
        clients.forEach(client => {
            client.postMessage({
                type: 'PUSH_NOTIFICATION_RECEIVED',
                data: payload,
            });
        });
    });
});
