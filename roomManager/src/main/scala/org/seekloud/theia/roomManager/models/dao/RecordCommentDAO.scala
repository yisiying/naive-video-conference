package org.seekloud.theia.roomManager.models.dao

import org.seekloud.theia.roomManager.models.SlickTables
import slick.jdbc.PostgresProfile.api._
import org.seekloud.theia.roomManager.Boot.executor
import org.seekloud.theia.roomManager.models
import org.seekloud.theia.roomManager.utils.DBUtil._
import org.seekloud.theia.roomManager.models.SlickTables._

import scala.concurrent.Future

/**
  * created by benyafang on 2019/9/23 16:20
  **/
object RecordCommentDAO {
  def addRecordComment(r: SlickTables.rRecordComment): Future[Long] = {
    db.run(SlickTables.tRecordComment.returning(SlickTables.tRecordComment.map(_.commentId)) += r)
  }

  def getRecordComment(roomId: Long, recordTime: Long): Future[scala.Seq[SlickTables.tRecordComment#TableElementType]] = {
    db.run(SlickTables.tRecordComment.filter(r => r.roomId === roomId && r.recordTime === recordTime).result)
  }

  def addCommentAccess(recordId: Long, hostId: Long, /*hostRoomId:Long,*/ userId: Long): Future[Int] = {
    db.run(SlickTables.tCommentAccess += SlickTables.rCommentAccess(1l, recordId, hostId, /*hostRoomId,*/ userId, System.currentTimeMillis()))
  }

  def deleteCommentAccess(recordId: Long, operatorId: Long, userId: Long): Future[Int] = {
    val record = db.run(tRecord.filter(_.id === recordId).result.headOption)
    record.flatMap {
      case Some(r) =>
        if (r.roomid == operatorId) {
          db.run(tCommentAccess.filter(l => l.recordId === recordId && l.allowUid === userId).delete)
        } else {
          Future(-1) //删除操作不是主持人，不能执行
        }
      case None => //无该录像ID，返回-2
        Future(-2)
    }
  }

  def checkAccess(recordId:Long, userId:Long): Future[Boolean] = {
    db.run(tCommentAccess.filter(r => r.recordId === recordId).map(_.allowUid).result).map{r =>
      r.contains(userId)
    }
  }

  def checkHostAccess(recordId: Long, hostId: Long): Future[Boolean] = {
    db.run(tCommentAccess.filter(r => r.recordId === recordId).map(_.hostId).result).map(r => r.contains(hostId))
  }

}
