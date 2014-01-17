var express = require('express');
var app = express()
  , server = require('http').createServer(app)
  , io = require('socket.io').listen(server)
  , url = require('url')
  , fs = require('fs')
  , http = require('http')
  , path = require('path')
  , MongoClient = require('mongodb').MongoClient
  , db;
app.use(express.bodyParser({
    uploadDir: __dirname + '/uploads',
    keepExtensions: true
}));

server.listen(9000);

app.get('/monitor.js', function(request, response, next) {
  response.sendfile(__dirname + '/monitor.js');
}); 
app.post('/images', addImage); // endpoint to post new images
app.get('/images', getImages); 

app.use(express.static(path.join(__dirname, './uploads')));

// Create the "uploads" folder if it doesn't exist
fs.exists(__dirname + '/uploads', function (exists) {
    if (!exists) {
        console.log('Creating directory ' + __dirname + '/uploads');
        fs.mkdir(__dirname + '/uploads', function (err) {
            if (err) {
                console.log('Error creating ' + __dirname + '/uploads');
                process.exit(1);
            }
        })
    }
});

// Connect to database
var url = "mongodb://localhost:27017/PictureFeed";
MongoClient.connect(url, {native_parser: true}, function (err, connection) {
    if (err) {
        console.log("Cannot connect to database " + url);
        process.exit(1);
    }
    db = connection;
});
 
function getImages  (req, res, next){
    var images = db.collection('images');

    images.find().sort({ _id: -1 }).limit(20).toArray(function (err, data) {
        if (err) {
            console.log(err);
            return next(err);
        }
        res.json(data);
    });
};

function addImage (req, res, next){
    console.log("upload");

    var file = req.files.file,
        filePath = file.path,
//        fileName = file.name, file name passed by client. Not used here. We use the name auto-generated by Node
        lastIndex = filePath.lastIndexOf("/"),
        tmpFileName = filePath.substr(lastIndex + 1),
        image = req.body,
        images = db.collection('images');

    image.fileName = tmpFileName;
    console.log(tmpFileName);

    images.insert(image, function (err, result) {
        if (err) {
            console.log(err);
            return next(err);
        }
        res.json(image);
    });

};

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