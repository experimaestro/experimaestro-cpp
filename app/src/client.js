// @flow

// Connects to the Websocket server

export default class Client {
    ws: WebSocket;

    constructor() {
        this.ws = new WebSocket("ws://localhost:12345");
        this.ws.addEventListener('open', this.open);        
        this.ws.addEventListener('close', this.close);        
        this.ws.addEventListener('message', this.message);        
    }

    open = () => {
        console.log("Websocket connection open", this.ws);
        this.ws.send(JSON.stringify({a: "Hello" }));
    }

    close = () => {
        console.log("Websocket connection closed");
    }

    message = (event: any) => {
        console.log('Message from server ', event.data);
    }
}