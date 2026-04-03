package repositories

import javax.inject.{Inject, Singleton}
import models.*
import models.Tables.*
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api.*

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import slick.basic.DatabasePublisher
import play.api.Logging

/**
 * Repository for preset database operations using Slick.
 *
 * Demonstrates Scala-idiomatic patterns:
 * - Future-based async operations
 * - Option/Either for error handling
 * - Functional query composition
 */
@Singleton
class PresetRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(using ec: ExecutionContext) extends Logging {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig.*

  // ========== Read Operations ==========

  def list(page: Int, size: Int, defaultOnly: Boolean): Future[(Seq[Preset], Int)] = {
    logger.info(s"Listing presets: page=$page, size=$size, defaultOnly=$defaultOnly")

    val baseQuery = if (defaultOnly) {
      presets.filter(_.isDefault === true)
    } else {
      presets
    }

    val pagedQuery = baseQuery
      .sortBy(_.createdAt.desc)
      .drop(page * size)
      .take(size)

    val countQuery = baseQuery.length

    db.run(for {
      items <- pagedQuery.result
      total <- countQuery.result
    } yield {
      logger.info(s"Found ${items.size} presets (total: $total)")
      (items, total)
    })
  }

  def findById(id: UUID): Future[Option[Preset]] = {
    logger.info(s"Finding preset: id=$id")
    db.run(presets.filter(_.id === id).result.headOption)
  }

  def findByShareCode(code: String): Future[Option[Preset]] = {
    logger.info(s"Finding preset by share code: $code")
    db.run(presets.filter(_.shareCode === code).result.headOption)
  }

  def popular(limit: Int): Future[Seq[Preset]] = {
    logger.info(s"Fetching popular presets: limit=$limit")
    db.run(
      presets
        .filter(p => p.isDefault === true || p.shareCode.isDefined)
        .sortBy(_.likesCount.desc)
        .take(limit)
        .result
    )
  }

  def streamAll(): DatabasePublisher[Preset] = {
    logger.info("Creating preset stream")
    db.stream(
      presets.sortBy(_.createdAt.desc).result
    )
  }

  // ========== Write Operations ==========

  def create(preset: Preset): Future[Preset] = {
    logger.info(s"Creating preset: name=${preset.name}")
    db.run(presets += preset).map { _ =>
      logger.info(s"Preset created: id=${preset.id}, name=${preset.name}")
      preset
    }
  }

  def update(id: UUID, name: Option[String], settings: Option[String]): Future[Option[Preset]] = {
    logger.info(s"Updating preset: id=$id")

    val query = presets.filter(_.id === id)

    val updateAction = for {
      existing <- query.result.headOption
      _ <- existing match {
        case Some(_) =>
          val updates = Seq(
            name.map(n => query.map(_.name).update(n)),
            settings.map(s => query.map(_.settings).update(s))
          ).flatten

          DBIO.sequence(updates)
        case None =>
          DBIO.successful(Seq.empty)
      }
      updated <- query.result.headOption
    } yield {
      updated.foreach(p => logger.info(s"Preset updated: id=${p.id}, name=${p.name}"))
      updated
    }

    db.run(updateAction.transactionally)
  }

  def delete(id: UUID): Future[Either[String, Unit]] = {
    logger.info(s"Deleting preset: id=$id")

    val action = for {
      existing <- presets.filter(_.id === id).result.headOption
      result <- existing match {
        case None =>
          DBIO.successful(Left("Preset not found"))
        case Some(p) if p.isDefault =>
          DBIO.successful(Left("Cannot delete default presets"))
        case Some(_) =>
          presets.filter(_.id === id).delete.map(_ => Right(()))
      }
    } yield {
      result match {
        case Right(_) => logger.info(s"Preset deleted: id=$id")
        case Left(err) => logger.warn(s"Delete refused: id=$id, reason=$err")
      }
      result
    }

    db.run(action.transactionally)
  }

  // ========== Like Operations ==========

  def toggleLike(presetId: UUID, userId: UUID): Future[Either[String, (String, Int)]] = {
    logger.info(s"Toggle like: presetId=$presetId, userId=$userId")

    val action = for {
      presetOpt <- presets.filter(_.id === presetId).result.headOption
      result <- presetOpt match {
        case None =>
          DBIO.successful(Left("Preset not found"))
        case Some(preset) =>
          val existingLike = likes.filter(l => l.userId === userId && l.presetId === presetId)

          for {
            likeOpt <- existingLike.result.headOption
            (action, newCount) <- likeOpt match {
              case Some(_) =>
                // Unlike
                for {
                  _ <- existingLike.delete
                  nc = Math.max(0, preset.likesCount - 1)
                  _ <- presets.filter(_.id === presetId).map(_.likesCount).update(nc)
                } yield ("unliked", nc)
              case None =>
                // Like
                val newLike = Like(UUID.randomUUID(), userId, presetId, None)
                for {
                  _ <- likes += newLike
                  nc = preset.likesCount + 1
                  _ <- presets.filter(_.id === presetId).map(_.likesCount).update(nc)
                } yield ("liked", nc)
            }
          } yield {
            logger.info(s"$action: presetId=$presetId, newCount=$newCount")
            Right((action, newCount))
          }
      }
    } yield result

    db.run(action.transactionally)
  }
}
