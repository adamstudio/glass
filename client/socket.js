var express = require('express');
var app = express()
  , server = require('http').createServer(app)
  , io = require('socket.io').listen(server)
  , url = require('url');

server.listen(9000);

app.get('/monitor.js', function(request, response, next) {
  response.sendfile(__dirname + '/monitor.js');
});

app.get('/', function (req, res) {
  res.sendfile(__dirname + '/index.html');
});
app.get('/socket',show); 

function show(req, res){
  var url_parts = url.parse(req.url, true);
  var query = url_parts.query;
  console.log(query.url+ ' ' +query.id); 
   io.sockets.emit('news',{ url: query.url, id: query.id });    
  res.send();
}