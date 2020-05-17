export class Communicator {

static newInstance(): Communicator {
  return new Communicator();
}

constructor() {

}

static _name: string = 'communicator';

static _version: string = '0.0.0.1';

_eventNotify: any = null
 
_channelMap: Map<String, any> = new Map();

_socketMap: Map<String,any> = new Map();

_idCounter: number = 0;

_totalSentMessage: number = 0;
_totalSentBytes: number = 0;

_timer:any = null;

_totalReceivedMessage: number = 0;
_totalReceivedBytes: number = 0;

_begin: number = new Date().getTime();

_receivingBegin: number = new Date().getTime();

static STATE = {
  INITIALIZING: 'INITIALIZING', 
  SENT: 'SENT',
  READY: 'READY'
};
static TYPE = {
    QUEUE: 'QUEUE', 
    TOPIC: 'TOPIC'
  };

  static EVENT = {
    OPEN: 'OPENED', 
    CLOSE: 'CLOSED', 
    EXIST: 'EXIST', 
    FAIL: 'FAIL',
    PUSH: 'PUSH', 
    DATA: 'DATA'
  };

  static MESSAGE = {
    CONSUME: 'CONSUME', 
    UNCONSUME: 'UNCONSUME',
    PRODUCE: 'PRODUCE', 
    SUBSCRIBE: 'SUBSCRIBE', 
    UNSUBSCRIBE: 'UNSUBSCRIBE', 
    PUBLISH: 'PUBLISH', 
    HEARTBEAT: 'HEARTBEAT',
    SEND: 'SEND', 
    PUSH: 'PUSH', 

    ACK_CONSUME: 'ACK_CONSUME', 
    ACK_PRODUCE: 'ACK_PRODUCE',
    ACK_UNCONSUME: 'ACK_UNCONSUME',
    ACK_PUBLISH: 'ACK_PUBLISH',
    ACK_SUBSCRIBE: 'ACK_SUBSCRIBE',
    ACK_UNSUBSCRIBE: 'ACK_UNSUBSCRIBE',
    ACK_HEARTBEAT: 'ACK_HEARTBEAT', 
    ACK_PUSH: 'ACK_PUSH'
  };

  _authenticatedInfo = {
    parentId: null, 
    systemId: null, 
    ticket: null, 
    appId: null, 
    destSystemId: null
  };

  isReady: boolean = false;

  tryReconnect(url) {
    //console.log('start to connect to remote...');
    const socketInfo = this._socketMap.get(url);
    if(socketInfo) {
    //  console.log('socket exist. reuse it...');
  
      //perform consume or subscribe.
      if (socketInfo.socket.isReady) {
        this.processInitializing(socketInfo.socket);
      }
      
      return ;
    }

    let socket = <any>new WebSocket(url);
    socket.isReady = false;
    socket.workingUrl = url;
    //console.log('socket->', socket);
    socket.onmessage = (event) => {
      Communicator.onMessage(this, socket, event);
    }
    socket.onopen = (event) => {
      Communicator.onOpen(this, socket, event);
    }
    socket.onclose = (event) => {
      Communicator.onClose(this, socket, event);
    }
    socket.onerror = (event) => {
      Communicator.onError(this, socket, event);
    }
  
    //add socket information.
    this._socketMap.set(url, {
      socket: socket, 
      refCount: 0
    });
  
  }

  processInitializing(socket) {
    this._channelMap.forEach((channel) => {
      if (channel.url == socket.workingUrl) {
        if (channel.state == Communicator.STATE.INITIALIZING) {
            let requestMessage = {
            parentId: this._authenticatedInfo.parentId, 
            systemId: this._authenticatedInfo.systemId, 
            topicParentId: null, 
            topicSystemId: null, 
            topicMemberSystemId: null, 
            ticket: this._authenticatedInfo.ticket, 
            id: ++this._idCounter, 
            action: channel.type == Communicator.TYPE.QUEUE ?
            Communicator.MESSAGE.CONSUME : Communicator.MESSAGE.SUBSCRIBE,
            address: channel.address,
            appId: this._authenticatedInfo.appId, 
            key: channel.key, 
            destSystemId: null
          };

          if (channel.type == Communicator.TYPE.TOPIC) {
            requestMessage.topicParentId = channel.parentId;
            requestMessage.topicSystemId = channel.systemId;
            requestMessage.topicMemberSystemId = channel.memberSystemId;
            requestMessage.destSystemId = channel.destSystemId;
          }

          const message = JSON.stringify(requestMessage);
          //console.log('websocket->', socket);
          socket.send(message);
          channel.state = Communicator.STATE.SENT;
          //console.log('channel->', channel);
         // console.log('send initializing request ok...message->', message);
        }
      }
    });
  }

static onOpen(communicator, socket, event) {
    //try to send message.
    //console.log('websocket is opened...communicator->', communicator, ', event->', event);

    socket.isReady = true;
    communicator.processInitializing(socket);
}

static onMessage(communicator, socket, event) {
  const responseMessage = JSON.parse(event.data);

  if (Communicator.MESSAGE.ACK_CONSUME == responseMessage.action) {
    const responseMessage = JSON.parse(event.data);

    const channelKey = responseMessage.type + '_' + responseMessage.address;
    const channelInfo = communicator._channelMap.get(channelKey);
    if (null == channelInfo) {
      console.log('not found channel information in push...');
      return ;
    }
    if (channelInfo.type != responseMessage.type) {
      console.log('channel not right...');
      return ;
    }
    communicator.ackConsume(channelInfo, responseMessage);

    return;
  }

  if (Communicator.MESSAGE.ACK_SUBSCRIBE == responseMessage.action) {
    const responseMessage = JSON.parse(event.data);

    const channelKey = responseMessage.type + '_' + responseMessage.address;
    const channelInfo = communicator._channelMap.get(channelKey);
    if (null == channelInfo) {
      console.log('not found channel information in push...');
      return ;
    }
    if (channelInfo.type != responseMessage.type) {
      console.log('channel not right...');
      return ;
    }
    communicator.ackSubscribe(channelInfo, responseMessage);

    return;
  }

  if (Communicator.MESSAGE.ACK_UNCONSUME == responseMessage.action) {
    const responseMessage = JSON.parse(event.data);

    const channelKey = responseMessage.type + '_' + responseMessage.address;
    const channelInfo = communicator._channelMap.get(channelKey);
    if (null == channelInfo) {
      console.log('not found channel information in push...');
      return ;
    }
    if (channelInfo.type != responseMessage.type) {
      console.log('channel not right...');
      return ;
    }
    communicator.ackUnconsume(channelInfo, responseMessage);

    return ;
  }

  if (Communicator.MESSAGE.ACK_UNSUBSCRIBE == responseMessage.action) {
    const responseMessage = JSON.parse(event.data);

    const channelKey = responseMessage.type + '_' + responseMessage.address;
    const channelInfo = communicator._channelMap.get(channelKey);
    if (null == channelInfo) {
      console.log('not found channel information in push...');
      return ;
    }
    if (channelInfo.type != responseMessage.type) {
      console.log('channel not right...');
      return ;
    }
    communicator.ackUnsubscribe(channelInfo, responseMessage);

    return ;
  }

  if (Communicator.MESSAGE.PUSH == responseMessage.action) {
    const responseMessage = JSON.parse(event.data);

    const channelKey = responseMessage.type + '_' + responseMessage.address;
    const channelInfo = communicator._channelMap.get(channelKey);
    if (null == channelInfo) {
      console.log('not found channel information in push...');
      return ;
    }
    if (channelInfo.type != responseMessage.type) {
      console.log('channel not right...');
      return ;
    }

    communicator.ackPush(channelInfo, responseMessage);

    return ;
  }
}

static onClose(communicator, socket, event) {
  communicator._socketMap.delete(socket.workingUrl);

  communicator._channelMap.forEach((channel) => {
      if (channel.url == socket.workingUrl) {
        channel.state = Communicator.STATE.INITIALIZING;
      }
    });

      //check if reconnect.
      setTimeout(() => {
         if (communicator.countSocketChannel(socket.workingUrl) <= 0) {
            console.log('no socket channel available...workingUrl->', socket.workingUrl);
            
            return ;
          }

          communicator.tryReconnect(socket.workingUrl);
      }, 1000 * 6);
}

static onError(communicator, socket, event) {
  communicator._socketMap.delete(socket.workingUrl);

  communicator._channelMap.forEach((channel) => {
      if (channel.url == socket.workingUrl) {
        channel.state = Communicator.STATE.INITIALIZING;
      }
    });

      setTimeout(() => {
          if (communicator.countSocketChannel(socket.workingUrl) <= 0) {
            console.log('no socket channel available...workingUrl->', socket.workingUrl);
            
            return ;
          }

          communicator.tryReconnect(socket.workingUrl);
      }, 1000 * 6);
}

ackPush(channelInfo, responseMessage) {
  //console.log('responseMessage->', responseMessage);
  this._eventNotify({
    event: Communicator.EVENT.DATA, 
    action: responseMessage.action, 
    type: channelInfo.type,
    address: channelInfo.address, 
    id: responseMessage.id, 
    appId: this._authenticatedInfo.appId, 
    data: responseMessage.data, 
    userData: channelInfo.userData
  });

  const socketInfo = this._socketMap.get(channelInfo.url);
  if(!socketInfo) {
    console.log('socket not exist..');

    //perform consume or subscribe.
    return ;
  }

  if (responseMessage.type != 'TOPIC') {
    //ack push by socket.
    const ackMessage = {
      parentId: this._authenticatedInfo.parentId, 
      systemId: this._authenticatedInfo.systemId, 
      ticket: this._authenticatedInfo.ticket, 
      type: responseMessage.type, 
      address: responseMessage.address, 
      id: responseMessage.id, 
      appId: this._authenticatedInfo.appId, 
      action: Communicator.MESSAGE.ACK_PUSH, 
      destSystemId: responseMessage.fromSystemId
    };
    const message = JSON.stringify(ackMessage);
    //console.log('websocket->', socket);
    socketInfo.socket.send(message);  
  }

  //console.log(responseMessage.type, ' acking id->', ackMessage.id);
  this._totalReceivedMessage++;
  this._totalReceivedBytes += responseMessage.data.length;
  const diffTime = new Date().getTime() -  this._receivingBegin;
  if (diffTime > 1000 * 10) {
    //console.log('receiving speed->', this._totalReceivedMessage / (diffTime / 1000));
    //console.log('receiving bytes speed->', this._totalReceivedBytes / (diffTime / 1000));

    this._totalReceivedMessage = 0;
    this._totalReceivedBytes = 0;
    this._receivingBegin = new Date().getTime();
  }
}

ackUnsubscribe(channelInfo, responseMessage) {
  const channelKey = channelInfo.type + '_' + channelInfo.address;

  const channel = this._channelMap.get(channelKey);
  if (!channel) {
    console.log('not found channel...');

    return ;
  }

  if (0 != responseMessage.result) {
    console.log('unsubscribe failed...');
    this._eventNotify({
      event: Communicator.EVENT.FAIL, 
      action: responseMessage.action, 
      type: channelInfo.type, 
      url: channel.url, 
      sendingUrl: channel.sendingUrl, 
      destSystemId: channelInfo.destSystemId,
      appId: this._authenticatedInfo.appId, 
      address: channelInfo.address, 
      userData: channelInfo.userData
    });

    console.log('unsubscribe failed. channel information->', channelInfo);

    return ;
  }

  const key = responseMessage.type + '_' + responseMessage.address;
  console.log('key->', key);

  if (!this._channelMap.get(key)) {
    console.log('channel not found', key);

    return ;
  }

this._channelMap.delete(key);

this._eventNotify({
  event: Communicator.EVENT.CLOSE, 
  action: responseMessage.action, 
  url: channel.url, 
  sendingUrl: channel.sendingUrl, 
  type: channelInfo.type, 
  address: channelInfo.address,
  destSystemId: channelInfo.destSystemId, 
  appId: this._authenticatedInfo.appId, 
  userData: channelInfo.userData
});

const socketInfo = this._socketMap.get(channelInfo.url);
if (socketInfo) {
  socketInfo.refCount--;
  if (socketInfo.refCount <= 0) {
    console.log('start to remove socket...');
    let tmpChannel = this._socketMap.get(socketInfo.url);
    if (tmpChannel) {
      console.log('start to close socket...');
      tmpChannel.socket.close();
    }
    this._socketMap.delete(socketInfo.url);
  }
}
}

ackUnconsume(channelInfo, responseMessage) {
  if (0 != responseMessage.result) {
    console.log('unconsume failed...');

    this._eventNotify({
      event: Communicator.EVENT.FAIL, 
      action: responseMessage.action, 
      type: channelInfo.type, 
      destSystemId: channelInfo.destSystemId,
      appId: this._authenticatedInfo.appId, 
      address: channelInfo.address, 
      userData: channelInfo.userData
    });

    console.log('unconsume failed. channel information->', channelInfo);

    return ;
  }

  const key = responseMessage.type + '_' + responseMessage.address;
  console.log('key->', key);

  if (!this._channelMap.get(key)) {
    console.log('channel not found', key);

    return ;
  }

this._channelMap.delete(key);

this._eventNotify({
  event: Communicator.EVENT.CLOSE, 
  action: responseMessage.action, 
  type: channelInfo.type, 
  address: channelInfo.address,
  destSystemId: channelInfo.destSystemId, 
  appId: this._authenticatedInfo.appId, 
  userData: channelInfo.userData
});

const socketInfo = this._socketMap.get(channelInfo.url);
if (socketInfo) {
  socketInfo.refCount--;
  if (socketInfo.refCount <= 0) {
    console.log('start to remove socket...');
    let tmpChannel = this._socketMap.get(socketInfo.url);
    if (tmpChannel) {
      console.log('start to close socket...');
      tmpChannel.socket.close();
    }
    this._socketMap.delete(socketInfo.url);
  }
}

}

ackSubscribe(channelInfo, responseMessage) {
  const channelKey = channelInfo.type + '_' + channelInfo.address;
  
  const channel = this._channelMap.get(channelKey);
  if (!channel) {
    console.log('not found channel...');

    return ;
  }

  if (0 != responseMessage.result) {
    //subscribe topic  failed.
    console.log('subscribe failed...');

     this._channelMap.delete(channelKey);

      this._eventNotify({
        event: Communicator.EVENT.FAIL, 
        type: channelInfo.type, 
        action: responseMessage.action, 
        destSystemId: channelInfo.destSystemId,
        appId: this._authenticatedInfo.appId, 
        address: channelInfo.address,
        description: responseMessage.description, 
        userData: channelInfo.userData
    });

    return ;
  }

  //channel is ready.
  channelInfo.state = Communicator.STATE.READY;
  //console.log('consume channel ok, channel->', channelInfo);

  this._eventNotify({
        event: Communicator.EVENT.OPEN, 
        url: channel.url, 
        sendingUrl: channel.sendingUrl, 
        type: channelInfo.type, 
        action: responseMessage.action, 
        destSystemId: channelInfo.destSystemId,
        appId: this._authenticatedInfo.appId, 
        address: channelInfo.address, 
        userData: channelInfo.userData
    });

  const socketInfo = this._socketMap.get(channelInfo.url);
  if (socketInfo) {
    socketInfo.refCount++;
  }

//  console.log('subscribe channel', channel.address, ' ok...');
}

ackConsume(channelInfo, responseMessage) {
  const channelKey = channelInfo.type + '_' + channelInfo.address;
  
  const channel = this._channelMap.get(channelKey);
  if (!channel) {
    console.log('not found channel...');

    return ;
  }

  if (0 != responseMessage.result) {
    //consume queue failed.
    console.log('consume failed...');

     this._channelMap.delete(channelKey);

      this._eventNotify({
        event: Communicator.EVENT.FAIL, 
        type: channelInfo.type, 
        action: responseMessage.action, 
        destSystemId: channelInfo.destSystemId,
        appId: this._authenticatedInfo.appId, 
        address: channelInfo.address, 
        description: responseMessage.description, 
        userData: channelInfo.userData
    });

    return ;
  }

  //channel is ready.
  channelInfo.state = Communicator.STATE.READY;
  //console.log('consume channel ok, channel->', channelInfo);

  this._eventNotify({
        event: Communicator.EVENT.OPEN, 
        url: channel.url, 
        sendingUrl: channel.sendingUrl, 
        type: channelInfo.type, 
        action: responseMessage.action, 
        destSystemId: channelInfo.destSystemId,
        appId: this._authenticatedInfo.appId, 
        address: channelInfo.address, 
        userData: channelInfo.userData
    });

  const socketInfo = this._socketMap.get(channelInfo.url);
  if (socketInfo) {
    socketInfo.refCount++;
  }
//  console.log('consume ', channelInfo.address, ' ok...');
}

  setAuthenticatedInfo(authenticatedInfo) {
    this._authenticatedInfo = Object.assign({}, authenticatedInfo);
  }

  setEventNotify(eventNotify) {
    this._eventNotify = eventNotify;
  }

  destroy() {
    this._socketMap.forEach((mapItem) => {
      mapItem.socket.close();
    });
    this._socketMap.clear();

    this._channelMap.clear();
    //clean socket.
//    console.log('communicator closed...');
  }

produce(url, destSystemId, address, message): boolean {
  let retValue = false;

  var data = new FormData();
  data.append('parentId', this._authenticatedInfo.parentId);
  data.append('systemId', this._authenticatedInfo.systemId);
  data.append('ticket', this._authenticatedInfo.ticket);
  data.append('id', ++this._idCounter + '');
  data.append('destSystemId', destSystemId);
  data.append('type', Communicator.TYPE.QUEUE);
  data.append('address', address);
  data.append('appId', this._authenticatedInfo.appId);
  data.append('data', message);
  
  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  let sendingUrl = url + '/client/send';
  //console.log('sendingUrl->', sendingUrl);
  xhr.open('POST', sendingUrl, true);
  xhr.timeout = 1000 * 15;
  xhr.ontimeout = (event) => {
    console.log('produce message timeout, event->', event);
  };
  xhr.onerror = (event) => {
    console.log('produce data to queue error happened->', event);
  };
  xhr.onload = () => {
      // do something to response
      if (!xhr.responseText) {
        console.log('xhr.responseText->', xhr.responseText);

        return;
      }

      const responseMessage = JSON.parse(xhr.responseText);
        if (0 != responseMessage.result) {
          console.log('xhr.responseText->', xhr.responseText);

          this._eventNotify({
            event: Communicator.EVENT.FAIL, 
            sendingUrl: url, 
            action: Communicator.MESSAGE.SEND, 
            type: Communicator.TYPE.QUEUE, 
            destSystemId: destSystemId,
            address: address
        });

        return ;
      }
  };
  xhr.send(data);

  this._totalSentMessage++;
  this._totalSentBytes += message.length;

  retValue = true;

  return retValue;
}

send(channel, message, durable = true) {
  if (!channel) {
    return ;
  }
  //use http to send.
  //search channel.
  const channelKey = channel.type + '_' + channel.address;
  const channelInfo = this._channelMap.get(channelKey);
  if (!channelInfo) {
    console.log('not found channel information...channelKey->', channelKey);
    return false;
  }

  if (Communicator.TYPE.QUEUE == channelInfo.type) {
    var data = new FormData();
    data.append('parentId', this._authenticatedInfo.parentId);
    data.append('systemId', this._authenticatedInfo.systemId);
    data.append('ticket', this._authenticatedInfo.ticket);
    data.append('id', ++this._idCounter + '');
    data.append('destSystemId', channelInfo.destSystemId);
    data.append('type', channelInfo.type);
    data.append('address', channelInfo.address);
    data.append('appId', this._authenticatedInfo.appId);
    data.append('data', message);
    data.append('durable', '' + durable);
    
    var xhr = new XMLHttpRequest();
    xhr.withCredentials = true;
    let sendingUrl = channelInfo.sendingUrl + '/client/send';
    //console.log('sendingUrl->', sendingUrl);
    xhr.open('POST', sendingUrl, true);
    xhr.timeout = 1000 * 15;
    xhr.ontimeout = (event) => {
      console.log('send message timeout, event->', event);
    };
    xhr.onerror = (event) => {
      console.log('send data to queue error happened->', event);
    };
    xhr.onload = () => {
        // do something to response
        if (!xhr.responseText) {
          console.log('xhr.responseText->', xhr.responseText);

          return;
        }
  
        const responseMessage = JSON.parse(xhr.responseText);
          if (0 != responseMessage.result) {
            console.log('xhr.responseText->', xhr.responseText);

            this._eventNotify({
              event: Communicator.EVENT.FAIL, 
              sendingUrl: channelInfo.sendingUrl, 
              action: Communicator.MESSAGE.SEND, 
              type: channel.type, 
              destSystemId: channel.destSystemId,
              address: channel.address
          });
  
          return ;
        }
    };
    xhr.send(data);

    this._totalSentMessage++;
    this._totalSentBytes += message.length;
  } else {
    //loop send all topic address.
    const topicAddresses = channelInfo.topicAddresses;
    for (let i = 0; i < topicAddresses.length; ++i) {
      const topicAddress = topicAddresses[i];

      var data = new FormData();
      data.append('parentId', this._authenticatedInfo.parentId);
      data.append('systemId', this._authenticatedInfo.systemId);
      data.append('ticket', this._authenticatedInfo.ticket);
      data.append('topicParentId', channelInfo.parentId);
      data.append('topicSystemId', channelInfo.systemId);
      data.append('topicMemberSystemId', channelInfo.memberSystemId);
      data.append('id', ++this._idCounter + '');
      data.append('destSystemId', channelInfo.destSystemId);
      data.append('type', channelInfo.type);
      data.append('address', channelInfo.address);
      data.append('appId', this._authenticatedInfo.appId);
      data.append('data', message);
      data.append('durable', '' + durable);
      
      var xhr = new XMLHttpRequest();
      xhr.withCredentials = true;
      const publishingUrl =  topicAddress + '/client/send';
 //     console.log('publishingUrl->', publishingUrl);
      xhr.open('POST', publishingUrl, true);
      xhr.timeout = 1000 * 15;
      xhr.ontimeout = (event) => {
        console.log('send message timeout, event->', event);
      };
      xhr.onerror = (event) => {
        console.log('send data to topic error happened->', event);
      };

      xhr.onload = () => {
          // do something to response
          if (!xhr.responseText) {
            console.log('xhr.responseText->', xhr.responseText);

            return;
          }
    
          const responseMessage = JSON.parse(xhr.responseText);
            if (0 != responseMessage.result) {
              console.log('responseMessage->',  responseMessage);
              this._eventNotify({
                event: Communicator.EVENT.FAIL, 
                sendingUrl: channel.sendingUrl, 
                action: Communicator.MESSAGE.SEND, 
                type: channel.type, 
                destSystemId: channel.destSystemId,
                address: channel.address
            });
    
            return ;
          }
        
      };
      xhr.send(data);

      this._totalSentMessage++;
      this._totalSentBytes += message.length;
    }
  }

  const diffTime = new Date().getTime() - this._begin;
  if (diffTime > 1000 * 10) {
    //console.log('sending speed->', this._totalSentMessage / (diffTime / 1000));
    //console.log('sending bytes speed->', this._totalSentBytes / (diffTime / 1000));

    this._begin = new Date().getTime();
    this._totalSentMessage = 0;
    this._totalSentBytes = 0;
  }

  return true;
}

removeChannel(channel) {
    const key = channel.type + '_' + channel.address;
    const channelInfo = this._channelMap.get(key);
    if (!channelInfo) {
      console.log('channel not exist.');

      return ;
    }

    if (channelInfo.state != Communicator.STATE.READY) {
      console.log('channel is not ready...');

      return ;
    }

    const socketInfo = this._socketMap.get(channelInfo.url);

    const requestMessage = {
      parentId: this._authenticatedInfo.parentId, 
      systemId: this._authenticatedInfo.systemId, 
      ticket: this._authenticatedInfo.ticket, 
      id: ++this._idCounter, 
      action: channelInfo.type == Communicator.TYPE.QUEUE ?
         Communicator.MESSAGE.UNCONSUME : Communicator.MESSAGE.UNSUBSCRIBE, 
      destSystemId: channelInfo.destSystemId,
      appId: this._authenticatedInfo.appId, 
      address: channelInfo.address
    };
    socketInfo.socket.send(JSON.stringify(requestMessage));
}

countSocketChannel(url) {
    let count = 0;

    this._channelMap.forEach((channel) => {
      if (channel.url == url) {
       ++count;
      }
    });

    return count;
}

addChannel(channelInfo){
  if (!this._timer) {
    this._timer = setInterval(() => {
      this._socketMap.forEach((socketInfo) => {
        if(socketInfo.socket.isReady) {
          this.processInitializing(socketInfo.socket);
        }
      })
    }, 1000 * 1);
  }

  const channelKey = channelInfo.type + '_' + channelInfo.address;

  let channel = this._channelMap.get(channelKey);
  if (channel) {
    console.log('channel already exist...');

    return ;
  }

//add channel information.
let newChannel = 
{
  parentId: null, 
  systemId: null, 
  memberSystemId: null, 
  state: Communicator.STATE.INITIALIZING, 
  key: channelInfo.key, 
  type: channelInfo.type, 
  address: channelInfo.address, 
  url: channelInfo.url,
  sendingUrl: channelInfo.sendingUrl, 
  destSystemId: channelInfo.destSystemId, 
  topicAddresses: null, 
  userData: channelInfo.userData
};

if (Communicator.TYPE.TOPIC == channelInfo.type) {
  newChannel.topicAddresses = channelInfo.topicAddresses;
  newChannel.parentId = channelInfo.parentId;
  newChannel.systemId = channelInfo.systemId;
  newChannel.memberSystemId = channelInfo.memberSystemId;
}

this._channelMap.set(channelKey, Object.assign({}, newChannel));

//init socket.
this.tryReconnect(channelInfo.url);
}

}