package lila.analyse

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config
import lila.notify.NotifyApi
import scala.util.{ Success, Failure }
import spray.caching.{ LruCache, Cache }

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    roundSocket: ActorSelection,
    indexer: ActorSelection,
    notifyApi: NotifyApi,
    getLightUser : lila.common.LightUser.Getter) {

  private val CollectionAnalysis = config getString "collection.analysis"
  private val NetDomain = config getString "net.domain"
  private val CachedNbTtl = config duration "cached.nb.ttl"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private[analyse] lazy val analysisColl = db(CollectionAnalysis)

  lazy val notifier = new Notifier(notifyApi, getLightUser)

  lazy val analyser = new Analyser(
    indexer = indexer,
    roundSocket = roundSocket,
    notifier = notifier,
    bus = system.lilaBus)

  lazy val annotator = new Annotator(NetDomain)

}

object Env {

  lazy val current = "analyse" boot new Env(
    config = lila.common.PlayApp loadConfig "analyse",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    roundSocket = lila.hub.Env.current.socket.round,
    indexer = lila.hub.Env.current.actor.gameSearch,
    notifyApi = lila.notify.Env.current.api,
    getLightUser = lila.user.Env.current.lightUser)
}
