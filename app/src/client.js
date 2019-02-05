// @flow

import store from './store'

/// Connects to the Websocket server
class Client {
    ws: WebSocket;
    waiting: Map<number, any>;
    queued: Array<any>;

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
        store.dispatch({ type: "CONNECTED", payload: true });
    }

    close = () => {
        store.dispatch({ type: "CONNECTED", payload: false });
    }

    message = (event: any) => {        
        store.dispatch(JSON.parse(event.data));
    }


    /** Send without waiting for an answer */
    send = (data: any) => {
        if (this.ws.readyState === WebSocket.OPEN) {
            return this.ws.send(JSON.stringify(data));
        } else {
            console.log("Connection not ready");
        }
            
    }

    /** Wait for an answer */
    query = (data: any, timeout: number = 60) => {
        return this.ws.send(JSON.stringify(data));
    }
}

export default new Client();