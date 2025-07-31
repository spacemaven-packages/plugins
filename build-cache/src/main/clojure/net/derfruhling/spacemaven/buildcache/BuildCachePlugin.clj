(ns net.derfruhling.spacemaven.buildcache.BuildCachePlugin
  (:import (org.gradle.api.provider ProviderFactory)
           (org.gradle.caching.http HttpBuildCache)
           (org.gradle.api Action)
           (org.slf4j Logger LoggerFactory)
           (org.gradle.api.initialization Settings)))

(gen-class :name net.derfruhling.spacemaven.buildcache.BuildCachePlugin
           :implements [org.gradle.api.Plugin]
           :constructors {^{javax.inject.Inject {}}
                          [org.gradle.api.provider.ProviderFactory] []}
           :init init
           :state state)

(defn -init [^ProviderFactory factory]
  [[] (atom {:factory factory
             :logger (LoggerFactory/getLogger "net.derfruhling.spacemaven.buildcache.BuildCachePlugin")})])

(defn get-field [this key]
  (@(.state this) key))

(defn -apply [this ^Settings settings]
  (do
    (let [factory (cast ProviderFactory (get-field this :factory))
          ^String domain (.getOrNull (.gradleProperty factory "spacemaven.build-cache.domain"))]
      (if (nil? domain)
        ; If domain is not defined:
        ;   This is done using a property instead of an extension due to the
        ;   limitations of settings plugins used like this. The extension is
        ;   not processed by the time the plugin is being applied, and the
        ;   `settings` object passed does not provide the ability to defer
        ;   configuration.
        ;
        ;   There is probably a better way to do this.
        (let [^Logger logger (get-field this :logger)]
          (throw (IllegalStateException/new "You have configured this project to use the Gradle build cache provided by spacemaven, but have not set the property 'spacemaven.build-cache.domain'. Please set this to the groupId of your project, or some other group you have access to use build caches with.")))
        ; Otherwise:
        (-> settings
            (.getBuildCache)
            (.remote HttpBuildCache
                     (reify Action
                       (execute [_ buildCache]
                         (let [buildCache (cast HttpBuildCache buildCache)
                               username (.getOrNull (.gradleProperty factory "spacemaven.user"))
                               password (.getOrNull (.gradleProperty factory "spacemaven.key"))]
                           ; `domain` is a string with a format similar to the
                           ; "group" of a gradle project.
                           (.setUrl buildCache (str "https://spacemaven.derfruhling.net/build-cache/" (.replace domain "." "/")))
                           (if (and (some? username) (some? password))
                             ; Final check for credentials
                             (let [creds (.getCredentials buildCache)]
                               (.setUsername creds username)
                               (.setPassword creds password)
                               (.setPush buildCache true))
                             ; If user has not defined credentials for publishing
                             ; to the build cache, the build cache will still be
                             ; configured, but will not be published to.
                             (let [logger (get-field this :logger)]
                               (.warn logger "No credentials were provided to allow pushing to build cache, disabling push")
                               (.setPush buildCache false)))))))))))
  )
