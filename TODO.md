### Modules

[√]     app : Application tweaks
			- [√] void sms(string msg,string to) // launch a messagerie apps for send sms
			- [√] void dial(string to) // launch a telephonie apps for call
			- [√] void web(string url) // launch a web apps for browsing url
			- [√] void foreground() // set the a main apps on top (open if is closed)
			- [√] statusBar : tweaks for status bar
				- [√] void open() // expand the status bar
				- [√] void close() // close the status bar (if expended)

[√]		utils : utilities functions
			- [√] string format(Any ...) // return a string representation of object
			- [√] object assign(object target,object ...) // polyfill of Object.assign
			- [√] Events() // Event emitter library which provides the observer pattern to object (https://github.com/jeromeetienne/microevent.js)
			- [√] string getType(Any value) // return the type of value
			- [√] bool isURL(string str) // test if str is an url
			- [√] string param(object data) // return a querystring of data
			- [√] string UUID // return an unique UUIDv4

[√]    	console : post message in logcat
            - [√] void log(Any ...)
            - [√] void info(Any ...)
            - [√] void error(Any ...)
            - [√] void warn(Any ...)

[√]		json : A JSON implementation that deals with multiple / circular references (https://github.com/graniteds/jsonr/blob/master/jsonr.js)
			- [√] string stringify(Any value[, function replacer[, string space]])
			- [√] any parse(string json[, function reviver])

[√]		notification : display notification
			- [√] void toast(string msg) : show a toast message
			- [√] int notification({
					title : String, : title of notification
					text  : String, : short text
					body  : String, : long text
					icon  : String, : path to png icon
					smallIcon  : String, : path to png icon
					sound  : String, : path to mp3 sound
					stick  : booloan, stick the notification
					badge : Number, 
					led : String, Hex ARGB value that you would like the LED on the device to blink
					onclick : function fn(),
					oncancel : function fn(),
					actions : [{
						icon : String,
						label : String,
						onclick : function(){}

					}...]// max elements is
	    		  }) : display a notification

[-]    	http : http client
            - [√] void get(string url,function cd(err,res)[, object header])
            - [√] void post(string url,object data,function cb(err,res)[, object header])
            - [√] void post(string url,string data,function cb(err,res)[, object header])
            - [√] sse  sse(string url) // server sent event
				- [√] void off(string evt,function cb) // remove register
				- [√] void on(string evt,function cb) // 
				- [√] void once(string evt,function cb) //
				- [√] void disconnect(function cb) // cb : executed after disconnecttion
				- [√] void connect(function cb) // cb : executed after connection
			- [-] io   io(string url) // socket.io 1.x client
				- [ ] void off(string evt,function cb) //
				- [√] void on(string evt,function cb) //
				- [√] void once(string evt,function cb) //
				- [√] void emit(string evt,Any... data,[function ack]) //
				- [√] void connect(function cb) // cb : executed after connection
				- [√] void disconnect(function cb) // cb : executed after disconnecttion
		    - [ ] ws : web socket client

[√]    	socket : raw socket TCP/UDP client
		- [√] TCP : Connecxion to TCP Server
			- [√] : Socket 	TCP(int port, string host) // port and host of the server
			- [√] : void 	start(function fnForConnection) // start the connection
			- [√] : void 	stop() // stop the connection
			- [√] : void 	send() // send message to the server
			- [√] : function 	onMessage // function to handle message to the server
			- [√] : function 	onError // function to handle message to the server


[-]    	db : persistent storage api
		- [ ] sqlite : sqlite DB
		- [ ] localstorage : serialized storage api //may be use android.content.SharedPreferences
		- [√] PicoDB : Persistant storage api with mongolike query (based on https://github.com/jclo/picodb - verion 0.8.5)
		- [√] ArrayDB : Persistant storage api based on javascript Array
			- [√] ArrayDB   ArrayDB([string name[, string nameOfIDKey]]) // constructor
            - [√] int       length // item count in length (read only)
			- [√] int       limit // size limit of database
			- [√] Element[] allDocs // items in length
			- [√] Element[] find(function filter) // return all matching element
			- [√] Element   findDoc(function filter) // return the first matching element, return undefiend if not found
			- [√] Element   findLastDoc(function filter) // return the last matching element
			- [√] Element   addDoc(object doc[, boolean forceNew]) // add and return the Element, return undefiend if not add
			- [√] void      addDocs(object[] docs) // add bluk docs
			- [√] Element[] remove(function filter) // remove and return all matching element
			- [√] Element   removeDoc(string id) // remove and return the matching element, return undefined if not found
			- [√] Element   removeDoc(number id) // remove and return thematching element, return undefined if not found
			- [√] void      save() // if name is defined save database to file system
			- [√] void      load(string json) // decode and load json data into the database
			- [√] void      load(object data) // load arbitrary data into the database
            - [√] void      load(array data) // add data into the database
			- [√] void      clear() // clear all data into the database
			- [√] Element : database Element
				- [√] Element set(string key, Any value) // update or add new property and save into database
				- [√] Element save() // save all modification on database
				- [√] Element clone() // clone element on database and return the new Element
				- [√] Element remove() // remove element on database 
			        +-------------------------------+
                 .  | The accessors can be used     |
                /!\ | for add and update object,    |
                    | for save you need call save() |
                    +-------------------------------+

### Functions

[√]    	Timer : setTimeout, setInterval(), setImmediate
				clearTimeout,clearInterval,clearImmediate

[√]		Any require(string moduleNameOrJsRelativePathName) // import a module name or a js file (the path must be relative to the current file)
		** Usage **
		var console = require('console'); // load the module console
		/!\ IMPORTANT /!\
		Circular require() return undefined

### Cordova Bridge

[√]    	onMessage : fn(Any message) // call when onMessage is called
		 	** Usage **
    		onMessage = function(msg){
    			switch(msg.cmd || 'ping'){
    				case 'add':
    					postMessage((msg.a||0) + (msg.b||0)); // make a + b and send to cordova
    					break;
    			}
    		}

[√]    	postMessage(Any message) // call for send a message to cordova
    		** Usage **
    		postMessage("today is "+(new Date()));

### Network Monitor

[√]    	online : fn(type) // call when online is detectect
		 	** Usage **
    		online = function(type){
    			notification.toast("Network Status : "+type)
    		}

[√]    	offline() // call for send a message to cordova
    		** Usage **
    		offline = function(){
    			notification.toast("Network Status : offline")
    		}