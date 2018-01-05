var c = processBinding("ConsoleModule");
c.TAG = "WorkerConsole";
var utils = require("utils");
exports.log = function () {
	c.log(utils.format.apply(this,arguments));
}

exports.error = function () {
	c.error(utils.format.apply(this,arguments));
}

exports.info = function () {
	c.info(utils.format.apply(this,arguments));
}

exports.warn = function () {
	c.warn(utils.format.apply(this,arguments));
}
