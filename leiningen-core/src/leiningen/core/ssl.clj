(ns leiningen.core.ssl
  (:require [clojure.java.io :as io])
  (:import java.security.KeyStore
           java.security.KeyStore$TrustedCertificateEntry
           java.security.cert.CertificateFactory
           javax.net.ssl.SSLContext
           javax.net.ssl.TrustManagerFactory
           javax.net.ssl.X509TrustManager
           org.apache.http.conn.ssl.SSLSocketFactory
           org.apache.http.conn.scheme.Scheme
           org.apache.maven.wagon.providers.http.HttpWagon
           org.apache.http.conn.ssl.BrowserCompatHostnameVerifier))

(defn ^TrustManagerFactory trust-manager-factory [^KeyStore keystore]
  (doto (TrustManagerFactory/getInstance "PKIX")
    (.init keystore)))

(defn default-trust-managers []
  (let [tmf (trust-manager-factory nil)
        tms (.getTrustManagers tmf)]
    (filter #(instance? X509TrustManager %) tms)))

(defn default-trusted-certs
  "Lists the CA certificates trusted by the JVM."
  []
  (mapcat #(.getAcceptedIssuers %) (default-trust-managers)))

(defn read-certs
  "Read one or more X.509 certificates in DER or PEM format."
  [f]
  (let [cf (CertificateFactory/getInstance "X.509")
        in (io/input-stream (or (io/resource f) (io/file f)))]
    (.generateCertificates cf in)))

(defn make-keystore
  "Construct a KeyStore that trusts a collection of certificates."
  [certs]
  (let [ks (KeyStore/getInstance "jks")]
    (.load ks nil nil)
    (doseq [[i cert] (map vector (range) certs)]
      (.setEntry ks (str i) (KeyStore$TrustedCertificateEntry. cert) nil))
    ks))

(defn make-sslcontext
  "Construct an SSLContext that trusts a collection of certificatess."
  [trusted-certs]
  (let [ks (make-keystore trusted-certs)
        tmf (trust-manager-factory ks)]
   (doto (SSLContext/getInstance "TLS")
     (.init nil (.getTrustManagers tmf) nil))))

(defn https-scheme
  "Construct a Scheme that uses a given SSLContext."
  ([context] (https-scheme context 443))
  ([context port]
     (let [factory (SSLSocketFactory. context (BrowserCompatHostnameVerifier.))]
       (Scheme. "https" port factory))))

(defn register-scheme
  "Register a scheme with the HTTP Wagon for use with Aether."
  [scheme]
  (-> (.getConnectionManager (HttpWagon.))
      (.getSchemeRegistry)
      (.register scheme)))
