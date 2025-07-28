(ns net.derfruhling.spacemaven.tools.ToolsPlugin
  (:gen-class
    :name net.derfruhling.spacemaven.tools.ToolsPlugin
    :implements [org.gradle.api.Plugin]
    )
  )

(import (org.gradle.api Project Action)
        (org.gradle.api.artifacts ArtifactSelectionDetails Configuration DependencyResolveDetails ResolutionStrategy))

(defn configureToolConfiguration [^Configuration cfg]
  (.setTransitive cfg false)
  (.setDescription cfg "Describes all spacemaven formatted tools to pull from maven repositories")

  (-> cfg
      (.getResolutionStrategy)
      (.eachDependency (reify Action
                         (execute [this dep]
                           (.artifactSelection (cast DependencyResolveDetails dep)
                                               (reify Action
                                                 (execute [this artifact]
                                                   (.selectArtifact (cast ArtifactSelectionDetails artifact)
                                                                    "tool" "zip" nil))))))))
  )

(defn -apply [this ^Project project]
  (do
    (let [cfgs (.register (.getConfigurations project) "tool")]
        (.configure cfgs (reify Action (execute [this cfg]
                                         (configureToolConfiguration cfg))))
      )
    )
  )
