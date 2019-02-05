const proxy = require('http-proxy-middleware');

module.exports = function(app) {
  console.log(process.env.ENV_VARIABLE);
  app.use(proxy('/ws', { target: 'http://127.0.0.1:12346/ws', ws: true }));
};