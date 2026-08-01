const path = require('path');
const rrdPkg = require.resolve('react-router-dom/package.json');
const rrPkg = require.resolve('react-router/package.json', { paths: [path.dirname(rrdPkg)] });
module.exports = require(path.join(path.dirname(rrPkg), 'dist/development/index.js'));
