package org.seekloud.theia.faceAnalysis.scene

import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, PasswordField, TextField}
import javafx.scene.image.ImageView
import javafx.scene.layout.{BorderPane, GridPane, HBox, VBox}
import org.seekloud.theia.faceAnalysis.scene.panel.SceneImpl

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午2:31
  * Define labels and layouts
  */
trait HomeScene extends SceneImpl {

  protected def login(name: String, pwd: String): Unit

  protected def register(email: String, name: String = "", pwd: String = "", pwd2: String = ""): Unit

  /*1:登录，2:注册*/
  var BUTTON_WHICH = 1

  /*css*/
  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  /*buttons*/
  val loginBtn = new Button("登  录")
  loginBtn.setPrefSize(180, 30)
  val registerBtn = new Button(" 注  册")
  registerBtn.setPrefSize(180, 30)
  loginBtn.getStyleClass.add("homeSceneBtn-login")
  registerBtn.getStyleClass.add("homeSceneBtn-login")
  /*button effects*/
  //  val shadow = new DropShadow()
  //  loginBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
  //    loginBtn.setEffect(shadow)
  //  })
  //  loginBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
  //    loginBtn.setEffect(null)
  //  })

  setListeners()
  reloadGroup()

  def reloadGroup(): Unit = {
    group.getChildren.clear()
    /*background*/
    val img = new ImageView("img/background.png")
    img.setFitWidth(width)
    img.setFitHeight(height)
    group.getChildren.add(img)
    group.getChildren.add(getBorderArea(getHeadArea, getContentArea))
  }

  def setListeners(): Unit = {
    loginBtn.setOnAction(_ => onclickLoginButton())
    registerBtn.setOnAction(_ => onclickRegisterButton())
  }

  def getHeadArea: HBox = {
    val btnHBox = new HBox(0)
    //btnHBox.setPrefHeight(100)
    btnHBox.setPrefWidth(300)
    btnHBox.setAlignment(Pos.CENTER)
    btnHBox.getChildren.addAll(loginBtn, registerBtn)
    //  btnHBox.setStyle("-fx-background-color:#673ab7;-fx-background-radius: 10px;")
    btnHBox
  }

  //根据点击的按钮切换登录、注册
  def getContentArea: VBox = {
    val vBox = new VBox(0)
    vBox.setAlignment(Pos.CENTER)

    BUTTON_WHICH match {
      case 1 =>
        vBox.getChildren.add(getLoginArea)
      case 2 =>
        vBox.getChildren.add(getRegisterArea)
      case _ =>
    }
    vBox
  }

  def getUserIcon: ImageView = {
    val userIcon = new ImageView("img/user.png")
    userIcon.setFitHeight(30)
    userIcon.setFitWidth(30)
    userIcon
  }

  def getPromptImageView(imageUrl: String): ImageView = {
    val promptImageView = new ImageView(imageUrl)
    promptImageView.setFitHeight(30)
    promptImageView.setFitWidth(30)
    promptImageView
  }

  def getPwdIcon: ImageView = {
    val pwdIcon = new ImageView("img/pwd.png")
    pwdIcon.setFitHeight(30)
    pwdIcon.setFitWidth(30)
    pwdIcon
  }

  def getPlaintTxtFld(promptText: String): TextField = {
    val nameTxtFld = new TextField()
    nameTxtFld.setPromptText(promptText)
    nameTxtFld
  }

  def getPwdTxtFld: TextField = {
    val pwdTxtFld = new PasswordField()
    pwdTxtFld.setPromptText("密码")
    pwdTxtFld
  }

  def getPwdTxtFld2: TextField = {
    val pwdTxtFld2 = new PasswordField()
    pwdTxtFld2.setPromptText("再次输入密码")
    pwdTxtFld2
  }


  def getGridPane: GridPane = {
    val gridPane = new GridPane
    gridPane.setHgap(20)
    gridPane.setVgap(20)
    gridPane.setPadding(new Insets(30, 50, 20, 50))
    gridPane
  }

  def getConfirmBtn: Button = {
    val confirmBtn = new Button("登   陆")
    BUTTON_WHICH match {
      case 1 =>
        confirmBtn.setText("登   陆")
      case 2 =>
        confirmBtn.setText("注   册")
      case _ =>
    }
    confirmBtn.getStyleClass.add("homeSceneBtn-confirm")
    confirmBtn.setPrefWidth(155)
    confirmBtn
  }

  def getLoginArea: GridPane = {
    val nameTxtFld = getPlaintTxtFld("用户名/邮箱")
    val pwdTxtFld = getPwdTxtFld
    val confirmBtn = getConfirmBtn
    /*GridPane*/
    val gridPane = getGridPane
    gridPane.add(getUserIcon, 0, 0)
    gridPane.add(nameTxtFld, 1, 0)
    gridPane.add(getPwdIcon, 0, 1)
    gridPane.add(pwdTxtFld, 1, 1)
    gridPane.add(confirmBtn,1,2)
    confirmBtn.setOnAction(_=>onConfirmClick("",nameTxtFld.getText(),pwdTxtFld.getText(),""))
    gridPane
  }

  def getRegisterArea: GridPane = {
    val nameTxtFld = getPlaintTxtFld("用户名")
    val emailTxtFld = getPlaintTxtFld("邮箱(xxx@xxx.xxx)")
    val pwdTxtFld = getPwdTxtFld
    val pwdTxtFld2 = getPwdTxtFld2
    val confirmBtn = getConfirmBtn
    /*GridPane*/
    val gridPane = getGridPane
    gridPane.add(getPromptImageView("img/email.png"), 0, 0)
    gridPane.add(emailTxtFld, 1, 0)
    gridPane.add(getUserIcon, 0, 1)
    gridPane.add(nameTxtFld, 1, 1)
    gridPane.add(getPwdIcon, 0, 2)
    gridPane.add(pwdTxtFld, 1, 2)
    gridPane.add(getPwdIcon, 0, 3)
    gridPane.add(pwdTxtFld2, 1, 3)
    gridPane.add(confirmBtn,1,4)
    confirmBtn.setOnAction(_=>onConfirmClick(emailTxtFld.getText,nameTxtFld.getText(),pwdTxtFld.getText(),pwdTxtFld2.getText()))
    gridPane
  }

  def getBorderArea(headArea: HBox,contentArea: VBox):BorderPane={
    val borderPane=new BorderPane()
    borderPane.setTop(headArea)
    borderPane.setCenter(contentArea)
    borderPane.setStyle("-fx-background-color:#fff;-fx-background-radius: 10px;")
    val borderPanePosX=320
    val borderPanePosY=150
    borderPane.setLayoutX(borderPanePosX)
    borderPane.setLayoutY(borderPanePosY)
    borderPane
  }

  def onclickLoginButton():Unit={
    BUTTON_WHICH=1
    reloadGroup()
  }

  def onclickRegisterButton(): Unit = {
    BUTTON_WHICH = 2
    reloadGroup()
  }

  def onConfirmClick(email: String = "", name: String = "", pwd: String = "", pwd2: String = ""): Unit = {
    BUTTON_WHICH match {
      case 1 =>
        login(name, pwd)
      case 2 => register(email, name, pwd, pwd2)
      case _ =>
    }
  }

}
