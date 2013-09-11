(ns ring.adapter.jetty9
  "Adapter for the Jetty webserver."
  (:import (org.eclipse.jetty.server 
             Server Request HttpConfiguration ServerConnector
             HttpConnectionFactory SecureRequestCustomizer
             SslConnectionFactory ConnectionFactory)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.util.thread QueuedThreadPool)
           (org.eclipse.jetty.util.ssl SslContextFactory)
           (javax.servlet.http HttpServletRequest HttpServletResponse))
  (:require [ring.util.servlet :as servlet]))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (let [request-map  (servlet/build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (servlet/update-servlet-response response response-map)
          (.setHandled base-request true))))))

(defn- ssl-context-factory
  "Creates a new SslContextFactory instance from a map of options."
  [options]
  (let [context (SslContextFactory.)]
    (if (string? (options :keystore))
      (.setKeyStorePath context (options :keystore))
      (.setKeyStore context (options :keystore)))
    (.setKeyStorePassword context (options :key-password))
    (when (options :truststore)
      (.setTrustStore context (options :truststore)))
    (when (options :trust-password)
      (.setTrustStorePassword context (options :trust-password)))
    (case (options :client-auth)
      :need (.setNeedClientAuth context true)
      :want (.setWantClientAuth context true)
      nil)
    context))

(defn- http-connector [server options secure?]
  (let [config (doto (HttpConfiguration.)
                 (.setSendDateHeader true))]
    (when secure?
      (doto config
        (.setSecurePort (options :ssl-port 443))
        (.setSecureScheme "https")))
    (doto (ServerConnector. server (into-array [(HttpConnectionFactory. config)]))
      (.setPort (options :port 80))
      (.setName "http"))))

(defn ssl-connector [server options]
  (let [config (doto (HttpConfiguration.)
                 (.setSendDateHeader true)
                 (.setSecurePort (options :ssl-port 443))
                 (.setSecureScheme "https")
                 (.addCustomizer (SecureRequestCustomizer.)))]
    (doto (ServerConnector. server
                            (into-array ConnectionFactory [(SslConnectionFactory. (ssl-context-factory options) "http/1.1")
                                                           (HttpConnectionFactory. config)]))
      (.setPort (options :ssl-port 443))
      (.setName "https"))))


(defn- create-server
  "Construct a Jetty Server instance."
  [options]
  (let [server (doto (Server. (QueuedThreadPool. (options :max-threads 50))))
        secure? (or (options :ssl?) (options :ssl-port))
        http-conn (http-connector server options secure?)]
    (.addConnector server http-conn)
    (when secure?
      (.addConnector server (ssl-connector server options)))
    server))

(defn ^Server run-jetty
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:

  :configurator - a function called with the Jetty Server instance
  :port         - the port to listen on (defaults to 80)
  :host         - the hostname to listen on
  :join?        - blocks the thread until server ends (defaults to true)
  :ssl?         - allow connections over HTTPS
  :ssl-port     - the SSL port to listen on (defaults to 443, implies :ssl?)
  :keystore     - the keystore to use for SSL connections
  :key-password - the password to the keystore
  :truststore   - a truststore to use for SSL connections
  :trust-password - the password to the truststore
  :max-threads  - the maximum number of threads to use (default 50)
  :client-auth  - SSL client certificate authenticate, may be set to :need,
                  :want or :none (defaults to :none)"
  [handler options]
  (let [^Server s (create-server (dissoc options :configurator))]
    (doto s
      (.setHandler (proxy-handler handler)))
    (when-let [configurator (:configurator options)]
      (configurator s))
    (.start s)
    (when (:join? options true)
      (.join s))
    s))
