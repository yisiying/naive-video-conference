package org.seekloud.theia.statistics.utils

import scala.scalajs.js.Date

/**
  * Created by hongruying on 2017/5/15.
  */
object TimeTool {


  def timeFormat(timestamp: Long) = {
    new Date(timestamp).toLocaleString
  }

  /**
    * dateFormat default yyyy-MM-dd HH:mm:ss
    **/
  def dateFormatDefault(timestamp: Long, format: String = "yyyy-MM-dd HH:mm:ss"): String = {
    DateFormatter(new Date(timestamp), format)
  }
//
//  def parseDate2(date: String) = {
//    val year = date.take(4).toInt
//    val month = date.substring(5, 7).toInt - 1
//    val d = date.takeRight(2).toInt
//    val x = new Date(year, month, d)
//    x
//  }

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
      case "CN-yMdhm" =>
        y + "-" + mS + "-" + dS + "T" + hS + ":" + miS
      case x =>
        y + "-" + mS + "-" + dS + " " + hS + ":" + miS + ":" + sS
    }
  }

  def WeekFormatter(date: Date): List[Date] = {
    var first = date.getTime()
    var last = date.getTime()
    date.getDay() match {
      case 0 =>
        first = date.getTime() - 86400000 * 6
        last = date.getTime()
        List(new Date(first), new Date(last))
      case 1 =>
        first = date.getTime()
        last = date.getTime() + 86400000 * 6
        List(new Date(first), new Date(last))
      case 2 =>
        first = date.getTime() - 86400000
        last = date.getTime() + 86400000 * 5
        List(new Date(first), new Date(last))
      case 3 =>
        first = date.getTime() - 86400000 * 2
        last = date.getTime() + 86400000 * 4
        List(new Date(first), new Date(last))
      case 4 =>
        first = date.getTime() - 86400000 * 3
        last = date.getTime() + 86400000 * 3
        List(new Date(first), new Date(last))
      case 5 =>
        first = date.getTime() - 86400000 * 4
        last = date.getTime() + 86400000 * 2
        List(new Date(first), new Date(last))
      case 6 =>
        first = date.getTime() - 86400000 * 5
        last = date.getTime() + 86400000
        List(new Date(first), new Date(last))
      case x =>
        List(date)
    }
  }

  //
  def parseDate(date: String) = {
    val year = date.take(4).toInt
    val month = date.substring(4, 6).toInt - 1
    val d = date.takeRight(2).toInt
    val x = new Date(year, month, d)
    x
  }

  def parseDate_yyyy_mm_dd(date: String) = {
    val year = date.take(4).toInt
    val month = date.substring(5, 7).toInt - 1
    val d = date.takeRight(2).toInt
    val x = new Date(year, month, d)
    x
  }

  def parseDateTime(date: String) ={
    val year = date.take(4).toInt
    val month = date.substring(5, 7).toInt - 1
    val d = date.substring(8, 10).toInt
    val h = date.substring(11, 13).toInt
    val m = date.substring(14, 16).toInt
    val s = date.takeRight(2).toInt
    val x = new Date(year, month, d, h, m, s)
    x
  }

  def parseDate_yyyyMM(date: String) = {
    val year = date.take(4).toInt
    val month = date.substring(4, 6).toInt
    (year, month)
  }

  def parseDate_yyyyWW(date: String) = {
    val year = date.take(4).toInt
    val week = date.substring(4, 6).toInt
    (year, week)
  }

  def paserDateYMD(date:String) = {
    val year = date.take(4).toInt
    val month = date.substring(5, 7).toInt
    val d = date.takeRight(2).toInt
    val x = new Date(year, month, d)
    x.getMilliseconds()
  }

  def getWeekOfYear(date: Date) = {
    val year = date.getFullYear()
    val firstDay = new Date(year, 0, 1)
    val firstWeekDays = 7 - firstDay.getDay()
    val dayOfYear = ((new Date(year, date.getMonth(), date.getDate()).getTime() - firstDay.getTime()) / (24 * 3600 * 1000)) + 1
    Math.ceil((dayOfYear - firstWeekDays) / 7) + 1
  }

  def parse_yyyyMMdd_2_yyyyWW(date: String) = {
    val d = parseDate(date)
    val week = getWeekOfYear(d)
    val year = d.getFullYear()
    if (week < 10) year + "0" + week
    else year.toString + week.toString
  }


  def plusDay(date: String, day: Int): String = {
    val ts = parseDate(date).getTime() + day * (24 * 3600 * 1000)
    dateFormatDefault(ts.toLong, "yyyyMMdd")
  }

  def plusOneMinute(date: String) = {
    var time = date.take(10)
    val m = if(date.takeRight(2).toInt>=0 && date.takeRight(2).toInt<9){
      "0" + (date.takeRight(2).toInt + 1).toString
    }
    else if(date.takeRight(2).toInt==9){
      "10"
    }
    else if(date.takeRight(2).toInt==59){
      time = plusOneHour(time)
      "00"
    }
    else{
      (date.takeRight(2).toInt+1).toString
    }
    val ts = time+m
    ts
  }

  def getLatest5Minute(date:String) :String = {
    var result = ""
    for(i <- 0 to 55 by 5){
      if(date.takeRight(2).toInt >= i && date.takeRight(2).toInt < i+5){
        result = if(i < 10) "0" + i.toString else i.toString
      }
    }
    date.dropRight(2) + result
  }


  def plusOneHour(date: String) = {
    var time = date.take(8)
    val h = if(date.takeRight(2).toInt>=0 && date.takeRight(2).toInt<9){
      "0" + (date.takeRight(2).toInt + 1).toString
    }
    else if(date.takeRight(2).toInt==9){
      "10"
    }
    else if(date.takeRight(2).toInt==23){
      time = plusOneDay(time)
      "00"
    }
    else{
      (date.takeRight(2).toInt+1).toString
    }
    val ts = time+h
    ts
  }

  def plusOneDay(date: String) = {
    val ts = parseDate(date).getTime() + (24 * 3600 * 1000)
    dateFormatDefault(ts.toLong, "yyyyMMdd")
  }

  def plusOneMonth(date: String): String = {
    val yyyyMM = parseDate_yyyyMM(date)
    val month = if ((yyyyMM._2 + 1) % 12 == 0) 12 else (yyyyMM._2 + 1) % 12
    if (month < 10) yyyyMM._1 + "0" + month
    else yyyyMM._1.toString + month.toString
  }

  def plusOneMonth(stamp: Long):Long = {
    val date = new Date(stamp)
    val year = date.getFullYear()
    val month = date.getMonth()+1
    val addOne = if((month+1) > 12) s"${year+1}/${(month+1)%12}/01" else s"$year/${month+1}/01"
    println(s"${new Date(addOne).getTime().toLong}")
    new Date(addOne).getTime().toLong
  }

  def plusOneWeek(date: String): String = {
    val yyyyWW = parseDate_yyyyWW(date)
    val lastWeek = getWeekOfYear(new Date(yyyyWW._1, 11, 31))
    val (year, week) = if (yyyyWW._2 == lastWeek) (yyyyWW._1 + 1, 1) else (yyyyWW._1, yyyyWW._2 + 1)
    if (week < 10) year + "0" + week
    else year.toString + week.toString
  }

  def parseDate2(date: String) = {
    val year = date.take(4).toInt
    val month = date.substring(5, 7).toInt - 1
    val d = date.takeRight(2).toInt
    val x = new Date(year, month, d)
    x
  }

  def parseMonth(date: String) = {
    val year = date.take(4).toInt
    val month = date.substring(5, 7).toInt - 1
    val x = new Date(year, month, 1)
    x
  }


  def getStayTimeString(time: Long) = {
    time match {
      case t if t >= 24 * 60 * 60 * 1000 =>
        s"${t / 86400000}天${t % 86400000 / 3600000}时${t % 86400000 % 3600000 / 60000}分${t % 86400000 % 3600000 % 60000 / 1000}秒"
      case t if t >= 60 * 60 * 1000 =>
        s"${t / 3600000}时${t % 3600000 / 60000}分${t % 3600000 % 60000 / 1000}秒"
      case t if t >= 60 * 1000 =>
        s"${t / 60000}分${t % 60000 / 1000}秒"
      case t if t >= 1000 =>
        s"${t / 1000}秒"
      case t if t < 1000 =>
        s"${t}毫秒"
    }
  }

  def checkTime(s: Long, e: Long) = {
    if (s == 0l || e == 0l || s >= e) false else {
      if(e-s>31*24*60*60*1000l){
        false
      } else true
    }
  }

  def getCurrentMonth={
    val monthS=dateFormatDefault(System.currentTimeMillis(),"yyyy-MM")
    (monthS,new Date(monthS).getTime().toLong)
  }

  def getPreMonth={
    val date = new Date(System.currentTimeMillis())
    val year = date.getFullYear()
    val month = date.getMonth()+1
    val preOne = if((month-1) <= 0) s"${year-1}/12/01" else s"$year/${month-1}/01"
    val preOneTime = new Date(preOne).getTime().toLong
    println(s"${new Date(preOne).getTime().toLong}")
    val monthS=dateFormatDefault(preOneTime,"yyyy-MM")
    (monthS,preOneTime)
  }

  def getCurrentDayBegin={
    val date = new Date(System.currentTimeMillis())
    date.setHours(0)
    date.setMinutes(0)
    date.setSeconds(0)
    date.setMilliseconds(0)
    date.getTime().toLong
  }

  def getDayByCurrentMonth = {
    val day = new Date(System.currentTimeMillis()).getDate()
    day
  }

  def parseYyNMmy2Long(s:String)={
    new Date(s.take(4)+"/"+s.slice(5, 7)).getTime().toLong
  }

  def getWeekStart(stamp:Long):Long = {
    val date = new Date(stamp)
    val weekDay = if(date.getDay() == 0) 7 else date.getDay()
    date.getTime().toLong - (weekDay - 1)*24*3600000L
  }

  def getMonthStart(stamp:Long):Long = {
    val date = new Date(stamp)
    val monthDay = date.getDate()
    date.getTime().toLong - (monthDay - 1)*24*3600000L
  }
}
