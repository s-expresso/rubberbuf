Releases
========

# v0.3.2
* postprocess: mapify: duplicate field options into every enum, msg or svc entry
* (BREAKING) parse: parse extensions 'option' https://github.com/s-expresso/rubberbuf/issues/7
  - appended to `[:extensions ...]` as last element
  - `nil` is appended if 'option' is not set.

# v0.3.1
* editions: fix parsing `optional` keyword

# v0.3.0
* proto: support protobuf editions syntax https://github.com/s-expresso/rubberbuf/issues/6

# v0.2.2
* postprocess: preserve field ordering after mapify

# v0.2.1
* textformat: fix parsing of 0 and make top level parser handle message instead of field
* postprocess: fix mapify, missing handling of field+ and missing handling of extensions

# v0.2.0
* release: baseline