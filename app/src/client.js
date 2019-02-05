// @flow

// Connects to the Websocket server

class Client {
    ws: WebSocket;

    constructor() {
        console.log("Connecting to websocket");

        let location = window.location;
        var url = 'ws://'+location.hostname+(location.port ? ':'+location.port: '') + '/ws';

        this.ws = new WebSocket(url);
        this.ws.addEventListener('open', this.open);        
        this.ws.addEventListener('close', this.close);        
        this.ws.addEventListener('message', this.message);        
    }

    open = () => {
        console.log("Websocket connection open", this.ws);
        this.ws.send(JSON.stringify({action: "experiments.refresh" }));
    }

    close = () => {
        console.log("Websocket connection closed");
    }

    message = (event: any) => {        
        console.log('Message from server ', event.data);
    }


    /** Send without waiting for an answer */
    send = (data: any) => {
        // return this.ws.send(JSON.stringify(data));
    }
}

export default new Client();