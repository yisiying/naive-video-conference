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

  def addCommentAccess(roomId: Long, startTime: Long, hostId: Long, userId: Long): Future[Int] = {
    db.run(SlickTables.tCommentAccess += SlickTables.rCommentAccess(1l, roomId, startTime, hostId,  userId, System.currentTimeMillis()))
  }

  def deleteCommentAccess(roomId: Long, startTime: Long, operatorId: Long, userId: Long): Future[Int] = {
    if (operatorId == userId) {
      Future(-3) //不能删除主持人的全限
    } else {
      db.run(tCommentAccess.filter(r => r.roomId === roomId && r.startTime === startTime).result).flatMap { s =>
        if (s.isEmpty) {
          Future(-2) //无该录像，返回-2
        } else {
          if (s.map(_.hostId).contains(operatorId)) {
            db.run(tCommentAccess.filter(l => l.roomId === roomId && l.startTime === startTime && l.allowUid === userId).delete)
          } else {
            Future(-1) //删除操作不是主持人，不能执行
          }
        }
      }
    }
    //    val record = db.run(tRecord.filter(_.id === recordId).result.headOption)
    //    record.flatMap {
    //      case Some(r) =>
    //        if (r.roomid == operatorId) {
    //          db.run(tCommentAccess.filter(l => l.recordId === recordId && l.allowUid === userId).delete)
    //        } else {
    //          Future(-1) //删除操作不是主持人，不能执行
    //        }
    //      case None => //无该录像ID，返回-2
    //        Future(-2)
    //    }
  }

  def deleteComment(roomId:Long,startTime:Long,commentId:Long,operator:Long): Future[Int] ={
    db.run(tCommentAccess.filter(r=>r.roomId===roomId&& r.startTime===startTime&&r.hostId===operator).result).flatMap{res=>
      if(res.isEmpty){
        Future(-1)
      }else{
        db.run(tRecordComment.filter(_.commentId===commentId).delete)
      }
    }
  }

  def checkAccess(roomId: Long, startTime: Long, userId: Long): Future[Boolean] = {
    db.run(tCommentAccess.filter(r => r.roomId === roomId && r.startTime === startTime).map(_.allowUid).result).map { r =>
      r.contains(userId)
    }
  }

  def checkHostAccess(roomId: Long, startTime: Long, hostId: Long): Future[Boolean] = {
    db.run(tCommentAccess.filter(r => r.roomId === roomId && r.startTime === startTime).map(_.hostId).result).map(r => r.contains(hostId))
  }

  def getAudienceIds(roomId:Long, startTime:Long): Future[Seq[Long]] = {
    db.run(tCommentAccess.filter(r => r.roomId === roomId && r.startTime === startTime).map(_.allowUid).result)
  }

}
