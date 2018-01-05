var json = require("json");
function argumentsToString(){
    var msg = "";
    for (var i =  0; i < arguments.length; i++){
        if(typeof arguments[i] == "string" || typeof arguments[i] == "number"){
            arguments[i];
        } else if(typeof arguments[i] == "boolean"){
            arguments[i] = arguments[i] ? "true" : "false";
        }else if(typeof arguments[i] == "function"){
            arguments[i] = "<function>";
        } else if(typeof arguments[i] == "null"){
            arguments[i] = "null";
        } else if(typeof arguments[i] == "undefined"){
            arguments[i] = "undefined";
        }else{
            arguments[i] = json.stringify(arguments[i],null,4);
        }
        msg+=(msg ? " ":"") + arguments[i];
    }
    return msg;
}

exports.format = argumentsToString;


function assign (target) {

   if (target === undefined || target === null) {
    throw new TypeError('Cannot convert undefined or null to object');
  }

  var output = Object(target);
  for (var index = 0; index < arguments.length; index++) {
    var source = arguments[index];
    if (source !== undefined && source !== null) {
      for (var nextKey in source) {
        if (Object.prototype.hasOwnProperty.call(source, nextKey)) {
          output[nextKey] = source[nextKey];
        }
      }
    }
  }
  return output;
};

exports.assign = assign;

// from https://github.com/jeromeetienne/microevent.js
var MicroEvent  = function(){};
MicroEvent.prototype    = {
    on  : function(event, fct){
        this._events = this._events || {};
        this._events[event] = this._events[event]   || [];
        this._events[event].push([false,fct]);
    },
    once : function(event, fct){
        this._events = this._events || {};
        this._events[event] = this._events[event]   || [];
        this._events[event].push([true,fct]);
    },
    off : function(event, fct){
        this._events = this._events || {};
        if( event in this._events === false  )  return;
        var i;
        for(var i = 0; i < this._events[event].length; i++)
            if(this._events[event][i][1] == fct)
                this._events[event].splice(i--, 1);
    },
    trigger : function(event /* , args... */){
        this._events = this._events || {};
        try{
            if(event !== "event") // call global event listener
                this.trigger.bind(this,"event").apply(this, Array.prototype.slice.call(arguments, 1));
        }catch(e){}
        if( event in this._events === false  )  return;
        for(var i = 0; i < this._events[event].length; i++){
            this._events[event][i][1].apply(this, Array.prototype.slice.call(arguments, 1));
            if(this._events[event][i][0])
                this._events[event].splice(i--, 1);
        }
    }
};

MicroEvent.prototype.emit = MicroEvent.prototype.fire = MicroEvent.prototype.trigger;
/**
 * mixin will delegate all MicroEvent.js function in the destination object
 *
 * - require('MicroEvent').mixin(Foobar) will make Foobar able to use MicroEvent
 *
 * @param {Object} the object which will support MicroEvent
*/
MicroEvent.mixin    = function(destObject){
    var props   = ['on', 'once', 'off', 'trigger', 'fire'];
    for(var i = 0; i < props.length; i ++){
        if( typeof destObject === 'function' ){
            destObject.prototype[props[i]]  = MicroEvent.prototype[props[i]];
        }else{
            destObject[props[i]] = MicroEvent.prototype[props[i]];
        }
    }
    return destObject;
}

exports.Events = MicroEvent;


function getType(value) {
  if (value == null) return value + ''

  var _typeof = typeof value
  if (_typeof == 'number' && isNaN(value))
    return 'nan'

  var type = Object.prototype.toString.call(value).slice(8, -1)
  if (type == 'Arguments' || _typeof == 'object' &&
    typeof value.callee == 'function') {
    return 'arguments'
  }

  type = value.constructor ?
    value.constructor.name || type :
    type

  return type ? type.toLowerCase() : _typeof
}

exports.getType = getType;


function isURL(str) {
     var urlRegex = '^(?!mailto:)(?:(?:http|https|ftp|ftps|ws|wss)://)(?:\\S+(?::\\S*)?@)?(?:(?:(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[0-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))|localhost)(?::\\d{2,5})?(?:(/|\\?|#)[^\\s]*)?$';
     var url = new RegExp(urlRegex, 'i');
     return str.length < 2083 && url.test(str);
}
exports.isURL = isURL;

function param(a) {
    var s = [], rbracket = /\[\]$/,
        isArray = function (obj) {
            return Object.prototype.toString.call(obj) === '[object Array]';
        }, add = function (k, v) {
            v = typeof v === 'function' ? v() : v === null ? '' : v === undefined ? '' : v;
            s[s.length] = encodeURIComponent(k) + '=' + encodeURIComponent(v);
        }, buildParams = function (prefix, obj) {
            var i, len, key;

            if (prefix) {
                if (isArray(obj)) {
                    for (i = 0, len = obj.length; i < len; i++) {
                        if (rbracket.test(prefix)) {
                            add(prefix, obj[i]);
                        } else {
                            buildParams(prefix + '[' + (typeof obj[i] === 'object' ? i : '') + ']', obj[i]);
                        }
                    }
                } else if (obj && String(obj) === '[object Object]') {
                    for (key in obj) {
                        buildParams(prefix + '[' + key + ']', obj[key]);
                    }
                } else {
                    add(prefix, obj);
                }
            } else if (isArray(obj)) {
                for (i = 0, len = obj.length; i < len; i++) {
                    add(obj[i].name, obj[i].value);
                }
            } else {
                for (key in obj) {
                    buildParams(key, obj[key]);
                }
            }
            return s;
        };

    return buildParams('', a).join('&').replace(/%20/g, '+');
};

exports.param = param;



Object.defineProperty(exports,"UUID",{
    get : function(){
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        });
    }
});