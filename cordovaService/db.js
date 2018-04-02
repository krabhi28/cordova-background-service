var file = processBinding("FileModule");
var utils = require("utils");
var console = require("console");
var JSONX = require("json");


var Element = function(idKey,id,el,db){
  Object.defineProperty(this,idKey,{
    value :  id,
    writable : false,
    enumerable: true
  });
  if(!(idKey in el))
    el[idKey] = id;
  if(utils.getType(el[idKey]) != "string" && utils.getType(el[idKey]) != "number") throw new TypeError("Id must me a string or a number");
  for(var i in el)
    if(i != idKey && el.hasOwnProperty(i) && utils.getType(el[i]) != "function")
      this[i] = el[i];

  Object.defineProperty(this,"set",{
    enumerable: false,
    configurable: false,
    writable: false,
    value : function(name,value){
      this[name] = value;
      this.save();
      return this;
    }
  })

  Object.defineProperty(this,"save",{
    enumerable: false,
    configurable: false,
    writable: false,
    value : function(){
      return db.addDoc(this);
    }
  });

  Object.defineProperty(this,"clone",{
    enumerable: false,
    configurable: false,
    writable: false,
    value : function(){
      return db.addDoc(this,true);
    }
  });

  Object.defineProperty(this,"remove",{
    enumerable: false,
    configurable: false,
    writable: false,
    value : function(){
      return db.removeDoc(this[idKey]);
    }
  });
};

var ArrayDB = function (name,idKey) {
  if(!(this instanceof ArrayDB)) return new ArrayDB(name,idKey);
  file.TAG = "ArrayDB";
  idKey = idKey || "id";
  var data = {};
  var length = 0;
  var limit = limit || 0;
  var timeout = null;
  if(name)
    name = ("database/" + name + ".db").toLowerCase();
  Object.defineProperty(this,"length",{
    enumerable: true,
    get : function(){
      return length
    }
  })
  Object.defineProperty(this,"limit",{
    enumerable: true,
    get : function(){
      return limit
    },
    set : function(v){
      if(utils.getType(v) == "number"){
        limit = v > 0 ? v : 0;
      }
      while(limit > 0 && length > limit)
          this.removeDoc(Object.keys(data)[0]); 
    }
  })
  this.clear = function() {
    data = {};
    length = 0;
    this.save();
  };
  this.save = function() {
    if(!name) return;
    clearTimeout(timeout);
    timeout = setTimeout(function() {
      try{
        file.write(name,JSON.stringify(data));
      }catch(e){
        console.log("Error on write",name,e,e.stack);
      }
    }, 200);
  };
  this.load = function(json) {
    clearTimeout(timeout);
    if(utils.getType(json) == "string"){
      try{
        json = JSONX.parse(json);
      }catch(e){
        return;
      }
    }
    var length = this.length;
    if(utils.getType(json) == "array")
      json.map((function(el){
        this.addDoc(el);
      }).bind(this));
    else if(utils.getType(json) == "object")
      for(var i in json)
        if(json.hasOwnProperty(i))
          this.addDoc(json[i]);
    if(length == this.length)
      clearTimeout(timeout);
  };
  Object.defineProperty(this,"allDocs",{
    enumerable: true,
    get : function(){

      return Object.keys(data).map(function(k){
        return data[k];
      })
    }
  });

  this.find = function(fn){
    if((utils.getType(fn) == "string" || utils.getType(fn) == "number") && fn in data)
      return [data[fn]];
    else if((utils.getType(fn) == "string" || utils.getType(fn) == "number"))
      return [];
    return Object.keys(data).map(function(k){
      return data[k];
    }).filter(fn);
  }
  this.remove = function(fn){
    if((utils.getType(fn) == "string" || utils.getType(fn) == "number") && fn in data)
      return [this.removeDoc(data[fn])];
    else if((utils.getType(fn) == "string" || utils.getType(fn) == "number"))
      return [];
    Object.keys(data).map(function(k){
      return data[k];
    }).filter((function(el){
      if(fn(el)){
        this.removeDoc(el);
        return true;
      }
      return false;
    }).bind(this));
  }
  this.removeDoc = function(el) {
    if(utils.getType(el) == "object"){
      el = el.id || undefined;
    };
    if(utils.getType(el) != "string" && utils.getType(el) != "number")
      return undefined;
    if(!(el in data))
      return undefined;
    var id = ""+el;
    el = data[el];
    data[id] = undefined;
    length--;
    delete data[id];
    this.save();
    return el;
  };
  this.findDoc = function(fn){
    if((utils.getType(fn) == "string" || utils.getType(fn) == "number") && fn in data)
      return data[fn];
    else if((utils.getType(fn) == "string" || utils.getType(fn) == "number"))
      return undefined;
    var ret;
    Object.keys(data).map(function(k){
      return data[k];
    }).some(function(el){
      if(fn(el)){
        ret = el;
        return true;
      } else
        return false;
    });
    return ret;
  }

  this.findLastDoc = function(fn){
    if((utils.getType(fn) == "string" || utils.getType(fn) == "number") && fn in data)
      return data[fn];
    else if((utils.getType(fn) == "string" || utils.getType(fn) == "number"))
      return undefined;
    var ret;
    Object.keys(data).reverse().map(function(k){
      return data[k];
    }).some(function(el){
      if(fn(el)){
        ret = el;
        return true;
      } else
        return false;
    });
    return ret;
  }

  this.addDocs = function() {
    for(var i = 0; i<arguments.length;i++){
      if(utils.getType(arguments[i]) != "object") continue; 
      var id = arguments[i].id || utils.UUID;
      try{
        this.addDoc(utils.assign({},{id:id},arguments[i]));
      }catch(e){}
    }
    this.save();
  };
  this.addDoc = function(el,forceNew) {
    if(utils.getType(el) != "object") return false; 
      var id = el.id || utils.UUID; 
      id = forceNew ? utils.UUID : id;
      try{    
        var nouv = !(id in data);   
        data[id] = new Element(idKey,id,el,this);
        if(nouv)
          length++;
        while(limit > 0 && length > limit)
          this.removeDoc(Object.keys(data)[0]);
      }catch(e){
        return undefined;
      }
      this.save();
      return data[id];
  };
  if(name){
    try{
      this.load(file.read(name));
    }catch(e){
      console.log("Error on read",name,e,e.stack);
    }
  }
}

exports.ArrayDB = ArrayDB;
/**
 * PicoDB v0.8.5
 *
 * @license
 * PicoDB is a tiny in-memory database that stores JSON documents.
 * Copyright (c) 2017 jclo <jclo@mobilabs.fr> (http://www.mobilabs.fr/).
 * Released under the MIT license. You may obtain a copy of the License
 * at: http://www.opensource.org/licenses/mit-license.php).
 */
var PicoDB = (function () {
    'use strict';
    var PicoDB = function () {};
    PicoDB.VERSION = '0.8.5';
    var _ = {
        isUndefined: function (obj) {
            return obj === undefined;
        },
        isNull: function (obj) {
            return obj === null;
        },
        isBoolean: function (obj) {
            return obj === true || obj === false || Object.prototype.toString.call(obj) === '[object Boolean]';
        },
        isString: function (obj) {
            return Object.prototype.toString.call(obj) === '[object String]';
        },
        isNumber: function (obj) {
            return Object.prototype.toString.call(obj) === '[object Number]';
        },
        isNaN: function (obj) {
            return _.isNumber(obj) && obj !== +obj;
        },
        isOdd: function (obj) {
            var n = obj % 2;
            return obj === parseFloat(obj) ? !!n : void 0;
        },
        isObject: function (obj) {
            var type = typeof obj;
            return (type === 'function' || type === 'object') && !!obj;
        },
        isFunction: function (obj) {
            return Object.prototype.toString.call(obj) === '[object Function]';
        },
        isArray: Array.isArray || function (obj) {
            return Object.prototype.toString.call(obj) === '[object Array]';
        },
        isMath: function (obj) {
            return Object.prototype.toString.call(obj) === '[object Math]';
        },
        isDate: function (obj) {
            return Object.prototype.toString.call(obj) === '[object Date]';
        },
        isEmpty: function (obj) {
            var key;
            if (obj === null) return true;
            if (_.isArray(obj) || _.isString(obj)) return obj.length === 0;
            for (key in obj)
                if (obj.hasOwnProperty(key)) return false;
            return true;
        },
        clone: function (obj) {
            var clone = _.isArray(obj) ? [] : {},
                prop;
            if (!_.isObject(obj)) return void 0;

            for (prop in obj) {
                if (_.isArray(obj[prop])) {
                    clone[prop] = _.clone(obj[prop]);
                } else if (_.isObject(obj[prop])) {
                    clone[prop] = _.extend(obj[prop]);
                } else {
                    clone[prop] = obj[prop];
                }
            }
            return clone;
        },
        extend: function (obj) {
            var source, prop, i;
            if (!_.isObject(obj)) return obj;
            for (i = 1; i < arguments.length; i++) {
                source = arguments[i];
                for (prop in source) {
                    if (!_.isArray(arguments[i][prop]) && _.isObject(arguments[i][prop])) {
                        obj[prop] = obj[prop] !== undefined ? obj[prop] : {};
                        _.extend(obj[prop], arguments[i][prop]);
                    } else if (hasOwnProperty.call(source, prop)) {
                        obj[prop] = _.isArray(source[prop]) ? _.clone(source[prop]) : source[prop];
                    }
                }
            }
            return obj;
        },
        keys: function (obj) {
            return Object.keys(obj);
        },
        forPropIn: function (obj, callback) {
            _.keys(obj).forEach(function (key) {
                if ({}.hasOwnProperty.call(obj, key)) {
                    callback(key);
                }
            });
        },
        contains: function (list, value) {
            return list.indexOf(value) !== -1;
        },
        flatten: function (obj, shallow) {
            var o = [],
                idx = 0,
                i;

            if (!_.isArray(obj)) return void 0;
            if (shallow) return [].concat.apply([], obj);

            for (i = 0; i < obj.length; i++) {
                if (_.isArray(obj[i])) {
                    o = o.concat(_.flatten(obj[i]));
                    idx = o.length;
                } else {
                    o[idx++] = obj[i];
                }
            }
            return o;
        },
        max: function (obj) {
            var max = null,
                o, i;
            if (!_.isArray(obj)) return void 0;
            o = _.flatten(obj);
            for (i = 0; i < o.length; i++) {
                if (max === null || max < o[i]) {
                    max = typeof o[i] === 'number' ? o[i] : max;
                }
            }
            return max !== null ? max : void 0;
        },
        min: function (obj) {
            var min = null,
                o, i;
            if (!_.isArray(obj)) return void 0;
            o = _.flatten(obj);
            for (i = 0; i < o.length; i++) {
                if (min === null || min > o[i]) {
                    min = typeof o[i] === 'number' ? o[i] : min;
                }
            }
            return min !== null ? min : void 0;
        },
        share: function (array) {
            var result = [],
                item, i, j;
            for (i = 0; i < array.length; i++) {
                item = array[i];
                if (!_.contains(result, item)) {
                    for (j = 1; j < arguments.length; j++) {
                        if (!_.contains(arguments[j], item)) {
                            break;
                        }
                    }
                    if (j === arguments.length) {
                        result.push(item);
                    }
                }
            }
            return result;
        },

        token: function () {
            return Math.random().toString(36).substr(2);
        },
        makeid: function (l) {
            return utils.UUID;
            var ll = _.isNumber(l) ? l : 16,
                c = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz',
                id = '',
                i;
            for (i = 0; i < ll; i++) 
                id += c.charAt(Math.floor(Math.random() * c.length));
            return id;
        },
        nop: function () {}
    };
    var _event = {
        _isValidEvent: function (event, type, listener) {
            if ({}.hasOwnProperty.call(event, type) && typeof listener === 'function')
                return true;
            return false;
        },
        setEventListenerList: function () {
            return {
                insert: {
                    listeners: [],
                    listenersOnce: []
                },
                update: {
                    listeners: [],
                    listenersOnce: []
                },
                delete: {
                    listeners: [],
                    listenersOnce: []
                },
                change: {
                    listeners: [],
                    listenersOnce: []
                }
            };
        },
        fire: function (eventList, event, payload) {
            var i;
            if (!eventList[event])
                return;
            for (i = 0; i < eventList[event].listeners.length; i++)
                eventList[event].listeners[i](payload);
            for (i = 0; i < eventList[event].listenersOnce.length; i++)
                eventList[event].listenersOnce[i](payload);
            eventList[event].listenersOnce.splice(0, eventList[event].listenersOnce.length);
        },
        addEventListener: function (eventList, type, listener) {
            if (!_event._isValidEvent(eventList, type, listener))
                return;
            eventList[type].listeners.push(listener);
        },
        addOneTimeEventListener: function (eventList, type, listener) {
            if (!_event._isValidEvent(eventList, type, listener))
                return;
            eventList[type].listenersOnce.push(listener);
        },
        removeEventListener: function (eventList, type, listener) {
            var index;
            if (!_event._isValidEvent(eventList, type, listener))
                return;
            index = eventList[type].listeners.indexOf(listener);
            if (index >= 0)
                eventList[type].listeners.splice(index, 1);
        }
    };
    _event.on = _event.addEventListener;
    _event.one = _event.addOneTimeEventListener;
    _event.off = _event.removeEventListener;
    var _geo = {
        _lawOfHaversines: function (obj, source) {
            var λ1 = source.coordinates[0],
                λ2 = obj.coordinates[0],
                Δλ = (λ2 - λ1) * (Math.PI / 180),
                φ1 = source.coordinates[1] * (Math.PI / 180),
                φ2 = obj.coordinates[1] * (Math.PI / 180),
                Δφ = (φ2 - φ1),
                R = 6371e3,
                a, c;
            a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
            c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        },
        _lawOfCosines: function (obj, source) {
            var λ1 = source.coordinates[0],
                λ2 = obj.coordinates[0],
                Δλ = (λ2 - λ1) * (Math.PI / 180),
                φ1 = source.coordinates[1] * (Math.PI / 180),
                φ2 = obj.coordinates[1] * (Math.PI / 180),
                R = 6371e3;
            return Math.acos(Math.sin(φ1) * Math.sin(φ2) + Math.cos(φ1) * Math.cos(φ2) * Math.cos(Δλ)) * R;
        },
        _equirectangularProjection: function (obj, source) {
            var λ1 = source.coordinates[0] * (Math.PI / 180),
                λ2 = obj.coordinates[0] * (Math.PI / 180),
                φ1 = source.coordinates[1] * (Math.PI / 180),
                φ2 = obj.coordinates[1] * (Math.PI / 180),
                R = 6371e3,
                x, y;
            x = (λ2 - λ1) * Math.cos((φ1 + φ2) / 2);
            y = φ2 - φ1;
            return Math.sqrt(x * x + y * y) * R;
        },
        _getDistanceOnEarth: function (obj, source) {
            return _geo._lawOfCosines(obj, source);
        },
        _isPointInPolygon2: function (point, polygon) {
            var intersections, vertex1, vertex2, xinter, i;
            for (i = 0; i < polygon.length; i++) {
                if (point[0] === polygon[i][0] && point[1] === polygon[i][1])
                    return 'vertex';
            }
            intersections = 0;
            for (i = 1; i < polygon.length; i++) {
                vertex1 = polygon[i - 1];
                vertex2 = polygon[i];
                if (vertex1[1] === vertex2[1] && vertex1[1] === point[1] &&
                    point[0] > Math.min(vertex1[0], vertex2[0]) &&
                    point[0] < Math.max(vertex1[0], vertex2[0])) {
                    return 'boundary';
                }
                if (point[1] > Math.min(vertex1[1], vertex2[1]) &&
                    point[1] <= Math.max(vertex1[1], vertex2[1]) &&
                    point[0] <= Math.max(vertex1[0], vertex2[0]) &&
                    vertex1[1] !== vertex2[1]) {
                    xinter = (point[1] - vertex1[1]) * (vertex2[0] - vertex1[0]) / (vertex2[1] - vertex1[1]) + (vertex1[0]);
                    if (xinter === point[0])
                        return 'boundary';
                    if (vertex1[0] === vertex2[0] || point[0] <= xinter)
                        intersections += 1;
                }
            }
            return intersections % 2 !== 0 ? 'inside' : 'outside';
        },
        _isPointInPolygon: function (point, polygon) {
            var cn = 0,
                vt, i;
            for (i = 0; i < polygon.length - 1; i++) {
                if (((polygon[i][1] <= point[1]) && (polygon[i + 1][1] > point[1])) ||
                    ((polygon[i][1] > point[1]) && (polygon[i + 1][1] <= point[1]))) {
                    vt = (point[1] - polygon[i][1]) / (polygon[i + 1][1] - polygon[i][1]);
                    if (point[0] < polygon[i][0] + vt * (polygon[i + 1][0] - polygon[i][0])) {
                        cn++;
                    }
                }
            }
            return cn % 2 !== 0 ? true : false;
        },
        _isGeometryInsideGeoObject: function (obj, source) {
            var breakloop, i, j, k, l;
            switch (source.type) {
                case 'MultiPolygon':
                    for (i = 0; i < obj.length; i++) {
                        for (j = 0; j < obj[i].length; j++) {
                            for (k = 0; k < source.coordinates.length; k++) {
                                breakloop = false;
                                for (l = 0; l < source.coordinates[k].length; l++) {
                                    if (_geo._isPointInPolygon(obj[i][j], source.coordinates[k][l])) {
                                        breakloop = true;
                                        break;
                                    }
                                }
                                if (breakloop)
                                    break;
                            }
                            if (!breakloop)
                                return false;
                        }
                    }
                    return true;
                case 'Polygon':
                    for (i = 0; i < obj.length; i++) {
                        for (j = 0; j < obj[i].length; j++) {
                            breakloop = false;
                            for (k = 0; k < source.coordinates.length; k++) {
                                if (_geo._isPointInPolygon(obj[i][j], source.coordinates[k])) {
                                    breakloop = true;
                                    break;
                                }
                            }
                            if (!breakloop)
                                return false;
                        }
                    }
                    return true;
                default:
                    throw new Error('_geo._within: the GeoSpatial $geoWihin operator with a $geometry.type "' + source.type + '" is unknown!');
            }
        },
        _toPolygonCoordinates: function (obj) {
            switch (obj.type) {
                case 'Point':
                    return [
                        [obj.coordinates]
                    ];
                case 'LineString':
                    return [obj.coordinates];
                case 'Polygon':
                    return obj.coordinates;
                case 'MultiPoint':
                    return [obj.coordinates];
                case 'MultiLineString':
                    return obj.coordinates;
                default:
                    throw new Error('_geo._toPolygonCoordinates: the GeoJSON type "' + obj.type + '" is not supported!');
            }
        },
        _box: function (obj, box) {
            var c, p;
            c = [
                [
                    [box[0][0], box[0][1]],
                    [box[1][0], box[0][1]],
                    [box[1][0], box[1][1]],
                    [box[0][0], box[1][1]]
                ]
            ];
            p = {
                type: 'Polygon',
                coordinates: c
            };
            return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), p);
        },
        _polygon: function (obj, polygon) {
            var p;
            p = {
                type: 'Polygon',
                coordinates: [polygon, [polygon[0][0], polygon[0][1]]]
            };
            return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), p);
        },
        _center: function (obj, center) {
            var d;
            d = Math.sqrt(Math.pow((center[0][0] - obj.coordinates[0]), 2) + Math.pow((center[0][1] - obj.coordinates[1]), 2));
            return d < center[1] ? true : false;
        },
        _centerSphere: function (obj, centerSphere) {
            var d;
            d = Math.sqrt(Math.pow((centerSphere[0][0] - obj.coordinates[0]), 2) + Math.pow((centerSphere[0][1] - obj.coordinates[1]), 2));
            return d < (centerSphere[1] / Math.PI * 180) ? true : false;
        },
        _within: function (obj, source) {
            switch (obj.type) {
                case 'Point':
                    return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), source);
                case 'LineString':
                    return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), source);
                case 'Polygon':
                    return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), source);
                case 'MultiPoint':
                    return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), source);
                case 'MultiLineString':
                    return _geo._isGeometryInsideGeoObject(_geo._toPolygonCoordinates(obj), source);
                case 'MultiPolygon':
                    return false;
                case 'GeometryCollection':
                    return false;
                default:
                    return false;
            }
        },
        _interLineString: function (obj, source) {
            var inside, outside, breakloop, i, j;
            for (i = 0; i < obj.coordinates.length; i++) {
                for (j = 0; j < source.coordinates.length; j++) {
                    breakloop = false;
                    if (_geo._isPointInPolygon(obj.coordinates[i], source.coordinates[j])) {
                        inside = true;
                        breakloop = true;
                        if (inside && outside)
                            return true;
                        break;
                    }
                }
                if (!breakloop) {
                    outside = true;
                    if (inside && outside)
                        return true;
                }
            }
            return false;
        },
        _interPolygon: function (obj, source) {
            var inside, outside, breakloop, i, j, k;
            for (i = 0; i < obj.coordinates.length; i++) {
                for (j = 0; j < obj.coordinates[i].length; j++) {
                    for (k = 0; k < source.coordinates.length; k++) {
                        breakloop = false;
                        if (_geo._isPointInPolygon(obj.coordinates[i][j], source.coordinates[k])) {
                            inside = true;
                            breakloop = true;
                            if (inside && outside)
                                return true;
                            break;
                        }
                    }
                    if (!breakloop) {
                        outside = true;
                        if (inside && outside)
                            return true;
                    }
                }
            }
            return false;
        },
        _intersects: function (obj, source) {
            switch (obj.type) {
                case 'Point':
                    return false;
                case 'LineString':
                    return _geo._interLineString(obj, source);
                case 'Polygon':
                    return _geo._interPolygon(obj, source);
                case 'MultiPoint':
                    return false;
                case 'MultiLineString':
                    return false;
                case 'MultiPolygon':
                    return false;
                case 'GeometryCollection':
                    return false;
                default:
                    return false;
            }
        },
        _isPointNear: function (obj, source, max, min) {
            var d;
            if (max === undefined && min === undefined)
                return true;
            if (max < min)
                return false;
            d = _geo._getDistanceOnEarth(obj, source);
            return (!min || d >= min ? true : false) && (!max || d <= max ? true : false) ? true : false;
        },
        _geoNear: function (obj, source, max, min) {
            switch (obj.type) {
                case 'Point':
                    return _geo._isPointNear(obj, source, max, min);
                default:
                    return false;
            }
        },
        _geoWithin: function (obj, source) {
            var op = _.keys(source)[0];
            if (!_.isObject(source))
                return false;
            switch (op) {
                case '$geometry':
                    return _geo._within(obj, source.$geometry);
                case '$box':
                    return _geo._box(obj, source.$box);
                case '$polygon':
                    return _geo._polygon(obj, source.$polygon);
                case '$center':
                    return _geo._center(obj, source.$center);
                case '$centerSphere':
                    return _geo._centerSphere(obj, source.$centerSphere);
                default:
                    throw new Error('_geo._geoWithin: the GeoSpatial $geoWihin operator "' + op + '" is unknown!');
            }
        },
        _geoIntersects: function (obj, source) {
            if (!{}.hasOwnProperty.call(source, '$geometry'))
                return false;
            switch (source.$geometry.type) {
                case 'Polygon':
                    return _geo._intersects(obj, source.$geometry);
                default:
                    throw new Error('_geo._geoIntersects: the GeoSpatial $geoIntersects type "' + source.$geometry.type + '" is not supported!');
            }
        },
        _near: function (obj, source) {
            if (!{}.hasOwnProperty.call(source, '$geometry'))
                return false;
            switch (source.$geometry.type) {
                case 'Point':
                    return _geo._geoNear(obj, source.$geometry, source.$maxDistance, source.$minDistance);
                default:
                    return false;
            }
        },
        _query: function (obj, source) {
            var status;
            _.forPropIn(source, function (prop) {
                switch (prop) {
                    case '$geoWithin':
                        status = _geo._geoWithin(obj, source[prop]);
                        break;
                    case '$geoIntersects':
                        status = _geo._geoIntersects(obj, source[prop]);
                        break;
                    case '$near':
                        status = _geo._near(obj, source[prop]);
                        break;
                    case '$nearSphere':
                        status = false;
                        break;
                    default:
                        throw new Error('_geo._query: the Geo Operator "' + prop + '" is unknown!');
                }
            });
            return status;
        },
        query: function (obj, source) {
            return _geo._query(obj, source);
        }
    };
    var _project = {
        _include: function (obj, source, data) {
            _.forPropIn(source, function (prop) {
                if (obj[prop]) {
                    if (_.isObject(source[prop])) {
                        data[prop] = {};
                        _project._include(obj[prop], source[prop], data[prop]);
                    } else if (source[prop] === 1) {
                        if (_.isObject(obj[prop])) {
                            data[prop] = _.clone(obj[prop]);
                        } else if (obj[prop]) {
                            data[prop] = obj[prop];
                        }
                    }
                }
            });
            return data;
        },
        _exclude: function (obj, source, data) {
            _.forPropIn(obj, function (prop) {
                if (source[prop] !== undefined) {
                    if (_.isObject(source[prop])) {
                        data[prop] = {};
                        _project._exclude(obj[prop], source[prop], data[prop]);
                    }
                } else if (_.isObject(obj[prop])) {
                    data[prop] = _.clone(obj[prop]);
                } else {
                    data[prop] = obj[prop];
                }
            });
            return data;
        },
        setProjection: function (projection, type) {
            if (!type)
                return projection;
            if (projection['_id'] !== undefined)
                return projection;
            return _.extend({
                _id: 1
            }, projection);
        },
        isProjectionTypeInclude: function (projection) {
            var prop;
            for (prop in projection) {
                if (_.isObject(projection[prop])) {
                    if (_project.isProjectionTypeInclude(projection[prop])) {
                        return true;
                    }
                } else if (projection[prop]) {
                    return true;
                }
            }
            return false;
        },
        add: function (doc, data, projection) {
            if (_.isEmpty(projection.value))
                doc.push(_.clone(data));
            else if (projection.type)
                doc.push(_project._include(data, projection.value, {}));
            else
                doc.push(_project._exclude(data, projection.value, {}));
        }
    };
    var _query = {};
    _query = {
        _isHavingNotOperator: function (query) {
            var op = ['$ne', '$nin', '$not'],
                qar, not, re, x, i, j;
            not = [];
            if (_.contains(_.keys(query), '$or')) {
                qar = query['$or'];
                for (i = 0; i < qar.length; i++) {
                    _.forPropIn(qar[i], function (key) {
                        for (j = 0; j < op.length; j++) {
                            re = new RegExp('"\\' + op[j] + '":');
                            x = JSON.stringify(qar[i]).match(re);
                            if (x) {
                                not.push(key);
                            }
                        }
                    });
                }
            } else {
                _.forPropIn(query, function (key) {
                    for (j = 0; j < op.length; j++) {
                        re = new RegExp('"\\' + op[j] + '":');
                        x = JSON.stringify(query[key]).match(re);
                        if (x) {
                            not.push(key);
                        }
                    }
                });
            }
            return not.length !== 0 ? not : false;
        },
        _isHavingOrOperator: function (query) {
            return (!query['$or'] || !_.isArray(query['$or'])) ? false : query['$or'];
        },
        _isHavingSpecialOperator: function (query) {
            return {
                not: _query._isHavingNotOperator(query),
                or: _query._isHavingOrOperator(query)
            };
        },
        _isConditionTrue: function (obj, source, op) {
            switch (op) {
                case '$eq':
                    return obj === source;
                case '$gt':
                    return obj > source;
                case '$gte':
                    return obj >= source;
                case '$lt':
                    return obj < source;
                case '$lte':
                    return obj <= source;
                case '$ne':
                    return obj !== source;
                case '$in':
                    return _.isArray(obj) ?
                        !_.isEmpty(_.share(source, obj)) :
                        _.contains(source, obj);
                case '$nin':
                    return _.isArray(obj) ?
                        _.isEmpty(_.share(source, obj)) :
                        !_.contains(source, obj);
                case '$not':
                    return !_query._areConditionsTrue(obj, source);
                case '$exists':
                    return source;
                case '$geoWithin':
                    return _geo.query(obj, {
                        $geoWithin: source
                    });
                case '$geoIntersects':
                    return _geo.query(obj, {
                        $geoIntersects: source
                    });
                case '$near':
                    return _geo.query(obj, {
                        $near: source
                    });
                case '$nearSphere':
                    return _geo.query(obj, {
                        $nearSphere: source
                    });
                default:
                    throw new Error('_query._isConditionTrue: the operator "' + op + '" is unknown!');
            }
        },
        _areConditionsTrue: function (obj, source) {
            var prop;
            if (!_.isArray(source) && !_.isObject(source)) {
                if (obj === source)
                    return true;
                return false;
            }
            for (prop in source) {
                if (!_query._isConditionTrue(obj, source[prop], prop))
                    return false;
            }
            return true;
        },
        _query: function (obj, source, op) {
            var level = 0,
                rootKey;
            function parse(obj, source) {
                var prop;
                for (prop in source) {
                    if (level === 0) {
                        rootKey = prop;
                    }
                    if (!obj[prop]) {
                        if (!op.not || !_.contains(op.not, rootKey)) {
                            return false;
                        } else if (op.or) {
                            return true;
                        }
                        continue;
                    }
                    if (_.isObject(source[prop]) && !_.keys(source[prop])[0].match(/^\$/)) {
                        level += 1;
                        if (!parse(obj[prop], source[prop])) {
                            level -= 1;
                            if (!op.or) {
                                return false;
                            }
                        } else if (op.or) {
                            return true;
                        }
                    } else if (!_query._areConditionsTrue(obj[prop], source[prop])) {
                        if (!op.or) {
                            return false;
                        }
                    } else if (op.or) {
                        return true;
                    }
                }
                return !op.or;
            }
            return parse(obj, source);
        },
        isHavingSpecialOperator: function (query) {
            return _query._isHavingSpecialOperator(query);
        },
        isMatch: function (doc, query, sop) {
            var i;
            if (!sop.or)
                return _query._query(doc, query, sop);
            for (i = 0; i < query['$or'].length; i++)
                if (_query._query(doc, query['$or'][i], sop))
                    return true;
            return false;
        }
    };
    var _count = {
        _count: function (db, query, options, callback) {
            query = query || {};
            var sop = _query.isHavingSpecialOperator(query),
                count, i;
            if (!_.isObject(query) || _.isArray(query) || _.isFunction(query)) {
                if (callback)
                    callback('query is not a valid object!', 0);
                else
                    throw new Error('query is not a valid object!');
                return;
            }
            count = 0;
            for (i = 0; i < db.data.length; i++)
                if (_query.isMatch(db.data[i], query, sop)) {
                    count += 1;
                }
            if (callback)
                callback(null, count);
            else
              return count;
        },
        count: function (_this, query, options, callback) {
            var db = _this.db;
            if (callback && !_.isFunction(callback)) {
                callback = undefined;
            } else if (!callback && !_.isFunction(options)) {
                callback = undefined;
            } else if (!callback && _.isFunction(options)) {
                callback = options;
                options = {};
            }
            if (_.isArray(options) || !_.isObject(options))
                options = {};
            return _count._count(db, query, options, callback);
        }
    };
    var _delete = {
        _delete: function (db, eventQ, filter, options, callback) {
            var sop = _query.isHavingSpecialOperator(filter),
                removed, dblength, docOut, i;
            if (!_.isObject(filter) || _.isArray(filter)) {
                if (callback)
                    callback(null, null);
                return;
            }
            if (_.isEmpty(filter)) {
                docOut = [];
                if (!options.many) {
                    removed = 1;
                    docOut = db.data.splice(0, 1);
                } else {
                    removed = db.data.length;
                    docOut = _.clone(db.data);
                    db.data.length = 0;
                }
                _event.fire(eventQ, 'change', docOut);
                _event.fire(eventQ, 'delete', docOut);
                if (callback)
                    callback(null, removed);
                else
                    return removed;
            }
            removed = 0;
            docOut = [];
            dblength = db.data.length;
            for (i = 0; i < dblength; i++) {
                if (_query.isMatch(db.data[i], filter, sop)) {
                    docOut.push(db.data.splice(i, 1));
                    removed += 1;
                    i -= 1;
                    dblength -= 1;
                    if (!options.many)
                        break;
                }
            }
            _event.fire(eventQ, 'change', docOut);
            _event.fire(eventQ, 'delete', docOut);
            if (callback)
                callback(null, removed);
            else
                return removed;
        },
        delete: function (_this, many, filter, options, callback) {
            var db = _this.db,
                eventQ = _this.eventQ;
            if (callback && !_.isFunction(callback)) {
                callback = undefined;
            } else if (!callback && !_.isFunction(options)) {
                callback = undefined;
            } else if (!callback && _.isFunction(options)) {
                callback = options;
                options = {};
            }
            if (_.isArray(options) || !_.isObject(options))
                options = {};
            options.many = many;
            return _delete._delete(db, eventQ, filter, options, callback);
        }
    };
    var _find = {
        _initCursor: function () {
            return {
                query: {},
                projection: {
                    type: null,
                    value: null
                }
            };
        },
        _process: function (dbO, callback) {
            var query = dbO.cursor.query,
                projection = dbO.cursor.projection,
                db = dbO.db,
                sop = _query.isHavingSpecialOperator(query),
                docs, i;
            if (_.isArray(query) || _.isFunction(query) || !_.isObject(query)) {
                if (callback)
                    callback('This query isn\'t a valid Cursor query object');
                else
                    throw new Error('This query isn\'t a valid Cursor query object');
                return;
            }
            docs = [];
            for (i = 0; i < db.data.length; i++)
                if (_query.isMatch(db.data[i], query, sop))
                    _project.add(docs, db.data[i], projection);
            if (callback)
                callback(null, docs);
            else
                return docs;
        },
        find: function (_this, query, projection) {
            if (!_this.cursor)
                _this.cursor = _find._initCursor();
            _this.cursor.query = !_.isArray(query) && _.isObject(query) ?
                query : {};
            if (!_.isArray(projection) && _.isObject(projection)) {
                _this.cursor.projection.type = _project.isProjectionTypeInclude(projection);
                _this.cursor.projection.value = _project.setProjection(projection, _this.cursor.projection.type);
            } else {
                _this.cursor.projection.value = {};
            }
        },
        toArray: function (_this, callback) {
            if (!_.isFunction(callback) && callback !== undefined)
                return;
            return _find._process(_this, callback);
        }
    };
    var _insert = {
        _insert: function (db, eventQ, docs, options, callback) {
            var arr, docOut, id, i;
            arr = [];
            if (!_.isArray(docs) && _.isObject(docs))
                arr.push(docs);
            else
                arr = docs;
            docOut = [];
            for (i = 0; i < arr.length; i++)
                if (_.isObject(arr[i]))
                    if (arr[i]._id) {
                        if (_insert._isNewId(db, arr[i]._id)) {
                            db.data.push(_.extend({}, arr[i]));
                            docOut.push(_.extend({}, arr[i]));
                            if (!options.many)
                                break;
                        }
                    } else {
                        id = _.token();
                        db.data.push(_.extend({
                            _id: id
                        }, arr[i]));
                        docOut.push(_.extend({
                            _id: id
                        }, arr[i]));
                        if (!options.many)
                            break;
                    }
            _event.fire(eventQ, 'change', docOut);
            _event.fire(eventQ, 'insert', docOut);
            if (callback)
                callback(null, docOut);
            else
                return docOut;
        },
        _schema: function () {
            return {
                data: []
            };
        },
        insert: function (_this, many, docs, options, callback) {
            var db = _this.db,
                eventQ = _this.eventQ;
            if (callback && !_.isFunction(callback)) {
                callback = undefined;
            } else if (!callback && !_.isFunction(options)) {
                callback = undefined;
            } else if (!callback && _.isFunction(options)) {
                callback = options;
                options = {};
            }
            if (_.isArray(options) || !_.isObject(options))
                options = {};
            options.many = many;
            if (_.isArray(docs) || _.isObject(docs))
                return _insert._insert(db, eventQ, docs, options, callback);
        },
        _isNewId: function (db, id) {
            return !db.data.some(function (x) {
                return x.id == id
            });
        },
    };
    var _update = {
        _pull: function (obj, source) {
            var prop, key, match, index, op, arr, i;
            for (prop in source) {
                if (_.isObject(source[prop]) && !_.isArray(obj[prop])) {
                    if (obj[prop]) {
                        _update._pull(obj[prop], source[prop]);
                    }
                } else if (hasOwnProperty.call(source, prop)) {
                    if (!obj[prop] || !_.isArray(obj[prop]))
                        continue;
                    if (typeof source[prop] === 'boolean' || typeof source[prop] === 'number' || typeof source[prop] === 'string') {
                        index = obj[prop].indexOf(source[prop]);
                        if (index > -1)
                            obj[prop].splice(index, 1);
                        continue;
                    }
                    if (_.isObject(source[prop]) && !_.keys(source[prop])[0].match(/^\$/)) {
                        for (i = obj[prop].length - 1; i >= 0; i--) {
                            if (!_.isObject(obj[prop][i]))
                                break;
                            match = true;
                            for (key in source[prop]) {
                                if (obj[prop][i][key] !== source[prop][key]) {
                                    match = false;
                                    break;
                                }
                            }
                            if (match)
                                obj[prop].splice(i, 1);
                        }
                        continue;
                    }
                    if (_.isObject(source[prop])) {
                        op = _.keys(source[prop])[0];
                        switch (op) {
                            case '$eq':
                                index = obj[prop].indexOf(source[prop]['$eq']);
                                if (index > -1)
                                    obj[prop].splice(index, 1);
                                break;
                            case '$gt':
                                for (i = obj[prop].length - 1; i >= 0; i--)
                                    if (obj[prop][i] > source[prop]['$gt'])
                                        obj[prop].splice(i, 1);
                                break;
                            case '$gte':
                                for (i = obj[prop].length - 1; i >= 0; i--)
                                    if (obj[prop][i] >= source[prop]['$gte'])
                                        obj[prop].splice(i, 1);
                                break;
                            case '$lt':
                                for (i = obj[prop].length - 1; i >= 0; i--)
                                    if (obj[prop][i] < source[prop]['$lt'])
                                        obj[prop].splice(i, 1);
                                break;
                            case '$lte':
                                for (i = obj[prop].length - 1; i >= 0; i--)
                                    if (obj[prop][i] <= source[prop]['$lte'])
                                        obj[prop].splice(i, 1);
                                break;
                            case '$ne':
                                for (i = obj[prop].length - 1; i >= 0; i--)
                                    if (obj[prop][i] !== source[prop]['$ne'])
                                        obj[prop].splice(i, 1);
                                break;
                            case '$in':
                                if (!_.isArray(source[prop]['$in']))
                                    break;
                                for (i = 0; i < source[prop]['$in'].length; i++) {
                                    index = obj[prop].indexOf(source[prop]['$in'][i]);
                                    if (index > -1)
                                        obj[prop].splice(index, 1);
                                }
                                break;
                            case '$nin':
                                if (!_.isArray(source[prop]['$nin']))
                                    break;
                                arr = [];
                                for (i = 0; i < source[prop]['$nin'].length; i++) {
                                    index = obj[prop].indexOf(source[prop]['$nin'][i]);
                                    if (index > -1)
                                        arr.push(source[prop]['$nin'][i]);
                                }
                                obj[prop] = _.clone(arr);
                                break;
                            default:
                                throw new Error('_update._pull: the operator "' + op + '" is not supported!');
                        }
                        continue;
                    }
                }
            }
            return obj;
        },
        _push: function (obj, source) {
            var prop, subprop, position, slice, i;
            for (prop in source) {
                if (!{}.hasOwnProperty.call(source, prop))
                    continue;
                subprop = _.keys(source[prop]);
                if (!_.isArray(source[prop]) && _.isObject(source[prop]) && !subprop[0].match(/^\$/)) {
                    if (!obj[prop])
                        obj[prop] = {};
                    _update._push(obj[prop], source[prop]);
                } else if (hasOwnProperty.call(source, prop)) {
                    if (!obj[prop])
                        obj[prop] = [];
                    if (typeof source[prop] === 'boolean' || typeof source[prop] === 'number' || typeof source[prop] === 'string') {
                        obj[prop].push(source[prop]);
                        continue;
                    }
                    if (_.isArray(source[prop])) {
                        obj[prop].push(_.clone(source[prop]));
                        continue;
                    }
                    if (_.isObject(source[prop]) && _.isArray(source[prop]['$each'])) {
                        position = source[prop]['$position'];
                        if (position === undefined || typeof position !== 'number' || position < 0)
                            position = obj[prop].length;
                        slice = source[prop]['$slice'];
                        if (slice === undefined || typeof position !== 'number')
                            slice = null;
                        for (i = source[prop]['$each'].length - 1; i >= 0; i--)
                            obj[prop].splice(position, 0, source[prop]['$each'][i]);
                        if (slice > 0)
                            obj[prop].splice(slice, obj[prop].length - slice);
                        else if (slice === 0)
                            obj[prop].length = 0;
                        else if (slice < 0)
                            obj[prop].splice(0, obj[prop].length + slice);
                    }
                }
            }
            return obj;
        },
        _apply: function (obj, source, op) {
            var prop, i, j;
            for (prop in source) {
                if (!_.isArray(source[prop]) && _.isObject(source[prop])) {
                    if (!obj[prop] && (op === '$rename' || op === '$unset' || op === '$pop'))
                        break;
                    else if (!obj[prop])
                        obj[prop] = {};
                    _update._apply(obj[prop], source[prop], op);
                } else if (hasOwnProperty.call(source, prop)) {
                    switch (op) {
                        case '$inc':
                            if (typeof obj[prop] === 'number')
                                obj[prop] += source[prop];
                            else
                                obj[prop] = source[prop];
                            break;
                        case '$mul':
                            if (typeof obj[prop] === 'number')
                                obj[prop] *= source[prop];
                            else
                                obj[prop] = 0;
                            break;
                        case '$rename':
                            if (obj[prop]) {
                                obj[source[prop]] = obj[prop];
                                delete obj[prop];
                            }
                            break;
                        case '$set':
                            if (_.isArray(source[prop]))
                                obj[prop] = _.clone(source[prop]);
                            else
                                obj[prop] = source[prop];
                            break;
                        case '$unset':
                            if (obj[prop])
                                delete obj[prop];
                            break;
                        case '$min':
                            if (!obj[prop] || (typeof obj[prop] === 'number' && source[prop] < obj[prop]))
                                obj[prop] = source[prop];
                            break;
                        case '$max':
                            if (!obj[prop] || (typeof obj[prop] === 'number' && source[prop] > obj[prop]))
                                obj[prop] = source[prop];
                            break;
                        case '$pop':
                            if (_.isArray(obj[prop]))
                                if (source[prop] === 1)
                                    obj[prop].pop();
                                else if (source[prop] === -1)
                                obj[prop].shift();
                            break;
                        case '$pullAll':
                            if (_.isArray(obj[prop]) && _.isArray(source[prop]))
                                for (i = 0; i < source[prop].length; i++)
                                    for (j = obj[prop].length - 1; j >= 0; j--)
                                        if (obj[prop][j] === source[prop][i])
                                            obj[prop].splice(j, 1);
                            break;
                        default:
                            throw new Error('_update._apply: the operator "' + op + '" is unknown!');
                    }
                }
            }
            return obj;
        },
        _replace: function (obj, source) {
            var keys = _.keys(obj),
                i;
            for (i = 0; i < keys.length; i++) {
                if (keys[i] !== '_id') {
                    delete obj[keys[i]];
                }
            }
            return _.extend(obj, source);
        },
        _applyTime: function (obj, source) {
            var prop, subprop;
            for (prop in source) {
                if ({}.hasOwnProperty.call(source, prop)) {
                    subprop = _.keys(source[prop])[0];
                    if (_.isObject(source[prop]) && subprop !== '$type') {
                        if (!obj[prop])
                            obj[prop] = {};
                        _update._applyTime(obj[prop], source[prop]);
                    } else if (hasOwnProperty.call(source, prop)) {
                        if (source[prop][subprop] === 'timestamp')
                            obj[prop] = Date.now();
                        else
                            obj[prop] = new Date().toISOString();
                    }
                }
            }
            return obj;
        },
        _updateThisDoc: function (doc, update) {
            var keys = _.keys(update);
            if (!keys[0].match(/^\$/))
                return _update._replace(doc, update);
            switch (keys[0]) {
                case '$inc':
                    return _update._apply(doc, update['$inc'], '$inc');
                case '$mul':
                    return _update._apply(doc, update['$mul'], '$mul');
                case '$rename':
                    return _update._apply(doc, update['$rename'], '$rename');
                case '$set':
                    return _update._apply(doc, update['$set'], '$set');
                case '$unset':
                    return _update._apply(doc, update['$unset'], '$unset');
                case '$min':
                    return _update._apply(doc, update['$min'], '$min');
                case '$max':
                    return _update._apply(doc, update['$max'], '$max');
                case '$currentDate':
                    return _update._applyTime(doc, update['$currentDate']);
                case '$pop':
                    return _update._apply(doc, update['$pop'], '$pop');
                case '$pullAll':
                    return _update._apply(doc, update['$pullAll'], '$pullAll');
                case '$pull':
                    return _update._pull(doc, update['$pull'], '$pull');
                case '$push':
                    return _update._push(doc, update['$push'], '$push');
                default:
                    throw new Error('The Update Operator "' + keys[0] + '" isn\'t supported!');
            }
        },
        _update: function (db, eventQ, query, update, options, callback) {
            var sop = _query.isHavingSpecialOperator(query),
                docOut, i;
            if (_.isArray(query) || _.isFunction(query) || !_.isObject(query)) {
                if (callback)
                    callback('filter is not a valid object!');
                else
                    throw new Error('filter is not a valid object!');
                return;
            }
            if (_.isArray(update) || _.isFunction(update) || !_.isObject(update)) {
                if (callback)
                    callback('update is not a valid object!');
                else
                    throw new Error('update is not a valid object!');
                return;
            }
            docOut = [];
            for (i = 0; i < db.data.length; i++)
                if (_query.isMatch(db.data[i], query, sop)) {
                    _update._updateThisDoc(db.data[i], update);
                    docOut.push(_.extend({}, db.data[i]));
                    if (!options.many)
                        break;
                }
            _event.fire(eventQ, 'change', docOut);
            _event.fire(eventQ, 'update', docOut);
            if (callback)
                callback(null, docOut);
            else
                return docOut;
        },
        update: function (_this, many, query, update, options, callback) {
            var db = _this.db,
                eventQ = _this.eventQ;
            if (callback && !_.isFunction(callback)) {
                callback = undefined;
            } else if (!callback && !_.isFunction(options)) {
                callback = undefined;
            } else if (!callback && _.isFunction(options)) {
                callback = options;
                options = {};
            }
            if (_.isArray(options) || !_.isObject(options))
                options = {};
            options.many = many;
            return _update._update(db, eventQ, query, update, options, callback);
        }
    };
    var _init = function (_this) {
        _this.db = _insert._schema();
        _this.eventQ = _event.setEventListenerList();
    };
    PicoDB.Create = function () {
        this.db;
        var buildCallback = (function(fn){
          return typeof fn == "function" ? (function(){
            setTimeout(fn.apply(this.db, Array.prototype.slice.call(arguments, 1)),1)
          }).bind(this) : undefined;
        }).bind(this);
        var _export = function () {
            if (!this.db)
              _init(this);
            return this.db.data;
        };
        var _import = function (data) {
            if (!this.db)
              _init(this);
            this.db.data = data;
        };
        var count = function (query, options, callback) {
            if (!this.db || arguments.length < 1)
                return;
            return _count.count(this, query||{}, options||{}, buildCallback(callback));
        };
        var deleteMany = function (filter, options, callback) {
            if (!this.db || arguments.length < 1)
                return;
            return _delete.delete(this, true, filter, options, buildCallback(callback));
        };
        var deleteOne = function (filter, options, callback) {
            if (!this.db || arguments.length < 1)
                return;
            return _delete.delete(this, false, filter, options, buildCallback(callback));
        };
        var insertMany = function (docs, options, callback) {
            if (!this.db)
                _init(this);
            if (arguments.length < 1)
                return;
            return _insert.insert(this, true, docs, options, buildCallback(callback));
        };
        var insertOne = function (doc, options, callback) {
            if (!this.db)
                _init(this);
            if (arguments.length < 1)
                return;
            return _insert.insert(this, false, doc, options, buildCallback(callback));
        };
        var updateMany = function (query, update, options, callback) {
            if (!this.db || arguments.length < 2)
                return;
            return _update.update(this, true, query, update, options, buildCallback(callback));
        };
        var updateOne = function (query, update, options, callback) {
            if (!this.db || arguments.length < 2)
                return;
            return _update.update(this, false, query, update, options, buildCallback(callback));
        };
        var find = function (query, projection, callback) {
            query = query || {};
            projection = projection || {};
            if (!this.db)
                return this;
            _find.find(this, query, projection);
            if(callback = buildCallback(callback))
              _find.toArray(this,buildCallback(callback));
            else
              return _find.toArray(this);
        };
        var addEventListener = function (type, listener) {
            if (!this.db)
                _init(this);
            _event.addEventListener(this.eventQ, type, listener);
        };
        var addOneTimeEventListener = function (type, listener) {
            if (!this.db)
                _init(this);
            _event.addOneTimeEventListener(this.eventQ, type, listener);
        };
        var removeEventListener = function (type, listener) {
            if (!this.db)
                _init(this);
            _event.removeEventListener(this.eventQ, type, listener);
        };
        _init(this);
        return {
            import: _import,
            export: _export,
            count: count,
            deleteMany: deleteMany,
            deleteOne: deleteOne,
            insertMany: insertMany,
            insertOne: insertOne,
            updateMany: updateMany,
            updateOne: updateOne,
            find: find,
            addEventListener: addEventListener,
            addOneTimeEventListener: addOneTimeEventListener,
            removeEventListener: removeEventListener,
            on: addEventListener,
            one: addOneTimeEventListener,
            off: removeEventListener
        };
    };
    return PicoDB;
})();
exports.PicoDB = function (name) {
    if (!(this instanceof exports.PicoDB)) return new exports.PicoDB(name);
    file.TAG = "PicoDB";
    var db = PicoDB.Create();
    if (name) {
        var timeout;
        name = ( name + ".txt").toLowerCase();
        try{
          db.import(JSONX.parse( file.read(name)));
        }catch(e){
          db.data = []
          console.error("Error on read", name, e, e.stack);
        }
        db.save = function () {
            clearTimeout(timeout);
            timeout = setTimeout(function () {
                try {
                    file.write(name,JSON.stringify(db.export()));
                } catch (e) {
                    console.error("Error on write", name, e, e.stack);
                }
            }, 1);
        }
        db.on('change', db.save);
    }
    return db;
}
