var t = processBinding("AppModule");
t.TAG = "WorkerApp";
var statusBarSlots = {
	IME : "ime",
	SYNC_FAILING : "sync_failing",
	SYNC_ACTIVE : "sync_active",
	GPS : "gps",
	BLUETOOTH : "bluetooth",
	NFC : "nfc",
	TTY : "tty",
	SPEAKERPHONE : "speakerphone",
	MUTE : "mute",
	VOLUME : "volume",
	WIFI : "wifi",
	CDMA_ERI : "cdma_eri",
	DATA_CONNECTION : "data_connection",
	PHONE_EVDO_SIGNAL : "phone_evdo_signal",
	PHONE_SIGNAL : "phone_signal",
	BATTERY : "battery",
	ALARM_CLOCK : "alarm_clock",
	SECURE : "secure",
	CLOCK : "clock"
}

exports.statusBar = {
	slots : statusBarSlots,
	open : function () {
		t.openStatusBar();
	},
	close : function () {
		t.closeStatusBar();
	},
	addIcon : function(icon,slot){
		t.addStatusBarIcon(icon.toString(),slot ? slot.toString() : statusBarSlots.IME);
	}
}

exports.sms =function(msg, to){
	if(typeof to == "undefined" && msg)
		t.sendSMS(msg.toString());
	else if(to && msg)
		t.sendSMS(msg.toString(), encodeURIComponent(to.toString()));
	else throw new TypeError("Bad arguments");
}

exports.dial =function(to){
	if(to)
		t.dial(encodeURIComponent(to.toString()));
	else throw new TypeError("Bad arguments");
}

exports.web =function(to){
	if(to)
		t.web(to.toString());
	else throw new TypeError("Bad arguments");
}

exports.foreground =function(){
		t.openApp();
}