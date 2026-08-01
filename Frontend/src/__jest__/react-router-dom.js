const path = require('path');
const rrdPkg = require.resolve('react-router-dom/package.json');
module.exports = require(path.join(path.dirname(rrdPkg), 'dist/index.js'));
