var t = processBinding("HttpModule");
t.TAG = "WorkerHTTP";
var utils = require("utils");
var defHeaders = {
	"user-agent" : t.USER_AGENT,
	"X-Processor": t.TAG+" - Ajaxer Cordova"
}

exports.io = function(url){
	if(utils.getType(url) != "string")
		throw new TypeError("Url must be a string");
	if(!utils.isURL(url))
		throw new TypeError("url must be a valid url");
	return t.io(url);
}
exports.sse = exports.EventSource = function(url){
	if(utils.getType(url) != "string")
		throw new TypeError("Url must be a string");
	else if(!utils.isURL(url))
		throw new TypeError("url must be a valid url");
	var sse = t.sse(url);
	var events = new utils.Events();
	events.connect = events.open = function(fn){
		if(utils.getType(fn) == "function")
			events.once("connect",fn)
		sse.connect();
	}
	events.disconnect = events.close = function(fn){
		if(utils.getType(fn) == "function")
			events.once("disconnect",fn)
		sse.disconnect();
	}
	events.toString = function(){
		return "[Object SSE]";
	}
	events.toJSON = function(){
		return this.toString();
	}
	sse.onEvent(function(){
		events.emit.apply(events,arguments);
	})
	return events;
}
exports.get = function (url,cb,headers) {
	if(utils.getType(cb) != "function")
		throw new TypeError("Callback must be a function");
	if(!utils.isURL(url))
		throw new TypeError("url must be a valid url");
	if(utils.getType(headers) != "object")
		headers = {};
	headers = utils.assign({},defHeaders,headers);
	t.get(url,cb,headers);
}

function post(method,url,data,cb,headers){
	if(utils.getType(data) == "function"){
		cd = data;
		data = "";
	}else if(utils.getType(data) != "string" && utils.getType(data) != "object"){
		data = "";
	}else if(utils.getType(data) == "object")
		data = utils.param(data); // convert into string
	if(utils.getType(headers) != "object")
		headers = {};
	headers = utils.assign({},defHeaders,headers);
	t.post(method,url,data,"application/x-www-form-urlencoded; charset=utf-8",cb,headers);
}

var methods = "POST,DELETE,PUT,PATCH".split(',');
for(var i =methods.length; i--; )
	exports[methods[i].toLowerCase()] = post.bind(null,methods[i]);

exports.send = post;
exports.setHeader = function(name,value){
	defHeaders[name] = value;
}
exports.delHeader = function(name){
	delete defHeaders[name];
}