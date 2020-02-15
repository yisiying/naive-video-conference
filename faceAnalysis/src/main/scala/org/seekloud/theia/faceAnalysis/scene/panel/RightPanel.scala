package org.seekloud.theia.faceAnalysis.scene.panel

import javafx.beans.property.{ObjectProperty, SimpleObjectProperty, SimpleStringProperty, StringProperty}
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.control._
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.layout.{HBox, StackPane, VBox}
import javafx.scene.text.Font
import org.seekloud.theia.faceAnalysis.common.Pictures
import org.seekloud.theia.faceAnalysis.component.Emoji.{emojiBtnLists, emojiFont, getEmojiGridPane}
import org.seekloud.theia.faceAnalysis.component.Gift.{getGiftGridPane, giftBtnLists}
import org.seekloud.theia.faceAnalysis.component.Special.{getSpecialGridPane, specialBtnLists}
import org.seekloud.theia.faceAnalysis.controller.HomeController
import org.seekloud.theia.faceAnalysis.controller.HomeController.User
import org.seekloud.theia.protocol.ptcl.CommonInfo
import org.seekloud.theia.protocol.ptcl.CommonInfo.{UserDes, UserInfo}
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol.{Comment, RcvComment}

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 15:21
  */
trait RightPanel extends SceneImpl {

  protected def sendCmt(comment: Comment)
  var rightCommentArea: VBox = _
  var rightContentPane: StackPane = _
  var scrollPane: ScrollPane = _
  var commentBoard: VBox = _
  var commentBox: VBox = _
  var rightOnlineArea: VBox = _
  var watchingNumText: Label = _
  var watchingTable: TableView[WatchingListInfo] = _
  val watchingList: ObservableList[WatchingListInfo] = FXCollections.observableArrayList()
  protected val defaultHeaderUrl = "img/header.png"

  def addRightArea(): VBox = {

    val rightAreaBox = new VBox()
    rightContentPane = new StackPane()

    //留言内容区
    createCommentArea
    //留言输入区
    createCommentBox
    //在线用户列表
    createOnlineArea

    rightContentPane.getChildren.addAll(rightCommentArea, rightOnlineArea)

    rightAreaBox.setPadding(new Insets(15, 0, 0, 0))
    rightAreaBox.setSpacing(0)
    rightAreaBox.getChildren.addAll(getHeadArea, rightContentPane, commentBox)
    rightAreaBox
  }

  /**
    * 在线用户
    */

  case class WatchingListInfo(
    header: ObjectProperty[ImageView],
    userInfo: StringProperty
  ) {
    def getHeader: ImageView = header.get()

    def setHeader(headerImg: ImageView): Unit = header.set(headerImg)

    def getUserInfo: String = userInfo.get()

    def setUserInfo(info: String): Unit = userInfo.set(info)

  }

  lazy val commentBtn: Button = createHeadButton("留言")
  lazy val onlineUserBtn: Button = createHeadButton("在线用户")
  setHeadButtonAction()

  def createHeadButton(text: String): Button = {
    val btn = new Button(text)
    btn.setPrefWidth(width * 0.152)
    btn
  }

  def setHeadButtonAction(): Unit = {
    commentBtn.setOnAction { _ =>
      rightOnlineArea.setVisible(false)
      rightCommentArea.setVisible(true)

    }
    onlineUserBtn.setOnAction { _ =>
      rightCommentArea.setVisible(false)
      rightOnlineArea.setVisible(true)

    }
  }

  def createCommentArea: Unit = {
    rightCommentArea = new VBox()
    commentBoard = new VBox(5)
    scrollPane = new ScrollPane()
    scrollPane.setContent(commentBoard)
    scrollPane.setPrefHeight(height * 0.73)
    scrollPane.setPrefWidth(width * 0.26)
    scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS)
    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER)
    scrollPane.setStyle("-fx-background-color:#c1c1c13d;-fx-background-radius: 10px;")
    scrollPane.setPadding(new Insets(5, 0, 5, 5))
    rightCommentArea.getChildren.add(scrollPane)
  }

  def createCommentBox: Unit = {
    val commentBoxListener = new InvalidationListener {
      override def invalidated(observable: Observable): Unit = {
        scrollPane.setVvalue(1D)
      }
    }
    commentBoard.heightProperty().addListener(commentBoxListener)
    val commentFiled = new TextArea()
    commentFiled.setPrefWidth(width * 0.25)
    commentFiled.setFont(Font.font(emojiFont, 15))
    commentFiled.setPromptText("在这里输入你的留言吧~")
    commentFiled.setPrefRowCount(1)
    commentFiled.setWrapText(true)
    commentFiled.setStyle("-fx-background-radius: 5px;")
    emojiBtnLists.foreach { button =>
      button.setOnAction { _ =>
        commentFiled.setText(commentFiled.getText + button.getText)
        commentFiled.setFont(Font.font(emojiFont))
      }
    }

    specialBtnLists.zipWithIndex.foreach {
      s =>
        s._1.setOnAction {
          _ =>
            s._2 match {
              case 0 => commentFiled.setText(commentFiled.getText + "【放大特效】！")
              case 1 => commentFiled.setText(commentFiled.getText + "【缩小特效】！")
            }
        }
    }

    giftBtnLists.zipWithIndex.foreach {
      s =>
        s._1.setOnAction {
          _ =>
            s._2 match {
              case 0 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:掌声！")
              case 1 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:蛋糕！")
              case 2 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:鲜花！")
              case 3 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:汽车！")
              case 4 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:轮船！")
              case 5 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:飞机！")
              case 6 => commentFiled.setText(commentFiled.getText + s"送出【礼物】:火箭！")
            }
        }
    }

    val sendIcon = new ImageView("img/confirm.png")
    sendIcon.setFitHeight(30)
    sendIcon.setFitWidth(20)
    val sendBtn = new Button("", sendIcon)
    sendBtn.setStyle("-fx-background-radius: 5px;")
    sendBtn.setOnAction { _ =>
      if (HomeController.usersInfo.nonEmpty) {

        val comment = Comment(HomeController.usersInfo.get.loginInfo.userId, HomeController.roomInfo.get.roomId, commentFiled.getText)
        sendCmt(comment)
        commentFiled.setText("")
        commentFiled.setPromptText("在这里输入你的留言吧~")
      } else {
        log.debug("notLogin")
      }
    }
    val emojiBtn = new Button("\uD83D\uDE00") //😀
    emojiBtn.setStyle("-fx-background-radius: 5px;")
    emojiBtn.setFont(Font.font(emojiFont, 15))
    var emojiBtnClick = true
    val emojiArea = getEmojiGridPane
    val specialArea = getSpecialGridPane
    val giftArea = getGiftGridPane
    emojiBtn.setOnAction { _ =>
      if (emojiBtnClick) {
        group.getChildren.remove(specialArea)
        group.getChildren.remove(giftArea)
        group.getChildren.add(emojiArea)
      } else {
        group.getChildren.remove(specialArea)
        group.getChildren.remove(emojiArea)
        group.getChildren.remove(giftArea)
      }
      emojiBtnClick = !emojiBtnClick
    }

    val giftIcon = new ImageView("img/picture.png")
    giftIcon.setFitHeight(20)
    giftIcon.setFitWidth(20)
    val giftBtn = new Button("", giftIcon)
    giftBtn.setStyle("-fx-background-radius: 5px;")
    var giftBtnClick = true
    giftBtn.setOnAction { _ =>
      if (giftBtnClick) {
        group.getChildren.remove(emojiArea)
        group.getChildren.remove(specialArea)
        group.getChildren.add(giftArea)
      } else {
        group.getChildren.remove(giftArea)
        group.getChildren.remove(specialArea)
        group.getChildren.remove(emojiArea)
      }
      giftBtnClick = !giftBtnClick
    }

    val specialIcon = new ImageView("img/special.png")
    specialIcon.setFitHeight(20)
    specialIcon.setFitWidth(20)
    val specialBtn = new Button("", specialIcon)
    specialBtn.setStyle("-fx-background-radius: 5px;")
    var specialBtnClick = true
    specialBtn.setOnAction { _ =>
      if (specialBtnClick) {
        group.getChildren.remove(emojiArea)
        group.getChildren.remove(giftArea)
        group.getChildren.add(specialArea)
      } else {
        group.getChildren.remove(specialArea)
        group.getChildren.remove(emojiArea)
        group.getChildren.remove(giftArea)
      }
      specialBtnClick = !specialBtnClick
    }

    val functionBox = new HBox(emojiBtn, giftBtn, specialBtn)
    functionBox.setSpacing(10)
    val commentBtnBox = new HBox(commentFiled, sendBtn)
    commentBtnBox.setSpacing(5)
    commentBox = new VBox(functionBox, commentBtnBox)

    commentBox.setAlignment(Pos.CENTER_LEFT)
    commentBox.setSpacing(10)
  }

  /**
    * 右部header
    */
  def getHeadArea: HBox = {
    val btnHBox = new HBox(0)
    btnHBox.setAlignment(Pos.CENTER)
    btnHBox.getChildren.addAll(commentBtn, onlineUserBtn)
    btnHBox
  }


  def createOnlineArea: Unit = {
    rightOnlineArea = new VBox()
    //观看直播人数
    createWatchingNumText
    //观众列表
    createAudTbArea

    def createWatchingNumText: Unit = {
      watchingNumText = new Label()
      watchingNumText.setPrefWidth(width * 0.31)
      watchingNumText.setStyle("-fx-background-color:#c1c1c13d;")
      if (HomeController.roomInfo.nonEmpty) {
        watchingNumText.setText(s"有${HomeController.roomInfo.get.observerNum + 1}人正在观看该直播")
      } else {
        watchingNumText.setText(s"房间信息获取失败")
      }
    }

    def createAudTbArea: Unit = {
      watchingTable = new TableView[WatchingListInfo]()
      watchingTable.getStyleClass.add("table-view")

      val headerCol = new TableColumn[WatchingListInfo, ImageView]("头像")
      headerCol.setCellValueFactory(new PropertyValueFactory[WatchingListInfo, ImageView]("header"))
      headerCol.setPrefWidth(width * 0.05)

      val userInfoCol = new TableColumn[WatchingListInfo, String]("用户信息")
      userInfoCol.setPrefWidth(width * 0.2)
      userInfoCol.setCellValueFactory(new PropertyValueFactory[WatchingListInfo, String]("userInfo"))

      watchingTable.setItems(watchingList)
      watchingTable.getColumns.addAll(headerCol, userInfoCol)
      watchingTable.setPrefHeight(height * 0.71)
      watchingTable.setPlaceholder(new Label(""))
    }

    rightOnlineArea.getChildren.addAll(watchingNumText, watchingTable)
    rightOnlineArea.setVisible(false)
  }

  def createCommentTextArea(commentKind: Int, comments: String): Label = {
    val commentTextArea = new Label(comments)
    commentTextArea.setFont(Font.font(emojiFont, 15))
    commentKind match {
      case -1 => commentTextArea.setStyle("-fx-text-fill: orange")
      case 1 => commentTextArea.setStyle("-fx-text-fill: #073068")
      case _ => commentTextArea.setStyle("-fx-text-fill: green")
    }
    commentTextArea.setPrefWidth(width * 0.3)
    commentTextArea.setWrapText(true)
    commentTextArea.setTextOverrun(OverrunStyle.CLIP)
    commentTextArea
  }

  def updateComment(comment: RcvComment): Unit = {
    val userId = HomeController.usersInfo.getOrElse(User("", UserInfo(1l, "", "", "", 1l))).loginInfo.userId
    val comments = comment.userId match {
      case -1 =>
        s"[系统消息]: ${comment.comment}"
      case `userId` =>
        s"[主播]${comment.userName}: ${comment.comment}"
      case _ =>
        s"${comment.userName}: ${comment.comment}"
    }

    comment.userId match {
      case -1 =>
        val commentTextArea = createCommentTextArea(-1, comments)
        commentBoard.getChildren.add(commentTextArea)
      case `userId` =>
        val commentTextArea = createCommentTextArea(1, comments)
        commentBoard.getChildren.add(commentTextArea)
      case _ =>
        val commentTextArea = createCommentTextArea(2, comments)
        commentBoard.getChildren.add(commentTextArea)
    }
  }

  /**
    * 更新观众列表
    *
    **/


  def updateWatchingList(list: List[UserDes]): Unit = {
    watchingNumText.setText(s"有${list.length}人正在观看该直播:")
    if (list.size < watchingList.size()) { // Audience leave, reduce from watchingList.
      var removePos = 0
      for (i <- 0 until watchingList.size()) {
        if (list.filter(l => s"StringProperty [value: ${l.userName}(${l.userId})]" == watchingList.get(i).userInfo.toString) == List()) {
          removePos = i
        }
      }
      log.debug(s"removePos: $removePos")
      watchingList.remove(removePos)
      watchingTable.setItems(watchingList)
    } else if (list.size > watchingList.size()) { // Audience come, add to watchingList.
      var addList = List[CommonInfo.UserDes]()
      list.foreach { l =>
        var add = l
        for (i <- 0 until watchingList.size()) {
          if (watchingList.get(i).userInfo.toString == s"StringProperty [value: ${l.userName}(${l.userId})]")
            add = null
        }
        if (add == l) {
          addList = add :: addList
        }
      }
      log.debug(s"addList:$addList")
      addList.foreach { l =>
        var imgUrl = defaultHeaderUrl
        if (l.headImgUrl.nonEmpty) {
          if (!l.headImgUrl.equals("")) {
            imgUrl = defaultHeaderUrl
          }
        }
        val headerImg = Pictures.getPic(imgUrl)
        headerImg.setFitHeight(25)
        headerImg.setFitWidth(25)
        val newRequest = WatchingListInfo(
          new SimpleObjectProperty[ImageView](headerImg),
          new SimpleStringProperty(s"${l.userName}(${l.userId})")
        )
        watchingList.add(0, newRequest)
        watchingTable.setItems(watchingList)
      }
    }
  }
  }
