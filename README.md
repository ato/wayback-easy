# wayback-easy

This is a proof of concept maven overlay that allows OpenWayback to be
configured using a simple pywb-style YAML file.

```yaml
collections:
  testcoll: /data/example/

  coll2:
    index: /data/cdx/
    resource: /data/warcs/

  remotecoll:
    index: http://cdx.example.org/cdx/
    resource: http://warcs.example.org/warcs/

  # While YAML is not quite as flexible as Spring XML you can call setters
  # on bean-like objects and pass arguments to constructors.
  # So the same sort of mechanisms can be used to enable custom
  # implementations of some components.
  #
  # Obviously this power should only be used in non-standard cases, as like
  # Spring XML it couples the configuration to class names.
  #
  # Note that abbreviations can be defined. So these could be tagged "!locationdb"
  # instead to maintain decoupling.
  locationdb:
    index: ${TESTDIR}/cdx
    resource: !!org.archive.wayback.resourcestore.LocationDBResourceStore
      db: !!org.archive.wayback.resourcestore.locationdb.RemoteResourceFileLocationDB
      - http://example.org/location-db

surt_ordered: true
```

## Running it

Put some WARCs and CDX files somewhere.

   ```
   collections:
     example: /data/warcs-and-cdxes/
   ```

Either deploy to app container (Jetty, Tomcat) or just run:

    mvn jetty:run -Dwayback.config=config.yaml

Set the path to your config.yaml using the `wayback.config` system property,
servlet context init paramter or the environment variable `WAYBACK_CONFIG`.

Hit http://localhost:8080/example/ in your browser.

## Why?

* Simple and readable
* Approachable for non-programmers (and arguably to most programmers too!)
* Decouples the configuration format from implementation details of the application (class, property names) allowing refactoring and deprecation
* Potential for better error messages and deprecation warnings
* Pywb and (Open)Wayback could share a common configuration subset
  
## Known Issues

These are trivially fixable, I just haven't gotten around to it as this is a
proof of concept.

* No root index page.
* Error if you don't have a trailing slash on the collection URL
* Only a few things can be configured.