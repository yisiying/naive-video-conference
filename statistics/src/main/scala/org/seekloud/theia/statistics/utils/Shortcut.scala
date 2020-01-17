package org.seekloud.theia.statistics.utils

import org.scalajs.dom

import scala.scalajs.js.Date

/**
  * User: Taoz
  * Date: 12/2/2016
  * Time: 11:12 AM
  */
object Shortcut {

  def redirect(url: String): Unit = {
    dom.window.location.href = url
  }


  def setTitle(title: String): Unit = {
    dom.document.title = title
  }

  def error(msg: String) = dom.console.error(msg)


  def addMobileMeta() = {
    import scalatags.JsDom.short._
    val oMeta =
      meta(
        *.name := "viewport",
        *.content := "width=device-width, initial-scale=1, maximum-scale=1"
      ).render
    dom.document.head.appendChild(oMeta)
  }

  def getUrlParams: Map[String, String] = {
    val paramStr =
      Option(dom.document.getElementById("fakeUrlSearch"))
        .map(_.textContent).getOrElse(dom.window.location.search)

    val str1 = paramStr.substring(1)
    val pairs = str1.split("&").filter(s => s.length > 0)
    val tmpMap = pairs.map(_.split("=", 2)).filter(_.length == 2)
    tmpMap.map(d => (d(0), d(1))).toMap
  }


  def errorDetailMsg(t: Throwable, line: Int = 5, customerMsg: Option[String] = None): String = {
    val stack = t.getStackTrace.take(line).map(t => t.toString).mkString("\n")
    val msg = t.getMessage
    val localMsg = t.getLocalizedMessage
    s"msg: $msg \nlocalMsg: $localMsg \n stack: $stack \n customerMsg: ${customerMsg.getOrElse("none")}"
  }


  def formatyyyyMMdd(date: Date) = {
    val y = date.getFullYear()
    val m = date.getMonth() + 1 match {
      case x if x <= 9 => "0" + x
      case x => x.toString
    }
    val d = date.getDate() match {
      case x if x <= 9 => "0" + x
      case x => x.toString
    }
    y + m + d
  }

  def formatyyyyMM(date: Date) = {
    val y = date.getFullYear()
    val m = date.getMonth() + 1 match {
      case x if x <= 9 => "0" + x
      case x => x.toString
    }
    y + m
  }

  def dateFormatDefault(timestamp: Long, format: String = "yyyy-MM-dd HH:mm:ss"): String = {
    DateFormatter(new Date(timestamp), format)
  }

  def DateFormatter(date: Date, `type`: String): String = {
    val y = date.getFullYear()
    val m = date.getMonth() + 1
    val d = date.getDate()
    val h = date.getHours()
    val mi = date.getMinutes()
    val s = date.getSeconds()
    date.getDate()
    val mS = if (m < 10)
      "0" + m
    else
      m
    val dS = if (d < 10)
      "0" + d
    else
      d
    val hS = if (h < 10)
      "0" + h
    else
      h
    val miS = if (mi < 10)
      "0" + mi
    else
      mi
    val sS = if (s < 10)
      "0" + s
    else
      s
    `type` match {
      case "yyyy-MM-dd hh:mm:ss" =>
        y + "-" + mS + "-" + dS + " " + hS + ":" + miS + ":" + sS
      case "yyyy-MM-dd hh:mm" =>
        y + "-" + mS + "-" + dS + " " + hS + ":" + miS
      case "yyyy-MM-dd" =>
        y + "-" + mS + "-" + dS
      case "yyyyMMdd" =>
        y + "" + mS + "" + dS
      case "yyyy/MM/dd" =>
        y + "/" + mS + "/" + dS
      case "yyyy-MM" =>
        y + "-" + mS
      case "MM-dd" =>
        mS + "-" + dS
      case "hh:mm" =>
        hS + ":" + miS
      case x =>
        y + "-" + mS + "-" + dS + " " + hS + ":" + miS + ":" + sS
    }
  }

  def scheduleOnce(f: () => Any, delayMillisecond: Long): Int = {
    dom.window.setTimeout(() => f(), delayMillisecond)
  }

  def schedule(f: () => Unit, delayMillisecond: Long): Int = {
    dom.window.setInterval(() => f(), delayMillisecond)
  }

  def cancelSchedule(h:Int): Unit = {
    dom.window.clearInterval(h)
  }



}
