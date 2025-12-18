// Filename: mbhuiyan_Activity07.js
/*
  Author: Ash Bhuiyan
  ISU Netid: mbhuiyan
  Email: mbhuiyan@iastate.edu
  Date: November 14, 2025
*/

var express = require("express");
var cors = require("cors");
var app = express();
var fs = require("fs");
var bodyParser = require("body-parser");

app.use(cors());
app.use(bodyParser.json());

const port = "8080";
const host = "localhost";

app.listen(port, () => {
  console.log("App listening at http://%s:%s", host, port);
});

// Request Method GET - read robots.json
app.get("/listRobots", (req, res) => {
  fs.readFile(__dirname + "/" + "robots.json", "utf8", (err, data) => {
    console.log(data);
    res.status(200);
    res.send(data);
  });
});

// GET "/" - respond with HTML
app.get("/", (req, res) => {
  res.status(200);
  res.send(
    "<h1 style='color:Green;background-color: black;border: 0px; '>Hello World From Node </h1>"
  );
});

// GET "/person" - respond with JavaScript object as JSON
app.get("/person", (req, res) => {
  const person = {
    name : 'alex',
    email : 'alex@mail.com',
    job : 'software dev'
  };

  console.log(person);
  res.status(200);
  res.send(person);
});
