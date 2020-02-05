package org.seekloud.theia.roomManager.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.H2Profile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tCommentAccess.schema ++ tRecord.schema ++ tRecordComment.schema ++ tUserInfo.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tCommentAccess
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param recordId Database column RECORD_ID SqlType(BIGINT)
   *  @param hostId Database column HOST_ID SqlType(BIGINT)
   *  @param allowUid Database column ALLOW_UID SqlType(BIGINT)
   *  @param addTime Database column ADD_TIME SqlType(BIGINT) */
  case class rCommentAccess(id: Long, recordId: Long, hostId: Long, allowUid: Long, addTime: Long)
  /** GetResult implicit for fetching rCommentAccess objects using plain SQL queries */
  implicit def GetResultrCommentAccess(implicit e0: GR[Long]): GR[rCommentAccess] = GR{
    prs => import prs._
    rCommentAccess.tupled((<<[Long], <<[Long], <<[Long], <<[Long], <<[Long]))
  }
  /** Table description of table COMMENT_ACCESS. Objects of this class serve as prototypes for rows in queries. */
  class tCommentAccess(_tableTag: Tag) extends profile.api.Table[rCommentAccess](_tableTag, Some("PUBLIC"), "COMMENT_ACCESS") {
    def * = (id, recordId, hostId, allowUid, addTime) <> (rCommentAccess.tupled, rCommentAccess.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(recordId), Rep.Some(hostId), Rep.Some(allowUid), Rep.Some(addTime))).shaped.<>({r=>import r._; _1.map(_=> rCommentAccess.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column RECORD_ID SqlType(BIGINT) */
    val recordId: Rep[Long] = column[Long]("RECORD_ID")
    /** Database column HOST_ID SqlType(BIGINT) */
    val hostId: Rep[Long] = column[Long]("HOST_ID")
    /** Database column ALLOW_UID SqlType(BIGINT) */
    val allowUid: Rep[Long] = column[Long]("ALLOW_UID")
    /** Database column ADD_TIME SqlType(BIGINT) */
    val addTime: Rep[Long] = column[Long]("ADD_TIME")
  }
  /** Collection-like TableQuery object for table tCommentAccess */
  lazy val tCommentAccess = new TableQuery(tag => new tCommentAccess(tag))

  /** Entity class storing rows of table tRecord
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param roomid Database column ROOMID SqlType(BIGINT)
   *  @param startTime Database column START_TIME SqlType(BIGINT)
   *  @param coverImg Database column COVER_IMG SqlType(VARCHAR), Length(256,true)
   *  @param recordName Database column RECORD_NAME SqlType(VARCHAR), Length(10485760,true)
   *  @param recordDes Database column RECORD_DES SqlType(VARCHAR), Length(10485760,true)
   *  @param viewNum Database column VIEW_NUM SqlType(INTEGER)
   *  @param likeNum Database column LIKE_NUM SqlType(INTEGER)
   *  @param duration Database column DURATION SqlType(VARCHAR), Length(100,true), Default()
   *  @param recordAddr Database column RECORD_ADDR SqlType(VARCHAR), Length(100,true), Default() */
  case class rRecord(id: Long, roomid: Long, startTime: Long, coverImg: String, recordName: String, recordDes: String, viewNum: Int, likeNum: Int, duration: String = "", recordAddr: String = "")
  /** GetResult implicit for fetching rRecord objects using plain SQL queries */
  implicit def GetResultrRecord(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[rRecord] = GR{
    prs => import prs._
    rRecord.tupled((<<[Long], <<[Long], <<[Long], <<[String], <<[String], <<[String], <<[Int], <<[Int], <<[String], <<[String]))
  }
  /** Table description of table RECORD. Objects of this class serve as prototypes for rows in queries. */
  class tRecord(_tableTag: Tag) extends profile.api.Table[rRecord](_tableTag, Some("PUBLIC"), "RECORD") {
    def * = (id, roomid, startTime, coverImg, recordName, recordDes, viewNum, likeNum, duration, recordAddr) <> (rRecord.tupled, rRecord.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(roomid), Rep.Some(startTime), Rep.Some(coverImg), Rep.Some(recordName), Rep.Some(recordDes), Rep.Some(viewNum), Rep.Some(likeNum), Rep.Some(duration), Rep.Some(recordAddr))).shaped.<>({r=>import r._; _1.map(_=> rRecord.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column ROOMID SqlType(BIGINT) */
    val roomid: Rep[Long] = column[Long]("ROOMID")
    /** Database column START_TIME SqlType(BIGINT) */
    val startTime: Rep[Long] = column[Long]("START_TIME")
    /** Database column COVER_IMG SqlType(VARCHAR), Length(256,true) */
    val coverImg: Rep[String] = column[String]("COVER_IMG", O.Length(256,varying=true))
    /** Database column RECORD_NAME SqlType(VARCHAR), Length(10485760,true) */
    val recordName: Rep[String] = column[String]("RECORD_NAME", O.Length(10485760,varying=true))
    /** Database column RECORD_DES SqlType(VARCHAR), Length(10485760,true) */
    val recordDes: Rep[String] = column[String]("RECORD_DES", O.Length(10485760,varying=true))
    /** Database column VIEW_NUM SqlType(INTEGER) */
    val viewNum: Rep[Int] = column[Int]("VIEW_NUM")
    /** Database column LIKE_NUM SqlType(INTEGER) */
    val likeNum: Rep[Int] = column[Int]("LIKE_NUM")
    /** Database column DURATION SqlType(VARCHAR), Length(100,true), Default() */
    val duration: Rep[String] = column[String]("DURATION", O.Length(100,varying=true), O.Default(""))
    /** Database column RECORD_ADDR SqlType(VARCHAR), Length(100,true), Default() */
    val recordAddr: Rep[String] = column[String]("RECORD_ADDR", O.Length(100,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tRecord */
  lazy val tRecord = new TableQuery(tag => new tRecord(tag))

  /** Entity class storing rows of table tRecordComment
   *  @param commentId Database column COMMENT_ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param roomId Database column ROOM_ID SqlType(BIGINT)
   *  @param recordTime Database column RECORD_TIME SqlType(BIGINT)
   *  @param comment Database column COMMENT SqlType(VARCHAR), Default()
   *  @param commentTime Database column COMMENT_TIME SqlType(BIGINT)
   *  @param commentUid Database column COMMENT_UID SqlType(BIGINT)
   *  @param authorUid Database column AUTHOR_UID SqlType(BIGINT), Default(None)
   *  @param relativeTime Database column RELATIVE_TIME SqlType(BIGINT), Default(0) */
  case class rRecordComment(commentId: Long, roomId: Long, recordTime: Long, comment: String = "", commentTime: Long, commentUid: Long, authorUid: Option[Long] = None, relativeTime: Long = 0L)
  /** GetResult implicit for fetching rRecordComment objects using plain SQL queries */
  implicit def GetResultrRecordComment(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[Long]]): GR[rRecordComment] = GR{
    prs => import prs._
    rRecordComment.tupled((<<[Long], <<[Long], <<[Long], <<[String], <<[Long], <<[Long], <<?[Long], <<[Long]))
  }
  /** Table description of table RECORD_COMMENT. Objects of this class serve as prototypes for rows in queries. */
  class tRecordComment(_tableTag: Tag) extends profile.api.Table[rRecordComment](_tableTag, Some("PUBLIC"), "RECORD_COMMENT") {
    def * = (commentId, roomId, recordTime, comment, commentTime, commentUid, authorUid, relativeTime) <> (rRecordComment.tupled, rRecordComment.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(commentId), Rep.Some(roomId), Rep.Some(recordTime), Rep.Some(comment), Rep.Some(commentTime), Rep.Some(commentUid), authorUid, Rep.Some(relativeTime))).shaped.<>({r=>import r._; _1.map(_=> rRecordComment.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column COMMENT_ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val commentId: Rep[Long] = column[Long]("COMMENT_ID", O.AutoInc, O.PrimaryKey)
    /** Database column ROOM_ID SqlType(BIGINT) */
    val roomId: Rep[Long] = column[Long]("ROOM_ID")
    /** Database column RECORD_TIME SqlType(BIGINT) */
    val recordTime: Rep[Long] = column[Long]("RECORD_TIME")
    /** Database column COMMENT SqlType(VARCHAR), Default() */
    val comment: Rep[String] = column[String]("COMMENT", O.Default(""))
    /** Database column COMMENT_TIME SqlType(BIGINT) */
    val commentTime: Rep[Long] = column[Long]("COMMENT_TIME")
    /** Database column COMMENT_UID SqlType(BIGINT) */
    val commentUid: Rep[Long] = column[Long]("COMMENT_UID")
    /** Database column AUTHOR_UID SqlType(BIGINT), Default(None) */
    val authorUid: Rep[Option[Long]] = column[Option[Long]]("AUTHOR_UID", O.Default(None))
    /** Database column RELATIVE_TIME SqlType(BIGINT), Default(0) */
    val relativeTime: Rep[Long] = column[Long]("RELATIVE_TIME", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tRecordComment */
  lazy val tRecordComment = new TableQuery(tag => new tRecordComment(tag))

  /** Entity class storing rows of table tUserInfo
   *  @param uid Database column UID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param userName Database column USER_NAME SqlType(VARCHAR), Length(100,true)
   *  @param password Database column PASSWORD SqlType(VARCHAR), Length(100,true)
   *  @param roomid Database column ROOMID SqlType(BIGINT), AutoInc
   *  @param token Database column TOKEN SqlType(VARCHAR), Length(63,true), Default()
   *  @param tokenCreateTime Database column TOKEN_CREATE_TIME SqlType(BIGINT)
   *  @param headImg Database column HEAD_IMG SqlType(VARCHAR), Length(256,true), Default()
   *  @param coverImg Database column COVER_IMG SqlType(VARCHAR), Length(256,true), Default()
   *  @param email Database column EMAIL SqlType(VARCHAR), Length(256,true), Default()
   *  @param createTime Database column CREATE_TIME SqlType(BIGINT)
   *  @param rtmpToken Database column RTMP_TOKEN SqlType(VARCHAR), Length(256,true), Default()
   *  @param `sealed` Database column SEALED SqlType(BOOLEAN), Default(false)
   *  @param sealedUtilTime Database column SEALED_UTIL_TIME SqlType(BIGINT), Default(0)
   *  @param allowAnchor Database column ALLOW_ANCHOR SqlType(BOOLEAN), Default(true) */
  case class rUserInfo(uid: Long, userName: String, password: String, roomid: Long, token: String = "", tokenCreateTime: Long, headImg: String = "", coverImg: String = "", email: String = "", createTime: Long, rtmpToken: String = "", `sealed`: Boolean = false, sealedUtilTime: Long = 0L, allowAnchor: Boolean = true)
  /** GetResult implicit for fetching rUserInfo objects using plain SQL queries */
  implicit def GetResultrUserInfo(implicit e0: GR[Long], e1: GR[String], e2: GR[Boolean]): GR[rUserInfo] = GR{
    prs => import prs._
    rUserInfo.tupled((<<[Long], <<[String], <<[String], <<[Long], <<[String], <<[Long], <<[String], <<[String], <<[String], <<[Long], <<[String], <<[Boolean], <<[Long], <<[Boolean]))
  }
  /** Table description of table USER_INFO. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: sealed */
  class tUserInfo(_tableTag: Tag) extends profile.api.Table[rUserInfo](_tableTag, Some("PUBLIC"), "USER_INFO") {
    def * = (uid, userName, password, roomid, token, tokenCreateTime, headImg, coverImg, email, createTime, rtmpToken, `sealed`, sealedUtilTime, allowAnchor) <> (rUserInfo.tupled, rUserInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(uid), Rep.Some(userName), Rep.Some(password), Rep.Some(roomid), Rep.Some(token), Rep.Some(tokenCreateTime), Rep.Some(headImg), Rep.Some(coverImg), Rep.Some(email), Rep.Some(createTime), Rep.Some(rtmpToken), Rep.Some(`sealed`), Rep.Some(sealedUtilTime), Rep.Some(allowAnchor))).shaped.<>({r=>import r._; _1.map(_=> rUserInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get, _12.get, _13.get, _14.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column UID SqlType(BIGINT), AutoInc, PrimaryKey */
    val uid: Rep[Long] = column[Long]("UID", O.AutoInc, O.PrimaryKey)
    /** Database column USER_NAME SqlType(VARCHAR), Length(100,true) */
    val userName: Rep[String] = column[String]("USER_NAME", O.Length(100,varying=true))
    /** Database column PASSWORD SqlType(VARCHAR), Length(100,true) */
    val password: Rep[String] = column[String]("PASSWORD", O.Length(100,varying=true))
    /** Database column ROOMID SqlType(BIGINT), AutoInc */
    val roomid: Rep[Long] = column[Long]("ROOMID", O.AutoInc)
    /** Database column TOKEN SqlType(VARCHAR), Length(63,true), Default() */
    val token: Rep[String] = column[String]("TOKEN", O.Length(63,varying=true), O.Default(""))
    /** Database column TOKEN_CREATE_TIME SqlType(BIGINT) */
    val tokenCreateTime: Rep[Long] = column[Long]("TOKEN_CREATE_TIME")
    /** Database column HEAD_IMG SqlType(VARCHAR), Length(256,true), Default() */
    val headImg: Rep[String] = column[String]("HEAD_IMG", O.Length(256,varying=true), O.Default(""))
    /** Database column COVER_IMG SqlType(VARCHAR), Length(256,true), Default() */
    val coverImg: Rep[String] = column[String]("COVER_IMG", O.Length(256,varying=true), O.Default(""))
    /** Database column EMAIL SqlType(VARCHAR), Length(256,true), Default() */
    val email: Rep[String] = column[String]("EMAIL", O.Length(256,varying=true), O.Default(""))
    /** Database column CREATE_TIME SqlType(BIGINT) */
    val createTime: Rep[Long] = column[Long]("CREATE_TIME")
    /** Database column RTMP_TOKEN SqlType(VARCHAR), Length(256,true), Default() */
    val rtmpToken: Rep[String] = column[String]("RTMP_TOKEN", O.Length(256,varying=true), O.Default(""))
    /** Database column SEALED SqlType(BOOLEAN), Default(false)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `sealed`: Rep[Boolean] = column[Boolean]("SEALED", O.Default(false))
    /** Database column SEALED_UTIL_TIME SqlType(BIGINT), Default(0) */
    val sealedUtilTime: Rep[Long] = column[Long]("SEALED_UTIL_TIME", O.Default(0L))
    /** Database column ALLOW_ANCHOR SqlType(BOOLEAN), Default(true) */
    val allowAnchor: Rep[Boolean] = column[Boolean]("ALLOW_ANCHOR", O.Default(true))

    /** Uniqueness Index over (roomid) (database name USER_INFO_ROOMID_UINDEX) */
    val index1 = index("USER_INFO_ROOMID_UINDEX", roomid, unique=true)
  }
  /** Collection-like TableQuery object for table tUserInfo */
  lazy val tUserInfo = new TableQuery(tag => new tUserInfo(tag))
}
