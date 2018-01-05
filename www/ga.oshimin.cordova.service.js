/*global cordova, module*/
module.exports = {
	/**
	  * Send Message to service
	  *
	  * @param successCallback The callback which will be called if the method is successful
	  * @param failureCallback The callback which will be called if the method encounters an error
	  */
    postMessage: function (message, successCallback, errorCallback) {
      	cordova.exec(
        	successCallback,
        	errorCallback,
        	"cordovaService",
        	"postMessage",
        	[message]
        );
    },
    /**
	  * Registers for handle messages
	  *
	  * @param successCallback The callback which will be called if the method is successful
	  * @param failureCallback The callback which will be called if the method encounters an error
	  */
    onMessage: function (successCallback) {
        cordova.exec(
        	successCallback,
        	function(){},
        	"cordovaService",
        	"onMessage",
        	[]
        );
    },

    /**
	  * start service
	  *
	  * @param successCallback The callback which will be called if the method is successful
	  * @param failureCallback The callback which will be called if the method encounters an error
	  */
    start: function (successCallback, errorCallback) {
        cordova.exec(
        	successCallback,
        	errorCallback,
        	"cordovaService",
        	"start",
        	[]
        );
    },
    /**
	  * stop service
	  *
	  * @param successCallback The callback which will be called if the method is successful
	  * @param failureCallback The callback which will be called if the method encounters an error
	  */
    stop: function (successCallback, errorCallback) {
        cordova.exec(
        	successCallback,
        	errorCallback,
        	"cordovaService",
        	"stop",
        	[]
        );
    },


    /**
	  * check if service is started
	  *
	  * @param successCallback The callback which will be called if the method is successful
	  * @param failureCallback The callback which will be called if the method encounters an error
	  */
    isStart: function (successCallback, errorCallback) {
        cordova.exec(
        	successCallback,
        	errorCallback,
        	"cordovaService",
        	"isStart",
        	[]
        );
    }
};

module.exports.isStart(function(e){
	console.log(e ? "started" : "fails");
},function(e){
	console.log("Error fn");
}); // check if module is started (module must auto start)

