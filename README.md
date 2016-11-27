# wayback-easy

This is a proof of concept maven overlay that allows OpenWayback to be
configured using a simple pywb-style YAML file.

```yaml
collections:
  testcoll: /data/cdx

  coll2:
    index: /data/cdx
    resource: /data/warcs

  remotecoll:
    index: http://cdx.example.org/cdx
    resource: http://warcs.example.org/warcs

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

## Why?

* More readable and simpler
* Far, far more approachable for non-programmers (and arguably to most programmers too!)
* Decouples the configuration format from implementation details of the application (class, property names) allowing refactoring and deprecation
* Potential for better error messages and deprecation warnings
* Pywb and owb could share a configuration subset, allowing easy testing with both  