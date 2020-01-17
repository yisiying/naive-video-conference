package org.seekloud.theia.phoneClient.common.Components

/**
  * create by 13
  * 2019/9/5
  *
  * 用来处理点赞，关注，礼物等互动元素的文本信息
  */
object InteractiveText {
  case class Gift(
                   number: Int,
                   cost:Int,
                   name: String,
                   img: String,
                   desc: String,
                   tip: String
                 )

  val giftsList = List(
    Gift(1, 10, "面包", "/theia/roomManager/static/img/gifts/bread.png", "一个面包", "花费10金币，获得10点好感"),
    Gift(2, 15, "蛋糕", "/theia/roomManager/static/img/gifts/cake.png", "一个蛋糕", "花费15金币，获得10点好感"),
    Gift(3, 2, "西蓝花", "/theia/roomManager/static/img/gifts/broccoli.png", "一个西蓝花", "花费2金币，获得10点好感"),
    Gift(4, 8, "雪糕", "/theia/roomManager/static/img/gifts/ice-cream.png", "一个雪糕", "花费8金币，获得10点好感"),
    Gift(5, 5, "棒棒糖", "/theia/roomManager/static/img/gifts/lollipop.png", "一个棒棒糖", "花费5金币，获得10点好感"),
    Gift(6, 12, "果汁", "/theia/roomManager/static/img/gifts/juice.png", "一个果汁", "花费12金币，获得10点好感")
  )

}
