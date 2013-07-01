(ns leiningen.core.ssl
  (:require [clojure.java.io :as io]
            [leiningen.core.user :as user])
  (:import java.security.KeyStore
           java.security.KeyStore$TrustedCertificateEntry
           java.security.Security
           java.security.cert.CertificateFactory
           javax.net.ssl.KeyManagerFactory
           javax.net.ssl.SSLContext
           javax.net.ssl.TrustManagerFactory
           javax.net.ssl.X509TrustManager
           java.io.FileInputStream
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

(defn key-manager-props []
  (let [read #(java.lang.System/getProperty %)]
    (merge {:file (read "javax.net.ssl.keyStore")
            :type (read "javax.net.ssl.keyStoreType")
            :provider (read "javax.net.ssl.keyStoreProvider")
            :password (read "javax.net.ssl.keyStorePassword")}
           (-> (user/profiles) :user :key-manager-properties))))

(defn key-manager-factory [{:keys [file type provider password]}]
  (let [type (or type (KeyStore/getDefaultType))
        fis (if-not (empty? file) (FileInputStream. file))
        pwd (and password (.toCharArray password))
        store (if provider
                (KeyStore/getInstance type provider)
                (KeyStore/getInstance type))]
    (.load store fis pwd)
    (when fis (.close fis))
    (doto (KeyManagerFactory/getInstance
           (KeyManagerFactory/getDefaultAlgorithm))
      (.init store pwd))))

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

;; TODO: honor settings from project.clj, not just user profile
(defn make-sslcontext
  "Construct an SSLContext that trusts a collection of certificates."
  [trusted-certs]
  (let [ks (make-keystore trusted-certs)
        kmf (key-manager-factory (key-manager-props))
        tmf (trust-manager-factory ks)]
    (doto (SSLContext/getInstance "TLS")
      (.init (.getKeyManagers kmf) (.getTrustManagers tmf) nil))))

(alter-var-root #'make-sslcontext memoize)

(defn https-scheme
  "Construct a Scheme that uses a given SSLContext."
  ([context] (https-scheme context 443))
  ([context port]
     (let [factory (SSLSocketFactory. context (BrowserCompatHostnameVerifier.))]
       (Scheme. "https" port factory))))

(def register-scheme
  "Register a scheme with the HTTP Wagon for use with Aether."
  (memoize (fn [scheme]
             (-> (.getConnectionManager (HttpWagon.))
                 (.getSchemeRegistry)
                 (.register scheme)))))
