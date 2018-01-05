var t = processBinding("ToastModule");
t.TAG = "WorkerNotification";
var n = processBinding("NotificationModule");
n.TAG = "WorkerNotification";
var console = require("console");
var utils = require("utils");
var closeAll = 0;
module.exports = {
	toast : function (msg) {
		t(msg);
	},
	cancelAllNotifications : function(){
		n.cancelAll();
		closeAll++;
	},
 	notification :  function(shortText,title,opts){
 		/*fn(shortText[,title][,options])
 		{
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

		}
 		*/
		if(typeof shortText == "object"){
			opts = shortText;
			title = opts.title || undefined;
			shortText = opts.text || undefined;
		}else if(typeof title == "object"){
			opts = title;
			title = opts.title || undefined;
		}
		if(utils.getType(opts) != "object" && utils.getType(opts) != "undefined")
			opts = {};
		//console.log("args",">>",arguments);
		opts = utils.assign({},{
			/* default options */
			stick : false
		},opts||{});
		// keep just the real function
		if("onclick" in opts && typeof opts.onclick != 'function')
			delete opts.onclick;
		if("oncancel" in opts && typeof opts.oncancel != 'function')
			delete opts.oncancel;
		else if("oncancel" in opts)
			opts.onclose = opts.oncancel;
		else
			delete opts.onclose;

		if(opts.actions && utils.getType(opts.actions) != "object" && utils.getType(opts.actions) != "array")
			opts.actions = undefined;
		if(utils.getType(opts.actions) == "undefined")
			opts.actions = [];
		if(utils.getType(opts.actions) == "object")
			opts.actions = [opts.actions];
		var actions = [];
		for(var i = 0; i<3 && i<opts.actions.length; i++)
			actions[i] = {
				title : opts.actions[i].title || "",
				icon : opts.actions[i].icon || "",
				onclick : opts.actions[i].onclick || function(){}
			};
		opts.actions = actions;
		opts.text = shortText || opts.text || undefined;
		opts.title = title || opts.title || undefined;
		
		if(!opts.title)
			delete opts.title;
		
		opts.autoCancel = opts.stick != false;
		delete opts.stick;
		if(opts.autoCancel){
			opts.autoClear = false;
			opts.ongoing =  true;
		} else {
			if(!("led" in opts))
				opts.led = "FFFFFF";
			opts.autoClear = true;
			opts.ongoing =  false;
		}
		
		if(!opts.led)
			delete opts.led;

		if(typeof opts.text != "string")
			throw new TypeError("the text must be a string");
		//console.log("notification",">>",opts);
		var self = this;
		//if(opts.autoCancel)
			return (function(){
				var id = n.notification(opts);
				var closed = false;
				var mCloseAll = 0+closeAll;
				var defaults = utils.assign({},opts); 
				return {
					isClosed : function(){return mCloseAll != closeAll && closed;},
					cancel : function(){
						if(mCloseAll != closeAll && closed) return;
						n.cancel(id);
						closed =  true;
					},
					update : function(shortText,title,o){
						if(mCloseAll != closeAll && closed) return;						
						if(typeof shortText == "object"){
							o = shortText;
							title = o.title || undefined;
							shortText = o.text || undefined;
						}else if(typeof title == "object"){
							o = title;
							title = o.title || undefined;
						}
						o = utils.assign({},defaults,o || {});
						o.id = id;
						o.stick = defaults.autoCancel;
						defaults = utils.assign({},defaults,o);
						//console.log("update >>",o);
						//console.log("defaults >>",defaults);
						self.notification(shortText||o.text,title||o.title,o);
					}
				};
			})();
		//else
		//	n.notification(opts);
 	}
}