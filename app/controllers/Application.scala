package controllers

import play.api._
import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsArray
import scala.util.Random
import play.api.libs.json.JsNull

object Application extends Controller {

  def apiKey =
    play.api.Play.maybeApplication.
      map(_.configuration.getString("echo.nest.api.key")).
      getOrElse(
        throw new Exception("Couldn't locate echo.nest.api.key , " +
          "make sure it's setup in your conf file.")).get

  def index = Action {
    Ok(views.html.index())
  }

  private def getPlaylistFromEchoNest(
    minTempo: Int,
    maxTempo: Int,
    minDancability: Double) = {
    assert(minDancability >= 0 && minDancability <= 1,
      "Dancability value: " + minDancability + " not allowed")

    val url = "http://developer.echonest.com/api/v4/song/search" +
      "?api_key=" + apiKey + "&bucket=id:spotify" +
      "&results=90" +
      "&style=swing&style=%20revival&style=western%20swing" +
      "&style=retro%20swing&style=neoswing&style=big%20band" +
      "&style=jazz&style=blues" +
      "&bucket=tracks" +
      "&min_danceability=" + minDancability +
      "&min_tempo=" + minTempo + "&max_tempo=" + maxTempo

    Logger.debug("url=" + url)

    val playlist: Future[play.api.libs.ws.Response] =
      WS.url(url).get()

    playlist
  }

  def getPlaylist(
    minbeat: Int,
    maxbeat: Int,
    minDancability: Double) = Action.async {
    Logger.debug("Getting playlist for range: " + minbeat + "-" + maxbeat +
      " minDancability=" + minDancability)

    val playlistResponse = getPlaylistFromEchoNest(minbeat, maxbeat, minDancability)

    val timeoutFuture = play.api.libs.concurrent.Promise.
      timeout("Timed out on Echo Nest request.", 5000)

    Future.firstCompletedOf(Seq(playlistResponse, timeoutFuture)).map {
      case i: play.api.libs.ws.Response => {
        //Logger.debug("playlist=" + Json.prettyPrint(i.json))
        val uniqueSongs = getUniqueSongs(i.json)
        Logger.debug(uniqueSongs.toString)
        Ok(uniqueSongs)
      }
      case t: String => {
        Logger.error(t)
        InternalServerError(t)
      }
    }
  }

  def getUniqueSongs(jSon: JsValue): JsValue = {

    val result = (jSon \\ "songs")

    val titleArtistAndSId = result.flatMap(x => {

      val y = x.as[List[JsValue]]
      Logger.debug("Result size: " + y.size)

      val songs = y.map(f => {
        val title = f \ "title"
        val artist = f \ "artist_name"
        val spotifyId = f \\ "foreign_id"
        val idList = spotifyId.toList.
          filter(x => x.as[String].startsWith("spotify:track"))
        val pickedId =
          if (idList.size > 0)
            Random.shuffle(idList).head
          else
            JsNull

        Logger.debug("title=" + title)
        Logger.debug("artist=" + artist)
        Logger.debug("spotifyId=" + pickedId)

        Json.obj("title" -> title, "artist" -> artist, "spotify_id" -> pickedId)
      })

      songs.filter(p => p \ "spotify_id" != JsNull)

    })

    Logger.debug("Found " + titleArtistAndSId.length + " songs.")

    titleArtistAndSId.foldLeft(Json.arr())((array, value) => {
      array.prepend(value)
    })
  }
}