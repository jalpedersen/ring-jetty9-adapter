# Jetty 9 adapter for Ring

## Synoptis

Adapter for Ring using Jetty 9.

Behaves exactly like the normal jetty adapter, but uses Jetty 9 instead of 7.

For more information, consult the offical Ring site: https://github.com/ring-clojure/ring

## Example:

"Hello World" in Ring:

    (use 'ring.adapter.jetty9)

    (defn app [req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    "Hello World from Ring and Jetty 9"})

    (run-jetty app {:port 8080})

## Download and Installation
Add the following dependency to your `project.clj` and do a `lein deps`.

    [org.signaut/ring-jetty9-adapter "1.2.0"]
