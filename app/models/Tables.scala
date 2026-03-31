package models

import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import play.api.libs.json._

/**
 * Slick table definitions for IME preset database.
 */

// --- Case Classes ---

case class Preset(
  id: UUID,
  userId: Option[UUID],
  name: String,
  settings: String,
  shareCode: Option[String],
  likesCount: Int = 0,
  isDefault: Boolean = false,
  createdAt: Option[Timestamp] = None,
  updatedAt: Option[Timestamp] = None
) {
  def createdAtOffset: Option[OffsetDateTime] = createdAt.map(_.toInstant.atOffset(ZoneOffset.UTC))
  def updatedAtOffset: Option[OffsetDateTime] = updatedAt.map(_.toInstant.atOffset(ZoneOffset.UTC))
}

case class Like(
  id: UUID,
  userId: UUID,
  presetId: UUID,
  createdAt: Option[Timestamp] = None
)

// --- JSON Formats ---

object Preset {
  implicit val timestampWrites: Writes[Timestamp] = (ts: Timestamp) =>
    JsString(ts.toInstant.atOffset(ZoneOffset.UTC).toString)
  implicit val timestampReads: Reads[Timestamp] = (json: JsValue) =>
    json.validate[String].map(s => Timestamp.from(OffsetDateTime.parse(s).toInstant))
  implicit val format: OFormat[Preset] = Json.format[Preset]
}

object Like {
  import Preset.{timestampWrites, timestampReads}
  implicit val format: OFormat[Like] = Json.format[Like]
}

// --- Slick Table Mappings ---

class PresetsTable(tag: Tag) extends Table[Preset](tag, "presets") {
  def id         = column[UUID]("id", O.PrimaryKey)
  def userId     = column[Option[UUID]]("user_id")
  def name       = column[String]("name")
  def settings   = column[String]("settings")
  def shareCode  = column[Option[String]]("share_code")
  def likesCount = column[Int]("likes_count", O.Default(0))
  def isDefault  = column[Boolean]("is_default", O.Default(false))
  def createdAt  = column[Option[Timestamp]]("created_at")
  def updatedAt  = column[Option[Timestamp]]("updated_at")

  def * = (id, userId, name, settings, shareCode, likesCount, isDefault, createdAt, updatedAt).mapTo[Preset]

  def idxUserId    = index("idx_presets_user_id", userId)
  def idxShareCode = index("idx_presets_share_code", shareCode, unique = true)
  def idxDefault   = index("idx_presets_is_default", isDefault)
}

class LikesTable(tag: Tag) extends Table[Like](tag, "likes") {
  def id        = column[UUID]("id", O.PrimaryKey)
  def userId    = column[UUID]("user_id")
  def presetId  = column[UUID]("preset_id")
  def createdAt = column[Option[Timestamp]]("created_at")

  def * = (id, userId, presetId, createdAt).mapTo[Like]

  def presetFk = foreignKey("fk_likes_preset", presetId, TableQuery[PresetsTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  def uniqueUserPreset = index("idx_likes_user_preset", (userId, presetId), unique = true)
}

// --- Table Query Objects ---

object Tables {
  val presets = TableQuery[PresetsTable]
  val likes   = TableQuery[LikesTable]
}
