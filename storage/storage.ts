 export class Storage {
    static newInstance(): Storage {
      return new Storage();
    }
    
    constructor() {
    
    }

    _authentication: any = null;

    setAuthentication(authentication): void {
        this._authentication = Object.assign({}, authentication);
    }
    
    createDir(dir): any {
        console.log('file is ok, start to upload it...');
        
        var xhr = new XMLHttpRequest();
        xhr.withCredentials = true;
        let requestUrl = this._authentication.httpAddress + '/client/create_dir?'
            + 'parentId=' + this._authentication.parentId
            + '&systemId=' + this._authentication.systemId 
            + '&ticket=' + this._authentication.ticket 
            + '&dir=' + dir;
        xhr.open('GET', requestUrl);
        xhr.timeout = 1000 * 16;
        xhr.ontimeout = (event) => {
            console.log('send message timeout, event->', event);
        };
        xhr.onload = () => {
            // do something to response
            if (!xhr.responseText) {
                console.log('upload file return nothing...');
                 return;
            }
    
            const responseMessage = JSON.parse(xhr.responseText);
            if (0 != responseMessage.result) {
                console.log('create dir failed...response->', responseMessage);
                return ;
            }
            
        };
        xhr.send();
    }

    upload(path, file): Promise<number> {
        return new Promise<number>((resolve) => {
            //check file.
            if (!file) {
                console.log('file is null...');

                resolve(-1);

                return ;
            }

            console.log('file is ok, start to upload it...');
            var data = new FormData();
            data.append('parentId', this._authentication.parentId);
            data.append('systemId', this._authentication.systemId);
            data.append('ticket', this._authentication.ticket);
            data.append('path', path);
            data.append('file', file);
            data.append('fileSize', file.size);
            data.append('fileName', file.name);

            var xhr = new XMLHttpRequest();
            xhr.withCredentials = true;
            let sendingUrl = this._authentication.httpAddress + '/client/upload';
            xhr.open('POST', sendingUrl, true);
            xhr.timeout = 1000 * 16;
            xhr.ontimeout = (event) => {
                console.log('send message timeout, event->', event);

                resolve(-2);
            };
            xhr.onload = () => {
                // do something to response
                if (!xhr.responseText) {
                    console.log('upload file return nothing...');
                    resolve(-3);

                    return;
                }

                const responseMessage = JSON.parse(xhr.responseText);
                if (0 != responseMessage.result) {
                    console.log('upload file failed...response->', responseMessage);
                    resolve(-4);

                    return ;
                }
                
                console.log('upload file ok...');

                resolve(0);
            };
            xhr.onerror = (error) => {
                console.log('error->', error);

                resolve(-5);
            };

            xhr.send(data);
        });
    }

    getDonwloadPath(path): any {
        return this._authentication.httpAddress + '/client/download?'
            + 'parentId=' + this._authentication.parentId
            + '&systemId=' + this._authentication.systemId 
            + '&ticket=' + this._authentication.ticket
            + '&path=' + path;
    }

    shareFile(file): void {

    }

    getDownloadShareFilePath(path): any {

    }
 }
