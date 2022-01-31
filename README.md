
# iterargs

A library that's used in [iterargs-pandoc][1].

[1]: https://github.com/mat5n/iterargs-pandoc

## Setup

Prerequisites: [Java][3] and [Leiningen][4].

To get an interactive development environment run:

    lein fig:dev

and open your browser at [localhost:9500][2].  This will auto compile
and send all changes to the browser without the need to reload.  After
the compilation process is complete, you will get a Browser Connected
REPL.  An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To create a production build run:

    lein fig:prod

[2]: http://localhost:9500/
[3]: https://openjdk.java.net
[4]: https://leiningen.org

## License

Copyright © 2022 mat5n

Distributed under the Eclipse Public License, Version 2.0.
