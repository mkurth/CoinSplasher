if (process.env.NODE_ENV === "production") {
    const opt = require("./coinsplasher-web-opt.js");
    opt.entrypoint.main();
    module.exports = opt;
} else {
    var exports = window;
    exports.require = require("./coinsplasher-web-fastopt-entrypoint.js").require;
    window.global = window;

    const fastOpt = require("./coinsplasher-web-fastopt.js");
    fastOpt.entrypoint.main()
    module.exports = fastOpt;

    if (module.hot) {
        module.hot.accept();
    }
}
