var module = processBinding("SocketModule"),
	emptyfn = function(){};
var TCP = function (port,host) {
	if(typeof host == "number" && typeof port == "string"){
		var tmp = port;
		port = host;
		host = tmp;
	}
	if(!port){
		throw new Error("port is missing");
	}
	host = host || "127.0.0.1";
	port = parseInt(port);
	if(isNaN(port))
		throw new Error("port must be number");
	this.socket = module.socket(port,host);
}
TCP.prototype.send = function(msg) {
	if(this.isConnected())
		this.socket.sendMessage(msg);
	else
		throw new Error("Socket Closed")
};
TCP.prototype.isConnected = function() {
	return !this.socket.isClosed();
} ;
TCP.prototype.onError = function() {} ;
TCP.prototype.onMessage = function() {} ;
TCP.prototype.start = function() {
	fn = typeof fn != "function" ? this.startFn : fn;
	if(typeof fn != "function")
		fn = emptyfn;
	this.socket.run(fn,typeof this.onMessage != "function" ? emptyfn : this.onMessage,typeof this.onError != "function" ? emptyfn : this.onError);
};
TCP.prototype.stop = function(){
	this.socket.stop();
}

exports.TCP = TCP;