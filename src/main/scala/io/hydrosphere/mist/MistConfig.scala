package io.hydrosphere.mist

import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigValue, ConfigException, ConfigFactory, Config}

import scala.collection.JavaConversions._
import scala.concurrent.duration.{FiniteDuration, Duration}

/** Configuration wrapper */
private[mist] object MistConfig {

  private val config = ConfigFactory.load()

  /** Common application settings */
  object Settings {
    private val settings = config.getConfig("mist.settings")

    /** Max number of threads for JVM where jobs are running */
    lazy val threadNumber: Int = settings.getInt("threadNumber")
  }

  /** HTTP specific settings */
  object HTTP {
    private val http = config.getConfig("mist.http")

    /** To start HTTP server or not to start */
    val isOn: Boolean = http.getBoolean("on")

    /** HTTP server host */
    lazy val host: String = http.getString("host")
    /** HTTP server port */
    lazy val port: Int = http.getInt("port")
  }

  /** Settings for each spark context */
  object Spark {
    private val spark = config.getConfig("mist.spark")

    /** Spark master server url
      *
      * Any clear for spark string:
      * local[*]
      * spark://host:7077
      * mesos://host:5050
      * yarn
      */
    lazy val master: String = spark.getString("master")
  }

  /** MQTT specific settings */
  object MQTT {
    private val mqtt = config.getConfig("mist.mqtt")

    /** To start MQTT subscriber on not to start */
    val isOn: Boolean = mqtt.getBoolean("on")

    /** MQTT host */
    lazy val host: String = mqtt.getString("host")
    /** MQTT port */
    lazy val port: Int = mqtt.getInt("port")
    /** MQTT topic used for ''reading'' */
    lazy val subscribeTopic: String = mqtt.getString("subscribeTopic")
    /** MQTT topic used for ''writing'' */
    lazy val publishTopic: String = mqtt.getString("publishTopic")


    private val recovery = config.getConfig("mist.mqtt.recovery")

    /** MQTT job recovery after mist Failure*/
    lazy val recoveryOn: Boolean = recovery.getBoolean("on")
    //** MQTT job recovery multi start limit */
    lazy val recoveryMultilimit: Int = recovery.getInt("multilimit")
    /** MQTT job recovery MapDb file name*/
    lazy val recoveryDbFileName: String = recovery.getString("dbfilename")
  }


  /** Settings for all contexts generally and for each context particularly */
  object Contexts {
    private val contexts = if (config.hasPath("mist.contexts")) config.getConfig("mist.contexts") else null
    private val contextDefaults = config.getConfig("mist.contextDefaults")
    private val contextSettings = if (config.hasPath("mist.contextSettings")) config.getConfig("mist.contextSettings") else null

    /** Flag of context creating on start or on demand */
    lazy val precreated: List[String] = if (contextSettings != null) contextSettings.getStringList("onstart").toList else List()

    /** Return config for specified context or default settings
      *
      * @param contextName    context name
      * @return               config for `contextName` or default config
      */
    private def getContextOrDefault(contextName: String): Config = {
      var contextConfig:Config = null
      try {
        contextConfig = contexts.getConfig(contextName).withFallback(contextDefaults)
      }
      catch {
        case _: ConfigException.Missing  | _: NullPointerException =>
          contextConfig = contextDefaults
      }
      contextConfig
    }

    /** Waiting for job completion timeout */
    def timeout(contextName: String): FiniteDuration = {
      FiniteDuration(Duration(getContextOrDefault(contextName).getString("timeout")).toNanos, TimeUnit.NANOSECONDS)
    }

    /** If true we'll stop context */
    def isDisposable(contextName: String): Boolean = {
      getContextOrDefault(contextName).getBoolean("disposable")
    }

    /** Settings for SparkConf */
    def sparkConf(contextName: String): Set[List[String]] = {
      getContextOrDefault(contextName).getConfig("sparkConf").entrySet.map {
        case (m: java.util.Map.Entry[String, ConfigValue]) => List(m.getKey, m.getValue.unwrapped().toString)
      }.toSet
    }

  }
}
