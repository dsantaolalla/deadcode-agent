#!/usr/bin/python

import httplib, urllib
import json

host = "localhost"
port = 9200

if __name__ == "__main__":
    connection = httplib.HTTPConnection("localhost", 9200)

    scanned_classes = {}

    size = 100
    offset = 0
    while True:
        connection.request("GET", "/_search?q=eventValue%3AclassScanned&size=" + str(size) + "&from=" + str(offset))

        response = connection.getresponse()
        j = json.loads(response.read())

        for entry in j["hits"]["hits"]:
            scanned_class = entry["_source"]["mdc"]["className"]
            scanned_classes[scanned_class] = 1
            print "Scanned class:", scanned_class

        total = j["hits"]["total"]
        if offset >= total: break

        offset += size

    print "Scanned classes:", len(scanned_classes)

    offset = 0
    loaded_count = 0
    while True:
        connection.request("GET", "/_search?q=eventValue%3AclassLoaded&size=" + str(size) + "&from=" + str(offset))

        response = connection.getresponse()
        j = json.loads(response.read())

        for entry in j["hits"]["hits"]:
            loaded_count += 1
            loaded_class = entry["_source"]["mdc"]["className"]
            print "Removing loaded class:", loaded_class
            scanned_classes.pop(loaded_class, None)

        total = j["hits"]["total"]
        if offset >= total: break

        offset += size

    print "Loaded classes:", loaded_count

    # what remains in scanned_classes now is unused
    for unused_class in scanned_classes:
        print "Unused class:", unused_class

    print "Unused classes:", len(scanned_classes)


