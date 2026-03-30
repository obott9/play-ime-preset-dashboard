package controllers

import javax.inject.{Inject, Singleton}
import models._
import repositories.PresetRepository
import play.api.libs.json._
import play.api.mvc._

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

/**
 * REST API controller for IME preset operations (Scala version).
 *
 * Demonstrates Play Framework 3.0 + Scala patterns:
 * - Future-based async actions
 * - Functional error handling with Either/Option
 * - Pekko Streams for reactive data streaming
 * - Pattern matching for request routing
 * - Type-safe JSON serialization
 */
@Singleton
class PresetController @Inject()(
  cc: ControllerComponents,
  repository: PresetRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc) with Logging {

  // ========== CRUD ==========

  /**
   * GET /api/presets
   * List presets with pagination. Functional query composition via Slick.
   */
  def list(page: Int, size: Int, defaultOnly: Boolean): Action[AnyContent] = Action.async {
    repository.list(page, size, defaultOnly).map { case (items, totalCount) =>
      val totalPages = (totalCount + size - 1) / size
      Ok(Json.obj(
        "page"       -> page,
        "size"       -> size,
        "totalCount" -> totalCount,
        "totalPages" -> totalPages,
        "data"       -> Json.toJson(items)
      ))
    }
  }

  /**
   * GET /api/presets/:id
   * Get by UUID. Returns Option → 200 or 404.
   */
  def get(id: UUID): Action[AnyContent] = Action.async {
    repository.findById(id).map {
      case Some(preset) => Ok(Json.toJson(preset))
      case None         => NotFound(errorJson("Preset not found", Some(id.toString)))
    }
  }

  /**
   * POST /api/presets
   * Create a new preset. Validates with pattern matching on JSON fields.
   */
  def create(): Action[JsValue] = Action.async(parse.json) { request =>
    val body = request.body

    val nameOpt = (body \ "name").asOpt[String].filter(_.nonEmpty)
    val settingsOpt = (body \ "settings").toOption.map(Json.stringify)
    val userIdOpt = (body \ "user_id").asOpt[String].map(UUID.fromString)

    (nameOpt, settingsOpt) match {
      case (None, _) =>
        Future.successful(BadRequest(errorJson("Field 'name' is required", None)))
      case (_, None) =>
        Future.successful(BadRequest(errorJson("Field 'settings' is required", None)))
      case (Some(name), Some(settings)) =>
        val preset = Preset(
          id = UUID.randomUUID(),
          userId = userIdOpt,
          name = name,
          settings = settings,
          shareCode = None
        )
        repository.create(preset).map(p => Created(Json.toJson(p)))
    }
  }

  /**
   * PUT /api/presets/:id
   * Partial update. Only provided fields are updated.
   */
  def update(id: UUID): Action[JsValue] = Action.async(parse.json) { request =>
    val body = request.body
    val nameOpt = (body \ "name").asOpt[String].filter(_.nonEmpty)
    val settingsOpt = (body \ "settings").toOption.map(Json.stringify)

    repository.update(id, nameOpt, settingsOpt).map {
      case Some(preset) => Ok(Json.toJson(preset))
      case None         => NotFound(errorJson("Preset not found", Some(id.toString)))
    }
  }

  /**
   * DELETE /api/presets/:id
   * Delete with Either-based error handling.
   */
  def delete(id: UUID): Action[AnyContent] = Action.async {
    repository.delete(id).map {
      case Right(_)            => NoContent
      case Left("Preset not found") => NotFound(errorJson("Preset not found", Some(id.toString)))
      case Left(msg)           => Forbidden(errorJson(msg, Some(id.toString)))
    }
  }

  // ========== Share ==========

  /**
   * GET /api/presets/shared/:code
   */
  def getByShareCode(code: String): Action[AnyContent] = Action.async {
    repository.findByShareCode(code).map {
      case Some(preset) => Ok(Json.toJson(preset))
      case None         => NotFound(errorJson("Preset not found for share code", Some(code)))
    }
  }

  // ========== Like ==========

  /**
   * POST /api/presets/:id/like
   * Toggle like with transactional Either result.
   */
  def toggleLike(id: UUID): Action[JsValue] = Action.async(parse.json) { request =>
    (request.body \ "user_id").asOpt[String] match {
      case None =>
        Future.successful(BadRequest(errorJson("Field 'user_id' is required", None)))
      case Some(uid) =>
        val userId = UUID.fromString(uid)
        repository.toggleLike(id, userId).map {
          case Right((action, count)) =>
            Ok(Json.obj("action" -> action, "likesCount" -> count))
          case Left(msg) =>
            NotFound(errorJson(msg, Some(id.toString)))
        }
    }
  }

  // ========== Popular ==========

  /**
   * GET /api/presets/popular
   */
  def popular(limit: Int): Action[AnyContent] = Action.async {
    repository.popular(limit).map(presets => Ok(Json.toJson(presets)))
  }

  // ========== Streaming (Pekko Streams) ==========

  /**
   * GET /api/presets/stream
   * Server-Sent Events stream of all presets using Pekko Streams.
   *
   * Demonstrates reactive streaming — a key differentiator
   * of the Scala version over the Java version.
   */
  def streamPresets(): Action[AnyContent] = Action {
    val publisher = repository.streamAll()

    val source = Source
      .fromPublisher(publisher)
      .map { preset =>
        val json = Json.toJson(preset)
        ByteString(s"data: ${Json.stringify(json)}\n\n")
      }

    Ok.chunked(source).as("text/event-stream")
  }

  // ========== Health ==========

  /**
   * GET /api/health
   */
  def health(): Action[AnyContent] = Action {
    Ok(Json.obj(
      "status"    -> "ok",
      "framework" -> "Play Framework 3.0.10",
      "language"  -> "Scala 2.13",
      "java"      -> System.getProperty("java.version")
    ))
  }

  // ========== Helpers ==========

  private def errorJson(message: String, detail: Option[String]): JsObject = {
    val base = Json.obj("error" -> message)
    detail.fold(base)(d => base + ("detail" -> JsString(d)))
  }
}
